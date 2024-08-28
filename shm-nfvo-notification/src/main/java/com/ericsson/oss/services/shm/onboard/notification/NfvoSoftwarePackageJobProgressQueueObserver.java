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
package com.ericsson.oss.services.shm.onboard.notification;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.mediation.vran.model.response.NfvoSwPackageMediationResponse;

/**
 * This class listens for onboard software package Job progress Notifications
 *
 * @author xjhosye
 */
public class NfvoSoftwarePackageJobProgressQueueObserver implements EMessageListener<NfvoSwPackageMediationResponse> {

    @Inject
    private NfvoSoftwarePackageJobProgressHandler onBoardJobProgressHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(NfvoSoftwarePackageJobProgressQueueObserver.class);

    /**
     * Method to listen to the mediation response of NfvoSwPackageMediationResponse type from shm notification queue and sends it to corresponding handler for further processing
     *
     * @param nfvoSwPackageMediationResponse
     */
    @Override
    public void onMessage(final NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse) {
        LOGGER.debug("Nfvo software package response received from mediation:  {}", nfvoSwPackageMediationResponse);
        final Date notificationReceivedDate = new Date();
        onBoardJobProgressHandler.handleJobProgressResponse(nfvoSwPackageMediationResponse, notificationReceivedDate);
    }
}
