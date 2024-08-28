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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackageCreateDeleteJobRequest;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackagePollJobRequest;

/**
 * 
 * This class triggers/sends Mediation task events to perform Software package delete job on Nfvo
 * 
 * @author xjhosye
 */
public class MTRSender {

    @Inject
    @Modeled
    private EventSender<NfvoSwPackageCreateDeleteJobRequest> eventSender;

    @Inject
    private DeleteJobStatusCheckScheduler deleteJobStatusCheckScheduler;

    private static final Logger LOGGER = LoggerFactory.getLogger(MTRSender.class);

    /**
     * Method to send mediation task request for software package delete actions.
     */
    public void sendDeleteSoftwarePackageRequest(final String nodeAddress, final String vnfPackageId) {

        LOGGER.debug("Building mediation task request to invoke delete software package action with attributes nodeAddress: {} vnfPackageId :{} ", nodeAddress, vnfPackageId);

        final NfvoSwPackageCreateDeleteJobRequest deleteActionRequest = new NfvoSwPackageCreateDeleteJobRequest();
        deleteActionRequest.setNodeAddress(nodeAddress);
        deleteActionRequest.setVnfPackageId(vnfPackageId);
        eventSender.send(deleteActionRequest);
        LOGGER.debug("Delete software package action request has been sent successfully. Event : {}", deleteActionRequest);
    }

    /**
     * Method to send mediation task request to get the current running software package delete job status
     */
    public void sendJobStatusRequest(final String nodeAddress, final String vnfPackageId, final String jobId) {

        LOGGER.debug("Building mediation task request to invoke software package delete job status request with attributes :nodeAddress : {} vnfPackageId : {} jobId : {}", nodeAddress, vnfPackageId,
                jobId);
        final NfvoSwPackagePollJobRequest jobStatusRequest = new NfvoSwPackagePollJobRequest();
        jobStatusRequest.setNodeAddress(nodeAddress);
        jobStatusRequest.setNfvoJobId(jobId);
        deleteJobStatusCheckScheduler.scheduleNfvoSwPackageSinglePollJobRequest(jobStatusRequest);
    }

}