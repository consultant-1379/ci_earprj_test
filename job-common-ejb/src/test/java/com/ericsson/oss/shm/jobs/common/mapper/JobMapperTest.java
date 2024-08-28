package com.ericsson.oss.shm.jobs.common.mapper;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants.SELECTED_NES;
import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityDetails;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityJobDetail;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Job;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ShmJobs;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

@RunWith(MockitoJUnitRunner.class)
public class JobMapperTest {

    private static final String RUNNING = "Running";
    private static final String COMPLETED = "Completed";
    private static final String NODENAME2 = "LTE02";
    private static final String NODENAME3 = "LTE03";
    private static final String NODENAME4 = "LTE04";
    private static final String NODENAME5 = "LTE05";
    private static final String PRECHECK_COMPLETED = "Precheck Completed.";
    private static final String KEY_VALUE = "key";
    private static final String VALUE_VALUE = "value";

    @InjectMocks
    JobMapper objectTobeTested;

    @Mock
    NetworkElementRetrievalBean networkElementRetrievalBean;

    @Test
    public void testGetSHMJobsDetails() {
        final List<Map<String, Object>> mainJobPoList = new ArrayList<>();
        final Map<String, Object> mainJobPo = new HashMap<String, Object>();
        mainJobPo.put(ShmConstants.PO_ID, 123l);
        mainJobPo.put(ShmConstants.PROGRESSPERCENTAGE, 66.33);
        mainJobPo.put(ShmConstants.RESULT, "Success");
        mainJobPo.put(ShmConstants.STARTTIME, new Date());
        mainJobPo.put(ShmConstants.ENDTIME, new Date());
        mainJobPo.put(ShmConstants.STATE, RUNNING);
        mainJobPo.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, 5);
        mainJobPo.put(ShmConstants.JOBTEMPLATEID, 456l);
        mainJobPo.put(JobModelConstants.JOB_COMMENT, new ArrayList<Map<String, Object>>());
        mainJobPoList.add(mainJobPo);
        ShmJobs shmJobs = new ShmJobs();
        final Map<Long, JobDetails> jobDetailsMap = new HashMap<Long, JobDetails>();
        shmJobs.setJobDetailsList(jobDetailsMap);
        shmJobs = objectTobeTested.getSHMJobsDetails(mainJobPoList, shmJobs);
        Assert.assertNotNull(shmJobs);
        Assert.assertNotNull(shmJobs.getJobDetailsMap());
        Assert.assertTrue(shmJobs.getJobDetailsMap().size() == 1);
        Assert.assertTrue(shmJobs.getJobDetailsMap().get(456l).getJobList().size() == 1);
    }

    @Test
    public void testGetSHMJobsDetailsForMultipleJobs() {
        final List<Map<String, Object>> mainJobPoList = new ArrayList<>();
        final Map<String, Object> mainJobPo = new HashMap<String, Object>();
        mainJobPo.put(ShmConstants.PO_ID, 123l);
        mainJobPo.put(ShmConstants.PROGRESSPERCENTAGE, 66.33);
        mainJobPo.put(ShmConstants.RESULT, "Success");
        mainJobPo.put(ShmConstants.STARTTIME, new Date());
        mainJobPo.put(ShmConstants.ENDTIME, new Date());
        mainJobPo.put(ShmConstants.STATE, RUNNING);
        mainJobPo.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, 5);
        mainJobPo.put(ShmConstants.JOBTEMPLATEID, 456l);
        mainJobPo.put(JobModelConstants.JOB_COMMENT, new ArrayList<Map<String, Object>>());
        mainJobPoList.add(mainJobPo);
        final JobDetails jobDetails = new JobDetails();
        final List<Job> jobList = new ArrayList<Job>();
        jobList.add(new Job());
        jobDetails.setJobList(jobList);
        ShmJobs shmJobs = new ShmJobs();
        final Map<Long, JobDetails> jobDetailsMap = new HashMap<Long, JobDetails>();
        jobDetailsMap.put(456l, jobDetails);
        shmJobs.setJobDetailsList(jobDetailsMap);
        shmJobs = objectTobeTested.getSHMJobsDetails(mainJobPoList, shmJobs);
        Assert.assertNotNull(shmJobs);
        Assert.assertNotNull(shmJobs.getJobDetailsMap());
        Assert.assertTrue(shmJobs.getJobDetailsMap().size() == 1);
        Assert.assertTrue(shmJobs.getJobDetailsMap().get(456l).getJobList().size() == 2);
    }

    @Test
    public void testgetJobConfigurationDetails() {
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<>();
        final Map<String, Object> jobTemplateAttributes = new HashMap<String, Object>();
        jobTemplateAttributes.put("name", "JobName");
        jobTemplateAttributes.put("jobType", "BACKUP");
        jobTemplateAttributes.put("owner", "shm_admin");
        jobTemplateAttributes.put("creationTime", new Date());
        jobTemplateAttributes.put("jobCategory", "UI");

        final Map<String, Object> jobConfigurationdetails = new HashMap<>();

        jobConfigurationdetails.put(SELECTED_NES, getNeInfo());
        jobConfigurationdetails.put(MAIN_SCHEDULE, getScheduleAttributes());
        jobTemplateAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationdetails);
        jobConfigurationAttributesHolder.put(456l, jobTemplateAttributes);
        final JobDetails jobDetails = new JobDetails();
        final List<Job> jobList = new ArrayList<Job>();
        jobList.add(new Job());
        jobDetails.setJobList(jobList);
        ShmJobs shmJobs = new ShmJobs();
        final Map<Long, JobDetails> jobDetailsMap = new HashMap<>();
        jobDetailsMap.put(456l, jobDetails);
        shmJobs.setJobDetailsList(jobDetailsMap);
        shmJobs = objectTobeTested.getJobConfigurationDetails(jobConfigurationAttributesHolder, shmJobs);
        Assert.assertNotNull(shmJobs);
    }

    @Test
    public void testGetJobTemplateDetails() {
        final Map<String, Object> jobConfigurationDetailsMap = new HashMap<>();
        jobConfigurationDetailsMap.put(ShmConstants.JOBPROPERTIES, getJobProperties("KEY", "VALUE"));
        jobConfigurationDetailsMap.put(ShmConstants.NETYPEJOBPROPERTIES, buildProperties(ShmConstants.NETYPE, "ERBS"));
        jobConfigurationDetailsMap.put(ShmConstants.PLATFORMJOBPROPERTIES, buildProperties(ShmConstants.PLATFORM, "CPP"));
        jobConfigurationDetailsMap.put(SELECTED_NES, getNeInfo());
        jobConfigurationDetailsMap.put(MAIN_SCHEDULE, getScheduleAttributes());
        jobConfigurationDetailsMap.put(ACTIVITIES, getActivities());
        jobConfigurationDetailsMap.put("neJobProperties", buildProperties(ShmConstants.NE_NAME, "LTE01"));
        final Map<String, Object> jobTemplateAttributes = new HashMap<>();
        jobTemplateAttributes.put("name", "SHM_Job");
        jobTemplateAttributes.put("jobType", "BACKUP");
        jobTemplateAttributes.put("owner", "admin");
        jobTemplateAttributes.put("creationTime", new Date());
        jobTemplateAttributes.put("jobCategory", "UI");
        jobTemplateAttributes.put("description", "New Job");
        jobTemplateAttributes.put("jobConfigurationDetails", jobConfigurationDetailsMap);
        final JobTemplate jobTemplate = objectTobeTested.getJobTemplateDetails(jobTemplateAttributes, 123l);
        Assert.assertTrue("SHM_Job".equals(jobTemplate.getName()));
        Assert.assertTrue(JobCategory.UI.equals(jobTemplate.getJobCategory()));
        Assert.assertNotNull(jobTemplate.getJobConfigurationDetails());
    }

    @Test
    public void testGetJobReportRefined() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<Long, List<ActivityJobDetail>>();
        final Date d = new Date();
        final Map<String, Object> neJob = prepareNeJob();
        //final Map<String, List<ActivityJobDetail>>
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        matchedNeJobs.add(neJob);
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.WAIT_FOR_USER_INPUT.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy((JobConfigurationConstants.NE_JOBID));
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 1);
        for (NeJobDetails list : neDetails.getResult()) {
            Assert.assertTrue(list.getNodeType() != null);
        }
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsRunning() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        final Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        matchedNeJobs.add(neJob);
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.RUNNING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy((JobConfigurationConstants.NE_NODE_NAME));
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<String, Object>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 1);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancelling() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        final Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        matchedNeJobs.add(neJob);
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy((JobConfigurationConstants.NE_STATUS));
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<String, Object>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 1);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultAscendingWithNedetailsUnavailable() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 0);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultAscendingWithNedetailsNotNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, COMPLETED);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultAscendingWithFirstNeDetailsNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy((JobConfigurationConstants.NE_RESULT));
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultAscendingWithSecondNedetailsNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultAscendingWithNedetailsNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy(ShmConstants.ASENDING);
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultAscendingWithFiveNedetailEntries() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();

        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 60);
        neJob.put(ShmConstants.NE_NAME, NODENAME3);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 60d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 50);
        neJob.put(ShmConstants.NE_NAME, NODENAME4);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 50d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, "Waiting");
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 20);
        neJob.put(ShmConstants.NE_NAME, NODENAME5);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 20d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy(ShmConstants.ASENDING);
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        jobInput.setOffset(3);
        jobInput.setLimit(5);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();

        Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_2", "Dummy_Value_2"));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME3);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_3", "Dummy_Value_3"));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME4);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_4", "Dummy_Value_4"));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME5);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_5", "Dummy_Value_5"));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 5);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultDescendingWithNedetailsNotNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, COMPLETED);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("desc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultDescendingWithFirstNedetailsNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("desc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultDescendingWithSecondNedetailsNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("desc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultDescendingWithNedetailsNull() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy(ShmConstants.DESENDING);
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        final Map<String, Object> argumentMap = new HashMap<String, Object>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, KEY_VALUE, VALUE_VALUE));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 2);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultDescendingWithFiveNedetails() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();

        Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80);
        neJob.put(ShmConstants.NE_NAME, NODENAME2);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 80d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 60);
        neJob.put(ShmConstants.NE_NAME, NODENAME3);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 60d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, RUNNING);
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 50);
        neJob.put(ShmConstants.NE_NAME, NODENAME4);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 50d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, "Waiting");
        matchedNeJobs.add(neJob);

        neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 20);
        neJob.put(ShmConstants.NE_NAME, NODENAME5);
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 20d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, PRECHECK_COMPLETED + ShmConstants.DELIMITER_PIPE + d.getTime());
        neJob.put(ShmConstants.NE_RESULT, null);
        matchedNeJobs.add(neJob);

        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy(ShmConstants.DESENDING);
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_RESULT);
        jobInput.setOffset(2);
        jobInput.setLimit(5);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);

        final List<Map<String, Object>> neJobProperties = new ArrayList<>();

        Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME2);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_2", "Dummy_Value_2"));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME3);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_3", "Dummy_Value_3"));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME4);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_4", "Dummy_Value_4"));
        neJobProperties.add(neJobProperty);

        neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, NODENAME5);
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE, "Dummy_Key_5", "Dummy_Value_5"));
        neJobProperties.add(neJobProperty);

        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 5);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsCancellingSortByNeResultDescendingWithNedetailsUnavailable() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("desc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_STATUS);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 0);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityIsSystemCancelling() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        final Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        matchedNeJobs.add(neJob);
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.SYSTEM_CANCELLING.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("asc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy((JobConfigurationConstants.NE_RESULT));
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 1);
    }

    @Test
    public void testGetJobReportRefinedWhenAcivityExecmodeIsManual() {
        final List<Map<String, Object>> matchedNeJobs = new ArrayList<>();
        final List<ActivityJobDetail> activityJobs = new ArrayList<ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> activityResponseList = new HashMap<>();
        final Date d = new Date();
        final Map<String, Object> neJob = prepareNeJob();
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100d);
        neJob.put(ShmConstants.LAST_LOG_MESSAGE, "Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        matchedNeJobs.add(neJob);
        final ActivityJobDetail activityJobDetail = new ActivityJobDetail();
        activityJobDetail.setState(JobState.WAIT_FOR_USER_INPUT.name());
        activityJobDetail.setLastLogMessage("Precheck Initiated." + ShmConstants.DELIMITER_PIPE + d.getTime());
        activityJobs.add(activityJobDetail);
        activityResponseList.put(451L, activityJobs);
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setOrderBy("desc");
        jobInput.setFilterDetails(new ArrayList<FilterDetails>());
        jobInput.setSortBy(JobConfigurationConstants.NE_JOBID);
        final Map<String, Object> argumentMap = new HashMap<>();
        argumentMap.put("matchedNeJobAttributes", matchedNeJobs);
        argumentMap.put("activityResponseList", activityResponseList);
        argumentMap.put("jobInput", jobInput);
        final List<Map<String, Object>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmConstants.NE_NAME, "LTE01");
        neJobProperty.put(ShmConstants.NETYPEJOBPROPERTIES, getJobProperties(ShmConstants.KEY, ShmConstants.VALUE));
        neJobProperties.add(neJobProperty);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        final Map<String, Object> jobPOAttributes = new HashMap<>();
        jobPOAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
        final NeDetails neDetails = objectTobeTested.getJobReportRefined(jobPOAttributes, "BACKUP", argumentMap);
        Assert.assertNotNull(neDetails);
        Assert.assertTrue(neDetails.getTotalCount() == 1);
    }

    @Test
    public void testGetJobActivityDetails() {
        final List<Map<String, Object>> activityResponseList = new ArrayList<>();
        final Map<String, Object> activityJob = prepareActivityJob();
        activityJob.put(ShmConstants.ACTIVITY_RESULT, "Success");
        activityJob.put(ShmConstants.ACTIVITY_END_DATE, new Date());
        activityJob.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJob.put(ShmConstants.START_DATE, "2016-11-01 10:45:00");
        activityJob.put(ShmConstants.EXECUTION_MODE, "Scheduled");
        activityJob.put(ShmConstants.ACTIVITY_CONFIGURATION, new HashMap<>());
        activityResponseList.add(activityJob);
        final List<ActivityDetails> activityDetails = objectTobeTested.getJobActivityDetails(activityResponseList, null);
        Assert.assertNotNull(activityDetails);
        Assert.assertTrue(activityDetails.size() == 1);
    }

    @Test
    public void testGetMainJobDeatils() {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        final Map<String, Object> jobTemplatePOAttributes = new HashMap<>();
        final List<Map<String, Object>> commentList = new ArrayList<>();
        final Map<String, Object> comment = new HashMap<>();
        comment.put(ShmConstants.DATE, new Date());
        comment.put(ShmConstants.COMMENT, "Job Created");
        comment.put(ShmConstants.USERNAME, "shm admin");
        commentList.add(comment);
        mainJobAttributes.put(ShmConstants.RESULT, "Success");
        mainJobAttributes.put(ShmConstants.STARTTIME, new Date());
        mainJobAttributes.put(ShmConstants.ENDTIME, new Date());
        mainJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 100d);
        mainJobAttributes.put(ShmConstants.STATE, COMPLETED);
        mainJobAttributes.put(JobModelConstants.JOB_COMMENT, commentList);
        jobTemplatePOAttributes.put(ShmConstants.NAME, "Job Name");
        jobTemplatePOAttributes.put(ShmConstants.OWNER, "admin");
        jobTemplatePOAttributes.put(ShmConstants.DESCRIPTION, "New backup");
        jobTemplatePOAttributes.put(ShmConstants.JOB_TYPE, "BACKUP");
        jobTemplatePOAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, new HashMap<>());
        final JobReportDetails jobReportDetails = objectTobeTested.getMainJobDeatils(mainJobAttributes, jobTemplatePOAttributes);
        Assert.assertNotNull(jobReportDetails);
        Assert.assertTrue("Success".equals(jobReportDetails.getJobResult()));
        Assert.assertTrue("BACKUP".equals(jobReportDetails.getJobType()));
    }

    private Map<String, Object> prepareNeJob() {
        final Map<String, Object> neJob = new HashMap<>();
        neJob.put(ShmConstants.PO_ID, 451l);
        neJob.put(ShmConstants.NE_START_DATE, new Date());
        neJob.put(ShmConstants.NE_END_DATE, new Date());
        neJob.put(ShmConstants.NE_PROG_PERCENTAGE, 100);
        neJob.put(ShmConstants.NE_NAME, "LTE01");
        neJob.put(ShmConstants.NE_RESULT, "");
        neJob.put(ShmConstants.NE_STATUS, "running");
        neJob.put(ShmConstants.NETYPE, "ERBS");
        return neJob;
    }

    private Map<String, Object> prepareActivityJob() {
        final Map<String, Object> activityJob = new HashMap<>();
        activityJob.put(ShmConstants.PO_ID, 798l);
        activityJob.put(ShmConstants.ACTIVITY_NE_JOB_ID, 451l);
        activityJob.put(ShmConstants.ACTIVITY_NAME, "Install");
        activityJob.put(ShmConstants.ACTIVITY_ORDER, 3);
        return activityJob;
    }

    private List<Map<String, Object>> getActivities() {
        final List<Map<String, Object>> activitiesMapList = new ArrayList<>();
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(NAME, "install");
        activityMap.put(ORDER, 1);
        activityMap.put(PLATFORM, "CPP");
        activityMap.put(SCHEDULE, getScheduleAttributes());
        activityMap.put(NETYPE, "ERBS");
        activitiesMapList.add(activityMap);
        return activitiesMapList;
    }

    private List<Map<String, Object>> getJobProperties(final String key, final String value) {
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        jobPropertyMap.put(key, "Job_key");
        jobPropertyMap.put(value, "Job_Value");
        jobPropertiesList.add(jobPropertyMap);
        return jobPropertiesList;
    }

    private List<Map<String, Object>> getJobProperties(final String key, final String value, final String keyValue, final Object valueValue) {
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        jobPropertyMap.put(key, keyValue);
        jobPropertyMap.put(value, valueValue);
        jobPropertiesList.add(jobPropertyMap);
        return jobPropertiesList;
    }

    private List<Map<String, Object>> buildProperties(final String key, final String value) {
        final List<Map<String, Object>> neTypeJobPropertiesMap = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(key, value);
        neJobProperty.put(ShmConstants.JOBPROPERTIES, new ArrayList<JobProperty>());
        neTypeJobPropertiesMap.add(neJobProperty);
        return neTypeJobPropertiesMap;
    }

    private Map<String, Object> getScheduleAttributes() {
        final List<Map<String, Object>> schedulePropertyList = new ArrayList<>();
        final Map<String, Object> schedulePropertyMap = new HashMap<String, Object>();
        schedulePropertyMap.put("name", "schedule");
        schedulePropertyMap.put("value", "nextday");
        schedulePropertyList.add(schedulePropertyMap);
        final Map<String, Object> scheduleMap = new HashMap<String, Object>();
        scheduleMap.put("execMode", "SCHEDULED");
        scheduleMap.put("scheduleAttributes", schedulePropertyList);
        return scheduleMap;
    }

    private Map<String, Object> getNeInfo() {
        final Map<String, Object> neInfo = new HashMap<String, Object>();
        neInfo.put("collectionNames", Arrays.asList("collection1"));
        neInfo.put("neNames", Arrays.asList("neName"));
        neInfo.put(ShmConstants.SAVED_SEARCH_IDS, Arrays.asList("savedSearchId"));
        return neInfo;
    }
}