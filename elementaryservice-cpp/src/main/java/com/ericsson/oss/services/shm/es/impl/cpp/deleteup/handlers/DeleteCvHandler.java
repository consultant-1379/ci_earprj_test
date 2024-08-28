/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RequestScoped
@Traceable
@Profiled
public class DeleteCvHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCvHandler.class);

    @Inject
    private CommonCvOperations commonCvOperations;

    @Inject
    private PersistJobData persistJobData;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();

    public boolean deleteCv(final long activityJobId, final long neJobId, final String nodeName, final String cvName, final boolean isRemoveFromRollbackSelected) {
        LOGGER.debug("Deleting a backup:{} on Node:{} in activityjob:", cvName, nodeName, activityJobId);
        final ConfigurationVersionMO cvMo = getCvMO(nodeName);
        boolean isCVRemoved = false;
        if (cvName != null) {
            isCVRemoved = deleteCvFromNode(nodeName, cvName, activityJobId, isRemoveFromRollbackSelected, cvMo.getFdn(), cvMo.getAllAttributes());
        }
        persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, null, jobLogs, 0.0);
        return isCVRemoved;
    }

    private boolean deleteCvFromNode(final String nodeName, final String cvName, final Long activityJobId, final boolean isRemoveFromRollbackSelected, final String cvMoFdn,
            final Map<String, Object> cvMoAttr) {
        boolean proceedToDeleteCV = true;

        if (cvMoAttr != null) {
            final List<String> rollBackList = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
            if (isRemoveFromRollbackSelected) {
                LOGGER.debug("Remove From RollbackList has selected for CV:{} on Node:{}", cvName, nodeName);
                removeFromRollbackList(activityJobId, cvMoFdn, cvName, rollBackList);
            } else {
                proceedToDeleteCV = cvNotPresentInRollbackList(cvName, rollBackList);
            }
            if (proceedToDeleteCV) {
                final String startableCV = (String) cvMoAttr.get(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
                final String currentLoadedCV = (String) cvMoAttr.get(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION);
                String logMessage = "";
                if (startableCV.equals(cvName)) {
                    proceedToDeleteCV = false;
                    logMessage = String.format(JobLogConstants.CV_REMOVAL_FROM_ROLLBACK_FAILED, cvName, "It is set as startable.");
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                } else if (currentLoadedCV.equals(cvName)) {
                    proceedToDeleteCV = false;
                    logMessage = String.format(JobLogConstants.CV_REMOVAL_FROM_ROLLBACK_FAILED, cvName, "It is a current loaded CV.");
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                } else {
                    proceedToDeleteCV = true;
                }
                LOGGER.debug("CV: {} not present in rollbacklist, and next evaluated step is:{}", cvName, logMessage);
            }
        }
        return proceedToDeleteCV;
    }

    private boolean cvNotPresentInRollbackList(final String cvName, final List<String> rollBackList) {
        boolean cvNotPresentInRollbackList = true;
        if (rollBackList != null) {
            for (final String rollBackCVName : rollBackList) {
                if (cvName.equals(rollBackCVName)) {
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.CV_REMOVAL_FROM_ROLLBACK_FAILED, cvName, "It is in RollbackList"), new Date(),
                            JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    cvNotPresentInRollbackList = false;
                    LOGGER.debug("CV: {} is present in rollback list. So it can not be deleted", cvName);
                    break;
                }
            }
        }
        return cvNotPresentInRollbackList;
    }

    private void removeFromRollbackList(final long activityJobId, final String cvMoFdn, final String cvName, final List<String> rollBackList) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            final Map<String, Object> actionArgument = new HashMap<String, Object>();
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            actionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvName);
            for (final String rollBackCVName : rollBackList) {
                if (cvName.equals(rollBackCVName)) {
                    final int actionID = commonCvOperations.executeActionOnMo(BackupActivityConstants.REMOVE_ROLLBACK_LIST, cvMoFdn, actionArgument);
                    LOGGER.info("Backup: {} is removed from rollback list with action id:{}", cvName, actionID);
                    final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
                    final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
                    if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                    }
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.CV_REMOVED_FROM_ROLLBACK, cvName), new Date(), JobLogType.SYSTEM.toString(),
                            JobLogLevel.INFO.toString());
                    break;
                }
            }
        } catch (final JobDataNotFoundException ex) {
            LOGGER.error("DeleteCvHandler.removeFromRollbackList- Unable to trigger action. Reason: ", ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogs, null, ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
        } catch (final Exception exception) {
            logDeleteFailAsCVInRollbackList(cvName, exception, activityJobId);
        }
    }

    private void logDeleteFailAsCVInRollbackList(final String cvName, final Exception exception, final long activityJobId) {
        LOGGER.error("Unable to remove CV:{} from rollBackList for activityJobId:{}, due to:", cvName, activityJobId, exception);
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
        if (!exceptionMessage.isEmpty()) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.CV_REMOVAL_FROM_ROLLBACK_FAILED, cvName, exceptionMessage), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.CV_REMOVAL_FROM_ROLLBACK_FAILED, cvName, exception.getMessage()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        }
    }

    public ConfigurationVersionMO getCvMO(final String nodeName) {
        return commonCvOperations.getCVMo(nodeName);
    }

}
