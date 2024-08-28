/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.common.NodeRestartActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.common.NodeRestartServiceRetryProxy;
import com.ericsson.oss.services.shm.es.noderestart.NodeRestartActivityTimer;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.cpp.activity.NodeRestartActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * This class facilitates the Restart of CPP based node by invoking the manual restart MO action that triggers the node restart activity.
 */
@EServiceQualifier("CPP.NODERESTART.manualrestart")
@ActivityInfo(activityName = "manualrestart", jobType = JobTypeEnum.NODERESTART, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NodeRestartService implements Activity, ActivityCompleteCallBack, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRestartService.class);

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    protected NodeRestartActivityTimer nodeRestartActivityCompleteTimer;

    @Inject
    private NodeRestartActivityHandler nodeRestartActivityHandler;

    @Inject
    private CppNodeRestartConfigParamListener cppNodeRestartConfigParamListener;

    @Inject
    private NodeRestartServiceRetryProxy nodeRestartServiceRetryProxy;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    /**
     * This method validates the node is in sync with OSS to decide if create activity can be started or not and sends back the activity result to Work Flow Service.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside NodeRestartService.precheck() with precheck : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception e) {
            LOGGER.error("Exception occurred for NodeRestart at precheck for activityJobId : {}, Exception : ", activityJobId, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, nodeRestartActivityHandler.getActivityType(), e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME, activityStepResultEnum);
                return;
            }
            activityStepResultEnum = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred while pre validing the NodeRestartService.asyncPrecheck for the activityJobId: {}. Exception is: ", activityJobId, ex);
            activityUtils.handleExceptionForPrecheckScenarios(activityJobId, nodeRestartActivityHandler.getActivityType(), ex.getMessage());
        }
        LOGGER.debug("NodeRestartService.asyncPrecheck completed for the activityJobId {} with NodeName {} and its result is {}", activityJobId, nodeName, activityStepResultEnum);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME, activityStepResultEnum);
    }

    private ActivityStepResultEnum getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String jobLogMessage = null;
        String jobLogLevel = null;
        long activityStartTime = 0;
        String nodeName = null;
        try {
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();

            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());

            final Map<String, Object> managedElementAttributesMap = nodeRestartServiceRetryProxy.getManagedElementAttributes(nodeName);
            if (!isManagedElementExist(managedElementAttributesMap)) {
                jobLogMessage = String.format(RestartActivityConstants.MO_NOT_EXIST, nodeRestartActivityHandler.getActivityType());
                jobLogLevel = JobLogLevel.ERROR.toString();
            } else {
                jobLogMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, nodeRestartActivityHandler.getActivityType());
                jobLogLevel = JobLogLevel.INFO.toString();
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                final String moFdn = (String) managedElementAttributesMap.get(ShmConstants.FDN);
                systemRecorder.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CPP_NODE_RESTART_PRECHECK, EventLevel.COARSE, nodeName, moFdn, jobLogMessage);
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            }

        } catch (final Exception e) {
            LOGGER.error("Exception occurred for NodeRestart at precheck for activityJobId : {}, Exception : ", activityJobId, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, nodeRestartActivityHandler.getActivityType(), exceptionMessage);
            jobLogLevel = JobLogLevel.ERROR.toString();
        }

        if (ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION == activityStepResultEnum) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped or failed.");
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResultEnum;
    }

    /**
     * @param MoAttributesMap
     * @return
     */
    private boolean isManagedElementExist(final Map<String, Object> managedElementAttributesMap) {
        if (managedElementAttributesMap != null && !managedElementAttributesMap.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This performs the MO action and returns activity result to Workflow Service
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Inside NodeRestartService.execute() with activityJobId : {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();

        try {
            final String neType = networkElementRetrievalBean.getNeType(jobEnvironment.getNodeName());
            final String platform = platformTypeProviderImpl.getPlatformType(neType).name();
            final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
            final Map<String, Object> actionArguments = getActionArguments(jobConfigurationDetails, nodeName);
            final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = getActivityInfo(jobEnvironment.getActivityJobId(),
                    activityTimeoutsService.getActivityTimeoutAsInteger(neType, platform, JobTypeEnum.NODERESTART.name(), NodeRestartActivityConstants.MANUALRESTART),
                    cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval(), cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType));
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
            nodeRestartActivityHandler.executeNodeRestartAction(jobEnvironment, actionArguments, nodeRestartJobActivityInfo);
        } catch (final Exception ex) {
            LOGGER.error("Failed to proceed for noderestart activity {}. Exception : ", RestartActivityConstants.RESTART_NODE, ex);
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            String jobLogMessage = ExceptionParser.getReason(ex).isEmpty() ? ex.getMessage() : ExceptionParser.getReason(ex);
            jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, RestartActivityConstants.RESTART_NODE, jobLogMessage);
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_EXECUTE, CommandPhase.FINISHED_WITH_ERROR,
                    jobEnvironment.getNodeName(), ActivityConstants.EMPTY, jobLogMessage);
            activityUtils.additionalInfoForCommand(jobEnvironment.getActivityJobId(), jobEnvironment.getMainJobId(), JobTypeEnum.NODERESTART);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            final String businessKey = (String) jobEnvironment.getNeJobAttributes().get(ShmConstants.BUSINESS_KEY);
            activityUtils.failActivity(activityJobId, jobLogList, businessKey, RestartActivityConstants.RESTART_NODE);

        }
    }

    private NodeRestartJobActivityInfo getActivityInfo(final long activityJobId, final int maxTimeForCppNodeRestart, final int waitIntervalForEachRetry, final int nodeRestartSleepTime) {
        final ActivityInfo activityInfoAnnotation = this.getClass().getAnnotation(ActivityInfo.class);
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = new NodeRestartJobActivityInfo(activityJobId, activityInfoAnnotation.activityName(), activityInfoAnnotation.jobType(),
                activityInfoAnnotation.platform(), maxTimeForCppNodeRestart, waitIntervalForEachRetry, nodeRestartSleepTime);
        return nodeRestartJobActivityInfo;
    }

    /**
     * This method is used to get the action arguments.
     * 
     * @param jobConfigurationDetails
     *            , neName, neType, platformtype
     * @return actionArguments
     * 
     */

    private Map<String, Object> getActionArguments(final Map<String, Object> jobConfigurationDetails, final String neName) {
        final List<String> keyList = new ArrayList<String>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(neName));
        final String neType = networkElementList.get(0).getNeType();
        final String platformType = networkElementList.get(0).getPlatformType().toString();
        final Map<String, String> actionArguments = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType);
        final Map<String, Object> actionArgumentsNoDeRestart = new HashMap<String, Object>();
        for (final Entry<String, String> arguments : actionArguments.entrySet()) {
            actionArgumentsNoDeRestart.put(arguments.getKey(), arguments.getValue());
        }
        return actionArgumentsNoDeRestart;
    }

    /**
     * This method handles timeout scenario for Create Cv Activity and checks the stored configuration version in CV MO on node to see if CV is created or not.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Verifying activity result in handle timeout for the action  : Node Restart and with action Id : {}", activityJobId);
        nodeRestartActivityHandler.cancelTimer(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String logMessage = String.format(JobLogConstants.OPERATION_TIMED_OUT, RestartActivityConstants.RESTART_NODE);
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final boolean isActivityCompleted = nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, false);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        if (!isActivityCompleted) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else {
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        }
        final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        return activityStepResult;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */

    @Override
    public ActivityStepResult cancel(final long activityJobId) {

        LOGGER.debug("Inside NodeRestartService.cancel() with activityJobId:{}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        return nodeRestartActivityHandler.cancelNodeRestartAction(jobEnvironment);
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("Inside NodeRestartService.cancelTimeout() with activityJobId {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        return nodeRestartActivityHandler.cancelTimeout(finalizeResult, jobEnvironment, ActivityConstants.FALSE);
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack#onActionComplete(long)
     */
    @Override
    public void onActionComplete(final long activityJobId) {
        LOGGER.debug("Inside NodeRestartService.onActionComplete() with activityJobId {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE);
        final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        activityUtils.sendNotificationToWFS(jobEnvironment, jobEnvironment.getActivityJobId(), nodeRestartActivityHandler.getActivityType(), null);
        LOGGER.debug("Outside NodeRestartService.onActionComplete() ");
    }

    @Override
    public void precheckHandleTimeout(long activityJobId) {
        LOGGER.info("Entering into NodeRestartService.precheckHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, RestartActivityConstants.RESTART_NODE);
    }

    @Override
    public void asyncHandleTimeout(long activityJobId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void timeoutForAsyncHandleTimeout(long activityJobId) {
        // TODO Auto-generated method stub

    }

}
