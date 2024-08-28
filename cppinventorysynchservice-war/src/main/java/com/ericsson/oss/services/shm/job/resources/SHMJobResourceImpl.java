package com.ericsson.oss.services.shm.job.resources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EPredefinedRole;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.presentation.server.shm.util.InvalidResourceResponse;
import com.ericsson.oss.services.shm.common.TimeSpendOnJob;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.constants.AccessControlConstants;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.entities.ShmMainJobsResponse;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.impl.CommonUtilityProvider;
import com.ericsson.oss.services.shm.job.service.JobConfigurationDetailService;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.service.vran.impl.NfvoProvider;
import com.ericsson.oss.services.shm.job.utils.CsvBuilder;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.jobexecutor.TopologyEvaluationServiceManager;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobExecutorLocal;
import com.ericsson.oss.services.shm.jobs.common.api.JobQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.api.NetworkElementJobDetails;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Job;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobComment;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.CollectionOrSavedSearchDetails;
import com.ericsson.oss.services.shm.jobservice.common.CommentInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobHandlerErrorCodes;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesPlatformData;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * This class implements the SHMJobResource interface and generates the response accordingly.
 *
 * @author xjhosye
 */
@Path("/job")
public class SHMJobResourceImpl implements SHMJobResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobResourceImpl.class);

    @Inject
    SHMJobService shmJobService;

    @Inject
    JobExecutorLocal jobExecutorLocal;

    @Inject
    private RestDataMapper mapper;

    @Inject
    private ContextService contextService;

    @Inject
    private SHMJobUtil shmJobUtil;

    @Inject
    private JobConfigurationDetailService jobConfigurationDetailService;

    @Inject
    private UserContextBean userContextBean;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NfvoProvider nfvoProvider;

    @Inject
    private TopologyEvaluationServiceManager topologyEvaluationServiceManager;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    private JobConfigurationService jobConfigurationService;

    /**
     * @deprecated Alternatively getSHMJobsData method is used for getting the SHM jobs data
     */

    @Override
    @Deprecated
    public Response getSHMJobs(final JobInput jobInput) {
        LOGGER.debug("Start of getSHMJobs() ");
        final Date startDate = new Date();
        final JobOutput jobOutput = shmJobService.getJobDetails(jobInput);
        LOGGER.debug("Retrieved SHMJobList Successfully");
        final Date endDate = new Date();
        systemRecorder.recordEvent("Overall Jobs Retrieval Time Consumption", EventLevel.COARSE, "DPS Calls, Filtering and Sorting for retrieval of Jobs and Job Templates", "Main Jobs Page",
                TimeSpendOnJob.getDifference(endDate, startDate));
        return Response.status(Response.Status.OK).entity(jobOutput).build();
    }

    @Override
    public Response getSHMJobsData(final JobInput jobInput) {
        LOGGER.debug("Retrieval of SHM Jobs data has been initiated");
        final Date startDate = new Date();
        final ShmMainJobsResponse shmMainJobsResponse = shmJobService.getShmMainJobs(jobInput);
        LOGGER.debug("Retrieved Main Jobs Data Successfully");
        final Date endDate = new Date();
        systemRecorder.recordEvent("Overall Jobs Retrieval Time Consumption for - /v2/jobs", EventLevel.COARSE, "DPS Calls, Filtering and Sorting for retrieval of Jobs and Job Templates",
                "Main Jobs Page", TimeSpendOnJob.getDifference(endDate, startDate));
        return Response.status(Response.Status.OK).entity(shmMainJobsResponse).build();
    }

    @Override
    public Response createJob(final JobInfo jobInfo) {
        LOGGER.info("Create Job request received with information : {}", jobInfo.toString());
        String result = "";
        try {
            if (jobInfo.getName() != null) {
                LOGGER.debug("Start of createJob for the job: {} ", jobInfo.getName());
            }
            final Map<String, Object> res = shmJobService.createShmJob(jobInfo);

            if (res.get("errorCode").toString().equals(JobHandlerErrorCodes.SUCCESS.getResponseDescription())) {
                result = JobHandlerErrorCodes.SUCCESS.getResponseDescription();
                LOGGER.debug("Result : {}", result);
                return Response.status(Response.Status.CREATED).entity(res).build();
            } else {
                result = res.get("errorCode").toString();
                LOGGER.error("Result : {}", result);
                return Response.status(Response.Status.OK).entity(result).build();
            }
        } catch (final NoMeFDNSProvidedException e) {
            result = e.getMessage();
            LOGGER.error("Result :{}", result);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (final NoJobConfigurationException e) {
            result = e.getMessage();
            LOGGER.error("Result : {}", result);
            return Response.status(Response.Status.OK).entity(result).build();
        }
    }

    @Override
    public Response getJobConfiguration(final Long jobTemplateId) {
        LOGGER.info("Calling from REST layer to getSHMJobConfiguration for job config id {} ", jobTemplateId);
        final JobQuery jobQuery = new JobQuery();
        final List<Long> poIds = new ArrayList<Long>();
        poIds.add(jobTemplateId);
        jobQuery.setPoIds(poIds);

        final List<JobTemplate> jobTemplateList = shmJobService.getJobTemplates(jobQuery);
        if (jobTemplateList.size() > 0) {
            // Currently UI is being requesting only for one Job configuration,
            // Now we won't have more than one entries in the result. so directly retrieving the entry as below.
            final JobTemplate jobTemplate = jobTemplateList.iterator().next();
            LOGGER.debug("Retrieved JobConfiguration to REST layer Successfully");
            return Response.status(Response.Status.OK).entity(mapper.mapJobConfigToRestDataFormat(jobTemplate)).build();
        } else {
            LOGGER.info("No Data Found for selected jobTemplateId ID {}", jobTemplateId);
            return Response.status(Response.Status.NOT_FOUND).entity("No Data Found for selected Job").build();
        }
    }

    @Override
    public Response getJobConfigurationDetails(final Long jobTemplateId) {

        LOGGER.info("Calling from REST layer to getSHMJobConfiguration for job config id {} ", jobTemplateId);
        final JobQuery jobQuery = new JobQuery();
        final List<Long> poIds = new ArrayList<Long>();
        poIds.add(jobTemplateId);
        jobQuery.setPoIds(poIds);

        final List<JobTemplate> jobTemplateList = shmJobService.getJobTemplates(jobQuery);
        if (jobTemplateList.size() > 0) {
            // Currently UI is being requesting only for one Job configuration,
            // Now we won't have more than one entries in the result. so directly retrieving the entry as below.
            final JobTemplate jobTemplate = jobTemplateList.iterator().next();
            LOGGER.debug("Retrieved JobConfiguration to REST layer Successfully for the with templateId : {}", jobTemplate.getJobTemplateId());
            return Response.status(Response.Status.OK).entity(jobConfigurationDetailService.getJobConfigurationDetails(jobTemplate)).build();
        } else {
            LOGGER.info("No Data Found for selected jobTemplateId ID {}", jobTemplateId);
            return Response.status(Response.Status.NOT_FOUND).entity("No Data Found for selected Job").build();
        }

    }

    @Override
    public Response cancelJobs(final List<Long> jobIds) {
        LOGGER.debug("Cancelling jobs for the ids:  {} ", jobIds);
        final Map<String, Object> cancelResult = shmJobService.cancelJobs(jobIds);

        LOGGER.debug("Cancelling for the jobs {} has done.", jobIds);
        // TODO: The return object contains status of cancellation for each job. Make use of it to return specific info to UI
        return Response.status(Response.Status.OK).entity(cancelResult).build();
    }

    /**
     * Method gets the response containing job logs from SHM job service class and sends it to UI.
     *
     * @param jobLogRequest
     * @return Response object having Job Log details
     */
    @Override
    public Response retrieveJobLogs(final JobLogRequest jobLogRequest) {
        LOGGER.debug("RetrieveJobLogs resource layer method entry with {} neJobIds", jobLogRequest.getNeJobIds());
        final JobOutput jobOutput = shmJobService.retrieveJobLogs(jobLogRequest);
        LOGGER.debug("Job Log Result is : {} ", jobOutput.getResult());
        LOGGER.debug("RetrieveJobLogs resource layer method exited with {} neJobIds", jobLogRequest.getNeJobIds());
        return Response.status(Response.Status.OK).entity(jobOutput).build();
    }

    @Override
    public Response getJobProgressDetails(final List<Long> poIds) {
        final List<Job> jobs = shmJobService.getJobProgressDetails(poIds);
        return Response.status(jobs.isEmpty() ? Response.Status.NO_CONTENT : Response.Status.OK).entity(jobs).build();
    }

    @Override
    public Response getSHMJobReportDetails(final Long jobId, final int offset, final int limit, final String sortBy, final String orderBy, final boolean selectAll) {
        LOGGER.debug("Start of getJobReportDetails() with jobId:{} ", jobId);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setJobIdsList(Arrays.asList(jobId));
        jobInput.setOffset(offset);
        jobInput.setLimit(limit);
        jobInput.setSortBy(sortBy);
        jobInput.setOrderBy(orderBy);
        jobInput.setSelectAll(selectAll);

        final JobReportData jobReportData = shmJobService.getJobReportDetails(jobInput);
        if (jobReportData != null && jobReportData.getJobDetails() != null && jobReportData.getNeDetails() != null) {
            return Response.status(Response.Status.OK).entity(jobReportData).build();

        }
        LOGGER.debug("No Data found for the selected Job with job id {}", jobId);
        return Response.status(Response.Status.NOT_FOUND).entity("No Job found with the given Job id").build();

    }

    /**
     * This method deletes the Job and all related sub-jobs based on Job PO IDs received from UI as Input
     *
     * @param deleteJobRequest
     * @return Response object having delete status information
     */
    @Override
    public Response deleteJobs(final List<String> deleteJobRequest) {
        LOGGER.debug("deleteJobs resource layer method entry for {}  number of jobs", deleteJobRequest.size());
        final long startTime = System.currentTimeMillis();
        final Map<String, String> resultMsg = shmJobService.deleteShmJobs(deleteJobRequest);
        LOGGER.debug("resource layer method deleteJobs with response {}", resultMsg.values());
        final long timeTaken = System.currentTimeMillis() - startTime;
        systemRecorder.recordEvent(SHMEvents.DELETE_JOBS, EventLevel.COARSE, "", "",
                String.format(SHMEvents.JOB_COMPLETION_RESULT, resultMsg.get(ShmConstants.MESSAGE), deleteJobRequest.size(), timeTaken));
        return Response.status(Response.Status.OK).entity(resultMsg).build();
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.UPDATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public Response invokeMainJobsManually(final List<Long> jobIds) {
        LOGGER.debug("Total number of Jobs requested to initiate is {}", jobIds.size());
        final String loggedInUser = userContextBean.getLoggedInUserName();
        jobExecutorLocal.invokeMainJobsManually(jobIds, loggedInUser);

        final Map<String, Object> response = new HashMap<String, Object>();
        response.put(ShmConstants.MESSAGE, ShmConstants.CONTINUE_JOB_RESPONSE_MESSAGE);
        response.put(ShmConstants.STATUS, "success");
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.UPDATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public Response invokeNeJobsManually(final List<Long> neJobIds) {
        LOGGER.debug("Total number of Jobs requested to initiate is {}", neJobIds.size());
        final String loggedInUser = userContextBean.getLoggedInUserName();
        jobExecutorLocal.invokeNeJobsManually(neJobIds, loggedInUser);
        final Map<String, Object> response = new HashMap<String, Object>();
        response.put(ShmConstants.MESSAGE, ShmConstants.CONTINUE_JOB_RESPONSE_MESSAGE);
        response.put(ShmConstants.STATUS, "success");
        return Response.status(Response.Status.OK).entity(response).build();
    }

    /**
     * This method adds the comments to the selected jobs on basis of JobPOIDs received from UI as request
     *
     * @param commentInfo
     * @return Response object having status information
     */
    @Override
    public Response addComment(final CommentInfo commentInfo) {
        LOGGER.debug("Entering into addComment() method with id:{} and with comment:{}", commentInfo.getJobId(), commentInfo.getComment());
        final Map<String, Object> latestComment = shmJobService.addJobComment(commentInfo);
        return Response.status(Response.Status.OK).entity(latestComment).build();
    }

    @Override
    public Response exportJobLogs(final String neJobIds, final String mainJobId) {
        LOGGER.debug("Export job logs of NE Job IDs {} ", neJobIds);
        List<Long> neJobIdList;
        if (neJobIds == null || neJobIds.length() == 0) {
            final long mainJobIdLong = Long.parseLong(mainJobId);
            neJobIdList = shmJobService.getNeJobIdsForMainJob(mainJobIdLong);
        } else {
            neJobIdList = shmJobUtil.getNEJobIdListforExport(neJobIds);
            LOGGER.trace("Comma separated nejobids added to a list {}", neJobIdList);
        }

        final List<Long> neAllJobIdList = new ArrayList<Long>();
        neAllJobIdList.addAll(neJobIdList);
        if (neAllJobIdList.isEmpty()) {
            LOGGER.info("Job logs cannot be retrieved as there are no NE Jobs available for Main Job :{}", mainJobId);
            return Response.status(Response.Status.OK).build();
        }

        final Long neJobId = neJobIdList.get(0);
        final JobTemplate templateJobAttributeMap = shmJobService.getJobTemplateByNeJobId(neJobId);
        final String jobName = templateJobAttributeMap.getName();
        final String jobType = templateJobAttributeMap.getJobType().name();
        LOGGER.debug("jobname = {} , jobtype = {}", jobName, jobType);
        final StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                final Writer writer = new BufferedWriter(new OutputStreamWriter(os));
                final ExportJobLogRequest jobLogRequestForExport = new ExportJobLogRequest();
                //Defining tab as the separation character to help MS Excel identify the separation character and show exported job logs in tabular form.
                final String columnSeperator = "sep=\t\r\n";
                final String titles = CsvBuilder.getTitles();
                writer.write(columnSeperator + titles);
                jobLogRequestForExport.setJobName(jobName);
                jobLogRequestForExport.setJobType(jobType);

                for (final Long neJobId : neAllJobIdList) {
                    LOGGER.debug("nejobId = {}", neJobId);
                    jobLogRequestForExport.setNeJobIds(neJobId);
                    final String csvOutput = shmJobService.exportJobLogs(jobLogRequestForExport);
                    LOGGER.debug("Successfully retrieved activity job logs for  {}", neJobId);
                    if (csvOutput != null) {
                        writer.write(csvOutput);
                        LOGGER.debug("Writing csv output to memory");
                        writer.flush();
                    }
                }

                if (mainJobId != null && (neJobIds == null || neJobIds.length() == 0)) {
                    jobLogRequestForExport.setMainJobId(Long.parseLong(mainJobId));
                    final String csvOutput = shmJobService.exportMainJobLogs(jobLogRequestForExport);
                    LOGGER.debug("Successfully retrieved main job logs for  {}", mainJobId);
                    if (csvOutput != null) {
                        writer.write(csvOutput);
                        LOGGER.debug("Writing csv output to memory");
                        writer.flush();
                    }

                }
            }
        };

        final String user = contextService.getContextValue("X-Tor-UserID");

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(JobConfigurationConstants.DATE_FORMAT, Locale.getDefault());

        final String fileName = jobName + "_" + jobType + "_" + user + "_" + simpleDateFormat.format(Calendar.getInstance().getTime()) + ".csv";
        LOGGER.debug("fileName : {}", fileName);
        return Response.ok(stream, "text/plain").header("content-disposition", "attachment; filename=" + fileName).build();
    }

    @GET
    @Path("template/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobTemplate(@PathParam("name") final String name) {
        LOGGER.info("In SHMJobResourceImpl, jobName {}", name);
        final JobTemplate jobTemplate = shmJobService.getJobTemplate(name);

        if (jobTemplate != null) {
            LOGGER.info("Job name {} provided by operator can't be used. This name is alredy in use.", name);
            return Response.ok(jobTemplate).build();
        }

        final InvalidResourceResponse invalidResourceResponse = new InvalidResourceResponse();
        invalidResourceResponse.setId("jobTemplate");
        invalidResourceResponse.setMessage("Job with name '" + name + "' is not found");
        LOGGER.info("Job name {} provided by operator can be used.", name);
        return Response.status(Response.Status.NOT_FOUND).entity(invalidResourceResponse).build();
    }

    /**
     * Retrieves job comments for the provided main job Id.
     *
     * @param mainJobId
     * @return Response - List of JobComment as a JSON object
     */
    @Override
    public Response retrieveJobComments(final Long mainJobId) {
        final List<JobComment> jobComments = shmJobService.retrieveJobComments(mainJobId);
        LOGGER.trace("SHMJobResourceImpl.retrieveJobComments() method exit with list of job comments for main job {} : {}", mainJobId, jobComments);
        return Response.status(Response.Status.OK).entity(jobComments).build();
    }

    @Override
    public Response getNodeActivityDetails(final Long neJobId) {
        LOGGER.debug("Start of getJobReportDetails() with jobId:{} ", neJobId);

        final NetworkElementJobDetails networkElementJobDetails = shmJobService.getNodeActivityDetails(neJobId);
        if (networkElementJobDetails != null) {
            return Response.status(Response.Status.OK).entity(networkElementJobDetails).build();

        } else {
            LOGGER.error("No Activity Data Found for selected Job {}", neJobId);
            return Response.status(Response.Status.NO_CONTENT).entity(networkElementJobDetails).build();
        }
    }

    /**
     * Retrieves job details for given NeJobInput.
     */
    @Override
    public Response getFilteredJobDetails(final NeJobInput jobInput) {
        final JobReportData jobReportData = shmJobService.getJobReportDetails(jobInput);
        if (jobReportData != null) {
            return Response.status(Response.Status.OK).entity(jobReportData).build();
        } else {
            LOGGER.debug("No Data Found for selected Job {}", jobInput.getJobIdsList());
            return Response.status(Response.Status.NO_CONTENT).entity(jobReportData).build();
        }
    }

    /**
     * Method to update the given node count(updatedNodeCount) of a schedule job with mainJobPoId
     */
    @Override
    public Response updateTotalNoOfNodesCount(final Long mainJobPoId, final int updatedNodeCount) {
        final boolean updatedResult = shmJobService.updateNodeCountOfJob(mainJobPoId, updatedNodeCount);
        if (updatedResult) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    @Override
    public Response getNfvoList() {
        final List<String> nfvoNames = nfvoProvider.getNfvoNames();
        return Response.status(Response.Status.OK).entity(nfvoNames).build();
    }

    @Override
    @GET
    @Path("/collections")
    @Produces("application/json")
    public Response getCollectionDetails(@QueryParam("jobOwner") final String jobOwner, @QueryParam("collectionId") final String collectionId) {
        LOGGER.info("Calling REST Layer collectionId :{}, jobOwner:{}", collectionId, jobOwner);
        CollectionDetails collectionDetails = new CollectionDetails();
        try {
            collectionDetails = topologyEvaluationServiceManager.getCollectionDetails(collectionId, jobOwner);
        } catch (final Exception exception) {
            LOGGER.error("Found exception while getting collection data:", exception);
            throw new ServerInternalException(" Exception occurred while getting collection data");
        }
        if (collectionDetails != null) {
            if (collectionDetails.getErrorMessage() != null && collectionDetails.getErrorMessage().contains(CollectionDetails.FORBIDDEN_ERROR)) {
                return Response.status(Response.Status.FORBIDDEN).entity("User doesn't have rights to access the collection").build();
            } else {
                return Response.status(Response.Status.OK).entity(collectionDetails).build();
            }
        } else {
            LOGGER.info("Collection not found for the collection ID:{}", collectionId);
            return Response.status(Response.Status.NOT_FOUND).entity("Collection Not Found").build();
        }
    }

    @Override
    @GET
    @Path("/savedsearches")
    @Produces("application/json")
    public Response getSavedSearchDetails(@QueryParam("jobOwner") final String jobOwner, @QueryParam("savedSearchId") final String savedSearchId) {
        LOGGER.info("Calling REST Layer for savedSearchId : {}, jobOwner:{}", savedSearchId, jobOwner);
        SavedSearchDetails savedSearchDetails = new SavedSearchDetails();
        try {
            savedSearchDetails = topologyEvaluationServiceManager.getSavedSearchDetails(savedSearchId, jobOwner);
        } catch (final Exception exception) {
            LOGGER.error("Found exception while getting saved search  data:", exception);
            throw new ServerInternalException(" Exception occurred while getting saved search data");
        }
        if (savedSearchDetails != null) {
            if (savedSearchDetails.getErrorMessage() != null && savedSearchDetails.getErrorMessage().contains(SavedSearchDetails.FORBIDDEN_ERROR)) {
                return Response.status(Response.Status.FORBIDDEN).entity("User doesn't have rights to access the saved search").build();
            } else {
                return Response.status(Response.Status.OK).entity(savedSearchDetails).build();
            }
        } else {
            LOGGER.info("Saved Search not found for the saved search ID:{}", savedSearchId);
            return Response.status(Response.Status.NOT_FOUND).entity("Saved Search Not Found").build();
        }
    }

    @Override
    public Response getCollectionOrSavedSearchDetails(final Long jobId) {
        final Map<String, Object> attributes = jobConfigurationService.retrieveJob(jobId);
        List<Map<String, Object>> jobprop = new ArrayList<>();
        if (attributes != null && !attributes.isEmpty()) {
            jobprop = (List<Map<String, Object>>) attributes.get(ShmConstants.JOBPROPERTIES);
        }
        for (final Map<String, Object> maps : jobprop) {
            if (ShmConstants.COLLECTIONORSSINFO.equals(maps.get(ShmConstants.KEY))) {
                final String listOfMaps = (String) maps.get(ShmConstants.VALUE);
                final List<Map<String, Object>> convertedMap = CommonUtilityProvider.convertStringToList(listOfMaps);
                final List<CollectionOrSavedSearchDetails> collectionOrSSInfo = getCollectionOrSSDetails(convertedMap);
                if (collectionOrSSInfo != null && !collectionOrSSInfo.isEmpty()) {
                    return Response.status(Response.Status.OK).entity(collectionOrSSInfo).build();
                }
            }
        }
        LOGGER.info("Collection or SavedSearch details not found for : {}", jobId);
        return Response.status(Response.Status.NOT_FOUND).entity("Collection or SavedSearch details not found for" + jobId).build();
    }

    private List<CollectionOrSavedSearchDetails> getCollectionOrSSDetails(final List<Map<String, Object>> convertedMap) {
        CollectionOrSavedSearchDetails collectionOrSSInfo = null;
        final List<CollectionOrSavedSearchDetails> list = new ArrayList<>();
        if (convertedMap != null) {
            for (final Map<String, Object> map : convertedMap) {
                final Set<String> set = map.keySet();
                String collectionOrSSIDName = null;
                String collectionOrSSID = null;
                List<String> neNamesList = new ArrayList<>();

                for (String key : set) {
                    if (key.equals(ShmConstants.COLLECTIONORSSNAME)) {
                        collectionOrSSIDName = (String) map.get(ShmConstants.COLLECTIONORSSNAME);
                    } else {
                        neNamesList = (List<String>) map.get(key);
                        collectionOrSSID = key;
                    }
                }
                final List<Map<String, Object>> nodeDetailsInfo = new ArrayList<>();
                for (String neName : neNamesList) {
                    NetworkElementData neDetails = null;
                    try {
                        neDetails = networkElementRetrievalBean.getNetworkElementData(neName.trim());
                    } catch (MoNotFoundException e) {
                        LOGGER.error("neDetails not found for :{},error is {}", neName, e);
                    }
                    if (neDetails != null) {
                        final Map<String, Object> neInfo = new HashMap<>();
                        neInfo.put(ShmConstants.MONAME, neName);
                        neInfo.put(ShmConstants.PLATEFORMTYPE, platformTypeProviderImpl.getPlatformType(neDetails.getNeType()).toString());
                        neInfo.put(ShmConstants.NODETYPE, neDetails.getNeType());
                        neInfo.put(ShmConstants.FDN, neDetails.getNeFdn());
                        nodeDetailsInfo.add(neInfo);
                    }
                }
                if (collectionOrSSID != null && collectionOrSSIDName != null) {
                    collectionOrSSInfo = new CollectionOrSavedSearchDetails();
                    collectionOrSSInfo.setId(new Long(collectionOrSSID));
                    collectionOrSSInfo.setName(collectionOrSSIDName);
                    collectionOrSSInfo.setObjects(nodeDetailsInfo);
                    list.add(collectionOrSSInfo);
                }
            }
        }
        return list;
    }

    @Override
    public Response getNeTypePlatforms(final NeTypesInfo neTypesInfo) {
        LOGGER.debug("Calling REST Layer to get platforms for provided netypes: {} and given jobType: {}", neTypesInfo.getNeTypes(), neTypesInfo.getJobType());
        if (!neTypesInfo.getNeTypes().isEmpty()) {
            final NeTypesPlatformData neTypesPlatformData = shmJobService.getNeTypesPlatforms(neTypesInfo);
            return Response.status(Response.Status.OK).entity(neTypesPlatformData).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Request,no neType found in Request").build();
        }

    }

    @Override
    public Response getActivityTimeout(final long activityJobId, final String jobType, final String activityName, final String nodeName) {
        LOGGER.debug("Calling getActivityTimeout for activityJobId: {} and given jobType: {}", activityJobId, jobType);
        int timeout = 0;
        try {
            timeout = shmJobService.getActivityTimeout(activityJobId, jobType, activityName, nodeName);
        } catch (final Exception ex) {
            LOGGER.info("Exception occurred while fetching activity timeout for jobType:{} and activityName:{}. Reason:", activityName, ex);
        }
        return Response.status(Response.Status.OK).entity(timeout).build();
    }

    @Override
    public Response getSupportedNes(final Map<String, List<String>> neNamesAndJobTypeMap) {
        LOGGER.debug("Calling Rest Layer to get supported and unsupported network elements for job creation");
        List<String> neNames = new ArrayList<>();
        String jobType = "";
        for (final Entry<String, List<String>> entry : neNamesAndJobTypeMap.entrySet()) {
            jobType = entry.getKey();
            neNames = entry.getValue();
        }
        final List<String> nodeNamesWithoutDuplicates = new ArrayList<>(new HashSet<>(neNames));
        final Map<String, Object> supportedAndUnSupportedNetworkElements = shmJobService.getSupportedNes(nodeNamesWithoutDuplicates, jobType);
        return Response.status(Response.Status.OK).entity(supportedAndUnSupportedNetworkElements).build();
    }

    @Override
    public Response getNetworkElementPoIds(final List<String> neNames) {
        final NetworkElementIdResponse networkElementIdsResponse = shmJobService.getNetworkElementPoIds(neNames);

        return Response.status(Response.Status.OK).entity(networkElementIdsResponse).build();

    }
}
