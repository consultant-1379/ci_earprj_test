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
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
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
 * This class facilitates the confirmation of upgrade package of CPP based node by invoking the UpgradePackage MO action(depending on the action type) that initializes the confirm activity.
 *
 * @author xcharoh , xvishsr
 */
@EServiceQualifier("CPP.UPGRADE.confirm")
@ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
@Stateless
@Profiled
@Traceable
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmService extends AbstractUpgradeActivity implements Activity, ActivityCallback, AsynchronousActivity {

    /**
     *
     */
    private static final String ACTIVITYNAME_CONFIRM = "confirm";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmService.class);

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    /**
     * This method validates the upgrade package to decide if confirm activity can be started or not and sends back the activity result to Work Flow Service.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside InstallService Precheck() with activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);

            activityStepResult = precheck(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured while processing precheck for Confirm activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
            if (!(exceptionMessage.isEmpty())) {
                jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, e.getMessage());
            }
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
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
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final String actionType = UpgradeActivityConstants.ACTION_CONFIRM_UPGRADE;
        boolean isActionPerformed;
        NEJobStaticData neJobStaticData = null;
        String nodeName = "";
        String neType = null;
        long neJobId = 0;
        long mainJobId = 0;
        String upMoFdn = null;
        String logMessage = "";
        int actionId = -1;
        String neJobBusinessKey = null;
        FdnNotificationSubject fdnNotificationSubject = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            neJobId = neJobStaticData.getNeJobId();
            mainJobId = neJobStaticData.getMainJobId();
            nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            LOGGER.debug("Got UpMoFdn {} for confirm upgrade activity {}", activityJobId, upMoFdn);
            fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            notificationRegistry.register(fdnNotificationSubject);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(SHMEvents.CONFIRM_SERVICE, CommandPhase.STARTED, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

            actionId = dpsWriterRetryProxy.performAction(upMoFdn, actionType, new HashMap<String, Object>(), dpsRetryPolicies.getDpsMoActionRetryPolicy());
            LOGGER.debug("Action Id for Confirm Upgrade activity {} is : {}", activityJobId, actionId);
            isActionPerformed = true;
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            activityUtils.addJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, "true", jobPropertyList);
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
        } catch (final Exception exception) {
            LOGGER.error("Unable to perform write operation in DB for upMoFdn:{} actionType :{} due to : {}", upMoFdn, actionType, exception.getMessage());
            isActionPerformed = false;
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.CONFIRM) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.CONFIRM) + String.format(JobLogConstants.FAILURE_REASON, exception.getMessage()), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        }
        if (isActionPerformed) {
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.ACTION_ID);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
            jobPropertyList.add(jobProperty);

            final Integer confirmActivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.name(), ACTIVITYNAME_CONFIRM);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.CONFIRM, confirmActivityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            logMessage = actionType + " action is initiated on UP MO having FDN: " + upMoFdn;
            LOGGER.debug("Inside ConfirmService.execute() : {} for activity {}", logMessage, activityJobId);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
            systemRecorder.recordCommand(SHMEvents.CONFIRM_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            activityUtils.recordEvent(SHMEvents.CONFIRM_EXECUTE, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        } else {
            logMessage = "Unable to start MO Action with action: " + actionType + " on UP MO having FDN: " + upMoFdn;
            LOGGER.error("Inside ConfirmService.execute() : {} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.CONFIRM_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            notificationRegistry.removeSubject(fdnNotificationSubject);

            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Failure
            systemRecorder.recordCommand(SHMEvents.CONFIRM_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

            //Persist Result as Failed in case of unable to trigger action.
            final Map<String, Object> result = new HashMap<String, Object>();
            result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
            result.put(ActivityConstants.JOB_PROP_VALUE, JobResult.FAILED.getJobResult());
            jobPropertyList.add(result);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            sendActivateToWFS(activityJobId, neJobBusinessKey, ActivityConstants.EXECUTE);
            return;
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }

    /**
     * This method processes the notifications by fetching notification subject and then validates the notification. It de-registers from notification if it founds activity is completed and notifies
     * to WorkFlow Service or else waits for another notification if the activity is still on-going.
     *
     * @param notification
     * @return void
     *
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered cpp upgrade confirm activity - processNotification with event type : {} ", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp upgrade confirm activity - Discarding non-AVC notification.");
            return;
        }

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        boolean isActionCompleted = false;
        long activityJobId = -1;
        NEJobStaticData neJobStaticData = null;
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        try {
            activityJobId = activityUtils.getActivityJobId(notificationSubject);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside Confirm Service processNotification with modifiedAttr : {}", modifiedAttr);

            final UpgradeProgressInformation progressHeader = getCurrentProgressHeader(modifiedAttr);
            final UpgradePackageState currentUpState = getCurrentUpState(modifiedAttr);
            final UpgradePackageState previousUpState = getPreviousUpState(modifiedAttr);
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);

            if (currentUpState != null && currentUpState != previousUpState) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.UP_MO_STATE, currentUpState.getStateMessage()), notificationTime, JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
            }

            if (progressHeader != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.UP_MO_PROGRESS_HEADER, progressHeader.getProgressMessage()), notificationTime,
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }

            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            jobLogList.clear();

            isActionCompleted = isActionCompleted(currentUpState, progressHeader);

            if (isActionCompleted) {
                final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
                final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
                final String nodeName = neJobStaticData.getNodeName();
                final String upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
                final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                notificationRegistry.removeSubject(fdnNotificationSubject);

                final JobResult jobResult = evaluateJobResult(currentUpState, progressHeader, activityJobId, jobLogList, jobPropertyList);

                activityUtils.recordEvent(SHMEvents.CONFIRM_PROCESS_NOTIFICATION, nodeName, upMoFdn,
                        "SHM:" + activityJobId + ":" + nodeName + ":" + ActivityConstants.CONFIRM + jobResult.getJobResult());

                final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                LOGGER.debug("Sending activate for confirm activity to wfs with activityJobId {} businessKey {} ", activityJobId, businessKey);
                if (isJobResultPersisted) {
                    activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.CONFIRM, null);
                }
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
            }
        } catch (final Exception exception) {
            final String errorMsg = "An exception occurred while processing notification for confirm upgrade activity. Exception is :";
            LOGGER.error(errorMsg, exception);
        }
    }

    /**
     * This method handles timeout scenario for Confirm Activity and checks the state on node to see if the action got failed or successfully executed.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("In handle timeout scenario for Confirm activity : {}", activityJobId);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            activityStepResult = processTimeout(activityJobId, jobLogList, jobPropertyList, neJobStaticData);
            activityStartTime = neJobStaticData.getActivityStartTime();
        } catch (final Exception exception) {
            final String errorMsg = "An exception occurred while handling timeout scenario for confirm upgrade activity with JobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, exception);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            String errorMessage = "";
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = exception.getMessage();
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM) + " Reason: " + errorMessage, new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (activityStartTime > 0) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return activityStepResult;
    }

    /**
     * @param activityJobId
     * @param activityStepResult
     * @param jobLogList
     * @param jobPropertyList
     * @param currentUpState
     * @param currentUpHeader
     * @param neJobStaticData
     */
    private ActivityStepResult processTimeout(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final NEJobStaticData neJobStaticData) {
        JobResult jobResult;
        String currentUpStateValue;
        String currentUpHeaderValue;
        UpgradePackageState currentUpState = null;
        UpgradeProgressInformation currentUpHeader = null;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final long neJobId = neJobStaticData.getNeJobId();
        final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();
        final String upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        notificationRegistry.removeSubject(fdnNotificationSubject);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_PROG_HEADER };
        final Map<String, Object> upAttributesMap = getUpMoData(activityJobId, attributeNames);

        if (upAttributesMap.containsKey(UpgradePackageMoConstants.UP_MO_STATE)) {
            currentUpStateValue = (String) upAttributesMap.get(UpgradePackageMoConstants.UP_MO_STATE);
            currentUpState = UpgradePackageState.getState(currentUpStateValue);
        }

        if (upAttributesMap.containsKey(UpgradePackageMoConstants.UP_MO_PROG_HEADER)) {
            currentUpHeaderValue = (String) upAttributesMap.get(UpgradePackageMoConstants.UP_MO_PROG_HEADER);
            currentUpHeader = UpgradeProgressInformation.getHeader(currentUpHeaderValue);
        }

        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, ActivityConstants.CONFIRM), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        final String logMessage = "Proceeding with timeout for confirm activity as no notifications received.";
        activityUtils.recordEvent(SHMEvents.CONFIRM_TIME_OUT, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + mainJobId + ":" + logMessage);

        jobResult = evaluateJobResult(currentUpState, currentUpHeader, activityJobId, jobLogList, jobPropertyList);

        if (jobResult == JobResult.SUCCESS) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return activityStepResult;
    }

    /**
     * Based on the state and progress header, this method determines the status of confirm activity after confirm action is triggered on the node.
     *
     * @param currentUpState
     * @param progressHeader
     * @param jobProperties
     * @return boolean
     */
    private static boolean isActionCompleted(final UpgradePackageState currentUpState, final UpgradeProgressInformation progressHeader) {
        boolean isActionCompleted = false;
        if (UpgradePackageState.UPGRADE_COMPLETED.equals(currentUpState) || UpgradeProgressInformation.EXECUTION_FAILED.equals(progressHeader)) {
            isActionCompleted = true;
        }
        LOGGER.debug("Is Confirm Action Completed : {}", isActionCompleted);
        return isActionCompleted;
    }

    public JobResult evaluateJobResult(final UpgradePackageState currentUpState, final UpgradeProgressInformation progressHeader, final long activityJobId, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList) {
        JobResult jobResult = JobResult.FAILED;
        String jobLogMessage = null;
        String logLevel = "INFO";

        if (UpgradeProgressInformation.EXECUTION_FAILED.equals(progressHeader)) {
            jobLogMessage = "Execution failed header found, assuming confirmation failed. Reason : " + "Confirm upgrade stopped in unexpected state - " + currentUpState.getStateMessage();
            logLevel = JobLogLevel.ERROR.toString();
        } else if (UpgradePackageState.UPGRADE_COMPLETED.equals(currentUpState)) {
            jobResult = JobResult.SUCCESS;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.CONFIRM);
            logLevel = JobLogLevel.INFO.toString();
        } else {
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM) + " Reason : Confirm Upgrade activity stopped in unexpected state - "
                    + currentUpState.getStateMessage();
            logLevel = JobLogLevel.ERROR.toString();
        }

        LOGGER.debug("Job Log Message {} Job Result for Confirm activity {}", jobLogMessage, jobResult);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);

        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
        result.put(ActivityConstants.JOB_PROP_VALUE, jobResult.getJobResult());
        jobPropertyList.add(result);
        return jobResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        try {
            LOGGER.debug("Inside ConfirmService cancel() with activityJobId:{}", activityJobId);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final long mainJobId = neJobStaticData.getMainJobId();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.logCancelledByUser(jobLogList, jobConfigurationServiceProxy.getMainJobAttributes(mainJobId), jobConfigurationServiceProxy.getNeJobAttributes(neJobId),
                    ActivityConstants.CONFIRM);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final Exception e) {
            LOGGER.error("Exception occured while cancel of a confirm activity for activityJobId {} Reason :{}", activityJobId, e);
        }
        return new ActivityStepResult();
    }

    /**
     * This method determines that whether UP MO is valid for Installation or not.
     *
     * @param activityJobId
     * @param nodeName
     * @param upgradePackageMoData
     * @param jobLogList
     * @param neJobId
     * @param neJobAttributes
     * @return ActivityStepResultEnum
     */
    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum checkValidity(final long activityJobId, final NEJobStaticData neJobStaticData, final Map<String, Object> upgradePackageMoData,
            final List<Map<String, Object>> jobLogList, final Map<String, Object> neJobAttributes) {

        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final UpgradePackageState upState = getUPState(upgradePackageMoData);
        final String stateMessage = upState.getStateMessage();
        final String nodeName = neJobStaticData.getNodeName();
        final long neJobId = neJobStaticData.getNeJobId();
        LOGGER.debug("In Confirm activity precheck, UP state & UP State Message for activity {} : {} {}", activityJobId, upState, stateMessage);

        if (UpgradePackageState.AWAITING_CONFIRMATION.equals(upState)) {

            final String upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.CONFIRM), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());

            final String logMessage = "Proceeding confirm Activity for " + activityJobId + " as " + stateMessage;
            LOGGER.debug(logMessage);
            activityUtils.recordEvent(SHMEvents.CONFIRM_PRECHECK, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);

        } else if (UpgradePackageState.UPGRADE_COMPLETED.equals(upState)) {

            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTIVITY_SKIP, ActivityConstants.CONFIRM, "the selected software package does not require explicit confirmation."), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            final String logMessage = "Confirm activity is skipped as the selected software package does not require explicit confirmation.";
            LOGGER.debug("Precheck message for confirm upgrade activity {} is {}", activityJobId, logMessage);
            activityUtils.recordEvent(SHMEvents.CONFIRM_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);

        } else {

            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CONFIRM, "Node is not able to confirm upgrade package. Current UP state is: " + upState), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            final Map<String, Object> administrativeData = (Map<String, Object>) upgradePackageMoData.get(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA);
            final String logMessage = "Confirm upgrade failed for " + activityJobId + " because upgrade package [ProductNumber="
                    + administrativeData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER) + ",ProductRevision="
                    + administrativeData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION) + "] is not in awaiting confirmation. Current UP state is: " + upState;
            LOGGER.error("Precheck message for confirm upgrade activity {} is {}", activityJobId, logMessage);
            activityUtils.recordEvent(SHMEvents.CONFIRM_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

        }
        return activityStepResultEnum;
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
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck(long)
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        LOGGER.debug("Inside InstallService asyncPrecheck() with activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITYNAME_CONFIRM);
            if (!isUserAuthorized) {
                activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.CONFIRM, activityStepResultEnum);
                return;
            }
            activityStepResult = precheck(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured while processing precheck for Confirm activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
            if (!(exceptionMessage.isEmpty())) {
                jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, e.getMessage());
            }
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.CONFIRM, activityStepResult.getActivityResultEnum());
    }

    private ActivityStepResult precheck(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        long activityStartTime = 0;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.CONFIRM), JobLogLevel.INFO.toString()));
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final long neJobId = neJobStaticData.getNeJobId();
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
        final String nodeName = neJobStaticData.getNodeName();
        final String treatAsInfo = activityUtils.isTreatAs(nodeName);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        final Map<String, Object> upgradePackageMoData = getUpMoData(activityJobId, attributeNames);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.CONFIRM), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());

        if (upgradePackageMoData.size() == 0) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.CONFIRM, UpgradeActivityConstants.UP_MO_TYPE), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());

            final String logMessage = "Unable to proceed Confirm Activity as MO doesn't exist.";
            LOGGER.error("Unable to proceed Confirm Activity for {} as MO doesn't exist.", activityJobId);
            activityUtils.recordEvent(SHMEvents.CONFIRM_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        } else {
            activityStepResultEnum = checkValidity(activityJobId, neJobStaticData, upgradePackageMoData, jobLogList, neJobAttributes);
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped.");
        }
        return activityStepResult;
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
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.CONFIRM, precheckTimeout), JobLogLevel.ERROR.toString()));
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
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            activityStepResult = processTimeout(activityJobId, jobLogList, jobPropertyList, neJobStaticData);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured in asyncHandleTimeout for Confirm activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.CONFIRM, exceptionMessage);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.CONFIRM, activityStepResult.getActivityResultEnum());

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into ConfirmService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.CONFIRM);
    }
}
