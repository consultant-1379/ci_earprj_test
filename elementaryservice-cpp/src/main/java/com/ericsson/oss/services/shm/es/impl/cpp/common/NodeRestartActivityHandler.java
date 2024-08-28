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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.RestartActivityConstants;
import com.ericsson.oss.services.shm.es.noderestart.NodeRestartActivityTimer;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * Common class to perform node restart activity. It will be called from NodeRestartService and RestoreService.
 * 
 * 
 */
public class NodeRestartActivityHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRestartActivityHandler.class);

    @Inject
    private NodeRestartServiceRetryProxy nodeRestartServiceRetryProxy;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NodeRestartActivityTimer nodeRestartActivityTimer;

    @Inject
    private ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    private ConfigurationVersionService configurationVersionService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    public void executeNodeRestartAction(final JobEnvironment jobEnvironment, final Map<String, Object> nodereStartActionArguments, final NodeRestartJobActivityInfo nodeRestartJobActivityInfo) {
        LOGGER.debug("Entering into executeNodeRestartAction with activityJobId : {} ", jobEnvironment.getActivityJobId());
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        String managedElementFdn = null;
        String nodeName = null;

        nodeName = jobEnvironment.getNodeName();
        final long activityJobId = jobEnvironment.getActivityJobId();
        try {
            managedElementFdn = nodeRestartServiceRetryProxy.getManagedObjectFdn(nodeName);
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_EXECUTE, CommandPhase.STARTED, nodeName, managedElementFdn,
                    activityUtils.additionalInfoForCommand(jobEnvironment.getActivityJobId(), jobEnvironment.getMainJobId(), JobTypeEnum.NODERESTART));
            final String logMessage = String.format(JobLogConstants.ACTION_TRIGGERING, getActivityType()) + managedElementFdn;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final Exception ex) {
            LOGGER.error("Failed to proceed for noderestart activity {}. Exception : ", getActivityType(), ex);
            String jobLogMessage = ExceptionParser.getReason(ex).isEmpty() ? ex.getMessage() : ExceptionParser.getReason(ex);
            jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, getActivityType(), jobLogMessage);
            prepareFailedJobProperties(jobEnvironment, managedElementFdn, jobLogList, jobPropertyList, jobLogMessage);
            activityUtils.sendNotificationToWFS(jobEnvironment, jobEnvironment.getActivityJobId(), getActivityType(), null);
            return;
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(jobEnvironment.getActivityJobId(), jobPropertyList, jobLogList, null);
        performAction(jobEnvironment, nodereStartActionArguments, managedElementFdn, nodeRestartJobActivityInfo);
        LOGGER.debug("Exiting from NodeRestartActivityHandler.executeNodeRestartAction with activityJobId: {}", activityJobId);
    }

    private void performAction(final JobEnvironment jobEnvironment, final Map<String, Object> nodereStartActionArguments, final String managedElementFdn,
            final NodeRestartJobActivityInfo nodeRestartJobActivityInfo) {

        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final String nodeName = jobEnvironment.getNodeName();
        boolean performActionStatus = false;
        try {
            final String restartRank = (String) nodereStartActionArguments.get(RestartActivityConstants.RESTART_RANK);
            final String restartReason = (String) nodereStartActionArguments.get(RestartActivityConstants.RESTART_REASON);
            performActionStatus = nodeRestartServiceRetryProxy.performAction(nodeName, managedElementFdn, RestartActivityConstants.ACTION_NAME, nodereStartActionArguments);
            LOGGER.debug("Node restart action triggered status : {}", performActionStatus);
            if (performActionStatus) {
                systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName,
                        managedElementFdn, String.format(JobLogConstants.ACTION_TRIGGERED, getActivityType()) + String.format(RestartActivityConstants.RESULT_INFORMATION, restartRank, restartReason));
                processNodeRestartResponse(jobEnvironment.getActivityJobId(), getActivityType(), jobLogList, jobPropertyList, nodereStartActionArguments,
                        nodeRestartJobActivityInfo.getMaxTimeForCppNodeRestart());
                jobLogList.clear();
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, RestartActivityConstants.RESTART_NODE_CHECK, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(jobEnvironment.getActivityJobId(), null, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
                nodeRestartActivityTimer.startTimer(nodeRestartJobActivityInfo);
                final Map<String, Object> activityJobAttrs = jobEnvironment.getActivityJobAttributes();
                final long activityStartTime = ((Date) activityJobAttrs.get(ShmConstants.ACTIVITY_START_DATE)).getTime();
                activityUtils.persistStepDurations(jobEnvironment.getActivityJobId(), activityStartTime, ActivityStepsEnum.EXECUTE);
            } else {
                final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, getActivityType())
                        + String.format(RestartActivityConstants.RESULT_INFORMATION, restartRank, restartReason);
                prepareFailedJobProperties(jobEnvironment, managedElementFdn, jobLogList, jobPropertyList, jobLogMessage);
            }
            LOGGER.debug("Node restart action completed on the node : {}", nodeName);
        } catch (final Exception ex) {
            LOGGER.error("Failed to trigger noderestart activity {}. Exception : ", jobEnvironment.getActivityJobId(), ex);
            String jobLogMessage = ExceptionParser.getReason(ex).isEmpty() ? ex.getMessage() : ExceptionParser.getReason(ex);
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, getActivityType()) + String.format(JobLogConstants.FAILURE_REASON, jobLogMessage);
            prepareFailedJobProperties(jobEnvironment, managedElementFdn, jobLogList, jobPropertyList, jobLogMessage);
        }
        if (!performActionStatus) {
            jobUpdateService.readAndUpdateRunningJobAttributes(jobEnvironment.getActivityJobId(), jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, jobEnvironment.getActivityJobId(), getActivityType(), null);
        }
    }

    public boolean getActionCompletionStatus(final JobEnvironment jobEnvironment, final boolean isFromRestore) {
        LOGGER.debug("Inside NodeRestartActivityHandler.getActionCompletionStatus() with activityJobId:{}", jobEnvironment.getActivityJobId());
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        JobResult jobResult = JobResult.FAILED;
        JobLogLevel logLevel = JobLogLevel.ERROR;
        String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, getActivityType());
        String managedElementFdn = null;
        String nodeName = null;
        boolean isNodeRestoreSuccess = false;
        String cvName = null;
        boolean isActionCompleted = false;
        final long activityJobId = jobEnvironment.getActivityJobId();
        String jobExectuionUser = null;
   
        try {
            nodeName = jobEnvironment.getNodeName();
            jobExectuionUser = activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId());
            managedElementFdn = nodeRestartServiceRetryProxy.getManagedObjectFdn(nodeName);
            if (nodeRestartServiceRetryProxy.isNodeReachable(nodeName)) {
                logLevel = JobLogLevel.INFO;
                jobResult = JobResult.SUCCESS;
                isActionCompleted = true;

                jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, getActivityType());
                systemRecorder.recordEvent(jobExectuionUser, SHMEvents.CPP_NODE_RESTART_ACTION_COMPLETED, EventLevel.COARSE, nodeName, managedElementFdn,
                        activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
                if (isFromRestore) {
                    cvName = configurationVersionUtils.getNeJobPropertyValue(jobEnvironment.getMainJobAttributes(), jobEnvironment.getNodeName(), BackupActivityConstants.CV_NAME);
                    if (isCvRestored(jobEnvironment, isFromRestore, cvName)) {
                        isNodeRestoreSuccess = true;
                    } else {
                        jobResult = JobResult.FAILED;
                        isActionCompleted = false;
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CONFIGURATION_VERSION_NOT_LOADED, cvName), new Date(), JobLogType.SYSTEM.toString(),
                                JobLogLevel.ERROR.toString());
                        systemRecorder.recordEvent(jobExectuionUser, SHMEvents.CPP_CV_RESTORE_ACTION_FAILED, EventLevel.COARSE, managedElementFdn, cvName,
                                activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
                    }
                }
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel.toString());
            LOGGER.debug("jobResult from handleTimeoutForNodeRestartAction : {}", jobResult);
        } catch (final Exception ex) {
            LOGGER.error("Exception ocuurred while getting node retart action status for the node : {} . Failur reason : {} ", nodeName, ExceptionParser.getReason(ex));
            jobResult = JobResult.FAILED;
            jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, getActivityType(), ExceptionParser.getReason(ex));
            systemRecorder.recordEvent(jobExectuionUser, SHMEvents.CPP_NODE_RESTART_ACTION_FAILED, EventLevel.COARSE, nodeName, managedElementFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            isActionCompleted = false;
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isNodeRestoreSuccess) {
            final List<Map<String, Object>> neJobLogList = new ArrayList<>();
            jobLogUtil.prepareJobLogAtrributesList(neJobLogList, String.format(JobLogConstants.NODE_RESTORE_SUCCESSFUL, cvName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        cancelTimer(activityJobId);
        LOGGER.debug("Exiting from NodeRestartActivityHandler.getActionCompletionStatus() with activityJobId:{} and complete status : {}", activityJobId, isActionCompleted);
        return isActionCompleted;
    }

    public boolean isRestoreActionCompleted(final JobEnvironment jobEnvironment, final boolean isFromRestore) {
        final long activityJobId = jobEnvironment.getActivityJobId();
        final String nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("Inside NodeRestartActivityHandler.isRestoreActionCompleted() with activityJobId:{}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        String managedElementFdn = null;
        boolean isActionCompleted = true;

        try {
            managedElementFdn = nodeRestartServiceRetryProxy.getManagedObjectFdn(nodeName);
            final String cvName = configurationVersionUtils.getNeJobPropertyValue(jobEnvironment.getMainJobAttributes(), nodeName, BackupActivityConstants.CV_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.VERIFY_RESTORE_STATUS, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            if (isCvRestored(jobEnvironment, isFromRestore, cvName)) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NODE_RESTORE_SUCCESSFUL, cvName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
            } else {
                isActionCompleted = checkWaitTimeElapsed(jobEnvironment, cvName, managedElementFdn, jobPropertyList, jobLogList);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception ocuurred while getting node retart action status for the node : {} . Failur reason : {} ", nodeName, ExceptionParser.getReason(ex));
            final String jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, getActivityType(), ExceptionParser.getReason(ex));
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_ACTION_FAILED, EventLevel.COARSE, nodeName, managedElementFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
            if (nodeRestartActivityTimer.isWaitTimeElapsed(activityJobId)) {
                parseException(ex, jobPropertyList, jobLogList);
            } else {
                isActionCompleted = false;
            }
        }
        if (isActionCompleted) {
            cancelTimer(activityJobId);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        LOGGER.debug("Exiting from NodeRestartActivityHandler.isRestoreActionCompleted() with activityJobId:{} and complete status : {}", activityJobId, isActionCompleted);
        return isActionCompleted;
    }

    private boolean checkWaitTimeElapsed(final JobEnvironment jobEnvironment, final String cvName, final String managedElementFdn, final List<Map<String, Object>> jobPropertyList,
            final List<Map<String, Object>> jobLogList) {
        final String nodeName = jobEnvironment.getNodeName();
        boolean isActionCompleted = true;
        final boolean isMaxTimeReached = nodeRestartActivityTimer.isWaitTimeElapsed(jobEnvironment.getActivityJobId());
        if (isMaxTimeReached) {
            final String logMessage = String.format(JobLogConstants.CONFIGURATION_VERSION_NOT_LOADED, cvName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_CV_RESTORE_ACTION_FAILED, EventLevel.COARSE, cvName, managedElementFdn,
                    activityUtils.additionalInfoForEvent(jobEnvironment.getActivityJobId(), nodeName, logMessage));
        } else {
            isActionCompleted = false;
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_CV_RESTORE_ACTION_IN_PROGRESS, EventLevel.COARSE, nodeName, managedElementFdn,
                    activityUtils.additionalInfoForEvent(jobEnvironment.getActivityJobId(), nodeName, JobLogConstants.RESTORE_IN_PROGRESS));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.RESTORE_IN_PROGRESS, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        return isActionCompleted;
    }

    private void parseException(final Exception ex, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        final boolean isNodeNotReachable = ExceptionParser.isNodeUnreachable(ex);
        if (isNodeNotReachable) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.NODE_NOT_REACHABLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } else {
            final String jobLogMessage = String.format(JobLogConstants.RESULT_EVALUATION_FAILED, getActivityType(), ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

    /**
     * This method will verify whether the provided cv restored successfully OR not on the node.
     * 
     * @param jobEnvironment
     * @param isFromRestore
     * @param jobLogList
     * @return
     */
    private boolean isCvRestored(final JobEnvironment jobEnvironment, final boolean isFromRestore, final String cvName) {

        if (!isFromRestore) {
            return true;
        }
        final String loadedCvName = getloadedConfigurationVersionName(jobEnvironment);
        LOGGER.debug("Expected cv name to be restored : {} and CV from node :{} ", cvName, loadedCvName);

        return cvName != null && loadedCvName != null && cvName.equalsIgnoreCase(loadedCvName);
    }

    public ActivityStepResult cancelNodeRestartAction(final JobEnvironment jobEnvironment) {
        LOGGER.debug("Inside NodeRestartActivityHandler.cancelNodeRestartAction() with activityJobId:{}", jobEnvironment.getActivityJobId());
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        String nodeName = null;
        String managedElementFdn = null;
        final long activityJobId = jobEnvironment.getActivityJobId();
        try {

            nodeName = jobEnvironment.getNodeName();
            managedElementFdn = nodeRestartServiceRetryProxy.getManagedObjectFdn(nodeName);
            activityUtils.logCancelledByUser(jobLogList, jobEnvironment, getActivityType());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, getActivityType()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, ShmConstants.TRUE.toLowerCase());
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_CANCEL, EventLevel.COARSE, nodeName, managedElementFdn,
                    String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, getActivityType()));
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while cancelling Node Restart action for activityJobId : {}, Exception : ", activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.FAILURE_REASON, ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_CANCEL, EventLevel.COARSE, nodeName, managedElementFdn,
                    jobLogMessage);
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return new ActivityStepResult();
    }

    public ActivityStepResult cancelTimeout(final boolean finalizeResult, final JobEnvironment jobEnvironment, final boolean isFromRestore) {
        LOGGER.debug("Entering into NodeRestartActivityHandler.cancelTimeout() with activityJobId {}", jobEnvironment.getActivityJobId());
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        String nodeName = null;
        String jobExecutionUser = null;
        String managedElementFdn = null;
        boolean isNodeRestoreSuccess = false;
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final long activityJobId = jobEnvironment.getActivityJobId();
        String cvName = null;

        try {
            nodeName = jobEnvironment.getNodeName();
            jobExecutionUser = activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId());
            managedElementFdn = nodeRestartServiceRetryProxy.getManagedObjectFdn(nodeName);
            if (nodeRestartServiceRetryProxy.isNodeReachable(nodeName)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, RestartActivityConstants.RESTART_NODE_SUCCESS, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                systemRecorder.recordEvent(jobExecutionUser, SHMEvents.CPP_NODE_RESTART_TIMEOUT, EventLevel.COARSE, nodeName, managedElementFdn,
                        String.format(RestartActivityConstants.RESTART_NODE_SUCCESS, nodeName));
                if (isFromRestore) {
                    cvName = configurationVersionUtils.getNeJobPropertyValue(jobEnvironment.getMainJobAttributes(), jobEnvironment.getNodeName(), BackupActivityConstants.CV_NAME);
                    if (!isCvRestored(jobEnvironment, isFromRestore, cvName)) {
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CONFIGURATION_VERSION_NOT_LOADED, cvName), new Date(), JobLogType.SYSTEM.toString(),
                                JobLogLevel.ERROR.toString());
                        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                        systemRecorder.recordEvent(jobExecutionUser, SHMEvents.CPP_NODE_RESTART_TIMEOUT, EventLevel.COARSE, nodeName, managedElementFdn,
                                String.format(RestartActivityConstants.RESTART_NODE_FAIL, nodeName));
                    } else {
                        isNodeRestoreSuccess = true;
                    }
                }
            } else {
                if (finalizeResult) {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, RestartActivityConstants.RESTART_NODE_FAIL, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                    systemRecorder.recordEvent(jobExecutionUser, SHMEvents.CPP_NODE_RESTART_TIMEOUT, EventLevel.COARSE, nodeName, managedElementFdn,
                            String.format(RestartActivityConstants.RESTART_NODE_FAIL, nodeName));
                } else {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred for NodeRestart at cancelTimeout for activityJobId : {}, Exception : ", activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.FAILURE_REASON, ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            systemRecorder.recordEvent(jobExecutionUser, SHMEvents.CPP_NODE_RESTART_TIMEOUT, EventLevel.COARSE, nodeName, managedElementFdn, jobLogMessage);
        }

        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        if (isNodeRestoreSuccess) {
            final List<Map<String, Object>> neJobLogList = new ArrayList<>();
            jobLogUtil.prepareJobLogAtrributesList(neJobLogList, String.format(JobLogConstants.NODE_RESTORE_SUCCESSFUL, cvName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(jobEnvironment.getNeJobId(), null, neJobLogList, null);
        }
        LOGGER.debug("Exiting from NodeRestartActivityHandler.cancelTimeoutForNodeRestart() ");
        return activityStepResult;
    }

    private void prepareFailedJobProperties(final JobEnvironment jobEnvironment, final String managedElementFdn, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList, final String jobLogMessage) {
        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_EXECUTE, CommandPhase.FINISHED_WITH_ERROR,
                jobEnvironment.getNodeName(), managedElementFdn, activityUtils.additionalInfoForCommand(jobEnvironment.getActivityJobId(), jobEnvironment.getMainJobId(), JobTypeEnum.NODERESTART));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
    }

    private void processNodeRestartResponse(final long activityJobId, final String activityType, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final Map<String, Object> actionArguments, final int timeoutInMillis) {
        final String restartRank = (String) actionArguments.get(RestartActivityConstants.RESTART_RANK);
        final String restartReason = (String) actionArguments.get(RestartActivityConstants.RESTART_REASON);
        final int timeoutInInt = timeoutInMillis != 0 ? timeoutInMillis / 60000 : 0;
        final String jobLogMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, activityType, timeoutInInt)
                + String.format(RestartActivityConstants.RESULT_INFORMATION, restartRank, restartReason);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED, RestartActivityConstants.ACTION_NAME);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ShmConstants.TRUE.toLowerCase());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    public String getActivityType() {
        return RestartActivityConstants.RESTART_NODE;
    }

    public String getNotificationEventType() {
        return SHMEvents.CPP_NODE_RESTART_EXECUTE;
    }

    @SuppressWarnings("unchecked")
    private String getloadedConfigurationVersionName(final JobEnvironment jobEnvironment) {
        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName());
        final Map<String, Object> cvMoAttribute = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
        return (String) cvMoAttribute.get(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION);
    }

    public void cancelTimer(final long activityJobId) {
        //Canceling node restart timer
        nodeRestartActivityTimer.cancelTimer(activityJobId);
    }

}
