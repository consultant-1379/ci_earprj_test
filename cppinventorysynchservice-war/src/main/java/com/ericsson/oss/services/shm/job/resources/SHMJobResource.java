package com.ericsson.oss.services.shm.job.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobservice.common.CommentInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesInfo;

/**
 * This interface is used to listen to the rest url and generate the response accordingly.
 * 
 * @author xjhosye
 */
@Path("/job")
public interface SHMJobResource {

    /**
     * Retrieval of jobData
     * 
     * @deprecated Alternatively /v2/jobs rest call(getSHMJobsData method) is used for getting the SHM jobs data
     * @URL url : http://localhost:8080/oss/shm/rest/job/jobs
     * @param Input
     *            : {"offset" : "1", "limit" :"10", "sortBy" : "jobId", //can give any column name "orderBy" : "asc/desc", "columns" : null //not in use right now}
     */

    @POST
    @Path("/jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    Response getSHMJobs(final JobInput jobInput);

    /**
     * Retrieval of jobData
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/v2/jobs
     * @param Input
     *            : {"offset" : "1", "limit" :"10", "sortBy" : "jobId", //can give any column name "orderBy" : "asc/desc", "columns" : null //not in use right now}
     */

    @POST
    @Path("/v2/jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getSHMJobsData(final JobInput jobInput);

    /**
     * Persisting job and job configuration
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job
     * @param Input
     *            : {"description":"CreateUpgradeJob-2014-04-28_11-40", "jobType":"UPGRADE", "name":"UpgradeName-2014-04-28_11-40", "owner":"shmtest", "packageNames":{"CPP":"abc","ECIM":"def"},
     *            "collections":[{"name":"coll1"} ,{"name":"coll2"}],"neNames":[{"name":"ne1"},{"name":"ne2"}], "mainSchedule":{"scheduleAttributes":[{"name" :"START_DATE","value": "Sun Jun 01
     *            12:53:10 IST 2014" }, {"name":"OCCURENCES","value":"1"}],"execMode":"IMMEDIATE"}, "activitySchedules":[{"PlatformType":"CPP",
     *            "value":[{"activityName":"Installation","execMode":"MANUAL", "scheduleAttributes" :[{"name":"START_DATE","value":"Sun Jun 02 12:53:10 IST 2014"
     *            },{"name":"PERIOD_YEAR","value":"2"}]}, {"activityName":"Verify","execMode" :"SCHEDULED","scheduleAttributes":[{"name" :"START_DATE","value":"Sun Jun 03 12:53:10 IST 2014"},
     *            {"name":"PERIOD_MONTH","value":"3"}]}]}, {"PlatformType":"ECIM","value":[{ "activityName":"Installation","execMode":"IMMEDIATE", "scheduleAttributes" :[{"name":"START_DATE","value":
     *            "Sun Jun 04 12:53:10 IST 2014" },{"name":"PERIOD_YEAR","value":"2"}]}, {"activityName":"Verify","execMode" :"MANUAL","scheduleAttributes":[{"name" :"START_DATE","value": "Sun Jun 05
     *            12:53:10 IST 2014" },{"name":"PERIOD_MONTH","value":"3"}]}]} ], "ucf":"ucfname", "selectiveInstall":true, "forceInstall":true }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createJob(final JobInfo jobInfo);

    /**
     * This endpoint is now replaced by jobconfigurationdetails/{jobConfigId} also add Method to get the details of Job Configuration for a given jobConfigId
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/jobconfiguration/jobConfigId
     * @param jobConfigId
     * @return Response - JobConfiguration as a Json object
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("jobconfiguration/{jobTemplateId}")
    @Deprecated
    Response getJobConfiguration(@PathParam("jobTemplateId") final Long jobConfigId);

    /**
     * Method to get the details of Job Configuration for a given jobConfigId
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/jobconfigurationdetails/jobConfigId
     * @param jobConfigId
     * @return Response - JobConfiguration as a Json object
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("jobconfigurationdetails/{jobTemplateId}")
    Response getJobConfigurationDetails(@PathParam("jobTemplateId") final Long jobConfigId);

    /**
     * Cancels executing main jobs and NE jobs. When a main job is cancelled, all sub-jobs(NE Jobs, activity jobs) are cancelled. When a NE Job is cancelled, only the NE Job(and associated Activity
     * Jobs) is cancelled, the main job will continue running the other NE Jobs.
     * 
     * @param jobIds
     *            - List of Job PO ids - ["poid1", "poid2", "poid3"]. In a single invocation, it could be either a list of main job ids or NE Jobs ids, but not a mixture of both.
     * @return - Response object having Job cancellation details
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/cancelJobs")
    Response cancelJobs(final List<Long> jobIds);

    /**
     * Method that takes a list of running job poids and gives the progress percentage, status, result and endtime as response
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/progress
     * @param Input
     *            ["poid1", "poid2", "poid3"]
     * @return Response - List of Job-s as a json object
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("progress")
    Response getJobProgressDetails(final List<Long> poIds);

    // Added by Namrata on 20th June for TORF-16715
    /**
     * Method to get the details of Job for a given jobConfigId
     * 
     * @param jobConfigId
     * @return Response - JobDetails as a JSON object
     */
    @GET
    @Path("/jobdetails")
    @Produces(MediaType.APPLICATION_JSON)
    Response getSHMJobReportDetails(@QueryParam("jobId") final Long jobId, @QueryParam("offset") final int offset, @QueryParam("limit") final int limit, @QueryParam("sortBy") final String sortBy,
            @QueryParam("orderBy") final String orderBy, @QueryParam("selectAll") final boolean selectAll);

    /**
     * Method used to get job details while applying filtering.
     * 
     * @param NeJobInput
     * @return Response object having Job details.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/filteredJobDetails")
    Response getFilteredJobDetails(final NeJobInput jobInput);

    /**
     * Method to retrieve Job Logs for NE JOB IDs received from UI
     * 
     * @URL http://localhost:8080/oss/shm/rest/job/joblog
     * @param JobLogRequest
     *            { "neJobIds":"1125899906855281,1125899906855277,1", "orderBy":"asc", "sortBy":"neName", "offset":"1", "limit":"3" }
     * @return Response object having Job Log details
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/joblog")
    Response retrieveJobLogs(final JobLogRequest jobLogRequest);

    /**
     * This method deletes the Job and all related sub-jobs based on Job PO IDs received from UI as Input
     * 
     * @param List
     *            <String> deleteJobRequest
     * @return Response object having delete status information
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/delete")
    Response deleteJobs(final List<String> deleteJobRequest);

    /**
     * @param jobIds
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/jobs/continue")
    Response invokeMainJobsManually(final List<Long> jobIds);

    /**
     * @param neJobIds
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/nejobs/continue/")
    Response invokeNeJobsManually(final List<Long> neJobIds);

    /**
     * Persisting job comments
     * 
     * @URL url : http://<hostname>:<port>/oss/shm/rest/job/comment
     * @param Input
     *            : {"comment":"This job is the Upgrade Job","jobId":"1125899906855281"}
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/comment")
    Response addComment(final CommentInfo commentInfo);

    /**
     * This method exports the activity level Jobs that are exported using exportJobLogs
     * 
     * @param NeJobId
     *            , mainJobId
     * @return Response object having Job Log details
     */
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/joblog/export")
    Response exportJobLogs(@FormParam("neJobIds") final String neJobIds, @FormParam("mainJobId") final String mainJobId);

    /**
     * Retrieves job comments for the provided main job Id.
     * 
     * @param mainJobId
     * @return Response - List of JobComment as a JSON object
     */
    @GET
    @Path("/jobComments")
    @Produces(MediaType.APPLICATION_JSON)
    Response retrieveJobComments(@QueryParam("mainJobId") final Long mainJobId);

    /**
     * Method to get the activity details of NE Job for a given NEJobId
     * 
     * @param jobConfigId
     * @return Response - ActivityJobDetails as a JSON object
     */
    @GET
    @Path("/nodeactivities")
    @Produces(MediaType.APPLICATION_JSON)
    Response getNodeActivityDetails(@QueryParam("neJobId") final Long jobId);

    /**
     * Method to update the given node count(nodeCountToUpdate) of a schedule job with given mainJobPoId
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/jobconfigurationdetails/mainJobPoId/nodeCountToUpdate
     * @param mainJobPoId
     * @param nodeCountToUpdate
     * @return Response - Success or failure status.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("jobconfigurationdetails/{mainJobPoId}/{nodeCountToUpdate}")
    Response updateTotalNoOfNodesCount(@PathParam("mainJobPoId") final Long mainJobPoId, @PathParam("nodeCountToUpdate") final int nodeCountToUpdate);

    /**
     * Rest endpoint provided to get existing NFVOs list to UI
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/nfvolist
     * @return
     */
    @GET
    @Path("/nfvolist")
    @Produces(MediaType.APPLICATION_JSON)
    Response getNfvoList();

    /**
     * Rest point for retrieving the collection details.
     * 
     * @URl url : http://localhost:8080/oss/shm/rest/job/collections
     * @param jobOwner
     *            user id of the user who created the collection.
     * @param collectionId
     *            collection id.
     * @return Response - Collection details as a json object.
     */
    @GET
    @Path("/collections")
    @Produces(MediaType.APPLICATION_JSON)
    Response getCollectionDetails(@QueryParam("jobOwner") final String jobOwner, @QueryParam("collectionId") final String collectionId);

    /**
     * Rest point for retrieving the saved search details
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/savedsearches
     * @param jobOwner
     *            user id of the user who created the collection.
     * @param savedSearchId
     *            saved search id
     * @return Response - Saved search details as a json object.
     */
    @GET
    @Path("/savedsearches")
    @Produces(MediaType.APPLICATION_JSON)
    Response getSavedSearchDetails(@QueryParam("jobOwner") final String jobOwner, @QueryParam("savedSearchId") final String savedSearchId);

    /**
     * Method to retrieve platform for given neTypes in given use case
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/platforms
     * @param neTypesInfo
     *            : Json consist of jobType and list of neTypes :{"jobType": "UPGRADE","neTypes": ["RadioNode", "MSC-BC-BSP","MSC-BC-IS","SGSN-MME","ERBS"]}
     * @return Response - Is a json consist supported and unsupported neTypes by platform
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/platforms")
    Response getNeTypePlatforms(final NeTypesInfo neTypesInfo);

    /**
     * Method to get nodes data of collection or saved search from our database
     * 
     * @param jobOwner
     *            contains job owner username
     * @param jobId
     *            contains id of main jobId
     * @param collectionOrSSID
     *            is the collectionId or SaveSearchId
     * @return
     */
    @GET
    @Path("/getCollectionOrSavedSearchDetails")
    @Produces(MediaType.APPLICATION_JSON)
    Response getCollectionOrSavedSearchDetails(@QueryParam("jobId") final Long jobId);

    /**
     * Method to get the activity timeout.
     * 
     * @param inputDetails
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getActivityTimeout")
    Response getActivityTimeout(@QueryParam("activityJobId") final long activityJobId, @QueryParam("jobType") final String jobType, @QueryParam("activityName") final String activityName,
            @QueryParam("nodeName") final String nodeName);

    /**
     * Rest end point provided to get Supported and unsupported network elements
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/getSupportedNes
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getSupportedNes")
    Response getSupportedNes(final Map<String, List<String>> inputData);

    /**
     * Method to Retrieve network element poid's based on node names
     *
     * @URL url : http://localhost:8080/oss/shm/rest/job/networkelementids
     * @param nodeNames
     *            Example : [ "LTE06dg2ERBS00002", "LTE06dg2ERBS00003", "LTE06dg2ERBS00004" ]
     * @return poIds: [2044042,2044030]
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/networkelementids")
    Response getNetworkElementPoIds(List<String> neNames);
}
