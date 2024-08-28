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
package com.ericsson.oss.services.shm.jobservice.common;

import static org.mockito.Mockito.when;

import java.util.*;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class JobFactoryTest {

    @InjectMocks
    private JobFactory jobFactoryTest;

    @Mock
    private ScheduleHelperService scheduleHelperService;

    @Mock
    private ScheduleHelperServiceImpl scheduleHelperServiceImpl;

    @Mock
    UserContextBean userContextBean;

    @Test
    public void testCreateJob() throws Exception {

        final Map<String, Object> mapOfActivitySchedules = new HashMap<String, Object>();
        final Map<String, Object> mainSchedule = new HashMap<String, Object>();
        final Map<String, Object> mapOfActivityScheduleValues = new HashMap<String, Object>();
        final List<Map<String, Object>> listOfActivityScheduleValues = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> listOfActivitySchedules = new ArrayList<Map<String, Object>>();

        mapOfActivityScheduleValues.put("activityName", "INSTALL");
        mapOfActivitySchedules.put("PlatformType", "CPP");

        final List<Map<String, Object>> ListOfScheduleProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> schedules = new HashMap<String, Object>();
        final Map<String, Object> scheduleProperties = new HashMap<String, Object>();

        schedules.put("execMode", "IMMEDIATE");

        scheduleProperties.put("name", "PERIOD_MONTH");
        scheduleProperties.put("value", "3");
        ListOfScheduleProperties.add(scheduleProperties);

        mapOfActivityScheduleValues.put("scheduleAttributes", ListOfScheduleProperties);

        listOfActivityScheduleValues.add(mapOfActivityScheduleValues);
        mapOfActivitySchedules.put("value", listOfActivityScheduleValues);

        listOfActivitySchedules.add(mapOfActivitySchedules);

        mainSchedule.put("execMode", "MANUAL");
        mainSchedule.put("scheduleAttributes", ListOfScheduleProperties);

        final JobInfo upgradeJob = new JobInfo();

        upgradeJob.setJobType(JobTypeEnum.UPGRADE);
        upgradeJob.setName("jobName");
        upgradeJob.setOwner("shmtest");
        upgradeJob.setActivitySchedules(listOfActivitySchedules);
        upgradeJob.setMainSchedule(mainSchedule);
        when(userContextBean.getLoggedInUserName()).thenReturn("administrator");
        final JobInfo job1 = jobFactoryTest.createJob(upgradeJob);

        Assert.assertEquals(upgradeJob.getName(), job1.getName());
        Assert.assertEquals(upgradeJob.getJobType(), job1.getJobType());
        Assert.assertEquals(upgradeJob.getDescription(), job1.getDescription());
        Assert.assertEquals(upgradeJob.getMainSchedule(), job1.getMainSchedule());

    }

}
