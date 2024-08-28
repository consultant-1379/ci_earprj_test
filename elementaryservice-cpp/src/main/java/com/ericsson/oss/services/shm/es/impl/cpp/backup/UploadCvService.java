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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.EXECUTE_REPEAT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;

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
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.instrumentation.impl.SmrsFileSizeInstrumentationService;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This class facilitates the upload of configuration version of CPP based node by invoking the ConfigurationVersion MO action that initializes the upload activity.
 * 
 * @author tcsrohc
 * 
 */
@EServiceQualifier("CPP.BACKUP.exportcv")
@ActivityInfo(activityName = "exportcv", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class UploadCvService extends AbstractBackupActivity implements Activity, ActivityCallback, ActivityCompleteCallBack, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadCvService.class);

    private static final String SLASH = "/";

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    NotificationRegistry notificationRegistry;

    @Inject
    SmrsFileStoreService smrsServiceUtil;

    @Inject
    ResourceOperations resourceOperations;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    protected NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    private SmrsFileSizeInstrumentationService smrsFileSizeInstrumentationService;

    /**
     * This method validates CV MO is already in use or not to decide if upload activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStepResult.setActivityResultEnum(getPrecheckResponse(activityJobId, neJobStaticData, false).getActivityStepResultEnum());
        } catch (final JobDataNotFoundException jdnfEx) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.UPLOAD_CV, jdnfEx.getMessage()));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.UPLOAD_CV, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return activityStepResult;
    }

    private CppBackupPrecheckResponse getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final boolean isPrecheckAlreadyDone) {
        CppBackupPrecheckResponse precheckResponse = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> emptyMap = new HashMap<String, Object>();
        final String nodeName = neJobStaticData.getNodeName();
        final long neJobId = neJobStaticData.getNeJobId();
        try {
            if (!isPrecheckAlreadyDone) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.UPLOAD_CV), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            }
            final String cvMoFdn = getCVMoFdn(nodeName);
            if (cvMoFdn == null || cvMoFdn.isEmpty()) {
                LOGGER.error("No CV MO found for {} activity with activityJobId {} for node {}", getActivityType(), activityJobId, nodeName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.UPLOAD_CV, BackupActivityConstants.CV_MO_TYPE), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
                precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, emptyMap, emptyMap);
                return precheckResponse;
            }
            if (!isPrecheckAlreadyDone) {
                doPrecheckPostValidation(activityJobId, nodeName, cvMoFdn, jobPropertyList, jobLogList, neJobId, activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()));
            }
            final Map<String, Object> cvMoFdnMap = new HashMap<>();
            cvMoFdnMap.put(ShmConstants.FDN, cvMoFdn);
            precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, cvMoFdnMap, emptyMap);
        } catch (final Exception exception) {
            LOGGER.error("Exception occurred in precheck of {} activity having activityJobId {} for node {} due to : ", getActivityType(), activityJobId, nodeName, exception);
            final String exceptionMessage = ExceptionParser.getReason(exception);
            final String jobLogMsg = getActivityType() + " precheck failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMsg, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, emptyMap, emptyMap);
        }
        if (!isPrecheckAlreadyDone) {
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        return precheckResponse;
    }

    private void doPrecheckPostValidation(final long activityJobId, final String nodeName, final String cvMoFdn, final List<Map<String, Object>> jobPropertyList,
            final List<Map<String, Object>> jobLogList, final long neJobId, final String jobExecutionUser) {
        String logMessage, jobLogMessage;
        jobLogMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.UPLOAD_CV);
        logMessage = "Proceeding Upload CV.";
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        LOGGER.debug("{} for {} activity with activityJobId {}", logMessage, getActivityType(), activityJobId);
        activityUtils.recordEvent(jobExecutionUser, SHMEvents.UPLOAD_BACKUP_PRECHECK, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This performs the MO action and sends back activity result to Work Flow Service
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @SuppressWarnings({ "PMD.ExcessiveMethodLength", "unchecked" })
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Entering UploadCvService.execute for activity : {}", activityJobId);
        boolean isActionInvoked = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        Map<String, Object> activityJobAttributes = new HashMap<>();
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        String cvMoFdn = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.EXPORT_CV_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), getActivityType());
                return;
            }

            Integer actionId = -1;
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            activityJobAttributes = jobEnvironment.getActivityJobAttributes();
            final boolean isPrecheckAlreadyDone = activityUtils.isPrecheckDone((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
            final CppBackupPrecheckResponse uploadCvPrecheckResponse = getPrecheckResponse(activityJobId, neJobStaticData, isPrecheckAlreadyDone);
            if (uploadCvPrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                LOGGER.debug("Executing {} activity for activityJobId {} on node {}", getActivityType(), activityJobId, nodeName);
                final String cvNames = getConfigurationVersionName(jobEnvironment, BackupActivityConstants.UPLOAD_CV_NAME);
                final String cvName = getCvNameToBeProcessed(cvNames, activityJobAttributes, jobPropertyList);
                activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.CURRENT_BACKUP, cvName);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
                final String neType = networkElementRetrievalBean.getNeType(nodeName);
                final Map<String, Object> moAttributesMap = uploadCvPrecheckResponse.getCvMoAttributes();
                if (moAttributesMap != null && !moAttributesMap.isEmpty()) {
                    cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                }
                final SmrsAccountInfo smrsDetails = retrieveSmrsDetails(activityJobId, jobLogList, neType, nodeName);
                if (smrsDetails == null) {
                    LOGGER.error("Couldn't retrieve SMRS details. Failed to upload CV");
                    activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), getActivityType());
                    return;
                }
                final boolean isDirectoryExists = createDirectoryAndValidatePermissions(smrsDetails.getPathOnServer(), nodeName);
                final Map<String, Object> actionArguments = UploadCvService.prepareActionArgumentsWithSmrsDetails(cvName, nodeName, smrsDetails);
                if (isDirectoryExists) {
                    try {
                        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.STARTED, nodeName, cvMoFdn,
                                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
                        activityUtils.subscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                        actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_UPLOAD_CV, cvMoFdn, actionArguments, backupMoActionRetryPolicy.getDpsMoActionRetryPolicy());
                        isActionInvoked = true;
                        activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
                        final String existingStepDurations = (String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS);
                        if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                        }
                        final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.BACKUP.name(),
                                BackupActivityConstants.EXPORT_CV);
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ADDING_CV, cvName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                                String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED_WITH_ID, getActivityType(), BackupActivityConstants.ACTION_UPLOAD_CV, actionId, activityTimeout), new Date(),
                                JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));
                        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName,
                                cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
                    } catch (final Exception e) {
                        LOGGER.error("MO Action initialization failed. Reason :", e);
                        String message = NodeMediationServiceExceptionParser.getReason(e);
                        message = message.isEmpty() ? e.getMessage() : message;
                        String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, getActivityType());
                        if (message != null && !message.isEmpty()) {
                            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, getActivityType(), message);
                        }
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName,
                                cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
                        activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                        activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), getActivityType());
                    }
                    if (isActionInvoked) {
                        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_ID, Integer.toString(actionId));
                        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
                        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                    }
                } else {
                    final String logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.UPLOAD_CV, JobLogConstants.NO_DIRECTORY_FOUND);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), getActivityType());
                }
            } else {
                LOGGER.error("Precheck in execute failed for {} activity having activityJobId {} on node {} with result {}.", getActivityType(), activityJobId, nodeName,
                        uploadCvPrecheckResponse.getActivityStepResultEnum());
                activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), getActivityType());
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action. Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, getActivityType(), jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, getActivityType());
        } catch (final Exception exception) {
            final String exceptionMessage = ExceptionParser.getReason(exception);
            if (!isActionInvoked) {
                activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                LOGGER.error("Exception occurred while triggering Upload CV action on the node {} due to: ", nodeName, exception);
                String logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, getActivityType(), exceptionMessage);
                if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                    logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, getActivityType());
                }
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), getActivityType());
            } else {
                LOGGER.warn("Exception occurred in execute of Upload CV for node {} due to : {}", nodeName, exception);
            }
        }
    }

    private SmrsAccountInfo retrieveSmrsDetails(final long activityJobId, final List<Map<String, Object>> jobLogList, final String neType, final String nodeName) {
        LOGGER.debug("Entered into retrieveSmrsDetails() with nodeType {}, and activityJobId{}", neType, activityJobId);
        SmrsAccountInfo smrsDetails = null;
        try {
            smrsDetails = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, nodeName);
        } catch (final Exception e) {
            LOGGER.error("Unable to Upload CV for activity with Id : {} because : {}", activityJobId, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Unable to upload the configuration version due to exception:" + e.getMessage(), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            return smrsDetails;
        }
        return smrsDetails;
    }

    private boolean createDirectoryAndValidatePermissions(final String pathOnFtpServer, final String nodeName) {
        resourceOperations.createDirectory(pathOnFtpServer, nodeName);
        return resourceOperations.isDirectoryExistsWithWritePermissions(pathOnFtpServer, nodeName);
    }

    private static Map<String, Object> prepareActionArgumentsWithSmrsDetails(final String cvName, final String nodeName, final SmrsAccountInfo smrsDetails) {
        String relativePathToSmrsRoot = smrsDetails.getRelativePathToSmrsRoot();
        if (!relativePathToSmrsRoot.endsWith(SLASH)) {
            relativePathToSmrsRoot = relativePathToSmrsRoot + SLASH;
        }
        final String pathForNode = relativePathToSmrsRoot + nodeName;
        final String ftpServerPassword = new String(smrsDetails.getPassword());
        return UploadCvService.prepareActionArguments(cvName, pathForNode, smrsDetails.getServerIpAddress(), smrsDetails.getUser(), ftpServerPassword);
    }

    /**
     * This method processes the notifications by fetching the notification subject and validates the notification. It de-register from the notification as it founds activity is completed and notifies
     * to WorkFlowService or else it will wait for another notification.
     * 
     * @param notification
     * @return void
     * 
     */
    @Override
    public void processNotification(final Notification notification) {
        long activityJobId = 0;
        final String activityType = getActivityType();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        LOGGER.debug("Entered cpp backup activity {} - processNotification with event type : {} and notification subject : {}", activityType, notification.getNotificationEventType(),
                notification.getNotificationSubject());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp backup activity - Discarding non-AVC notification.");
            return;
        }
        try {
            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            activityJobId = activityUtils.getActivityJobId(notificationSubject);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside {} processNotification with modifiedAttr = {}", activityType, modifiedAttributes);
            configurationVersionUtils.reportNotification(configurationVersionUtils.getNewCvActivity(notification), jobLogList);
            final Map<String, Object> actionResultData = backupActionResultUtility.getActionResultData(modifiedAttributes);
            final String configurationVersionName = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME);
            final String cvMoFdn = activityUtils.getNotifiedFDN(notification);
            final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
            final boolean isActionCompleted = processActionResult(activityJobId, cvMoFdn, neJobStaticData, actionResultData, activityJobAttributes);
            if (isActionCompleted) {
                final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS.substring(0, JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS.length() - 1),
                        activityType) + " for " + configurationVersionName;
                logActivityCompletionFlow(neJobStaticData, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS, logMessage, jobLogList, activityJobId, cvMoFdn, configurationVersionName);
                persistResultAndNotifyWFS(activityJobId, jobLogList, neJobStaticData, activityJobAttributes, cvMoFdn, JobResult.SUCCESS);
                final String nodeName = neJobStaticData.getNodeName();
                smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(nodeName, null, configurationVersionName);
            } else {
                jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to process notification(s). Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final Exception ex) {
            LOGGER.error("An exception occured while processing {} notification. Notification :{}, Exception is : {}", activityType, notification, ex);
        }
    }

    /*
     * This method handles timeout scenario for UploadCvService Activity and checks the actionResult on node to see if it is failed or success.
     * 
     * @param activityJobId
     * 
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPLOAD_CV, neJobStaticData.getNodeName());
            activityStepResult = handleTimeoutActivity(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException jdnfEx) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            LOGGER.error("Unable to trigger timeout for {} activity having activityJobId {}. Reason : {}", getActivityType(), activityJobId, jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } catch (final Exception ex) {
            LOGGER.error("UploadService.handleTimeout-Unable to verify activity status for activityJobId: {} .Reason:  ", activityJobId, ex);
        }
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside UploadCvService cancel() with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityUtils.logCancelledByUser(jobLogList, neJobStaticData, ActivityConstants.UPLOAD_CV);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.UPLOAD_CV), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to cancel {} activity having activityJobId {} for node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in UploadCvService.cancel for node {} having activityJobId {}. Reason is : ", nodeName, activityJobId, ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
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
    private static Map<String, Object> prepareActionArguments(final String cvName, final String pathOnFtpServer, final String ftpServerIpAddress, final String ftpServerUserId,
            final String ftpServerPassword) {
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
        return actionArguments;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack# onActionComplete(long)
     */
    @Override
    public void onActionComplete(final long activityJobId) {
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            super.onActionComplete(activityJobId, neJobStaticData);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPLOAD_CV, neJobStaticData.getNodeName());
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
        } catch (final JobDataNotFoundException dataNotFoundfEx) {
            LOGGER.error("Unable to trigger onActionComplete for {} activity. Reason : ", ActivityConstants.BACKUP, dataNotFoundfEx);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final Exception ex) {
            LOGGER.error("UploadService.onActionComplete-Unable to verify activity status for activityJobId: {} .Reason:  ", activityJobId, ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity #getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.UPLOAD_CV;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity #getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.UPLOAD_PROCESS_NOTIFICATION;
    }

    @Override
    protected void persistResultAndNotifyWFS(final long activityJobId, final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData,
            final Map<String, Object> activityJobAttributes, final String cvMoFdn, final JobResult jobResult) {
        final String activityType = getActivityType();
        boolean isJobResultPersisted = false;
        final String eventType = getNotificationEventType();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final String businessKey = neJobStaticData.getNeJobBusinessKey();
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        try {
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventType, neJobStaticData.getNodeName(), cvMoFdn,
                    "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":" + activityType + jobResult);
            final String cvNames = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.UPLOAD_CV_NAME);
            final String[] listOfCvNames = cvNames.split(ActivityConstants.COMMA);
            final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(activityJobId, cvNames, jobResult, jobPropertyList);
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            final Double currentProgressPercentage = activityAndNEJobProgressPercentageCalculator.calculateActivityProgressPercentage(activityJobId, listOfCvNames.length, activityJobAttributes,
                    EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
            isJobResultPersisted = activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, currentProgressPercentage);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            LOGGER.debug("Sending activate to wfs with activityJobId {} businessKey {} ", activityJobId, businessKey);
            if (repeatRequired) {
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
            } else {
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
            }
        } catch (final JobDataNotFoundException e) {
            LOGGER.error("Persist Result And Notify WFS failed for node {} due to : {}", neJobStaticData.getNodeName(), e);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, getActivityType()) + String.format(JobLogConstants.FAILURE_REASON, e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        if (isJobResultPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, processVariables);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isAllBackupsProcessed(final long activityJobId, final String cvNames) {
        final String[] listOfCvNames = cvNames.split(ActivityConstants.COMMA);
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId).get(ShmConstants.JOBPROPERTIES);
        final int processedBackups = getCountOfProcessedBackups(activityJobPropertyList);
        LOGGER.info("Inside UploadCvService, checking isAllBackupsProcessed : listOfCvNames {}, processedBackups {}, activityJobPropertyList {}", listOfCvNames, processedBackups,
                activityJobPropertyList);
        if (listOfCvNames.length == processedBackups) {
            LOGGER.info("All Backups Processed");
            return true;
        }
        LOGGER.info("All Backups Not Processed");
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isAnyIntermediateFailureHappened(final long activityJobId) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId).get(ShmConstants.JOBPROPERTIES);
        LOGGER.info("isAnyIntermediateFailureHappened activityJobPropertyList : {}", activityJobPropertyList);
        for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
            if (BackupActivityConstants.INTERMEDIATE_FAILURE.equals(eachJobProperty.get(ShmConstants.KEY))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void setTimeoutResult(final long activityJobId, final NEJobStaticData neJobStaticData, final ActivityStepResult activityStepResult, final JobResult moActionResult,
            final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        try {
            final String cvNames = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.UPLOAD_CV_NAME);
            final String[] listOfCvNames = cvNames.split(ActivityConstants.COMMA);
            final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
            final Double currentProgressPercentage = activityAndNEJobProgressPercentageCalculator.calculateActivityProgressPercentage(activityJobId, listOfCvNames.length, activityJobAttributes,
                    EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
            LOGGER.debug("listOfCvNames {} and currentProgressPercentage {} ", listOfCvNames.length, currentProgressPercentage);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, currentProgressPercentage);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(activityJobId, cvNames, moActionResult, jobPropertyList);
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            if (repeatRequired) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
            } else {
                final JobResult activityResult = (JobResult) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
                if (activityResult == JobResult.SUCCESS) {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                } else {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                }
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
            }
        } catch (final JobDataNotFoundException e) {
            LOGGER.error("Set time out failed for node {} due to : {}", neJobStaticData.getNodeName(), e);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, getActivityType()) + String.format(JobLogConstants.FAILURE_REASON, e.getMessage());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        }
    }

    private Map<String, Object> evaluateRepeatRequiredAndActivityResult(final long activityJobId, final String cvNames, final JobResult moActionResult,
            final List<Map<String, Object>> jobPropertyList) {
        LOGGER.debug("Evaluate whether repeat is Required and activity result. moActionResult {}, jobPropertyList {}", moActionResult, jobPropertyList);
        boolean recentUploadFailed = false;
        boolean repeatExecute = true;
        JobResult activityJobResult = null;
        if (moActionResult == JobResult.FAILED) {
            recentUploadFailed = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        }
        final boolean allBackupsProcessed = isAllBackupsProcessed(activityJobId, cvNames);
        if (allBackupsProcessed) {
            final boolean intermediateFailureHappened = isAnyIntermediateFailureHappened(activityJobId);
            if (intermediateFailureHappened || recentUploadFailed) {
                activityJobResult = JobResult.FAILED;
            } else {
                activityJobResult = JobResult.SUCCESS;
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
            repeatExecute = false;
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            activityJobResult = JobResult.CANCELLED;
            repeatExecute = false;
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        map.put(ActivityConstants.ACTIVITY_RESULT, activityJobResult);
        LOGGER.debug("Is Repeat Required or ActivityResult evaluated : {}", map);
        return map;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.info("Entered into cancelTimeout of CPP UploadCvService with activityJobId : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPLOAD_CV, neJobStaticData.getNodeName());
            activityStepResult.setActivityResultEnum(cancelTimeoutActivity(activityJobId, neJobStaticData, finalizeResult).getActivityResultEnum());
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout. Reason : {}", jdnfEx.getMessage());
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        return activityStepResult;
    }

    @Override
    protected void setTimeoutResultForCancel(final long activityJobId, final NEJobStaticData neJobStaticData, final ActivityStepResult activityStepResult, final JobResult moActionResult,
            final boolean finalizeResult) {
        if (moActionResult != null) {
            LOGGER.info("Inside setTimeoutResultForCancel : moActionResult {}", moActionResult.name());
            try {
                final String cvNames = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.UPLOAD_CV_NAME);
                final boolean allBackupsProcessed = isAllBackupsProcessed(activityJobId, cvNames);
                if (allBackupsProcessed) {
                    final boolean anyIntermediateFailure = isAnyIntermediateFailureHappened(activityJobId);
                    if (anyIntermediateFailure || moActionResult == JobResult.FAILED) {
                        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                    } else if (moActionResult == JobResult.SUCCESS || moActionResult == JobResult.SKIPPED) {
                        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                    }
                } else {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                }
            } catch (final JobDataNotFoundException e) {
                LOGGER.error("Set time out Result for Cancel failed for node {} due to : {}", neJobStaticData.getNodeName(), e);
                final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, getActivityType()) + String.format(JobLogConstants.FAILURE_REASON, e.getMessage());
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                final List<Map<String, Object>> jobLogList = new ArrayList<>();
                final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            }
        } else {
            if (finalizeResult) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck (long)
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        // Not needed if there is no node read calls in precheck or async threads are still not increased.
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity# precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // Implement this when (if) precheck is made async.
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout (long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        long neJobId = 0;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobId = neJobStaticData.getNeJobId();
            activityStepResultEnum = handleTimeoutActivity(activityJobId, neJobStaticData).getActivityResultEnum();
            if (activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
                final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
                final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
                final String cvNames = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.UPLOAD_CV_NAME);
                final String cvName = getProcessedCvName(cvNames, activityJobAttributes);
                smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(nodeName, null, cvName);
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout for {} activity having activityJobId {} on node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        LOGGER.info("Sending back ActivityStepResult to WorkFlow from CPP UploadCvService.asyncHandleTimeout with result : {} for node {} with activityJobId {} and neJobId {}", activityStepResultEnum,
                nodeName, activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.UPLOAD_CV, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity# timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Failing the activity and sending back to WorkFlow from CPP UploadCvService.timeoutForAsyncHandleTimeout for activityJobId : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.UPLOAD_CV);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        LOGGER.info("Entered into processPollingResponse in UploadCvService for activityJobId {} with response attributes :{}", activityJobId, responseAttributes);
        final String activityType = getActivityType();
        try {
            final Map<String, Object> modifiedAttributes = (Map<String, Object>) responseAttributes.get(ShmConstants.MO_ATTRIBUTES);
            final String cvMoFdn = (String) responseAttributes.get(ShmConstants.FDN);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
            if (!isActivityCompleted) {
                processResponseAttributes(activityJobId, modifiedAttributes, cvMoFdn, neJobStaticData);
            } else {
                LOGGER.debug("Found UploadCv activity result already persisted in ActivityJob PO, Assuming activity completed on the node for activityJobId: {} and FDN: {}.", activityJobId, cvMoFdn);
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityType, neJobStaticData.getNodeName());
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to process polling response. Reason : {}", jdnfEx);
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final Exception ex) {
            LOGGER.error("An exception occured while processing {} polling response :{}, Exception is : {}", activityType, responseAttributes, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void processResponseAttributes(final long activityJobId, final Map<String, Object> modifiedAttributes, final String cvMoFdn, final NEJobStaticData neJobStaticData) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        configurationVersionUtils.reportNotification(configurationVersionUtils.getNewCvActivity(modifiedAttributes), jobLogList);
        final Map<String, Object> actionResultData = (Map<String, Object>) modifiedAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
        final String configurationVersionName = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME);
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);

        final boolean isActionCompleted = processActionResult(activityJobId, cvMoFdn, neJobStaticData, actionResultData, activityJobAttributes);
        if (isActionCompleted) {
            final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING.substring(0, JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING.length() - 1),
                    ActivityConstants.UPLOAD_CV) + " for " + configurationVersionName;
            logActivityCompletionFlow(neJobStaticData, ActivityConstants.COMPLETED_THROUGH_POLLING, logMessage, jobLogList, activityJobId, cvMoFdn, configurationVersionName);
            persistResultAndNotifyWFS(activityJobId, jobLogList, neJobStaticData, activityJobAttributes, cvMoFdn, JobResult.SUCCESS);
            final String nodeName = neJobStaticData.getNodeName();
            smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(nodeName, null, configurationVersionName);
        } else {
            jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
        }
    }

    private boolean processActionResult(final long activityJobId, final String cvMoFdn, final NEJobStaticData neJobStaticData, final Map<String, Object> actionResultData,
            final Map<String, Object> activityJobAttributes) {
        boolean isActionCompleted = false;
        final String activityType = getActivityType();
        if (backupActionResultUtility.isActionResultNotified(actionResultData)) {
            activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            if (isJobResultSuccess(activityJobId, actionResultData, neJobStaticData, activityJobAttributes)) {
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityType, neJobStaticData.getNodeName());
                isActionCompleted = true;
            } else {
                final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
                activityCompleteTimer.startTimer(jobActivityInfo);
                LOGGER.debug("Activity wait timer is started for {} activity with activityJobId:{}", activityType, activityJobId);
            }
            LOGGER.debug("Exiting {} processPollingResponse() for NodeName = {}", activityType, neJobStaticData.getNodeName());
        }
        return isActionCompleted;
    }

    /**
     * @param ecimUpgradeInfo
     * @param completionFlow
     * @param logMessage
     * @param jobLogList
     * @param activityJobId
     */
    private void logActivityCompletionFlow(final NEJobStaticData neJobStaticData, final String completionFlow, final String logMessage, final List<Map<String, Object>> jobLogList,
            final long activityJobId, final String cvMoFdn, final String configurationVersionName) {
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.BACKUP, ActivityConstants.UPLOAD);
        activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventName, neJobStaticData.getNodeName(), cvMoFdn,
                "SHM:" + activityJobId + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, completionFlow));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FOR_CV_COMPLETED_SUCCESSFULLY, ActivityConstants.UPLOAD_CV, configurationVersionName), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    @Override
    public void subscribeForPolling(final long activityJobId) {
        LOGGER.debug("SubscribeForPolling in UploadService for activityJobId {}", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        try {
            final boolean isDpsAvailable = isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
            if (isDpsAvailable) {
                final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
                final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(neJobStaticData.getNodeName());
                final String moFdn = getCVMoFdn(neJobStaticData.getNodeName());
                if (moFdn != null) {
                    final List<String> moAttributes = Arrays.asList(ConfigurationVersionMoConstants.ACTION_RESULT, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY);
                    pollingActivityManager.subscribe(jobActivityInfo, networkElementData, null, moFdn, moAttributes);
                    LOGGER.debug("Polling subscription started in UploadService with activityJobId {}", activityJobId);
                } else {
                    LOGGER.info("Unable to subscribe for polling for activityJobId: {} as MoFdn is null", activityJobId);
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("UploadService.subscribeForPolling-Unable to subscribe for polling for activityJobId: {} .Reason:  ", activityJobId, ex);
            isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
        }
    }

    private boolean isDpsAvailable(final boolean isDataBaseAvaialble, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        if (!isDataBaseAvaialble) {
            LOGGER.info("DPS service is not available, so adding polling entry into cache for the activity: {} with activityJobId: {}", jobActivityInfo.getActivityName(), activityJobId);
            pollingActivityManager.prepareAndAddPollingActivityDataToCache(activityJobId, jobActivityInfo);
            return false;
        }
        return true;
    }

}
