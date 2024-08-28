/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson newTimeoutValue01newTimeoutValue
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MainJobsUpdateInvocationServiceTest {

    @InjectMocks
    private MainJobsUpdateInvocationService objectUnderTest;

    @Mock
    private MainJobProgressUpdateInitiationTimer mainJobProgressTimerMock;
    private int newTimeoutValue = 2;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void test_listenForShmJobProgressUpdateTimerTimeout() {
        int timeOutValue = (int) Whitebox.getInternalState(objectUnderTest, "shmJobProgressUpdateTimerTimeoutInSec");
        String timerName = (String) Whitebox.getInternalState(objectUnderTest, "SHM_JOB_PROGRESS_UPDATE_TIMER");

        objectUnderTest.listenForShmJobProgressUpdateTimerTimeout(newTimeoutValue);

        Assert.assertEquals(0, timeOutValue);
        Assert.assertEquals(newTimeoutValue, Whitebox.getInternalState(objectUnderTest, "shmJobProgressUpdateTimerTimeoutInSec"));
        verify(mainJobProgressTimerMock, times(1)).reStartProgressUpdateTimer(timerName, newTimeoutValue);
    }

    @Test
    public void test_startTimer() {
        int timeOut = (int) Whitebox.getInternalState(objectUnderTest, "shmJobProgressUpdateTimerTimeoutInSec");
        String timerName = (String) Whitebox.getInternalState(objectUnderTest, "SHM_JOB_PROGRESS_UPDATE_TIMER");
        objectUnderTest.startTimer();
        verify(mainJobProgressTimerMock, times(1)).startProgressUpdateTimer(timerName, timeOut);
    }
}
