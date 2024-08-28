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
package com.ericsson.oss.services.shm.notifications.impl;

import javax.ejb.*;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradeEvent;
import com.ericsson.oss.services.shm.cpp.inventory.service.upgrade.UpgradeEventHandler;
import com.ericsson.oss.services.shm.es.api.JobsNotificationLoadCounter;
import com.ericsson.oss.services.shm.shared.NotificationListener;

@Singleton
public class UpgradeEventNotifier implements UpgradeEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeEventNotifier.class);

    @Inject
    JobsNotificationLoadCounter notificationLoadCounter;

    @Inject
    TimerService timerService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    @Any
    private Instance<NotificationListener> notificationListeners;

    private static final int MAX_WAIT_TIME = 60000;
    private static final int INTERVAL_TIME = 10000;
    private int pollingTime;

    private UpgradeEvent event;

    @Override
    public void verifyOngoingNotificationsAndAcceptUpgrade(final UpgradeEvent event) {
        this.event = event;
        int stoppedListeners = 0;
        if ((notificationListeners) != null) {
            for (final NotificationListener listener : notificationListeners) {
                final boolean isStoppedListener = listener.stopNotificationListener();
                stoppedListeners = isStoppedListener ? ++stoppedListeners : stoppedListeners;
                LOGGER.debug("listener {{}} stopped::{}", listener.getClass().getSimpleName(), isStoppedListener);
            }
        }

        LOGGER.info("Stopped {} unmodeled Event Listeners ", stoppedListeners);
        LOGGER.info("Currently {} notifications are in process", notificationLoadCounter.getCounter());
        pollForZeroOngoingProcesses();
    }

    /**
     * To Poll the processesCounter. It checks the ongoing processes to be zero for every 10 seconds. Will exits once it finds the zero on going processes or if it reaches MAX_WAIT_TIME
     * 
     */
    public void pollForZeroOngoingProcesses() {
        LOGGER.debug(" Polling started ");
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.hour("*");
        scheduleExpression.minute("*");
        scheduleExpression.second("*/" + INTERVAL_TIME / 1000);
        final Timer timer = timerService.createCalendarTimer(scheduleExpression, new TimerConfig("JobPoll", false));
        LOGGER.debug("Created a timer{}", timer);
    }

    /**
     * Method will be triggered after every INTERVAL_TIME{10000 milliseconds}
     * 
     * @param timer
     *            - to stop
     */
    @Timeout
    public void startTimer(final Timer timer) {
        LOGGER.debug("Waiting for {{} milliseconds} to poll the ongoing notification processes. Now polling time reached to {} ", INTERVAL_TIME, pollingTime);
        final int jobsCounter = notificationLoadCounter.getCounter();

        LOGGER.debug("jobsCounter=={}", jobsCounter);
        if ((jobsCounter == 0) || (pollingTime >= MAX_WAIT_TIME)) {
            LOGGER.debug("Found {jobsProcesses={}}, after waiting for {} milliseconds. So cancelling the polling timer and will accept the Upgrade Event.", jobsCounter, pollingTime);
            event.accept("CppInventorySynchEventListener Instance is ready for Upgrade.");
            systemRecorder.recordEvent("Upgrade Notification Observer", EventLevel.DETAILED, "CppInventorySynchEventListener Upgrade", "CppInventorySynchEventListener Upgrade", jobsCounter
                    + " Job Notifications found to be in process, while accepting Upgrade event");
            timer.cancel();
        } else {
            pollingTime = pollingTime + INTERVAL_TIME;
        }
    }
}
