package com.ericsson.oss.services.shm.job.service;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse;
import com.ericsson.oss.services.shm.job.entities.CancelResponse;
import com.ericsson.oss.services.shm.job.entities.DeleteResponse;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.entities.ShmMainJobsResponse;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.jobs.common.api.JobQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.api.NetworkElementJobDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobComment;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobservice.common.CommentInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesPlatformData;

public interface SHMJobService {

    /**
     * function to retrieve job details based on job input & job category
     * 
     * @deprecated
     * @param jobInput
     * @param jobCategory
     * @return
     */
    @Deprecated
    JobOutput getJobDetails(JobInput jobInput);

    /**
     * 
     * Function to retrieve job details,this is same as getJobDetails method with the re factored code
     * 
     * @param jobInput
     * @return
     */

    ShmMainJobsResponse getShmMainJobs(JobInput jobInput);

    /**
     * function to create Shm job based on jobInfo. RBAC will be verified within the method.
     * 
     * @param jobInfo
     * @return
     */

    Map<String, Object> createShmJob(final JobInfo jobInfo) throws NoJobConfigurationException, NoMeFDNSProvidedException, IllegalArgumentException;

    /**
     * function to create job based on jobInfo. Caller has to implement RBAC for this.
     * 
     * @param jobInfo
     * @return
     */
    Map<String, Object> createNhcJob(final JobInfo jobInfo) throws NoMeFDNSProvidedException;

    /**
     * To retrieve the JobConfiguaration Details based on the filled JobQuery
     * 
     * @param jobConfigQuery
     * @return JobConfiguration
     */
    List<JobTemplate> getJobTemplates(final JobQuery jobConfigQuery);

    /**
     * To retrieve the JobConfiguaration Details based on the filled JobQuery. Caller has to verify the RBAC.
     * 
     * @param jobConfigQuery
     * @return JobConfiguration
     */
    List<JobTemplate> getShmJobTemplates(final JobQuery jobConfigQuery);

    /**
     * Method that takes a list of running job poids and returns a list of jobs
     * 
     * @param poIds
     * @return List of Job-s
     */
    List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> getJobProgressDetails(final List<Long> poIds);

    /**
     * Method calls the cm service class to retrieve job log details from DPS
     * 
     * @param jobLogRequest
     * @return JobOutput
     */
    JobOutput retrieveJobLogs(final JobLogRequest jobLogRequest);

    // Added by Namrata for TORF-16715 for reteriving the ME level job details.
    JobReportData getJobReportDetails(final NeJobInput jobInput);

    /**
     * Cancels executing main jobs and NE jobs. When a main job is cancelled, all sub-jobs(NE Jobs, activity jobs) are cancelled. When a NE Job is cancelled, only the NE Job(and associated Activity
     * Jobs) is cancelled, the main job will continue running the other NE Jobs.
     * 
     * @param jobIds
     *            - List of Job PO ids - ["poid1", "poid2", "poid3"]. In a single invocation, it could be either a list of main job ids or NE Jobs ids, but not a mixture of both.
     * @return - Response object having Job cancellation details
     */
    Map<String, Object> cancelJobs(final List<Long> jobIds);

    /**
     * This method retrieves the PO Id for Job, NEJobs, Activity Jobs and Job Template. Also it calls the delete method for deleting Jobs and related sub-jobs.
     * 
     * @param List
     *            <String> deleteJobRequest
     * @return Map<String, Object>
     */
    Map<String, String> deleteShmJobs(final List<String> deleteJobRequest);

    /**
     * This method retrieves the PO Id for Job and adds a comment by calling the addComment.
     * 
     * @param commentInfo
     */
    Map<String, Object> addJobComment(CommentInfo commentInfo);

    /**
     * This method retrieves the JobName, JobType, NodeName, ActivityName, EntryTime and Message.
     * 
     * @param jobLogRequestForExport
     * @return csvOutput
     */
    String exportJobLogs(final ExportJobLogRequest jobLogRequestForExport);

