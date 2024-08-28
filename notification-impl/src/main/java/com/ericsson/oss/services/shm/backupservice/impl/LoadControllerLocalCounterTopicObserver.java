/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.backupservice.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.loadcontrol.local.api.LoadControllerLocalCache;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * Listens for Activity Completion message to decrease the local Counter of Load Controller.
 * 
 * @author tcsvnag
 */
@ApplicationScoped
@Profiled
@Traceable
public class LoadControllerLocalCounterTopicObserver {

    private static final Logger logger = LoggerFactory.getLogger(LoadControllerLocalCounterTopicObserver.class);

    @Inject
    private LoadControllerLocalCache loadControllerLocalCache;

    @Inject
    private SHMLoadControllerLocalService loadControllerLocalService;

    /**
     * Listens on a jms topic : shmLoadControllerLocalCounterTopic.
     */
    public void onMessage(@Observes @Consumes(endpoint = ShmCommonConstants.SHM_LOAD_CONTROLLER_LOCALCOUNTER_NOTIFICATION_CHANNEL_URI) final Event message) {
        final SHMLoadControllerCounterRequest shmLoadControllerCounterRequest = (SHMLoadControllerCounterRequest) message.getPayload();
        logger.debug("Observer ==> Received request [{}] and CurrentActivityLoadCount", shmLoadControllerCounterRequest);
        if (ActivityConstants.ACTIVITY_EXECUTION_STARTED.equals(shmLoadControllerCounterRequest.getActivityStatus())) {
            loadControllerLocalService.incrementGlobalCounter(shmLoadControllerCounterRequest);
        } else if (ActivityConstants.ACTIVITY_EXECUTION_COMPLETED.equals(shmLoadControllerCounterRequest.getActivityStatus())) {
            loadControllerLocalCache.removeActivityJobIdFromCacheAndDecrementLocalCounter((SHMLoadControllerCounterRequest) message.getPayload());
            loadControllerLocalService.decrementGlobalCounter(shmLoadControllerCounterRequest);
        }
        logger.debug("Observer ==> updated GlobalCount in ApplicationScoped Bean for {}", shmLoadControllerCounterRequest);
    }
}