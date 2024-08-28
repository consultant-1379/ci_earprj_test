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
package com.ericsson.oss.services.shm.system.restore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.LatestNeWorkFlowDefinitions;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.shared.util.ProcessVariablesUtil;
import com.ericsson.oss.services.shm.system.restore.common.JobQueryService;
import com.ericsson.oss.services.shm.system.restore.common.JobRestoreHandlingServiceUtil;
import com.ericsson.oss.services.shm.system.restore.common.JobRestoreLogConstants;
import com.ericsson.oss.services.shm.system.restore.common.MainJob;
import com.ericsson.oss.services.shm.workflow.BatchWorkFlowProcessVariables;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryBuilder;
import com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.Restriction;
import com.ericsson.oss.services.wfs.api.query.RestrictionBuilder;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;

/**
 * 
 * @author xcharoh
 */
@Traceable
@Profiled
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobRecoveryServiceImpl implements JobRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRecoveryServiceImpl.class);

    private static final int MAX_BATCH_SIZE = 10;

    @Inject
    WorkflowInstanceNotifier workflowInstanceHelper;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    JobRestoreHandlingServiceUtil jobRestoreHandlingServiceUtil;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    private ProcessVariablesUtil processVariablesUtil;

    @Inject
    JobQueryService jobQueryService;

    @Inject
    private CheckPeriodicity checkPeriodicity;

    @Override
    @Asynchronous
    public Future<List<JobRestoreResult>> handleJobRestore() {

        final List<JobRestoreResult> jobRestoreResult = new ArrayList<JobRestoreResult>();
        LOGGER.debug("Getting Suspended Job Details");
        final List<MainJob> mainJobAttributesList = jobRestoreHandlingServiceUtil.getSuspendedJobDetails();
        LOGGER.debug("Main job attributes size : {}", mainJobAttributesList.size());
        if (mainJobAttributesList.size() > 0) {

            for (final MainJob mainJobAttributes : mainJobAttributesList) {

                //Cancel the suspended workflow for the active job.
                cancelWorkflows(mainJobAttributes.getMainWorkflowInstanceId());
                //Cancel the active job.
                cancelJob(mainJobAttributes);

                LOGGER.debug("Check for Periodic Job for templateJob {}", mainJobAttributes.getTemplateJobId());

                jobRestoreResult.add(new JobRestoreResult(mainJobAttributes.getMainJobId(), mainJobAttributes.getExecutionIndex()));
            }
        }

        // It will return a list of Jobs whose workflow doesn't exist but job is in active state.
        LOGGER.debug("Getting suspended job details if any whose workflow doesn't exist");
        final List<MainJob> mainJobInActiveStateList = jobQueryService.getMainJobsInActiveState();
        LOGGER.debug("Main job attributes whose workflow doesn't exist {}", mainJobInActiveStateList.size());
        if (mainJobInActiveStateList.size() > 0) {
            for (final MainJob mainJobInActiveStateAttributes : mainJobInActiveStateList) {

                //Cancel the active job.
                cancelJob(mainJobInActiveStateAttributes);
                jobRestoreResult.add(new JobRestoreResult(mainJobInActiveStateAttributes.getMainJobId(), mainJobInActiveStateAttributes.getExecutionIndex()));
            }
        }

        //Re-submit batch workflow for periodic job.
        for (final MainJob mainJobAttributes : mainJobAttributesList) {
            checkAndResubmitBatchWorkflow(mainJobAttributes);
        }

        return new AsyncResult<List<JobRestoreResult>>(jobRestoreResult);
    }

    /**
     * This method will handle suspended NE jobs.
     * 
     * @param mainJobId
     * @param jobName
     */
    public void handleNeJobRestore(final long mainJobId, final String jobName, final int executionIndex) {
        LOGGER.debug("Inside JobRecoveryServiceImpl.handleNeJobRestore() with main job id: {} and job name:{}", mainJobId, jobName);
        final List<NEJob> neJobAttributesList = jobRestoreHandlingServiceUtil.getSuspendedNEJobDetails(mainJobId);
        if (neJobAttributesList != null && !neJobAttributesList.isEmpty()) {
            LOGGER.debug("Got {} NE job Attributes list from jobRestoreHandlingServiceUtil.getSuspendedNEJobDetails(mainJobId) for {}", neJobAttributesList.size(), mainJobId);

            final List<List<NEJob>> batchedNeJobAttributesList = ListUtils.partition(neJobAttributesList, MAX_BATCH_SIZE);
            if (batchedNeJobAttributesList.size() > 0) {
                for (final List<NEJob> batchedNeJobAttributes : batchedNeJobAttributesList) {

                    //Cancel the suspended workflow for the active job.
                    LOGGER.debug("Cancelling Suspended workflows of {} Batched NE jobs", MAX_BATCH_SIZE);
                    for (final NEJob neJobAttributes : batchedNeJobAttributes) {
                        cancelWorkflows(neJobAttributes.getNeWorkflowInstanceId());
                    }

                    //Cancel the active Ne jobs.
                    LOGGER.debug("System Cancelling {} Batched NE jobs", MAX_BATCH_SIZE);
                    cancelNeJob(batchedNeJobAttributes, jobName, executionIndex);
                }
            }
        }
    }

    /**
     * This method will cancel the all active NE jobs and Activity jobs.
     * 
     * @param batchedNeJobAttributes
     * @param jobName
     */
    private void cancelNeJob(final List<NEJob> batchedNeJobAttributes, final String jobName, final int executionIndex) {
        LOGGER.debug("Inside JobRecoveryServiceImpl.handleNeJobRestore(--) with job name:{}", jobName);
        final Map<Long, Map<String, Object>> batchNeJobsToBeUpdated = new HashMap<Long, Map<String, Object>>();
        final Map<String, Object> attributesToBePersisted = new HashMap<String, Object>();
        final List<Long> neJobIds = new ArrayList<Long>();
        for (final NEJob neJob : batchedNeJobAttributes) {
            neJobIds.add(neJob.getNeJobId());
        }
        //Canceling Activities of Batched NE Jobs
        jobQueryService.cancelActivitiesAndUpdateState(neJobIds, jobName, executionIndex);
        //updating Batch NE job state to  SYSTEM_CANCELLED, After Cancelling activity job
        for (final NEJob neJobAttributes : batchedNeJobAttributes) {
            final String cancelledLogMessage = String.format(JobRestoreLogConstants.NE_SYSTEM_CANCELLED, jobName, neJobAttributes.getNodeName());
            LOGGER.debug("System Cancelling {}'s Job with Message: {}", neJobAttributes.getNodeName(), cancelledLogMessage);
            prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage, attributesToBePersisted);
            batchNeJobsToBeUpdated.put(neJobAttributes.getNeJobId(), attributesToBePersisted);
            final String additionalInfo = "Initial State = " + neJobAttributes.getState();
            systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, neJobAttributes.getNodeName(), additionalInfo);
        }
        jobRestoreHandlingServiceUtil.updateJobsInBatch(batchNeJobsToBeUpdated);
    }

    /**
     * @param mainJobId
     */
    private void cancelJob(final MainJob mainJobAttributes) {
        LOGGER.debug("Cancelling Job for the JobId:{}", mainJobAttributes.getMainJobId());
        final Map<String, Object> attributesToBePersisted = new HashMap<String, Object>();
        final String jobName = mainJobAttributes.getJobName();
        final int executionIndex = mainJobAttributes.getExecutionIndex();
        final JobState initialJobState = mainJobAttributes.getMainJobState();
        final long mainJobId = mainJobAttributes.getMainJobId();
        LOGGER.debug("Job name {}, index {}, state {}, mainJobId {}", jobName, executionIndex, initialJobState, mainJobId);
        final String cancellingLogMessage = String.format(JobRestoreLogConstants.SYSTEM_CANCELLING, mainJobAttributes.getJobName());

        prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLING, cancellingLogMessage, attributesToBePersisted);
        LOGGER.info("Job attributes to be persisted are : {}", attributesToBePersisted);
        dpsWriter.update(mainJobId, attributesToBePersisted);
        if (!JobState.isJobInactive(initialJobState)) {
            final String additionalInfo = "Initial State = " + initialJobState + " and Execution Index = " + Integer.toString(executionIndex);
            systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLING, EventLevel.COARSE, jobName, jobName, additionalInfo);

            handleNeJobRestore(mainJobId, jobName, executionIndex);

            final String cancelledLogMessage = String.format(JobRestoreLogConstants.SYSTEM_CANCELLED, mainJobAttributes.getJobName());
            prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage, attributesToBePersisted);
            LOGGER.debug("Job attributes to be persisted are : {}", attributesToBePersisted);
            dpsWriter.update(mainJobId, attributesToBePersisted);

            LOGGER.debug("Attributes persisted");

            systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, jobName, additionalInfo);
            LOGGER.debug("Cancelled Job for the JobId:{}", mainJobAttributes.getMainJobId());
        }
    }

    /**
     * @param attributesToBePersisted
     * @param systemCancelling
     * @param jobName
     * @param initialJobState
     * @param executionIndex
     */
    private Map<String, Object> prepareAttributesToBeUpdated(final JobState state, final String logMessage, final Map<String, Object> attributesToBePersisted) {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        attributesToBePersisted.put(ShmConstants.STATE, state.toString());
        attributesToBePersisted.put(ShmConstants.LOG, jobLogList);

        return attributesToBePersisted;
    }

    /**
     * @param wfsId
     */
    private void cancelWorkflows(final String wfsId) {
        LOGGER.debug("Cancelling Workflow with workflow service id: {}", wfsId);
        try {
            workflowInstanceHelper.cancelWorkflowInstance(wfsId);
            LOGGER.debug("Cancelled Workflow with workflow service id: {}", wfsId);
        } catch (final Exception e) {
            LOGGER.error("Exception occured in Canceling workflow for WFS Id {} because {}", wfsId, e.getMessage());
        }
    }

    /**
     * @param templateJobId
     */
    private void checkAndResubmitBatchWorkflow(final MainJob mainJobAttributes) {

        Map<String, Object> processVariables = new HashMap<String, Object>();

        boolean isPeriodicJob = false;
        final Schedule schedule = mainJobAttributes.getMainSchedule();
        final List<ScheduleProperty> scheduleAttributeList = schedule.getScheduleAttributes();

        final List<Map<String, Object>> scheduledProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> attributeList = new HashMap<String, Object>();

        final ExecMode exeMode = schedule.getExecMode();
        LOGGER.debug("Main job execution mode: {}", schedule.getExecMode());

        if (ExecMode.SCHEDULED.toString().equalsIgnoreCase(exeMode.toString())) {
            if (scheduleAttributeList != null && !scheduleAttributeList.isEmpty()) {
                LOGGER.debug("Schedule attributes: {}", scheduleAttributeList.size());
                for (final ScheduleProperty scheduleProperty : scheduleAttributeList) {
                    attributeList.put(scheduleProperty.getName(), scheduleProperty.getValue());
                }
                scheduledProperties.add(attributeList);
                isPeriodicJob = checkPeriodicity.isJobPeriodic(scheduledProperties);
            }
        }
        final long templateJobId = mainJobAttributes.getTemplateJobId();
        final JobType jobType = mainJobAttributes.getJobType();
        LOGGER.debug("IsPeriodicJob {}", isPeriodicJob);
        if (isPeriodicJob) {
            try {
                processVariables = processVariablesUtil.setProcessVariablesForSchedule(scheduleAttributeList, processVariables);
                processVariables.put(BatchWorkFlowProcessVariables.TEMPLATE_JOB_ID, templateJobId);
                processVariables.put(BatchWorkFlowProcessVariables.BATCH_STARTUP, exeMode.toString().toLowerCase());
                processVariables.put(BatchWorkFlowProcessVariables.JOB_TYPE, jobType);
                processVariables.put(BatchWorkFlowProcessVariables.IS_JOB_RESUMED, true);
                LOGGER.debug("Process variables : {}", processVariables);
                final WorkflowInstance workflowInstance = workflowInstanceHelper.submitAndGetWorkFlowInstance(Long.toString(templateJobId), processVariables);
                final Map<String, Object> attributes = new HashMap<String, Object>();
                attributes.put(ShmConstants.WFS_ID, workflowInstance.getId());
                dpsWriter.update(templateJobId, attributes);
            } catch (final Exception exception) {
                LOGGER.error("Unable to re-submit batch workflow of {} because :", templateJobId, exception);
                systemRecorder.recordEvent(SHMEvents.RESUBMIT_FAILED, EventLevel.COARSE, String.valueOf(templateJobId), String.valueOf(jobType), exception.getMessage());
            }
        }
    }

    /**
     * This method queries work flow service for suspended batch work flows.
     * 
     * @return List<WorkflowObject>
     */
    @Override
    public void cancelAllWorkFlows() {
        final QueryBuilder queryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
        final Query query = queryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
        final RestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction suspendedStateRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.STATE, WorkflowInstanceQueryAttributes.State.SUSPENDED);
        final Restriction activeStateRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.STATE, WorkflowInstanceQueryAttributes.State.ACTIVE);

        final Restriction backupWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID,
                LatestNeWorkFlowDefinitions.CPP_BACKUP_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        final Restriction deleteBackupWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID,
                LatestNeWorkFlowDefinitions.CPP_DELETE_CV_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        final Restriction licenseWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID,
                LatestNeWorkFlowDefinitions.CPP_LICENSE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        final Restriction restoreWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID,
                LatestNeWorkFlowDefinitions.CPP_RESTORE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        final Restriction upgradeWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID,
                LatestNeWorkFlowDefinitions.CPP_UPGRADE_WORKFLOW_DEF_ID.getWorkFlowDefinition());

        final Restriction neWorkflowRestriction = restrictionBuilder.anyOf(suspendedStateRestriction, activeStateRestriction, backupWorkflowRestriction, deleteBackupWorkflowRestriction,
                licenseWorkflowRestriction, restoreWorkflowRestriction, upgradeWorkflowRestriction);
        query.setRestriction(neWorkflowRestriction);

        List<WorkflowObject> batchWorkFlowList = new ArrayList<WorkflowObject>();
        LOGGER.debug("Executing WFS query {} ", query);
        try {
            batchWorkFlowList = workflowInstanceHelper.executeWorkflowQuery(query);
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("Failed to execute workflow query, because:", e);
        }
        String workflowInstanceId = null;
        for (final WorkflowObject workflow : batchWorkFlowList) {
            try {
                workflowInstanceId = (String) workflow.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID);
                workflowInstanceHelper.cancelWorkflowInstance(workflowInstanceId);
            } catch (final WorkflowServiceInvocationException e) {
                LOGGER.error("Failed to cancel workflow instance, because:", e);
                systemRecorder.recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, workflowInstanceId, "", e.getMessage());
            }
        }

        LOGGER.info("Cancelled all workFlows!");

    }

}
