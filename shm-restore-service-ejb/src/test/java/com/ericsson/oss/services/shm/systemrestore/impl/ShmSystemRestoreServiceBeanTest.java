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
package com.ericsson.oss.services.shm.systemrestore.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cluster.restore.*;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.system.restore.JobRecoveryService;
import com.ericsson.oss.services.shm.system.restore.JobRestoreResult;

@RunWith(MockitoJUnitRunner.class)
public class ShmSystemRestoreServiceBeanTest {

    @InjectMocks
    RestoreDelegateServiceBean testBean;

    @Mock
    RestoreManager restoreManagerMock;

    @Mock
    ServiceRestoreResponse serviceRestoreResponseMock;

    @Mock
    SystemRecorder systemRecorderMock;

    @Mock
    JobRecoveryService jobRecoveryServiceMock;

    @Mock
    Future<List<JobRestoreResult>> jobRestoreResultMock;

    @Mock
    List<JobRestoreResult> jobSynchResult;

    @Test
    public void testRestoreWithAllowedState() throws InterruptedException, ExecutionException {
        when(restoreManagerMock.tryRestore(anyLong(), any(TimeUnit.class))).thenReturn(serviceRestoreResponseMock);
        when(serviceRestoreResponseMock.getStatus()).thenReturn(ServiceRestoreStatus.ALLOWED);
        when(jobRecoveryServiceMock.handleJobRestore()).thenReturn(jobRestoreResultMock);
        when(jobRestoreResultMock.get()).thenReturn(jobSynchResult);
        testBean.triggerJobRestore();
        verify(restoreManagerMock, Mockito.atLeastOnce()).finishRestoreWith(ServiceRestoreCompletionStatus.SUCCESS);
    }

    @Test
    public void testRestoreWithNotAllowedState() throws InterruptedException, ExecutionException {
        when(restoreManagerMock.tryRestore(anyLong(), any(TimeUnit.class))).thenReturn(serviceRestoreResponseMock);
        when(serviceRestoreResponseMock.getStatus()).thenReturn(ServiceRestoreStatus.NOT_ALLOWED);
        testBean.triggerJobRestore();
        verify(restoreManagerMock, never()).finishRestoreWith(ServiceRestoreCompletionStatus.SUCCESS);
    }

    @Test
    public void testRestoreWithNotCompletedState() throws InterruptedException, ExecutionException {
        when(restoreManagerMock.tryRestore(anyLong(), any(TimeUnit.class))).thenReturn(serviceRestoreResponseMock);
        when(serviceRestoreResponseMock.getStatus()).thenReturn(ServiceRestoreStatus.COMPLETED);
        testBean.triggerJobRestore();
        verify(restoreManagerMock, never()).finishRestoreWith(ServiceRestoreCompletionStatus.SUCCESS);
    }
}