package com.ericsson.oss.services.shm.job.utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.*;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.InvalidFilterException;
import com.ericsson.oss.services.shm.job.entities.*;
import com.ericsson.oss.services.shm.jobs.common.api.*;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobUtilConstants;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@RunWith(MockitoJUnitRunner.class)
public class SHMJobUtilTest {

    @InjectMocks
    SHMJobUtil jobUtil;

    @Mock
    @Inject
    NeJobLogDetails neJobDetail;

    @Mock
    @Inject
    JobLogDetails activityJobDetail;

    @Mock
    @Inject
    LogDetails logDetail;

    @Mock
    JobReportData jobReportData;

    @Mock
    NeJobInput jobInput;

    @Mock
    JobLogResponse JobLogResponseMock;

    @Mock
    private NeDetails neDetailsMock;

    @Mock
    NeJobDetails neJobDetailsMock;

    @Mock
    JobLogRequest jobLogRequest;

    @Test
    public void getJobOutputJobIdAscTest_JobId() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobId", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_noOfMEs() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("noOfMEs", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_progress() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("progress", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_jobName() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobName", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_jobType() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobType", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_createdBy() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("createdBy", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_status() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("status", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_result() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("result", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_startDate() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("startDate", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_endDate() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("endDate", "asc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdAscTest_null() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(1));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobId", "asc", 1));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_jobId() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobId", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_jobTemplateId() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobTemplateId", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_noOfMEs() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("noOfMEs", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_progress() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("progress", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_jobName() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobName", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_jobType() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobType", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_createdBy() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("createdBy", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_status() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("status", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_result() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("result", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_startDate() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("startDate", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_endDate() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(0));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("endDate", "desc", 0));
        assertNotNull(jobOutput);
    }

    @Test
    public void getJobOutputJobIdDecTest_null() {
        final List<SHMJobData> shmJobDataList = jobUtil.getJobDetailsList(getShmJobs(1));
        final JobOutput jobOutput = jobUtil.sortAndGetPageData(shmJobDataList, getjobInput("jobId", "desc", 1));
        assertNotNull(jobOutput);
    }

    private ShmJobs getShmJobs(final int number) {
        if (number == 0) {

            final ShmJobs shmJobs = new ShmJobs();
            final Map<Long, JobDetails> jobDetailsMap = new HashMap<Long, JobDetails>();

            final List<Map<String, Object>> commentsList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> commentsmap = new HashMap<String, Object>();
            commentsmap.put("date", new Date());
            commentsList.add(commentsmap);

            final JobDetails jobDetails = new JobDetails();
            final JobDetails jobDetails1 = new JobDetails();
            jobDetails.setId(11111l);
            jobDetails1.setId(22222l);
            final List<JobProperty> jobProperties = new ArrayList<JobProperty>();
            final JobProperty jobProperty = new JobProperty("key", "value");
            jobProperties.add(jobProperty);

            final List<JobProperty> jobProperties1 = new ArrayList<JobProperty>();
            final JobProperty jobProperty1 = new JobProperty("Testkey", "Testvalue");
            jobProperties1.add(jobProperty1);
            final List<String> collectionNames = new ArrayList<String>();
            collectionNames.add("abcde");
            collectionNames.add("efgh");

            final List<String> collectionNames1 = new ArrayList<String>();
            collectionNames1.add("Testabcde");
            collectionNames1.add("Testefgh");

            final JobTemplate jobTemplate = new JobTemplate();
            final JobTemplate jobTemplate1 = new JobTemplate();
            final JobConfiguration jobConfiguration = new JobConfiguration();
            final JobConfiguration jobConfiguration1 = new JobConfiguration();
            final List<Job> jobList = new ArrayList<Job>();
            final List<Job> jobList1 = new ArrayList<Job>();
            final Job job = new Job();
            final Job job1 = new Job();

            jobTemplate.setCreationTime(new Date());
            jobTemplate.setDescription("description");
            jobTemplate.setName("jobName");
            jobTemplate.setJobType(JobType.UPGRADE);
            jobTemplate.setOwner("owner");
            final Schedule mainSchedule = new Schedule();
            mainSchedule.setExecMode(ExecMode.SCHEDULED);
            final List<ScheduleProperty> scheduleProperties = new ArrayList<ScheduleProperty>();
            final ScheduleProperty scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName("REPEAT_COUNT");
            scheduleProperty.setValue("3");
            scheduleProperties.add(scheduleProperty);
            mainSchedule.setScheduleAttributes(scheduleProperties);
            jobConfiguration.setMainSchedule(mainSchedule);
            jobTemplate.setJobConfigurationDetails(jobConfiguration);

            jobTemplate1.setCreationTime(new Date());
            jobTemplate1.setDescription("description1");
            jobTemplate1.setName("jobName1");
            jobTemplate1.setJobType(JobType.RESTORE);
            jobTemplate1.setOwner("Admin");
            jobConfiguration1.setMainSchedule(mainSchedule);
            jobTemplate1.setJobConfigurationDetails(jobConfiguration1);

            job.setJobConfigId(11111l);
            job.setId(11111l);
            job.setLevel(number);
            job.setProgressPercentage(0.36);
            job.setResult("result");
            job.setState("state");
            job.setStartTime(new Date());
            job.setEndTime(new Date());
            job.setComment(commentsList);

            job1.setJobConfigId(22222l);
            job1.setId(22222l);
            job1.setLevel(number);
            job1.setProgressPercentage(0.37);
            job1.setResult("Testresult");
            job1.setState("Teststate");
            job1.setStartTime(new Date());
            job1.setEndTime(new Date());
            job1.setComment(commentsList);

            jobList.add(job);
            jobList1.add(job1);
            jobDetails.setJobList(jobList);
            jobDetails1.setJobList(jobList1);

            jobDetails.setJobTemplate(jobTemplate);
            jobDetails1.setJobTemplate(jobTemplate1);

            final List<Activity> activitiesList = new ArrayList<Activity>();
            final List<Activity> activitiesList1 = new ArrayList<Activity>();
            final Activity activity = new Activity();
            final Activity activity1 = new Activity();
            final Schedule schedule = new Schedule();
            schedule.setExecMode(ExecMode.IMMEDIATE);
            final Schedule schedule1 = new Schedule();
            schedule1.setExecMode(ExecMode.IMMEDIATE);

            activity.setName("INSTALL");
            activity.setOrder(1);
            activity.setPlatform(PlatformTypeEnum.CPP);
            activity.setSchedule(schedule);

            activity1.setName("VERIFY");
            activity1.setOrder(2);
            activity1.setPlatform(PlatformTypeEnum.CPP);
            activity1.setSchedule(schedule1);

            final List<NEJobProperty> NEJobPropertyList = new ArrayList<NEJobProperty>();
            final NEJobProperty NeJobProperty = new NEJobProperty();
            NeJobProperty.setJobProperties(jobProperties);
            NeJobProperty.setNeName("LTERBS");
            final List<NEJobProperty> NEJobPropertyList1 = new ArrayList<NEJobProperty>();
            final NEJobProperty NeJobProperty1 = new NEJobProperty();
            NeJobProperty1.setJobProperties(jobProperties1);
            NeJobProperty1.setNeName("ERBS");
            final NEInfo neInfo = new NEInfo();
            neInfo.setCollectionNames(collectionNames);
            neInfo.setNeNames(collectionNames);
            final NEInfo neInfo1 = new NEInfo();
            neInfo1.setCollectionNames(collectionNames1);
            neInfo1.setNeNames(collectionNames1);

            jobConfiguration.setActivities(activitiesList);
            jobConfiguration.setJobProperties(jobProperties);
            jobConfiguration.setNeJobProperties(NEJobPropertyList);
            jobConfiguration.setSelectedNEs(neInfo);

            jobConfiguration1.setActivities(activitiesList1);
            jobConfiguration1.setJobProperties(jobProperties1);
            jobConfiguration1.setNeJobProperties(NEJobPropertyList1);
            jobConfiguration1.setSelectedNEs(neInfo1);

            jobDetailsMap.put(11111l, jobDetails);
            jobDetailsMap.put(22222l, jobDetails1);
            shmJobs.setJobDetailsList(jobDetailsMap);
            return shmJobs;
        } else {
            final ShmJobs shmJobs = new ShmJobs();
            final Map<Long, JobDetails> jobDetailsMap = new HashMap<Long, JobDetails>();
            final List<Map<String, Object>> commentsList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> commentsmap = new HashMap<String, Object>();
            commentsmap.put("date", new Date());
            commentsList.add(commentsmap);

            final JobDetails jobDetails = new JobDetails();
            final JobDetails jobDetails1 = new JobDetails();

            final List<JobProperty> jobProperties = new ArrayList<JobProperty>();
            final JobProperty jobProperty = new JobProperty("key", "value");
            jobProperties.add(jobProperty);
            final List<JobProperty> jobProperties1 = new ArrayList<JobProperty>();
            final JobProperty jobProperty1 = new JobProperty("", "");
            jobProperties1.add(jobProperty1);

            final List<String> collectionNames = new ArrayList<String>();
            collectionNames.add("Test1");
            collectionNames.add("Test2");

            final JobTemplate jobTemplate = new JobTemplate();
            final JobTemplate jobTemplate1 = new JobTemplate();
            final JobConfiguration jobConfiguration = new JobConfiguration();
            final JobConfiguration jobConfiguration1 = new JobConfiguration();
            final Job job = new Job();
            final Job job1 = new Job();
            jobTemplate.setCreationTime(null);
            jobTemplate.setDescription(null);
            jobTemplate.setName(null);
            jobTemplate.setJobType(null);
            jobTemplate.setOwner(null);
            jobTemplate.setJobConfigurationDetails(null);

            jobTemplate1.setCreationTime(null);
            jobTemplate1.setDescription(null);
            jobTemplate1.setName(null);
            jobTemplate1.setJobType(null);
            jobTemplate1.setOwner(null);
            jobTemplate1.setJobConfigurationDetails(null);

            job.setJobConfigId(12345l);
            job.setId(12345l);
            job.setLevel(number);
            job.setProgressPercentage(0);
            job.setResult(null);
            job.setState(null);
            job.setStartTime(null);
            job.setEndTime(null);
            job.setComment(commentsList);

            job1.setJobConfigId(12345l);
            job1.setId(12345l);
            job1.setLevel(number);
            job1.setProgressPercentage(0);
            job1.setResult(null);
            job1.setState(null);
            job1.setStartTime(null);
            job1.setEndTime(null);
            job1.setComment(commentsList);

            final NEInfo neInfo = new NEInfo();
            final NEInfo neInfo1 = new NEInfo();
            final List<Job> jobList = new ArrayList<Job>();
            final List<Job> jobList1 = new ArrayList<Job>();
            final List<NEJobProperty> NEJobPropertyList = new ArrayList<NEJobProperty>();
            final NEJobProperty NeJobProperty = new NEJobProperty();
            final List<NEJobProperty> NEJobPropertyList1 = new ArrayList<NEJobProperty>();
            final NEJobProperty NeJobProperty1 = new NEJobProperty();

            NeJobProperty.setJobProperties(jobProperties);
            NeJobProperty.setNeName("");
            NeJobProperty1.setJobProperties(jobProperties);
            NeJobProperty1.setNeName("");
            jobList.add(job);
            jobList1.add(job1);
            jobDetails.setId(12345l);
            jobDetails.setJobTemplate(jobTemplate);
            jobDetails.setJobList(jobList);
            jobDetails1.setId(54321l);
            jobDetails1.setJobTemplate(jobTemplate1);
            jobDetails1.setJobList(jobList1);

            final List<ScheduleProperty> SchedulePropertyList = new ArrayList<ScheduleProperty>();
            final List<ScheduleProperty> SchedulePropertyList1 = new ArrayList<ScheduleProperty>();
            final Schedule schedule = new Schedule();
            final Schedule schedule1 = new Schedule();

            schedule.setExecMode(ExecMode.IMMEDIATE);
            schedule.setScheduleAttributes(SchedulePropertyList);

            schedule1.setExecMode(ExecMode.IMMEDIATE);
            schedule1.setScheduleAttributes(SchedulePropertyList1);
            final List<Activity> activitiesList = new ArrayList<Activity>();
            final Activity activity = new Activity();
            final List<Activity> activitiesList1 = new ArrayList<Activity>();
            final Activity activity1 = new Activity();
            activity.setName("");
            activity.setOrder(1);
            activity.setPlatform(PlatformTypeEnum.CPP);
            activity.setSchedule(schedule);

            activity1.setName("");
            activity1.setOrder(1);
            activity1.setPlatform(PlatformTypeEnum.CPP);
            activity1.setSchedule(schedule1);

            jobConfiguration.setJobProperties(jobProperties);
            jobConfiguration.setActivities(activitiesList);
            jobConfiguration.setMainSchedule(schedule);
            jobConfiguration.setNeJobProperties(NEJobPropertyList);
            jobConfiguration.setSelectedNEs(neInfo);

            jobConfiguration1.setJobProperties(jobProperties1);
            jobConfiguration1.setActivities(activitiesList1);
            jobConfiguration1.setMainSchedule(schedule1);
            jobConfiguration1.setNeJobProperties(NEJobPropertyList1);
            jobConfiguration1.setSelectedNEs(neInfo1);

            jobDetailsMap.put(12345l, jobDetails);
            jobDetailsMap.put(54321l, jobDetails1);
            shmJobs.setJobDetailsList(jobDetailsMap);
            return shmJobs;
        }
    }

    private JobInput getjobInput(final String sort, final String order, final int number) {
        final JobInput jobInput = new JobInput();
        if (number == 0) {
            final List<String> columns = new ArrayList<String>();
            columns.add("JobName");
            jobInput.setOffset(0);
            jobInput.setLimit(10);
            jobInput.setOrderBy(order);
            jobInput.setSortBy(sort);
            jobInput.setColumns(columns);
        } else {
            final List<String> columns = new ArrayList<String>();
            columns.add("JobName");
            jobInput.setOffset(1);
            jobInput.setLimit(0);
            jobInput.setOrderBy(order);
            jobInput.setSortBy(sort);
            jobInput.setColumns(columns);
        }
        return jobInput;

    }

    @Test
    public void getNEJobIdListTest() {

        final JobLogRequest jobLogInput = new JobLogRequest();
        jobLogInput.setNeJobIds("1,2");

        assertNotNull(jobUtil.getNEJobIdList(jobLogInput));
    }

    @Test
    public void getNEJobIdListNumberFormatExceptionTest() {

        final JobLogRequest jobLogInput = new JobLogRequest();
        jobLogInput.setNeJobIds("1,,2");

        assertNotNull(jobUtil.getNEJobIdList(jobLogInput));
    }

    @Test
    public void getNeJobOutputTest_neNodeName_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeNodeName()).thenReturn("node1", "node2");

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neNodeName");
        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neActivity_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeActivity()).thenReturn("INSTALL", "VERIFY");

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neActivity");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neProgress_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeProgress()).thenReturn(50.00, 100.00);

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neProgress");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neStatus_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeStatus()).thenReturn("RUNNING", "COMPLETED");

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neStatus");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neResult_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeResult()).thenReturn("SUCCESS", "FAILED");

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neResult");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neStartDate_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeStartDate()).thenReturn("31-DEC", "1-JAN");

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neStartDate");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neEndDate_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeEndDate()).thenReturn("1-JAN", "31-JAN");

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neEndDate");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neComments_asc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(jobInput.getOrderBy()).thenReturn("asc");
        when(jobInput.getSortBy()).thenReturn("neComments");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neNodeName_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeNodeName()).thenReturn("node1", "node2");

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neNodeName");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neActivity_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeActivity()).thenReturn("INSTALL", "VERIFY");

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neActivity");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neProgress_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeProgress()).thenReturn(50.00, 100.00);

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neProgress");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neStatus_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeStatus()).thenReturn("RUNNING", "COMPLETED");

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neStatus");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neResult_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeResult()).thenReturn("SUCCESS", "FAILED");

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neResult");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neStartDate_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeStartDate()).thenReturn("31-DEC", "1-JAN");

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neStartDate");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neEndDate_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(neJobDetailsMock.getNeEndDate()).thenReturn("1-JAN", "31-JAN");

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neEndDate");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNeJobOutputTest_neComments_desc() {

        when(jobReportData.getNeDetails()).thenReturn(neDetailsMock);
        when(neDetailsMock.getResult()).thenReturn(Arrays.asList(neJobDetailsMock, neJobDetailsMock));

        when(jobInput.getOrderBy()).thenReturn("desc");
        when(jobInput.getSortBy()).thenReturn("neComments");

        final JobReportData response = jobUtil.getNeJobOutput(jobReportData, jobInput);
        Assert.assertEquals(jobReportData, response);

    }

    @Test
    public void getNEJobIdListforExportTest() {
        final List<Long> response = jobUtil.getNEJobIdListforExport("12345");
        Assert.assertNotNull(response);
    }

    @Test
    public void getJobLogResponsetest_asc_neName() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.asc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.neName);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    @Test
    public void getJobLogResponsetest_asc_activityName() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.asc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.activityName);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    @Test
    public void getJobLogResponsetest_asc_entryTime() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.asc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.entryTime);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));
    }

    @Test
    public void getJobLogResponsetest_asc_message() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.asc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.message);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    @Test
    public void getJobLogResponsetest_desc() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.desc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.neName);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    @Test
    public void getJobLogResponsetest_desc_activityName() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.desc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.activityName);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    @Test
    public void getJobLogResponsetest_desc_entryTime() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.desc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.entryTime);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    @Test
    public void getJobLogResponsetest_desc_message() {

        final JobLogResponse jobLogResponse = new JobLogResponse();
        jobLogResponse.setLogLevel("INFO");
        final List<JobLogResponse> jobLogList = new ArrayList<JobLogResponse>();
        jobLogList.add(jobLogResponse);
        when(jobLogRequest.getOrderBy()).thenReturn(OrderByEnum.desc);
        when(jobLogRequest.getSortBy()).thenReturn(SHMJobUtilConstants.message);
        when(jobLogRequest.getLogLevel()).thenReturn("INFO");
        Assert.assertNotNull(jobUtil.getJobLogResponse(jobLogList, jobLogRequest));

    }

    private List<JobLogResponse> jobLogResponse() {
        final List<JobLogResponse> response = new ArrayList<JobLogResponse>();
        final JobLogResponse jobLogResponse = new JobLogResponse();
        final JobLogResponse jobLogResponse1 = new JobLogResponse();
        jobLogResponse.setActivityName(null);
        jobLogResponse.setEntryTime(null);
        jobLogResponse.setMessage(null);
        jobLogResponse.setNeName(null);

        jobLogResponse1.setActivityName(null);
        jobLogResponse1.setEntryTime(null);
        jobLogResponse1.setMessage(null);
        jobLogResponse1.setNeName(null);

        response.add(jobLogResponse);
        response.add(jobLogResponse1);
        return response;

    }

    @Test
    public void convertTimeZonesTest() {
        Assert.assertNotNull(SHMJobUtil.convertTimeZones("GMT", "GMT", "1"));
    }

    /**
     * To test the fix for bug TORF-56545. Required one item in ShmJob whose template job doesn't exist. Expected Output is its main jobs will not come under shmJobDataList.
     */
    @Test
    public void testGetJobDetailsList() {
        final int mainJobForEachTemplateJob = 5;
        final int otherTemplateJobs = 3;
        final ShmJobs shmJobs = new ShmJobs();
        final Map<Long, JobDetails> jobDetailsMap = new HashMap<Long, JobDetails>();

        final JobDetails jobDetailsWhichIsNotHavingTemplateJob = new JobDetails();
        jobDetailsWhichIsNotHavingTemplateJob.setId(1234L);
        final List<Job> jobsListWhichIsNotHavingTemplateJob = new ArrayList<Job>();
        jobsListWhichIsNotHavingTemplateJob.add(new Job());
        jobsListWhichIsNotHavingTemplateJob.add(new Job());
        jobsListWhichIsNotHavingTemplateJob.add(new Job());
        jobDetailsWhichIsNotHavingTemplateJob.setJobList(jobsListWhichIsNotHavingTemplateJob);
        jobDetailsMap.put(1L, jobDetailsWhichIsNotHavingTemplateJob);

        for (int i = 1; i <= otherTemplateJobs; i++) {
            final JobDetails jobDetails = new JobDetails();
            jobDetails.setId((long) i);
            final JobTemplate jobConfiguration = new JobTemplate();
            jobDetails.setJobTemplate(jobConfiguration);
            final List<Job> jobList = new ArrayList<Job>();
            for (int j = 0; j < mainJobForEachTemplateJob; j++) {
                jobList.add(new Job());
            }
            jobDetails.setJobList(jobList);
            jobDetailsMap.put((long) i, jobDetails);
        }

        shmJobs.setJobDetailsList(jobDetailsMap);
        assertEquals(otherTemplateJobs * mainJobForEachTemplateJob, jobUtil.getJobDetailsList(shmJobs).size());
    }

    @Test(expected = InvalidFilterException.class)
    public void testValidate_Shm_Jobs_Failure1() {
        JobInput jobInput = new JobInput();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("");
        filterDetail.setFilterOperator("*");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        jobInput.setFilterDetails(filterDetails);
        jobUtil.validateShmJobData(jobInput);

    }

    @Test(expected = InvalidFilterException.class)
    public void testValidate_Shm_Jobs_Failure2() {
        JobInput jobInput = new JobInput();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("jobName");
        filterDetail.setFilterOperator("-");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        jobInput.setFilterDetails(filterDetails);
        jobUtil.validateShmJobData(jobInput);

    }

    @Test
    public void testValidate_Shm_Jobs_Failure3() {
        JobInput jobInput = new JobInput();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("jobName");
        filterDetail.setFilterOperator("*");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        jobInput.setFilterDetails(filterDetails);
        jobUtil.validateShmJobData(jobInput);

    }

    @Test(expected = InvalidFilterException.class)
    public void testValidate_Shm_Jobs_Log_Failure1() {
        JobLogRequest jobLogRequest = new JobLogRequest();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("");
        filterDetail.setFilterOperator("*");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        jobLogRequest.setFilterDetails(filterDetails);
        jobUtil.validateShmJobLog(jobLogRequest);

    }

    @Test(expected = InvalidFilterException.class)
    public void testValidate_Shm_Jobs_Log_Failure2() {
        JobLogRequest jobLogRequest = new JobLogRequest();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("neName");
        filterDetail.setFilterOperator("-");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        jobLogRequest.setFilterDetails(filterDetails);
        jobUtil.validateShmJobLog(jobLogRequest);

    }

    @Test
    public void testValidate_Shm_Jobs_Log_Failure3() {
        JobLogRequest jobLogRequest = new JobLogRequest();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("neName");
        filterDetail.setFilterOperator("*");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        jobLogRequest.setFilterDetails(filterDetails);
        jobUtil.validateShmJobLog(jobLogRequest);

    }

    @Test(expected = InvalidFilterException.class)
    public void testValidate_Shm_Jobs_Detail_Failure1() {
        NeJobInput neJobInput = new NeJobInput();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("neName");
        filterDetail.setFilterOperator("*");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        neJobInput.setFilterDetails(filterDetails);
        jobUtil.validateShmJobDetail(neJobInput);

    }

    @Test(expected = InvalidFilterException.class)
    public void testValidate_Shm_Jobs_Detail_Failure2() {
        NeJobInput neJobInput = new NeJobInput();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("neNodeName");
        filterDetail.setFilterOperator("-");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        neJobInput.setFilterDetails(filterDetails);
        jobUtil.validateShmJobDetail(neJobInput);

    }

    @Test
    public void testValidate_Shm_Jobs_Detail_Failure3() {
        NeJobInput neJobInput = new NeJobInput();
        List<FilterDetails> filterDetails = new ArrayList<FilterDetails>();
        FilterDetails filterDetail = new FilterDetails();
        filterDetail.setColumnName("neNodeName");
        filterDetail.setFilterOperator("*");
        filterDetail.setFilterText("");
        filterDetails.add(filterDetail);
        neJobInput.setFilterDetails(filterDetails);
        jobUtil.validateShmJobDetail(neJobInput);

    }

}
