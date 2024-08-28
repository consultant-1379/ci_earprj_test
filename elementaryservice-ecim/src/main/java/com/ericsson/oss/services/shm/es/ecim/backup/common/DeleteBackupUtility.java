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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class DeleteBackupUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupUtility.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private EcimCommonUtils ecimCommonUtils;

    @Inject
    private CancelBackupService cancelBackupService;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    public void deleteBackupFromNode(final long activityJobId, final NEJobStaticData neJobStaticData, final String backupName, final JobTypeEnum jobTypeEnum, final JobActivityInfo jobActivityInfo) {
        final String nodeName = neJobStaticData.getNodeName();
        LOGGER.debug("Deleting backup from Node with backupName : {} , nodeName : {} , and activityJobId: {}", backupName, nodeName, activityJobId);
        String brmBackupManagerMoFdn = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        if (backupName != null && !backupName.isEmpty()) {
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(backupName);
            try {
                final String logMessage = String.format(JobLogConstants.EXECUTION_STARTED, EcimBackupConstants.DELETE_BACKUP, ecimBackupInfo.getBackupName());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo);
                activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_EXECUTE, nodeName, brmBackupManagerMoFdn, "SHM:" + activityJobId + ":" + nodeName);
                activityUtils.subscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
                performActionOnNode(neJobStaticData, ecimBackupInfo, brmBackupManagerMoFdn, jobLogList, jobActivityInfo);
            } catch (final MoNotFoundException moNotFoundException) {
                LOGGER.error("BrmBackupManagerMo not found to proceed with delete backup action : {}", moNotFoundException);
                final String jobLogMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName); //Job Logs
                handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, jobLogMessage);
            } catch (final Exception exception) {
                LOGGER.error("Unable to start MO Action for {} activity with activity job id {} on BrmBackupManager MO with FDN: {} due to the exception : {}", EcimBackupConstants.DELETE_BACKUP,
                        activityJobId, brmBackupManagerMoFdn, exception);
                final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.DELETE_BACKUP)
                        + String.format(JobLogConstants.FAILURE_REASON, exception.getMessage());
                handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, jobLogMessage);
            }
        }
    }

    /**
     * Method to execute MO Action for "deletebackup" activity on the node.
     * 
     * @param activityJobId
     * @param neJobStaticData
     * @param ecimBackupInfo
     * @param nodeName
     * @param brmBackupManagerMoFdn
     * @param jobLogList
     */
    private void performActionOnNode(final NEJobStaticData neJobStaticData, final EcimBackupInfo ecimBackupInfo, final String brmBackupManagerMoFdn, final List<Map<String, Object>> jobLogList,
            final JobActivityInfo jobActivityInfo) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobTypeEnum jobTypeEnum = jobActivityInfo.getJobType();
        int actionInvocationResult = -1;
        boolean actionSuccessfullyTriggered = false;
        new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final long mainJobId = neJobStaticData.getMainJobId();
        final String nodeName = neJobStaticData.getNodeName();
        String neType = null;
        try {
            actionInvocationResult = brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupManagerMoFdn, EcimBackupConstants.DELETE_BACKUP);
            LOGGER.debug("performaction on node : actionInvocationResult : {}", actionInvocationResult);
            actionSuccessfullyTriggered = true;
            if (actionSuccessfullyTriggered) {
                final Map<String, Object> activityJobAttrs = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
                final String existingStepDurations = ((String) activityJobAttrs.get(ShmConstants.STEP_DURATIONS));
                if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                }
            }
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(nodeName), SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
            }
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            handleUnsupportedFragmentException(ecimBackupInfo, jobLogList, unsupportedFragmentException);
        } catch (final Exception exception) {
            handleException(ecimBackupInfo, jobLogList, exception);
            if (exception.getMessage() != null && exception.getMessage().contains(JobLogConstants.BACKUP_DOES_NOT_EXISTS)) {
                final String logMessage = String.format(JobLogConstants.BACKUP_CANNOT_BE_DELETED_ON_NODE, ecimBackupInfo.getBackupName());
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
                handleActivityJobSkippedScenario(activityJobId, neJobStaticData, jobLogList, jobPropertyList, logMessage);
                return;
            }
        }
        LOGGER.debug("actionInvocationResult and actionSuccessfullyTriggered: {}, {}", actionInvocationResult, actionSuccessfullyTriggered);
        if (actionInvocationResult == 0 && actionSuccessfullyTriggered) {
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), jobTypeEnum.name(), EcimBackupConstants.DELETE_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.BACKUP_NAME + JobLogConstants.ASYNC_ACTION_TRIGGERED, ecimBackupInfo.getBackupName(), EcimBackupConstants.DELETE_BACKUP, activityTimeout),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobTypeEnum));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } else {
            final String logMessage = String.format(JobLogConstants.BACKUP_NAME + JobLogConstants.ACTION_TRIGGER_FAILED, ecimBackupInfo.getBackupName(), EcimBackupConstants.DELETE_BACKUP);
            activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobTypeEnum));
            handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, logMessage);
        }
    }

    private void handleException(final EcimBackupInfo ecimBackupInfo, final List<Map<String, Object>> jobLogList, final Exception exception) {
        String jobLogMessage;
        final Throwable cause = exception.getCause();
        final String message = cause != null ? cause.getMessage() : exception.getMessage();
        LOGGER.error(exception.getMessage(), exception);
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
        if (!exceptionMessage.isEmpty()) {
            jobLogMessage = String.format(JobLogConstants.BACKUP_NAME + JobLogConstants.ACTION_TRIGGER_FAILED + JobLogConstants.FAILURE_REASON, ecimBackupInfo.getBackupName(),
                    EcimBackupConstants.DELETE_BACKUP, exceptionMessage);
        } else {
            jobLogMessage = String.format(JobLogConstants.BACKUP_NAME + JobLogConstants.ACTION_TRIGGER_FAILED + JobLogConstants.FAILURE_REASON, ecimBackupInfo.getBackupName(),
                    EcimBackupConstants.DELETE_BACKUP, message);
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
    }

    private void handleUnsupportedFragmentException(final EcimBackupInfo ecimBackupInfo, final List<Map<String, Object>> jobLogList, final UnsupportedFragmentException unsupportedFragmentException) {
        String jobLogMessage;
        final Throwable cause = unsupportedFragmentException.getCause();
        final String message = cause != null ? cause.getMessage() : unsupportedFragmentException.getMessage();
        LOGGER.error(unsupportedFragmentException.getMessage(), unsupportedFragmentException);
        jobLogMessage = String.format(JobLogConstants.BACKUP_NAME + JobLogConstants.ACTION_TRIGGER_FAILED + JobLogConstants.FAILURE_REASON, ecimBackupInfo.getBackupName(),
                EcimBackupConstants.DELETE_BACKUP, message);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
    }

    public void handleNotification(final EcimBackupInfo ecimBackupInfo, final NotificationSubject notificationSubject, final Map<String, AttributeChangeData> modifiedAttributes,
            final NEJobStaticData neJobStaticData, final JobTypeEnum jobTypeEnum, final JobActivityInfo jobActivityInfo) {
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        String jobLogMessage = null;
        AsyncActionProgress progressReport = null;
        nodeName = neJobStaticData.getNodeName();
        final String backupName = ecimBackupInfo.getBackupName();
        JobResult jobResult = null;
        double totalActivityProgressPercentage = 0.0;
        try {
            progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributes);
            LOGGER.debug("progressReport in processNotification{}", progressReport);
            final boolean isInvalidNotification = cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.BACKUP_DELETE_ACTION_BSP,
                    EcimBackupConstants.BACKUP_DELETE_ACTION);
            if (isInvalidNotification) {
                LOGGER.warn("Discarding invalid notification,for the activityJobId {} and modifiedAttributes {}", activityJobId, modifiedAttributes);
                return;
            }
            final String brmBackupManagerMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
            if (EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName())) {
                jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, notificationTime, EcimBackupConstants.DELETE_BACKUP);
            } else if (EcimBackupConstants.BACKUP_DELETE_ACTION.equals(progressReport.getActionName()) || EcimBackupConstants.BACKUP_DELETE_ACTION_BSP.equals(progressReport.getActionName())) {
                jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, notificationTime, EcimBackupConstants.DELETE_BACKUP, backupName);
                totalActivityProgressPercentage = ecimCommonUtils.calculateActivityProgressPercentage(activityJobId, progressReport);
            }
            if (jobResult != null) {
                onActionCompleted(jobLogList, jobPropertyList, neJobStaticData, jobResult, brmBackupManagerMoFdn, jobTypeEnum, jobActivityInfo, backupName, totalActivityProgressPercentage);
            } else {
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, totalActivityProgressPercentage);
                progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
            }
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.error("BrmBackupManagerMo not found for notification on backup {} on node {} with exception {}", backupName, nodeName, moNotFoundException);
            jobLogMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName);
            handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, jobLogMessage);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("Unsupported fragment for notification on backup {} on node {} with exception {}", backupName, nodeName, unsupportedFragmentException);
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, jobLogMessage);
        } catch (final Exception exception) {
            LOGGER.error("Exception occured during processing of notifications for delete backup action on backup {} on node {} with exception : {}", backupName, nodeName, exception);
            jobLogMessage = String.format(JobLogConstants.BACKUP_NAME + JobLogConstants.ACTION_FAILED + JobLogConstants.FAILURE_DUE_TO_EXCEPTION, ecimBackupInfo.getBackupName())
                    + exception.getMessage();
            handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, jobLogMessage);
        }
    }

    private void onActionCompleted(final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList, final NEJobStaticData neJobStaticData, final JobResult jobResult,
            final String brmBackupManagerMoFdn, final JobTypeEnum jobTypeEnum, final JobActivityInfo jobActivityInfo, final String backupName, final double totalActivityProgressPercentage) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();
        if (jobResult == JobResult.SUCCESS) {
            final String jobLogMessage = String.format("Backup File = %s  deleted successfully on the node = %s.", backupName, nodeName);//Job Logs.
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobTypeEnum));
        } else if (jobResult == JobResult.FAILED) {
            final String jobLogMessage = String.format("Deletion of backup - %s failed on the node - %s.", backupName, nodeName);//Job logs.
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobTypeEnum));
        }
        activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        } else if (jobResult == JobResult.SUCCESS || jobResult == JobResult.FAILED) {
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, totalActivityProgressPercentage);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, EcimBackupConstants.DELETE_BACKUP, processVariables);
    }

    public void handleFailureOrException(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final String jobLogMessage) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        //Update intermediate failure details and set the activity result. Notify the workflow.
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, EcimBackupConstants.DELETE_BACKUP, processVariables);
    }

    public void handleActivityJobSkippedScenario(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList, final String jobLogMessage) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        //Update intermediate failure details and set the activity result. Notify the workflow.
        final Map<String, Object> processVariables = new HashMap<>();
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SKIPPED, jobPropertyList);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, EcimBackupConstants.DELETE_BACKUP, processVariables);
    }

    public JobResult evaluateJobResult(final EcimBackupInfo ecimBackupInfo, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final JobTypeEnum jobTypeEnum,
            final JobActivityInfo jobActivityInfo) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();
        JobResult jobResult = null;
        LOGGER.debug("handleTimeoutFordeleteBackupOnNode with backupName : {} , nodeName : {} , and activityJobId: {}", ecimBackupInfo.getBackupName(), nodeName, activityJobId);
        String brmBackupManagerMoFdn = null;
        String jobLogMessage = null;
        try {
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
            final String logMessage = String.format("Delete Backup has Timed Out on node %s", nodeName);
            activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            if (brmMoServiceRetryProxy.isBackupDeletionCompleted(nodeName, ecimBackupInfo)) { //Timeout success.
                jobLogMessage = String.format("Backup File = %s  deleted successfully after timeout on the node = %s.", ecimBackupInfo.getBackupName(), nodeName);//Job Logs.
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                systemRecorder.recordCommand(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                        activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), jobTypeEnum));
                LOGGER.debug("Completed deletion of {} successfully on the node {} after timeout.", ecimBackupInfo.getBackupName(), nodeName);
                jobResult = JobResult.SUCCESS;
            } else { //Timeout failure.
                jobLogMessage = String.format("Deletion of backup - %s failed after timeout on the node - %s.", ecimBackupInfo.getBackupName(), nodeName);//Job logs. 
                LOGGER.error("Backup deletion failed after timeout for backup - {} on the node {}.", ecimBackupInfo.getBackupName(), nodeName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                systemRecorder.recordCommand(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                        activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), jobTypeEnum));
                jobResult = JobResult.FAILED;
            }
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node {} with exception : {}", nodeName, moNotFoundException);//Job Logs.
            jobLogMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            systemRecorder.recordCommand(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), jobTypeEnum));
            jobResult = JobResult.FAILED;

        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("Unsupported fragment during time out evaluation of activity completion for the node {} with exception : {}", nodeName, unsupportedFragmentException); //Job Logs.
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            systemRecorder.recordCommand(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), jobTypeEnum));
            jobResult = JobResult.FAILED;
        }
        return jobResult;
    }

    public Map<String, Object> evaluateRepeatRequiredAndActivityResult(final long activityJobId, final JobResult moActionResult, final List<Map<String, Object>> jobPropertyList) {
        LOGGER.debug("Evaluate whether repeat is Required and activity result. moActionResult {}, jobPropertyList {}", moActionResult, jobPropertyList);
        boolean recentUploadFailed = false;
        boolean repeatExecute = true;
        boolean isActivitySuccess = false;
        boolean isRecentDeleteSuccess = false;
        JobResult activityJobResult = null;
        if (moActionResult == JobResult.FAILED) {
            recentUploadFailed = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        } else if (moActionResult == JobResult.SUCCESS) {
            isRecentDeleteSuccess = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_SUCCESS, String.valueOf(isRecentDeleteSuccess));
        }
        final boolean allBackupsProcessed = isAllBackupsProcessed(activityJobId);
        if (allBackupsProcessed) {
            final boolean intermediateFailureHappened = isAnyIntermediateFailureHappened(activityJobId);
            final boolean isDeleteActivityPassed = isDeleteActivityPassed(activityJobId);
            if (intermediateFailureHappened || recentUploadFailed) {
                activityJobResult = JobResult.FAILED;
            } else if (isDeleteActivityPassed || moActionResult == JobResult.SUCCESS) {
                activityJobResult = JobResult.SUCCESS;
            } else {
                activityJobResult = JobResult.SKIPPED;
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
            repeatExecute = false;
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            activityJobResult = JobResult.CANCELLED;
            repeatExecute = false;
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
        }

        if (activityJobResult == JobResult.SUCCESS || activityJobResult == JobResult.SKIPPED) {
            isActivitySuccess = true;
        }
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, isActivitySuccess);
        LOGGER.debug("Is Repeat Required or ActivityResult evaluated : {}", repeatRequiredAndActivityResult);
        return repeatRequiredAndActivityResult;
    }

    private boolean isAllBackupsProcessed(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final int processedBackups = getCountOfProcessedBackups(activityJobAttributes);
        final int totalBackups = getCountOfTotalBackups(activityJobAttributes);
        if (processedBackups == totalBackups) {
            return true;
        }
        return false;
    }

    public int getCountOfTotalBackups(final Map<String, Object> activityJobAttributes) {
        return ecimCommonUtils.getCountOfTotalBackups(activityJobAttributes);
    }

    protected int getCountOfProcessedBackups(final Map<String, Object> activityJobAttributes) {
        int processedBackups = 0;
        final String processedBackupsString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.PROCESSED_BACKUPS);
        if (processedBackupsString != null && !processedBackupsString.isEmpty()) {
            processedBackups = Integer.parseInt(processedBackupsString);
        }
        LOGGER.debug("Processed backups count = {}", processedBackups);
        return processedBackups;
    }

    private boolean isAnyIntermediateFailureHappened(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final String intermediateFailure = activityUtils.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.INTERMEDIATE_FAILURE);
        if (intermediateFailure != null && !intermediateFailure.isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean isDeleteActivityPassed(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final String isDeleteBackupActivitySuccesss = activityUtils.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.INTERMEDIATE_SUCCESS);
        return (isDeleteBackupActivitySuccesss != null && !isDeleteBackupActivitySuccesss.isEmpty());
    }

    /**
     * Returns the current backup which is under deletion
     * 
     * @param activityJobId
     * @return
     */
    public String getBackupUnderDeletion(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final String backupUnderDeletion = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP);
        LOGGER.debug("Backup Under Deletion : {}", backupUnderDeletion);
        return backupUnderDeletion;
    }

    /**
     * This method maintains the current backup name, processed backups and total backups and adds them to the job property list.
     * 
     * @param backupDataList
     * @param activityJobAttributes
     * @param activityJobId
     * @return backName to be deleted
     */
    public String getBackupNameToBeProcessed(final List<String> backupDataList, final Map<String, Object> activityJobAttributes, final long activityJobId) {
        String backupToBeProcessed = "";
        if (backupDataList != null && !backupDataList.isEmpty()) {
            final int processedBackups = getCountOfProcessedBackups(activityJobAttributes);
            final String[] listOfBackups = backupDataList.toArray(new String[0]);
            try {
                backupToBeProcessed = listOfBackups[processedBackups];
                LOGGER.debug("processedBackups : {} , listOfBackups : {} , backupToBeProcessed : {}", processedBackups, listOfBackups, backupToBeProcessed);
            } catch (final Exception ex) {
                LOGGER.error("Exception occurred while fetching current backup name, Reason : ", ex);
            } finally {
                // Reason for these in finally is because in case of multiple backups to delete, even if one of them fails,
                // it should proceed to try to delete the next backup ( even though main job will fail with reason as intermediate failure).
                final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.PROCESSED_BACKUPS, Integer.toString(processedBackups + 1));
                activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.CURRENT_BACKUP, backupToBeProcessed);
                activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.TOTAL_BACKUPS, Integer.toString(listOfBackups.length));
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
            }
        }
        return backupToBeProcessed;
    }

}
