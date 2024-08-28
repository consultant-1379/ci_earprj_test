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
package com.ericsson.oss.services.shm.webpush.notifications.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.shared.NotificationListener;

@Singleton
@Startup
@Profiled
@Traceable
public class JobNotificationObserver implements NotificationListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private EventConsumerBean eventConsumerBean;

    @Inject
    private JobNotificationUtil jobNotificationUtil;

    @Inject
    private SystemRecorder systemRecorder;

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

    @Override
    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    protected EventConsumerBean getEventConsumerBean() {
        final EventConsumerBean eventConsumerBean = new EventConsumerBean(ShmCommonConstants.SHM_JOB_NOTIFICATION_CHANNEL_URI);
        return eventConsumerBean;
    }

    protected boolean startListeningForJobNotifications(final EventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(new ShmJobNotificationQueueListener(jobNotificationUtil));
    }

    protected String getFilter() {
        return ShmCommonConstants.SHM_JOB_NOTIFICATION_FILTER;
    }
}