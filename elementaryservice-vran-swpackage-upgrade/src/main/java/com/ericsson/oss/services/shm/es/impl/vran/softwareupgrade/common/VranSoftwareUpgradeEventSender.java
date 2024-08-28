/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.VranUpgradeJobMediationTaskRequest;
import com.ericsson.oss.mediation.vran.model.request.VranUpgradeJobStatusRequest;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service.common.SoftwareUpgradeStatusCheckScheduler;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;

/**
 *
 * Service to send mediation task request to perform VRAN Software upgrade job
 *
 * @author xindkag
 */
public class VranSoftwareUpgradeEventSender {

    @Inject
    @Modeled
    private EventSender<VranUpgradeJobMediationTaskRequest> actionInvocationRequestSender;

    @Inject
    private SoftwareUpgradeStatusCheckScheduler softwareUpgradeStatusCheckScheduler;

    @Inject
    private ActivityUtils activityUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(VranSoftwareUpgradeEventSender.class);

    public void sendSoftwareUpgradeActionRequest(final long activityJobId, final String activityName, final String nodeAddress, final Map<String, Object> eventAttributes) {

        LOGGER.debug("Building mediation task request to invoke {} action with attributes : {} for node {}", activityName, eventAttributes, nodeAddress);

        final VranUpgradeJobMediationTaskRequest activityInitiateRequest = new VranUpgradeJobMediationTaskRequest();
        activityInitiateRequest.setNodeAddress(nodeAddress);
        activityInitiateRequest.setActivityName(activityName);
        activityInitiateRequest.setEventAttributes(eventAttributes);

        actionInvocationRequestSender.send(activityInitiateRequest);

        activityUtils.recordEvent(VranJobEvents.PREPARE_PROCESS_NOTIFICATION, nodeAddress, activityName,
                activityUtils.additionalInfoForEvent(activityJobId, nodeAddress, activityInitiateRequest.toString()));

        LOGGER.debug("Mediation task request has been sent successfully. Event : {}", activityInitiateRequest);
    }

    public void sendUpgradeJobStatusRequest(final String activityName, final int vnfJobId, final String nodeAddress, final Map<String, Object> eventAttributes) {

        LOGGER.debug("Building mediation task request to invoke {} status request with attributes : {} for node {}  ", activityName, eventAttributes, nodeAddress);

        final VranUpgradeJobStatusRequest jobStatusRequest = new VranUpgradeJobStatusRequest();
        jobStatusRequest.setNodeAddress(nodeAddress);
        jobStatusRequest.setActivityName(activityName);
        jobStatusRequest.setVnfJobId(vnfJobId);
        jobStatusRequest.setEventAttributes(eventAttributes);

        softwareUpgradeStatusCheckScheduler.schedule(jobStatusRequest);

        LOGGER.debug("Mediation task has been scheduled successfully . Event {} ", jobStatusRequest);

    }

}
