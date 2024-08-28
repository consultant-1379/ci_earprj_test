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
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This elementary service class confirms the Restore Configuration Version Activity on node.
 * 
 * @author tcsvisr
 * 
 */
@EServiceQualifier("CPP.RESTORE.confirm")
@ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmRestoreService extends AbstractBackupActivity implements Activity, ActivityCallback, ActivityCompleteCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmRestoreService.class);

    @Inject
    ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    NotificationRegistry notificationRegistry;

    @Inject
    RestorePrecheckHandler restorePrecheckHandler;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    /**
     * This method performs the PreCheck for Confirm Restore activity that includes the check for availability of Configuration Version MO.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        return restorePrecheckHandler.getRestorePrecheckResult(activityJobId, ActivityConstants.CONFIRM_RESTORE_CV, ActivityConstants.CONFIRM);
    }

    /**
     * This method confirms the Restore action on the node.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Restore Confirm activity initiated with activity job ID {}", activityJobId);
        String neType = null;
        String configurationVersionMoFdn = null, key = null;
        String logMessage;
        boolean executedWithoutException = false;
        Integer actionId = -1;
        String jobExecutionUser = null;
        JobResult activityResult = JobResult.FAILED;
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        final long mainJobId = jobEnvironment.getMainJobId();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();

        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(nodeName); //Fetching MO Attributes
        key = BackupActivityConstants.CV_NAME;
        final String configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, key); //Fetching CV Name

        if (moAttributesMap != null) {
            configurationVersionMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
        }
        LOGGER.debug("Confirming restore for {}", configurationVersionName);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Confirming Restore of Configuration Version " + configurationVersionName + " on node.", new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(configurationVersionMoFdn, activityJobId, jobActivityInfo); //Registering for Notifications
        notificationRegistry.register(fdnNotificationSubject);
        try {
            jobExecutionUser = activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId());
            final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, configurationVersionMoFdn, actionArguments); //Executing Confirm Restore action.
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            LOGGER.debug("Confirm Restore action performed successfuly for activty {}", activityJobId);
            executedWithoutException = true;
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(nodeName));
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
            }
        } catch (final Exception e) {
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        "Unable to trigger Confirm Restore activity for CV:" + configurationVersionName + " on Node." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Unable to trigger Confirm Restore activity for CV:" + configurationVersionName + " on Node.", new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
            logMessage = "Unable to start MO Action with action: " + BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV + " on CV MO having FDN: " + configurationVersionMoFdn;
            LOGGER.error("{} for activity {} due to:{}", logMessage, activityJobId, e);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Failure
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CONFIRM_RESTORE_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, configurationVersionMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
            notificationRegistry.removeSubject(fdnNotificationSubject);
            activityResult = JobResult.FAILED;
        }
        if (executedWithoutException) {
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CONFIRM_RESTORE_EXECUTE, CommandPhase.STARTED, nodeName, configurationVersionMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
            LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();

            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.name(),
                    BackupActivityConstants.ACTION_CONFIRM);

            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ADDING_CV, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED_WITH_ID, ActivityConstants.CONFIRM_RESTORE_CV, BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, actionId, activityTimeout),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityResult = JobResult.SUCCESS;
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
            jobPropertyList.add(jobProperty);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        }

        // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
        systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CONFIRM_RESTORE_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, configurationVersionMoFdn,
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
        if (activityResult == JobResult.FAILED) {
            //Persist Result as Failed in case of unable to trigger action.
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> result = new HashMap<String, Object>();
            result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
            result.put(ActivityConstants.JOB_PROP_VALUE, activityResult.getJobResult().toString());
            LOGGER.error("Confirm Restore activity failed  with the activity ID : {}", activityJobId);
            jobPropertyList.add(result);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, "execute", null);
            return;
        }
    }

    /**
     * This method processes and validates the notifications by fetching notification subject. If action performed on the node is successful, this method will unregister the service to stop getting
     * further notifications.
     * 
     * @param notification
     * @return void
     * 
     */
    @Override
    public void processNotification(final Notification notification) {
        handleNotification(notification, SHMCapabilities.RESTORE_JOB_CAPABILITY);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStepResult = handleTimeoutActivity(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout for {} activity. Reason : {}", getActivityType(), jdnfEx);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        return activityStepResult;
    }

    /**
     * This method cancels the confirm restore activity.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside ConfirmRestoreService cancel() with activityJobId:{}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.CONFIRM_RESTORE_CV);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.CONFIRM_RESTORE_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack#onActionComplete(long)
     */
    @Override
    public void onActionComplete(final long activityJobId) {
        super.onActionComplete(activityJobId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.CONFIRM_RESTORE_CV;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.CONFIRM_RESTORE_EXECUTE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return cancelTimeoutActivity(activityJobId, finalizeResult);
    }
}
