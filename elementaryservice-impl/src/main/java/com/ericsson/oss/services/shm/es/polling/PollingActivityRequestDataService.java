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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadRequest;
import com.ericsson.oss.services.shm.model.event.based.mediation.PollCycleStatus;

/**
 * This method gets all Polling Entries from DPS and place them in the Request Queue.
 * 
 */
@Stateless
public class PollingActivityRequestDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingActivityRequestDataService.class);

    @Inject
    @Modeled
    private EventSender<MOReadRequest> eventSender;

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    private PollingActivityUtil pollingActivityUtil;

    public void startPolling() {
        final List<Map<String,Object>> persistenceObjectList = pollingActivityManager.getPollingActivityPOs();
        String pollCycleStatus = PollCycleStatus.READY.name();
        try {
            for (final Map<String,Object> attributes : persistenceObjectList) {
                final long pollingActivityPoId = (long)attributes.get(ShmConstants.PO_ID);
                LOGGER.debug("Entered in PollingActivityRequestDataService.startPolling with polling Po ID {}", pollingActivityPoId);
                final boolean isActivityTimeoutElapsed = pollingActivityUtil.isActivityTimeoutElapsed(attributes);
                if (!isActivityTimeoutElapsed) {
                    final MOReadRequest shmReadRequest = populateShmReadRequest(pollingActivityPoId, attributes);

                    if (attributes.get(PollingActivityConstants.POLL_CYCLE_STATUS) != null) {
                        pollCycleStatus = (String) attributes.get(PollingActivityConstants.POLL_CYCLE_STATUS);
                    }
                    if (PollCycleStatus.READY.toString().equalsIgnoreCase(pollCycleStatus)) {
                        shmReadRequest.setPollCycleStatus(com.ericsson.oss.services.shm.model.event.based.mediation.PollCycleStatus.READY);
                    } else if (PollCycleStatus.IN_PROGRESS.toString().equalsIgnoreCase(pollCycleStatus)) {
                        shmReadRequest.setPollCycleStatus(com.ericsson.oss.services.shm.model.event.based.mediation.PollCycleStatus.IN_PROGRESS);
                    } else if (PollCycleStatus.COMPLETED.toString().equalsIgnoreCase(pollCycleStatus)) {
                        shmReadRequest.setPollCycleStatus(com.ericsson.oss.services.shm.model.event.based.mediation.PollCycleStatus.COMPLETED);
                    }
                    eventSender.send(shmReadRequest);
                    LOGGER.debug("Successfully sent MOReadRequest to ShmPollEntriesRequestQueue having activityJobId: {}", shmReadRequest.getActivityJobId());
                } else {
                    LOGGER.info("Un-subscribing for polling for the activityJobId: {} as activity is timed-out", attributes.get(PollingActivityConstants.ACTIVITY_JOB_ID));
                    pollingActivityManager.unsubscribeByPOId(pollingActivityPoId);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception occured while preparing the request queue data and sending it to the request queue:", e);
        }

    }

    /**
     * @param pollingActivityPoId
     * @param attributes
     * @return
     */
    private MOReadRequest populateShmReadRequest(final long pollingActivityPoId, final Map<String, Object> attributes) {
        final MOReadRequest shmReadRequest = new MOReadRequest();
        long activityJobId = 0;
        long pollInitiatedTime = 0;
        long maxWaitTimeToRead = 0;
        String moFdn = null;
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        String mimVersion = null;
        String namespace = null;
        List<String> moAttributes = new ArrayList<String>();

        if (attributes.get(PollingActivityConstants.ACTIVITY_JOB_ID) != null) {
            activityJobId = (long) attributes.get(PollingActivityConstants.ACTIVITY_JOB_ID);
        }
        if (attributes.get(PollingActivityConstants.MO_ATTRIBUTES) != null) {
            moAttributes = (List<String>) attributes.get(PollingActivityConstants.MO_ATTRIBUTES);
        }
        if (attributes.get(PollingActivityConstants.POLL_INITIATED_TIME) != null) {
            pollInitiatedTime = (long) attributes.get(PollingActivityConstants.POLL_INITIATED_TIME);
        }
        if (attributes.get(PollingActivityConstants.MAX_WAIT_TIME_TO_READ) != null) {
            maxWaitTimeToRead = (long) attributes.get(PollingActivityConstants.MAX_WAIT_TIME_TO_READ);
        }
        if (attributes.get(PollingActivityConstants.MO_FDN) != null) {
            moFdn = (String) attributes.get(PollingActivityConstants.MO_FDN);
        }
        if (attributes.get(PollingActivityConstants.ADDITIONAL_INFORMATION) != null) {
            additionalInformation.putAll((Map<String, String>) attributes.get(PollingActivityConstants.ADDITIONAL_INFORMATION));
        }

        additionalInformation.put(ShmConstants.PO_ID, String.valueOf(pollingActivityPoId));

        if (attributes.get(PollingActivityConstants.MIM_VERSION) != null) {
            mimVersion = (String) attributes.get(PollingActivityConstants.MIM_VERSION);
        }
        if (attributes.get(PollingActivityConstants.NAMESPACE) != null) {
            namespace = (String) attributes.get(PollingActivityConstants.NAMESPACE);
        }

        shmReadRequest.setActivityJobId(activityJobId);
        shmReadRequest.setAdditionalInformation(additionalInformation);
        shmReadRequest.setMaxWaitTimeToRead(maxWaitTimeToRead);
        shmReadRequest.setPollInitiatedTime(pollInitiatedTime);
        shmReadRequest.setMimVersion(mimVersion);
        shmReadRequest.setMoAttributes(moAttributes);
        shmReadRequest.setMoFdn(moFdn);
        shmReadRequest.setNamespace(namespace);
        return shmReadRequest;
    }

}
