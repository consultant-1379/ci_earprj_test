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
package com.ericsson.oss.services.shm.job.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;

public class SHMJobServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobServiceHelper.class);

    @Inject
    private JobsDeletionRetryProxy jobsDeletionRetryProxy;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private WorkflowInstanceNotifier localWorkflowQueryServiceProxy;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private CheckPeriodicity checkPeriodicity;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private Event<JobWebPushEvent> eventSender;

    @SuppressWarnings("unchecked")
    public Map<Long, List<Job>> fetchJobDetails(final Set<Long> poIdSet, final JobsDeletionReport jobsDeletionReport) {
        final Map<Long, List<Job>> jobDetails = new HashMap<Long, List<Job>>();
        final Set<Long> jobsNotFound = new HashSet<Long>(poIdSet);
        final List<Object[]> mainJobItems = jobsDeletionRetryProxy.retrieveJobDetails(poIdSet);
        final List<Long> jobTemplateIds = new ArrayList<Long>();
        for (final Object[] eachMainJobItem : mainJobItems) {
            //Retrieving mainJobId, jobTemplateId, jobState and executionIndex based on the same order as supplied in Projection Query.
            final long eachMainJobId = (long) eachMainJobItem[0];
            final long eachJobTemplateId = (long) eachMainJobItem[1];
            final String eachJobState = (String) eachMainJobItem[2];
            final int eachExecutionIndex = (int) eachMainJobItem[3];
            //Remove each job from Set of jobsNotFound.
            jobsNotFound.remove(eachMainJobId);
            final Job job = setJobDetails(eachMainJobId, eachJobTemplateId, eachJobState, eachExecutionIndex);
            //If other jobs exists with same job template, then update in that list or else create new one.
            List<Job> jobs = jobDetails.get(eachJobTemplateId);
            if (jobs == null || jobs.isEmpty()) {
                jobs = new ArrayList<Job>();
                jobDetails.put(eachJobTemplateId, jobs);
            }
            jobs.add(job);
        }
        if (someJobsNotFound(jobsNotFound)) {
            LOGGER.warn("Jobs not found {}", jobsNotFound);
            jobsDeletionReport.incrementJobsNotFoundCount(jobsNotFound.size());
        }
        //Preparing a list of all job Template Ids for quering job Templates.
        jobTemplateIds.addAll(jobDetails.keySet());
        final List<Map<String, Object>> jobTemplates = jobsDeletionRetryProxy.fetchJobTemplateAttributes(jobTemplateIds);
        for (final Map<String, Object> attributesOfEachJobTemplate : jobTemplates) {
            final String eachJobName = (String) attributesOfEachJobTemplate.get(ShmConstants.NAME);
            final String eachJobType = (String) attributesOfEachJobTemplate.get(ShmConstants.JOB_TYPE);
            final String eachWfsId = (String) attributesOfEachJobTemplate.get(ShmConstants.WFS_ID);
            final Map<String, Object> eachJobConfigurationDetails = (Map<String, Object>) attributesOfEachJobTemplate.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<Job> jobs = jobDetails.get(attributesOfEachJobTemplate.get(ShmConstants.JOBTEMPLATEID));
            for (final Job eachJob : jobs) {
                setJobTemplateDetails(eachJob, eachJobName, eachJobType, eachWfsId, eachJobConfigurationDetails);
            }
        }
        return jobDetails;
    }

    public List<Job> extractListOfJobs(final Map<Long, List<Job>> jobDetailsForDeletion) {
        final List<Job> jobs = new ArrayList<Job>();
        final Collection<List<Job>> jobsHavingSameTemplate = jobDetailsForDeletion.values();

        for (final List<Job> jobsForEveryTemplate : jobsHavingSameTemplate) {
            jobs.addAll(jobsForEveryTemplate);
        }
        return jobs;
    }

    private Job setJobDetails(final long mainJobId, final long jobTemplateId, final String jobState, final int executionIndex) {
        final Job job = new Job();
        job.setMainJobId(mainJobId);
        job.setJobTemplateId(jobTemplateId);
        job.setJobState(jobState);
        job.setExecutionIndex(executionIndex);
        return job;
    }

    private void setJobTemplateDetails(final Job job, final String jobName, final String jobType, final String wfsId, final Map<String, Object> jobConfigurationDetails) {
        job.setJobName(jobName);
        job.setJobType(jobType);
        job.setWfsId(wfsId);
        job.setJobConfigurationDetails(jobConfigurationDetails);
    }

    private boolean someJobsNotFound(final Set<Long> jobsNotFound) {
        if (jobsNotFound.size() == 0) {
            return false;
        }
        return true;
    }

    private void fireWebPushEvent(final Map<String, Object> jobAttributes, final String applicationType) {
        LOGGER.debug("Pushing to {} Page with job attributes {}", applicationType, jobAttributes);
        final JobWebPushEvent jobWebPushEvent = new JobWebPushEvent();
        jobWebPushEvent.setAttributeMap(jobAttributes);
        jobWebPushEvent.setApplicationType(applicationType);
        eventSender.fire(jobWebPushEvent);
    }

    public int deleteJobHirerachy(final Job jobsDeletionAttributes) {
        final long mainJobId = jobsDeletionAttributes.getMainJobId();
        LOGGER.debug("Entry of deleteJobHirerachy with jobId:{}", mainJobId);
        boolean jobTemplateIsAllowedToDelete = false;
        int jobDeleteCount = 0;
        final String jobCategory = jobConfigurationService.getJobCategory(mainJobId);
        jobTemplateIsAllowedToDelete = canDeleteJobTemplate(jobsDeletionAttributes);
        LOGGER.debug("Is jobTemplate deleteable {}", jobTemplateIsAllowedToDelete);
        if (jobTemplateIsAllowedToDelete) {
            jobDeleteCount = jobsDeletionRetryProxy.deleteJobHierarchyWithJobTemplate(jobsDeletionAttributes);
        } else {
            jobDeleteCount = jobsDeletionRetryProxy.deleteJobHierarchyWithoutJobTemplate(jobsDeletionAttributes);
        }

        final Map<String, Object> deleteJobAttributes = new HashMap<>();
        deleteJobAttributes.put(WebPushConstants.JOB_ID, mainJobId);
        deleteJobAttributes.put(WebPushConstants.JOB_EVENT, WebPushConstants.DELETE_JOB);
        if (jobCategory != null && !jobCategory.isEmpty()) {
            if (JobCategory.NHC_UI.toString().equals(jobCategory)) {
                fireWebPushEvent(deleteJobAttributes, WebPushConstants.NHC_JOBS_APPLICATION);
            } else {
                fireWebPushEvent(deleteJobAttributes, WebPushConstants.SHM_JOBS_APPLICATION);
            }
        }

        return jobDeleteCount;
    }

    @SuppressWarnings("unchecked")
    private boolean canDeleteJobTemplate(final Job jobsDeletionAttributes) {
        String execMode = null;
        boolean deleteTemplateFlag = false;
        final Map<String, Object> jobConfiguration = jobsDeletionAttributes.getJobConfigurationDetails();
        if (jobConfiguration != null) {
            final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfiguration.get(ShmConstants.MAIN_SCHEDULE);
            if (mainSchedule != null) {
                execMode = (String) mainSchedule.get(ShmConstants.EXECUTION_MODE);
                if (ExecMode.IMMEDIATE.toString().equalsIgnoreCase(execMode) || ExecMode.MANUAL.toString().equalsIgnoreCase(execMode)) {
                    deleteTemplateFlag = true;
                } else {
                    final boolean isPeriodic = checkPeriodicity.isJobPeriodic((List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES));
                    if (isPeriodic) {//isPeriodicJob((List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES));
                        deleteTemplateFlag = isJobTemplateNotInUse(jobsDeletionAttributes.getJobTemplateId(), jobsDeletionAttributes.getWfsId());
                    } else {
                        deleteTemplateFlag = true;
                    }
                }
            }
        }
        return deleteTemplateFlag;
    }

    private boolean isJobTemplateNotInUse(final Long jobTemplateId, final String wfsId) {
        boolean jobTemplateNotInUse = false;

        if (associatedBatchWorkflowDoesNotExists(wfsId) && jobTemplateHasOnlyOneMainJob(jobTemplateId)) {
            jobTemplateNotInUse = true;
        }
        return jobTemplateNotInUse;

    }

    private boolean associatedBatchWorkflowDoesNotExists(final String wfsId) {
        boolean associatedBatchWorkflowDoesNotExists = true;
        final List<WorkflowObject> associatedBatchWorkflows = retrieveBatchWorkflow(wfsId);
        if (associatedBatchWorkflows != null && associatedBatchWorkflows.size() != 0) {
            associatedBatchWorkflowDoesNotExists = false;
        }
        return associatedBatchWorkflowDoesNotExists;
    }

    private List<WorkflowObject> retrieveBatchWorkflow(final String wfsId) {
        final com.ericsson.oss.services.wfs.api.query.QueryBuilder batchWorkflowQueryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
        final com.ericsson.oss.services.wfs.api.query.Query wfsQuery = batchWorkflowQueryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
        final com.ericsson.oss.services.wfs.api.query.RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
        final com.ericsson.oss.services.wfs.api.query.Restriction restrictionOnWfsId = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId);
        wfsQuery.setRestriction(restrictionOnWfsId);
        LOGGER.debug("Executing WFS query {} ", wfsQuery);
        final List<WorkflowObject> batchWorkFlowList = localWorkflowQueryServiceProxy.executeWorkflowQuery(wfsQuery);

        return batchWorkFlowList;
    }

    private boolean jobTemplateHasOnlyOneMainJob(final long jobTemplateId) {
        boolean isJobTemplateHavingOnlyOneMainJob = true;
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(ShmConstants.JOB_TEMPLATE_ID, jobTemplateId);
        final Long countOfMainJobsHavingSameTemplateJobId = dpsReader.getCountForItemsQueried(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictionAttributes);
        if (countOfMainJobsHavingSameTemplateJobId > 1) {

            isJobTemplateHavingOnlyOneMainJob = false;
        }
        return isJobTemplateHavingOnlyOneMainJob;
    }

    public boolean isJobActive(final String jobState) {
        return !JobState.isJobInactive(JobState.getJobState(jobState));
    }

    public JobsDeletionReport updateJobStatusAndGetJobDeletionReport(final List<Long> mainJobIds) {
        return jobsDeletionRetryProxy.updateJobStatusAndGetJobDeletionReport(mainJobIds);
    }

    public void deleteJobs(final List<Long> poIdList) {

        final Set<Long> poIdSet = new HashSet<Long>(poIdList);
        Map<Long, List<Job>> jobDetailsForDeletion = new HashMap<Long, List<Job>>();
        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(poIdSet.size());

        //Fetching attributes of MainJob and JobTemplate which are required during deletion of jobs.
        try {
            jobDetailsForDeletion = fetchJobDetails(poIdSet, jobsDeletionReport);
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving job details for jobs to be deleted. Reason : {}", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }

        final List<Job> jobsPresentInDatabase = extractListOfJobs(jobDetailsForDeletion);
        LOGGER.info("Jobs to be deleted {} and jobs present in DB {}", poIdSet.size(), jobsPresentInDatabase.size());

        //This method will iterate over a loop and delete jobs in sequential manner. 
        deleteJobsInSequence(jobsPresentInDatabase, jobsDeletionReport);

        if (jobsDeletionReport.getJobsDeletionFailed() > 0) {
            LOGGER.info("Job Deletion failed for Po Ids are " + jobsDeletionReport.getFailedJobIds());
        }

        final Map<String, String> jobDeletionReport = jobsDeletionReport.generateResponseForUser();
        systemRecorder.recordEvent(SHMEvents.DELETE_JOBS, EventLevel.COARSE, "", "", (String) jobDeletionReport.get(ShmConstants.MESSAGE));
    }

    private void deleteJobsInSequence(final List<Job> listOfJobDetailsForDeletion, final JobsDeletionReport jobsDeletionReport) {
        final StringBuilder failedJobIds = new StringBuilder(jobsDeletionReport.getFailedJobIds());
        int countOfJobDeleted = 0, executionIndex;
        boolean databaseIsDown = false;
        String jobName, jobType, jobState;
        long mainJobId;
        for (final Job job : listOfJobDetailsForDeletion) {

            jobName = job.getJobName();
            jobType = job.getJobType();
            jobState = job.getJobState();
            mainJobId = job.getMainJobId();
            executionIndex = job.getExecutionIndex();

            final boolean activeJob = isJobActive(job.getJobState());
            if (activeJob) {
                LOGGER.error("{} Job {} with id {} and executionIndex {}, is not allowed for deletion because it is {}", jobType, jobName, mainJobId, executionIndex, jobState);
                jobsDeletionReport.incrementActiveJobsCount();
                continue;
            }
            LOGGER.debug("{} Job {} with id {} is allowed for deletion", jobType, jobName, mainJobId);
            try {
                countOfJobDeleted = deleteJobHirerachy(job);
            } catch (final Exception exception) {
                LOGGER.error("Exception while deleting job for the PoId : {}. Reason: ", job.getMainJobId(), exception);
                databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            }
            if (countOfJobDeleted > 0) {
                jobsDeletionReport.incrementJobsDeletedCount();
                systemRecorder.recordEvent(SHMEvents.JOB_DELETED_SUCCESSFULLY, EventLevel.COARSE, job.getJobName(), job.getJobType(), "MainJobId : " + job.getMainJobId());
            } else {
                jobsDeletionReport.incrementJobsNotFoundCount(1);
            }
            if (databaseIsDown) {
                jobsDeletionReport.setDatabaseDown(databaseIsDown);
                failedJobIds.append(job.getMainJobId()).append(" ");
                jobsDeletionReport.incrementfailedJobsDeletionCount();
                break;
            }

        }
        jobsDeletionReport.setFailedJobIds(failedJobIds.toString());
    }
}
