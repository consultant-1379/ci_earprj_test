package com.ericsson.oss.services.shm.loadcontrol.monitor;

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
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.loadcontrol.impl.ActivityLoadControlManager;
import com.ericsson.oss.services.shm.notifications.NotificationListener;

@Singleton
@Startup
public class ShmStagedActivityQueueListener implements NotificationListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private EventConsumerBean eventConsumerBean;

    @Inject
    private ActivityLoadControlManager activityLoadControlManager;

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
     * To start listening for ShmStagedActivitiyQueue
     */
    private void startListener() {
        eventConsumerBean = getEventConsumerBean();
        final boolean isListening = startListeningForStagedActivities(eventConsumerBean);
        logger.debug("subscribing to ShmStagedActivityQueue for receiving activity load Notifications, and started listening:: {} ", isListening);

    }

    @Override
    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    protected EventConsumerBean getEventConsumerBean() {
        return new EventConsumerBean(ShmCommonConstants.SHM_STAGED_ACTIVITY_CHANNEL_URI);
    }

    protected boolean startListeningForStagedActivities(final EventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(new ShmStagedActivityQueueObserver(activityLoadControlManager));
    }
}