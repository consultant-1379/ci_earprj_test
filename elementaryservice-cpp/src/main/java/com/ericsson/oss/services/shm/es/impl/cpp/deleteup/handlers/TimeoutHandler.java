/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.EXECUTE_REPEAT_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpMO;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageService;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageUtil;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * To handle the timeout scenarios for an Upgrade Package removal and CV removal from rollback list.
 * <li>CV (ConfigurationVersion) removal from rollbackList is a synchronous operation(MO action).
 * <li>UP (UpgradePackage) removal from node is a CRUD operation.
 * 
 * @author xrajeke
 *
 */
public class TimeoutHandler {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private DeleteUpgradePackageUtil deleteUpgradePackageUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private DeleteUpMO deleteUpMO;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private PersistJobData persistJobData;

    @Inject
    private DeleteCvHandler cvHandler;

    @Inject
    private JobConfigurationServiceRetryProxy jobReaderProxy;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutHandler.class);

    public ActivityStepResult handleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final ActivityStepResult timeoutResponse = new ActivityStepResult();
        long activityStartTime = 0;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            activityStartTime = neJobStaticData.getActivityStartTime();
            JobResult jobResult = null;

            final Map<String, Object> activityJobAttributes = jobReaderProxy.getActivityJobAttributes(activityJobId);
            final String currentDeleteActivity = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.PROCESSING_MO_TYPE);
            if (ShmConstants.CV_MO_TYPE.equals(currentDeleteActivity)) {
                final String cvName = activityUtils.getActivityJobAttributeValue(activityJobAttributes, ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME);
                jobResult = evaluateCVRemovalFromRollBackList(activityJobId, nodeName, cvName);
            } else {
                final String upFdn = activityUtils.getActivityJobAttributeValue(activityJobAttributes, ShmConstants.FDN);
                final String[] currentUP = (activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.CURRENT_PROCESSING_UP))
                        .split(UpgradeActivityConstants.UPGRADEPACKAGES_PERSISTENCE_DELIMTER);
                jobResult = evaluateUPRemovalFromNode(activityJobId, nodeName, neJobStaticData, currentUP[0], currentUP[1], upFdn);
            }
            timeoutResponse.setActivityResultEnum(evaluateTimeoutResultEnum(activityJobId, neJobStaticData, networkElement, jobResult));

        } catch (Exception ex) {
            LOGGER.error("Exception occured in handleTimeout while evaluating the UpgradePackage deletion status.", ex);
            final String logMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME, ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
            timeoutResponse.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        return timeoutResponse;
    }

    private JobResult evaluateCVRemovalFromRollBackList(final long activityJobId, final String nodeName, final String cvName) {
        final ConfigurationVersionMO cvMo = cvHandler.getCvMO(nodeName);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        final List<String> rollbackList = (List<String>) (cvMo.getAllAttributes().get(ConfigurationVersionMoConstants.ROLLBACK_LIST));
        LOGGER.info("Evaluating CV:{} removal from rollback list:{} in Timeout for activityJobId {}", cvName, rollbackList, activityJobId);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        if (rollbackList.contains(cvName)) {
            return JobResult.FAILED;
        } else {
            return JobResult.SUCCESS;
        }

    }

    private ActivityStepResultEnum evaluateTimeoutResultEnum(final long activityJobId, final NEJobStaticData neJobStaticData, final NetworkElementData networkElement, final JobResult jobResult) {
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        ActivityStepResultEnum timeoutResponse = null;
        try {
            if (jobResult == JobResult.FAILED) {
                activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.INTERMEDIATE_FAILURE, jobResult.toString());
            }
            final Map<String, Object> repeatRequiredAndActivityResult = deleteUpgradePackageUtil.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, neJobStaticData,
                    networkElement.getNeType());
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            final JobResult evaluatedActivityResult = (JobResult) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
            final NEJobStaticData jobStaticContext = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);

            if (repeatRequired) {
                timeoutResponse = ActivityStepResultEnum.REPEAT_EXECUTE;
            } else {
                timeoutResponse = (evaluatedActivityResult == JobResult.SUCCESS) ? ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS : ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
                activityUtils.persistStepDurations(activityJobId, jobStaticContext.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, evaluatedActivityResult.getJobResult());
            }
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobStaticData.getNeJobId(), jobProperties, null, 0.0);
        } catch (final JobDataNotFoundException ex) {
            LOGGER.error("TimeoutHandler.evaluateTimeoutResultEnum- {} ", ex.getMessage());
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.addJobLog(String.format(JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, ex.getMessage()), JobLogLevel.ERROR.toString(), jobLogList,
                    JobLogType.SYSTEM.toString());
            timeoutResponse = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        }
        return timeoutResponse;
    }

    private JobResult evaluateUPRemovalFromNode(final long activityJobId, final String nodeName, final NEJobStaticData neJobStaticData, final String currentUpProductNumber,
            final String currentUpProductRevision, final String executedUPFdn) {
        LOGGER.info("Evaluating UP:[ProductNumber={}, ProductRevision={}, Fdn:{}] removal from node:{} in Timeout", currentUpProductNumber, currentUpProductRevision, executedUPFdn, nodeName);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String logMessage = "";
        JobResult activityResult = null;
        final ManagedObject upMO = deleteUpMO.findMoByFdn(executedUPFdn);

        activityUtils.unSubscribeToMoNotifications(executedUPFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.DELETEUP_TIMEOUT, ActivityConstants.DELETE_UP_DISPLAY_NAME, currentUpProductNumber, currentUpProductRevision),
                new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        if (upMO == null) {
            logMessage = String.format(JobLogConstants.UP_DELETED_SUCCESSFULLY, currentUpProductNumber, currentUpProductRevision);
            activityUtils.recordEvent(SHMEvents.CPP_DELETEUPGRADEPACKAGE_TIMEOUT, nodeName, executedUPFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityResult = JobResult.SUCCESS;
            final Map<String, Object> activityJobAttributes = jobReaderProxy.getActivityJobAttributes(activityJobId);
            final String totalUPs = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.TOTAL_UPS);
            final int totalUPsCount = Integer.parseInt(totalUPs);
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final Double currentProgressPercentage = activityAndNEJobProgressPercentageCalculator.calculateActivityProgressPercentage(jobEnvironment, totalUPsCount,
                    EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobStaticData.getNeJobId(), null, null, currentProgressPercentage);
        } else {
            logMessage = String.format(JobLogConstants.UP_DELETION_FAILED, currentUpProductNumber, currentUpProductRevision);
            activityUtils.recordEvent(SHMEvents.CPP_DELETEUPGRADEPACKAGE_TIMEOUT, nodeName, executedUPFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityResult = JobResult.FAILED;
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityResult;
    }
}
