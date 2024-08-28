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
package com.ericsson.oss.services.shm.es.vran.common;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackageListRequest;

/**
 * This class triggers Mediation task request to synchronize NFVO VNF Software package details
 *
 * @author ztharan
 */
public class NfvoVnfPackageSyncMTRSender {

    @Inject
    @Modeled
    private EventSender<NfvoSwPackageListRequest> nfvoVnfPackagesRequestSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(NfvoVnfPackageSyncMTRSender.class);

    /**
     * Method to send mediation task request to synchronize NFVO VNF Software package details
     */
    public void sendNfvoVnfPackagesSyncRequest(final String nfvoAddress) {
        final NfvoSwPackageListRequest nfvoSwPackagesListRequest = new NfvoSwPackageListRequest();
        nfvoSwPackagesListRequest.setNodeAddress(nfvoAddress);
        nfvoVnfPackagesRequestSender.send(nfvoSwPackagesListRequest);
        LOGGER.info("NfvoPackageRequest has been sent successfully. Event : {}", nfvoSwPackagesListRequest);
    }
}
