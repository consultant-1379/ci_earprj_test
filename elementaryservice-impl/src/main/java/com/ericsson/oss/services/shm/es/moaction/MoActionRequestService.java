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

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.moaction.cache.MoActionCacheProvider;
import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionRequest;

/**
 * This class gets all MoAction attributes from cache and place them in the Request Queue for load balancing.
 * 
 */
@Stateless
public class MoActionRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoActionRequestService.class);

    @Inject
    @Modeled
    private EventSender<MOActionRequest> eventSender;

    @Inject
    private MoActionCacheProvider moActionCacheProvider;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    public void processMoActionRequests() {
        final MOActionRequest requestEvent = new MOActionRequest();
        final List<ShmEBMCMoActionData> moActionData = moActionCacheProvider.getAll();
        try {
            if (dpsStatusInfoProvider.isDatabaseAvailable()) {
                for (ShmEBMCMoActionData shmEBMCMoActionData : moActionData) {
                    final long currentTime = System.currentTimeMillis();
                    if (currentTime >= shmEBMCMoActionData.getMaxWaitTime()) {
                        LOGGER.debug("Going to retry mo action as no response received within max wait time");
                        requestEvent.setActivityJobId(shmEBMCMoActionData.getActivityJobId());
                        requestEvent.setActionName(shmEBMCMoActionData.getActionName());
                        requestEvent.setMoFdn(shmEBMCMoActionData.getMoFdn());
                        requestEvent.setMoName(shmEBMCMoActionData.getMoName());
                        requestEvent.setMoActionAttributes(shmEBMCMoActionData.getMoActionAttributes());
                        requestEvent.setMimVersion(shmEBMCMoActionData.getMimVersion());
                        requestEvent.setNamespace(shmEBMCMoActionData.getNamespace());
                        requestEvent.setAdditionalInformation(shmEBMCMoActionData.getAdditionalInformation());
                        eventSender.send(requestEvent);
                    }
                }
            } else {
                LOGGER.debug("Cannot proceed for action initiation as database service is not availble.");
            }
        } catch (Exception e) {
            LOGGER.error("Exception occured while preparing the MO Action request data and sending it to the request queue :", e);
        }
    }

}
