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
package com.ericsson.oss.services.shm.job.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobservice.common.HungJobQueryService;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * In case jobs are running for long time. Application will identify the jobs as hanging jobs.
 * 
 * @author xghamdg
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HungJobsMarkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HungJobsMarkerService.class);
    private static final String NE_SYSTEM_CANCELLED = "For \"%s\", \"%s\"'s Job is System Cancelled.";
    private static final String SYSTEM_CANCELLED = "\"%s\" is System Cancelled.";
    private static final String IS_ACTIVITY_JOBS_UPDATED = "isActivityJobsUpdated";

    @Inject
    private HungJobsConfigParamChangeListener hungJobsConfigParamChangeListener;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceHelper;

    @Inject
    private HungJobQueryService hungJobQueryService;

    @Inject
    private SHMLoadControllerLocalService shmLoadControllerLocalService;

    @Inject
    private CheckPeriodicity checkPeriodicity;

    /**
     * This method will identify all the hung jobs as per running time limits for job execution provided as configuration parameter and update the jobs to be system cancelled
     * 
     */
    @SuppressWarnings("unchecked")
    public void updateHungJobsToSystemCancelled() {
        try {
            final int maxTimeLimitForJobExecutionInHours = hungJobsConfigParamChangeListener.getMaxTimeLimitForJobExecutionInHours();

            if (maxTimeLimitForJobExecutionInHours <= 0) {
                LOGGER.error("Failed to get value of configuration parameter MaxTimeLimitForJobExecutionInHours : {}", maxTimeLimitForJobExecutionInHours);
                return;
            }

            final List<Object[]> mainJobAttributes = hungJobQueryService.getLongRunningJobs(maxTimeLimitForJobExecutionInHours);
            LOGGER.info("Total number of hung jobs {} which are hanging for more than {} hours.", mainJobAttributes.size(), maxTimeLimitForJobExecutionInHours);

            if (!mainJobAttributes.isEmpty()) {
                for (final Object[] mainJobAttribute : mainJobAttributes) {
                    try {
                        final long mainJobId = (long) mainJobAttribute[0];
                        final int executionIndex = (int) mainJobAttribute[1];
                        final String state = (String) mainJobAttribute[2];
                        final long jobTemplateId = (long) mainJobAttribute[3];
                        final String businessKey = (String) mainJobAttribute[4];
                        final List<Map<String, Object>> schedulePropertiesList = (List<Map<String, Object>>) mainJobAttribute[5];

                        String mainJobName = "";
                        String jobType = "";

                        final Map<String, Object> jobNameAndWorkflowAttributes = hungJobQueryService.getJobNameAndWorkflowId(jobTemplateId);
                        if (!jobNameAndWorkflowAttributes.isEmpty()) {
                            mainJobName = (String) jobNameAndWorkflowAttributes.get(ShmConstants.NAME);
                            jobType = (String) jobNameAndWorkflowAttributes.get(ShmConstants.JOB_TYPE);
                        }

                        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = hungJobsConfigParamChangeListener.getMaxTimeLimitForAxeUpgradeJobExecutionInHours();
                        final List<NEJob> neJobs = hungJobQueryService.getHungNeJobs(mainJobId, maxTimeLimitForJobExecutionInHours, maxTimeLimitForAxeUpgradeJobExecutionInHours, jobType);
                        boolean systemCancelMainJob = false;
                        if (neJobs == null || neJobs.isEmpty()) {
                            LOGGER.debug("No hung Ne Job found for the main Job: {}, Job name:{}", mainJobId, mainJobName);
                        }
                        for (final NEJob neJob : neJobs) {
                            systemCancelMainJob = true;
                            LOGGER.debug("System Cancelled NE Jobs and respective Activity Jobs, Node Name: {} Job state: {},platform :{}", neJob.getNodeName(), neJob.getState().getJobStateName(),
                                    neJob.getPlatformType());
                            neJob.setJobType(jobType);
                            boolean isNeJobUpdated = false;
                            if (PlatformTypeEnum.AXE.equals(PlatformTypeEnum.getPlatform(neJob.getPlatformType()))) {
                                LOGGER.debug("For AXE check nejob running for more than {}", maxTimeLimitForAxeUpgradeJobExecutionInHours);
                                isNeJobUpdated = cancelNeAndActivityJobs(neJob, mainJobName, executionIndex, maxTimeLimitForAxeUpgradeJobExecutionInHours);
                            } else {
                                isNeJobUpdated = cancelNeAndActivityJobs(neJob, mainJobName, executionIndex, maxTimeLimitForJobExecutionInHours);
                            }
                            if (!isNeJobUpdated) {
                                systemCancelMainJob = false;
                                break;
                            }
                        }
                        // If NE jobs are in Running state. mainJob will not update to SYSTEM CANCELLED
                        final boolean isRunningNeJobsExist = hungJobQueryService.checkRunningNeJobs(mainJobId);
                        if ((systemCancelMainJob || systemCancelMainJobWithNoNEJobs(state, neJobs)) && !isRunningNeJobsExist) {
                            //Update main job to SYSTEM_CANCELLED
                            updateMainJobsToSystemCancelled(mainJobId, mainJobName, executionIndex, state);

                            //Check Main Job Periodicity
                            final boolean periodicJob = checkPeriodicity.isJobPeriodic(schedulePropertiesList);
                            if (periodicJob) {
                                //Notify to WFS -- allNeDone
                                sendAllNeDone(businessKey);
                            } else {
                                //Cancel main job workflows
                                cancelMainJobWorkflows(jobNameAndWorkflowAttributes);
                            }
                        }

                    } catch (final Exception e) {
                        LOGGER.error("Exception while identifying Hung NE Job and corresponding Activity Job for the main Job id: {} -Reason {} ", mainJobAttribute[0], e);
                    }
                }
            }

        } catch (final Exception exception) {
            LOGGER.error("{} happened while system cancellation of jobs. Stacktrace : ", exception, exception);
        }
    }

    private boolean systemCancelMainJobWithNoNEJobs(final String state, final List<NEJob> neJobs) {
        return (neJobs == null || neJobs.isEmpty())
                && ((JobState.getJobState(state) == JobState.CREATED) || (JobState.getJobState(state) == JobState.CANCELLING) || (JobState.getJobState(state) == JobState.RUNNING));
    }

    /**
     * This method will update mainJob JobState to SYSTEM CANCELLED
     * 
     * @param mainJobId
     * @param jobName
     * @param executionIndex
     * @param initialJobState
     */
    private void updateMainJobsToSystemCancelled(final long mainJobId, final String jobName, final int executionIndex, final String initialJobState) {

        LOGGER.debug("Job name {}, index {}, state {}, mainJobId {}", jobName, executionIndex, initialJobState, mainJobId);
        final String cancelledLogMessage = String.format(SYSTEM_CANCELLED, jobName, jobName);
        final Map<String, Object> attributesToBeUpdated = hungJobQueryService.prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage);

        jobUpdateService.updateJobAttributes(mainJobId, attributesToBeUpdated);

        final String additionalInfo = "Initial State = " + initialJobState + " and Execution Index = " + Integer.toString(executionIndex);
        systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, jobName, additionalInfo);
        LOGGER.debug("SYSTEM CANCELLED MainJob for the JobName:{} JobId:{}", jobName, mainJobId);

    }

    /**
     * Cancel mainJob work flow
     * 
     * @param jobTemplateIds
     */
    private void cancelMainJobWorkflows(final Map<String, Object> jobNameAndWorkflowAttributes) {
        LOGGER.debug("Canceling main job workflows");

        if (!jobNameAndWorkflowAttributes.isEmpty()) {
            final String wfsId = (String) jobNameAndWorkflowAttributes.get(ShmConstants.WFS_ID);
            cancelWorkflows(wfsId);
        }

    }

    /**
     * This method will cancel the all active NE jobs and Activity jobs and their respective work flow. If any of NE or Activity jobs are in scheduled or waiting for user input this method return
     * false
     * 
     * @param batchedNeJobAttributes
     * @param jobName
     * @param executionIndex
     * @param maxTimeLimitForJobExecutionInHours
     * @return
     */
    private boolean cancelNeAndActivityJobs(final NEJob neJob, final String jobName, final int executionIndex, final int maxTimeLimitForJobExecutionInHours) {
        LOGGER.debug("Inside HungJobsMarkerService.cancelNeAndActivityJobs(--) with job name:{}", jobName);

        final Map<Long, Map<String, Object>> batchNeJobsToBeUpdated = new HashMap<Long, Map<String, Object>>();
        final Map<String, Object> activityJobsUpdatedStatusMap = hungJobQueryService.cancelActivitiesAndUpdateState(neJob.getNeJobId(), jobName, executionIndex, maxTimeLimitForJobExecutionInHours);
        String activityName = null;
        if (activityJobsUpdatedStatusMap != null && !activityJobsUpdatedStatusMap.isEmpty()) {
            activityName = (String) activityJobsUpdatedStatusMap.get(ShmConstants.ACTIVITYNAME);
            final boolean isActivityJobsUpdated = (boolean) activityJobsUpdatedStatusMap.get(IS_ACTIVITY_JOBS_UPDATED);
            if (!isActivityJobsUpdated) {
                return false;
            }
        }
        LOGGER.debug("Cancelling workflows of  NE jobs");
        decrementLoadControlValue(neJob, activityName);
        cancelWorkflows(neJob.getNeWorkflowInstanceId());

        final String cancelledLogMessage = String.format(NE_SYSTEM_CANCELLED, jobName, neJob.getNodeName());
        LOGGER.debug("System Cancelling {}'s Job with Message: {}", neJob.getNodeName(), cancelledLogMessage);
        final Map<String, Object> attributesToBeUpdated = hungJobQueryService.prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage);
        batchNeJobsToBeUpdated.put(neJob.getNeJobId(), attributesToBeUpdated);
        final String additionalInfo = "Initial State = " + neJob.getState();
        systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, neJob.getNodeName(), additionalInfo);
        hungJobQueryService.updateJobsInBatch(batchNeJobsToBeUpdated);
        return true;
    }

    /**
     * This method is used to decrement the load control value for the activity sprcified.
     * 
     * @param neJob
     * @param activityName
     */
    private void decrementLoadControlValue(final NEJob neJob, final String activityName) {
        final SHMActivityRequest shmActivityRequest = new SHMActivityRequest();
        shmActivityRequest.setActivityName(activityName);
        shmActivityRequest.setPlatformType(neJob.getPlatformType());
        shmActivityRequest.setJobType(neJob.getJobType());
        shmLoadControllerLocalService.decrementCounter(shmActivityRequest);
        final SHMLoadControllerCounterRequest shmStagedActivityRequest = new SHMLoadControllerCounterRequest();
        shmStagedActivityRequest.setActivityName(activityName);
        shmStagedActivityRequest.setPlatformType(neJob.getPlatformType());
        shmStagedActivityRequest.setJobType(neJob.getJobType());
        shmLoadControllerLocalService.decrementGlobalCounter(shmStagedActivityRequest);
    }

    /**
     * @param wfsId
     */
    private void cancelWorkflows(final String wfsId) {
        try {
            workflowInstanceHelper.cancelWorkflowInstance(wfsId);
            LOGGER.debug("Cancelled Workflow with id: {}", wfsId);
        } catch (final Exception e) {
            LOGGER.error("Exception occured in Canceling workflow for WFS Id {} because {}", wfsId, e);
        }
    }

    private void sendAllNeDone(final String businessKey) {
        try {
            workflowInstanceHelper.sendAllNeDone(businessKey);
            LOGGER.info("Batch Workflow instance notified for business key: {}", businessKey);
        } catch (final Exception e) {
            LOGGER.error("Exception occured in notifying workflow for business key {} because {}", businessKey, e);
        }

    }

    /**
     * This method will retrieve ShmStagedActivity POs which are in "READY" state & more than 48 hrs and then delete them
     */
    public void deleteStagedActivityPOs() {
        try {
            LOGGER.debug("deletion started for StagedActivity POs");
            final int maxTimeLimitForStagedActivitiesInHours = hungJobsConfigParamChangeListener.getMaxTimeLimitForStagedActivitiesInHours();
            if (maxTimeLimitForStagedActivitiesInHours <= 0) {
                LOGGER.error("Failed to get value of configuration parameter MaxTimeLimitForJobExecutionInHours : {}", maxTimeLimitForStagedActivitiesInHours);
                return;
            }
            final List<PersistenceObject> stagedPersistenceObjects = hungJobQueryService.getStagedActivityPOs(maxTimeLimitForStagedActivitiesInHours);
            LOGGER.info("Total number of stagedPersistenceObjects {} which are in READY state for more than {} hours.", stagedPersistenceObjects.size(), maxTimeLimitForStagedActivitiesInHours);

            for (PersistenceObject persistenceObject : stagedPersistenceObjects) {
                hungJobQueryService.deleteStagedActivityPO(persistenceObject.getPoId());
            }
        } catch (final Exception exception) {
            LOGGER.error("{} occured while getting or deleting staged PersistenceObjects : ", exception);
        }

    }
}
