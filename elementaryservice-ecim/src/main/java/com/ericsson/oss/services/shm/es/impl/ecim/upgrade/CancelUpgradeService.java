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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CancelUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelUpgradeService.class);

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private EcimSwMUtils ecimSwMUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private PollingActivityManager pollingActivityManager;

    public final static String ACTIVITY_NAME = "cancel";
    private static final String UPGRADE_PACKAGE_NOT_EXIST = "UpgardePackage MO doesn't exist on node to activate.";

    public ActivityStepResult cancel(final long activityJobId, final String activityName) {
        LOGGER.info("Inside CancelUpgradeService cancel() with activityJobId {}", activityJobId);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ACTIVITY_NAME), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        String upgradePackageMo = null;
        String jobLogMessage = "";
        EcimUpgradeInfo ecimUpgradeInfo = null;
        try {
            ecimUpgradeInfo = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);
            final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
            activityUtils.logCancelledByUser(jobLogList, ecimUpgradeInfo.getJobEnvironment().getMainJobAttributes(neJobStaticData.getMainJobId()),
                    ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(neJobStaticData.getNeJobId()), activityName);
            upgradePackageMo = upMoServiceRetryProxy.getNotifiableMoFdn(activityName, ecimUpgradeInfo);
            activityStepResult = performCancelAction(activityJobId, activityName, ecimUpgradeInfo, jobLogList);
            LOGGER.info("Cancel action activity step result : {}", activityStepResult.getActivityResultEnum());
            return activityStepResult;
        } catch (final UnsupportedFragmentException ex) {
            LOGGER.error("UnsupportedFragmentException is caught in cancel  action, exception message : ", ex);
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, ecimUpgradeInfo.getNeJobStaticData().getNodeName());
        } catch (final MoNotFoundException ex) {
            LOGGER.error("MoNotFoundException is caught in cancel  action, exception message : ", ex);
            jobLogMessage = String.format(JobLogConstants.MO_DOES_NOT_EXIST, String.format(UPGRADE_PACKAGE_NOT_EXIST, upgradePackageMo));
        } catch (final ArgumentBuilderException ex) {
            LOGGER.error("ArgumentBuilderException is caught in cancel  action, exception message : ", ex);
            jobLogMessage = ex.getMessage();
        } catch (final SoftwarePackageNameNotFound ex) {
            LOGGER.error("SoftwarePackageNameNotFound is caught in cancel  action, exception message : ", ex);
            jobLogMessage = ex.getMessage();
        } catch (final SoftwarePackagePoNotFound ex) {
            LOGGER.error("SoftwarePackagePoNotFound is caught in cancel  action, exception message : ", ex);
            jobLogMessage = ex.getMessage();
        } catch (final NodeAttributesReaderException ex) {
            LOGGER.error("NodeAttributesReaderException is caught in cancel  action, exception message : ", ex);
            jobLogMessage = ex.getMessage();
        } catch (final Exception ex) {
            LOGGER.error("Exception is caught in cancel  action, exception message : ", ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            if (!(exceptionMessage.isEmpty())) {
                jobLogMessage = exceptionMessage;
            } else {
                jobLogMessage = ex.getMessage();
            }
        }
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        updateJobLog(activityJobId, null, jobLogList, jobLogMessage, JobLogLevel.ERROR.toString());
        return activityStepResult;
    }

    private ActivityStepResult performCancelAction(final long activityJobId, final String activityName, final EcimUpgradeInfo ecimUpgradeInfo, final List<Map<String, Object>> jobLogList)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException,
            NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        LOGGER.debug("Inside CancelUpgradeService.performCancelAction() with activitJobId {}", activityJobId);
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult = validateActionRequest(ecimUpgradeInfo, activityJobId, jobLogList);
        ActionResult actionResult = new ActionResult();
        if (activityStepResult == null || (activityStepResult != null && ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION == activityStepResult.getActivityResultEnum())) {
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
            return activityStepResult;
        }
        try {
            actionResult = upMoServiceRetryProxy.executeCancelAction(ecimUpgradeInfo, activityName);
            LOGGER.info("Action id for cancel action {} for the activityJobId {}", actionResult.getActionId(), activityJobId);
        } catch (final Exception exception) {
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            String jobLogMessage;
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ACTIVITY_NAME, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ACTIVITY_NAME);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityStepResult = validateActionResponse(activityJobId, actionResult, jobLogList, propertyList, ecimUpgradeInfo, activityName);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, propertyList, jobLogList);
        return activityStepResult;
    }

    private void updateJobLog(final long activityJobId, final List<Map<String, Object>> propertyList, final List<Map<String, Object>> jobLogList, final String logMessage, final String jobLogLevel) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, propertyList, jobLogList);
    }

    private ActivityStepResult validateActionRequest(final EcimUpgradeInfo ecimUpgradeInfo, final long activityJobId, final List<Map<String, Object>> jobLogList)
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException,
            NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        LOGGER.debug("Inside CancelUpgradeService.validateActionRequest() with activitJobId {}", activityJobId);
        String logMessage = null;

        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        //No need to pre check if last triggered action is "create", because up mo is not created after create activity and before prepare activity 
        if (!ACTIVITY_NAME.equals(ecimUpgradeInfo.getActionTriggered())) {
            final ActivityAllowed activityAllowed = upMoServiceRetryProxy.isActivityAllowed(ACTIVITY_NAME, ecimUpgradeInfo);
            if (!activityAllowed.getActivityAllowed()) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ACTIVITY_NAME, "UpMo not allowed to cancel");
            } else {
                logMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ACTIVITY_NAME);
            }
            LOGGER.info("{} for activity {}", logMessage, activityJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        LOGGER.debug("Cancel action activity step result before triggering action : {}", activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

    private ActivityStepResult validateActionResponse(final long activityJobId, final ActionResult actionResult, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> propertyList, final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) {
        LOGGER.debug("Inside CancelUpgradeService.validateActionResponse() with activitJobId {}", activityJobId);
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        String neType = null;
        try {
            neType = networkElementRetrievalBean.getNeType(nodeName);
        } catch (final MoNotFoundException e) {
            LOGGER.error("Exception while fetching neType of node :  {}", nodeName);
        }
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        if (actionResult.isTriggerSuccess()) {
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.name(), ACTIVITY_NAME);
            activityUtils.addJobProperty(EcimCommonConstants.ACTION_TRIGGERED, ACTIVITY_NAME, propertyList);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ACTIVITY_NAME, activityTimeout), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityName, nodeName);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ACTIVITY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        }
        LOGGER.debug("Cancel action activity step result : {}", activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }
}
