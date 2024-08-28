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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobexecutor.JobExecutorServiceHelper;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class JobAdministratorTBACValidatorTest {

    @InjectMocks
    private JobAdministratorTBACValidatorImpl jobAdministratorTBACValidatorImpl;

    @Mock
    Map<String, Object> jobTemplateAttributesMock;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private JobTemplate jobTemplate;

    @Mock
    private JobConfiguration jobConfiguration;

    @Mock
    private NEInfo neInfo;

    @Mock
    private JobExecutorServiceHelper jobExecutorServiceHelper;

    @Mock
    private SHMTBACHandler shmTbacHandler;

    private static final long MAINJOBID = 789L;
    private static final long MAINJOBTEMPLATEID = 789L;
    private static final String USERNAME = "userName";

    @Test
    public void validateTBACForMainJob_Success() throws JobDataNotFoundException, MoNotFoundException {
        when(jobUpdateService.retrieveJobWithRetry(MAINJOBTEMPLATEID)).thenReturn(jobTemplateAttributesMock);
        when(jobMapper.getJobTemplateDetails(jobTemplateAttributesMock, MAINJOBTEMPLATEID)).thenReturn(jobTemplate);
        when(jobTemplate.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getSelectedNEs()).thenReturn(neInfo);
        final List<String> neNames = new ArrayList<>();
        neNames.add("NodeName1");
        when(neInfo.getNeNames()).thenReturn(neNames);
        when(shmTbacHandler.isAuthorized(USERNAME, neNames.toArray(new String[0]))).thenReturn(true);
        final boolean isUserAuthorized = jobAdministratorTBACValidatorImpl.validateTBACForMainJob(MAINJOBID, MAINJOBTEMPLATEID, USERNAME);
        assertNotNull(isUserAuthorized);
        assertEquals(true, isUserAuthorized);
    }

    @Test
    public void validateTBACForMainJob_Failure() throws JobDataNotFoundException, MoNotFoundException {
        when(jobUpdateService.retrieveJobWithRetry(MAINJOBTEMPLATEID)).thenReturn(jobTemplateAttributesMock);
        when(jobMapper.getJobTemplateDetails(jobTemplateAttributesMock, MAINJOBTEMPLATEID)).thenReturn(jobTemplate);
        when(jobTemplate.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getSelectedNEs()).thenReturn(neInfo);
        final List<String> neNames = new ArrayList<>();
        neNames.add("NodeName1");
        when(neInfo.getNeNames()).thenReturn(neNames);
        final boolean isUserAuthorized = jobAdministratorTBACValidatorImpl.validateTBACForMainJob(MAINJOBID, MAINJOBTEMPLATEID, USERNAME);
        assertNotNull(isUserAuthorized);
        assertEquals(false, isUserAuthorized);
    }
}
