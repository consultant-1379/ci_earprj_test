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
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.api.Activity;
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
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.upgrade.api.UpgradePrecheckResponse;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.UpgradePackageState;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * 
 * @author tcsespo
 * 
 */
@EServiceQualifier("ECIM.UPGRADE.confirm")
@ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Profiled
@Traceable
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmService implements Activity, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmService.class);
    private static final String UPGRADE_PACKAGE_NOT_IN_EXPECTED_STATE = "Upgrade package is not in a expected state to proceed to Confirm";
    private static final String ACTIVITY_NAME = "confirm";

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceHelper;

    @Inject
    private EcimSwMUtils ecimSwMUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheck(long)
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.info("Entered ConfirmService Precheck with activity Job ID ::{} ", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the ConfirmService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.CONFIRM, exceptionMessage);
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        LOGGER.debug("Precheck completed in confirm activity for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

    private ActivityStepResultEnum precheck(final EcimUpgradeInfo ecimUpgradeInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();

        ecimSwMUtils.initializeActivityJobLogs(neJobStaticData.getNodeName(), ActivityConstants.CONFIRM, jobLogList);
        final UpgradePrecheckResponse UpgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);

        return UpgradePrecheckResponse.getActivityStepResultEnum();
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
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String neJobBusinessKey = "";
        String nodeName = "";
        JobStaticData jobStaticData = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId, neJobStaticData);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CONFIRM);
                return;
            }
            ecimSwMUtils.initializeActivityJobLogs(nodeName, ActivityConstants.CONFIRM, jobLogList);
            final UpgradePrecheckResponse upgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);
            jobLogList.clear();

            if (upgradePrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                final ExecuteResponse executeResponse = executeAction(ecimUpgradeInfo, upgradePrecheckResponse, jobLogList);
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
                final boolean isJobAttributesPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                if (!isJobAttributesPersisted) {
                    LOGGER.error("jobPropertyList:{} and jobLogList:{} are not persisted for activityJobId: {}", jobPropertyList, jobLogList, activityJobId);
                }
                doExecutePostValidation(ecimUpgradeInfo, executeResponse, jobLogList);
            } else {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CONFIRM);
            }
        }catch (final JobDataNotFoundException jex) {
            LOGGER.error("ConfirmService.execute-Unable to trigger an action on the node:{} with activityJobId: {}. Reason:{} ", nodeName, activityJobId, jex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CONFIRM);
        }
        catch (final Exception ex) {
            LOGGER.error("ConfirmService.execute-Unable to trigger an action on the node:{} with activityJobId: {}. Reason:  ", nodeName, activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, ActivityConstants.CONFIRM, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CONFIRM);
        }
    }

    private ExecuteResponse executeAction(final EcimUpgradeInfo ecimUpgradeInfo, final UpgradePrecheckResponse upgradePrecheckResponse, final List<Map<String, Object>> jobLogList) {

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final String upMOFdn = upgradePrecheckResponse.getUpMoFdn();
        String logMessage = "";
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        ActionResult actionResult = new ActionResult();
        ExecuteResponse executeResponse = new ExecuteResponse(actionResult.isTriggerSuccess(), upgradePrecheckResponse.getUpMoFdn(), actionResult.getActionId());
        try {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGERED, ActivityConstants.CONFIRM), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.recordEvent(SHMEvents.CONFIRM_EXECUTE, nodeName, upMOFdn, activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
            actionResult = upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CONFIRM_UPGRADE_PACKAGE);
            executeResponse = new ExecuteResponse(actionResult.isTriggerSuccess(), upgradePrecheckResponse.getUpMoFdn(), actionResult.getActionId());
            return executeResponse;
        } catch (final MoNotFoundException e) {
            LOGGER.error("MoNotFoundException occured in Confirm Execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, e);
            logMessage = String.format("MoNotFoundException occurred while triggering activity : %s ", ActivityConstants.CONFIRM);
        } catch (final UnsupportedFragmentException e) {
            LOGGER.error("UnsupportedFragmentException occured in Confirm Execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, e);
            logMessage = String.format("UnsupportedFragmentException occurred while triggering activity : %s ", ActivityConstants.CONFIRM);
        } catch (final ArgumentBuilderException e) {
            LOGGER.error("ArgumentBuilderException occured in Confirm Execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, e);
            logMessage = String.format("ArgumentBuilderException occurred while triggering activity  : %s ", ActivityConstants.CONFIRM);
        } catch (final Exception e) {
            LOGGER.error("An exception occurred in Confirm Execute with activityJobId : {} nodeName : {}. Re-Verifying on the node... Details are {}", activityJobId, nodeName, e);
            executeResponse = reVerifyActionStatusOnNode(ecimUpgradeInfo, executeResponse, jobLogList);
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        return executeResponse;
    }

    private UpgradePrecheckResponse getPrecheckResponse(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        String upMoFdn = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        try {
            upMoFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CONFIRM_UPGRADE_PACKAGE, ecimUpgradeInfo);
            final ActivityAllowed activityAllowed = upMoServiceRetryProxy.isActivityAllowed(ACTIVITY_NAME, ecimUpgradeInfo);
            if (activityAllowed.getActivityAllowed()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.CONFIRM), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                LOGGER.info("Confirm Precheck action is found to be successful with activityJobId : {} and the nodeName : {} ", activityJobId, nodeName);
            } else {
                final String logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CONFIRM, UPGRADE_PACKAGE_NOT_IN_EXPECTED_STATE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                LOGGER.error("Precheck of Confirm activity failed with activityJobId : {} and the nodeName : {} because Upmo state is not {} ", activityJobId, nodeName,
                        UpgradePackageState.WAITING_FOR_COMMIT);
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("MoNotFoundException is found in Confirm Precheck with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, e);
            jobLogUtil
                    .prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_DOES_NOT_EXIST, e.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException | MoActionAbortRetryException | NodeAttributesReaderException e) {
            LOGGER.error("Exception occurred in Confirm Precheck with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CONFIRM, e.getMessage()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred in Confirm Precheck with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? (ex.getMessage() != null ? ex.getMessage() : "") : exceptionMessage;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CONFIRM, exceptionMessage), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        LOGGER.debug("End of Confirm Activity precheck() with activityJobId {} and result {}", activityJobId, activityStepResultEnum);
        if (activityStepResultEnum == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity:{} is to be skipped/failed for the activityJobId: {}.", ActivityConstants.CONFIRM, activityJobId);
        }
        return new UpgradePrecheckResponse(upMoFdn, activityStepResultEnum);
    }

    /**
     * This method validate the action triggered status based on ExecuteResponse and persists activity results accordingly.
     * 
     * @param ecimUpgradeInfo
     * @param executeResponse
     * @param jobLogList
     * @throws MoNotFoundException
     */
    private void doExecutePostValidation(final EcimUpgradeInfo ecimUpgradeInfo, final ExecuteResponse executeResponse, final List<Map<String, Object>> jobLogList) {

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = JobResult.FAILED;
        String logMessage = "";

        if (executeResponse.isActionRunning() && !executeResponse.isActionTriggered()) {
            LOGGER.info("Confirm action is still running on the node:{}. So do not fail the activity and wait till handleTimeout.", ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        } else if (executeResponse.isActionTriggered()) {
            logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.CONFIRM);
            jobResult = JobResult.SUCCESS;
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, ACTIVITY_NAME);
            LOGGER.info("Execution of Confirm activity completed successfully with activityJobId : {} and nodeName : {} ", activityJobId, neJobStaticData.getNodeName());
            notifyWfsWithResult(activityJobId, ecimUpgradeInfo, logMessage, jobResult, jobLogList, jobPropertyList);
        } else {
            LOGGER.error("Execution of Confirm activity failed with activityJobId : {} and the nodeName : {} ", activityJobId, neJobStaticData.getNodeName());
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
            notifyWfsWithResult(activityJobId, ecimUpgradeInfo, logMessage, jobResult, jobLogList, jobPropertyList);
        }

    }

    /**
     * This method verifies the node status in case of any other than the predefined (defined in fragment) exceptions and update the result accordingly.
     * 
     * @param ecimUpgradeInfo
     * @param jobLogList
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    private ExecuteResponse reVerifyActionStatusOnNode(final EcimUpgradeInfo ecimUpgradeInfo, final ExecuteResponse executeResponse, final List<Map<String, Object>> jobLogList) {

        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        ExecuteResponse reverifyExecuteResponse = new ExecuteResponse(executeResponse.isActionTriggered(), executeResponse.getFdn(), executeResponse.getActionId());
        try {
            final UpgradePackageState upgradePkgState = upMoServiceRetryProxy.getUpgradePkgState(ecimUpgradeInfo);

            if (upgradePkgState == UpgradePackageState.WAITING_FOR_COMMIT) {
                executeResponse.setActionRunning(true);
                final String logMessage = String.format(JobLogConstants.EXECUTING, ActivityConstants.CONFIRM);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
            } else if (upgradePkgState == UpgradePackageState.COMMIT_COMPLETED) {
                reverifyExecuteResponse = new ExecuteResponse(true, executeResponse.getFdn(), executeResponse.getActionId());
            }
        } catch (final MoNotFoundException | UnsupportedFragmentException e) {
            LOGGER.error("Exception occured while fetching the UP State for activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId,
                    neJobStaticData.getNodeName(), e.getMessage());
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final Exception e) {
            LOGGER.error("An exception occurred while fetching the UP State for activityJobId : {} nodeName : {} and can not be proceed further. Details are :", activityJobId,
                    neJobStaticData.getNodeName(), e);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        return reverifyExecuteResponse;
    }

    /**
     * @param activityJobId
     * @param environmentAttributes
     * @param logMessage
     */
    private void notifyWfsWithResult(final long activityJobId, final EcimUpgradeInfo ecimUpgradeInfo, final String logMessage, final JobResult jobResult, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> propertyList) {
        activityUtils.prepareJobPropertyList(propertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        workflowInstanceHelper.sendActivate(ecimUpgradeInfo.getNeJobStaticData().getNeJobBusinessKey(), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.warn("Handling Confirm activity after reaching timeout value with activityJobId : {} ", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String nodeName = "";
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = handletimeout(ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and cannot be proceeded further with activityId {} and nodeName {}. Reason : ", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.CONFIRM, exceptionMessage);
        }
        LOGGER.debug("handleTimeout completed in ConfirmService with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    private ActivityStepResultEnum handletimeout(final EcimUpgradeInfo ecimUpgradeInfo) {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, ActivityConstants.CONFIRM), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        JobResult jobResult = JobResult.FAILED;
        String logMessage = "";
        String nodeName = "";
        long activityStartTime = 0;
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        try {
            // Read the state of the UP MO and if it is Commit Completed means it was the previous activity , then return as success else fail.
            final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();

            activityStartTime = neJobStaticData.getActivityStartTime();
            final boolean isActivityCompleted = upMoServiceRetryProxy.isActivityCompleted(ACTIVITY_NAME, ecimUpgradeInfo);
            if (isActivityCompleted) {
                logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.CONFIRM);
                jobResult = JobResult.SUCCESS;
                LOGGER.info("In Confirm Handle timeout , action found to be successful with activityJobId : {} and nodeName : {} ", activityJobId, nodeName);
                activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
            } else {
                LOGGER.error("In Confirm Handle timeout , action found to be not yet completed, so failing the activity with activityJobId : {} and NodeName : {} ", activityJobId, nodeName);
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
                activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("MoNotFoundException occured in Confirm Handle timeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, e);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
        } catch (final UnsupportedFragmentException e) {
            LOGGER.error("UnsupportedFragmentException occured in Confirm Handle timeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId,
                    nodeName, e);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
        } catch (final NodeAttributesReaderException e) {
            LOGGER.error("NodeAttributesReaderException occured in Confirm Handle timeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId,
                    nodeName, e);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
        } catch (final Exception e) {
            LOGGER.error("An exception occurred in Confirm Handle timeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are :", activityJobId, nodeName, e);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM);
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.toString(), propertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        LOGGER.info("Handletimeout completed in Confirm with the activityJobId : {} and the result is {}", activityJobId, activityStepResultEnum);
        if (activityStartTime != 0) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return activityStepResultEnum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        // Once confirm is initiated it can't be cancelled
        return null;
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
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck (long)
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        LOGGER.info("Entered into ConfirmService Precheck with activity Job ID ::{} ", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the VerifyService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.CONFIRM, exceptionMessage);
        }
        LOGGER.debug("ConfirmService.asyncPrecheck completed for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.CONFIRM, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {

        LOGGER.info("Entering into ConfirmService.precheckHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.CONFIRM);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout (long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Handling Confirm activity after reaching timeout value with activityJobId : {} ", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            activityStepResultEnum = handletimeout(ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred and cannot be proceeded further with activityId {} and nodeName {}. Reason : ", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.CONFIRM, exceptionMessage);
        }
        LOGGER.debug("asyncHandleTimeout completed in ConfirmService with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.CONFIRM, activityStepResultEnum);
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into ConfirmService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.CONFIRM);
        LOGGER.info("Exiting from ConfirmService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
    }

}
