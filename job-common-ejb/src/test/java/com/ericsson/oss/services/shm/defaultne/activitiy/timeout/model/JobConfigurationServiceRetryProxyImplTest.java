/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model;

import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.impl.JobConfigurationServiceRetryProxyImpl;

@RunWith(MockitoJUnitRunner.class)
public class JobConfigurationServiceRetryProxyImplTest {

    @InjectMocks
    private JobConfigurationServiceRetryProxyImpl jobConfigurationServiceRetryProxyImpl;

    @Mock
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    private RetryManager retryManager;

    @Mock
    private JobConfigurationService jobConfigurationService;

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyReturningTrue() {
        getNoOfRetries();
        when(jobConfigurationService.getActivityAttributesByNeJobId(123l, Collections.emptyMap())).thenReturn(Collections.emptyList());
        jobConfigurationServiceRetryProxyImpl.getActivityJobAttributesByNeJobId(123l, Collections.emptyMap());
    }

    private void getNoOfRetries() {
        when(dpsConfigurationParamProvider.getdpsRetryCount()).thenReturn(5);
    }

}
