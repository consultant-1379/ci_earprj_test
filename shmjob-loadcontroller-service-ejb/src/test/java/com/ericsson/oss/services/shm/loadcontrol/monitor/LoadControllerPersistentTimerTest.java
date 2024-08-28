/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import static org.mockito.Mockito.times;

import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.common.timer.AbstractTimerService;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TimerConfig.class, ScheduleExpression.class, AbstractTimerService.class })
public class LoadControllerPersistentTimerTest {

    @InjectMocks
    private LoadControllerPersistentTimer objectUnderTest;

    @Mock
    private TimerService timerServiceMock;

    @Mock
    private TimerConfig timerConfigMock;

    @Mock
    private ScheduleExpression scheduleExpressionMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private MembershipListenerInterface membershipListenerInterface;

    @Mock
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    @Mock
    Timer timer;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadControllerPersistentTimerTest.class);
    private static final String TIMER_NAME = "LC_PersistDataTimer";

    @Test
    public void testStartTimer() {
        try {
            PowerMockito.whenNew(TimerConfig.class).withArguments(TIMER_NAME, false).thenReturn(timerConfigMock);
            PowerMockito.whenNew(ScheduleExpression.class).withNoArguments().thenReturn(scheduleExpressionMock);

            Mockito.when(scheduleExpressionMock.hour("*")).thenReturn(scheduleExpressionMock);
            Mockito.when(scheduleExpressionMock.minute("*/" + 1)).thenReturn(scheduleExpressionMock);
            Mockito.when(scheduleExpressionMock.second("0")).thenReturn(scheduleExpressionMock);

            objectUnderTest.startTimer();

            Mockito.verify(scheduleExpressionMock).hour("*");
            Mockito.verify(scheduleExpressionMock).minute("*/" + 1);
            Mockito.verify(scheduleExpressionMock).second("0");
            Mockito.verify(timerServiceMock).createCalendarTimer(scheduleExpressionMock, timerConfigMock);
            Mockito.verify(systemRecorderMock).recordEvent("SHM.TIMER_SERVICE", EventLevel.COARSE, "", TIMER_NAME, "Timer service started successfully with timout of 1 minutes");
        } catch (Exception e) {
            LOGGER.error("Exception occured in testStartTimer {}", e);
        }
    }

    @Test
    public void testreadDBEntries() {
        Mockito.when(membershipListenerInterface.isMaster()).thenReturn(true);
        Mockito.when(timer.getInfo()).thenReturn(TIMER_NAME);
        objectUnderTest.readDBEntries(timer);
        Mockito.verify(loadControllerPersistenceManager, times(1)).readAndProcessStagedActivityPOs();

    }

    @Test
    public void testreadDBEntriesWhenVMisNotMaster() {
        Mockito.when(membershipListenerInterface.isMaster()).thenReturn(false);
        Mockito.when(timer.getInfo()).thenReturn(TIMER_NAME);
        objectUnderTest.readDBEntries(timer);
        Mockito.verify(loadControllerPersistenceManager, times(0)).readAndProcessStagedActivityPOs();

    }
}
