/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.shm.model.request.StnUpgradeJobMediationTaskRequest;
/**
 * 
 * Service to send mediation task request to perform STN Software upgrade_install job
 * 
 * @author xsamven
 */
public class StnSoftwareUpgradeInstallEventSender {
    
    private final Logger logger = LoggerFactory.getLogger(StnSoftwareEndUpgradeEventSender.class);
    
    @Inject
    @Modeled
    private EventSender<StnUpgradeJobMediationTaskRequest> actionInvocationRequestSender;

    /**
     * Method to send mediation task request for software upgrade actions on the STN node
     * 
     * @param activityJobId
     * @param packageName
     * @param nodeAddress
     * @param eventAttributes
     */
    public void sendSoftwareUpgradeActionRequest(final long activityJobId, final String packageName, final String nodeAddress, final Map<String, Object> eventAttributes) {

    	logger.debug("Building mediation task request to invoke {} action with attributes : {} ", packageName, eventAttributes);

    	final StnUpgradeJobMediationTaskRequest activityInitiateRequest = new StnUpgradeJobMediationTaskRequest();
    	activityInitiateRequest.setNodeAddress(nodeAddress);
    	activityInitiateRequest.setActivityName(packageName);
    	activityInitiateRequest.setEventAttributes(eventAttributes);
    	actionInvocationRequestSender.send(activityInitiateRequest);
    	logger.debug("Mediation task request has been sent successfully. Event : {}", activityInitiateRequest);
    }

 
}
