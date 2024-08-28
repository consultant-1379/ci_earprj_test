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

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.moaction.cache.MoActionCacheProvider;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmEcimMOActionMediationTaskRequest;

/**
 * This class is used to create and manage the MTR for Ecim MO Actions
 * 
 * @author xpavdeb
 */

@Stateless
public class MoActionMTRManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoActionMTRManager.class);

    @Inject
    @Modeled
    private EventSender<ShmEcimMOActionMediationTaskRequest> eventSender;

    @Inject
    private MoActionUtil moActionUtil;

    @Inject
    private MoActionCacheProvider moActionCacheProvider;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    /**
     * Prepares the MTR for MO Action and sends the request to Queue
     * 
     * @param nodeName
     * @param actionName
     * @param moFdn
     * @param moName
     * @param uploadActionArguments
     * @param jobActivityInfo
     * @throws MoNotFoundException
     */
    public void prepareAndSendMTRForMoAction(final String nodeName, final String parentMoFdn, final String moName, final String actionName, final Map<String, Object> uploadActionArguments,
            final JobActivityInfo jobActivityInfo) throws MoNotFoundException {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final ShmEBMCMoActionData ebmcMoActionData = moActionCacheProvider.get(activityJobId);

        ShmEcimMOActionMediationTaskRequest moActionMTRRequest = null;
        if (ebmcMoActionData != null) {
            final int retryCountInCache = (Integer) ebmcMoActionData.getAdditionalInformation().get(ShmConstants.RETRY_COUNT) + 1;
            moActionMTRRequest = moActionUtil.prepareMTRAttributes(ebmcMoActionData, retryCountInCache);
            updateMoActionCache(activityJobId, jobActivityInfo.getPlatform().toString(), ebmcMoActionData);
        } else {
            moActionMTRRequest = moActionUtil.prepareMTRAttributes(nodeName, parentMoFdn, moName, actionName, uploadActionArguments, jobActivityInfo);
            addMoActionMTRInCache(activityJobId, moActionMTRRequest, jobActivityInfo);
        }
        eventSender.send(moActionMTRRequest);
        LOGGER.debug("Successfully submitted the ShmEcimMOActionMediationTaskRequest for node {} and  activityJobId: {}.", nodeName, activityJobId);
    }

    public void updateMoActionCache(final long activityJobId, final String platform, final ShmEBMCMoActionData ebmcMoActionData) {

        final Map<String, Object> additionalInformation = ebmcMoActionData.getAdditionalInformation();
        ebmcMoActionData.setAdditionalInformation(additionalInformation);
        final long operationTimeOut = pollingActivityConfiguration.getOperationTimeOutBasedOnPlatformType(platform);
        ebmcMoActionData.setMaxWaitTime(System.currentTimeMillis() + operationTimeOut);
        moActionCacheProvider.add(activityJobId, ebmcMoActionData);
    }

    private void addMoActionMTRInCache(final long activityJobId, final ShmEcimMOActionMediationTaskRequest moActionMTRRequest, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("In Method addMoActionMTRInCache with activityJobId {}", activityJobId);
        if (moActionMTRRequest != null) {
            final ShmEBMCMoActionData shmEcimMoActionData = moActionUtil.retrieveMoActionDataAndAddToCache(activityJobId, moActionMTRRequest, jobActivityInfo.getPlatform().toString());
            moActionCacheProvider.add(activityJobId, shmEcimMoActionData);
        } else {
            LOGGER.error("Failed to place action MTR into cache as received invalid data for the activityJobId:{}", activityJobId);
        }
    }

    public void removeMoActionMTRFromCache(final long activityJobId) {
        moActionCacheProvider.remove(activityJobId);
        LOGGER.debug("Removed MoActionMTR from Cache with activityjobId: {}", activityJobId);
    }

    public ShmEBMCMoActionData getMoActionMTRFromCache(final long activityJobId) {
        return moActionCacheProvider.get(activityJobId);
    }

}
