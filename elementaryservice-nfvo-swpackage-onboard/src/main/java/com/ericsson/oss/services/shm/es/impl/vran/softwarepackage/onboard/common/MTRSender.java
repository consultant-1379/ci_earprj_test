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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackageCreateOnBoardJobRequest;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackagePollJobRequest;

/**
 * 
 * This class triggers Mediation task events to perform Software package onboard job
 * 
 * @author xjhosye
 */
public class MTRSender {

    @Inject
    @Modeled
    private EventSender<NfvoSwPackageCreateOnBoardJobRequest> eventSender;

    @Inject
    private OnboardSoftwarePackageStatusCheckScheduler onboardSoftwarePackageStatusCheckScheduler;

    private static final Logger LOGGER = LoggerFactory.getLogger(MTRSender.class);

    /**
     * Method to send mediation task request for software package onboard actions.
     */
    public void sendOnboardSoftwarePackageRequest(final String nodeAddress, final String fullFolderPath,final Map<String,Object> eventAttributes) {

        LOGGER.debug("Building mediation task request to invoke onboard software package action with attributes nodeAddress: {} vnfPackageId :{} eventAttributes : {}", nodeAddress, fullFolderPath, eventAttributes);

        final NfvoSwPackageCreateOnBoardJobRequest onboardActionRequest = new NfvoSwPackageCreateOnBoardJobRequest();
        onboardActionRequest.setNodeAddress(nodeAddress);
        onboardActionRequest.setfullFolderPath(fullFolderPath);
        onboardActionRequest.setEventAttributes(eventAttributes);

        eventSender.send(onboardActionRequest);
        LOGGER.debug("Onboard software package action request has been sent successfully. Event : {}", onboardActionRequest);
    }

    /**
     * Method to send mediation task request to get the current running software package onboard job status
     */
    public void sendJobStatusRequest(final String nodeAddress, final String vnfPackageId, final String jobId, final Map<String, Object> eventAttributes) {

        LOGGER.debug("Building mediation task request to invoke software package onboard job status request with attributes :nodeAddress : {} vnfPackageId : {} jobId : {} eventAttributes: {}", nodeAddress, vnfPackageId,
                jobId,eventAttributes);
        final NfvoSwPackagePollJobRequest jobStatusRequest = new NfvoSwPackagePollJobRequest();
        jobStatusRequest.setNodeAddress(nodeAddress);
        jobStatusRequest.setNfvoJobId(jobId);
        jobStatusRequest.setVnfPackageId(vnfPackageId);
        jobStatusRequest.setEventAttributes(eventAttributes);
        onboardSoftwarePackageStatusCheckScheduler.scheduleSingleNfvoSwPackagePollJobRequest(jobStatusRequest);
    }

}