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

import static org.mockito.Mockito.when;

import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradeEvent;
import com.ericsson.oss.services.shm.es.api.JobsNotificationLoadCounter;
import com.ericsson.oss.services.shm.notifications.impl.UpgradeEventNotifier;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeEventNotifierTest {

    @Mock
    private UpgradeEvent upgradeEvent;

    @Mock
    private TimerService timerService;

    @Mock
    private Timer timer;

    @Mock
    JobsNotificationLoadCounter notificationLoadCounter;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private UpgradeEvent event;

    @InjectMocks
    private UpgradeEventNotifier upgradeEventNotifier;

    @Test
    public void testVerifyOngoingNotificationsAndAcceptUpgrade() {
        upgradeEventNotifier.verifyOngoingNotificationsAndAcceptUpgrade(event);
    }

    @Test
    public void testPollForZeroOngoingProcesses() {
        upgradeEventNotifier.pollForZeroOngoingProcesses();
    }

    @Test
    public void testStartTimer() {
        upgradeEventNotifier.startTimer(timer);
    }

    @Test
    public void testStartTimerWithCounterZero() {
        when(notificationLoadCounter.getCounter()).thenReturn(0);
        upgradeEventNotifier.startTimer(timer);
    }
}
