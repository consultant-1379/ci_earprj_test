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
package com.ericsson.oss.services.shm.vran.software.upgrade.notification;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.mediation.vran.model.response.VranUpgradeJobResponse;

/**
 * Listens for vRAN Software Upgrade Job progress Notifications
 *
 * @author xchakoy
 */
public class VranSoftwareUpgradeJobProgressQueueObserver implements EMessageListener<VranUpgradeJobResponse> {

    @Inject
    private VranSoftwareUpgradeJobProgressHandler vranJobProgressHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(VranSoftwareUpgradeJobProgressQueueObserver.class);

    /**
     * Listens to the mediation response of VranUpgradeJobResponse type from shm notification queue and delegates it to corresponding handler for further processing.
     *
     * @param VranSoftwareUpgradeJobResponse
     *            vranUpgradeJobResponse
     */
    @Override
    public void onMessage(final VranUpgradeJobResponse vranUpgradeJobResponse) {
        LOGGER.debug("VRAN software upgrade job mediation received is:  {}", vranUpgradeJobResponse);
        final Date notificationReceivedDate = new Date();
        vranJobProgressHandler.handleJobProgressResponse(vranUpgradeJobResponse, notificationReceivedDate);
    }
}
