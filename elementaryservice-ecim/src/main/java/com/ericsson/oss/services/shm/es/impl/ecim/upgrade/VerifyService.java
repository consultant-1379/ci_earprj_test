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
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
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
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
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

@EServiceQualifier("ECIM.UPGRADE.verify")
@ActivityInfo(activityName = "verify", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class VerifyService implements Activity, ActivityCallback, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyService.class);

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private EcimSwMUtils ecimSwMUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private CancelUpgradeService cancelUpgradeService;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    private static final String ACTIVITY_NAME = "verify";
    private static final String ACTION_STATUS = "actionStatus";

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheck(long)
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.info("Inside VerifyService precheck() with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the VerifyService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.VERIFY, exceptionMessage);
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        LOGGER.debug("Precheck completed in verify activity for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        return activityStepResult;
    }

    private ActivityStepResultEnum precheck(final EcimUpgradeInfo ecimUpgradeInfo) {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();

        ecimSwMUtils.initializeActivityJobLogs(neJobStaticData.getNodeName(), ActivityConstants.VERIFY, jobLogList);
        final UpgradePrecheckResponse upgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);

        return upgradePrecheckResponse.getActivityStepResultEnum();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#execute(long)
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String neJobBusinessKey = "";
        String nodeName = "";
        JobStaticData jobStaticData = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            nodeName = neJobStaticData.getNodeName();
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId, neJobStaticData);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.VERIFY);
                return;
            }
            ecimSwMUtils.initializeActivityJobLogs(neJobStaticData.getNodeName(), ActivityConstants.VERIFY, jobLogList);
            final UpgradePrecheckResponse upgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);
            jobLogList.clear();
            if (upgradePrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {

                final ExecuteResponse executeResponse = executeAction(ecimUpgradeInfo, upgradePrecheckResponse, jobLogList);
                doExecutePostValidation(ecimUpgradeInfo, executeResponse, jobLogList);
            } else {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.VERIFY);
            }
        } catch (final JobDataNotFoundException jex) {
            LOGGER.error("VerifyService.execute-Unable to trigger an action on the node:{} with activityJobId: {}. Reason:{} ", nodeName, activityJobId, jex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.VERIFY);
        } catch (final Exception ex) {
            LOGGER.error("VerifyService.execute-Unable to trigger an action on the node:{} with activityJobId: {}. Reason:  ", nodeName, activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, ActivityConstants.VERIFY, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.VERIFY);
        }
    }

    private ExecuteResponse executeAction(final EcimUpgradeInfo ecimUpgradeInfo, final UpgradePrecheckResponse precheckResponse, final List<Map<String, Object>> jobLogList) {
        LOGGER.debug("Inside VerifyService executeAction() with activityId {}", ecimUpgradeInfo.getActivityJobId());
        String logMessage = null;
        final String upMOFdn = precheckResponse.getUpMoFdn();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        boolean isActionInvoked = false;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        ActionResult actionResult = new ActionResult();
        try {
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(SHMEvents.VERIFY_EXECUTE, CommandPhase.STARTED, nodeName, upMOFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
            activityUtils.subscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));

            actionResult = upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE);
            LOGGER.debug("Trigger of VERIFY activity completed with activityId : {} and nodeName {}", activityJobId, nodeName);
            isActionInvoked = true;
            if (isActionInvoked) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
                final boolean isJobAttributesPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                if (!isJobAttributesPersisted) {
                    LOGGER.error("jobPropertyList:{} and jobLogList:{} are not persisted for activityJobId: {}", jobPropertyList, jobLogList, activityJobId);
                }
            }
            LOGGER.info("actionId for prepare activity:{}", actionResult.getActionId());
            return new ExecuteResponse(actionResult.isTriggerSuccess(), upMOFdn, actionResult.getActionId());
        } catch (final UnsupportedFragmentException | MoNotFoundException | ArgumentBuilderException | SoftwarePackagePoNotFound ex) {
            LOGGER.error("Exception occurred in VerifyService.executeAction and cannot be proceed further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            logMessage = ex.getMessage();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in VerifyService.executeAction and cannot be proceed with activity {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            logMessage = exceptionMessage.isEmpty() ? ex.getMessage() != null ? ex.getMessage() : "" : exceptionMessage;
        }
        activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.VERIFY, logMessage);
        if (logMessage.isEmpty()) {
            logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.VERIFY);
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        return new ExecuteResponse(actionResult.isTriggerSuccess(), upMOFdn, actionResult.getActionId());
    }

    private UpgradePrecheckResponse getPrecheckResponse(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String logMessage = null;
        final String nodeName = neJobStaticData.getNodeName();
        String upMOFdn = "";
        try {
            upMOFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo);
            final ActivityAllowed isActivityAllowed = upMoServiceRetryProxy.isActivityAllowed(ACTIVITY_NAME, ecimUpgradeInfo);
            if (isActivityAllowed.getActivityAllowed()) {
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                logMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.VERIFY);
                activityUtils.recordEvent(SHMEvents.VERIFY_PRECHECK, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            } else {
                LOGGER.info("VerifyService pre-validation failed as UpMo not found for the activityJobId {} and nodeName {}", activityJobId, nodeName);
                logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.VERIFY, "UpMo state is not Prepare completed");
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            if (activityStepResultEnum == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
            } else {
                LOGGER.debug("Skipping persisting step duration as activity:{} is to be skipped/failed for the activityJobId: {}.", ActivityConstants.VERIFY, activityJobId);
            }
            return new UpgradePrecheckResponse(upMOFdn, activityStepResultEnum);
        } catch (final MoNotFoundException | UnsupportedFragmentException | MoActionAbortRetryException | NodeAttributesReaderException ex) {
            LOGGER.error("Exception occurred in VerifyService pre-validation and cannot be proceed further with activityJobId {} and nodeName {}. Reason: ", activityJobId, nodeName, ex);
            logMessage = ex.getMessage();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred during VerifyService pre-validation and cannot be proceeded further with activityId {} and nodeName {}. Reason: ", activityJobId, nodeName, ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            logMessage = exceptionMessage.isEmpty() ? (ex.getMessage() != null ? ex.getMessage() : "") : exceptionMessage;
        }
        if (logMessage != null) {
            logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.VERIFY, logMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        return new UpgradePrecheckResponse(upMOFdn, activityStepResultEnum);
    }

    /**
     * This method validate the action triggered status based on ExecuteResponse and persists activity results if action trigger failed else proceed further.
     *
     * @param ecimUpgradeInfo
     * @param executeResponse
     * @param jobLogList
     * @throws MoNotFoundException
     */
    private void doExecutePostValidation(final EcimUpgradeInfo ecimUpgradeInfo, final ExecuteResponse executeResponse, final List<Map<String, Object>> jobLogList) throws MoNotFoundException {

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final String upMOFdn = executeResponse.getFdn();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        if (executeResponse.isActionTriggered()) {
            final String neType = networkElementRetrievalBean.getNeType(nodeName);

            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.name(), ACTIVITY_NAME);
            final String logMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.VERIFY, activityTimeout);

            systemRecorder.recordCommand(SHMEvents.VERIFY_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upMOFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
            activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, ActivityConstants.VERIFY);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } else {
            activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.VERIFY);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            systemRecorder.recordCommand(SHMEvents.VERIFY_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upMOFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
            activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), ActivityConstants.VERIFY);

            LOGGER.info("Unable to trigger verify action with activityJobId {} and nodeName {}", activityJobId, nodeName);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Handling verify activity after reaching timeout value with activityJobId {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String upMOFdn = "";
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            upMOFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            activityStepResultEnum = handleTimeout(upMOFdn, ecimUpgradeInfo);

        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and can not be proceeded further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.VERIFY, exceptionMessage);
            activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        LOGGER.debug("Handletimeout completed in VerifyService with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResult);
        return activityStepResult;
    }

    private ActivityStepResultEnum handleTimeout(final String upMOFdn, final EcimUpgradeInfo ecimUpgradeInfo) {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        String errorMessage = null;
        try {
            activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            final AsyncActionProgress asyncActionProgress = upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo);
            final boolean isValid = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, asyncActionProgress, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE);
            if (isValid) {
                LOGGER.warn("Timeout not related to action {} , activityId {} , nodeName {}", asyncActionProgress.getActionName(), activityJobId, nodeName);
                return null;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, ActivityConstants.VERIFY), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final ActivityStepResult activityStepResult = processHandleTimeout(ecimUpgradeInfo, asyncActionProgress, upMOFdn, jobLogList, propertyList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
            return activityStepResult.getActivityResultEnum();
        } catch (final UnsupportedFragmentException ex) {
            LOGGER.error("UnsupportedFragment occurred and can not be proceeded further with activityId {} and nodeName {}.Reason : {}", activityJobId, nodeName, ex);
            errorMessage = ex.getMessage();
        } catch (final MoNotFoundException ex) {
            LOGGER.error("MoNotFoundException occurred and can not be proceeded further with activityId {} and nodeName {}. Reason: {}", activityJobId, nodeName, ex);
            errorMessage = ex.getMessage();
        } catch (final NodeAttributesReaderException ex) {
            LOGGER.error("NodeAttributesReaderException occurred in VerifyService.handleTimeout and can not be proceeded further with activityId {} and nodeName {}.Reason: ", activityJobId, nodeName,
                    ex);
            errorMessage = ex.getMessage();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and can not be proceeded further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = ex.getMessage();
            }
        }
        activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.VERIFY);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED, propertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        LOGGER.debug("Acitivity failed in VerifyService.handletimeout with activityJobId {} , nodeName {}  and failure reason", activityJobId, nodeName, errorMessage);
        return ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.info("Inside VerifyService cancel() with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = cancelUpgradeService.cancel(activityJobId, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE);
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification (com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM - upgrade - Verify Service - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("ECIM - upgrade - Verify Service - - Discarding non-AVC notification.");
            return;
        }

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        LOGGER.info("Inside VerifyService processNotification for notification {} with the activityJobId {}", notification, activityJobId);
        EcimUpgradeInfo ecimUpgradeInfo = null;
        String errorMessage = null;
        String nodeName = null;
        try {
            ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            final String upMOFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.info("Inside Verify Service processNotification with activityJobId :{} and nodeName : {}. Modified Attributes are : {}", activityJobId, nodeName, modifiedAttributes);
            final AsyncActionProgress asyncActionProgress = upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes);

            final boolean isValidNotificationReceived = upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, asyncActionProgress, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE);
            if (isValidNotificationReceived) {
                final String notificationName = asyncActionProgress != null ? asyncActionProgress.getActionName() : "Its a state notification";
                LOGGER.warn("Discarding invalid notification received for action {} , activityJobId {} , nodeName {} and expecting for action verify or cancel", notificationName, activityJobId,
                        nodeName);
                return;
            }
            processValidNotification(notificationSubject, asyncActionProgress, upMOFdn, ecimUpgradeInfo);
            return;
        } catch (final UnsupportedFragmentException ex) {
            LOGGER.error("UnsupportedFragmentException occurred in VerifyService.processNotification and can not be proceeded further for the activityJobId {},nodeName {}.Reason: {}", activityJobId,
                    nodeName, ex);
            errorMessage = ex.getMessage();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred inVerifyService.processNotification and can not be proceeded further for the activityJobId {},nodeName {}.Reason {}", activityJobId, nodeName, ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = ex.getMessage();
            }
        }
        final String logMessage = String.format(JobLogConstants.FAILURE_REASON, errorMessage);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        LOGGER.info("ProcessNotification completed for the verify activity with Id {} and nodeName {}", activityJobId, activityJobId);
    }

    private void processValidNotification(final NotificationSubject notificationSubject, final AsyncActionProgress asyncActionProgress, final String upMOFdn, final EcimUpgradeInfo ecimUpgradeInfo)
            throws JobDataNotFoundException {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final Map<String, Object> actionResult = upMoServiceRetryProxy.isActionCompleted(asyncActionProgress);
        final boolean isActionCompleted = (boolean) actionResult.get(ACTION_STATUS);

        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, asyncActionProgress.toString(), notificationSubject.getTimeStamp(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        if (isActionCompleted) {
            final long activityStartTime = ecimUpgradeInfo.getNeJobStaticData().getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
            final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
            final JobResult jobResult = (JobResult) actionResult.get(ActivityConstants.JOB_RESULT);
            final String logmessage = (String) actionResult.get(ActivityConstants.JOB_LOG_MESSAGE);
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), propertyList);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logmessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            if (ACTIVITY_NAME.equals(asyncActionProgress.getActionName())) {
                activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logmessage));
            } else {
                activityUtils.recordEvent(SHMEvents.VERIFY_CANCEL, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logmessage));
            }
            activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            LOGGER.info("{} activity completed and notifying to WFS now for the activity with activityId {} and nodeName {} .", asyncActionProgress.getActionName(), activityJobId, nodeName);
            final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
            if (isJobResultPersisted) {
                activityUtils.sendNotificationToWFS(ecimUpgradeInfo.getNeJobStaticData(), ecimUpgradeInfo.getActivityJobId(), ActivityConstants.VERIFY, new HashMap<String, Object>());
            }
        } else {
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, (double) asyncActionProgress.getProgressPercentage());
            progressPercentageCache.bufferNEJobs(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        }
        return;
    }

    private ActivityStepResult processHandleTimeout(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress asyncActionProgress, final String upMOFdn,
            final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {

        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final Map<String, Object> actionResult = upMoServiceRetryProxy.isActionCompleted(asyncActionProgress);
        final String logmessage = (String) actionResult.get(ActivityConstants.JOB_LOG_MESSAGE);
        final JobResult jobResult = (JobResult) actionResult.get(ActivityConstants.JOB_RESULT);
        if (JobResult.SUCCESS.getJobResult().equals(jobResult.getJobResult())) {
            LOGGER.debug("In {} action handle timeout , action found to be successful for the activityJobId {} and nodeName {}. ", asyncActionProgress.getActionName(), activityJobId, nodeName);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            if (ACTIVITY_NAME.equalsIgnoreCase(asyncActionProgress.getActionName())) {
                activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, "Timeout Success"));
            } else {
                activityUtils.recordEvent(SHMEvents.VERIFY_CANCEL, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, "Timeout Success"));
            }
        } else {
            LOGGER.error("In {} action handle timeout , action found to be not yet completed, so Failing the activity now for the activityJobId {} and nodeName {}.",
                    asyncActionProgress.getActionName(), activityJobId, nodeName);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            if (ACTIVITY_NAME.equalsIgnoreCase(asyncActionProgress.getActionName())) {
                activityUtils.recordEvent(SHMEvents.VERIFY_EXECUTE, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, "Timeout failure"));
            } else {
                activityUtils.recordEvent(SHMEvents.VERIFY_CANCEL, nodeName, upMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, "Timeout failure"));
            }
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logmessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), propertyList);
        return activityStepResult;
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
        LOGGER.debug("Entered into VerifyService precheck() with activityJobId {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the VerifyService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.VERIFY, exceptionMessage);
        }
        LOGGER.debug("VerifyService.asyncPrecheck completed for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into VerifyService.precheckHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.VERIFY);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout(long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Entered into VerifyService.asyncHandleTimeout for the activityJobId: {}", activityJobId);

        String upMOFdn = "";
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        try {

            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
            upMOFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo);
            activityStepResultEnum = handleTimeout(upMOFdn, ecimUpgradeInfo);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and cannot be proceeded further with activityId {} and nodeName {}. Reason : ", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.VERIFY, exceptionMessage);
            activityUtils.unSubscribeToMoNotifications(upMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        }
        LOGGER.debug("asyncHandleTimeout completed in VerifyService with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY, activityStepResultEnum);
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into VerifyService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.VERIFY);
    }
}
