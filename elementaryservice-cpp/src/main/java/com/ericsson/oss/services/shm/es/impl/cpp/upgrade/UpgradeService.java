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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
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
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * This class facilitates the upgrade of the upgrade package of CPP based node by invoking the UpgradePackage MO action(depending on the action type) that initializes the upgrade activity.
 * 
 * @author xcharoh
 */
@EServiceQualifier("CPP.UPGRADE.upgrade")
@ActivityInfo(activityName = "upgrade", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@SuppressWarnings("PMD.ExcessiveClassLength")
public class UpgradeService extends AbstractUpgradeActivity implements Activity, ActivityCompleteCallBack, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeService.class);

    private static final long UPGRADE_STATE_STABLE_TIME = 30 * 1000L;
    private static final String ACTIVITYNAME_UPGRADE = "upgrade";

    /**
     * PRODUCT_NUMBER and PRODUCT_REVISION can be read from the Job Properties inside the InstallServiceLocal, so passing as null.
     */
    private static final String PRODUCT_NUMBER = null;
    private static final String PRODUCT_REVISION = null;
    private static final String PRECHECK_FAILED = "Precheck for Upgrade Activity has failed because Upgrade Package";

    @Inject
    ActivityUtils activityutil;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private PollingActivityManager pollingActivityManager;

    /**
     * This method validates the upgrade package to decide if upgrade activity can be started or not and notifies Work Flow Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();

            initiateActivity(nodeName, jobLogList);
            activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the UpgradeService.asyncPrecheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.UPGRADE, exceptionMessage);
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
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
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final String logMessage = null;
        String nodeName = null;
        String upMoFdn = null;
        long mainJobId = 0;
        NEJobStaticData neJobStaticData = null;
        String neJobBusinessKey = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);

            nodeName = neJobStaticData.getNodeName();
            mainJobId = neJobStaticData.getMainJobId();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(mainJobId);
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITYNAME_UPGRADE);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.UPGRADE);
                return;
            }
            // Initializing activity job logs
            initiateActivity(nodeName, jobLogList);
            // Precheck
            final ActivityStepResultEnum activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
            jobLogList.clear();
            if (ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION == activityStepResultEnum) {
                final long neJobId = neJobStaticData.getNeJobId();
                final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
                upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
                activityutil.subscribeToMoNotifications(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                LOGGER.debug("Got UpMoFdn {} for activity {}", activityJobId, upMoFdn);
                if (StringUtils.isNotEmpty(upMoFdn)) {
                    performAction(activityJobId, upMoFdn, neJobStaticData, jobLogList);
                } else {
                    final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.UPGRADE)
                            + String.format(JobLogConstants.FAILURE_REASON, JobLogConstants.UP_MO_READ_FAILURE_MESSAGE);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    systemRecorder.recordCommand(SHMEvents.UPGRADE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, JobLogConstants.UP_MO_READ_FAILURE_MESSAGE,
                            activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
                    activityutil.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.UPGRADE);
                }
            } else {
                LOGGER.info("Precheck failed for Upgrade activity with the activityJobId: {} and nodeName: {}", activityJobId, nodeName);
                activityutil.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.UPGRADE);
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to perform {} action on the node {}, Reason:", ACTIVITYNAME_UPGRADE, nodeName, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.UPGRADE);
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.UPGRADE) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.recordEvent(SHMEvents.UPGRADE_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            systemRecorder.recordCommand(SHMEvents.UPGRADE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

            activityutil.unSubscribeToMoNotifications(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            activityutil.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.UPGRADE);
        }
    }

    private void performAction(final long activityJobId, final String upMoFdn, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) throws MoNotFoundException {

        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();

        LOGGER.debug("Got UpMoFdn {} for activity {}", activityJobId, upMoFdn);
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(mainJobId);
        final String actionType = getActionType(mainJobAttributes, nodeName, activityJobId);
        // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
        systemRecorder.recordCommand(SHMEvents.UPGRADE_SERVICE, CommandPhase.STARTED, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

        final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
        int actionId = -1;
        actionId = dpsWriterRetryProxy.performAction(upMoFdn, actionType, new HashMap<String, Object>(), dpsRetryPolicies.getDpsMoActionRetryPolicy());
        LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);

        final Integer upgradeActivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(networkElement.getNeType(), PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.name(),
                ACTIVITYNAME_UPGRADE);
        final String logMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.UPGRADE, upgradeActivityTimeout);

        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_ID, Integer.toString(actionId));
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));
        LOGGER.debug("{} for activity {} with UpMOFdn {}", logMessage, activityJobId, upMoFdn);

        activityUtils.recordEvent(SHMEvents.UPGRADE_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        systemRecorder.recordCommand(SHMEvents.UPGRADE_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upMoFdn,
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
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
        LOGGER.debug("Entered cpp upgrade activity - processNotification with event type : {} ", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp upgrade activity - Discarding non-AVC notification.");
            return;
        }

        try {
            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside Upgrade Service processNotification with modifiedAttr= {}", modifiedAttr);
            final UpgradeProgressInformation progressHeader = getCurrentProgressHeader(modifiedAttr);
            final UpgradePackageState currentUpState = getCurrentUpState(modifiedAttr);
            final UpgradePackageState previousUpState = getPreviousUpState(modifiedAttr);
            final List<Map<String, Object>> currentActionResult = getActionResult(modifiedAttr);

            final boolean isUpgradeCompleted = processAndReportActionResult(activityJobId, progressHeader, currentUpState, previousUpState, currentActionResult, neJobStaticData);
            if (isUpgradeCompleted) {
                final String upMoFdn = getUpMoFdn(activityJobId, neJobStaticData.getNeJobId(), jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId()));
                final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS, ActivityConstants.UPGRADE);
                updateActionCompletionStatus(activityJobId, upMoFdn, currentUpState, neJobStaticData, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS, logMessage);
            }
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while processing notification. Exception is :";
            LOGGER.error(errorMsg, e);
        }
    }

    private static boolean isUpgradeSucess(final UpgradePackageState currentUpState) {
        return currentUpState == UpgradePackageState.UPGRADE_COMPLETED || currentUpState == UpgradePackageState.AWAITING_CONFIRMATION;
    }

    private boolean isCancelExecuted(final UpgradePackageState currentUpState) {
        return currentUpState == UpgradePackageState.INSTALL_COMPLETED;
    }

    /**
     * This method handles timeout scenario for Upgrade Activity and checks the state on node to see if it is failed or success.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("In handle timeout with activity id {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            neJobStaticData.getNeJobId();
            activityStepResultEnum = processTimeout(activityJobId, jobLogList, jobPropertyList, neJobStaticData);
        } catch (final Exception e) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            final String errorMsg = "An exception occured while handlingTimeout for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.UPGRADE, exceptionMessage);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (neJobStaticData != null) {
            activityStartTime = neJobStaticData.getActivityStartTime();
        }
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        return activityUtils.getActivityStepResult(activityStepResultEnum);
    }

    private ActivityStepResultEnum processTimeout(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final NEJobStaticData neJobStaticData) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String upMoFdn;
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        upMoFdn = getUpMoFdn(activityJobId, neJobStaticData.getNeJobId(), jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId()));

        activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));

        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPGRADE, neJobStaticData.getNodeName());

        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.UPGRADE);
        activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.WARN.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        jobLogList.clear();

        final JobResult jobResult = evaluateJobResult(neJobStaticData, upMoFdn, activityJobId);
        String jobLogMessage = null;
        String logLevel = null;
        if (jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.UPGRADE);
            logLevel = JobLogLevel.INFO.toString();
        } else {
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.UPGRADE);
            logLevel = JobLogLevel.ERROR.toString();
        }
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult == null ? JobResult.FAILED.getJobResult() : jobResult.getJobResult(), jobPropertyList);
        activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, logLevel);
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITYNAME_UPGRADE);
        activityUtils.recordEvent(eventName, neJobStaticData.getNodeName(), upMoFdn,
                "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
        return activityStepResultEnum;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.info("Inside Upgrade Service cancel() with activityJobId :{}", activityJobId);
        NEJobStaticData neJobStaticData = null;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        String logMessage = "";
        String actionType = "";
        String upMoFdn = "";
        String nodeName = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final long mainJobId = neJobStaticData.getMainJobId();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            nodeName = neJobStaticData.getNodeName();
            final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(mainJobId);
            upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            actionType = UpgradeActivityConstants.ACTION_CANCEL_UPGRADE;
            activityUtils.logCancelledByUser(jobLogList, mainJobAttributes, neJobAttributes, ActivityConstants.UPGRADE);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            final Map<String, Object> actionArguments = new HashMap<>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

            int actionId = -1;
            actionId = dpsWriterRetryProxy.performAction(upMoFdn, actionType, actionArguments, dpsRetryPolicies.getDpsMoActionRetryPolicy());
            LOGGER.debug("actionId of the cancel performAction: {}", actionId);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
            activityUtils.addJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, "true", jobPropertyList);
            logMessage = "Cancel Upgrade action is successfully invoked on nodeName:" + nodeName;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.recordEvent(SHMEvents.UPGRADE_CANCEL, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPGRADE, neJobStaticData.getNodeName());
        } catch (final Exception e) {
            logMessage = String.format("Unable to start  \"%s\"  MO Action on UP MO having FDN:  \"%s\"   because  \"%s\" ", actionType, upMoFdn, e.getMessage());
            LOGGER.error(logMessage);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                activityUtils.addJobLog(String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, actionType, exceptionMessage), JobLogType.SYSTEM.toString(), jobLogList,
                        JobLogLevel.ERROR.toString());
            } else {
                activityUtils.addJobLog(String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, actionType, e.getMessage()), JobLogType.SYSTEM.toString(), jobLogList,
                        JobLogLevel.ERROR.toString());
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.recordEvent(SHMEvents.UPGRADE_CANCEL, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        }
        return activityStepResult;
    }

    /**
     * 
     * This method is responsible for deciding the action type for upgrade activity.
     * 
     * @param activityJobId
     * @return String
     * 
     */
    @SuppressWarnings("unchecked")
    private String getActionType(final Map<String, Object> mainJobAttributes, final String nodeName, final long activityJobId) {
        String actionType = null;
        String upgradeRebootValue = null;
        String neType = null;
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        try {
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
        } catch (final MoNotFoundException e) {
            LOGGER.error("Exception while fetching networkElement of node :  {} Reason: {}", nodeName, e);
        }
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.REBOOTNODEUPGRADE);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, PlatformTypeEnum.CPP.name());
        upgradeRebootValue = keyValueMap.get(UpgradeActivityConstants.REBOOTNODEUPGRADE);
        LOGGER.debug("REBOOTNODEUPGRADE {}", upgradeRebootValue);

        LOGGER.debug("Is reboot of node required for activity {} : {}", activityJobId, upgradeRebootValue);
        if (ActivityConstants.CHECK_TRUE.equalsIgnoreCase(upgradeRebootValue)) {
            actionType = UpgradeActivityConstants.ACTION_REBOOT_NODE_UPGRADE;
        } else {
            final boolean isUpgrade = getUpgradeType(activityJobId);
            if (isUpgrade) {
                actionType = UpgradeActivityConstants.ACTION_UPGRADE;
            } else {
                actionType = UpgradeActivityConstants.ACTION_UPDATE;
            }
        }
        LOGGER.debug("Action Type for activity {} : {}", activityJobId, actionType);
        return actionType;
    }

    /**
     * This method determines whether action type is upgrade or update.
     * 
     * @param activityJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean getUpgradeType(final long activityJobId) {
        final Map<String, Object> upPoMap = upgradePackageService.getUpPoData(activityJobId);
        LOGGER.debug("UpPoMap for activity {} : {}", activityJobId, upPoMap);
        final List<Map<String, Object>> activities = (List<Map<String, Object>>) upPoMap.get(UpgradeActivityConstants.UP_PO_ACTIVITIES);
        boolean isUpgrade = false;
        for (final Map<String, Object> activity : activities) {
            final String activityName = (String) activity.get(UpgradeActivityConstants.UP_PO_ACTIVITY_NAME);
            if (UpgradeActivityConstants.ACTION_UPGRADE.equalsIgnoreCase(activityName)) {
                isUpgrade = true;
            }
        }
        return isUpgrade;
    }

    @Asynchronous
    @Override
    public void onActionComplete(final long activityJobId) {

        LOGGER.debug("Entered onActionComplete with activity job id {}", activityJobId);
        JobResult jobResult = JobResult.FAILED;
        String upMoFdn = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            jobResult = evaluateJobResult(neJobStaticData, upMoFdn, activityJobId);
            if (jobResult == null) {
                jobResult = JobResult.FAILED;
            }
            activityUtils.recordEvent(SHMEvents.UPGRADE_ON_ACTION_COMPLETE, neJobStaticData.getNodeName(), upMoFdn, "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":"
                    + ActivityConstants.UPGRADE + jobResult);
            reportJobStateToWFS(neJobStaticData, jobResult, ActivityConstants.UPGRADE, activityJobId);
        } catch (final Exception e) {
            jobResult = JobResult.FAILED;
            final String errorMsg = "An exception occured while evaluating job result for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);

            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage = "";
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, ActivityConstants.UPGRADE, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, ActivityConstants.UPGRADE, e);
            }
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());

            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

            if (neJobStaticData != null) {
                reportJobStateToWFS(neJobStaticData, jobResult, ActivityConstants.UPGRADE, activityJobId);
            }
        }

    }

    /**
     * This method checks if the action invoked on the node is completed.
     * 
     * @param currentUpState
     * @param previousUpState
     * @param upgradeProgressInformation
     * @param currentActionResult
     * @param activityJobId
     * @return true if action invoked is completed on node, false if it is ongoing.
     */
    private boolean isActionCompleted(final UpgradePackageState currentUpState, final UpgradePackageState previousUpState, final UpgradeProgressInformation upgradeProgressInformation,
            final List<Map<String, Object>> currentActionResult, final long activityJobId, final NEJobStaticData neJobStaticData) {
        boolean isActionCompleted = false;
        LOGGER.debug("Previous UP state:{} != current UP state:{}", previousUpState, currentUpState);

        if (isUpgradeExecuting(currentUpState)) {
            LOGGER.debug("Upgrade is still executing");
        } else if (isUpgradeSucess(currentUpState)) {
            isActionCompleted = true;
        } else if (currentUpState == UpgradePackageState.INSTALL_COMPLETED && previousUpState == UpgradePackageState.UPGRADE_EXECUTING) {
            isActionCompleted = true;
        } else if (isFailureSeenInProgressInfo(upgradeProgressInformation)) {
            isActionCompleted = true;
        } else if (isActionResultNotified(currentActionResult, activityJobId, neJobStaticData)) {
            isActionCompleted = true;
        } else {
            LOGGER.debug("State change is not handled in notification parsing algorithm. Current up state: {}, Previous up state: {}", currentUpState, previousUpState);
        }

        return isActionCompleted;
    }

    /**
     * @param currentUpState
     * @return
     */
    private boolean isUpgradeExecuting(final UpgradePackageState currentUpState) {
        return currentUpState == UpgradePackageState.UPGRADE_EXECUTING || currentUpState == UpgradePackageState.VERIFICATION_EXECUTING;
    }

    /**
     * Evaluate job result based on various parameters
     * 
     * @param neJobStaticData
     * @param upMoFdn
     * @return JobResult
     */
    @SuppressWarnings("unchecked")
    private JobResult evaluateJobResult(final NEJobStaticData neJobStaticData, final String upMoFdn, final long activityJobId) {

        JobResult jobResult = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upgradePackageMoData = getUpMoAttributesByFdn(upMoFdn, attributeNames);
        final UpgradePackageState currentUpState = getUPState(upgradePackageMoData);

        if (isUpgradeSucess(currentUpState)) {
            jobResult = JobResult.SUCCESS;
        } else {

            final List<Map<String, Object>> actionResultDataList = (List<Map<String, Object>>) upgradePackageMoData.get(UpgradePackageMoConstants.UP_ACTION_RESULT);
            LOGGER.trace("Action Result data {} fetched for upMOFdn {}", actionResultDataList, upMoFdn);

            final Map<String, Object> mainActionResult = getMainActionResult(neJobStaticData, actionResultDataList, false, activityJobId);

            if (mainActionResult.isEmpty()) {
                final String jobLogMessage = JobLogConstants.MAIN_ACTION_RESULT_NOT_FOUND;
                activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
                LOGGER.warn("Unable to find main action result for activityJobId {}.", activityJobId);
                return jobResult;
            }

            String jobLogMessage = null;
            if (!processMainActionResult(activityJobId, mainActionResult)) {
                jobLogMessage = UpgradeActivityConstants.UPGRADE_INVOCATION_FAILED;
                jobResult = JobResult.FAILED;
            } else {

                final UpgradeProgressInformation failureHeader = getProgressInformation(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId));

                final boolean isUpgradeExecutingSeen = isUpgradeExecutingSeen(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId));

                if (failureHeader != null) {
                    LOGGER.debug("Failure header: {} for activityJobId: {}", failureHeader, activityJobId);
                    jobLogMessage = getJobLogMessage(failureHeader);
                    jobResult = JobResult.FAILED;
                } else if (currentUpState == UpgradePackageState.INSTALL_COMPLETED && isUpgradeExecutingSeen) {
                    jobLogMessage = UpgradeActivityConstants.UPGRADE_CANCELLED;
                } else if (!isUpgradeExecutingSeen) {
                    jobLogMessage = UpgradeActivityConstants.UPGRADE_EXECUTING_NOT_SEEN;
                    jobResult = JobResult.FAILED;
                } else {
                    jobLogMessage = String.format(UpgradeActivityConstants.UPGRADE_UNEXPECTED_UP_STATE, currentUpState.toString());
                    jobResult = JobResult.FAILED;
                }
            }

            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

        return jobResult;
    }

    /**
     * This method checks if main action result read from the node is conclusive for determining the job result. If ActionResultInformation == EXECUTION_FAILED main action result is conclusive no
     * further analysis is needed.
     * 
     * @param activityJobId
     * @param mainActionResult
     * @return isfurtherEvaluationNeeded
     */
    private boolean processMainActionResult(final long activityJobId, final Map<String, Object> mainActionResult) {

        boolean isExecutionFailed = true;
        if (!mainActionResult.isEmpty() && mainActionResult.get(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO) != null) {

            final ActionResultInformation actionResultInfo = (ActionResultInformation) mainActionResult.get(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO);

            if (actionResultInfo == ActionResultInformation.EXECUTION_FAILED) {
                isExecutionFailed = false;
                LOGGER.debug("Action result : EXECUTION_FAILED for activityJobId {}", activityJobId);
            }
        }
        return isExecutionFailed;
    }

    /**
     * This method retrieves UpgradeProgressInformation stored in activityJobAttributes
     * 
     * @param activityJobAttributes
     * @return true if UPGRADE_EXECUTING_SEEN state is seen in UpgradeActivity execution
     */
    private boolean isUpgradeExecutingSeen(final Map<String, Object> activityJobAttributes) {
        final String isUpgradeExecutingSeen = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.UPGRADE_EXECUTING_SEEN);
        return Boolean.parseBoolean(isUpgradeExecutingSeen);
    }

    /**
     * This method retrieves UpgradeProgressInformation stored in activityJobAttributes
     * 
     * @param activityJobAttributes
     * @return UpgradeProgressInformation stored in activityJobAttributes
     */
    private UpgradeProgressInformation getProgressInformation(final Map<String, Object> activityJobAttributes) {

        final String failureHeader = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.FAILURE_IN_UP_HEADER);
        return failureHeader != null && !failureHeader.isEmpty() ? UpgradeProgressInformation.valueOf(failureHeader) : null;
    }

    /**
     * Retrieves relevant jobLogMessage based on the given UpgradeProgressInformation
     * 
     * @param failureHeader
     * @return jobLogMessage
     */
    private String getJobLogMessage(final UpgradeProgressInformation failureHeader) {

        String jobLogMessage = "";

        switch (failureHeader) {
        case VERIFICATION_FAILED:
            jobLogMessage = UpgradeActivityConstants.VERIFICATION_FAILED_MSG;
            break;
        case EXECUTION_FAILED:
            jobLogMessage = UpgradeActivityConstants.EXECUTION_FAILED_MSG;
            break;
        case AUE_FAILURE:
            jobLogMessage = UpgradeActivityConstants.AUE_FAILURE;
            break;
        default:
            jobLogMessage = "Unexpected progressInformation seen while upgrading";
            break;
        }

        return jobLogMessage;
    }

    /**
     * Verifies the given UpgradeProgressInformation for failed information
     * 
     * @param upgradeProgressInformation
     * @return true if there is a failure
     */
    private static boolean isFailureSeenInProgressInfo(final UpgradeProgressInformation upgradeProgressInformation) {

        return upgradeProgressInformation == UpgradeProgressInformation.VERIFICATION_FAILED || upgradeProgressInformation == UpgradeProgressInformation.EXECUTION_FAILED
                || upgradeProgressInformation == UpgradeProgressInformation.AUE_FAILURE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean isCancelRetriesExhausted) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPGRADE, neJobStaticData.getNodeName());
            final long neJobId = neJobStaticData.getNeJobId();
            final String upMoFdn = getUpMoFdn(activityJobId, neJobId, jobConfigurationServiceProxy.getNeJobAttributes(neJobId));
            final JobResult jobResult = evaluateJobResult(neJobStaticData, upMoFdn, activityJobId);
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            setTimeoutResultForCancel(activityStepResult, jobResult, isCancelRetriesExhausted);

            if (isCancelRetriesExhausted) {
                final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, ActivityConstants.UPGRADE), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            }
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while handling cancel Timeout for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
        }
        return activityStepResult;
    }

    private static void setTimeoutResultForCancel(final ActivityStepResult activityStepResult, final JobResult jobResult, final boolean isCancelRetriesExhausted) {
        if (isCancelRetriesExhausted || jobResult == JobResult.FAILED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else if (jobResult == JobResult.SUCCESS || jobResult == JobResult.SKIPPED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        }

    }

    /**
     * @param activityJobId
     * @param currentActionResult
     * @return
     */
    private boolean isActionResultNotified(final List<Map<String, Object>> actionResultData, final long activityJobId, final NEJobStaticData neJobStaticData) {
        if (!actionResultData.isEmpty()) {
            for (final Map<String, Object> mainActionResult : actionResultData) {
                final int actionIdOnMo = getInvokedActionId(mainActionResult);
                final UpgradePackageInvokedAction typeOfInvokedAction = getInvokedAction(mainActionResult);
                LOGGER.debug("TypeOfInvokedAction is {}", typeOfInvokedAction);
                if (typeOfInvokedAction == UpgradePackageInvokedAction.UPGRADE && actionIdOnMo != -1) {
                    final int persistedActionId = activityUtils.getPersistedActionId(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId), neJobStaticData, activityJobId,
                            ActivityConstants.ACTION_ID);
                    if (persistedActionId == actionIdOnMo) {
                        return true;
                    }

                }

            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck (long)
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITYNAME_UPGRADE);
            if (!isUserAuthorized) {
                activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.UPGRADE, activityStepResultEnum);
                return;
            }
            initiateActivity(nodeName, jobLogList);
            activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the UpgradeService.asyncPrecheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.UPGRADE, exceptionMessage);
        }
        LOGGER.debug("UpgradeService.asyncPrecheck completed for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.UPGRADE, activityStepResultEnum);
    }

    private ActivityStepResultEnum getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String logMessage = "";
        try {
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());

            final String[] attributesToRead = { UpgradePackageMoConstants.UP_MO_STATE };
            final Map<String, Object> upgradePackageMoData = upgradePackageService.getUpMoData(activityJobId, attributesToRead, PRODUCT_NUMBER, PRODUCT_REVISION);
            if (upgradePackageMoData.size() != 0) {
                final UpgradePackageState upState = getUPState(upgradePackageMoData);
                LOGGER.debug("In Precheck UpState for activity {}: {}", activityJobId, upState);
                if (upState == null) {
                    logMessage = PRECHECK_FAILED + "state is null";
                } else if (upState.equals(UpgradePackageState.INSTALL_COMPLETED) || upState.equals(UpgradePackageState.UPGRADE_COMPLETED)) {
                    activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                    logMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.UPGRADE);
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
                } else {
                    logMessage = "Upgrade package is not ready for upgrade.Current UP MO state is :" + upState;
                }
            } else {
                logMessage = PRECHECK_FAILED + "MO does not exist";
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final Exception e) {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            LOGGER.error("An exception occured while processing precheck for Upgrade activity for activityJobId: {}. Exception is:", activityJobId, e);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.UPGRADE);
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED_WITH_REASON, ActivityConstants.UPGRADE, e.getMessage());
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResultEnum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Integer precheckTimeout = activityTimeoutsService.getPrecheckTimeoutAsInteger();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.UPGRADE, precheckTimeout), JobLogLevel.ERROR.toString()));
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout (long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        NEJobStaticData neJobStaticData = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            activityStepResultEnum = processTimeout(activityJobId, jobLogList, jobPropertyList, neJobStaticData);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured in asyncHandleTimeout for Upgrade activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.UPGRADE, exceptionMessage);
        }
        final boolean isJobResultPropertyPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isJobResultPropertyPersisted) {
            LOGGER.info("Inside asyncHandleTimeout(). Sending message to wfs for activityJob with PO Id:{}, activity:{}", activityJobId, ActivityConstants.UPGRADE);
            activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.UPGRADE, activityStepResultEnum);
        } else {
            LOGGER.error(
                    "ActivityJob attributes[jobProperties={},jobLogList={}] are not updated in Database for {} of activity[activityjob poId ={}]. Skipped to notify WFS as the job is already completed.",
                    jobPropertyList, jobLogList, ActivityConstants.UPGRADE, activityJobId);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity# timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into UpgradeService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.UPGRADE);
    }

    /**
     * @param activityJobId
     * @param upMoFdn
     * @param neJobStaticData
     * @param attributeData
     */
    private void processResponseAttributes(final long activityJobId, final String upMoFdn, final NEJobStaticData neJobStaticData, final Map<String, Object> responseAttributes) {
        if (!responseAttributes.isEmpty()) {
            final String header = (String) responseAttributes.get(UpgradePackageMoConstants.UP_MO_PROG_HEADER);
            final String state = (String) responseAttributes.get(UpgradePackageMoConstants.UP_MO_STATE);
            final UpgradePackageState currentUpState = UpgradePackageState.getState(state);
            final UpgradeProgressInformation progressHeader = UpgradeProgressInformation.getHeader(header);
            final List<Map<String, Object>> actionResultDataList = (List<Map<String, Object>>) responseAttributes.get(UpgradePackageMoConstants.UP_ACTION_RESULT);

            // In this case (polling), we cannot get previous UP state, So considering current UP state as previous UP state. Agreed by SHM CNA
            // https://confluence-nam.lmera.ericsson.se/display/EST/TORF-152359+Improve+activity+execution+in+a+job+to+handle+the+node+restart+scenario+more+efficiently.
            final boolean isUpgradeCompleted = processAndReportActionResult(activityJobId, progressHeader, currentUpState, currentUpState, actionResultDataList, neJobStaticData);
            if (isUpgradeCompleted) {
                final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING, ActivityConstants.UPGRADE);
                updateActionCompletionStatus(activityJobId, upMoFdn, currentUpState, neJobStaticData, ActivityConstants.COMPLETED_THROUGH_POLLING, logMessage);
            }
        }
    }

    private boolean processAndReportActionResult(final long activityJobId, final UpgradeProgressInformation progressHeader, final UpgradePackageState currentUpState,
            final UpgradePackageState previousUpState, final List<Map<String, Object>> currentActionResult, final NEJobStaticData neJobStaticData) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        reportUPProgress(jobLogList, progressHeader, currentUpState);

        // Verifying if progress header contains any failure.
        if (isFailureSeenInProgressInfo(progressHeader)) {
            activityUtils.addJobProperty(UpgradeActivityConstants.FAILURE_IN_UP_HEADER, progressHeader.toString(), jobPropertyList);
        }

        // Persisting upgrade executing seen for reporting job result when cancel of upgrade is invoked.
        if (currentUpState == UpgradePackageState.UPGRADE_EXECUTING) {
            final boolean isUpgradeExecutingSeen = true;
            activityUtils.addJobProperty(UpgradeActivityConstants.UPGRADE_EXECUTING_SEEN, String.valueOf(isUpgradeExecutingSeen), jobPropertyList);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        boolean isUpgradeCompleted = false;
        if (isActionCompleted(currentUpState, previousUpState, progressHeader, currentActionResult, activityJobId, neJobStaticData)) {
            isUpgradeCompleted = true;
            if (activityUtils.cancelTriggered(activityJobId) && isCancelExecuted(currentUpState)) {
                isUpgradeCompleted = false;
                activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.getJobResult(), jobPropertyList);
                activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_JOB_CANCELLED, ActivityConstants.UPGRADE), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                final String businessKey = neJobStaticData.getNeJobBusinessKey();
                sendCancelMOActionDoneToWFS(activityJobId, businessKey, ActivityConstants.UPGRADE);
            }
        }
        return isUpgradeCompleted;
    }

    private void updateActionCompletionStatus(final long activityJobId, final String upgradePackageMoFdn, final UpgradePackageState currentUpState, final NEJobStaticData neJobStaticData,
            final String completedThrough, final String logMessage) {
        LOGGER.debug("Action is completed for activityJobId:{}", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, jobActivityInfo);
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ActivityConstants.UPGRADE, neJobStaticData.getNodeName());
        // If up state = UPGRADE_COMPLETED or AWAITING_CONFIRMATION we can directly evaluate job result as success.
        if (isUpgradeSucess(currentUpState)) {
            logActivityCompletion(activityJobId, upgradePackageMoFdn, neJobStaticData, completedThrough, logMessage);
            reportJobStateToWFS(neJobStaticData, JobResult.SUCCESS, ActivityConstants.UPGRADE, activityJobId);
        } else {
            LOGGER.debug("Starting activityCompleteTimer for activityJobId:{}", activityJobId);
            activityCompleteTimer.startTimer(UPGRADE_STATE_STABLE_TIME, jobActivityInfo); // Waiting 30 seconds for the node to stabilize its state.
            LOGGER.debug("Timer Started for Upgrade Activity");
        }
        final long activityStartTime = neJobStaticData.getActivityStartTime();
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
    }

    private void logActivityCompletion(final long activityJobId, final String upgradePackageMoFdn, final NEJobStaticData neJobStaticData, final String completionFlow, final String logMessage) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ActivityConstants.UPGRADE);
        activityUtils.recordEvent(eventName, neJobStaticData.getNodeName(), upgradePackageMoFdn,
                "SHM:" + activityJobId + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, completionFlow));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, new ArrayList<Map<String, Object>>(), jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
    }

    @Override
    public void subscribeForPolling(final long activityJobId) {
        LOGGER.debug("SubscribeForPolling in UpgradeService for activityJobId {}", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        try {
            final boolean isDpsAvailable = isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
            if (isDpsAvailable) {
                final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
                final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
                final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(neJobStaticData.getNodeName());
                final String moFdn = getUpMoFdn(activityJobId, neJobStaticData.getNeJobId(), neJobAttributes);
                final List<String> moAttributes = Arrays.asList(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_PROG_HEADER);
                pollingActivityManager.subscribe(jobActivityInfo, networkElementData, null, moFdn, moAttributes);
                LOGGER.debug("Polling subscription started in UpgradeService with activityJobId {}", activityJobId);
            }
        } catch (final Exception ex) {
            LOGGER.error("UpgradeService-subscribeForPolling-Unable to subscribe for polling for activityJobId: {} .Reason:  ", activityJobId, ex);
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

    @SuppressWarnings("unchecked")
    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        LOGGER.debug("Entered into processPollingResponse in UpgradeService for activityJobId {} with response attributes :{}", activityJobId, responseAttributes);
        String jobLogMessage = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> modifiedAttributes = (Map<String, Object>) responseAttributes.get(ShmConstants.MO_ATTRIBUTES);
        final String upgradePackageMoFdn = (String) responseAttributes.get(ShmConstants.FDN);
        try {
            final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            if (!isActivityCompleted) {
                processResponseAttributes(activityJobId, upgradePackageMoFdn, neJobStaticData, modifiedAttributes);
            } else {
                LOGGER.debug("Found Upgrade activity result already persisted in ActivityJob PO, Assuming activity completed on the node for activityJobId: {} and FDN: {}.", activityJobId,
                        upgradePackageMoFdn);
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ACTIVITYNAME_UPGRADE, neJobStaticData.getNodeName());
            }
        } catch (final Exception e) {
            jobLogMessage = String.format(JobLogConstants.EXCEPTION_OCCURED, e.getMessage());
            LOGGER.error(jobLogMessage.concat(JobLogConstants.FAILUREREASON), e);
        }

        if (!jobLogMessage.isEmpty()) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
    }

    private void initiateActivity(final String nodeName, final List<Map<String, Object>> jobLogList) {

        final String treatAsInfo = activityUtils.isTreatAs(nodeName);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.UPGRADE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.UPGRADE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());

    }
}
