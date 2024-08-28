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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.shared.NotificationListener;

/**
 * Common for Upgrade / Backup / License Job Notification listeners(observers) for listening the respective MO changes
 * 
 * @author xrajeke
 * 
 */
public abstract class AbstractNotificationListener implements NotificationListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    protected NotificationReciever notificationReciever;

    private EventConsumerBean eventConsumerBean;

    @Inject
    private SystemRecorder systemRecorder;
    /**
     * To get the filter to listen only for filtered events.
     * 
     * @return
     */
    protected abstract String getFilter();

    @PostConstruct
    protected void subscribeForDpsEvents() {
        final long postConstructStarted = System.currentTimeMillis();
        startListener();
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /**
     * To start listening for job notifications on a Queue
     */
    private void startListener() {
        eventConsumerBean = getEventConsumerBean();
        eventConsumerBean.setFilter(getFilter());
        final boolean isListening = startListeningForJobNotifications(eventConsumerBean);
        logger.debug("subscribing to Queue for receiving Dps Notifications, and started listening:: {} ", isListening);

    }

    /**
     * To stop listening for job notifications on a Queue (<code>jms:/queue/shmNotificationQueue</code>)
     */
    @Override
    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    /**
     * To get the EventConsumerBean initialized with a Queue(<code>jms:/queue/shmNotificationQueue</code>)
     * 
     * @return - EventConsumerBean
     */
    protected EventConsumerBean getEventConsumerBean() {
        final EventConsumerBean eventConsumerBean = new EventConsumerBean(ShmCommonConstants.SHM_NOTIFICATION_CHANNEL_URI);
        return eventConsumerBean;
    }

    protected boolean startListeningForJobNotifications(final EventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(new ShmNotificationQueueListener(notificationReciever));
    }

}
