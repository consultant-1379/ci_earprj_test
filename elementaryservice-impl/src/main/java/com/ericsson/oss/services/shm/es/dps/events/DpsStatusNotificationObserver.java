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
package com.ericsson.oss.services.shm.es.dps.events;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.DpsNotificationConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

/**
 * 
 * This class will start the notification listener at the time of application startup.
 * 
 * @author xprapav
 * 
 */
@Singleton
@Startup
public class DpsStatusNotificationObserver {

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsStatusInfoProvider.class);

    private static final String CONTAINER_NAME_SHMCORESERV = System.getProperty("instance-name");

    private EventConsumerBean eventConsumerBean;

    @PostConstruct
    public void listenForDpsNotifications() {
        final long postConstructStarted = System.currentTimeMillis();
        eventConsumerBean = new EventConsumerBean(DpsNotificationConfiguration.DPS_EVENT_NOTIFICATION_CHANNEL_URI);
        eventConsumerBean.setFilter(String.format(ShmCommonConstants.DPS_CONNECTION_EVENT_FILTER, CONTAINER_NAME_SHMCORESERV));
        LOGGER.info("In DpsStatusNotificationObserver: Starting DPS event notification listener.... ");
        final boolean isListening = eventConsumerBean.startListening(new DpsStatusNotificationListener(dpsStatusInfoProvider, systemRecorder));
        if (isListening) {
            LOGGER.info("In DpsStatusNotificationObserver: DpsConnectionEvent notification listener started successfully on Container:{}.", CONTAINER_NAME_SHMCORESERV);
        } else {
            LOGGER.error("In DpsStatusNotificationObserver: DpsConnectionEvent notification listener not started.");
        }
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @PreDestroy
    public void stopNotificationListener() {
        eventConsumerBean.stopListening();
    }
}
