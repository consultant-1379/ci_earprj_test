package com.ericsson.oss.services.shm.generic.notification;

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
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

import com.ericsson.oss.itpf.sdk.eventbus.model.classic.ModeledEventConsumerBean;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;

/**
 * Service to register the observer to observe the SHMCommonCallBackNotification.
 * 
 */
@Singleton
@Startup
public class SHMCommonNotificationRegister {

    private static final String STN = "STN";
    private static final String STN_UPGRADE_JOB_PROGRESS_NOTIFICATION_FILTER = "(platformType = '" + STN + "')";

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMCommonNotificationRegister.class);

    @Inject
    private SHMCommonCallbackNotificationQueueObserver nonDpsJobProgressQueueObserver;

    private ModeledEventConsumerBean modeledeventConsumerBean;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Subscribes for SHMCommonCallBackNotification events and listens for notifications on a Queue
     */
    @PostConstruct
    protected void startListener() {
        final long postConstructStarted = System.currentTimeMillis();
        LOGGER.debug("Starting listener to listen SHMCommonCallBackNotifications sent from mediation");
        modeledeventConsumerBean = buildModeledEventConsumerBean();
        final boolean isListening = startListeningForJobNotifications(modeledeventConsumerBean);
        LOGGER.debug("Subscribing to queue for receiving SHMCommonCallBackNotifications, and is listener started: {} ", isListening);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /**
     * Stops listening for Software Upgrade job notifications on a Queue (<code>jms:/queue/shmNotificationQueue</code>)
     * 
     * @return - boolean
     */
    public boolean stopNotificationListener() {
        return modeledeventConsumerBean.stopListening();
    }

    /**
     * Building modeled event consumer bean of type SHMCommonCallBackNotification
     * 
     * @return - ModeledEventConsumerBean
     */
    protected ModeledEventConsumerBean buildModeledEventConsumerBean() {
        return new ModeledEventConsumerBean.Builder(SHMCommonCallbackNotification.class).filter(STN_UPGRADE_JOB_PROGRESS_NOTIFICATION_FILTER).build();
    }

    /**
     * To start listening for job Notifications using modeled event consumer bean
     */
    protected boolean startListeningForJobNotifications(final ModeledEventConsumerBean modeledeventConsumerBean) {
        LOGGER.debug("Listening for the SHMCommonCallBackNotifications {}", modeledeventConsumerBean);
        return modeledeventConsumerBean.startListening(nonDpsJobProgressQueueObserver);
    }
}
