/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.loadcontrol.local.api.StagedActivityRequestBean;

/**
 * Its responsibility is to add a request into load controller queue.
 * 
 * @author tcsgusw
 * 
 */
public class LoadControlQueueProducer {

    @Inject
    private ChannelLocator channelLocator;

    private final static Logger LOGGER = LoggerFactory.getLogger(LoadControlQueueProducer.class);

    /**
     * Method to keep the staged activities in load controller queue.
     * 
     * @param activityRequest
     */
    public void keepStagedActivitiesInQueue(final StagedActivityRequestBean stagedActivityRequest) {
        try {
            final String channelURI = ShmCommonConstants.SHM_STAGED_ACTIVITY_CHANNEL_URI;
            final Channel channel = channelLocator.lookupChannel(channelURI);
            if (channel == null) {
                LOGGER.error("Request : {} was not send on channel {} ", stagedActivityRequest, channelURI);
                return;
            }
            channel.send(stagedActivityRequest);
            LOGGER.debug("{} Request was send to channel Successfully {}", stagedActivityRequest, channel);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while seding stagedActivityRequest to SHMStagedActivity Queue for the activity:{}. Exception is:", ex);
        }
    }

}
