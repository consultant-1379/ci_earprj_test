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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.CmHeartbeatHandler;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@SuppressWarnings("PMD.TooManyFields")
@EServiceQualifier("ECIM.RESTORE.restorebackup")
@ActivityInfo(activityName = "restorebackup", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RestoreService implements Activity, ActivityCallback, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreService.class);
    private static final String BACKUP_FILE_NOT_EXIST = "Backup File \"%s\" doesn't exist on node to restore.";
    private static final String NOT_COMPLETED = "backup is not in completed state.";
    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private CancelBackupService cancelBackupService;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private CmHeartbeatHandler cmHeartbeatHandler;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.RESTORE_BACKUP);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred for activityJobId : {}. Exception is : {}", activityJobId, ex);
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, EcimBackupConstants.RESTORE_BACKUP, ex.getMessage());
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.RESTORE_BACKUP);
            if (!isUserAuthorized) {
                activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.RESTORE_BACKUP, activityStepResultEnum);
                return;
            }
            activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred for activityJobId : {}. Exception is : {}", activityJobId, ex);
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, EcimBackupConstants.RESTORE_BACKUP, ex.getMessage());
        }
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.RESTORE_BACKUP, activityStepResultEnum);
    }

    private ActivityStepResultEnum getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String jobLogMessage = null;
        long activityStartTime = 0;
        String nodeName = null;
        String backupName = null;
        String errorMessage = "";
        try {
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            backupName = ecimBackupInfo.getBackupName();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTIVITY_INITIATED, EcimBackupConstants.RESTORE_BACKUP) + String.format(JobLogConstants.BACKUP_NAME, ecimBackupInfo.getBackupName()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            if (treatAsInfo != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
            final String inputVersion = brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName);
            final String logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion);
            if (logMessage != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
            if (brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)) {
                jobLogMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, EcimBackupConstants.RESTORE_BACKUP, backupName);
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            } else {
                jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.RESTORE_BACKUP, NOT_COMPLETED);
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            }
            //prepare job logs and record event.
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.recordEvent(SHMEvents.ECIM_RESTORE_BACKUP_PRECHECK, nodeName, backupName, "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
        } catch (final MoNotFoundException moNotFoundException) {
            errorMessage = String.format(BACKUP_FILE_NOT_EXIST, backupName);
            LOGGER.error("MoNotFoundException occurred for activityJobId : {}. Exception is : {}", activityJobId, moNotFoundException);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            errorMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            LOGGER.error("UnsupportedFragmentException occurred for activityJobId : {}. Exception is : {}", activityJobId, unsupportedFragmentException);
        } catch (final Exception ex) {
            errorMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.RESTORE_BACKUP, ex.getMessage());
            LOGGER.error("Exception occurred for activityJobId : {}. Exception is : {}", activityJobId, ex);
        }
        if (!errorMessage.isEmpty()) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, errorMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.recordEvent(SHMEvents.ECIM_RESTORE_BACKUP_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, errorMessage));
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        }
        if (ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION == activityStepResultEnum || ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION == activityStepResultEnum) {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped.");
        } else {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        }
        //Persist job logs.
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResultEnum;
    }

    @Asynchronous
    @Override
    public void execute(final long activityJobId) {
        LOGGER.debug("Inside EcimRestoreService execute() with activityJobId {}", activityJobId);
        long activityStartTime = 0;
        String nodeName = "";
        String businessKey = "";
        long mainJobId = 0;
        String backupName = null;
        EcimBackupInfo ecimBackupInfo = null;
        String brmBackupMoFdn = "";
        boolean actionSuccessfullyTriggered = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String neType = null;
        String nodeFdn = null;
        List<Map<String, Object>> jobPropertyList = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            mainJobId = neJobStaticData.getMainJobId();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            nodeFdn = networkElement.getNeFdn();
            ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            backupName = ecimBackupInfo.getBackupName();
            brmBackupMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.subscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, RestoreService.class));
            brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupMoFdn, EcimBackupConstants.RESTORE_BACKUP);
            actionSuccessfullyTriggered = true;
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
        } catch (final MoNotFoundException e) {
            LOGGER.error("MoNotFoundException occurred. Exception is : {}", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(BACKUP_FILE_NOT_EXIST, backupName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            LOGGER.error("UnsupportedFragmentException occurred. Exception is : {}", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception e) {
            LOGGER.error("Exception occurred. Exception is : {}", e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage;
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.RESTORE_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimBackupConstants.RESTORE_BACKUP, e.getMessage());
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.recordEvent(SHMEvents.RESTORE_BACKUP_EXECUTE, nodeName, backupName, "SHM:" + activityJobId + ":" + nodeName + ":" + "Restore Backup Started");
        if (actionSuccessfullyTriggered) {
            LOGGER.debug("Restore Backup Activity is triggered on BrmBackup MO {} for backup file {} with activityJobId {}", brmBackupMoFdn, backupName, activityJobId);

            //Sending MediationTaskRequest to update heartbeat interval time on CmNodeHeartbeatSupervision MO to over come notification delays from mediation.
            if (nodeFdn != null) {
                final int cmHeartBeatInterval = pollingActivityConfiguration.getShmHeartBeatIntervalForEcim();
                cmHeartbeatHandler.sendHeartbeatIntervalChangeRequest(cmHeartBeatInterval, activityJobId, nodeFdn);
            } else {
                LOGGER.warn("Node FDN value returned null for the node:{} and activityJobId: {} in RestoreService ECIM ", nodeName, activityJobId);
            }

            final Integer restoreActivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.name(),
                    EcimBackupConstants.RESTORE_BACKUP);
            systemRecorder.recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, EcimBackupConstants.RESTORE_BACKUP, restoreActivityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } else {
            activityUtils.unSubscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, RestoreService.class));
            systemRecorder.recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
            jobPropertyList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            activityUtils.sendActivateToWFS(businessKey, null);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
    }

    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM -restore backup - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("ECIM - Restore backup - Discarding non-AVC notification.");
            return;
        }

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        String nodeName = "";
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        LOGGER.debug("modifiedAttributes in processNotification for activity {} : {}", activityJobId, modifiedAttributes);
        String brmBackupMoFdn = null;
        NEJobStaticData neJobStaticData = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            brmBackupMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes);
            final boolean isInvalidProgressReport = brmMoServiceRetryProxy.validateActionProgressReport(nodeName, progressReport, EcimBackupConstants.RESTORE_BACKUP);
            if (isInvalidProgressReport) {
                LOGGER.warn("Discarding invalid notification,for the activityJobId {} and modifiedAttributes", activityJobId, modifiedAttributes);
            } else if (cancelBackupService.isCancelActionTriggerred(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId))) {
                cancelBackupService.evaluateCancelProgress(progressReport, activityUtils.getActivityInfo(activityJobId, RestoreService.class), brmBackupMoFdn, neJobStaticData, "restore");
            } else {
                handleProgressReportState(jobPropertyList, jobLogList, neJobStaticData, progressReport, brmBackupMoFdn, activityJobId);
            }
        } catch (final Exception e) {
            LOGGER.error("Notification processing failed due to :: ", e);
        }

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Entered into handletimeout in Restore activity with activityJobId : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        long activityStartTime = 0;
        String nodeName = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, EcimBackupConstants.RESTORE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        JobResult jobResult = JobResult.FAILED;
        String brmBackupMoFdn = null;
        String logMessage = null;
        NEJobStaticData neJobStaticData = null;
        //Get BrmBackupDetails
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            brmBackupMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.unSubscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, RestoreService.class));
            if (cancelBackupService.isCancelActionTriggerred(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId))) {
                final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.BACKUP_CANCEL_ACTION,
                        ecimBackupInfo);
                return cancelBackupService.verifyCancelHandleTimeout(progressReport, activityJobId);
            }
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.RESTORE_BACKUP, ecimBackupInfo);
            if (progressReport != null) {
                jobResult = evaluateJobResult(progressReport, jobLogList);
                handleTimeoutStatus = jobResult == JobResult.SUCCESS ? ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS : ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
                logMessage = handleTimeoutStatus == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS ? String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimBackupConstants.RESTORE_BACKUP)
                        : String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.RESTORE_BACKUP);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            } else {
                handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.RESTORE_BACKUP);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (MoNotFoundException | UnsupportedFragmentException | JobDataNotFoundException e) {
            LOGGER.error("Unable to find the result in handleTimeout, due to :: ", e);
            logMessage = String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.RESTORE_BACKUP, e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            LOGGER.error("Unable to find the result in handleTimeout due to :: ", exception);
            logMessage = String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.RESTORE_BACKUP, exception.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        recordEvent(activityJobId, brmBackupMoFdn, logMessage, ActivityConstants.COMPLETED_THROUGH_TIMEOUT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        activityStepResult.setActivityResultEnum(handleTimeoutStatus);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        String nodeName = "";
        EcimBackupInfo ecimBackupInfo = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);

        } catch (final RetriableCommandException | IllegalArgumentException | MoNotFoundException | UnsupportedFragmentException | JobDataNotFoundException e) {
            LOGGER.error("Exception occured while cancel of Restore for node :  {} and Reason {}", nodeName, e);
        }
        return cancelBackupService.cancel(activityJobId, EcimBackupConstants.RESTORE_BACKUP, ecimBackupInfo);
    }

    private void handleProgressReportState(final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData,
            final AsyncActionProgress progressReport, final String brmBackupMoFdn, final long activityJobId) {
        final ActionStateType state = progressReport.getState();
        if (state == ActionStateType.RUNNING) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, (double) progressReport.getProgressPercentage());
            progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
        } else if (state == ActionStateType.FINISHED) {
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
            final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
            activityUtils.unSubscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, RestoreService.class));
            final JobResult jobResult = evaluateJobResult(progressReport, jobLogList);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
            final String logMessage = jobResult == JobResult.SUCCESS ? String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimBackupConstants.RESTORE_BACKUP)
                    : String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.RESTORE_BACKUP);
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            recordEvent(activityJobId, brmBackupMoFdn, logMessage, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS);
            final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
            if (isJobResultPersisted) {
                activityUtils.sendActivateToWFS(businessKey, null);
            }
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
        } else {
            LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
    }

    private JobResult evaluateJobResult(final AsyncActionProgress progressReport, final List<Map<String, Object>> jobLogList) {
        JobResult jobResult = JobResult.FAILED;
        if (isRestoreSuccessful(progressReport)) {
            jobResult = JobResult.SUCCESS;
        } else if (isRestoreFailed(progressReport)) {
            if (progressReport.getResultInfo() == null || progressReport.getResultInfo().isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.RESTORE_BACKUP, JobLogConstants.FAILURE_REASON_NOT_AVAILABLE),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.RESTORE_BACKUP, progressReport.getResultInfo()), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } else if (ActionResultType.NOT_AVAILABLE == progressReport.getResult()) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_RUNNING, EcimBackupConstants.RESTORE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return jobResult;
    }

    /**
     *
     * @param progressReport
     * @return
     */

    private boolean isRestoreSuccessful(final AsyncActionProgress progressReport) {
        if (ActionResultType.SUCCESS == progressReport.getResult()) {
            return true;
        }
        return false;
    }

    private boolean isRestoreFailed(final AsyncActionProgress progressReport) {
        return (ActionResultType.FAILURE == progressReport.getResult() ? true : false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        // TODO Auto-generated method stub
        return new ActivityStepResult();
    }

    private void recordEvent(final long activityJobId, final String brmBackupMoFdn, final String jobLogMessage, final String flow) {
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.RESTORE, EcimBackupConstants.RESTORE_BACKUP);
        activityUtils.recordEvent(eventName, brmBackupMoFdn, brmBackupMoFdn, "SHM:" + activityJobId + ":" + jobLogMessage + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }

    @Override
    public void precheckHandleTimeout(long activityJobId) {
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, EcimBackupConstants.RESTORE_BACKUP);

    }

    @Override
    public void asyncHandleTimeout(long activityJobId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void timeoutForAsyncHandleTimeout(long activityJobId) {
        // TODO Auto-generated method stub

    }
}
