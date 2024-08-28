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
package com.ericsson.oss.services.shm.tbac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class ActivityJobTBACHelperTest {

    @InjectMocks
    private ActivityJobTBACHelper activityJobTBACHelper;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationService;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    private final static long mainJobId = 789L;
    private final static long neJobId = 2;
    private final static String userName = "userName";
    private final static String neName = "Some Ne Name";

    @Test
    public void testGetJobExecutionUserFromNeJob() {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.SHM_JOB_EXEC_USER);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, userName);
        jobPropertyList.add(jobProperty);
        when(jobConfigurationService.getProjectedAttributes(eq(ShmConstants.NAMESPACE), eq(ShmConstants.NE_JOB), Matchers.anyMap(), eq(Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER))))
                .thenReturn(jobPropertyList);
        activityJobTBACHelper.getJobExecutionUserFromNeJob(neJobId);
        verify(jobConfigurationService).getProjectedAttributes(eq(ShmConstants.NAMESPACE), eq(ShmConstants.NE_JOB), Matchers.anyMap(), eq(Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER)));
    }

    @Test
    public void testGetJobExecutionUserFromMainJob() {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.SHM_JOB_EXEC_USER);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, userName);
        jobPropertyList.add(jobProperty);
        when(jobConfigurationService.getProjectedAttributes(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOB), Matchers.anyMap(), eq(Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER))))
                .thenReturn(jobPropertyList);
        activityJobTBACHelper.getJobExecutionUserFromMainJob(mainJobId);
        verify(jobConfigurationService).getProjectedAttributes(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOB), Matchers.anyMap(), eq(Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER)));
    }

    @Test
    public void testGetActivityExecutionMode() throws MoNotFoundException, JobDataNotFoundException {
        mockNeJobStaticData();
        when(networkElementRetrievalBean.getNeType(neJobStaticDataMock.getNodeName())).thenReturn("RadioNode");
        final String executionMode = activityJobTBACHelper.getActivityExecutionMode(neJobStaticDataMock, jobStaticData, "activate");
        assertNotNull(executionMode);
        assertEquals(ExecMode.IMMEDIATE.getMode(), executionMode);
    }

    private void mockNeJobStaticData() throws JobDataNotFoundException {
        final Map<String, Object> activitySchedules = new HashMap<String, Object>();
        activitySchedules.put(mainJobId + "_RadioNode_activate", ExecMode.IMMEDIATE.getMode());
        neJobStaticDataMock = new NEJobStaticData(neJobId, mainJobId, neName, "1234", "CPP", 5L, null);
        jobStaticData = new JobStaticData("Administrator", activitySchedules, ExecMode.IMMEDIATE.getMode(), JobType.UPGRADE, "TEST_USER");
    }
}
