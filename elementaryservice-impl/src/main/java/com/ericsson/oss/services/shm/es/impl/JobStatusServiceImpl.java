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
package com.ericsson.oss.services.shm.es.impl;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.TimeSpendOnJob;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepDurationsReportGenerator;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobConsolidationService;
import com.ericsson.oss.services.shm.es.api.JobStatusService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.es.local.JobStatusServiceLocal;
import com.ericsson.oss.services.shm.es.moaction.MoActionMTRManager;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.constants.SchedulePropertyConstants;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.HealthStatus;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.loadcontrol.local.api.LoadControllerLocalCache;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;
import com.ericsson.oss.services.shm.model.events.ShmJobStatusEvent;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.impl.JobsInternalAlarmService;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Traceable
@Profiled
@SuppressWarnings("PMD")
public class JobStatusServiceImpl implements JobStatusService, JobStatusServiceLocal {

    private static final String COMPLETED_ACTIVITIES = "completedActivities";
    private static final String TOTAL_ACTIVITIES = "totalActivities";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusServiceImpl.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private DpsRetryPolicies dpsPolicies;

    @Inject
    private JobsInternalAlarmService jobsInternalAlarmService;

    @Inject
    private MainJobProgressUpdaterRetryProxy mainJobProgressNotifier;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private ActivityStepDurationsReportGenerator activityStepDurationsReportGenerator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private LoadControllerLocalCache loadControllerLocalCache;

    @Inject
    @Modeled
    private EventSender<ShmJobStatusEvent> eventSender;

    @Inject
    private ShmJobStatusEventBuilder shmJobStatusEventBuilder;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private MoActionMTRManager moActionMTRManager;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private FaBuildingBlockResponseProvider buildingBlockResponseProvider;

    @Inject
    private NEJobProgressPercentageCache neJobProgressPercentageCache;

    @Inject
    private NeJobDetailsInstrumentation neJobDetailsInstrumentation;

    @Inject
    private FaBuildingBlockResponseProcessor faBuildingBlockResponseProcessor;

    @Override
    public void updateJob(final long poId, final Map<String, Object> attrs) {
        LOGGER.trace("inside updateJob of JobStatusServiceImpl having po id {}: attributes{}", poId, attrs);
        jobUpdateService.updateJobAttributes(poId, attrs);
        LOGGER.trace("updated job of {} with the attributes: {}", poId, attrs);
    }

    @Override
    public void updateNeJobStart(final long neJobId, final JobState state) {
        LOGGER.trace("Inside updateNeJobStart of JobStatusServiceImpl having NEJob poid {} and state {}", neJobId, state);
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put("state", state.getJobStateName());
        if (JobState.RUNNING.equals(state)) {
            jobAttributes.put(ShmConstants.STARTTIME, new Date());
        }
        jobUpdateService.updateJobAttributes(neJobId, jobAttributes);
        LOGGER.trace("Updated job with {} with the attributes:{}", neJobId, jobAttributes);

    }

