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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadRequest;

/**
 * This Class gets the data from the SHMPollEntriesRequest Queue and place it in Mediation Queue
 * 
 */

public class SHMPollEntriesRequestQueueDataObserver implements EMessageListener<MOReadRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMPollEntriesRequestQueueDataObserver.class);

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    PollingCallBackResolver pollingCallBackResolver;

    @Override
    public void onMessage(final MOReadRequest requestEvent) {
        if (requestEvent == null) {
            LOGGER.warn("Discarding the event as it is null");
        } else if (PlatformTypeEnum.AXE.getName().equals(requestEvent.getAdditionalInformation().get(ShmConstants.PLATFORM))) {
            final PollingCallBack pollingCallBackImpl = getPollingCallBackService(requestEvent.getAdditionalInformation().get(ShmConstants.ACTIVITYNAME),
                    JobTypeEnum.valueOf(requestEvent.getAdditionalInformation().get(ShmConstants.JOB_TYPE)),
                    PlatformTypeEnum.valueOf(requestEvent.getAdditionalInformation().get(ShmConstants.PLATFORM)));
            pollingCallBackImpl.processPollingResponse(requestEvent.getActivityJobId(), null);

        } else {
            pollingActivityManager.processMOReadRequest(requestEvent);
        }

    }

    protected PollingCallBack getPollingCallBackService(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        return pollingCallBackResolver.getPollingCallBackService(platform, jobType, activityName);
    }

}
