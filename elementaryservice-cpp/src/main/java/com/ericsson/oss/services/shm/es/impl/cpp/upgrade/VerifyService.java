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

package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.*;

import java.util.ArrayList;
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
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
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
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * This class facilitates the verification of the installed upgrade package of CPP based node by invoking the UpgradePackage MO action(verifyUpgrade) that initializes the verify activity.
 * 
 * @author xcharoh
 */
@EServiceQualifier("CPP.UPGRADE.verify")
@ActivityInfo(activityName = "verify", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class VerifyService extends AbstractUpgradeActivity implements Activity, ActivityCallback, ActivityCompleteCallBack, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyService.class);
    private static final String ACTIVITYNAME_VERIFY = "verify";

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    /**
     * This method validates the upgrade package to decide if verify activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);

            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } catch (final Exception e) {
            LOGGER.error("Exception occured in precheck() of a verify for activityJobId {} Reason :{}", activityJobId, e);
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResult;
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action and sends back activity result to Work
     * Flow Service
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {

        LOGGER.debug("Entering VerifyService:execute for activity: {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final JobResult activityResult = JobResult.FAILED;
        final String actionType = UpgradeActivityConstants.ACTION_VERIFY_UPGRADE;
        NEJobStaticData neJobStaticData = null;
        String nodeName = "";
        long neJobId = 0;
        long mainJobId = 0;
        String upMoFdn = null;
        String neJobBusinessKey = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            neJobId = neJobStaticData.getNeJobId();
            mainJobId = neJobStaticData.getMainJobId();
            nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            LOGGER.debug("Got UpMoFdn {} for activity {}", activityJobId, upMoFdn);

            if (upMoFdn == null || "".equals(upMoFdn)) {
                LOGGER.debug("Going to create UpgradePackage MO in verify activity for activityJobId {}", activityJobId);
                try {
                    upMoFdn = createUpgradeMO(activityJobId);
                    LOGGER.info("UpgradePackage MO created in verify activity with FDN {} for activityJobId  : {}", activityJobId, upMoFdn);
                } catch (final Exception exception) {
                    LOGGER.error("Unable to start Verify activity for {} as creation of MO failed. due to:", activityJobId, exception);
                    final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
                    final String logEntry = appendMediationFailureReasonInJobLog(String.format(JobLogConstants.MO_CREATION_FAILED, ActivityConstants.VERIFY), exceptionMessage);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, logEntry, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logEntry);

                    //Persist Result as Failed in case of unable to trigger action.
                    final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                    final Map<String, Object> result = new HashMap<String, Object>();
                    result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
                    result.put(ActivityConstants.JOB_PROP_VALUE, activityResult.getJobResult().toString());
                    jobPropertyList.add(result);
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                    sendActivateToWFS(activityJobId, neJobBusinessKey, "execute");
                    return;
                }
                if (upMoFdn != null) {
                    persistNeJobProperty(activityJobId, upMoFdn, neJobId, neJobAttributes);
                }
            }
            final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            notificationRegistry.register(fdnNotificationSubject);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(SHMEvents.VERIFY_SERVICE, CommandPhase.STARTED, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
            performAction(activityJobId, nodeName, actionType, upMoFdn, fdnNotificationSubject, mainJobId, jobLogList, neJobBusinessKey);
            LOGGER.debug("Exiting VerifyService: execute for activity: {}", activityJobId);
        } catch (final Exception ex) {
            LOGGER.error("VerifyService.execute-Unable to trigger action. Reason: {}", ex);
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ACTIVITYNAME_VERIFY);
        }
    }

    /**
     * This method performs the action on the node and update the activity job property with action id
     * 
     * @param activityJobId
     * @param nodeName
     * @param actionType
     * @param activityStepResult
     * @param upMoFdn
     * @param fdnNotificationSubject
     * @param mainJobId
     * @param actionArguments
     */
    private void performAction(final long activityJobId, final String nodeName, final String actionType, final String upMoFdn, final FdnNotificationSubject fdnNotificationSubject,
            final long mainJobId, final List<Map<String, Object>> jobLogList, final String businessKey) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        boolean isActionPerformed;
        int actionId = -1;
        String logMessage = null;
        String neType = null;
        long activityStartTime = 0;
        try {
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            final Map<String, Object> actionArguments = new HashMap<String, Object>();
            actionId = dpsWriterRetryProxy.performAction(upMoFdn, actionType, actionArguments, dpsRetryPolicies.getDpsMoActionRetryPolicy());
            LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
            isActionPerformed = true;
            activityStartTime = ((Date) activityUtils.getActivityJobAttributes(activityJobId).get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        } catch (final Exception e) {
            LOGGER.error("Failed to perform {} of dpsWriter, because: ", actionType, e);
            isActionPerformed = false;
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.VERIFY) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            }
        }
        if (isActionPerformed) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
            jobPropertyList.add(jobProperty);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            final Integer verifyActivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.name(), ACTIVITYNAME_VERIFY);

            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.VERIFY, verifyActivityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            logMessage = "MO Action Initiated with action:" + actionType + " on UP MO having FDN: " + upMoFdn;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);

            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
            systemRecorder.recordCommand(SHMEvents.VERIFY_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
            activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

        } else {
            logMessage = "Unable to start MO Action with action: " + actionType + " on UP MO having FDN: " + upMoFdn;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);

            activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

            notificationRegistry.removeSubject(fdnNotificationSubject);

            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Failure
            systemRecorder.recordCommand(SHMEvents.VERIFY_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

            //Persist Result as Failed in case of unable to trigger action.

            final Map<String, Object> result = new HashMap<String, Object>();
            result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
            result.put(ActivityConstants.JOB_PROP_VALUE, JobResult.FAILED.getJobResult().toString());
            jobPropertyList.add(result);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

            sendActivateToWFS(activityJobId, businessKey, "execute");
            return;
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
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
        LOGGER.debug("Entered cpp upgrade verify activity - processNotification with event type : {} ", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp upgrade verify activity - Discarding non-AVC notification.");
            return;
        }

        LOGGER.debug("Entering VerifyRestoreService.processNotification() with notificationSubject:{}", notification.getNotificationSubject());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        boolean isActionCompleted = false;
        NEJobStaticData neJobStaticData = null;
        try {
            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            final long neJobId = neJobStaticData.getNeJobId();
            final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside Verify Service processNotification with modifiedAttr= {}", modifiedAttr);

            final UpgradeProgressInformation progressHeader = getCurrentProgressHeader(modifiedAttr);
            final UpgradePackageState currentUpState = getCurrentUpState(modifiedAttr);
            reportUPProgress(jobLogList, progressHeader, currentUpState);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

            final List<Map<String, Object>> actionResultData = getActionResult(modifiedAttr);
            isActionCompleted = isActionCompleted(UpgradePackageInvokedAction.VERIFY_UPGRADE, actionResultData, neJobStaticData, activityJobId);

            if (isActionCompleted) {
                LOGGER.debug("Action is completed for activity {} with activityJobId:{} node name {} . Starting wait timer", ActivityConstants.VERIFY, activityJobId, nodeName);
                final String upMoFdn = getUpMoFdn(activityJobId, neJobId, jobConfigurationServiceProxy.getNeJobAttributes(neJobId));
                final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, jobActivityInfo);
                notificationRegistry.removeSubject(fdnNotificationSubject);
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
                activityCompleteTimer.startTimer(jobActivityInfo);
                LOGGER.debug("Activity wait timer is started and exiting VerifyService.processNotification() with notificationSubject={},NodeName={}, activityJobId={}",
                        notification.getNotificationSubject(), nodeName, activityJobId);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            }
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while processing notification. Exception is :";
            LOGGER.error(errorMsg, e);
        }
    }

    /**
     * This method handles timeout scenario for Verify Activity and checks the state on node to see if it is failed or success.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Entering VerifyRestoreService.handleTimeout() for activityJobId={} ", activityJobId);

        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            activityStepResultEnum = processTimeout(activityJobId, jobLogList, jobPropertyList);
        } catch (final Exception e) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            final String errorMsg = "An exception occured while processing timeout for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String errorMessage = "";
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.VERIFY);
            if (!exceptionMessage.isEmpty()) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = e.getMessage();
            }
            jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return activityUtils.getActivityStepResult(activityStepResultEnum);
    }

    private ActivityStepResultEnum processTimeout(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList)
            throws JobDataNotFoundException {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        long activityStartTime = 0;
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.VERIFY);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        jobLogList.clear();
        final String nodeName = neJobStaticData.getNodeName();
        activityStartTime = neJobStaticData.getActivityStartTime();
        final long neJobId = neJobStaticData.getNeJobId();
        final String upMoFdn = getUpMoFdn(activityJobId, neJobId, jobConfigurationServiceProxy.getNeJobAttributes(neJobId));
        activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));

        activityUtils.recordEvent(SHMEvents.VERIFY_TIME_OUT, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + neJobId + ":" + logMessage);
        final JobResult jobResult = evaluateJobResult(neJobStaticData, upMoFdn, activityJobId);
        String jobLogMessage = null;
        String logLevel;
        if (jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.VERIFY);
            logLevel = JobLogLevel.INFO.toString();
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else {
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.VERIFY);
            logLevel = JobLogLevel.ERROR.toString();
        }
        activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, logLevel);
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        return activityStepResultEnum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside VerifyService cancel() with activityJobId:{}", activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final long mainJobId = neJobStaticData.getMainJobId();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.logCancelledByUser(jobLogList, jobConfigurationServiceProxy.getMainJobAttributes(mainJobId), jobConfigurationServiceProxy.getNeJobAttributes(neJobId),
                    ActivityConstants.VERIFY);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        } catch (final Exception e) {
            LOGGER.error("Exception occured while cancel of a verify for activityJobId {} Reason :{}", activityJobId, e);
        }

        return new ActivityStepResult();
    }

    /**
     * This method determines that whether UP MO is valid for Verification or not.
     * 
     * @param activityJobId
     * @param nodeName
     * @param upgradePackageMoData
     * @param jobLogList
     * @return status
     */
    @SuppressWarnings("unused")
    private ActivityStepResultEnum checkValidity(final long activityJobId, final String nodeName, final Map<String, Object> upgradePackageMoData, final List<Map<String, Object>> jobLogList,
            final long neJobId, final Map<String, Object> neJobAttributes) {
        final UpgradePackageState upState = getUPState(upgradePackageMoData);
        ActivityStepResultEnum activityStepResultEnum;
        LOGGER.debug("In Precheck UpState for activity {} : {}", activityJobId, upState);
        final String stateMessage = upState.getStateMessage();
        LOGGER.debug("In Precheck UpStateMessage for activity {} : {}", activityJobId, stateMessage);
        final String upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
        if (upState == UpgradePackageState.INSTALL_EXECUTING || upState == UpgradePackageState.AWAITING_CONFIRMATION || upState == UpgradePackageState.ONLY_DELETEABLE
                || upState == UpgradePackageState.UPGRADE_EXECUTING) {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.VERIFY, stateMessage), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());

            final String logMessage = "Unable to proceed verify activity because " + stateMessage;
            LOGGER.info("{} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.VERIFY_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        } else {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());

            final String logMessage = "Proceeding verify Activity as " + stateMessage;
            LOGGER.info("{} for activity with Id {}", logMessage, activityJobId);

            activityUtils.recordEvent(SHMEvents.VERIFY_PRECHECK, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        }

        return activityStepResultEnum;
    }

    @Asynchronous
    @Override
    public void onActionComplete(final long activityJobId) {

        LOGGER.debug("Entered onActionComplete with activity job id {}", activityJobId);
        JobResult jobResult = JobResult.FAILED;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            final String upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            final String nodeName = neJobStaticData.getNodeName();

            jobResult = evaluateJobResult(neJobStaticData, upMoFdn, activityJobId);

            activityUtils.recordEvent(SHMEvents.VERIFY_ON_ACTION_COMPLETE, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + ActivityConstants.VERIFY + jobResult.getJobResult());

            reportJobStateToWFS(neJobStaticData, jobResult, ActivityConstants.VERIFY, activityJobId);

        } catch (final Exception e) {
            jobResult = JobResult.FAILED;
            final String errorMsg = "An exception occured while evaluating job result for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);

            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage = "";
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, ActivityConstants.VERIFY, exceptionMessage);

            } else {
                jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, ActivityConstants.VERIFY, e);
            }
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());

            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

            if (neJobStaticData != null) {
                reportJobStateToWFS(neJobStaticData, jobResult, ActivityConstants.VERIFY, activityJobId);
            }
        }
    }

    public JobResult evaluateJobResult(final NEJobStaticData neJobStaticData, final String upMoFdn, final long activityJobId) {
        final JobResult jobResult = JobResult.FAILED;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        final List<Map<String, Object>> actionResultDataList = getActionResultList(upMoFdn);
        final Map<String, Object> mainActionResult = getMainActionResult(neJobStaticData, actionResultDataList, false, activityJobId);

        if (mainActionResult.isEmpty()) {
            final String jobLogMessage = JobLogConstants.MAIN_ACTION_RESULT_NOT_FOUND;
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            LOGGER.warn("Unable to find main action result for activityJobId {}.", activityJobId);
            return jobResult;
        }
        return processMainActionResult(mainActionResult, activityJobId);
    }

    private JobResult processMainActionResult(final Map<String, Object> mainActionResultDetails, final long activityJobId) {
        JobResult jobResult = JobResult.FAILED;
        String actionResult = null;

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActionResultInformation actionResultInfo = (ActionResultInformation) mainActionResultDetails.get(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO);
        final String actionResultAdditionalInfo = (String) mainActionResultDetails.get(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO);

        if (actionResultInfo != null) {

            actionResult = "ActionResultInfo: " + actionResultInfo.getInfoMessage() + "; ActionResultAdditionalInfo: " + actionResultAdditionalInfo;
            LOGGER.debug("ActionResult for activityJobId {} is {} ", activityJobId, actionResult);

            if (actionResultInfo == ActionResultInformation.EXECUTED || actionResultInfo == ActionResultInformation.EXECUTED_WITH_WARNINGS) {

                if (!isUpgradeNotPossible(actionResultAdditionalInfo)) {
                    jobResult = JobResult.SUCCESS;
                }
            }
        } else {
            jobResult = JobResult.FAILED;
            final String jobLogMessage = JobLogConstants.MAIN_ACTION_RESULT_NOT_FOUND;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            LOGGER.warn("Main action result not found for activityJobId {}.", activityJobId);
        }
        LOGGER.info("JobResult is {} for activityJobId {}", jobResult.getJobResult(), activityJobId);

        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return jobResult;

    }

    /**
     * This method determines the status of activity after action is triggered based on the action result
     * 
     * @param jobEnvironment
     * 
     * @param currentUpState
     * @param previousUpState
     * @param jobEnvironment
     * @return
     */
    private boolean isActionCompleted(final UpgradePackageInvokedAction invokedAction, final List<Map<String, Object>> actionResultDataList, final NEJobStaticData neJobStaticData,
            final long activityJobId) {
        boolean isActionCompleted = false;
        if (!actionResultDataList.isEmpty()) {
            final int persistedActionId = activityUtils.getPersistedActionId(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId), neJobStaticData, activityJobId,
                    ActivityConstants.ACTION_ID);
            for (final Map<String, Object> actionResultData : actionResultDataList) {
                final int actionId = getInvokedActionId(actionResultData);
                final UpgradePackageInvokedAction invokedActionOnUp = getInvokedAction(actionResultData);
                if (isActionIdPresentOnNode(actionId, persistedActionId) && invokedAction == invokedActionOnUp) {
                    isActionCompleted = true;
                    break;
                }
            }
        }
        return isActionCompleted;
    }

    private boolean isUpgradeNotPossible(final String actionResultAdditionalInfo) {
        return actionResultAdditionalInfo != null && actionResultAdditionalInfo.toLowerCase().contains(UpgradeActivityConstants.ACTION_RESULT_UPGRADE_NOT_POSSIBLE);
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#precheck(long)
     */

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        JobStaticData jobStaticData = null;
        NEJobStaticData neJobStaticData = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITYNAME_VERIFY);
            if (!isUserAuthorized) {
                activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY, ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } catch (final Exception e) {
            LOGGER.error("Exception occured while cancel of a verify for activityJobId {} Reason :{}", activityJobId, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.VERIFY, e.getMessage()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Integer precheckTimeout = activityTimeoutsService.getPrecheckTimeoutAsInteger();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.VERIFY, precheckTimeout), JobLogLevel.ERROR.toString()));
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout(long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        NEJobStaticData neJobStaticData = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            activityStepResultEnum = processTimeout(activityJobId, jobLogList, jobPropertyList);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured in asyncHandleTimeout for Verify activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.VERIFY, exceptionMessage);
        }
        final boolean isJobResultPropertyPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isJobResultPropertyPersisted) {
            LOGGER.info("Inside asyncHandleTimeout(). Sending message to wfs for activityJob with PO Id:{}, activity:{}", activityJobId, ActivityConstants.VERIFY);
            activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY, activityStepResultEnum);
        } else {
            LOGGER.error(
                    "ActivityJob attributes[jobProperties={},jobLogList={}] are not updated in Database for {} of activity[activityjob poId ={}]. Skipped to notify WFS as the job is already completed.",
                    jobPropertyList, jobLogList, ActivityConstants.VERIFY, activityJobId);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into VerifyService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.VERIFY);
    }

}