    /**
     * HEAD This method retrieves the JobName, JobType, NodeName, ActivityName, EntryTime and Message. Caller has to verify the RBAC. ======= This method retrieves the JobName, JobType, NodeName,
     * ActivityName, EntryTime and Message. Caller has to take care of RBAC >>>>>>> 26344de... TORF-275801 : Support to export NHC report logs from REST
     * 
     * @param jobLogRequestForExport
     * @return csvOutput
     */
    String exportLogs(final ExportJobLogRequest jobLogRequestForExport);

    /**
     * @param neJobId
     * @return
     */
    JobTemplate getJobTemplateByNeJobId(Long neJobId);

    /**
     * @param name
     * @return
     */
    JobTemplate getJobTemplate(String name);

    /**
     * @param templateJobId
     * @return
     */
    String getJobStartTime(final Long templateJobId);

    /**
     * Method to retrieve job comments for the provided main job Id.
     * 
     * @param mainJobId
     * @return Response - List of JobComment as a JSON object
     */
    List<JobComment> retrieveJobComments(final Long mainJobId);

    /**
     * This method retrieves Activity details.
     * 
     * @param neJobId
     * @return ActivityDetails
     */
    NetworkElementJobDetails getNodeActivityDetails(final Long neJobId);

    /**
     * Retrieves all the Main jobs after filtered with jobStateEnum.
     * 
     * @param jobState
     * @return - List of main job ids.
     */
    List<Long> getMainJobIds(final String... jobState);

    /**
     * Retrieves all the NE jobs Id for main job
     * 
     * @param mainJobId
     * @return - List of NeJobId
     */
    List<Long> getNeJobIdsForMainJob(final Long mainJobId);

    /**
     * Retrieves all the NE jobs Id for main job
     * 
     * @param templateJobId
     * @return - SkippedNeJobscount
     */
    int getSkippedNeJobCount(final Long templateJobId);

    /**
     * Method to update current number of nodes involved for provided main job Id with a new count.
     * 
     * @param mainJobId
     *            - Id of the main job for which node count update is required.
     * @param nodeCount
     *            - Count with which the job should be updated.
     * @return boolean
     */
    boolean updateNodeCountOfJob(final Long mainJobId, final int nodeCount);

    /**
     * This method retrieves the JobName, JobType, EntryTime and Message of main job.
     * 
     * @param jobLogRequestForExport
     * @return csvOutput
     */
    String exportMainJobLogs(final ExportJobLogRequest jobLogRequestForExport);

    Map<Long, String> getNeJobDetails(final List<Long> neJobIds);

    /**
     * function to delete selected reports. Caller has to implement RBAC for this.
     * 
     * @param jobIds
     * @return
     */
    DeleteResponse deleteJobsWithNoRbac(final List<String> jobIds);

    JobOutput viewReportLogs(final JobLogRequest jobLogRequest, final List<Long> neJobIdList);

    /**
     * function to cancel selected reports. Caller has to implement RBAC for this.
     * 
     * @param jobIds
     * @return
     */
    CancelResponse cancelJobsWithNoRbac(final List<Long> jobIds);

    /**
     * This method will retrieves the platform type from capabilities
     * 
     * @param neTypeInfo
     * @return platforms To SupportedNeTypes.
     */
    NeTypesPlatformData getNeTypesPlatforms(final NeTypesInfo neTypeInfo);

    /**
     * This method will retrieves the activityTimout based on activities selected
     * 
     * @param activityJobId
     * @param jobType
     * @param activityName
     * @param nodeName
     * @return
     */
    int getActivityTimeout(final long activityJobId, final String jobType, final String activityName, final String nodeName);

    /**
     * This method will retrieves the supported and unsupported network elements for job creation
     * 
     * @param neNames
     * @param jobType
     * @return
     */
    Map<String, Object> getSupportedNes(final List<String> neNames, final String jobType);

    /**
     * Method to Retrieve network element poid's based on node names
     *
     * @param neNames
     *            Example : [ "LTE06dg2ERBS00002", "LTE06dg2ERBS00003", "LTE06dg2ERBS00004" ]
     * @return poIds: [2044042,2044030]
     */
    NetworkElementIdResponse getNetworkElementPoIds(final List<String> neNames);
}
