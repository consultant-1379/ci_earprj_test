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
package com.ericsson.oss.services.shm.es.instrumentation.impl;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.ScheduleExpression;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.timer.AbstractTimerService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TimerConfig.class, ScheduleExpression.class, AbstractTimerService.class })
public class NotificationsInstruementationServiceTest {

    @InjectMocks
    private NotificationsInstruementationService objectUnderTest;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private TimerService timerServiceMock;

    @Mock
    private TimerConfig timerConfigMock;

    @Mock
    private ScheduleExpression scheduleExpressionMock;

    @Test
    public void testStartTimer() {
        try {
            PowerMockito.whenNew(TimerConfig.class).withArguments("Notifications Instrumentation Timer", false).thenReturn(timerConfigMock);
            PowerMockito.whenNew(ScheduleExpression.class).withNoArguments().thenReturn(scheduleExpressionMock);
            Mockito.when(scheduleExpressionMock.hour("*")).thenReturn(scheduleExpressionMock);
            Mockito.when(scheduleExpressionMock.minute("*/" + 10)).thenReturn(scheduleExpressionMock);
            Mockito.when(scheduleExpressionMock.second("0")).thenReturn(scheduleExpressionMock);

            objectUnderTest.startTimer();

            Mockito.verify(scheduleExpressionMock).hour("*");
            Mockito.verify(scheduleExpressionMock).minute("*/" + 10);
            Mockito.verify(scheduleExpressionMock).second("0");
            Mockito.verify(timerServiceMock).createCalendarTimer(scheduleExpressionMock, timerConfigMock);
            Mockito.verify(systemRecorderMock).recordEvent("SHM.TIMER_SERVICE", EventLevel.COARSE, "", "Notifications Instrumentation Timer",
                    "Timer service started successfully with timout of 10 minutes");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testRecordNotificationMetricsDefaultValues() {
        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "notificationProcessingTimeInMillis"));
        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "noOfNotifications"));

        objectUnderTest.recordNotificationMetrics(null);

        verifyRecordEventData(0l, 0l);
    }

    @Test
    public void testCapture() {
        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "notificationProcessingTimeInMillis"));
        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "noOfNotifications"));

        objectUnderTest.capture(0);

        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "notificationProcessingTimeInMillis"));
        Assert.assertEquals(1l, Whitebox.getInternalState(objectUnderTest, "noOfNotifications"));

        objectUnderTest.recordNotificationMetrics(null);

        verifyRecordEventData(0l, 1l);
    }

    @Test
    public void testCaptureMultiplesAndRecord() {

        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "notificationProcessingTimeInMillis"));
        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "noOfNotifications"));

        objectUnderTest.capture(0);
        objectUnderTest.capture(400);
        objectUnderTest.capture(300);
        objectUnderTest.capture(700);

        Assert.assertEquals(1400l, Whitebox.getInternalState(objectUnderTest, "notificationProcessingTimeInMillis"));
        Assert.assertEquals(4l, Whitebox.getInternalState(objectUnderTest, "noOfNotifications"));

        objectUnderTest.recordNotificationMetrics(null);

        verifyRecordEventData(1400l, 4l);

    }

    private void verifyRecordEventData(final long notificationProcessingTimeInMillis, final long noOfNotifications) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("NotificationProcessingTime", notificationProcessingTimeInMillis);
        eventData.put("NumberOfNotifications", noOfNotifications);
        Mockito.verify(systemRecorderMock).recordEventData("SHM.NOTIFICATION_METRICS", eventData);

        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "notificationProcessingTimeInMillis"));
        Assert.assertEquals(0l, Whitebox.getInternalState(objectUnderTest, "noOfNotifications"));
    }
}
