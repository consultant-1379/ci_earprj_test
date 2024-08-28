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
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.UpgradeJobConfigurationListener;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.upgrade.api.UpgradePrecheckResponse;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.UpgradePackageState;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("ECIM.UPGRADE.prepare")
@ActivityInfo(activityName = "prepare", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PrepareService implements Activity, ActivityCallback, AsynchronousActivity {

    private static final Logger logger = LoggerFactory.getLogger(PrepareService.class);

    private static final String UPGRADE_PACKAGE_NOT_IN_GOOD_STATE = "upgrade package is not in a state to be prepared";
    private static final String NO_ACTION_TRIGGERED = "None of the required actions are triggered.";
    private static final String PREPARE_ACTIVITY_NAME = "prepare";
    private static final String CREATE_ACTIVITY_NAME = "createUpgradePackage";

    @Inject
    private EcimSwMUtils ecimSwmUtils;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private CancelUpgradeService cancelUpgradeService;

    @Inject
    protected JobPropertyUtils jobPropertyUtils;

    @Inject
    private PrepareServiceHandler prepareServiceHandler;

    @Inject
    private UpgradeJobConfigurationListener upgradeJobConfigurationListener;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        logger.debug("Inside PrepareService precheck() with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = "";
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            logger.error("An exception occurred while pre validing the PrepareService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.PREPARE, exceptionMessage);
        }
        logger.debug("Precheck completed in prepare activity for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    private ActivityStepResultEnum precheck(final EcimUpgradeInfo ecimUpgradeInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        ecimSwmUtils.initializeActivityJobLogs(neJobStaticData.getNodeName(), ActivityConstants.PREPARE, jobLogList);
        final UpgradePrecheckResponse upgradePrecheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);
        return upgradePrecheckResponse.getActivityStepResultEnum();
    }

    private boolean isPrepareActivityCompleted(final UpgradePackageState upgradePackageState) {
        if (upgradePackageState != null && (upgradePackageState == UpgradePackageState.PREPARE_COMPLETED || upgradePackageState == UpgradePackageState.WAITING_FOR_COMMIT
                || upgradePackageState == UpgradePackageState.COMMIT_COMPLETED)) {
            return true;
        }
        return false;
    }

    @Asynchronous
    @Override
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String neJobBusinessKey = "";
        String nodeName = "";
        boolean isActionInvoked = false;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, PREPARE_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, PREPARE_ACTIVITY_NAME);
                return;
            }
            ecimSwmUtils.initializeActivityJobLogs(nodeName, ActivityConstants.PREPARE, jobLogList);
            final UpgradePrecheckResponse precheckResponse = getPrecheckResponse(ecimUpgradeInfo, jobLogList);
            jobLogList.clear();
            if (precheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                if (upMoServiceRetryProxy.isUpgradePackageMoExists(ecimUpgradeInfo)) {
                    final ExecuteResponse executeResponse = executePrepareAction(precheckResponse.getUpMoFdn(), ecimUpgradeInfo, jobLogList);
                    isActionInvoked = true;
                    prepareServiceHandler.doExecutePostValidationForPrepareAction(ecimUpgradeInfo, executeResponse, activityUtils.getActivityInfo(activityJobId, this.getClass()), jobLogList);
                } else {
                    if (CREATE_ACTIVITY_NAME.equals(ecimUpgradeInfo.getActionTriggered())) {
                        throw new MoNotFoundException("Upgrade Package Mo is not found");
                    } else {
                        final ExecuteResponse executeResponse = executeCreateAction(ecimUpgradeInfo, precheckResponse, jobLogList);
                        prepareServiceHandler.doExecutePostValidationForCreateAction(ecimUpgradeInfo, executeResponse, activityUtils.getActivityInfo(activityJobId, this.getClass()), jobLogList);
                    }
                }
            } else if (precheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION) {
                activityUtils.skipActivity(activityJobId, neJobStaticData, jobLogList, neJobBusinessKey, PREPARE_ACTIVITY_NAME);
            } else {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, PREPARE_ACTIVITY_NAME);
            }
            if (isActionInvoked) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
                final boolean isJobAttributesPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                if (!isJobAttributesPersisted) {
                    logger.error("jobPropertyList:{} and jobLogList:{} are not persisted for activityJobId: {}", jobPropertyList, jobLogList, activityJobId);
                }
            }

        } catch (final MoNotFoundException moNotFoundException) {
            logger.error("moNotFoundException in execute() for the node:{} with activityJobId:{}. Reason: {}", nodeName, activityJobId, moNotFoundException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_DOES_NOT_EXIST, moNotFoundException.getMessage()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, PREPARE_ACTIVITY_NAME);
        } catch (final Exception ex) {
            logger.error("PrepareService.execute-Unable to trigger an action on the node:{} with activityJobId: {}. Reason:  ", nodeName, activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, PREPARE_ACTIVITY_NAME, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, PREPARE_ACTIVITY_NAME);
        }
    }

    private ExecuteResponse executePrepareAction(final String upMoFdn, final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList)
            throws UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        try {
            prepareServiceHandler.markActivityWithTriggeredAction(activityJobId, PREPARE_ACTIVITY_NAME);
            prepareServiceHandler.updateSmrsDetailsIfRequired(ecimUpgradeInfo, jobLogList);
            final ExecuteResponse executeResponse = prepareServiceHandler.triggerPrepareAction(upMoFdn, ecimUpgradeInfo, activityUtils.getActivityInfo(activityJobId, this.getClass()), jobLogList);
            return executeResponse;
        } catch (final Exception ex) {
            String jobLogMessage = NodeMediationServiceExceptionParser.getReason(ex);
            jobLogMessage = jobLogMessage.isEmpty() ? ex.getMessage() : jobLogMessage;
            String logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.PREPARE, jobLogMessage);
            if (jobLogMessage == null || jobLogMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.PREPARE);
            }
            handleException(ecimUpgradeInfo, jobLogList, logMessage, ex);
        }
        return new ExecuteResponse(false, upMoFdn, -1);

    }

    private ExecuteResponse executeCreateAction(final EcimUpgradeInfo ecimUpgradeInfo, final UpgradePrecheckResponse precheckResponse, final List<Map<String, Object>> jobLogList) {
        String jobLogMessage = "";
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        try {
            prepareServiceHandler.markActivityWithTriggeredAction(activityJobId, CREATE_ACTIVITY_NAME);
            final ExecuteResponse executeResponse = prepareServiceHandler.triggerCreateAction(ecimUpgradeInfo, activityUtils.getActivityInfo(activityJobId, this.getClass()), jobLogList);

            return executeResponse;
        } catch (final SoftwarePackageNameNotFound ex) {
            handleException(ecimUpgradeInfo, jobLogList, String.format(JobLogConstants.JOB_CONFIGURATION_ERROR, ex.getMessage(), nodeName), ex);
        } catch (final SoftwarePackagePoNotFound ex) {
            handleException(ecimUpgradeInfo, jobLogList, String.format(JobLogConstants.SW_PACKAGE_NOT_FOUND, nodeName), ex);
        } catch (final MoNotFoundException ex) {
            handleException(ecimUpgradeInfo, jobLogList, String.format(JobLogConstants.MO_DOES_NOT_EXIST, ex.getMessage()), ex);
        } catch (final UnsupportedFragmentException ex) {
            handleException(ecimUpgradeInfo, jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), ex);
        } catch (final ArgumentBuilderException e) {
            jobLogMessage = String.format("ArgumentBuilderException occurred while triggering activity  : %s ", CREATE_ACTIVITY_NAME);
            handleException(ecimUpgradeInfo, jobLogList, jobLogMessage, e);
        } catch (final Exception ex) {
            jobLogMessage = NodeMediationServiceExceptionParser.getReason(ex);
            jobLogMessage = jobLogMessage.isEmpty() ? (ex.getMessage() != null ? ex.getMessage() : "") : jobLogMessage;
            String logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, CREATE_ACTIVITY_NAME, jobLogMessage);
            if (jobLogMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, CREATE_ACTIVITY_NAME);
            }
            handleException(ecimUpgradeInfo, jobLogList, logMessage, ex);
        }
        return new ExecuteResponse(false, precheckResponse.getUpMoFdn(), -1);
    }

    private void handleException(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList, final String exceptionMessage, final Exception ex) {
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        logger.error("Exception occured in PrepareService.execute with activityJobId : {} nodeName : {} and cannot be proceed further. Details are {}", ecimUpgradeInfo.getActivityJobId(),
                neJobStaticData.getNodeName(), ex);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, exceptionMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
    }

    private UpgradePrecheckResponse getPrecheckResponse(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {
        UpgradePackageState upgradePackageState = null;
        ActivityAllowed activityAllowed = new ActivityAllowed();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String jobLogMessage = "";
        String upMoFdn = "";
        try {
            activityAllowed = upMoServiceRetryProxy.isActivityAllowed(PREPARE_ACTIVITY_NAME, ecimUpgradeInfo);
            upMoFdn = activityAllowed.getUpMoFdn();
            upgradePackageState = upMoServiceRetryProxy.getUpgradePkgState(ecimUpgradeInfo);
        } catch (final SoftwarePackageNameNotFound ex) {
            jobLogMessage = String.format(JobLogConstants.JOB_CONFIGURATION_ERROR, ex.getMessage());
            return prepareServiceHandler.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, upMoFdn);
        } catch (final SoftwarePackagePoNotFound softwarePackagePoNotFound) {
            jobLogMessage = String.format(JobLogConstants.SW_PACKAGE_NOT_FOUND, nodeName);
            return prepareServiceHandler.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, upMoFdn);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            return prepareServiceHandler.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, upMoFdn);
        } catch (final UpgradePackageMoNotFoundException upgradePackageMoNotFoundException) {
            //Nothing to do
        } catch (NodeAttributesReaderException attributesReaderException) {
            logger.error("Exception occured while reading attributes from the node:{} and activityJobId:{} in PrepareService.asyncPrecheck for the node. Details are: ", nodeName, activityJobId,
                    attributesReaderException);
            jobLogMessage = String.format(JobLogConstants.NODE_READ_EXCEPTION, attributesReaderException.getMessage());
            return prepareServiceHandler.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, upMoFdn);
        } catch (final ArgumentBuilderException e) {
            logger.error("ArgumentBuilderException occured in Prepare service precheck with activityJobId: {} nodeName : {} and cannot be proceed further. Details are {}", activityJobId, nodeName, e);
            jobLogMessage = String.format("ArgumentBuilderException occurred while triggering activity  : %s ", ActivityConstants.PREPARE);
            return prepareServiceHandler.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, upMoFdn);
        } catch (final Exception ex) {
            logger.error("Exception occured in Prepare service precheck with activityJobId : {} nodeName : {} and cannot be proceed further. Details are {}", activityJobId, nodeName, ex);
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ex.getMessage());
            return prepareServiceHandler.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, upMoFdn);
        }
        if (activityAllowed.getUpMoFdn() == null) {
            activityStepResultEnum = isCreateActivityAllowed(ecimUpgradeInfo, jobLogList);
        } else {
            activityStepResultEnum = isPrepareActivityAllowed(ecimUpgradeInfo, activityAllowed, upgradePackageState, jobLogList);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        if (activityStepResultEnum == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        } else {
            logger.debug("Skipping persisting step duration as activity:{} is to be skipped/failed for the activityJobId: {}.", ActivityConstants.PREPARE, activityJobId);
        }
        return new UpgradePrecheckResponse(upMoFdn, activityStepResultEnum);
    }

    private ActivityStepResultEnum isCreateActivityAllowed(final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList) {
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRECHECK_SUCCESS, EcimSwMConstants.CREATE_UPGRADE_PACKAGE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        final String logMessage = "Proceeding to Create UpgradePackage MO as it doesn't exist on node.";
        activityUtils.recordEvent(SHMEvents.PREPARE_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        return activityStepResultEnum;
    }

    private ActivityStepResultEnum isPrepareActivityAllowed(final EcimUpgradeInfo ecimUpgradeInfo, final ActivityAllowed activityAllowed, final UpgradePackageState upgradePackageState,
            final List<Map<String, Object>> jobLogList) {
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;

        if (upgradeJobConfigurationListener.isSkipInstallActivityEnabled() && isPrepareActivityCompleted(upgradePackageState)) {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            final String logMessage = String.format(JobLogConstants.PRE_CHECK_ACTIVITY_SKIP, ActivityConstants.PREPARE);
            logger.debug("{} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.PREPARE_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } else {
            if (activityAllowed.getActivityAllowed()) {
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                final String logMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.PREPARE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                logger.info("{}: {}", logMessage, activityJobId);
                activityUtils.recordEvent(SHMEvents.PREPARE_PRECHECK, nodeName, activityAllowed.getUpMoFdn(), activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            } else {
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
                final String logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, UPGRADE_PACKAGE_NOT_IN_GOOD_STATE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.recordEvent(SHMEvents.PREPARE_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            }
        }
        return activityStepResultEnum;
    }

    @Override
    public void processNotification(final Notification notification) {
        logger.info("Entered ECIM - upgrade - Prepare Service - processNotification with event type : {}", notification.getNotificationEventType());
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        try {
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
            //Process the AVC notifications received on SwM or UpgradePackage MO
            if (NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
                prepareServiceHandler.processAVCNotifications(notification, jobActivityInfo);
            }
            //Process the CREATE notification received for UpgradePackage MO
            else if (NotificationEventTypeEnum.CREATE.equals(notification.getNotificationEventType())) {
                prepareServiceHandler.processCreateNotifications(notification, jobActivityInfo);
            }
        } catch (final Exception ex) {
            logger.error("An exception occured while processing {} notification. Notification : {} , Exception: {}", notification.getNotificationEventType(), notification, ex);
            final String logMessage = ex.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        logger.debug("Entered into PrepareService handleTimeout() method with activityJobId : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String moFdn = "";
        String nodeName = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            final String actionTriggered = ecimUpgradeInfo.getActionTriggered();
            moFdn = getMoFdn(actionTriggered, ecimUpgradeInfo);
            logger.info("MoFdn in handleTimeout() method: {} with activityJobId: {} ", moFdn, activityJobId);
            activityStepResultEnum = handleTimeout(moFdn, ecimUpgradeInfo);
            nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        } catch (final Exception ex) {
            logger.error("Exception occurred and can not be proceeded further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.PREPARE, exceptionMessage);
            activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    private ActivityStepResultEnum handleTimeout(final String moFdn, final EcimUpgradeInfo ecimUpgradeInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final String nodeName = neJobStaticData.getNodeName();
        final long activityJobId = ecimUpgradeInfo.getActivityJobId();
        final String actionTriggered = ecimUpgradeInfo.getActionTriggered();
        JobResult jobResult = JobResult.FAILED;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        logger.info("PrepareService HandleTimeout for node {} and activityJobId {}. Action Triggered is {}", nodeName, activityJobId, actionTriggered);
        String jobLogMessage;
        String errorMessage;
        AsyncActionProgress progressReport = null;

        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());

        try {
            boolean isValidTimeout = false;
            progressReport = upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo);
            switch (actionTriggered) {
            case CREATE_ACTIVITY_NAME:
                isValidTimeout = prepareServiceHandler.validateActionProgress(progressReport, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
                if (isValidTimeout) {
                    jobResult = prepareServiceHandler.handleTimeoutForCreateActivity(moFdn, ecimUpgradeInfo, progressReport, jobActivityInfo);
                }
                break;
            case PREPARE_ACTIVITY_NAME:
                isValidTimeout = prepareServiceHandler.validateActionProgress(progressReport, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
                if (isValidTimeout) {
                    jobResult = prepareServiceHandler.handleTimeoutForPrepareActivity(moFdn, ecimUpgradeInfo, jobActivityInfo);
                }
                break;
            case EcimSwMConstants.CANCEL_UPGRADE_PACKAGE:
                isValidTimeout = prepareServiceHandler.validateActionProgress(progressReport, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
                if (isValidTimeout) {
                    jobResult = prepareServiceHandler.handleTimeoutForCancel(moFdn, ecimUpgradeInfo, jobActivityInfo);
                }
                break;
            default:
                logger.error("{} ActivityJobId {}, actionTriggeredProperty: {}", NO_ACTION_TRIGGERED, activityJobId, actionTriggered);
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.PREPARE) + " because " + NO_ACTION_TRIGGERED;
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (final SoftwarePackageNameNotFound softwarePackageNameNotFound) {
            errorMessage = softwarePackageNameNotFound.getMessage();
            jobLogMessage = String.format(JobLogConstants.JOB_CONFIGURATION_ERROR, errorMessage);
            jobResult = prepareServiceHandler.handleExceptionForTimeout(jobLogMessage, errorMessage, jobLogList);
        } catch (final SoftwarePackagePoNotFound softwarePackagePoNotFound) {
            errorMessage = softwarePackagePoNotFound.getMessage();
            jobLogMessage = String.format(JobLogConstants.SW_PACKAGE_NOT_FOUND, nodeName);
            jobResult = prepareServiceHandler.handleExceptionForTimeout(jobLogMessage, errorMessage, jobLogList);
        } catch (final MoNotFoundException moNotFoundException) {
            errorMessage = moNotFoundException.getMessage();
            jobLogMessage = String.format(JobLogConstants.SW_PACKAGE_NOT_FOUND, errorMessage);
            jobResult = prepareServiceHandler.handleExceptionForTimeout(jobLogMessage, errorMessage, jobLogList);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            errorMessage = unsupportedFragmentException.getMessage();
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            jobResult = prepareServiceHandler.handleExceptionForTimeout(jobLogMessage, errorMessage, jobLogList);
        } catch (final ArgumentBuilderException e) {
            logger.error("ArgumentBuilderException occured in Prepare service handleTimeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId,
                    nodeName, e);
            jobLogMessage = String.format("ArgumentBuilderException occurred while triggering activity  : %s ", PREPARE_ACTIVITY_NAME);
            jobResult = prepareServiceHandler.handleExceptionForTimeout(jobLogMessage, e.getMessage(), jobLogList);
        } catch (final NodeAttributesReaderException e) {
            logger.error("Exception occured while reading the attributes in PrepareService.handleTimeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}",
                    activityJobId, nodeName, e);
            jobLogMessage = String.format(JobLogConstants.NODE_READ_EXCEPTION, e.getMessage());
            jobResult = prepareServiceHandler.handleExceptionForTimeout(jobLogMessage, e.getMessage(), jobLogList);
        }

        if (CREATE_ACTIVITY_NAME.equals(actionTriggered) && jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.REPEAT_EXECUTE;
        } else if (PREPARE_ACTIVITY_NAME.equals(actionTriggered) && jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
        } else if (EcimSwMConstants.CANCEL_UPGRADE_PACKAGE.equals(actionTriggered) && jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        } else {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        }
        if (activityStepResultEnum != ActivityStepResultEnum.REPEAT_EXECUTE) {
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL || activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return activityStepResultEnum;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        logger.info("Inside PrepareService cancel() with activityJobId {}", activityJobId);

        boolean isCancelTriggeredOnCreate = false;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            final String actionTriggered = ecimUpgradeInfo.getActionTriggered();
            isCancelTriggeredOnCreate = upMoServiceRetryProxy.isCreateActionTriggered(ecimUpgradeInfo, actionTriggered);
        } catch (final UnsupportedFragmentException e) {

        } catch (final Exception ex) {
            logger.error("Exception is caught in cancel  action, exception message : ", ex);
            final String jobLogMessage = ex.getMessage();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            final ActivityStepResult activityStepResult = new ActivityStepResult();
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            return activityStepResult;
        }
        ActivityStepResult activityStepResult = null;
        if (isCancelTriggeredOnCreate) {
            activityStepResult = cancelUpgradeService.cancel(activityJobId, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
        } else {
            activityStepResult = cancelUpgradeService.cancel(activityJobId, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
        }

        return activityStepResult;
    }

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
        logger.debug("Entred into PrepareService precheck() with activityJobId {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            activityStepResultEnum = precheck(ecimUpgradeInfo);
            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
        } catch (final Exception ex) {
            logger.error("An exception occurred while pre validing the PrepareService.precheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.PREPARE, exceptionMessage);
        }
        logger.debug("PrepareService.asyncPrecheck completed for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.PREPARE, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        logger.info("Entering into PrepareService.precheckHandleTimeout for the activityJobId: {}", activityJobId);
        prepareServiceHandler.precheckHandleTimeout(activityJobId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout(long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {

        logger.debug("Entered into PrepareService handleTimeout() method with activityJobId : {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String moFdn = "";
        String nodeName = null;
        NEJobStaticData neJobStaticData = null;
        try {
            final EcimUpgradeInfo ecimUpgradeInfo = ecimSwmUtils.getEcimUpgradeInformation(activityJobId);
            final String actionTriggered = ecimUpgradeInfo.getActionTriggered();

            moFdn = getMoFdn(actionTriggered, ecimUpgradeInfo);
            logger.info("MoFdn in handleTimeout() method: {} with activityJobId: {} ", moFdn, activityJobId);
            activityStepResultEnum = handleTimeout(moFdn, ecimUpgradeInfo);

            neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            nodeName = neJobStaticData.getNodeName();
        } catch (final Exception ex) {
            logger.error("Exception occurred and can not be proceeded further with activityId {} and nodeName {}. Reason : {}", activityJobId, nodeName, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.PREPARE, exceptionMessage);
            activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        }
        logger.debug("asyncHandleTimeout completed in PrepareService with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.PREPARE, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        logger.info("Entering into PrepareService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.PREPARE);
    }

    private String getMoFdn(final String actionTriggered, final EcimUpgradeInfo ecimUpgradeInfo)
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        String moFdn = "";
        if (CREATE_ACTIVITY_NAME.equalsIgnoreCase(actionTriggered)) {
            moFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);
        } else if (PREPARE_ACTIVITY_NAME.equalsIgnoreCase(actionTriggered)) {
            moFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo);
        } else if (EcimSwMConstants.CANCEL_UPGRADE_PACKAGE.equalsIgnoreCase(actionTriggered)) {
            moFdn = upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, ecimUpgradeInfo);
        }

        return moFdn;
    }

}
