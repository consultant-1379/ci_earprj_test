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
package com.ericsson.oss.services.shm.loadcontrol.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.loadcontrol.local.api.LoadControllerLocalCache;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;

/**
 * Its responsibility is to add activity job Id into local cache when the activity job starts, send the request to load controller topic when the activity job ends and remove the activity job id from
 * local cache if exists .
 * 
 * @author tcsvnag
 * 
 */
@ApplicationScoped
public class LoadControllerLocalCacheImpl implements LoadControllerLocalCache {

    private final List<Long> activityJobIdsCache = Collections.synchronizedList(new ArrayList<Long>());

    @Inject
    private SHMLoadControllerLocalService loadControllerLocalService;

    @Inject
    private ChannelLocator channelLocator;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void addActivityJobIdToCache(final long activityJobId) {
        activityJobIdsCache.add(activityJobId);
    }

    public void removeActivityJobIdFromCacheAndDecrementLocalCounter(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest) {
        final long activityJobId = shmLoadControllerLocalCounterRequest.getActivityJobId();
        logger.debug("removing activityJobId : {} from cache on receiving shmActivityRequest {} from topic", activityJobId, shmLoadControllerLocalCounterRequest);
        if (activityJobIdsCache.contains(activityJobId)) {
            activityJobIdsCache.remove(activityJobId);
            final SHMActivityRequest shmActivityRequest = new SHMActivityRequest();
            shmActivityRequest.setActivityName(shmLoadControllerLocalCounterRequest.getActivityName());
            shmActivityRequest.setJobType(shmLoadControllerLocalCounterRequest.getJobType());
            shmActivityRequest.setPlatformType(shmLoadControllerLocalCounterRequest.getPlatformType());
            loadControllerLocalService.decrementCounter(shmActivityRequest);
            logger.info("removed activityJobId : {} from cache on receiving shmActivityRequest {} from topic", activityJobId, shmActivityRequest);
        }
    }

    public void keepMessageInTopic(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest) {
        try {
            final Channel channel = channelLocator.lookupChannel(ShmCommonConstants.SHM_LOAD_CONTROLLER_LOCALCOUNTER_NOTIFICATION_CHANNEL_URI);
            if (channel == null) {
                logger.error("Message : {} was not send on channel {} ", shmLoadControllerLocalCounterRequest, channel);
                return;
            }
            channel.send(shmLoadControllerLocalCounterRequest);
            logger.info("Request : {} was send to channel {} Successfully", shmLoadControllerLocalCounterRequest, channel);
        } catch (final Exception ex) {
            logger.error("Exception occurred while seding message to Load Controller topic for the activity:{}. Exception is:  ", shmLoadControllerLocalCounterRequest, ex);
        }
    }
}
