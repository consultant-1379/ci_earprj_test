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
package com.ericsson.oss.services.shm.job.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;

@RunWith(PowerMockRunner.class)
public class JobStaticDataProviderImplTest {

    @InjectMocks
    private JobStaticDataProviderImpl jobStaticDataProviderImpl;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private JobStaticDataCache jobStaticDataCache;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationService;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private JobConfiguration jobConfiguration;

    @Mock
    private Activity activityMock;

    @Mock
    private Schedule schedule;
    @Mock
    List<Map<String, Object>> jobPropertyList;

    private static final long JOB_ID = 123456L;
    private static final long TEMPLATE_JOB_ID = 5678L;
    private static final String OWNER = "Administrator";
    private static final String EXECUTION_MODE = "IMMEDIATE";
    private static final String EXECUTION_USER = "TEST_USER";

    public void mockJobStaticDataCache() {
        final Map<String, Object> activitySchedules = new HashMap<String, Object>();
        activitySchedules.put(JOB_ID + "_ERBS_install", EXECUTION_MODE);
        jobStaticData = new JobStaticData(OWNER, activitySchedules, EXECUTION_MODE, JobType.UPGRADE,EXECUTION_USER);
        when(jobStaticDataCache.get(JOB_ID)).thenReturn(jobStaticData);
    }

    @Test
    public void testGetJobStaticData_WhenJobStaticDataExists() throws JobDataNotFoundException {
        mockJobStaticDataCache();
        final JobStaticData jobStaticData = jobStaticDataProviderImpl.getJobStaticData(JOB_ID);
        assertNotNull(jobStaticData);
        assertEquals("able to read JobType", JobType.UPGRADE.name(), jobStaticData.getJobType().name());
    }

    @Test
    public void testGetJobStaticData_FromDPS() throws JobDataNotFoundException {
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, TEMPLATE_JOB_ID);
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        mainJobAttributes.put(JobConfigurationConstants.JOB_CONFIGURATIONDETAILS, jobConfigurationDetails);
        when(jobConfigurationService.getMainJobAttributes(JOB_ID)).thenReturn(mainJobAttributes);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.OWNER, "Administrator");
        jobProperty.put(ShmConstants.JOB_TYPE, JobType.UPGRADE.name());
        jobPropertyList.add(jobProperty);
        when(jobConfigurationService.getProjectedAttributes(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE), Matchers.anyMap(), eq(Arrays.asList(ShmConstants.OWNER, ShmConstants.JOB_TYPE))))
                .thenReturn(jobPropertyList);
        when(jobMapper.getJobConfigurationDetails(jobConfigurationDetails)).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(schedule);
        when(schedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        when(activityMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getName()).thenReturn("LTE");
        when(activityMock.getSchedule()).thenReturn(schedule);
        when(jobConfiguration.getActivities()).thenReturn(activities);
        final JobStaticData jobStaticData = jobStaticDataProviderImpl.getJobStaticData(JOB_ID);
        assertNotNull(jobStaticData);

    }
    @Test
    public void testGetShmJobExecUserWhenJobIsManual() {
               final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.SHM_JOB_EXEC_USER, EXECUTION_USER);
        jobProperty.put(ShmConstants.JOB_TYPE, JobType.BACKUP.name());
        jobPropertyList.add(jobProperty);
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(jobPropertyList);
        when(jobPropertyList.get(0)).thenReturn(jobProperty);
        final String jobExecUser = jobStaticDataProviderImpl.getShmJobExecUser(JOB_ID, ExecMode.MANUAL.getMode(),OWNER);
        assertNotNull(jobExecUser);
        assertEquals(EXECUTION_USER, jobExecUser);
    }

    @Test
    public void testGetShmJobExecUserWhenJobIsImmediate() {
           final String jobExecUser = jobStaticDataProviderImpl.getShmJobExecUser(JOB_ID,  ExecMode.IMMEDIATE.getMode(),OWNER);
        assertNotNull(jobExecUser);
        assertEquals(OWNER, jobExecUser);
    }

    @Test
    public void testGetShmJobExecUserWhenJobIsManualAndUserisNull() {
             final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.SHM_JOB_EXEC_USER, null);
        jobProperty.put(ShmConstants.JOB_TYPE, JobType.BACKUP.name());
        jobPropertyList.add(jobProperty);
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(jobPropertyList);
        when(jobPropertyList.get(0)).thenReturn(jobProperty);
        final String jobExecUser = jobStaticDataProviderImpl.getShmJobExecUser(JOB_ID, ExecMode.MANUAL.getMode(), OWNER);
        assertNotNull(jobExecUser);
        assertEquals(OWNER, jobExecUser);
    }
}
