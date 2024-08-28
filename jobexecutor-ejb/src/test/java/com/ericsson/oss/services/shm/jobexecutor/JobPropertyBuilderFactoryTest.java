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
package com.ericsson.oss.services.shm.jobexecutor;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import javax.enterprise.inject.Instance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobexecutorlocal.JobPropertyProvider;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeQualifier;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@RunWith(MockitoJUnitRunner.class)
public class JobPropertyBuilderFactoryTest {

    @InjectMocks
    JobPropertyBuilderFactory jobPropertyBuilderFactory;

    @Mock
    Instance<JobPropertyProvider> prepareJobPropertiesForBackup;

    @Mock
    Instance<JobPropertyProvider> jobPropertyProvider;

    @Test
    public void test_getProvider() {
        JobTypeQualifier jobTypeQualifier = new JobTypeQualifier(JobType.UPGRADE);
        when(prepareJobPropertiesForBackup.select(jobTypeQualifier)).thenReturn(jobPropertyProvider);
        assertNull(jobPropertyBuilderFactory.getProvider(JobType.UPGRADE));
    }
}
