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
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class MainJobsProgressUpdateServiceTest {

    @InjectMocks
    private MainJobsProgressUpdateService objectUnderTest;

    @Mock
    private RetryManager retryManagerMock;

    @Mock
    private DpsRetryPolicies dpsRetryPoliciesMock;

    @Mock
    private MainJobProgressUpdater mainJobProgressUpdater;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private Map<String, Object> mapMock;

    @Test
    public void test_invokeMainJobsProgressUpdate_noRunningJobsFound() {
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(ShmDpsRetriableCommand.class))).thenReturn(Collections.EMPTY_LIST);

        objectUnderTest.invokeMainJobsProgressUpdate();

        verify(mainJobProgressUpdater, never()).updateMainJobProgress(anyLong());
    }

    @Test
    public void test_invokeMainJobsProgressUpdate_updatesProgress() {
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(ShmDpsRetriableCommand.class))).thenReturn(Arrays.asList(123l));

        objectUnderTest.invokeMainJobsProgressUpdate();

        verify(mainJobProgressUpdater, times(1)).updateMainJobProgress(123l);
    }

    private void retryMock() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(dpsRetryPoliciesMock.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
    }
}
