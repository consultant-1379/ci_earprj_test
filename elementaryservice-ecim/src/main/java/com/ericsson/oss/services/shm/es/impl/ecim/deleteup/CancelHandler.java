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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@Traceable
@RequestScoped
public class CancelHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelHandler.class);

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private TimeoutHandler timeoutHandler;

    private String nodeName = "";

    private JobEnvironment jobEnvironment = null;

    private NEJobStaticData neJobStaticData = null;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

    public ActivityStepResult cancel(final long activityJobId) {

        initializeVariables(activityJobId);
        ActivityStepResult cancelResult = new ActivityStepResult();

        activityUtils.logCancelledByUser(jobLogs, jobEnvironment, ActivityConstants.DELETE_UP_DISPLAY_NAME);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);

        final String currentBackupData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.CURRENT_BKPNAME);

        if (!currentBackupData.isEmpty()) {
            final String[] currentBackupDataArray = currentBackupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            cancelResult = handleCancelFordeleteBackupOnNode(activityJobId, currentBackupDataArray[1]);
        } else {
            cancelResult = handleCancelFordeleteUpgardePackageOnNode(activityJobId);
        }
        return cancelResult;
    }

    private ActivityStepResult handleCancelFordeleteUpgardePackageOnNode(final long activityJobId) {
        LOGGER.debug("Inside cancelhandler handleCancelFordeleteUpgardePackageOnNode  nodeName : {} , and activityJobId: {}", nodeName, activityJobId);
        final ActivityStepResult cancelResult = new ActivityStepResult();
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_CANCEL_TRIGGERED, ShmConstants.TRUE.toLowerCase());

        final String logMessage = String.format(JobLogConstants.DELETEUP_CANCEL_NOT_POSSIBLE, ActivityConstants.DELETE_UP_DISPLAY_NAME);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());

        activityUtils.recordEvent(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_CANCEL, nodeName, "cancel", logMessage);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);

        cancelResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        return cancelResult;
    }

    private ActivityStepResult handleCancelFordeleteBackupOnNode(final long activityJobId, final String brmBackupManagerMoFdn) {
        LOGGER.debug("Inside cancelhandler handleCancelFordeleteBackupOnNode  nodeName : {} , and activityJobId: {}", nodeName, activityJobId);
        final ActivityStepResult cancelResult = new ActivityStepResult();
        String logMessage = null;
        int actionId = -1;
        final Map<String, Object> actionArguments = new HashMap<String, Object>();

        activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
        try {
            actionId = brmMoServiceRetryProxy.executeCancelAction(nodeName, brmBackupManagerMoFdn, EcimBackupConstants.BACKUP_CANCEL_ACTION, actionArguments);
            LOGGER.debug("Backup Cancel actionId {} ", actionId);
            logMessage = String.format(JobLogConstants.REFERRED_BKP_CANCEL_INVOKED_ON_NODE, ActivityConstants.DELETE_UP_DISPLAY_NAME);
            LOGGER.debug(logMessage);
            cancelResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
            activityUtils.addJobProperty(EcimCommonConstants.ACTION_TRIGGERED, EcimBackupConstants.BACKUP_CANCEL_ACTION, jobProperties);
            updateJobLog(activityJobId, logMessage);
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.error("MoNotFoundException : ", moNotFoundException);
            logMessage = String.format(JobLogConstants.CANCEL_MO_NOT_EXIST, EcimBackupConstants.BACKUP_CANCEL_ACTION);
            updateJobLog(activityJobId, logMessage);
            cancelResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            return cancelResult;
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("UnsupportedFragmentException : ", unsupportedFragmentException);
            logMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            updateJobLog(activityJobId, logMessage);
            cancelResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            return cancelResult;
        } catch (final Exception ex) {
            handleException(ex, activityJobId, cancelResult);
        }
        LOGGER.debug("{} for activity {}", logMessage, activityJobId);
        return cancelResult;
    }

    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        initializeVariables(activityJobId);

        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        final String currentBackupData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.CURRENT_BKPNAME);

        if (!currentBackupData.isEmpty()) {
            final String[] currentBackupDataArray = currentBackupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            activityStepResult = timeoutHandler.handleTimeoutFordeleteBackupOnNode(activityJobId, currentBackupDataArray[0], currentBackupDataArray[1], currentBackupData);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            final String logMessage = String.format(JobLogConstants.DELETEUP_CANCEL_NOT_POSSIBLE, ActivityConstants.DELETE_UP_DISPLAY_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            activityUtils.recordEvent(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_CANCEL_TIMEOUT, nodeName, "cancel", logMessage);
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);
        }
        return activityStepResult;
    }

    private void handleException(final Exception ex, final long activityJobId, final ActivityStepResult activityStepResult) {
        LOGGER.error("Exception is caught in backup cancel action, exception message : ", ex);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        final String errorMessage = activityUtils.prepareErrorMessage(ex);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs,
                String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME) + " " + String.format(JobLogConstants.FAILURE_REASON, errorMessage), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);
        jobLogs.clear();
    }

    private void updateJobLog(final long activityJobId, final String logMessage) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);
        jobLogs.clear();
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }
}
