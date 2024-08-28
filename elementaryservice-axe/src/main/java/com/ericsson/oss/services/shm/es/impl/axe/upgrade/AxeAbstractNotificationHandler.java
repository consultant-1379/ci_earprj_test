/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.axe.synchronous.AxeSynchronousActivityProcessor;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

public abstract class AxeAbstractNotificationHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    private AxeUpgradeServiceHelper axeUpgradeServiceHelper;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private AxeSynchronousActivityProcessor synchronousActivityProcessor;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;
    
    /**
     * @param opsResponseAttributes
     * @return
     */
    protected Double persistProgressPercentage(final Map<String, Object> opsResponseAttributes) {
        Double progressPercentage = 0.0;
        if (opsResponseAttributes.get(ActivityConstants.PROGRESS_PERCENTAGE) != null) {
            progressPercentage = (double) opsResponseAttributes.get(ActivityConstants.PROGRESS_PERCENTAGE);
        }
        return progressPercentage;
    }

    /**
     * @param opsResponseAttributes
     * @param activityJobId
     * @param jobPropertyList
     * @param jobLogList
     */
    protected void persistSessionAndClusterId(final Map<String, Object> opsResponseAttributes, final long activityJobId, final List<Map<String, Object>> jobPropertyList,
            final List<Map<String, Object>> jobLogList) {
        final String clusterId = opsResponseAttributes.get(ActivityConstants.OPS_CLUSTER_ID).toString();
        final String sessionId = opsResponseAttributes.get(ActivityConstants.OPS_SESSION_ID).toString();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.OPS_CLUSTER_ID, clusterId);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.OPS_SESSION_ID, sessionId);
        activityUtils.addJobLog(String.format(JobLogConstants.AXE_ACTIVITY_CLUSTER_SESSION_ID_MESSAGE, clusterId, sessionId), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        persistInitialProgressPercentage(activityJobId, jobPropertyList, jobLogList);

    }

    /**
     * @param activityJobId
     * @param jobPropertyList
     * @param jobLogList
     */
    private void persistInitialProgressPercentage(final long activityJobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        if (axeUpgradeServiceHelper.isResponseTimeStampAttributePersisted(activityJobId)) {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 2.0);
        }
    }

    /**
     * @param opsResponseAttributes
     * @param activityJobId
     */
    protected boolean verifyResponseTimeStampAttribute(final Map<String, Object> opsResponseAttributes, final long activityJobId) {
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE) != null) {
            final long responseEventTime = Long.parseLong(opsResponseAttributes.get(AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE).toString());
            final long responseTimeInDb = axeUpgradeServiceHelper.getResponseTimeStampAttribute(activityJobId);
            if (responseEventTime < responseTimeInDb) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param jobPropertyList
     * @param jobLogList
     * @param progressPercentage
     * @param status
     */
    protected void processNotStartedState(final boolean islatestResponse, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName, final Double progressPercentage,
            final OPSScriptExecStatus status, final String message) {
        if (islatestResponse) {
            boolean isPersisted;
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            addJobLogForStateAndPercentage(activityName, status.toString(), progressPercentage, jobLogList);
            isPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            if (isPersisted) {
                activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, message);
            }
        }
    }

    /**
     * @param opsResponseAttributes
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param progressPercentage
     * @param status
     * @param message
     */
    protected void processRunningState(final boolean islatestResponse, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName, final Double progressPercentage,
            final OPSScriptExecStatus status, final String message) {
        boolean isPersisted;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final boolean isScriptResumed = isScriptExecutionResumedAfterInterrupted(neJobStaticData.getNeJobId());
        if (isScriptResumed) {
            activityUtils.addJobLog(String.format(JobLogConstants.AXE_SCRIPT_EXECUTION_RESUMED, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
        if (islatestResponse) {
            isPersisted = persistNeJobState(neJobStaticData, JobState.RUNNING.getJobStateName(), activityJobId);
            if (isPersisted) {
                addJobLogForStateAndPercentage(activityName, status.toString(), progressPercentage, jobLogList);
                updateActivityAndNeJobProgressPercentage(null, jobLogList, progressPercentage, activityJobId, neJobStaticData);
                activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, message);
            }
        }
    }
    
    protected void addJobLogForStateAndPercentage(final String activityName, final String status, final Double progressPercentage, final List<Map<String, Object>> jobLogList) {
        activityUtils.addJobLog(String.format(JobLogConstants.AXE_ACTIVITY_SCRIPT_MESSAGE, activityName, status, progressPercentage), JobLogType.NE.toString(), jobLogList,
                JobLogLevel.INFO.toString());
    }

    private boolean isScriptExecutionResumedAfterInterrupted(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final String state = (String) activityJobAttributes.get(ShmConstants.STATE);
        return JobState.getJobState(state) == JobState.INTERRUPTED;
    }

    /**
     * @param opsResponseAttributes
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param progressPercentage
     * @param status
     * @param message
     */
    protected void processInterruptedState(final boolean islatestResponse, final NEJobStaticData neJobStaticData, final Map<String, Object> opsResponseAttributes, final String activityName, final Double progressPercentage,
            final OPSScriptExecStatus status, final String message) {
        boolean isPersisted;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final long activityJobId = (long) opsResponseAttributes.get(AxeUpgradeActivityConstants.ACTIVITY_ID);
        if (islatestResponse) {
            isPersisted = persistNeJobState(neJobStaticData, JobState.INTERRUPTED.getJobStateName(), activityJobId);
            if (isPersisted) {
                final String exceptionMessage = (String) opsResponseAttributes.get(AxeUpgradeActivityConstants.EXCEPTION_MESSAGE);
                updatingJobLogBasedOnExceptionMessage(activityName, jobLogList, exceptionMessage, JobLogConstants.AXE_SCRIPT_EXECUTION_INTERRUPTED_REASON, JobLogConstants.AXE_SCRIPT_EXECUTION_INTERRUPTED);
                addJobLogForStateAndPercentage(activityName, status.toString(), progressPercentage, jobLogList);
                updateActivityAndNeJobProgressPercentage(null, jobLogList, progressPercentage, activityJobId, neJobStaticData);
                activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, message);
            }
        }
    }

    /**
     * @param opsResponseAttributes
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param jobPropertyList
     * @param jobLogList
     * @param progressPercentage
     * @param status
     */
    protected void processWaitForInputState(final Map<String, Object> opsResponseAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName,
            final Double progressPercentage, final OPSScriptExecStatus status, final String message) {
        boolean isPersisted;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        if (isScriptExecutionResumedAfterInterrupted(neJobStaticData.getNeJobId())) {
            activityUtils.addJobLog(String.format(JobLogConstants.AXE_SCRIPT_EXECUTION_RESUMED, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
        isPersisted = persistNeJobState(neJobStaticData, JobState.WAIT_FOR_SCRIPT_INPUT.getJobStateName(), activityJobId);
        if (isPersisted) {
            prepareJobLogAndPropertyForUserInputState(activityName, jobLogList, status.toString(), progressPercentage, opsResponseAttributes);
            updateActivityAndNeJobProgressPercentage(null, jobLogList, progressPercentage, activityJobId, neJobStaticData);
            activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, message);
        }
    }

    /**
     * @param neJobStaticData
     */
    protected boolean persistNeJobState(final NEJobStaticData neJobStaticData, final String state, final long activityJobId) {
        if (!jobConfigurationService.isJobResultEvaluated(activityJobId)) {
            final Map<String, Object> jobAttributes = new HashMap<>();
            jobAttributes.put(ShmConstants.STATE, state);
            jobUpdateService.updateJobAttributes(neJobStaticData.getNeJobId(), jobAttributes);
            return true;
        }
        return false;
    }

    protected boolean updateActivityAndNeJobProgressPercentage(final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final Double progressPercentage,
            final long activityJobId, final NEJobStaticData neJobStaticData) {
        final boolean persisted = axeUpgradeServiceHelper.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, progressPercentage);
        if (persisted) {
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        }
        return persisted;
    }

    /**
     * @param opsResponseAttributes
     * @param activityName
     * @param jobPropertyList
     * @param jobLogList
     */
    protected void prepareJobLogAndPropertyForFailedState(final Map<String, Object> opsResponseAttributes, final String activityName, final List<Map<String, Object>> jobPropertyList,
            final List<Map<String, Object>> jobLogList) {
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        String exceptionMessage = null;
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.EXCEPTION_MESSAGE) != null) {
            exceptionMessage = (String) opsResponseAttributes.get(AxeUpgradeActivityConstants.EXCEPTION_MESSAGE);
        }
        logger.debug("Failing the status for activityName {} with exception {}", activityName, exceptionMessage);
        updatingJobLogBasedOnExceptionMessage(activityName, jobLogList, exceptionMessage,JobLogConstants.AXE_SCRIPT_EXECUTION_FAILED_REASON,JobLogConstants.AXE_SCRIPT_EXECUTION_FAILED);
    }

    /**
     * @param activityName
     * @param jobLogList
     * @param exceptionMessage
     */
    private void updatingJobLogBasedOnExceptionMessage(final String activityName, final List<Map<String, Object>> jobLogList, final String exceptionMessage, final String logMessageWithReason, final String logMesageWithOutReason) {
        if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
            activityUtils.addJobLog(String.format(logMessageWithReason, activityName, exceptionMessage), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }else{
            activityUtils.addJobLog(String.format(logMesageWithOutReason, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
    }
    

    /**
     * @param activityName
     * @param jobPropertyList
     * @param jobLogList
     */
    protected void prepareJobLogAndPropertyForFinishedState(final String activityName, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        activityUtils.addJobLog(String.format(JobLogConstants.AXE_SCRIPT_EXECUTION_COMPLETED_SUCCESSFULLY, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
    }

    /**
     * @param activityName
     * @param jobPropertyList
     * @param jobLogList
     * @param status
     */
    protected void prepareJobLogAndPropertyForUserInputState(final String activityName, final List<Map<String, Object>> jobLogList, final String status, final Double progressPercentage,
            final Map<String, Object> opsResponseAttributes) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ShmConstants.STATE, JobState.WAIT_FOR_SCRIPT_INPUT.getJobStateName());
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE) != null) {
            final String responseEventTime = (String) opsResponseAttributes.get(AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE);
            activityUtils.prepareJobPropertyList(jobPropertyList, AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE, responseEventTime);
        }
        addJobLogForStateAndPercentage(activityName, status, progressPercentage, jobLogList);
    }

    protected void checkForSyncActivityToNotify(final long activityJobId, final NEJobStaticData neJobStaticData, final int activityOrder) throws MoNotFoundException {
        final String neType = getNeType(neJobStaticData);
        logger.debug("checkForSyncActivityToNotify for activity {} and netype {}", activityJobId, neType);
        synchronousActivityProcessor.checkAndNotifySynchronousActivities(neJobStaticData, activityOrder, neType);
    }

    protected void failOtherNeJobsIfActvitityisSync(final long activityJobId, final NEJobStaticData neJobStaticData, final int activityOrder) throws MoNotFoundException {
        final String neType = getNeType(neJobStaticData);
        logger.info("cancelOtherNeJobsIfActvitityisSync for netype {},{}", neType, activityJobId);
        synchronousActivityProcessor.failOtherNeJobsIfActvitityisSync(neJobStaticData.getMainJobId(), neJobStaticData.getNeJobId(), neType, activityOrder);
    }

    protected void persistLogIfInvalidStatusReceived(final long activityJobId, final OPSScriptExecStatus status, final Map<String, Object> opsResponseAttributes) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        logger.error("Unknown or invalid job status is received : {}  ", status);
        activityUtils.addJobLog(String.format(JobLogConstants.AXE_ACTIVITY_INVALID_STATUS_MESSAGE, opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS)), JobLogType.NE.toString(), jobLogList,
                JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    protected void persistLogIfStatusNotReceived(final long activityJobId, final Map<String, Object> opsResponseAttributes) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        logger.info("Job status is not received for the activity Job id {}", activityJobId);
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.EXCEPTION_MESSAGE) != null) {
            activityUtils.addJobLog(String.format((String)opsResponseAttributes.get(AxeUpgradeActivityConstants.EXCEPTION_MESSAGE) ), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    } 
    protected void persistLogIfImproperProgressPercentageReceived(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        logger.error("Improper percentage is received for the activity Job id {}", activityJobId);
        activityUtils.addJobLog(JobLogConstants.AXE_ACTIVITY_IMPROPER_PROGRESS_RECEIVED_MESSAGE, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    /**
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param activityOrder
     * @param jobPropertyList
     * @param jobLogList
     * @param progressPercentage
     * @throws MoNotFoundException
     */
    protected void processSuccessCase(final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName, final int activityOrder,
            final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final Double progressPercentage) throws MoNotFoundException {
        boolean isPersisted;
        prepareJobLogAndPropertyForFinishedState(activityName, jobPropertyList, jobLogList);
        isPersisted = updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
        if (isPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
            checkForSyncActivityToNotify(activityJobId, neJobStaticData, activityOrder);
        }
    }

    /**
     * @param neJobStaticData
     * @return
     * @throws MoNotFoundException
     */
    private String getNeType(final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        String neType = null;
        if (neJobStaticData.getParentNodeName() != null) {
            neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
        } else {
            neType = networkElementRetrivalBean.getNeType(neJobStaticData.getNodeName());
        }
        return neType;
    }

}
