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

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentMainActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This class facilitates the download of CV as a part of Restore download.
 * 
 * @author xeeerrr
 * 
 */
@EServiceQualifier("CPP.RESTORE.download")
@ActivityInfo(activityName = "download", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DownloadCvService extends AbstractBackupActivity implements Activity, ActivityCallback, ActivityCompleteCallBack {

    private final static Logger LOGGER = LoggerFactory.getLogger(DownloadCvService.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    NotificationRegistry notificationRegistry;

    @Inject
    SmrsFileStoreService smrsServiceUtil;

    @Inject
    ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    RestorePrecheckHandler restorePrecheckHandler;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheck(long)
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        return restorePrecheckHandler.getRestorePrecheckResult(activityJobId, ActivityConstants.DOWNLOAD_CV, ActivityConstants.DOWNLOAD);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#execute(long)
     */
    @SuppressWarnings("PMD")
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        String cvMoFdn = null, key = null;
        String neType = null;
        boolean executedWithoutException = false;
        final JobResult activityResult = JobResult.FAILED;
        int actionId = -1;
        String jobExecutionUser = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final long mainJobId = jobEnvironment.getMainJobId();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(nodeName);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        key = BackupActivityConstants.CV_NAME;
        final String cvName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, key);
        if (moAttributesMap != null) {
            cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
        }
        try {
            jobExecutionUser = activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId());
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(nodeName));
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            LOGGER.error("Exception while fetching neType of node :  {}", nodeName);
        }
        if (cvMoFdn == null) {
            //Persist Result as Failed in case of unable to trigger action.
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, activityResult.getJobResult().toString());
            jobPropertyList.add(jobProperty);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
            return;
        }
        String logMessage;
        final SmrsAccountInfo smrsDetails = retrieveSmrsDetails(activityJobId, jobLogList, neType, cvName, nodeName);
        if (smrsDetails == null) {
            //Persist Result as Failed in case of unable to trigger action.
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> result = new HashMap<String, Object>();
            result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
            result.put(ActivityConstants.JOB_PROP_VALUE, activityResult.getJobResult().toString());
            LOGGER.error("Failed to retrieve SMRS details for activityJobId : {}", activityJobId);
            jobPropertyList.add(result);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
            return;
        }
        final String relativePathPathToSmrsRoot = smrsDetails.getRelativePathToSmrsRoot();
        final String pathForCvBackupOnFtpServer = relativePathPathToSmrsRoot + "/" + nodeName + "/" + cvName.concat(BackupActivityConstants.BACKUP_FILE_EXTENSION);
        final String ftpServerIpAddress = smrsDetails.getServerIpAddress();
        final String ftpServerUserId = smrsDetails.getUser();
        final String ftpServerPassword = new String(smrsDetails.getPassword());
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(cvMoFdn, activityJobId, jobActivityInfo);
        notificationRegistry.register(fdnNotificationSubject);

        final Map<String, Object> actionArguments = prepareActionArguments(pathForCvBackupOnFtpServer, ftpServerIpAddress, ftpServerUserId, ftpServerPassword);
        systemRecorder.recordCommand(jobExecutionUser, SHMEvents.DOWNLOAD_RESTORE_SERVICE, CommandPhase.STARTED, nodeName, cvMoFdn,
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
        try {
            actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_DOWNLOAD_CV, cvMoFdn, actionArguments);
            executedWithoutException = true;
            final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
        } catch (final Exception e) {
            LOGGER.error("Unable to start MO Action with action{}", e.getMessage());
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.DOWNLOAD_CV) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTION_TRIGGER_FAILED + JobLogConstants.FAILURE_DUE_TO_EXCEPTION + e.getMessage(), ActivityConstants.DOWNLOAD_CV), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
            logMessage = "Unable to start MO Action with action: " + BackupActivityConstants.ACTION_DOWNLOAD_CV + " on CV MO having FDN: " + cvMoFdn;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.DOWNLOAD_RESTORE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
            notificationRegistry.removeSubject(fdnNotificationSubject);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> result = new HashMap<String, Object>();
            result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
            result.put(ActivityConstants.JOB_PROP_VALUE, activityResult.getJobResult().toString());
            LOGGER.error(" Download has failed for CV : {} with activityJobId:{} on the Node:{}", cvMoFdn, activityJobId, nodeName);
            jobPropertyList.add(result);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
            return;
        }
        if (executedWithoutException) {
            LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.name(), BackupActivityConstants.DOWNLOAD_CV);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");

            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ADDING_CV, cvName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED_WITH_ID, ActivityConstants.DOWNLOAD_CV, BackupActivityConstants.ACTION_DOWNLOAD_CV, actionId, activityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.DOWNLOAD_RESTORE_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));

            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
            jobPropertyList.add(jobProperty);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        }

    }

    /**
     * This method prepares the action arguments required for performing the action.
     * 
     * @param cvName
     * @param pathOnFtpServer
     * @param cvBackupNameOnFtpServer
     * @param ftpServerIpAddress
     * @param ftpServerUserId
     * @param ftpServerPassword
     * @return
     */
    private Map<String, Object> prepareActionArguments(final String pathForCvBackupOnFtpServer, final String ftpServerIpAddress, final String ftpServerUserId, final String ftpServerPassword) {
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_PATH_FOR_CV_ON_FTP_SERVER, pathForCvBackupOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);

        return actionArguments;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification notification) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        LOGGER.debug("Entered processNotification  with event type : {} and notification subject : {}", notification.getNotificationEventType(), notification.getNotificationSubject());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp download activity - Discarding non-AVC notification.");
            return;
        }
        JobEnvironment jobEnvironment = null;
        String cvMoFdn = null;
        long activityJobId = 0;
        String nodeName = "";
        long mainJobId = 0L;
        String jobExecutionUser = "";
        try {
            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside {} processNotification with modifiedAttr= {}", activityType, modifiedAttr);
            activityJobId = activityUtils.getActivityJobId(notificationSubject);
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            nodeName = (null != jobEnvironment) ? jobEnvironment.getNodeName() : "";
            mainJobId = (jobEnvironment != null) ? jobEnvironment.getMainJobId() : 0;
            jobExecutionUser = activityUtils.getJobExecutionUser(mainJobId);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            configurationVersionUtils.reportNotification(configurationVersionUtils.getNewCvActivity(notification), jobLogList);
            cvMoFdn = activityUtils.getNotifiedFDN(notification);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            if (evaluateDownloadActivityStatus(notification)) {
                final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
                activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                activityCompleteTimer.startTimer(jobActivityInfo);
                LOGGER.debug("Activity wait timer is started for {} activity with activityJobId:{} ", activityType, activityJobId);
            }
            LOGGER.debug("Exiting from processNotification ");
        } catch (final Exception ex) {
            final String errorMsg = String.format("An exception occured while processing %s notification. Notification : %s, Exception is :", activityType, notification);
            activityUtils.recordEvent(jobExecutionUser, eventType, nodeName, cvMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, errorMsg));
            LOGGER.error(errorMsg, ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStepResult = handleTimeoutActivity(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeoutfor {} activity. Reason : {}", getActivityType(), jdnfEx);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside DownloadCvService cancel() with activityJobId:{}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.DOWNLOAD_CV);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.DOWNLOAD_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /**
     * This method is responsible for retrieving the SMRS details.
     * 
     * @param activityJobId
     * @param jobLogList
     * @param nodeName
     * @param cvName
     * @return SmrsDetails
     */
    private SmrsAccountInfo retrieveSmrsDetails(final long activityJobId, final List<Map<String, Object>> jobLogList, final String neType, final String cvName, final String nodeName) {
        SmrsAccountInfo smrsDetails = null;
        try {
            smrsDetails = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, nodeName);
        } catch (final Exception e) {
            LOGGER.error("Unable to download CV for activity {}, because:{}", activityJobId, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Unable to download the configuration version " + cvName + " as " + e.getMessage(), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

            return smrsDetails;
        }
        return smrsDetails;

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
        return ActivityConstants.DOWNLOAD_CV;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.DOWNLOAD_RESTORE_EXECUTE;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return cancelTimeoutActivity(activityJobId, finalizeResult);
    }

    /**
     * This will give currentMainActivity attribute status.
     * 
     * @param modifiedAttr
     * @return
     */
    private boolean evaluateDownloadActivityStatus(final Notification notification) {
        final CvActivity cvActivity = configurationVersionUtils.getNewCvActivity(notification);
        final CVCurrentMainActivity currentMainActivity = cvActivity.getMainActivity();
        if (currentMainActivity != null) {
            if (currentMainActivity == CVCurrentMainActivity.IDLE) {
                LOGGER.debug("Current Main Activity has completed");
                return true;
            }
        }
        return false;
    }
}
