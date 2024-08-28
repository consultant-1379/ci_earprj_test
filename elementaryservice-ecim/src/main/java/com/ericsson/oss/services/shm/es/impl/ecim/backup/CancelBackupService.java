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
package com.ericsson.oss.services.shm.es.impl.ecim.backup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * 
 * This is common class for all cancel actions happening to backup job(create and upload backup), delete backup job.
 * 
 * This is a reusable class that can be used for cancel restore backup as well.
 * 
 * @author xprapav
 * 
 * 
 */
public class CancelBackupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelBackupService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    /**
     * This method cancels current running action(i.e createBackup or uploadBackup or DeleteBackup) on Node.
     * 
     * @param activityJobId
     * @param activityName
     * @param defaultactivityTimeout
     */
    public ActivityStepResult cancel(final long activityJobId, final String activityName, final EcimBackupInfo ecimBackupInfo) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String logMessage = null;
        String nodeName = null;
        String mofdn = null;
        int actionId = -1;
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        LOGGER.debug("Cancel action on activityJobId {} and activityName {} ", activityJobId, activityName);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, activityName);
        nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("Cancel action going to be triggered on nodeName {}  ", nodeName);
        try {
            //final EcimBackupInfo ecimBackupInfo = inAnyRestoreActivity ? ecimBackupUtils.getBackupInfoForRestore(jobEnvironment) : ecimBackupUtils.getBackup(jobEnvironment);
            mofdn = brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo);
            actionId = brmMoServiceRetryProxy.executeCancelAction(nodeName, mofdn, EcimBackupConstants.BACKUP_CANCEL_ACTION, actionArguments);
            LOGGER.debug("BackUp Cancel actionId {} ", actionId);
            logMessage = String.format(JobLogConstants.CANCEL_INVOKED_ON_NODE, activityName);
            LOGGER.debug(logMessage);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
            // TODO Need to enable un-subscription in integration testing story
            //pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityName, nodeName);
            final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(propertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
            activityUtils.addJobProperty(EcimCommonConstants.ACTION_TRIGGERED, EcimBackupConstants.BACKUP_CANCEL_ACTION, propertyList);
            updateJobLog(activityJobId, logMessage, jobLogList, propertyList);
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.error("MoNotFoundException : ", moNotFoundException);
            logMessage = String.format(JobLogConstants.CANCEL_MO_NOT_EXIST, EcimBackupConstants.BACKUP_CANCEL_ACTION);
            updateJobLog(activityJobId, logMessage, jobLogList, null);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            return activityStepResult;
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("UnsupportedFragmentException : ", unsupportedFragmentException);
            logMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            updateJobLog(activityJobId, logMessage, jobLogList, null);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            return activityStepResult;
        } catch (final Exception ex) {
            final String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            LOGGER.error("Exception is caught in backUp cancel action, exception message : ", ex);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.BACKUP_CANCEL_ACTION) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED + message, EcimBackupConstants.BACKUP_CANCEL_ACTION);
            }
            updateJobLog(activityJobId, logMessage, jobLogList, null);
            return activityStepResult;
        }
        LOGGER.debug("{} for activity {}", logMessage, activityJobId);
        return activityStepResult;

    }

    /**
     * This method checks cancel Progress such as Success or Failure.
     * 
     * @param progressReport
     * @param jobActivityInfo
     * @param brmBackupManagerMoFdn
     * @param activityJobId
     * @param jobEnvironment
     * 
     */
    public void evaluateCancelProgress(final AsyncActionProgress progressReport, final JobActivityInfo jobActivityInfo, final String brmBackupManagerMoFdn, final NEJobStaticData neJobStaticData,
            final String activityName) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobTypeEnum jobType = jobActivityInfo.getJobType();
        LOGGER.debug("evaluateCancelProgress activityJobId {} and  activityName {}", activityJobId, progressReport.getActionName());
        if (!validateActionProgressReport(progressReport, activityName, activityName)) {
            final ActionStateType state = progressReport.getState();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            JobResult jobResult = null;
            String jobLogMessage = null;
            LOGGER.debug("evaluateCancelProgress state {} ", state.getInfoMessage());
            if ((state.equals(ActionStateType.RUNNING) && EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName()))
                    || ((state.equals(ActionStateType.CANCELLING) && activityName.equals(progressReport.getActionName())))) {
                final String logMessage = String.format(JobLogConstants.CANCEL_IN_PROGRESS, progressReport.getProgressPercentage(), progressReport.getProgressInfo());
                updateJobLog(activityJobId, logMessage, jobLogList, null);
            } else if ((state.equals(ActionStateType.FINISHED) && activityName.equals(progressReport.getActionName()))) {
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
                jobResult = JobResult.FAILED;
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, progressReport.getActionName());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());

                recordEvent(activityJobId, brmBackupManagerMoFdn, jobLogMessage, activityName, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS, jobType);
                updateJobLog(activityJobId, jobLogMessage, jobLogList, jobPropertyList);
            } else if (state.equals(ActionStateType.FINISHED) && EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName())) {
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
                if (isCancelSuccess(progressReport)) {
                    LOGGER.debug("Cancel Backup activity finished successfully");
                    jobResult = JobResult.CANCELLED;
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, progressReport.getActionName());
                    activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
                    recordEvent(activityJobId, brmBackupManagerMoFdn, jobLogMessage, activityName, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS, jobType);
                    updateJobLog(activityJobId, jobLogMessage, jobLogList, jobPropertyList);
                }
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
                LOGGER.debug("evaluateCancelProgress sendActivateToWFS {} ", state);
            }

        }
    }

    /**
     * This method checks cancel Progress when activity in HandleTimeout.
     * 
     * @param progressReport
     * @param activityJobId
     * 
     */
    public ActivityStepResult verifyCancelHandleTimeout(final AsyncActionProgress progressReport, final long activityJobId) {
        String jobLogMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        if (progressReport != null) {
            if (isCancelSuccess(progressReport)) {
                handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, progressReport.getActionName());

            } else {
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, progressReport.getActionName()) + progressReport.getResultInfo();
            }
        } else {
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.BACKUP_CANCEL_ACTION);
        }
        updateJobLog(activityJobId, jobLogMessage, jobLogList, null);
        activityStepResult.setActivityResultEnum(handleTimeoutStatus);
        return activityStepResult;
    }

    /**
     * This method checks whether cancel ActionTriggerred or not.
     * 
     * @param activityJobAttributes
     * @return boolean
     */
    public boolean isCancelActionTriggerred(final Map<String, Object> activityJobAttributes) {
        return EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.ACTION_TRIGGERED));
    }

    /**
     * This method checks whether Cancel Success or not.
     * 
     * @param progressReport
     * @return boolean
     */
    public boolean isCancelSuccess(final AsyncActionProgress progressReport) {
        return (ActionResultType.SUCCESS == progressReport.getResult()) ? true : false;
    }

    private void updateJobLog(final long activityJobId, final String logMessage, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList) {
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, propertyList, jobLogList);
    }

    public boolean validateActionProgressReport(final AsyncActionProgress progressReport, final String activityName) {
        boolean progressReportFlag = false;
        if (progressReport == null) {
            progressReportFlag = true;
        } else if (!(activityName.equals(progressReport.getActionName()) || EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName()))) {
            progressReportFlag = true;
        }
        return progressReportFlag;
    }

    public boolean validateActionProgressReport(final AsyncActionProgress progressReport, final String activityName, final String correlationActivityName) {
        boolean progressReportFlag = false;
        if ((progressReport == null)
                || (!(activityName.equals(progressReport.getActionName()) || correlationActivityName.equals(progressReport.getActionName()) || EcimBackupConstants.BACKUP_CANCEL_ACTION
                        .equals(progressReport.getActionName())))) {
            progressReportFlag = true;
        }
        return progressReportFlag;
    }

    private void recordEvent(final long activityJobId, final String brmBackupManagerMoFdn, final String jobLogMessage, final String activityName, final String flow, final JobTypeEnum jobType) {
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, jobType, activityName);
        activityUtils.recordEvent(eventName, brmBackupManagerMoFdn, brmBackupManagerMoFdn, "SHM:" + activityJobId + ":" + jobLogMessage + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }
}
