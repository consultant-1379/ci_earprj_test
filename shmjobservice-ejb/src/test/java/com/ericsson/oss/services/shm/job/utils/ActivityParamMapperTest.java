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
package com.ericsson.oss.services.shm.job.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeJobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

@RunWith(MockitoJUnitRunner.class)
public class ActivityParamMapperTest {

    @InjectMocks
    private ActivityParamMapper activityParamMapper;

    private final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    public JobConfiguration getMockData() {
        final JobConfiguration jobConfiguration = new JobConfiguration();
        List<Activity> activities = new ArrayList<Activity>();
        Activity activity = new Activity();
        activity.setName("install");
        activity.setNeType("ERBS");
        activity.setPlatform(PlatformTypeEnum.CPP);
        Schedule schedule = new Schedule();
        schedule.setExecMode(ExecMode.MANUAL);
        activity.setSchedule(schedule);

        Activity activity1 = new Activity();
        activity1.setName("confirm");
        activity1.setNeType("ERBS");
        activity1.setPlatform(PlatformTypeEnum.CPP);
        Schedule schedule1 = new Schedule();
        schedule1.setExecMode(ExecMode.MANUAL);
        activity1.setSchedule(schedule1);
        activities.add(activity1);

        jobConfiguration.setActivities(activities);
        List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<NeTypeJobProperty>();
        JobProperty jobProperty = new JobProperty(JobPropertyConstants.FORCE_INSTALL, "YES");
        List<JobProperty> jobProperties = new ArrayList<JobProperty>();
        jobProperties.add(jobProperty);
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        neTypeJobProperty.setJobProperties(jobProperties);
        neTypeJobProperty.setNeType("ERBS");

        neTypeJobProperties.add(neTypeJobProperty);
        jobConfiguration.setNeTypeJobProperties(neTypeJobProperties);
        return jobConfiguration;
    }

    @Test
    public void testGetJobConfigurationDetails() {

        acitvityParameters.put("install", Arrays.asList(JobPropertyConstants.FORCE_INSTALL, JobPropertyConstants.SELECTIVE_INSTALL));
        acitvityParameters.put("upgrade", Arrays.asList(JobPropertyConstants.REBOOTNODEUPGRADEPARAM));

        JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(getMockData(), "ERBS", PlatformTypeEnum.CPP.name(), acitvityParameters);
        Assert.assertNotNull(configurationDetails);
        Assert.assertEquals("ERBS", configurationDetails.getNeType());
    }

}
