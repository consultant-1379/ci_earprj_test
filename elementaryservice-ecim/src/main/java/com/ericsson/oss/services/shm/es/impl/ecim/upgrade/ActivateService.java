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
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade;

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
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.AsynchronousPollingActivity;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.es.upgrade.api.ActivityResult;
import com.ericsson.oss.services.shm.es.upgrade.api.UpgradePrecheckResponse;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivateState;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
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

@EServiceQualifier("ECIM.UPGRADE.activate")
@ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ActivateService implements Activity, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateService.class);

    private static final String UPGRADE_PACKAGE_NOT_EXIST = "ACTIVATE activity is failed due to UpgradePackage MO doesn't exist on node.";
    private static final String NOT_IN_PROPER_STATE = "Upgrade package is not in either PREPARE_COMPLETED or ACTIVATION_STEP_COMPLETE state";

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private EcimSwMUtils ecimSwMUtils;

    @Inject
    private CancelUpgradeService cancelUpgradeService;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    private ActivateServiceHelper activateServiceHelper;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private EcimCommonUtils ecimCommonUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    public static final String ACTIVATE_ACTIVITY_NAME = "activate";
    public static final String CANCEL_ACTIVATE_ACTIVITY = "cancel";

    /**
     * This method validates upgrade activate request to verify whether the activate activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside ActivateService precheck() with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the ActivateService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.ACTIVATE, exceptionMessage);
        }
        LOGGER.debug("Precheck completed in ActivateService for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    private ActivityStepResultEnum precheck(final EcimUpgradeInfo ecimUpgradeInfo) {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();

        ecimSwMUtils.initializeActivityJobLogs(nodeName, ActivityConstants.ACTIVATE, jobLogList);
        final UpgradePrecheckResponse upgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);

        return upgradePrecheckResponse.getActivityStepResultEnum();
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action on the node.
     * 
     * @param activityJobId
     * 
     */
    @Asynchronous
    @Override
    public void execute(final long activityJobId) {
        boolean isActionInvoked = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String neJobBusinessKey = "";
        String nodeName = "";
        UpgradePrecheckResponse upgradePrecheckResponse = null;
        JobStaticData jobStaticData = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            nodeName = neJobStaticData.getNodeName();
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId, neJobStaticData);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVATE_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.ACTIVATE);
                return;
            }
            ecimSwMUtils.initializeActivityJobLogs(neJobStaticData.getNodeName(), ActivityConstants.ACTIVATE, jobLogList);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            // For checking whether precheck is already completed in the N-1 step
            final boolean isPrecheckAlreadyDone = activityUtils.isPrecheckDone((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
            if (!isPrecheckAlreadyDone) {
                upgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);
                jobLogList.clear();
            } else {
                upgradePrecheckResponse = new UpgradePrecheckResponse(upMoServiceRetryProxy.getNotifiableMoFdn(ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo),
                        ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            }

            if (upgradePrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                final ExecuteResponse executeResponse = executeAction(upgradePrecheckResponse, ecimUpgradeInfo, jobLogList);
                isActionInvoked = true;
                if (isActionInvoked) {
                    activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
                    activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));
                    final boolean isJobAttributesPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                    if (!isJobAttributesPersisted) {
                        LOGGER.error("jobPropertyList:{} and jobLogList:{} are not persisted for activityJobId: {}", jobPropertyList, jobLogList, activityJobId);
                    }
                }
                activateServiceHelper.doExecutePostValidation(activityJobId, executeResponse, activityUtils.getActivityInfo(activityJobId, this.getClass()), jobLogList);
            } else {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.ACTIVATE);
            }
        }catch (final JobDataNotFoundException jex) {
            LOGGER.error("ActivateService.execute-Unable to trigger an action on the node: {} with activityJobId: {}. Reason:{} ", nodeName, activityJobId, jex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.ACTIVATE);
        }
        catch (final Exception ex) {
            LOGGER.error("ActivateService.execute-Unable to trigger an action on the node: {} with activityJobId: {}. Reason:  ", nodeName, activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, ActivityConstants.ACTIVATE, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.ACTIVATE);
        }
    }

    private ExecuteResponse executeAction(final UpgradePrecheckResponse upgradePrecheckResponse, final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        LOGGER.debug("Inside ActivateService execute() with activityJobId {}", activityJobId);
        final String upgradePackageMoFdn = upgradePrecheckResponse.getUpMoFdn();
        String nodeName = null;
        ActionResult actionResult = new ActionResult();
        try {
            final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
            final long mainJobId = neJobStaticData.getMainJobId();
            final boolean upMoIgnoreBreakPoints = ecimUpgradeInfo.isIgnoreBreakPoints();
            final Map<String, Object> changedAttributes = new HashMap<String, Object>();
            changedAttributes.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS, upMoIgnoreBreakPoints);
            upMoServiceRetryProxy.updateMOAttributes(ecimUpgradeInfo, changedAttributes);
            activityUtils.recordEvent(SHMEvents.ECIM_ACTIVATE_EXECUTE, nodeName, upgradePackageMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            activityUtils.subscribeToMoNotifications(upgradePackageMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            systemRecorder.recordCommand(SHMEvents.ACTIVATE_SERVICE, CommandPhase.STARTED, nodeName, upgradePackageMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
            actionResult = performActionOnNode(ecimUpgradeInfo, upgradePackageMoFdn, jobLogList);

        } catch (final MoNotFoundException e) {
            final String jobLogMessage = String.format(JobLogConstants.MO_DOES_NOT_EXIST, String.format(UPGRADE_PACKAGE_NOT_EXIST, upgradePackageMoFdn));
            LOGGER.error(jobLogMessage + "Reason for failure:", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            final String jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            LOGGER.error(jobLogMessage + "Reason for failure:", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred in activate execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, ex);
            String jobLogMessage = NodeMediationServiceExceptionParser.getReason(ex);
            jobLogMessage = jobLogMessage.isEmpty() ? ex.getMessage() : jobLogMessage;
            String logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.ACTIVATE, jobLogMessage);
            if (jobLogMessage == null || jobLogMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.ACTIVATE);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return new ExecuteResponse(actionResult.isTriggerSuccess(), upgradePackageMoFdn, actionResult.getActionId());
    }

    private UpgradePrecheckResponse getPrecheckResponse(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String jobLogMessage;
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final Map<String, Object> changedAttributes = new HashMap<String, Object>();
        changedAttributes.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS, ecimUpgradeInfo.isIgnoreBreakPoints());
        String upgradePackageMo = null;
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        try {
            upgradePackageMo = upMoServiceRetryProxy.getNotifiableMoFdn(ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo);
            if (upMoServiceRetryProxy.isActivityAllowed(ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo).getActivityAllowed()) {
                jobLogMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.ACTIVATE, upgradePackageMo);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

                final int activationSteps = upMoServiceRetryProxy.getActivationSteps(ecimUpgradeInfo, changedAttributes);
                jobLogMessage = ecimUpgradeInfo.isIgnoreBreakPoints() ? JobLogConstants.ONEGO_ACTIVATION : String.format(JobLogConstants.STEPBYSTEP_ACTIVATION, activationSteps);
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            } else {
                jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.ACTIVATE, NOT_IN_PROPER_STATE);
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final MoNotFoundException moNotFoundException) {
            jobLogMessage = handleException(activityJobId, jobLogList, UPGRADE_PACKAGE_NOT_EXIST, upgradePackageMo, moNotFoundException);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            jobLogMessage = handleException(activityJobId, jobLogList, JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName, unsupportedFragmentException);
        } catch (final Exception ex) {
            jobLogMessage = handleException(activityJobId, jobLogList, JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.ACTIVATE, ex);
        }
        activityUtils.recordEvent(SHMEvents.ECIM_ACTIVATE_PRECHECK, nodeName, upgradePackageMo, activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
        activityUtils.prepareJobPropertyList(propertyList, EcimCommonConstants.UpgradePackageMoConstants.UP_MO_ACTIVATION_STEP, "0");
        // Updating job properties with precheck completed flag.
        activityUtils.prepareJobPropertyList(propertyList, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        if (activityStepResultEnum == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity:{} is to be skipped/failed for the activityJobId: {}.", ActivityConstants.ACTIVATE, activityJobId);
        }
        return new UpgradePrecheckResponse(upgradePackageMo, activityStepResultEnum);

    }

    /**
     * This method processes the notifications by fetching the notification subject and validates the notification. It de-register from the notification as it founds activity is completed and notifies
     * to WorkFlowService or else it will wait for another notification.
     * 
     * @param notification
     * 
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM - upgrade - Activate Service - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("ECIM - upgrade - Activate Service - - Discarding non-AVC notification.");
            return;
        }

        String jobLogMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
        final String upgradePackageMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            final String actionTriggered = ecimUpgradeInfo.getActionTriggered();
            if (actionTriggered == null) {
                return;
            }
            switch (actionTriggered) {
            case ACTIVATE_ACTIVITY_NAME:
                handleNotificationForActivateAction(activityJobId, modifiedAttributes, notificationTime, upgradePackageMoFdn, nodeName, ecimUpgradeInfo);
                break;
            case EcimSwMConstants.CANCEL_UPGRADE_PACKAGE:
                handleNotificationForCancelAction(activityJobId, modifiedAttributes, notificationTime, upgradePackageMoFdn, nodeName, ecimUpgradeInfo);
                break;
            default:
                LOGGER.warn("None of the required actions are triggered. ActivityJobId {}, actionTriggeredProperty: {}", activityJobId, actionTriggered);
            }
        } catch (final UnsupportedFragmentException e) {
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            LOGGER.error(jobLogMessage.concat(JobLogConstants.FAILUREREASON), e);
        } catch (final Exception e) {
            jobLogMessage = String.format(JobLogConstants.EXCEPTION_OCCURED, e.getMessage());
            LOGGER.error(jobLogMessage.concat(JobLogConstants.FAILUREREASON), e);
        }
        if (jobLogMessage != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
    }

    /**
     * @param activityJobId
     * @param modifiedAttributes
     * @param notificationTime
     * @param upgradePackageMoFdn
     * @param nodeName
     * @param ecimUpgradeInfo
     * @throws UnsupportedFragmentException
     */
    private void handleNotificationForCancelAction(final long activityJobId, final Map<String, AttributeChangeData> modifiedAttributes, final Date notificationTime, final String upgradePackageMoFdn,
            final String nodeName, final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException {
        final AsyncActionProgress progressReportForCancel = upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        if (!activateServiceHelper.validateNotificationAttributesForCancel(ecimUpgradeInfo, progressReportForCancel)) {
            LOGGER.warn("Discarding as invalid notification as it doesn't contain progressReport attribute with cancel action. ActivityJobId {}; Notification Recieved : {}", activityJobId,
                    modifiedAttributes);
            return;
        }
        final ActivityResult activityResult = activateServiceHelper.processNotificationForCancel(ecimUpgradeInfo, progressReportForCancel, notificationTime);
        if (activityResult.isActivityCompleted()) {
            activateServiceHelper.reportCancelActivityIsFinished(progressReportForCancel, upgradePackageMoFdn, ecimUpgradeInfo, jobActivityInfo, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS);
        }
    }

    /**
     * @param activityJobId
     * @param modifiedAttributes
     * @param notificationTime
     * @param upgradePackageMoFdn
     * @param nodeName
     * @param ecimUpgradeInfo
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    private void handleNotificationForActivateAction(final long activityJobId, final Map<String, AttributeChangeData> modifiedAttributes, final Date notificationTime, final String upgradePackageMoFdn,
            final String nodeName, final EcimUpgradeInfo ecimUpgradeInfo)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        final AsyncActionProgress progressReport = upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes);
        final ActivateState upgradePackageState = upMoServiceRetryProxy.getUpgradePackageState(nodeName, modifiedAttributes);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        LOGGER.debug("UpgradePackageState in ActivateService process notification is : {} ", upgradePackageState);
        if (!activateServiceHelper.validateNotificationAttributes(ecimUpgradeInfo, progressReport, upgradePackageState)) {
            return;
        }
        final ActivityResult activityResult = activateServiceHelper.handleNotificationForActivateAction(ecimUpgradeInfo, progressReport, upgradePackageState, notificationTime, upgradePackageMoFdn);
        final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS, ActivityConstants.ACTIVATE);
        if (activityResult.isActivityCompleted() && activityResult.isActivitySuccess()) {
            activateServiceHelper.reportActivateActivityIsFinished(upgradePackageMoFdn, ecimUpgradeInfo, jobActivityInfo, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS, logMessage);
        } else if (activityResult.isActivityCompleted() && !activityResult.isActivitySuccess()) {
            activateServiceHelper.reportActivateActivityIsFailed(progressReport, ecimUpgradeInfo, upgradePackageMoFdn, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS, logMessage);
        }
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String upgradePackageMoFdn = "";
        String nodeName = "";
        LOGGER.debug("Handling activate activity after reaching timeout value-activityJobId {}", activityJobId);
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            upgradePackageMoFdn = upMoServiceRetryProxy.getNotifiableMoFdn(ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo);
            activityStepResultEnum = handleTimeout(upgradePackageMoFdn, ecimUpgradeInfo);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and can not be proceeded further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.ACTIVATE, exceptionMessage);
            activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    private ActivityStepResultEnum handleTimeout(final String upgradePackageMoFdn, final EcimUpgradeInfo ecimUpgradeInfo) {
        String jobLogMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        LOGGER.debug("Handling activate activity after reaching timeout value-activityJobId {}", activityJobId);

        long activityStartTime = 0;
        try {
            activityStartTime = neJobStaticData.getActivityStartTime();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, ActivityConstants.ACTIVATE), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            final AsyncActionProgress progressReport = upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo);
            final boolean isInvalid = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.ACTIVATE_UPGRADE_PACKAGE);
            if (isInvalid) {
                LOGGER.info("Progress report for activity job id {} is : {}", activityJobId, progressReport);
                handleInvalidAsyncActionProgress(activityJobId, jobLogList, propertyList, progressReport.getActionName(), upgradePackageMoFdn);
                return activityStepResultEnum;
            }
            activityStepResultEnum = processHandleTimeout(ecimUpgradeInfo, jobLogList, propertyList, progressReport, upgradePackageMoFdn);
            if (activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS || activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) {
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
            }
            return activityStepResultEnum;
        } catch (final MoNotFoundException e) {
            jobLogMessage = String.format(JobLogConstants.MO_DOES_NOT_EXIST, String.format(UPGRADE_PACKAGE_NOT_EXIST, upgradePackageMoFdn));
            LOGGER.error(jobLogMessage + "Reason for failure:" + e);
        } catch (final UnsupportedFragmentException e) {
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, neJobStaticData.getNodeName());
            LOGGER.error(jobLogMessage + "Reason for failure:" + e);
        } catch (final NodeAttributesReaderException e) {
            jobLogMessage = String.format(JobLogConstants.NODE_READ_EXCEPTION, e.getMessage());
            LOGGER.error(jobLogMessage + "Reason for failure:" + e);
        } catch (final Exception e) {
            jobLogMessage = String.format(JobLogConstants.EXCEPTION_OCCURED, e.getMessage());
            LOGGER.error(jobLogMessage + "Reason for failure:" + e);
        }
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString(), propertyList);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());

        if (activityStartTime != 0) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        recordEvent(activityJobId, upgradePackageMoFdn, jobLogMessage, ActivityConstants.COMPLETED_THROUGH_TIMEOUT);
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ACTIVATE_ACTIVITY_NAME, neJobStaticData.getNodeName());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        return activityStepResultEnum;
    }

    private void handleInvalidAsyncActionProgress(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList, final String actionName,
            final String upgradePackageMoFdn) {
        LOGGER.error("Failing activate activity as the action name in AsyncActionProgress ({}) does not match the actual action ({}) triggered for Activity Job Id {}", actionName,
                ACTIVATE_ACTIVITY_NAME, activityJobId);
        final String jobLogMessage = String.format(JobLogConstants.ACTION_NAME_MISMATCH_FAILURE_MSG, ActivityConstants.ACTIVATE, ACTIVATE_ACTIVITY_NAME, actionName);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString(), propertyList);
        recordEvent(activityJobId, upgradePackageMoFdn, jobLogMessage, ActivityConstants.COMPLETED_THROUGH_TIMEOUT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.info("Inside ActivateService cancel() with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = cancelUpgradeService.cancel(activityJobId, EcimSwMConstants.ACTIVATE_UPGRADE_PACKAGE);
        return activityStepResult;
    }

    private ActionResult performActionOnNode(final EcimUpgradeInfo ecimUpgradeInfo, final String upgradePackageMoFdn, final List<Map<String, Object>> jobLogList) {
        ActionResult actionResult = new ActionResult();
        String jobLogMessage = null;
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        LOGGER.debug("Action triggered is :: {} for the activityJobId:{}", ecimUpgradeInfo.getActionTriggered(), activityJobId);
        try {
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, ACTIVATE_ACTIVITY_NAME);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
            actionResult = upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.ACTIVATE_UPGRADE_PACKAGE);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("Couldn't trigger Activate mo action on node for upgrade {} because {}", upgradePackageMoFdn, unsupportedFragmentException);
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.ACTIVATE, unsupportedFragmentException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            LOGGER.error("Triggering of Activate mo action on node for upgrade failed of fdn {} because {}", upgradePackageMoFdn, exception);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.ACTIVATE, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.ACTIVATE);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        LOGGER.debug("Activate Activity is triggered on upgradePackage MO {}  with activityJobId as {} and actionId returned is {}", upgradePackageMoFdn, activityJobId, actionResult);

        return actionResult;
    }

    private ActivityStepResultEnum processHandleTimeout(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList,
            final AsyncActionProgress progressReport, final String upgradePackageMoFdn)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        LOGGER.debug("Entered ActivateService processHandleTimeout with actionName : {}", progressReport.getActionName());
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        ActivityStepResultEnum handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final boolean isActivityCompleted = upMoServiceRetryProxy.isActivityCompleted(progressReport.getActionName(), ecimUpgradeInfo);
        JobResult jobResult = JobResult.FAILED;

        String jobLogMessage = null;
        LOGGER.debug("isActivityCompleted {} and ignoreBreakPoints in handletimeout is : {}", isActivityCompleted, ecimUpgradeInfo.isIgnoreBreakPoints());
        if (isActivityCompleted) {
            handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.ACTIVATE);
            jobResult = JobResult.SUCCESS;
        } else if (!isActivityCompleted && ecimUpgradeInfo.isIgnoreBreakPoints()) {
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.ACTIVATE);
            LOGGER.debug("On Go activation has {}", handleTimeoutStatus.toString());
        } else {
            handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_REPEAT_EXECUTE_MANUAL;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.ACTIVATE);
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), propertyList);
        recordEvent(activityJobId, upgradePackageMoFdn, jobLogMessage, ActivityConstants.COMPLETED_THROUGH_TIMEOUT);
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        return handleTimeoutStatus;
    }

    /**
     * @param activityJobId
     * @param activityStepResult
     * @param upgradePackageMo
     * @param moNotFoundException
     * @return
     */
    private String handleException(final long activityJobId, final List<Map<String, Object>> jobLogList, final String logMessage, final String placeHolder, final Exception exception) {

        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
        String errorMessage = "";
        if (!(exceptionMessage.isEmpty())) {
            errorMessage = exceptionMessage;
        } else {
            errorMessage = exception.getMessage();
        }
        String jobLogMessage = "";
        if (placeHolder != null) {
            jobLogMessage = String.format(logMessage, placeHolder, errorMessage);
        } else {
            jobLogMessage = String.format(logMessage, "", errorMessage);
        }
        LOGGER.error(jobLogMessage + " for {}" + "Reason for failure:" + exception, activityJobId);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        return jobLogMessage;
    }

    /**
     * This method is called from shm-workflows {#ActivityCancelTimeout} when cancel activity is timed out . Verifies the cancel action status on node and updates the activity result accordingly.
     *
     * @param activityJobId
     * @param finalizeResult
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("Entered {} cancelTimeout() with activityJobId : {}", ACTIVATE_ACTIVITY_NAME, activityJobId);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        JobResult jobResult = JobResult.FAILED;

        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            final long mainJobId = ecimUpgradeInfo.getNeJobStaticData().getMainJobId();
            final  String upgradePackageMoFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.ACTIVATE_UPGRADE_PACKAGE, ecimUpgradeInfo);
            final AsyncActionProgress progressReport = upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo);
            final boolean isInvalid = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE);
            if (isInvalid) {
                handleInvalidAsyncActionProgress(activityJobId, jobLogList, jobPropertyList, progressReport.getActionName(), upgradePackageMoFdn);
                return activityStepResult;
            }
            if (progressReport != null && CANCEL_ACTIVATE_ACTIVITY.equals(progressReport.getActionName())) {
                jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, new Date(), ACTIVATE_ACTIVITY_NAME);
            }
            if (jobResult != null) {
                if (jobResult == JobResult.FAILED || jobResult == JobResult.CANCELLED) {
                    systemRecorder.recordCommand(SHMEvents.ACTIVATE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upgradePackageMoFdn,
                            activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
                }
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
                activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, ActivateService.class));
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ACTIVATE_ACTIVITY_NAME, nodeName);
                jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            } else {
                if (finalizeResult) {
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, CANCEL_ACTIVATE_ACTIVITY), new Date(), JobLogType.SYSTEM.toString(),
                            JobLogLevel.ERROR.toString());
                } else {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
                }
                jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            }
            return activityStepResult;
        } catch (final Exception e) {
            LOGGER.error("Exception occurred and can not be proceeded further for activityId {} . Reason : {}", activityJobId, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, exceptionMessage);
        }
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        LOGGER.debug("Entered into ActivateService asyncPrecheck() with activityJobId {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the ActivateService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.ACTIVATE, exceptionMessage);
        }
        LOGGER.debug("ActivateService.asyncPrecheck completed for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.ACTIVATE, activityStepResultEnum);
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into ActivateService.precheckHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.ACTIVATE);
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Handling activate activity after reaching timeout value-activityJobId {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String upgradePackageMoFdn = "";
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            upgradePackageMoFdn = upMoServiceRetryProxy.getNotifiableMoFdn(ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo);
            activityStepResultEnum = handleTimeout(upgradePackageMoFdn, ecimUpgradeInfo);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and can not be proceeded further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.ACTIVATE, exceptionMessage);
            activityUtils.unSubscribeToMoNotifications(upgradePackageMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        }
        LOGGER.debug("asyncHandleTimeout completed in ActivateService with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.ACTIVATE, activityStepResultEnum);
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into ActivateService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.ACTIVATE);
    }

    private void recordEvent(final long activityJobId, final String upgradePackageFdn, final String jobLogMessage, final String flow) {
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.UPGRADE, ACTIVATE_ACTIVITY_NAME);
        activityUtils.recordEvent(eventName, upgradePackageFdn, upgradePackageFdn, "SHM:" + activityJobId + ":" + jobLogMessage + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.polling.api.PollingActivity# subscribeForPolling(long)
     */
    @Override
    @Asynchronous
    public void subscribeForPolling(final long activityJobId) {
        LOGGER.debug("subscribeForPolling in ActivateService for activityJobId {}", activityJobId);

        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        try {
            final boolean isDpsAvailable = isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
            if (isDpsAvailable) {
                final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
                final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
                final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(neJobStaticData.getNodeName());
                final String moFdn = upMoServiceRetryProxy.getNotifiableMoFdn(ACTIVATE_ACTIVITY_NAME, ecimUpgradeInfo);
                final List<String> moAttributes = Arrays.asList(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_REPORT_PROGRESS, EcimCommonConstants.UpgradePackageMoConstants.UP_MO_STATE);
                pollingActivityManager.subscribe(jobActivityInfo, networkElementData, FragmentType.ECIM_SWM_TYPE.getFragmentName(), moFdn, moAttributes);
                LOGGER.debug("Polling subscription started in ActivateService with activityJobId {}", activityJobId);
            }
        } catch (final Exception ex) {
            LOGGER.error("ActivateService-subscribeForPolling-Unable to subscribe for polling for activityJobId: {} .Reason:  ", activityJobId, ex);
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.PollingCallBack# processPollingResponse(long, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        LOGGER.debug("Polling response received in ActivateService for activityJobId {} responseAttributes {}", activityJobId, responseAttributes);
        String jobLogMessage = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> modifiedAttributes = (Map<String, Object>) responseAttributes.get(ShmConstants.MO_ATTRIBUTES);
        final String upgradePackageMoFdn = (String) responseAttributes.get(ShmConstants.FDN);
        try {
            final boolean isActivityCompleted = activateServiceHelper.isActivityCompleted(activityJobId);
            if (!isActivityCompleted) {
                final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
                final String actionTriggered = ecimUpgradeInfo.getActionTriggered();
                if (actionTriggered == null) {
                    return;
                }
                switch (actionTriggered) {
                case ACTIVATE_ACTIVITY_NAME:
                    processPollingResponseForActivate(ecimUpgradeInfo, modifiedAttributes, upgradePackageMoFdn);
                    break;
                case EcimSwMConstants.CANCEL_UPGRADE_PACKAGE:
                    processPollingResponseForCancel(ecimUpgradeInfo, modifiedAttributes, new Date(), upgradePackageMoFdn);
                    break;
                default:
                    LOGGER.warn("None of the required actions are triggered. ActivityJobId {}, actionTriggeredProperty: {}", activityJobId, actionTriggered);
                }
            } else {
                LOGGER.debug("Found activate activity result already persisted in ActivityJob PO, Assuming activity completed on the node for activityJobId: {} and FDN: {}.", activityJobId,
                        upgradePackageMoFdn);
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, ACTIVATE_ACTIVITY_NAME, upgradePackageMoFdn);
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

    @SuppressWarnings("unchecked")
    private void processPollingResponseForActivate(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, Object> modifiedAttributes, final String upgradePackageMoFdn)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        LOGGER.debug("Inside Activate Service handlePollingResultForActivateAction with upgradePackageMoFdn : {}", upgradePackageMoFdn);
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());

        final AsyncActionProgress progressReport = new AsyncActionProgress((Map<String, Object>) modifiedAttributes.get(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_REPORT_PROGRESS));
        final ActivateState upgradePackageState = activateServiceHelper.getUpgradePackageState((String) modifiedAttributes.get(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_STATE));
        LOGGER.debug("For the node {} UpgradePackageState retrieved from the polling response is: {}", nodeName, upgradePackageState);

        if (!activateServiceHelper.validateNotificationAttributes(ecimUpgradeInfo, progressReport, upgradePackageState)) {
            LOGGER.warn("Discarding as invalid notification as it doesn't contain progressReport attribute. ActivityJobId {}", activityJobId);
            return;
        }
        final ActivityResult activityResult = activateServiceHelper.handleNotificationForActivateAction(ecimUpgradeInfo, progressReport, upgradePackageState, new Date(), upgradePackageMoFdn);
        final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING, ActivityConstants.ACTIVATE);
        if (activityResult.isActivityCompleted() && activityResult.isActivitySuccess()) {
            activateServiceHelper.reportActivateActivityIsFinished(upgradePackageMoFdn, ecimUpgradeInfo, jobActivityInfo, ActivityConstants.COMPLETED_THROUGH_POLLING, logMessage);
        } else if (activityResult.isActivityCompleted() && !activityResult.isActivitySuccess()) {
            activateServiceHelper.reportActivateActivityIsFailed(progressReport, ecimUpgradeInfo, upgradePackageMoFdn, ActivityConstants.COMPLETED_THROUGH_POLLING, logMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private void processPollingResponseForCancel(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, Object> modifiedAttributes, final Date notificationTime, final String moFdn)
            throws UnsupportedFragmentException {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());

        final AsyncActionProgress progressReport = new AsyncActionProgress((Map<String, Object>) modifiedAttributes.get(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_REPORT_PROGRESS));
        if (!activateServiceHelper.validateNotificationAttributesForCancel(ecimUpgradeInfo, progressReport)) {
            LOGGER.warn("Discarding as invalid notification as it doesn't contain progressReport attribute with cancel action. ActivityJobId {}; Notification Recieved : {}", activityJobId,
                    modifiedAttributes);
            return;
        }
        final ActivityResult activityResult = activateServiceHelper.processNotificationForCancel(ecimUpgradeInfo, progressReport, notificationTime);
        if (activityResult.isActivityCompleted()) {
            activateServiceHelper.reportCancelActivityIsFinished(progressReport, moFdn, ecimUpgradeInfo, jobActivityInfo, ActivityConstants.COMPLETED_THROUGH_POLLING);
        }
    }

}
