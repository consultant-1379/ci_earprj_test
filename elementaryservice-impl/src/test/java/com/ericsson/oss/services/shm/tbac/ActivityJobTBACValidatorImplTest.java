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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.models.TBACConfigurationProvider;

@RunWith(MockitoJUnitRunner.class)
public class ActivityJobTBACValidatorImplTest {

    @InjectMocks
    private ActivityJobTBACValidatorImpl activityJobTBACValidatorImpl;

    @Mock
    private TBACConfigurationProvider tbacConfigurationProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    @Mock
    private ActivityJobTBACHelper activityJobTBACHelper;

    @Mock
    private SHMTBACHandler shmTbacHandler;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private JobLogUtil jobLogUtil;

    private final static long activityJobId = 123L;
    private final static long neJobId = 456L;
    private final static long mainJobId = 789L;
    private final static String neName = "Some Ne Name";
    private final static String userName = "Administrator";

    @Test
    public void validateTbac_Success() throws JobDataNotFoundException, MoNotFoundException {
        mockNeJobStaticData();
        jobStaticData = new JobStaticData("Administrator", new HashMap<String, Object>(), ExecMode.MANUAL.getMode(), JobType.UPGRADE, "TEST_USER");
        when(activityJobTBACHelper.getJobExecutionUserFromMainJob(neJobStaticDataMock.getMainJobId())).thenReturn(userName);
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
        final boolean isUserAuthorized = activityJobTBACValidatorImpl.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, "activate");
        assertNotNull(isUserAuthorized);
        assertEquals(true, isUserAuthorized);
        verify(jobLogUtil, times(0)).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
    }

    @Test
    public void validateTbac_Failed() throws JobDataNotFoundException, MoNotFoundException {
        mockNeJobStaticData();
        jobStaticData = new JobStaticData("Administrator", new HashMap<String, Object>(), ExecMode.IMMEDIATE.getMode(), JobType.UPGRADE, "TEST_USER");
        when(activityJobTBACHelper.getJobExecutionUserFromMainJob(neJobStaticDataMock.getMainJobId())).thenReturn(userName);
        when(activityJobTBACHelper.getActivityExecutionMode(neJobStaticDataMock, jobStaticData, "activate")).thenReturn(ExecMode.IMMEDIATE.getMode());
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        final boolean isUserAuthorized = activityJobTBACValidatorImpl.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, "activate");
        assertNotNull(isUserAuthorized);
        assertEquals(false, isUserAuthorized);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());

    }

    private void mockNeJobStaticData() throws JobDataNotFoundException {
        neJobStaticDataMock = new NEJobStaticData(neJobId, mainJobId, neName, "1234", "CPP", 5L, null);
    }
}
