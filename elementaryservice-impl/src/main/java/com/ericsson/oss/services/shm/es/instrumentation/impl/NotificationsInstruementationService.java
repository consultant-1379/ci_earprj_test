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

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.timer.AbstractTimerService;
import com.ericsson.oss.services.shm.es.instrumentation.NotificationsInstruementation;

@Singleton
@Startup
public class NotificationsInstruementationService extends AbstractTimerService implements NotificationsInstruementation {

    private long notificationProcessingTimeInMillis;
    private long noOfNotifications;


    @Inject
    private SystemRecorder systemRecorder;

    public synchronized void capture(final long notificationProcessingTimeInMillis) {
        this.notificationProcessingTimeInMillis = this.notificationProcessingTimeInMillis + notificationProcessingTimeInMillis;
        this.noOfNotifications = this.noOfNotifications + 1;
    }

    @PostConstruct
    public void startTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        startTimerWithMin("Notifications Instrumentation Timer", 10);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @Timeout
    public void recordNotificationMetrics(final Timer timer) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("NotificationProcessingTime", this.notificationProcessingTimeInMillis);
        eventData.put("NumberOfNotifications", this.noOfNotifications);
        systemRecorder.recordEventData("SHM.NOTIFICATION_METRICS", eventData);

        this.notificationProcessingTimeInMillis = 0;
        this.noOfNotifications = 0;
    }
}
