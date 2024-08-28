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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.CREATE_UPGRADEPKG_END_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
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
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedAttributeException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.upgrade.api.UpgradePrecheckResponse;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class PrepareServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(PrepareServiceHandler.class);

    private static final String PREPARE_ACTIVITY_NAME = "prepare";
    private static final String CREATE_ACTIVITY_NAME = "createUpgradePackage";

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private EcimSwMUtils ecimSwmUtils;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private JobLogUtil jobLogUtil;

    public UpgradePrecheckResponse handleExceptionForPrecheck(final String jobLogMessage, final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList, final String upMoFdn) {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        logger.error(jobLogMessage + " for {}", activityJobId);
        activityUtils.recordEvent(SHMEvents.PREPARE_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
        return new UpgradePrecheckResponse(upMoFdn, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    public void markActivityWithTriggeredAction(final long activityJobId, final String actionName) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, actionName);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
    }

    public void updateSmrsDetailsIfRequired(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {
        String nodeName = "";
        long activityJobId = 0;
        try {
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            activityJobId = ecimUpgradeInfo.getActivityJobId();
            logger.debug("Updating SMRS details on UpgradePackage MO before triggering prepare action for node {}", nodeName);
            final Map<String, Object> smrsDetailsFromUpMo = upMoServiceRetryProxy.getUpgradePackageUri(ecimUpgradeInfo);

            final Map<String, Object> actualSmrsDetails = upMoServiceRetryProxy.buildUpgradePackageUri(ecimUpgradeInfo);

            final Map<String, Object> changedAttributes = new HashMap<String, Object>();
            if (!smrsDetailsFromUpMo.get(UpgradeActivityConstants.ACTION_ARG_PASSWORD).equals(actualSmrsDetails.get(UpgradeActivityConstants.ACTION_ARG_PASSWORD))) {
                changedAttributes.put(UpgradeActivityConstants.ACTION_ARG_PASSWORD, actualSmrsDetails.get(UpgradeActivityConstants.ACTION_ARG_PASSWORD));
            }
            if (changedAttributes.size() > 0) {
                logger.debug("Updating Upgrade Package MO for node {}", nodeName);
                upMoServiceRetryProxy.updateMOAttributes(ecimUpgradeInfo, changedAttributes);
            }
        } catch (final UnsupportedAttributeException e) {
            logger.warn("Password attribute not supported in this version of fragment for nodeName: {} and activityJobId: {}. Exception is: ", nodeName, activityJobId, e);
        } catch (final Exception ex) {
            logger.error("Failed to update upMO Attributes due to {}", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.FTPSERVER_DATA_NOT_UPDATED, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
    }

    public ExecuteResponse triggerPrepareAction(final String upMoFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo, final List<Map<String, Object>> jobLogList) {
        logger.debug("Inside triggerPrepareAction in execute() with node : {}", ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();

        systemRecorder.recordCommand(SHMEvents.PREPARE_SERVICE, CommandPhase.STARTED, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
        activityUtils.subscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo);
        return performPrepareActionOnNode(ecimUpgradeInfo, jobLogList, upMoFdn);
    }

    private ExecuteResponse performPrepareActionOnNode(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList, final String upMoFdn) {
        logger.debug("Entered into performPrepareActionOnNode with upMoFdn : {} and nodeName : {}", upMoFdn, ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        String jobLogMessage = "";

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();
        String neType = null;
        ActionResult actionResult = new ActionResult();
        try {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            neType = networkElementRetrievalBean.getNeType(nodeName);
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, PREPARE_ACTIVITY_NAME);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
            actionResult = upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.name(), PREPARE_ACTIVITY_NAME);
            jobLogMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.PREPARE, activityTimeout);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            final String existingStepDurations = (String) ecimUpgradeInfo.getJobEnvironment().getActivityJobAttributes().get(ShmConstants.STEP_DURATIONS);
            if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE); // persist in case of prepare only, not create
            }
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            logger.error("Couldn't trigger prepare mo action on node for upgrade package MO  {} because {}", upMoFdn, unsupportedFragmentException.getMessage());
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.PREPARE, unsupportedFragmentException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            logger.error("Triggering of Prepare action on node for upgrade package MO {}  failed because {}", upMoFdn, exception);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            exceptionMessage = exceptionMessage.isEmpty() ? exception.getMessage() : exceptionMessage;
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.PREPARE);
            if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.PREPARE, exceptionMessage);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return new ExecuteResponse(actionResult.isTriggerSuccess(), upMoFdn, actionResult.getActionId());
    }

    /**
     * This method validate the action triggered status based on ExecuteResponse and persists activity results if action trigger failed on the node else proceed further.
     *
     * @param ecimUpgradeInfo
     * @param executeResponse
     * @param jobActivityInfo
     * @param jobLogList
     */

    public void doExecutePostValidationForPrepareAction(final EcimUpgradeInfo ecimUpgradeInfo, final ExecuteResponse executeResponse, final JobActivityInfo jobActivityInfo,
            final List<Map<String, Object>> jobLogList) {

        final boolean isPrepareTriggered = executeResponse.isActionTriggered();
        final String upMoFdn = executeResponse.getFdn();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        if (isPrepareTriggered) {
            logger.info("Prepare Activity is triggered on UpgradePackage MO {} with activityJobId {}", upMoFdn, activityJobId);
            activityUtils.recordEvent(SHMEvents.PREPARE_EXECUTE, nodeName, upMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, String.format(JobLogConstants.ACTION_TRIGGERED, jobActivityInfo.getActivityName())));
            systemRecorder.recordCommand(SHMEvents.PREPARE_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));

            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } else {
            logger.error("Action trigger on Upgrade Package MO {} failed as prepareActionTriggered returned as {}", upMoFdn, isPrepareTriggered);
            failActivityWhileTriggeringAction(ecimUpgradeInfo, upMoFdn, jobLogList, jobActivityInfo, PREPARE_ACTIVITY_NAME);
        }
    }

    private void failActivityWhileTriggeringAction(final EcimUpgradeInfo ecimUpgradeInfo, final String moFdn, final List<Map<String, Object>> jobLogList, final JobActivityInfo jobActivityInfo,
            final String activityName) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final long mainJobId = neJobStaticData.getMainJobId();
        final String nodeName = neJobStaticData.getNodeName();

        activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);

        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED, activityName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        systemRecorder.recordCommand(SHMEvents.PREPARE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, moFdn,
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, new HashMap<String, Object>());
    }

    public ExecuteResponse triggerCreateAction(final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo, final List<Map<String, Object>> jobLogList)
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        logger.info("Inside triggerCreateAction in execute() with node : {}", ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String swmMoFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);
        systemRecorder.recordCommand(SHMEvents.CREATE_SERVICE, CommandPhase.STARTED, nodeName, swmMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));
        activityUtils.subscribeToMoNotifications(swmMoFdn, activityJobId, jobActivityInfo);

        final ExecuteResponse executeResponse = performCreateActionOnNode(ecimUpgradeInfo, swmMoFdn, jobLogList);
        return executeResponse;
    }

    private ExecuteResponse performCreateActionOnNode(final EcimUpgradeInfo ecimUpgradeInfo, final String swmMoFdn, final List<Map<String, Object>> jobLogList) {
        logger.debug("Entered into performCreateActionOnNode with swmMoFdn : {} and nodeName : {}", swmMoFdn, ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        String jobLogMessage = "";
        ActionResult actionResult = new ActionResult();
        try {
            actionResult = upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            logger.error("Couldn't trigger prepare mo action on node for upgrade package MO  {} because {}", swmMoFdn, unsupportedFragmentException);
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimSwMConstants.CREATE_UPGRADE_PACKAGE, unsupportedFragmentException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            logger.error("Triggering of Prepare action on node for upgrade package MO {}  failed because {}", swmMoFdn, exception.getMessage());
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimSwMConstants.CREATE_UPGRADE_PACKAGE, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED + exception.getMessage(), EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return new ExecuteResponse(actionResult.isTriggerSuccess(), swmMoFdn, actionResult.getActionId());
    }

    /**
     * This method validate the action triggered status based on ExecuteResponse and persists activity results if action trigger failed on the node else proceed further.
     *
     * @param ecimUpgradeInfo
     * @param executeResponse
     * @param jobActivityInfo
     * @param jobLogList
     */
    public void doExecutePostValidationForCreateAction(final EcimUpgradeInfo ecimUpgradeInfo, final ExecuteResponse executeResponse, final JobActivityInfo jobActivityInfo,
            final List<Map<String, Object>> jobLogList) {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String swmMoFdn = executeResponse.getFdn();
        final int actionId = executeResponse.getActionId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();
        try {
            if (actionId > 0) {
                logger.info("Create Upgrade Package action is triggered on SwM MO {} with activityJobId {}", swmMoFdn, activityJobId);
                final String neType = networkElementRetrievalBean.getNeType(nodeName);
                final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.name(), PREPARE_ACTIVITY_NAME);
                final String jobLogMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, EcimSwMConstants.CREATE_UPGRADE_PACKAGE, activityTimeout);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

                final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, CREATE_ACTIVITY_NAME);
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_ID, Long.toString(actionId));

                activityUtils.recordEvent(SHMEvents.CREATE_EXECUTE, nodeName, swmMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
                systemRecorder.recordCommand(SHMEvents.CREATE_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, swmMoFdn,
                        activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE));

                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            } else {
                logger.error("Creation of Upgrade Package failed on SwM MO  {} because action ID found : {}", swmMoFdn, actionId);
                failActivityWhileTriggeringAction(ecimUpgradeInfo, swmMoFdn, jobLogList, jobActivityInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
            }
        } catch (final Exception ex) {
            logger.error("Exceotion occurred during post validation of create action on the SwmMo:{}. Exception is:", swmMoFdn, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() != null ? ex.getMessage() : "" : exceptionMessage;
            String logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimSwMConstants.CREATE_UPGRADE_PACKAGE, exceptionMessage);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            failActivityWhileTriggeringAction(ecimUpgradeInfo, swmMoFdn, jobLogList, jobActivityInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
        }
    }

    public void failActivity(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final String businessKey) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, new HashMap<String, Object>());

        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }

    public void processAVCNotifications(final Notification notification, final JobActivityInfo jobActivityInfo) {
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);

            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            final String actionTriggered = ecimUpgradeInfo.getActionTriggered();
            logger.info("actionTriggered is {}", actionTriggered);
            final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            logger.info("ModifiedAttr in PrepareService processNotification is : {}", modifiedAttributes);
            logger.debug("ActionTriggered in Prepare Service processNotification() method is : {}", actionTriggered);
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
            final String moFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            switch (actionTriggered) {
            case CREATE_ACTIVITY_NAME:
                handleNotificationForCreateAction(ecimUpgradeInfo, modifiedAttributes, notificationTime, moFdn, jobActivityInfo);
                break;
            case PREPARE_ACTIVITY_NAME:
                handleNotificationForPrepareAction(ecimUpgradeInfo, modifiedAttributes, notificationTime, moFdn, jobActivityInfo);
                break;
            case EcimSwMConstants.CANCEL_UPGRADE_PACKAGE:
                handleNotificationForCancel(ecimUpgradeInfo, modifiedAttributes, notificationTime, moFdn, jobActivityInfo);
                break;
            default:
                logger.warn("None of the required actions are triggered. ActivityJobId {}, actionTriggeredProperty: {}", activityJobId, actionTriggered);
            }
        } catch (final UnsupportedFragmentException e) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final JobDataNotFoundException ex) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final MoNotFoundException ex) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, ex.getMessage(), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }

    }

    private void handleNotificationForCreateAction(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, AttributeChangeData> modifiedAttributes, final Date notificationTime, final String moFdn,
            final JobActivityInfo jobActivityInfo) throws UnsupportedFragmentException {
        logger.debug("Inside Prepare Service handleNotificationForCreateAction with moFdn : {}", moFdn);
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final AsyncActionProgress progressReportForCreateActivity = upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes);
        final boolean isValidNotification = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReportForCreateActivity, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);

        if (isValidNotification) {
            logInvalidNotificationInfo(activityJobId, modifiedAttributes, CREATE_ACTIVITY_NAME);
            return;
        }
        processNotificationForCreateActivity(ecimUpgradeInfo, progressReportForCreateActivity, notificationTime, moFdn, jobActivityInfo);
    }

    private void handleNotificationForPrepareAction(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, AttributeChangeData> modifiedAttributes, final Date notificationTime, final String moFdn,
            final JobActivityInfo jobActivityInfo) throws UnsupportedFragmentException {
        logger.debug("Inside Prepare Service handleNotificationForPrepareAction with modifiedAttributes = {} and moFDN : {}", modifiedAttributes, moFdn);
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final AsyncActionProgress progressReportForPrepareActivity = upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes);
        final boolean isInvalidNotification = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReportForPrepareActivity, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
        if (isInvalidNotification) {
            logInvalidNotificationInfo(activityJobId, modifiedAttributes, PREPARE_ACTIVITY_NAME);
            return;
        }
        processNotificationForPrepareActivity(ecimUpgradeInfo, progressReportForPrepareActivity, notificationTime, moFdn, jobActivityInfo);
    }

    private void handleNotificationForCancel(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, AttributeChangeData> modifiedAttributes, final Date notificationTime, final String moFdn,
            final JobActivityInfo jobActivityInfo) throws UnsupportedFragmentException {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final AsyncActionProgress progressReportForPrepareActivity = upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes);
        final boolean isInvalidNotification = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReportForPrepareActivity, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE);

        if (isInvalidNotification) {
            logInvalidNotificationInfo(activityJobId, modifiedAttributes, PREPARE_ACTIVITY_NAME);
            return;
        }
        processNotificationForCancel(ecimUpgradeInfo, progressReportForPrepareActivity, notificationTime, moFdn, jobActivityInfo);
    }

    private void logInvalidNotificationInfo(final long activityJobId, final Map<String, AttributeChangeData> modifiedAttributes, final String activityName) {
        logger.warn("Discarding as invalid notification as it doesn't contain progressReport attribute with {} activity action name. ActivityJobId {}; Notification Recieved : {}", activityName,
                activityJobId, modifiedAttributes);
    }

    private void processNotificationForCreateActivity(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final Date notificationTime, final String moFdn,
            final JobActivityInfo jobActivityInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActionStateType state = progressReport.getState();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        JobLogLevel jobLogLevel;
        switch (state) {
        case RUNNING:
            reportCreateActivityIsOngoing(progressReport, notificationTime, activityJobId);
            break;
        case FINISHED:
            jobLogLevel = JobLogLevel.INFO;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            reportCreateActivityIsFinished(progressReport, moFdn, ecimUpgradeInfo, jobActivityInfo);
            break;
        default:
            logger.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
    }

    private void processNotificationForPrepareActivity(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final Date notificationTime, final String moFdn,
            final JobActivityInfo jobActivityInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActionStateType state = progressReport.getState();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        JobLogLevel jobLogLevel;
        switch (state) {
        case RUNNING:
            reportPrepareActivityIsOngoing(progressReport, notificationTime, ecimUpgradeInfo);
            break;
        case FINISHED:
            jobLogLevel = JobLogLevel.INFO;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            reportPrepareActivityIsFinished(progressReport, moFdn, ecimUpgradeInfo, jobActivityInfo);
            break;
        default:
            logger.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
    }

    private void processNotificationForCancel(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final Date notificationTime, final String moFdn,
            final JobActivityInfo jobActivityInfo) {
        final ActionStateType state = progressReport.getState();

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();

        switch (state) {
        case RUNNING:
            reportCancelActivityIsOngoing(progressReport, notificationTime, ecimUpgradeInfo);
            break;
        case FINISHED:
            reportCancelActivityIsFinished(progressReport, moFdn, ecimUpgradeInfo, jobActivityInfo);
            break;

        default:
            logger.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }

    }

    private void reportCreateActivityIsOngoing(final AsyncActionProgress progressReport, final Date notificationTime, final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }

    private void reportCreateActivityIsFinished(final AsyncActionProgress progressReport, final String moFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();

        if (ActionResultType.SUCCESS != progressReport.getResult()) {
            final JobLogLevel jobLogLevel = JobLogLevel.ERROR;
            final JobResult jobResult = JobResult.FAILED;
            final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimSwMConstants.CREATE_UPGRADE_PACKAGE) + progressReport.getResultInfo();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
            activityUtils.recordEvent(SHMEvents.CREATE_UPGRADE_PACKAGE_PROCESS_NOTIFICATION, nodeName, moFdn,
                    "SHM:" + activityJobId + ":" + nodeName + ":" + EcimSwMConstants.CREATE_UPGRADE_PACKAGE + jobResult.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
            final boolean isJobResultPersisted = activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList,
                    CREATE_UPGRADEPKG_END_PROGRESS_PERCENTAGE);
            activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);
            if (isJobResultPersisted) {
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, CREATE_ACTIVITY_NAME, null);
            }
        }
    }

    private void reportPrepareActivityIsOngoing(final AsyncActionProgress progressReport, final Date notificationTime, final EcimUpgradeInfo ecimUpgradeInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
        final Double currentProgressPercentage = CREATE_UPGRADEPKG_END_PROGRESS_PERCENTAGE + ((double) progressReport.getProgressPercentage()) / 2;
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(ecimUpgradeInfo.getActivityJobId(), jobPropertyList, jobLogList, currentProgressPercentage);
        progressPercentageCache.bufferNEJobs(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
    }

    private void reportPrepareActivityIsFinished(final AsyncActionProgress progressReport, final String moFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityStartTime = neJobStaticData.getActivityStartTime();
        String jobLogMessage;
        JobResult jobResult;
        JobLogLevel jobLogLevel;
        activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);
        final ActionResultType result = progressReport.getResult();

        if (ActionResultType.SUCCESS == result) {
            jobLogLevel = JobLogLevel.INFO;
            jobResult = JobResult.SUCCESS;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.PREPARE);
        } else {
            jobLogLevel = JobLogLevel.ERROR;
            jobResult = JobResult.FAILED;
            jobLogMessage = (progressReport.getResultInfo() != null && !progressReport.getResultInfo().isEmpty())
                    ? String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.PREPARE) + " Reason : " + progressReport.getResultInfo()
                    : String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.PREPARE);
        }
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
        activityUtils.recordEvent(SHMEvents.PREPARE_PROCESS_NOTIFICATION, nodeName, moFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + progressReport.getActionName() + jobResult.getJobResult());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isJobResultPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, PREPARE_ACTIVITY_NAME, new HashMap<String, Object>());
        }
    }

    private void reportCancelActivityIsOngoing(final AsyncActionProgress progressReport, final Date notificationTime, final EcimUpgradeInfo ecimUpgradeInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(ecimUpgradeInfo.getActivityJobId(), jobPropertyList, jobLogList, progressReport.getProgressPercentage());
        progressPercentageCache.bufferNEJobs(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
    }

    private void reportCancelActivityIsFinished(final AsyncActionProgress progressReport, final String moFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();

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
        activityUtils.recordEvent(SHMEvents.CANCEL_NOTIFICATION, nodeName, moFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + progressReport.getActionName() + jobResult.getJobResult());

        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, new HashMap<String, Object>());

        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }

    public void processCreateNotifications(final Notification notification, final JobActivityInfo jobActivityInfo) {
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

            //Step1 : Get UpgradePackage MO Fdn from the received notification
            final String upgradePackageMOFdnReceivedFromNotification = notification.getDpsDataChangedEvent().getFdn();

            logger.info("Received create notification for node {} with upgradePackageMoFdn {}", nodeName, upgradePackageMOFdnReceivedFromNotification);

            //Step2 : Fetch filePath attribute from UpgradePackage MO
            final String uri = upMoServiceRetryProxy.getUriFromUpgradePackageFdn(nodeName, upgradePackageMOFdnReceivedFromNotification);

            //Step3 : Retrieve the filePath stored in Job Properties
            final String filePath = upMoServiceRetryProxy.getFilePath(ecimUpgradeInfo);
            //Step4 : Compare the filePath from the retrieved MO with the filePath stored in Job Properties. If both matches, then process the notification
            logger.info(
                    "For node {}, compare uri received from UpgradePackage MO {} and relative file path stored in Job Property {} to determine whether received create notification is for the package created by us or someone else.",
                    nodeName, uri, filePath);
            if (uri != null && uri.endsWith(filePath)) {
                String swmMoFdn = "";
                try {
                    swmMoFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);

                    //Unsubscribe from Notifications
                    activityUtils.unSubscribeToMoNotifications(swmMoFdn, activityJobId, jobActivityInfo);
                } catch (MoNotFoundException | UnsupportedFragmentException | SoftwarePackageNameNotFound | SoftwarePackagePoNotFound | ArgumentBuilderException exception) {
                    logger.warn("Unable to get SwM MO Fdn for node {} because {}. This will impact in unsubscription from recieving notifications.", nodeName, exception.getMessage());
                }
                //Update Job Log : "Create Upgrade Package is successfully completed."
                final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimSwMConstants.CREATE_UPGRADE_PACKAGE), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

                //Notify WFS.
                final Map<String, Object> processVariables = new HashMap<String, Object>();
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
                activityUtils.sendNotificationToWFS(ecimUpgradeInfo.getNeJobStaticData(), activityJobId, CREATE_ACTIVITY_NAME, processVariables);
            }
        } catch (final Exception ex) {
            logger.warn("Failed to process Create Upgrade package notifications for node {} because {}. This will impact in unsubscription from recieving notifications.", nodeName, ex.getMessage());
        }

    }

    public boolean validateActionProgress(final AsyncActionProgress asyncActionProgress, final String nodeName, final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList,
            final String activityName) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        if (asyncActionProgress == null) {
            final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, activityName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            return false;
        }
        final boolean isValid = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, asyncActionProgress, activityName);
        if (isValid) {
            logger.warn("Timeout not related to action {} , activityId {} , nodeName {}", asyncActionProgress.getActionName(), ecimUpgradeInfo.getActivityJobId(), nodeName);
            return false;
        }
        return true;
    }

    public JobResult handleTimeoutForCreateActivity(final String swmMoFdn, final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress progressReport, final JobActivityInfo jobActivityInfo)
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        logger.info("Entered into handleTimeoutForCreateActivity() method with progressReport : {}", progressReport.getActionName());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = null;
        JobLogLevel jobLogLevel = null;
        String jobLogMessage = "";

        jobLogMessage = String.format(JobLogConstants.TIMEOUT, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = neJobStaticData.getNodeName();

        activityUtils.unSubscribeToMoNotifications(swmMoFdn, activityJobId, jobActivityInfo);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        activityUtils.recordEvent(SHMEvents.CREATE_UPGRADE_PACKAGE_TIME_OUT, nodeName, swmMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);

        final short actionIdFromJobProperties = (short) getActionIdFromJobProperties(ecimUpgradeInfo.getJobEnvironment().getActivityJobAttributes());
        if (actionIdFromJobProperties == progressReport.getActionId()) {
            final Map<String, Object> jobResultMap = getJobResultMap(progressReport);

            jobLogLevel = (JobLogLevel) jobResultMap.get(ActivityConstants.JOB_LOG_LEVEL);
            jobLogMessage = (String) jobResultMap.get(ActivityConstants.JOB_LOG_MESSAGE);
            jobResult = (JobResult) jobResultMap.get(ActivityConstants.JOB_RESULT);
        } else {
            if (upMoServiceRetryProxy.isUpgradePackageMoExists(ecimUpgradeInfo)) {
                jobLogLevel = JobLogLevel.INFO;
                jobResult = JobResult.SUCCESS;
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
            } else {
                jobLogLevel = JobLogLevel.ERROR;
                jobResult = JobResult.FAILED;
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimSwMConstants.CREATE_UPGRADE_PACKAGE) + progressReport.getResultInfo();
            }
        }

        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());

        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

        return jobResult;

    }

    @SuppressWarnings("unchecked")
    private int getActionIdFromJobProperties(final Map<String, Object> activityJobAttributes) {
        int actionId = -1;
        final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES);
        for (final Map<String, String> eachJobProperty : jobPropertyList) {
            if (ActivityConstants.ACTION_ID.equals(eachJobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                actionId = parseActionId(eachJobProperty.get(ActivityConstants.JOB_PROP_VALUE));
            }
        }
        return actionId;
    }

    private Map<String, Object> getJobResultMap(final AsyncActionProgress progressReport) {
        JobResult jobResult = JobResult.FAILED;
        JobLogLevel jobLogLevel = null;
        String jobLogMessage = "";
        switch (progressReport.getResult()) {
        case SUCCESS:
            jobLogLevel = JobLogLevel.INFO;
            jobResult = JobResult.SUCCESS;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, progressReport.getActionName());
            break;
        case FAILURE:
            jobLogLevel = JobLogLevel.ERROR;
            jobResult = JobResult.FAILED;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, progressReport.getActionName()) + progressReport.getResultInfo();
            break;
        case NOT_AVAILABLE:
            jobLogLevel = JobLogLevel.ERROR;
            jobResult = JobResult.FAILED;
            jobLogMessage = String.format(JobLogConstants.STILL_EXECUTING, progressReport.getActionName());
            break;
        }
        final Map<String, Object> jobResultMap = new HashMap<String, Object>();
        jobResultMap.put(ActivityConstants.JOB_RESULT, jobResult);
        jobResultMap.put(ActivityConstants.JOB_LOG_MESSAGE, jobLogMessage);
        jobResultMap.put(ActivityConstants.JOB_LOG_LEVEL, jobLogLevel);
        return jobResultMap;
    }

    private int parseActionId(final String actionId) {
        try {
            return Integer.parseInt(actionId);
        } catch (final NumberFormatException numberFormatException) {
            logger.error("Action Id persisted in db is not in number format. action ID : {}. Error Message : {}", actionId, numberFormatException.getMessage());
            return -1;
        }
    }

    public JobResult handleTimeoutForPrepareActivity(final String upMoFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo)
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        JobResult jobResult = null;

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo);
        jobResult = getActivityJobResult(ecimUpgradeInfo, upMoFdn, SHMEvents.PREPARE_TIME_OUT, nodeName, activityJobId);
        return jobResult;

    }

    private JobResult getActivityJobResult(final EcimUpgradeInfo ecimUpgradeInfo, final String upMoFdn, final String shmEvent, final String nodeName, final long activityJobId)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        JobResult jobResult = JobResult.FAILED;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        JobLogLevel jobLogLevel = null;

        final AsyncActionProgress progressReport = upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo);
        String jobLogMessage = String.format(JobLogConstants.TIMEOUT, progressReport.getActionName());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        activityUtils.recordEvent(shmEvent, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);

        final Map<String, Object> jobResultMap = getJobResultMap(progressReport);

        jobLogLevel = (JobLogLevel) jobResultMap.get(ActivityConstants.JOB_LOG_LEVEL);
        jobLogMessage = (String) jobResultMap.get(ActivityConstants.JOB_LOG_MESSAGE);
        jobResult = (JobResult) jobResultMap.get(ActivityConstants.JOB_RESULT);

        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return jobResult;
    }

    public JobResult handleTimeoutForCancel(final String upMoFdn, final EcimUpgradeInfo ecimUpgradeInfo, final JobActivityInfo jobActivityInfo)
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        JobResult jobResult = null;

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo);
        jobResult = getActivityJobResult(ecimUpgradeInfo, upMoFdn, SHMEvents.CANCEL_TIME_OUT, nodeName, activityJobId);
        return jobResult;
    }

    public JobResult handleExceptionForTimeout(final String jobLogMessage, final String errorMessage, final List<Map<String, Object>> jobLogList) {
        logger.error(errorMessage);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        return JobResult.FAILED;
    }

    public void precheckHandleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Integer precheckTimeout = activityTimeoutsService.getPrecheckTimeoutAsInteger();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.PREPARE, precheckTimeout), JobLogLevel.ERROR.toString()));
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }
}
