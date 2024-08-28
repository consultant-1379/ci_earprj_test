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

import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.timer.TimerServiceRetryPolicies;
import com.ericsson.oss.services.shm.cpp.inventory.service.registration.CppInventorySynchEventListenerRegistration;

@RunWith(MockitoJUnitRunner.class)
public class MainJobProgressUpdateInitiationTimerTest {

    @InjectMocks
    private MainJobProgressUpdateInitiationTimer objectUnderTest;

    @Mock
    private MainJobsProgressUpdateService mainJobsProgressNotifierMock;

    @Mock
    private TimerService timerServiceMock;

    @Mock
    private Timer timerMock;

    @Mock
    private CppInventorySynchEventListenerRegistration membershipChangeListenerMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private RetryManager retryManager;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private TimerServiceRetryPolicies timerRetryPolocies;

    @Test
    public void test_startTimer() {

        when(timerRetryPolocies.getTimerRetryPolicy()).thenReturn(retryPolicyMock);

        String event = (String) Whitebox.getInternalState(objectUnderTest, "TIMER_START");

        objectUnderTest.startProgressUpdateTimer("timer", 30);

        verify(systemRecorderMock, times(1)).recordEvent(event, EventLevel.COARSE, "", "timer", "Timer service started successfully with timout of " + 30 + " seconds");
    }

    @Test
    public void test_reStartTimer_noTimersFound() {

        String event = (String) Whitebox.getInternalState(objectUnderTest, "TIMER_START");
        objectUnderTest.reStartProgressUpdateTimer("timer", 30);

        verify(timerMock, never()).cancel();
        verify(systemRecorderMock, never()).recordEvent(event, EventLevel.COARSE, "", "timer", "Timer service started successfully with timout of " + 30 + " seconds");
    }

    @Test
    public void test_reStartTimer_timerNotFound() {
        when(timerServiceMock.getTimers()).thenReturn(Arrays.asList(timerMock));
        String event = (String) Whitebox.getInternalState(objectUnderTest, "TIMER_START");
        objectUnderTest.reStartProgressUpdateTimer("timer", 30);

        verify(timerMock, never()).cancel();
        verify(systemRecorderMock, never()).recordEvent(event, EventLevel.COARSE, "", "timer", "Timer service started successfully with timout of " + 30 + " seconds");
    }

    @Test
    public void test_reStartTimer() {
        when(timerServiceMock.getTimers()).thenReturn(Arrays.asList(timerMock));
        when(timerMock.getInfo()).thenReturn("timer");
        String event = (String) Whitebox.getInternalState(objectUnderTest, "TIMER_START");

        objectUnderTest.reStartProgressUpdateTimer("timer", 30);

        verify(timerMock, times(1)).cancel();
        verify(systemRecorderMock, times(1)).recordEvent(event, EventLevel.COARSE, "", "timer", "Timer service started successfully with timout of " + 30 + " seconds");
    }

    @Test
    public void test_invokeMainJobProgressUpdate_notAMaster_noInvocation() {
        objectUnderTest.invokeMainJobsProgressUpdateService(timerMock);
        verify(membershipChangeListenerMock, times(1)).isMaster();
        verify(mainJobsProgressNotifierMock, never()).invokeMainJobsProgressUpdate();
    }

    @Test
    public void test_invokeMainJobProgressUpdate() {
        when(membershipChangeListenerMock.isMaster()).thenReturn(true);
        objectUnderTest.invokeMainJobsProgressUpdateService(timerMock);
        verify(mainJobsProgressNotifierMock, times(1)).invokeMainJobsProgressUpdate();
    }

}
