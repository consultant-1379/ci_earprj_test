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
package com.ericsson.oss.services.shm.es.activity.callback;

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
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionResponse;

/**
 * Listener for MoActionResponse from event based mediation client .
 * 
 * @author zdonkri
 */

@Singleton
@Startup
public class MOActionResponseQueueListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOActionResponseQueueListener.class);

    private ModeledEventConsumerBean modeledeventConsumerBean;

    @Inject
    private MOActionResponseQueueObserver moActionResponseQueueObserver;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    protected void startListener() {
        final long postConstructStarted = System.currentTimeMillis();
        LOGGER.debug("Starting listener to listen MO Action triggered from event based mediation client");
        modeledeventConsumerBean = getModeledEventConsumerBean();
        final boolean isListening = startListeningForMoActionResponses(modeledeventConsumerBean);
        LOGGER.debug("Subscribed to MOActionResponseQueue for receiving MO Action triggered from event based mediation client , is listener started: {} ", isListening);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    public boolean stopListeningToMoactionResponses() {
        return modeledeventConsumerBean.stopListening();
    }

    protected ModeledEventConsumerBean getModeledEventConsumerBean() {
        return new ModeledEventConsumerBean.Builder(MOActionResponse.class).filter("MessageType = 'MediationClientMOActionResponse'").build();
    }

    protected boolean startListeningForMoActionResponses(final ModeledEventConsumerBean modeledeventConsumerBean) {
        LOGGER.debug("Listening for the MO Action responses {}", modeledeventConsumerBean);
        return modeledeventConsumerBean.startListening(moActionResponseQueueObserver);
    }

}
