/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.moaction;

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
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionRequest;

/**
 * Listener for un-processed MoAction Requests .
 * 
 * @author tcssdas
 */

@Singleton
@Startup
public class MOActionRequestQueueListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOActionRequestQueueListener.class);

    private ModeledEventConsumerBean eventConsumerBean;

    @Inject
    private MOActionRequestQueueObserver moActionRequestQueueObserver;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * To start listening for un-processed action requests on a Queue(ShmPollEntriesRequestQueue)
     */
    @PostConstruct
    protected void startListener() {
        final long postConstructStarted = System.currentTimeMillis();
        eventConsumerBean = getEventConsumerBean();
        final boolean isListening = startListeningForRequestQueueNotifications(eventConsumerBean);
        LOGGER.debug("subscribing to queue for receiving un-processed action requests, and started listening:: {} ", isListening);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    protected ModeledEventConsumerBean getEventConsumerBean() {
        return new ModeledEventConsumerBean.Builder(MOActionRequest.class).filter("RequestType = 'MOActionRequest'").build();
    }

    protected boolean startListeningForRequestQueueNotifications(final ModeledEventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(moActionRequestQueueObserver);
    }

}
