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
package com.ericsson.oss.services.shm.es.polling;

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
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadRequest;

@Singleton
@Startup
@Profiled
@Traceable
public class SHMPollEntriesRequestQueueDataListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMPollEntriesRequestQueueDataListener.class);

    private ModeledEventConsumerBean eventConsumerBean;

    @Inject
    private SHMPollEntriesRequestQueueDataObserver shmPollEntriesRequestObserver;

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
        final boolean isListening = startListeningForRequestQueueNotifications(eventConsumerBean);
        LOGGER.debug("subscribing to Queue for receiving Polling activity request data, and started listening:: {} ", isListening);

    }

    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    protected ModeledEventConsumerBean getEventConsumerBean() {
        final ModeledEventConsumerBean eventConsumerBean = new ModeledEventConsumerBean.Builder(MOReadRequest.class).build();
        return eventConsumerBean;
    }

    protected boolean startListeningForRequestQueueNotifications(final ModeledEventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(shmPollEntriesRequestObserver);
    }

}
