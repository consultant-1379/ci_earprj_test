/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.common.CmHeartbeatHandler;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.es.upgrade.api.ActivityResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivateState;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.UpgradePackageState;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class ActivateServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateServiceHelper.class);

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private CmHeartbeatHandler cmHeartbeatHandler;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private EcimSwMUtils ecimSwMUtils;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private PollingActivityManager pollingActivityManager;

    public static final String ACTIVATE_ACTIVITY_NAME = "activate";

    public ActivityResult handleNotificationForActivateAction(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final ActivateState upgradePackageState,
            final Date notificationTime, final String upgradePackageMoFdn)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        LOGGER.debug("Inside Activate Service handleNotificationForActivate with upgradePackageMoFdn : {}", upgradePackageMoFdn);
        final ActivityResult activityResult = processValidNotification(progressReport, ecimUpgradeInfo, notificationTime, upgradePackageMoFdn);
        return processValidUpStateNotification(ecimUpgradeInfo, upgradePackageMoFdn, upgradePackageState, activityResult);
    }

    public ActivityResult processValidNotification(final AsyncActionProgress progressReport, final EcimUpgradeInfo ecimUpgradeInfo, final Date notificationTime, final String upgradePackageMoFdn)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        LOGGER.debug("Entered into processValidNotification() method with UpMoFdn : {} ", upgradePackageMoFdn);
        ActivityResult activityResult = new ActivityResult();
        if (progressReport == null) {
            return activityResult;
        }
        final ActionStateType state = progressReport.getState();
        if (state == null) {
            LOGGER.warn("Discarding as invalid notification as it doesn't contain state in progress report");
            return activityResult;
        }
        LOGGER.debug("Progress Report State in ProcessValidNotification is : {}  : {}", state, progressReport);

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        switch (state) {
        case RUNNING:
            reportActivityIsOngoing(progressReport, notificationTime, ecimUpgradeInfo);
            break;
        case FINISHED:
            activityResult = reportActivateActivityIsFinished(progressReport, activityResult);
            break;
        default:
            LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
        return activityResult;
    }

    private void reportActivityIsOngoing(final AsyncActionProgress progressReport, final Date notificationTime, final EcimUpgradeInfo ecimUpgradeInfo) {
        LOGGER.debug("Entered into ActivateService:reportActivateActivityIsOngoing for activityJobId : {}", ecimUpgradeInfo.getActivityJobId());
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(ecimUpgradeInfo.getActivityJobId(), jobPropertyList, jobLogList, (double) progressReport.getProgressPercentage());
        progressPercentageCache.bufferNEJobs(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
    }

    public void reportActivateActivityIsFinished(final String upgradePackageMoFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo, final String completionFlow,
            final String logMessage) {
        LOGGER.debug("Entered into ActivateService:reportActivateActivityIsFinished with upMoStateInNotification() method.");
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final long activityStartTime = ecimUpgradeInfo.getNeJobStaticData().getActivityStartTime();
        String jobLogMessage = null;
        JobResult jobResult;
        JobLogLevel jobLogLevel;
        activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, jobActivityInfo);
        jobLogLevel = JobLogLevel.INFO;
        jobResult = JobResult.SUCCESS;
        logActivityCompletionFlow(ecimUpgradeInfo, completionFlow, logMessage, jobLogList, activityJobId, upgradePackageMoFdn);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
        jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.ACTIVATE);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        processVariables.put(JobVariables.ACTIVITY_EXECUTE_MANUALLY, false);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());

        final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        if (isJobResultPersisted) {
            activityUtils.sendNotificationToWFS(ecimUpgradeInfo.getNeJobStaticData(), activityJobId, ACTIVATE_ACTIVITY_NAME, processVariables);
        }
    }

    private void reportActivateActivityIsOnGoing(final String upgradePackageMoFdn, final EcimUpgradeInfo ecimUpgradeInfo) {
        LOGGER.debug("Entered into ActivateService:reportActivateActivityIsOnGoing() method.");
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();
        final boolean ignoreBreakPoints = ecimUpgradeInfo.isIgnoreBreakPoints();

        String jobLogMessage = null;
        JobLogLevel jobLogLevel;
        if (ignoreBreakPoints) {
            return;
        }
        jobLogMessage = JobLogConstants.WAIT_FOR_USER_INPUT;
        jobLogLevel = JobLogLevel.INFO;
        processVariables.put(JobVariables.ACTIVITY_EXECUTE_MANUALLY, true);
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        activityUtils.recordEvent(SHMEvents.ECIM_ACTIVATE_PROCESS_NOTIFICATION, nodeName, upgradePackageMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + ACTIVATE_ACTIVITY_NAME);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ACTIVATE_ACTIVITY_NAME, processVariables);
    }

    private ActivityResult reportActivateActivityIsFinished(final AsyncActionProgress progressReport, final ActivityResult activityResult) {
        LOGGER.debug("Entered into ActivateService:reportActivateActivityIsFinished with Action Result : {} ", progressReport.getResult());
        final ActionResultType result = progressReport.getResult();
        if (ActionResultType.SUCCESS == result) {
            LOGGER.debug("Action Result in reportActivateActivityIsFinished() is : {} ", progressReport.getResult());
            activityResult.setActivitySuccess(true);
        } else {
            activityResult.setActivitySuccess(false);
        }
        activityResult.setActivityCompleted(true);
        return activityResult;
    }

    public void reportActivateActivityIsFailed(final AsyncActionProgress progressReport, final EcimUpgradeInfo ecimUpgradeInfo, final String upgradePackageMoFdn, final String completionFlow,
            final String logMessage) {
        LOGGER.debug("Entered into ActivateService:reportActivateActivityIsFinished with Action Result : {} ", progressReport.getResult());
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        String jobLogMessage = "";
        logActivityCompletionFlow(ecimUpgradeInfo, completionFlow, logMessage, jobLogList, activityJobId, upgradePackageMoFdn);
        jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.ACTIVATE) + progressReport.getResultInfo();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, (double) progressReport.getProgressPercentage());
        progressPercentageCache.bufferNEJobs(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        activityUtils.sendNotificationToWFS(ecimUpgradeInfo.getNeJobStaticData(), activityJobId, ACTIVATE_ACTIVITY_NAME, processVariables);
    }

    /**
     * @param ecimUpgradeInfo
     * @param completionFlow
     * @param logMessage
     * @param jobLogList
     * @param activityJobId
     */
    private void logActivityCompletionFlow(final EcimUpgradeInfo ecimUpgradeInfo, final String completionFlow, final String logMessage, final List<Map<String, Object>> jobLogList,
            final long activityJobId, final String upgradePackageMoFdn) {
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.UPGRADE, ACTIVATE_ACTIVITY_NAME);
        activityUtils.recordEvent(eventName, ecimUpgradeInfo.getNeJobStaticData().getNodeName(), upgradePackageMoFdn,
                "SHM:" + activityJobId + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, completionFlow));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
    }

    public ActivityResult processValidUpStateNotification(final EcimUpgradeInfo ecimUpgradeInfo, final String upgradePackageMoFdn, final ActivateState upMoStateInNotification,
            final ActivityResult activityResult) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        LOGGER.debug("Entered into processValidUpStateNotification with upMoStateInNotification : {} ", upMoStateInNotification);
        if (upMoStateInNotification == null) {
            return activityResult;
        }
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        switch (upMoStateInNotification) {
        case ACTIVATE_STEP_COMPLETED:
            reportActivateActivityIsOnGoing(upgradePackageMoFdn, ecimUpgradeInfo);
            break;
        case ACTIVATION_COMPLETED:
            activityResult.setActivityCompleted(true);
            activityResult.setActivitySuccess(true);
            break;
        default:
            LOGGER.warn("Unsupported MO State {} for activityJobId {}", upMoStateInNotification, activityJobId);
        }
        return activityResult;
    }

    public ActivityResult processNotificationForCancel(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final Date notificationTime) {
        final ActivityResult activityResult = new ActivityResult();
        final ActionStateType state = progressReport.getState();

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();

        switch (state) {
        case RUNNING:
            reportActivityIsOngoing(progressReport, notificationTime, ecimUpgradeInfo);
            break;
        case FINISHED:
            activityResult.setActivityCompleted(true);
            break;

        default:
            LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
        return activityResult;
    }

    public void reportCancelActivityIsFinished(final AsyncActionProgress progressReport, final String moFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo,
            final String completionFlow) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();

        String jobLogMessage;
        JobResult jobResult;
        JobLogLevel jobLogLevel;
        activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);
        final ActionResultType result = progressReport.getResult();

        if (ActionResultType.SUCCESS == result) {
            jobLogLevel = JobLogLevel.INFO;
            jobResult = JobResult.CANCELLED;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, progressReport.getActionName());
        } else {
            jobLogLevel = JobLogLevel.ERROR;
            jobResult = JobResult.FAILED;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, progressReport.getActionName()) + progressReport.getResultInfo();
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        logActivityCompletionFlow(ecimUpgradeInfo, completionFlow, jobLogMessage, jobLogList, activityJobId, moFdn);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(ecimUpgradeInfo.getNeJobStaticData(), activityJobId, progressReport.getActionName(), new HashMap<String, Object>());
    }

    public ActivateState getUpgradePackageState(final String modifiedStateAttribute) {
        LOGGER.debug("Current UP state in getUpgradePackageState() is: {}", modifiedStateAttribute);
        final UpgradePackageState upgradePackageState = UpgradePackageState.getState(modifiedStateAttribute);
        if (upgradePackageState == UpgradePackageState.ACTIVATION_STEP_COMPLETED) {
            return ActivateState.ACTIVATE_STEP_COMPLETED;
        }
        if ((upgradePackageState == UpgradePackageState.WAITING_FOR_COMMIT) || (upgradePackageState == UpgradePackageState.COMMIT_COMPLETED)) {
            return ActivateState.ACTIVATION_COMPLETED;
        }
        return null;
    }

    /**
     * @param ecimUpgradeInfo
     * @param progressReport
     * @param upgradePackageState
     * @throws UnsupportedFragmentException
     */
    public boolean validateNotificationAttributes(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final ActivateState upgradePackageState)
            throws UnsupportedFragmentException {
        boolean isValidNotification = false;
        if (progressReport == null && upgradePackageState == null) {
            return isValidNotification;
        }
        final boolean isValidUpgradePackageState = upgradePackageState != null
                ? ((upgradePackageState == ActivateState.ACTIVATE_STEP_COMPLETED) || (upgradePackageState == ActivateState.ACTIVATION_COMPLETED)) : false;
        final boolean isInValidReportProgress = isInValidReportProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.ACTIVATE_UPGRADE_PACKAGE);
        LOGGER.debug("isValidUpgradePackageState is : {} and isValid is : {} ", isValidUpgradePackageState, isInValidReportProgress);
        if (isInValidReportProgress && !isValidUpgradePackageState) {
            return isValidNotification;
        }
        isValidNotification = true;
        return isValidNotification;
    }

    /**
     * @param ecimUpgradeInfo
     * @param progressReport
     * @throws UnsupportedFragmentException
     */
    public boolean validateNotificationAttributesForCancel(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport) throws UnsupportedFragmentException {
        boolean isValidNotification = false;
        if (progressReport == null) {
            return isValidNotification;
        }
        isValidNotification = isInValidReportProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE);
        return isValidNotification;
    }

    /**
     * @param ecimUpgradeInfo
     * @param progressReport
     * @param activityName
     * @throws UnsupportedFragmentException
     */
    private boolean isInValidReportProgress(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final String activityName) throws UnsupportedFragmentException {
        final boolean isInValidReportProgress = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, activityName);
        LOGGER.debug("isInValidReportProgress is : {} ", isInValidReportProgress);
        if (isInValidReportProgress) {
            return true;
        }
        return false;
    }

    /**
     * This method validate the action triggered status based on ExecuteResponse and persists activity results if action trigger failed else proceed further. If action triggered successfully then this
     * method initiates MTR to update heartbeat interval on CmNodeHeartbeatSupervision MO to over come notification delays from mediation.
     *
     * @param ecimUpgradeInfo
     * @param executeResponse
     * @param neJobStaticData
     * @throws MoNotFoundException
     * @throws JobDataNotFoundException
     */
    public void doExecutePostValidation(final long activityJobId, final ExecuteResponse executeResponse, final JobActivityInfo jobActivityInfo, final List<Map<String, Object>> jobLogList)
            throws MoNotFoundException, JobDataNotFoundException {

        final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String upgradePackageMoFdn = executeResponse.getFdn();
        final String nodeName = neJobStaticData.getNodeName();
        if (executeResponse.isActionTriggered()) {
            final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final String neType = networkElementData.getNeType();
            final String nodeFdn = networkElementData.getNeFdn();
            if (nodeFdn != null) {
                final int heartbeatInterval = pollingActivityConfiguration.getShmHeartBeatIntervalForEcim();
                cmHeartbeatHandler.sendHeartbeatIntervalChangeRequest(heartbeatInterval, activityJobId, nodeFdn);
            } else {
                LOGGER.warn("node fdn value returned null for the node:{} and activityJobId: {}", nodeName, activityJobId);
            }
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            logActionTriggeredInfo(neType, ecimUpgradeInfo, jobLogList, jobPropertyList);

            systemRecorder.recordCommand(SHMEvents.ACTIVATE_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upgradePackageMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
        } else {
            activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, jobActivityInfo);
            LOGGER.error("Activate action Trigger failed for upgrade package {} because action ID found to be non-zero. ActionId found : {}", upgradePackageMoFdn, executeResponse);
            systemRecorder.recordCommand(SHMEvents.ACTIVATE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upgradePackageMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.ACTIVATE);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), ActivityConstants.ACTIVATE);
        }
    }

    private void logActionTriggeredInfo(final String neType, final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList) {

        final Integer activateActivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.name(), ACTIVATE_ACTIVITY_NAME);
        final boolean upMoIgnoreBreakPoints = ecimUpgradeInfo.isIgnoreBreakPoints();
        String logMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.ACTIVATE, activateActivityTimeout);
        if (!upMoIgnoreBreakPoints) {
            final int stepNumber = Integer.parseInt(
                    activityUtils.getActivityJobAttributeValue(ecimUpgradeInfo.getJobEnvironment().getActivityJobAttributes(), EcimCommonConstants.UpgradePackageMoConstants.UP_MO_ACTIVATION_STEP));
            final Integer stepNo = stepNumber + 1;
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.UpgradePackageMoConstants.UP_MO_ACTIVATION_STEP, stepNo.toString());
            LOGGER.debug("Executing activate step {} of Upgrade job for nodeName {}", stepNo, ecimUpgradeInfo.getNeJobStaticData().getNodeName());
            logMessage = String.format(JobLogConstants.STEPBYSTEP_TRIGGERED, stepNo, ActivityConstants.ACTIVATE, activateActivityTimeout);
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    public boolean isActivityCompleted(final long activityJobId) {
        return jobConfigurationService.isJobResultEvaluated(activityJobId);
    }
}
