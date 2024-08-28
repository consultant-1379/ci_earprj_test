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
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadResponse;

/**
 * This listener listens to the ClusteredCallbackNotificationQueue and delegates the data to the Observer.
 * 
 * @author xsrabop
 */
@Singleton
@Startup
public class ClusteredCallbackNotificationQueueListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredCallbackNotificationQueueListener.class);

    @Inject
    private ClusteredCallbackNotificationQueueObserver clusteredCallbackNotificationQueueObserver;

    private ModeledEventConsumerBean modeledeventConsumerBean;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    protected void startListener() {
        final long postConstructStarted = System.currentTimeMillis();
        LOGGER.debug("Starting listener to listen MO attributes sent from event based mediation client");
        modeledeventConsumerBean = getModeledEventConsumerBean();
        final boolean isListening = startListeningForPollingResponses(modeledeventConsumerBean);
        LOGGER.debug("Subscribed to ClusteredCallbackNotificationQueue for receiving MO attributes sent from event based mediation client , is listener started: {} ", isListening);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    public boolean stopListeningToPollingResponses() {
        return modeledeventConsumerBean.stopListening();
    }

    protected ModeledEventConsumerBean getModeledEventConsumerBean() {
        return new ModeledEventConsumerBean.Builder(MOReadResponse.class).filter("MessageType = 'MediationClientMOReadResponse'").build();
    }

    protected boolean startListeningForPollingResponses(final ModeledEventConsumerBean modeledeventConsumerBean) {
        LOGGER.debug("Listening for the polling responses {}", modeledeventConsumerBean);
        return modeledeventConsumerBean.startListening(clusteredCallbackNotificationQueueObserver);
    }
}
