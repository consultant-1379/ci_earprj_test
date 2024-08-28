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
package com.ericsson.oss.services.shm.es.polling;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class ActivityPollingTimerTest {

    @InjectMocks
    private ActivityPollingTimer activityPollingTimer;

    @Mock
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Resource
    @Mock
    private TimerService timerService;

    @Mock
    private Timer timer;

    @Mock
    private SystemRecorder systemRecorder;

    private static final String pollingTimerInfo = "CppActivityPollingTimer";

    @Test
    public void testInitTimer() {
        when(pollingActivityConfiguration.getInitialDelayToStartTimerAfterServiceStartUp()).thenReturn(120000);
        when(pollingActivityConfiguration.getPollingIntervalDelay()).thenReturn(120000);
        activityPollingTimer.initTimer();
        verify(timerService, times(1)).createIntervalTimer(Matchers.anyLong(), Matchers.anyLong(), (TimerConfig) Matchers.anyObject());
    }

    @Test
    public void testRestartTimer() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(pollingTimerInfo);
        timerConfig.setPersistent(false);
        when(timerService.getTimers()).thenReturn(Arrays.asList(timer));
        when(timer.getInfo()).thenReturn(timerConfig.getInfo());
        when(pollingActivityConfiguration.getInitialDelayToStartTimerAfterServiceStartUp()).thenReturn(120000);
        when(pollingActivityConfiguration.getPollingIntervalDelay()).thenReturn(120000);
        activityPollingTimer.restartTimer();
        verify(timer, times(1)).cancel();
    }
}