    @Override
    public void updateActivityJobStart(final long activityJobId, final JobState state) {
        LOGGER.trace("Inside updateActivityJobStart of JobStatusServiceImpl having ActivityJob poid {} and state {}", activityJobId, state);
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put("state", state.getJobStateName());
        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobId);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        Date startTime = new Date();
        if (JobState.RUNNING.equals(state)) {
            jobAttributes.put(ShmConstants.STARTTIME, startTime);
        } else if (JobState.SCHEDULED.equals(state)) {
            final String currentActivityName = (String) activityJobAttributes.get(ShmConstants.ACTIVITY_NAME);

            final String activityScheduleTime = getActivityScheduleTime(neJobId, currentActivityName);
            if (activityScheduleTime != null) {
                updateScheduleInformationInActivityJobLogs(activityJobId, currentActivityName, activityScheduleTime);
            }
        }
        updateActivityStartTimeInNeStartTime(neJobId, activityJobAttributes, startTime);
        jobUpdateService.updateJobAttributes(activityJobId, jobAttributes);
        LOGGER.trace("Updated job with {} with the attributes:{}", activityJobId, jobAttributes);

    }

    @Override
    public void updateActivityJobStart(final long activityJobId, final JobState state, final String jobType) {
        LOGGER.trace("Inside updateActivityJobStart of JobStatusServiceImpl having ActivityJob poid {} and state {} and jobType: {}", activityJobId, state, jobType);
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put(ShmConstants.STATE, state.getJobStateName());
        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobId);
        final String currentActivityName = (String) activityJobAttributes.get(ShmConstants.ACTIVITY_NAME);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        final Date startTime = new Date();
        if (JobState.RUNNING.equals(state)) {
            jobAttributes.put(ShmConstants.STARTTIME, startTime);

            final String capability = activityUtils.getCapabilityByJobType(jobType);
            try {
                neJobStaticDataProvider.updateNeJobStaticDataCache(activityJobId, capability, startTime.getTime());

                LOGGER.trace("Going to update currentActivityCount in Tpoic in JobStatusServiceImpl having ActivityJob poid {} and state {} and jobType: {} and currentActivityName {}", activityJobId,
                        state, jobType, currentActivityName);
            } catch (final JobDataNotFoundException ex) {
                LOGGER.error("Failed to get NE Job static data for the activityJob Id:{}. with Exception : ", activityJobId, ex);
            }
        } else if (JobState.SCHEDULED.equals(state)) {
            final String activityScheduleTime = getActivityScheduleTime(neJobId, currentActivityName);
            if (activityScheduleTime != null) {
                updateScheduleInformationInActivityJobLogs(activityJobId, currentActivityName, activityScheduleTime);
            }
        }
        updateActivityStartTimeInNeStartTime(neJobId, activityJobAttributes, startTime);
        jobUpdateService.updateJobAttributes(activityJobId, jobAttributes);
        LOGGER.trace("Updated job with {} with the attributes:{}", activityJobId, jobAttributes);

    }

    private void updateActivityStartTimeInNeStartTime(final long neJobId, final Map<String, Object> activityJobAttributes, final Date startTime) {
        final Map<String, Object> neJobAttributes = new HashMap<>();
        final int activityOrder = (int) activityJobAttributes.get(ShmConstants.ORDER);
        if (activityOrder == 1) {
            neJobAttributes.put(ShmConstants.STARTTIME, startTime);
            jobUpdateService.updateJobAttributes(neJobId, neJobAttributes);

        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void recordJobEndEvent(final long jobId) {
        final Map<String, Object> poAttributes = getPoAttributes(jobId);
        final Map<String, Object> jobConfiguration = (Map<String, Object>) poAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfiguration.get(ShmConstants.MAIN_SCHEDULE);
        final String executionMode = (String) mainSchedule.get(ShmConstants.EXECUTION_MODE);
        systemRecorder.recordEvent(SHMEvents.JOB_END, EventLevel.COARSE, executionMode + "JOB", "Job", "SHM:JOB" + ":execution ending for mainJobId " + jobId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.JobStatusService#updateNEJobEnd( long)
     */
    @Override
    public void updateNEJobEnd(final long jobId) {
        LOGGER.debug("Inside updateNEJobEnd of JobStatusServiceImpl with jobId : {}", jobId);
        jobUpdateService.updateNEJobEndAttributes(jobId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.JobStatusService# updateActivityJobEnd (long)
     */
    @Override
    public ActivityStepResultEnum updateActivityJobEnd(final long jobId) {
        return updateActivityEnd(jobId, null);
    }

    @Override
    public ActivityStepResultEnum updateActivityJobEnd(final long jobId, final String jobType) {
        return updateActivityEnd(jobId, jobType);
    }

    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum updateActivityEnd(final long jobId, final String jobType) {
        LOGGER.debug("Inside updateActivityJobEnd of JobStatusServiceImpl with jobId: {} ", jobId);
        String jobResult = JobResult.SKIPPED.getJobResult();
        Long neJobId = null;
        final Map<String, Object> activityJobPoAttributes = getPoAttributes(jobId);
        Date startTime = null;
        Date endTime = null;
        String activityName = null;
        Double activityPercentage = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.EXECUTION_SUCESS;
        if (activityJobPoAttributes != null) {
            neJobId = (long) activityJobPoAttributes.get("neJobId");
            startTime = (Date) activityJobPoAttributes.get(ShmConstants.STARTTIME);
            activityName = (String) activityJobPoAttributes.get("name");
            activityPercentage = (Double) activityJobPoAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
            if (activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
                final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
                LOGGER.debug("activity job properties of jobid {} are {}", jobId, activityJobProperties);
                for (final Map<String, Object> jobProperty : activityJobProperties) {
                    if (ActivityConstants.ACTIVITY_RESULT.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                        jobResult = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    }
                }
            }
        } else {
            LOGGER.debug("No Activity Job found with the PO Id: {}", jobId);
        }
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.RESULT, jobResult);
        activityJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        activityJobAttributes.put(ShmConstants.ENDTIME, new Date());

        jobUpdateService.updateJobAttributes(jobId, activityJobAttributes);
        LOGGER.debug("Updated ActivityJobEnd of :{} with the attributes: {}", jobId, activityJobAttributes);
        // This check is for Backward Compatability for different platform type nodes.
        if (activityPercentage == null || activityPercentage == 0.0) {
            if (neJobId != null) {
                updateNEJobProgressPercentage(neJobId);
            }
        }
        LOGGER.debug("jobResult : {} and Activity job Progress Percentage : {} ", jobResult, activityPercentage);
        if (JobResult.FAILED.getJobResult().equals(jobResult)) {
            activityStepResultEnum = ActivityStepResultEnum.EXECUTION_FAILED;
        } else if (!JobResult.CANCELLED.getJobResult().equals(jobResult)) {
            activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
            jobUpdateService.updateJobAttributes(jobId, activityJobAttributes);
        }
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        endTime = (Date) activityJobAttributes.get(ShmConstants.ENDTIME);
        activityInstrumentationRecord(endTime, startTime, activityName, neJobId);
        if (jobType != null) {
            keepLocalCounterMessageInTopic(jobId, activityName, jobType, ActivityConstants.ACTIVITY_EXECUTION_COMPLETED);
        }

        if (activityJobPoAttributes != null) {
            faBuildingBlockResponseProcessor.sendFaResponse(jobId, jobType, jobResult, activityJobPoAttributes);
        }
        neJobStaticDataProvider.clear(jobId);
        return activityStepResultEnum;
    }

    private void keepLocalCounterMessageInTopic(final long jobId, final String activityName, final String jobType, final String activityStatus) {

        final String capability = activityUtils.getCapabilityByJobType(jobType);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(jobId, capability);
            final SHMLoadControllerCounterRequest shmLoadControllerCounterRequest = new SHMLoadControllerCounterRequest();
            shmLoadControllerCounterRequest.setActivityJobId(jobId);
            shmLoadControllerCounterRequest.setActivityName(activityName);
            shmLoadControllerCounterRequest.setJobType(jobType);
            shmLoadControllerCounterRequest.setPlatformType(neJobStaticData.getPlatformType());
            shmLoadControllerCounterRequest.setActivityStatus(activityStatus);
            loadControllerLocalCache.keepMessageInTopic(shmLoadControllerCounterRequest);
            LOGGER.debug("Placed LC Request in Topic when activity {} with details : {}", activityStatus, shmLoadControllerCounterRequest);
        } catch (final JobDataNotFoundException e) {
            LOGGER.error("Error occurred while retrieving neJobStaticData ,so failed to send message to Topic for Activity job Id {}", jobId, e);
        }
    }

    private void updateNEJobProgressPercentage(final long neJobId) {

        final double progressPercentage = computeNEJobProgressPercentage(neJobId);
        LOGGER.debug("For neJobId: {} updating the NE progress percentage to :{}", neJobId, progressPercentage);
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, progressPercentage);

        jobUpdateService.updateJobAttributes(neJobId, neJobAttributes);
    }

    /**
     * @deprecated use bufferAndUpdateNEJobProgress as an alternative
     */
    @Override
    @Deprecated
    public void updateNEJobProgress(final long neJobId) {
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
    }

    @Override
    public void bufferAndUpdateNEJobProgress(final long neJobId) {
        neJobProgressPercentageCache.bufferNEJobs(neJobId);
    }

    private double computeNEJobProgressPercentage(final long neJobId) {
        double progressPercentage = 0;

        final Map<String, Object> activitiesCount = getActivitiesCount(neJobId);
        final int totalActivities = (int) activitiesCount.get(TOTAL_ACTIVITIES);
        final int completedActivities = (int) activitiesCount.get(COMPLETED_ACTIVITIES);
        LOGGER.debug("For neJobId: {} count of activities: {} count of completed activities:{}", neJobId, totalActivities, completedActivities);
        if (totalActivities > 0) {
            progressPercentage = (((double) completedActivities) * 100) / (totalActivities);
            LOGGER.debug("For neJobId: {} present NE Progress percentage: {}", neJobId, progressPercentage);
        }
        progressPercentage = Math.round(progressPercentage * 100);
        progressPercentage = progressPercentage / 100;
        return progressPercentage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updateJobProgress(final long mainjobId) {
        // Retrieving job state to check whether job is active or inactive before updating the job progress
        final Map<String, Object> jobAttributes = jobUpdateService.retrieveJobWithRetry(mainjobId);
        String jobState = (String) jobAttributes.get(ShmConstants.STATE);
        LOGGER.debug("Inside updateJobProgress of JobStatusServiceImpl with jobAttributes : {} ", jobAttributes);
        final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) jobAttributes.get(ShmConstants.JOBPROPERTIES);

        final String jobCategoryName = JobPropertyUtil.getProperty(jobPropertyList, ShmConstants.JOB_CATEGORY);
        LOGGER.debug("JobCategoryName {} ", jobCategoryName);

        // Update the job progress only if Job is active(i.e. jobstate must be other than cancelled and completed)
        String jobResult = null;
        if (!JobState.isJobInactive(JobState.getJobState(jobState))) {
            LOGGER.debug("Inside updateJobProgress of JobStatusServiceImpl with mainjobId : {} ", mainjobId);
            jobResult = JobResult.SUCCESS.getJobResult();
            if (jobPropertyList != null && checkTopologyEvaluation(jobPropertyList)) {
                jobResult = JobResult.FAILED.getJobResult();
            } else {
                jobResult = retrieveNeJobResult(mainjobId);
            }
            LOGGER.debug("The NEJob Result is:{}", jobResult);
            if (jobResult == JobResult.FAILED.getJobResult()) {
                final String jobType = jobConfigurationService.getJobType(mainjobId);
                jobsInternalAlarmService.checkIfAlarmHasToBeRaised(mainjobId, jobType);
            }
            jobState = computeMainJobState(mainjobId);
            final Map<String, Object> mainJobAttributes = new HashMap<>();
            mainJobAttributes.put(ShmConstants.ENDTIME, new Date());
            mainJobAttributes.put(ShmConstants.STATE, jobState);
            mainJobAttributes.put(ShmConstants.RESULT, jobResult);
            mainJobProgressNotifier.updateMainJobEndDetails(mainjobId, mainJobAttributes);
            mainJobInstrumentRecord(mainjobId, new Date());
            neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainjobId, jobAttributes);
            activityStepDurationsReportGenerator.generateJobReportAndUpdateMainJob(mainjobId);
            jobStaticDataProvider.clear(mainjobId);
        } else {
            LOGGER.info("Skipped to update jobprogress as job({}) is in {} state.", mainjobId, jobState);
        }

        // This event is to notify the job completion status to Auto Provision.
        sendJobStatusEvent(jobAttributes, jobCategoryName, jobState, jobResult);
    }

    private void sendJobStatusEvent(final Map<String, Object> jobAttributes, final String jobCategoryName, final String jobState, final String jobResult) {
        if (isJobCategoryRemote(jobCategoryName) && isJobStateCompleted(jobState)) {
            final ShmJobStatusEvent shmJobStatusEvent = shmJobStatusEventBuilder.buildJobStatusEvent(jobAttributes, jobResult);
            LOGGER.debug("Sending job response to remote users :{}", shmJobStatusEvent.toString());
            eventSender.send(shmJobStatusEvent);
        }
    }

    private static boolean isJobStateCompleted(final String jobState) {
        return JobState.COMPLETED.name().equalsIgnoreCase(jobState);
    }

    private boolean isJobCategoryRemote(final String jobCategoryName) {
        if (jobCategoryName != null && !jobCategoryName.isEmpty()) {
            return JobCategory.REMOTE.name().equalsIgnoreCase(jobCategoryName);
        }
        return false;
    }

    private String computeMainJobState(final long mainJobId) {
        final List<Object> neJobStates = getAssociatedNeJobsState(mainJobId);
        JobState mainJobStateTobeUpdated = JobState.COMPLETED;
        int cancelledNeJobsCount = 0;
        for (final Object nEJobState : neJobStates) {
            final JobState neJobState = JobState.getJobState((String) nEJobState);
            if (JobState.isJobCancelled(neJobState)) {
                cancelledNeJobsCount++;
            }
        }
        final int totalNeJobsCount = neJobStates.size();
        if (cancelledNeJobsCount == totalNeJobsCount) {
            mainJobStateTobeUpdated = JobState.COMPLETED;
        }
        LOGGER.debug("Main Job state computed as :{}", mainJobStateTobeUpdated);
        return mainJobStateTobeUpdated.getJobStateName();
    }

    private boolean checkTopologyEvaluation(final List<Map<String, String>> jobPropertyList) {
        for (final Map<String, String> eachJobProperty : jobPropertyList) {
            if (eachJobProperty.containsValue(ShmConstants.TOPOLOGY_EVALUATION_FAILED)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propagateCancelToMainJob(final long neJobId) {
        LOGGER.debug("Entering propagateCancelToMainJob with NE job id...{}", neJobId);
        // Find main job id using nejob id
        long mainjobId = -1;

        final Map<String, Object> neJobAttributes = getPoAttributes(neJobId);
        if (neJobAttributes != null) {
            mainjobId = (long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID);
        }

        JobState mainJobState = null;
        Long templateJobId = null;
        String wfsId = null;
        if (mainjobId != -1) {
            final Map<String, Object> mainJobAttributes = getPoAttributes(mainjobId);
            final String jobState = (String) mainJobAttributes.get(ShmConstants.STATE);
            templateJobId = (Long) mainJobAttributes.get(ShmConstants.JOBTEMPLATEID);
            wfsId = (String) mainJobAttributes.get(ShmConstants.WFS_ID);
            mainJobState = JobState.getJobState(jobState);

            LOGGER.debug("Main job state in propagateCancelToMainJob is {}  ", mainJobState.getJobStateName());
            LOGGER.debug("Template job id is {}", templateJobId);
            // update completed NE count in main job attributes
            final List<Map<String, String>> mainJobPropertyList = mainJobAttributes.get(ShmConstants.JOBPROPERTIES) != null
                    ? (List<Map<String, String>>) mainJobAttributes.get(ShmConstants.JOBPROPERTIES)
                    : new ArrayList<Map<String, String>>();

            int neCompleted = 0;
            if (mainJobPropertyList != null && mainJobPropertyList.size() > 0) {
                for (final Map<String, String> jobProperty : mainJobPropertyList) {
                    if (ShmConstants.NE_COMPLETED.equals(jobProperty.get(ShmConstants.KEY))) {
                        final String value = jobProperty.get(ShmConstants.VALUE);
                        neCompleted = Integer.parseInt(value);
                        jobProperty.put(ShmConstants.VALUE, Integer.toString(++neCompleted));
                    }
                }
                final Map<String, Object> modifiedAttributes = new HashMap<>();
                modifiedAttributes.put(ShmConstants.JOBPROPERTIES, mainJobPropertyList);
                dpsWriter.update(mainjobId, modifiedAttributes);
            } else {
                LOGGER.error("Job property List does not exist for", mainjobId);
            }
        }

        // Propagate cancel to main job only if this is the last NE job running within the main job
        boolean sendCancelMessageToMainJob = true;
        final List<Object> neJobStates = getAssociatedNeJobsState(mainjobId);
        LOGGER.debug("NE Jobs status list for the main job with id: {} is : {}", mainjobId, neJobStates);
        long checkCompleted = 0;
        for (final Object nEJobState : neJobStates) {
            final JobState neJobState = JobState.getJobState((String) nEJobState);
            // If we cancel main job
            if (JobState.isJobCancelInProgress(mainJobState)) {
                if (!(JobState.isJobInactive(neJobState))) {
                    sendCancelMessageToMainJob = false;
                    LOGGER.info("All NE Jobs for main job id {} not completed, hence not cancelling main job", mainjobId);
                    break;
                }
            } else {
                // If we cancel NE jobs.
                if (!(JobState.isJobInactive(neJobState))) {
                    sendCancelMessageToMainJob = false;
                    LOGGER.info("All NE Jobs for main job id {} not completed, hence not updating main job state", mainjobId);
                    break;
                } else {
                    // Job status is InActive but not cancelled, Then sending AllNeDone message to workflows
                    if (!JobState.isJobCancelled(neJobState)) {
                        sendCancelMessageToMainJob = false;
                        LOGGER.info("All NE Jobs for main job id {} not completed, hence not cancelling main job", mainjobId);
                    }
                    checkCompleted = checkCompleted + 1;
                }
            }
        }
        if (templateJobId != null && sendCancelMessageToMainJob) {
            // Send cancel message to main job
            LOGGER.debug("All NE Jobs within main job id {} have completed.Sending cancelMsg to main job", mainjobId);
            try {
                workflowInstanceNotifier.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, templateJobId.toString(),wfsId,null);
            } catch (final WorkflowServiceInvocationException e) {
                LOGGER.error("Sending Asynchronous message to WFS failed due to ::{}", e);
            }
            LOGGER.debug("CancelMsg sent to main job");
        } else if (templateJobId != null && (checkCompleted == neJobStates.size())) {
            LOGGER.debug("All NE Jobs within main job id {} have completed.Sending sendAllNE done to main job", mainjobId);
            workflowInstanceNotifier.sendAllNeDone(Long.toString(templateJobId));

        }
    }

    @Override
    public void updateActivityJobAsFailed(final long jobId) {
        updateActivityAsFailed(jobId, null);
    }

    @Override
    public void updateActivityJobAsFailed(final long jobId, final String jobType) {
        updateActivityAsFailed(jobId, jobType);
    }

    @SuppressWarnings("unchecked")
    private void updateActivityAsFailed(final long jobId, final String jobType) {
        LOGGER.debug("Inside updateActivityJobAsFailed of JobStatusServiceImpl : {}", jobId);
        final Map<String, Object> activityJobPoAttributes = getPoAttributes(jobId);
        String jobResult = JobResult.FAILED.getJobResult();
        Long neJobId = null;
        Date startTime = null;
        Date endTime = null;
        String activityName = null;
        Double activityPercentage = null;
        neJobId = (long) activityJobPoAttributes.get("neJobId");
        startTime = (Date) activityJobPoAttributes.get(ShmConstants.STARTTIME);
        activityName = (String) activityJobPoAttributes.get("name");
        activityPercentage = (Double) activityJobPoAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
        if (activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
            final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
            LOGGER.debug("Activity job properties of jobid :{} are: {}", jobId, activityJobProperties);
            for (final Map<String, Object> jobProperty : activityJobProperties) {
                LOGGER.debug("Inside updateActivityJobAsFailed: JobProperty {}", jobProperty);
                if (ActivityConstants.ACTIVITY_RESULT.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    jobResult = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);

                }
            }
        } else {
            LOGGER.info("No Job properties available for the Activity Job with the PO Id:{}", jobId);
        }

        if (activityPercentage == null || activityPercentage == 0) {
            if (neJobId != null) {
                updateNEJobProgressPercentage(neJobId);
            }
        }

        final Map<String, Object> jobAttributes = new HashMap<>();

        if (jobResult.equals(JobResult.SUCCESS.getJobResult())) {
            jobAttributes.put(ShmConstants.RESULT, JobResult.SUCCESS.getJobResult());
            if (activityPercentage != null) {
                jobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE);
            }
        } else {
            jobAttributes.put(ShmConstants.RESULT, JobResult.FAILED.getJobResult());
        }
        jobAttributes.put(ShmConstants.ENDTIME, new Date());
        jobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        jobUpdateService.updateJobAttributes(jobId, jobAttributes);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        LOGGER.debug("Updated job of: {} with the attributes: {}", jobId, jobAttributes);
        endTime = (Date) jobAttributes.get(ShmConstants.ENDTIME);
        if (jobType != null) {
            keepLocalCounterMessageInTopic(jobId, activityName, jobType, ActivityConstants.ACTIVITY_EXECUTION_COMPLETED);
        }
        activityInstrumentationRecord(endTime, startTime, activityName, neJobId);
        faBuildingBlockResponseProcessor.sendFaResponse(jobId, jobType, jobResult, activityJobPoAttributes);
        neJobStaticDataProvider.clear(jobId);
    }

    private void activityInstrumentationRecord(final Date endTime, final Date startTime, final String name, final long neJobId) {
        try {
            String neName = null;

            final Map<String, Object> neJobPoAttributes = getPoAttributes(neJobId);
            if (neJobPoAttributes != null) {
                neName = (String) neJobPoAttributes.get(ShmConstants.NE_NAME);
            } else {
                LOGGER.info("No NE Job found with the PO Id: {}", neJobId);
            }
            final String timeSpend = TimeSpendOnJob.getDifference(endTime, startTime);
            final String userMessage = generateUserMessage(name, timeSpend, startTime, endTime);
            systemRecorder.recordEvent("ActivityComplete", EventLevel.COARSE, "  ", neName, userMessage);
        } catch (final Exception e) {
            LOGGER.error("Activity instrumentation error:{}", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void mainJobInstrumentRecord(final long mainJobId, final Date endTime) {
        try {
            Date startTime = null;
            int executionIndex = 0;
            long templateJobId = 0;
            String jobName = null;
            String jobType = null;
            String status = null;
            double progressPercentage = 0;
            int numberOfNetworkElements = 0;
            int totalNoOfComponents = 0;
            String result = null;
            final Set<String> neTypes = new HashSet<>();

            final Map<String, Object> mainJobPoAttributes = getPoAttributes(mainJobId);
            if (mainJobPoAttributes != null) {
                startTime = (Date) mainJobPoAttributes.get(ShmConstants.STARTTIME);
                executionIndex = (int) mainJobPoAttributes.get(ShmConstants.EXECUTIONINDEX);
                templateJobId = (long) mainJobPoAttributes.get(ShmConstants.JOBTEMPLATEID);
                progressPercentage = (double) mainJobPoAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
                status = (String) mainJobPoAttributes.get(ShmConstants.STATE);
                numberOfNetworkElements = (int) mainJobPoAttributes.get(ShmConstants.NO_OF_NETWORK_ELEMENTS);
                result = (String) mainJobPoAttributes.get(ShmConstants.RESULT);
                LOGGER.debug("For main Job start time:{} end time:{} execution index:{} and template jobId:{} ", startTime, endTime, executionIndex, templateJobId);
            }

            if (templateJobId != 0) {
                final Map<String, Object> jobTemplateAttributes = getPoAttributes(templateJobId);
                if (jobTemplateAttributes != null) {
                    jobName = (String) jobTemplateAttributes.get(ShmConstants.NAME);
                    jobType = (String) jobTemplateAttributes.get(ShmConstants.JOB_TYPE);
                    final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobTemplateAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
                    if (jobType.equalsIgnoreCase(JobTypeEnum.NODE_HEALTH_CHECK.toString())) {
                        final List<Map<String, Object>> neTypeJobPropertiesMap = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NETYPEJOBPROPERTIES);
                        for (final Map<String, Object> neTypeJobProperty : neTypeJobPropertiesMap) {
                            final String neType = (String) neTypeJobProperty.get(ShmConstants.NETYPE);
                            neTypes.add(neType);
                        }
                    }
                    totalNoOfComponents = getComponentCount(jobConfigurationDetails);
                }
            }

            long timeSpentOnJobInMillis = 0;
            if (startTime != null) {
                timeSpentOnJobInMillis = endTime.getTime() - startTime.getTime();
            }
            final String timeSpentOnNeJobCreation = getPropertyValue(mainJobPoAttributes, ShmConstants.DURATION_FOR_NEJOBS_CREATION);
            if (jobType != null) {
                final Map<String, Object> recordEventData = new HashMap<>();
                recordEventData.put("JobType", jobType);
                recordEventData.put("JobName", jobName);
                recordEventData.put("NumberOfNetworkElements", numberOfNetworkElements);
                recordEventData.put("DurationOfJob", timeSpentOnJobInMillis);
                recordEventData.put("ProgressPercentage", progressPercentage);
                recordEventData.put("Status", status);
                recordEventData.put("Result", result);
                recordEventData.put("DurationOfNeJobsCreation", Long.parseLong(timeSpentOnNeJobCreation));
                if (jobType.equalsIgnoreCase(JobTypeEnum.NODE_HEALTH_CHECK.toString())) {
                    final Map<HealthStatus, Integer> nodesCountByHealthStatus = jobConfigurationService.getNodesByHealthStatus(mainJobId);

                    final Integer healthynodesCount = nodesCountByHealthStatus.get(HealthStatus.HEALTHY);
                    final String reportCategory = jobConfigurationService.getReportCategory(mainJobId);

                    final String neTypesAsString = getNeTypesAsString(neTypes);

                    recordEventData.put("HealthyNodesCount", healthynodesCount);
                    recordEventData.put("ReportCategory", reportCategory);
                    recordEventData.put("NeTypes", neTypesAsString);

                    systemRecorder.recordEventData("NHC.MainJobComplete", recordEventData);
                    systemRecorder.recordEvent("Main Job Complete", EventLevel.COARSE, " NHC ", jobName, " has executed " + executionIndex + " time(s)");
                } else {
                    LOGGER.debug("jobType:{} ,totalNoOfComponents:{}", jobType, totalNoOfComponents);
                    recordEventData.put("NumberOfComponents", totalNoOfComponents);
                    systemRecorder.recordEventData("SHM.MainJobComplete", recordEventData);
                    systemRecorder.recordEvent("Main Job Complete", EventLevel.COARSE, "  ", jobName, " has executed " + executionIndex + " time(s)");
                }
            }
        } catch (final Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
    }

    private int getComponentCount(final Map<String, Object> jobConfigurationDetails) {
        int totalNoOfComponents = 0;
        final Map<String, Object> selectedNes = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.SELECTED_NES);
        final List<Map<String, Object>> nesWithComponents = (List<Map<String, Object>>) selectedNes.get(ShmConstants.NE_WITH_COMPONENT_INFO);
        if (nesWithComponents != null && !nesWithComponents.isEmpty()) {
            for (final Map<String, Object> neNamesWithComponents : nesWithComponents) {
                if (neNamesWithComponents.get(ShmConstants.NE_NAME) != null) {
                    final int components = ((List<String>) neNamesWithComponents.get(ShmConstants.SELECTED_COMPONENTS)).size();
                    totalNoOfComponents += components;
                }
            }
        }
        return totalNoOfComponents;
    }

    private String getNeTypesAsString(final Set<String> neTypes) {
        final String[] arr = neTypes.toArray(new String[neTypes.size()]);
        return StringUtils.join(arr, ", ");
    }

    private static String generateUserMessage(final String name, final String timeSpend, final Date startTime, final Date endTime) {
        return name + " has been completed in " + timeSpend + " with StartTime " + startTime + " and EndTime " + endTime;
    }

    @Override
    public void updateJobAsCancelled(final long neJobId, final Map<String, Object> neJobAttributes, final boolean isActivityJobUpdate) {
        final Map<String, Object> allNeJobAttributes = getPoAttributes(neJobId);
        if (isActivityJobUpdate) {
            LOGGER.debug("Updating ActivityJob status as Completed...");
        } else {
            LOGGER.debug("Updating NEJob status as Completed...");
            final String jobResultValue = jobConfigurationService.retrieveActivityJobResult(neJobId);
            retrieveJobStateAndResult(neJobAttributes, jobResultValue);
            updateNeJobAttributeForNHCJobs(allNeJobAttributes, neJobId, neJobAttributes);
        }
        updateJob(neJobId, neJobAttributes);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> cancelJobLogs = new HashMap<>();

        if (allNeJobAttributes != null) {

            if (isActivityJobUpdate) {
                final String activityName = (String) allNeJobAttributes.get(ShmConstants.ACTIVITY_NAME);
                cancelJobLogs.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobLogConstants.ACTIVITY_JOB_CANCELLED, activityName));

            } else {
                if (!JobResult.CANCELLED.toString().equalsIgnoreCase((String) neJobAttributes.get(ShmConstants.RESULT))) {
                    return;
                }
                final String neName = (String) allNeJobAttributes.get(ShmConstants.NE_NAME);
                cancelJobLogs.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobLogConstants.NEJOB_CANCELLED, neName));

            }
            cancelJobLogs.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
            cancelJobLogs.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
            cancelJobLogs.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());

            jobLogList.add(cancelJobLogs);
            jobUpdateService.updateRunningJobAttributes(neJobId, null, jobLogList);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateNeJobAttributeForNHCJobs(final Map<String, Object> jobAttributes, final long neJobId, final Map<String, Object> neJobAttributes) {
        final long mainJobId = (long) jobAttributes.get(ShmConstants.MAIN_JOB_ID);
        try {
            final JobType jobType = jobStaticDataProvider.getJobStaticData(mainJobId).getJobType();

            if (jobType != null && JobType.NODE_HEALTH_CHECK.name() == jobType.name()) {
                final ServiceFinderBean sfb = new ServiceFinderBean();
                final JobConsolidationService nhcConsolidationService = (JobConsolidationService) sfb.find(JobConsolidationService.class, JobType.NODE_HEALTH_CHECK.name());
                final Map<String, Object> nhcNeJobUpdate = nhcConsolidationService.consolidateNeJobData(neJobId);

                LOGGER.debug("updateNeJobAttributeForNHCJobs nhcNeJobUpdate {}", nhcNeJobUpdate);
                final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) nhcNeJobUpdate.get(ShmConstants.JOBPROPERTIES);
                neJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
                neJobAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, (String) nhcNeJobUpdate.get(ShmConstants.NEJOB_HEALTH_STATUS));
            }
        } catch (final Exception exception) {
            LOGGER.error("Exception occured while consolidating NHC NE job properties for NE job id in updateJobAsCancelled {}. Exception is {}", neJobId, exception);
        }
    }

    private void retrieveJobStateAndResult(final Map<String, Object> jobAttributes, final String jobResultValue) {
        final JobResult jobResult = JobResult.valueOf(jobResultValue);
        jobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        jobAttributes.put(ShmConstants.RESULT, jobResult.toString());
    }

    @Override
    public void createSkipJob(final long templateJobId, final String executionMode) {
        LOGGER.debug("Creating Skipped Job");
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        jobAttributes.put(ShmConstants.RESULT, JobResult.SKIPPED.getJobResult());
        jobAttributes.put(ShmConstants.STARTTIME, new Date());
        jobAttributes.put(ShmConstants.ENDTIME, new Date());
        jobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, templateJobId);
        LOGGER.debug("Job Attributes passed for creation of skipped job {}", jobAttributes);
        final PersistenceObject skippedJob = createJobPoWithRetry(jobAttributes);
        LOGGER.debug("Skipped job is created successfully with poId {}.", skippedJob.getPoId());
        final String logMessage = "The Job creation is Successfull";
        systemRecorder.recordEvent(SHMEvents.SKIP_JOB, EventLevel.COARSE, "", executionMode + "JOB", logMessage);

    }

    /**
     * Retrieves the Po Attributes for given ID, with retry mechanism
     *
     * @param poId
     * @return
     */
    private Map<String, Object> getPoAttributes(final long poId) {
        final Map<String, Object> poAttributes = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return jobUpdateService.retrieveJobWithRetry(poId);
            }
        });
        return poAttributes;
    }

    /**
     * Retrieves the Po Attributes for given ID, without retry mechanism
     *
     * Need to remove once analysis is done for all calls to this method align to without retry mechanism
     *
     * @param poId
     * @return
     */
    private Map<String, Object> retrievePoAttributes(final long poId) {
        return retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return jobConfigurationService.retrieveJob(poId);
            }
        });
    }

    private Map<String, Object> getActivitiesCount(final long poId) {
        final Map<String, Object> poAttributes = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return jobConfigurationService.getActivitiesCount(poId);
            }
        });
        return poAttributes;
    }

    private String retrieveNeJobResult(final long poId) {
        final String neJobResult = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return jobConfigurationService.retrieveNeJobResult(poId);
            }
        });
        return neJobResult;
    }

    /**
     * Retrieves the Po Attributes for given ID, with retry mechanism
     *
     * @param poId
     * @return
     */
    private List<Object> getAssociatedNeJobsState(final long mainjobId) {
        final Map<String, Object> restrictionAttributes = new HashMap<>();
        restrictionAttributes.put(ShmConstants.MAIN_JOB_ID, mainjobId);
        final List<Object> poAttributes = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Object>>() {
            @Override
            public List<Object> execute() {
                return dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE, restrictionAttributes, ShmConstants.STATE);
            }
        });
        return poAttributes;
    }

    private PersistenceObject createJobPoWithRetry(final Map<String, Object> jobAttributes) {
        final PersistenceObject po = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<PersistenceObject>() {
            @Override
            public PersistenceObject execute() {
                return dpsWriter.createPO(ShmConstants.NAMESPACE, ShmConstants.JOB, ShmConstants.VERSION, jobAttributes);
            }
        });
        return po;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Boolean isExecuteCompleted(final long activityJobId) {
        final Map<String, Object> activityJobPoAttributes = getPoAttributes(activityJobId);

        if (activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
            final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
            LOGGER.info("activity job properties in isExecuteCompleted() of jobid {} are {}", activityJobId, activityJobProperties);
            for (final Map<String, Object> jobProperty : activityJobProperties) {
                if (ActivityConstants.IS_ACTIVITY_TRIGGERED.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void abortActivity(final long activityJobId) {
        LOGGER.info("Inside the method abortActivity for the activityJobId:{}", activityJobId);
        final Map<String, Object> activityJobPoAttributes = getPoAttributes(activityJobId);
        final JobResult jobResult = JobResult.CANCELLED;
        final String activityName = (String) activityJobPoAttributes.get("name");
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_ABORTED, activityName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.RESULT, jobResult.getJobResult());
        activityJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        activityJobAttributes.put(ShmConstants.ENDTIME, new Date());
        jobUpdateService.updateJobAttributes(activityJobId, activityJobAttributes);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, 0.0);
    }

    @Override
    public boolean isNEJobProceedsForCancel(final long neJobId) {
        try {
            final boolean isJobCompleted = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() {
                    return jobConfigurationService.isNEJobProceedsForCancel(neJobId);
                }
            });
            return isJobCompleted;
        } catch (final RetriableCommandException ex) {
            LOGGER.error("Failed to get/update NE Job state for ne job Id: {} , Exception occurred : ", neJobId, ex);
            return true;
        }
    }

    private void updateScheduleInformationInActivityJobLogs(final long activityJobId, final String currentActivityName, final String activityScheduleTime) {

        final Date date = DateTimeUtils.getDateFromStringValue(activityScheduleTime);
        final String formattedScheduleTime = formatScheduledTime(activityScheduleTime);
        String logMessage = String.format(JobLogConstants.ACTIVITY_SCHEDULE_TIME_INFO, currentActivityName, formattedScheduleTime);
        if (date != null && date.before(new Date())) {
            logMessage = String.format(JobLogConstants.ACTIVITY_SCHEDULE_TIME_WAS, currentActivityName, formattedScheduleTime);
        }
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        jobLogList.add(activityUtils.createNewLogEntry(logMessage, new Date(), JobLogLevel.INFO.toString()));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
    }

    private static String formatScheduledTime(final String activityScheduleTime) {
        final StringTokenizer st = new StringTokenizer(activityScheduleTime, " ");
        final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
        return formattedScheduleTime;
    }

    @SuppressWarnings("unchecked")
    private String getActivityScheduleTime(final long neJobId, final String currentActivityName) {

        LOGGER.debug("Retrieving schedule time for the activity : {} for NE job Id : {} ", currentActivityName, neJobId);
        try {
            final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobId);
            final String neName = (String) neJobAttributes.get(ShmCommonConstants.NE_NAME);
            final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(neName);
            final String neTypeFromNode = networkElementData.getNeType();
            LOGGER.debug("Found node type as : {} for Node : {} with NE Job Id : {}", neTypeFromNode, neName, neJobId);

            final Map<String, Object> mainJobAttributes = activityUtils.getMainJobAttributesByNeJobId(neJobId);
            final Map<String, Object> jobConfiguration = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<Map<String, Object>> activities = (List<Map<String, Object>>) jobConfiguration.get(ShmConstants.JOB_ACTIVITIES);
            if (activities != null && !activities.isEmpty()) {
                final String scheduledTime = populateScheduledTime(currentActivityName, neTypeFromNode, activities);
                return scheduledTime;
            }
            LOGGER.debug("Schedule time not found for the activity : {} with NE Job Id: {}", currentActivityName, neJobId);
        } catch (final MoNotFoundException e) {
            LOGGER.error("Exception while retriving NetworkElement ", e);
        }
        return null;
    }

    private String populateScheduledTime(final String currentActivityName, final String neTypeFromNE, final List<Map<String, Object>> activities) {
        for (final Map<String, Object> activity : activities) {
            final String activityName = (String) activity.get(ShmConstants.ACTIVITY_NAME);
            final String neType = (String) activity.get(ShmCommonConstants.NETYPE);
            if (activityName.equals(currentActivityName) && neType.equals(neTypeFromNE)) {
                final Map<String, Object> schedules = ((Map<String, Object>) activity.get(ShmConstants.ACTIVITY_SCHEDULE));
                if (schedules != null && !schedules.isEmpty()) {
                    final List<Map<String, Object>> actvityScheduleAttributes = (List<Map<String, Object>>) schedules.get(SchedulePropertyConstants.SCHEDULE_ATTRIBUTES);
                    LOGGER.debug("The activity scheduled attributes are: {}", actvityScheduleAttributes);
                    if (actvityScheduleAttributes != null) {
                        for (final Map<String, Object> activityScheduleInfo : actvityScheduleAttributes) {
                            if (activityScheduleInfo.get(SchedulePropertyConstants.NAME).equals(SchedulePropertyConstants.START_DATE)) {
                                final String scheduleTime = (String) activityScheduleInfo.get(SchedulePropertyConstants.VALUE);
                                LOGGER.debug("'{}' actviity scheduled to run at : {}", currentActivityName, scheduleTime);
                                return scheduleTime;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.debug("Schedule time not found for the activity : {}", currentActivityName);
        return null;
    }

    @Override
    public void updateMainJobAsCancelled(final long mainJobId, final Map<String, Object> mainJobAttributes) {
        final String jobResultValue = jobConfigurationService.retrieveNeJobResult(mainJobId);
        retrieveJobStateAndResult(mainJobAttributes, jobResultValue);
        mainJobProgressNotifier.updateMainJobEndDetails(mainJobId, mainJobAttributes);
        neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainJobId, mainJobAttributes);
        activityStepDurationsReportGenerator.generateJobReportAndUpdateMainJob(mainJobId);
        jobStaticDataProvider.clear(mainJobId);
    }

    /**
     * This API will get the delay time between execute call initiated from WFS and actual action triggered on the node.
     *
     * @see com.ericsson.oss.services.shm.es.api.JobStatusService#getDelayInActionTriggeredTime(long, String,String,String,String)
     */
    @Override
    public long getDelayInActionTriggeredTime(final long activityJobId, final int defaultActivityTimeout) {
        long activityTimeout = 0;
        try {
            final Map<String, Object> activityJobPoAttributes = retrievePoAttributes(activityJobId);

            if (activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
                final String dateString = getPropertyValue(activityJobPoAttributes, ActivityConstants.ACTION_TRIGGERED_TIME);
                if (dateString != null) {
                    activityTimeout = calculateDelayInActionTrigger(defaultActivityTimeout, dateString);
                } else {
                    activityTimeout = defaultActivityTimeout;
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while getting delay time between execute call from WFS to actual action triggered on the node. Exception is :", ex);
        }
        return activityTimeout;
    }

    private static long calculateDelayInActionTrigger(final Integer defaultActivityTimeout, final String actionTriggeredTimeString) {
        final long actionTriggeredTimeInMillis = Long.parseLong((actionTriggeredTimeString));
        final long actionTriggeredDelayInMillis = System.currentTimeMillis() - actionTriggeredTimeInMillis;
        final long remainingTimeout = actionTriggeredDelayInMillis / 60000;
        if (remainingTimeout > 1 && remainingTimeout < defaultActivityTimeout) {
            LOGGER.info("Action triggerd time : {}  and remaningTimeout value {}", actionTriggeredTimeInMillis, remainingTimeout);
            return (defaultActivityTimeout - remainingTimeout);
        } else {
            return 0;
        }
    }

    private String getPropertyValue(final Map<String, Object> activityJobPoAttributes, final String propertyName) {
        final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
        String propertyValue = null;
        for (final Map<String, Object> jobProperty : activityJobProperties) {
            if (propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                propertyValue = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
            }
        }
        return propertyValue;
    }

    @Override
    public boolean isMoActionTriggered(final long activityJobId) {
        try {
            final Map<String, Object> activityJobPoAttributes = getPoAttributes(activityJobId);
            if (activityJobPoAttributes.get(ActivityConstants.ACTIVITY_RESULT) != null) {
                return true;
            }
            if (activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
                final String dateString = getPropertyValue(activityJobPoAttributes, ActivityConstants.ACTION_TRIGGERED_TIME);
                LOGGER.debug("Action Triggered time: {} for activityJobId: {}", dateString, activityJobId);

                if (dateString != null || checkMoActionInCache(activityJobId)) {
                    return true;
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while getting MoAction triggered status for activityJobId: {}. Exception is :", activityJobId, ex);
        }
        return false;
    }

    @Override
    public String getBestTimeout(final String neType, final String platformType, final String jobType, final String activityName) {
        return activityTimeoutsService.getBestTimeout(neType, PlatformTypeEnum.getPlatform(platformType), JobTypeEnum.getJobType(jobType), activityName);
    }

    private boolean checkMoActionInCache(final long activityJobId) {
        final ShmEBMCMoActionData shmEBMCMoActionData = moActionMTRManager.getMoActionMTRFromCache(activityJobId);
        return shmEBMCMoActionData != null;
    }

    @Override
    public void updateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertis, final List<Map<String, Object>> jobLogs, final Double progressPercentage) {
        LOGGER.debug("Updating job attributes by remote client. [jobId={},jobPropertis={},jobLogs={},progressPercentage={}]", jobId, jobPropertis, jobLogs, progressPercentage);
        jobUpdateService.readAndUpdateRunningJobAttributes(jobId, jobPropertis, jobLogs, progressPercentage);
    }

    @Override
    public void keepActivityStartMessageInTopic(final long activityJobId, final JobState state, final String jobType, final String activityName) {
        if (JobState.RUNNING.equals(state)) {
            try {
                LOGGER.trace("Going to update currentActivityCount in Tpoic in JobStatusServiceImpl having ActivityJob poid {} and state {} and jobType: {} and activityName {}", activityJobId, state,
                        jobType, activityName);
                keepLocalCounterMessageInTopic(activityJobId, activityName, jobType, ActivityConstants.ACTIVITY_EXECUTION_STARTED);
            } catch (final Exception ex) {
                LOGGER.error("Failed to get NE Job static data for the activityJob Id:{}. with Exception : ", activityJobId, ex);
            }
        }
    }

}
