/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification;

public class ShmElisLicenseRefreshNotificationObserver implements EMessageListener<ShmElisLicenseRefreshNotification> {

    @Inject
    private LkfImportNotificationProcessor lkfImportNotificationProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmElisLicenseRefreshNotificationObserver.class);

    @Override
    public void onMessage(final ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification) {
        LOGGER.debug("ShmElisLicenseRefreshNotification received from mediation:  {}", shmElisLicenseRefreshNotification);
        lkfImportNotificationProcessor.processElisNotification(shmElisLicenseRefreshNotification);
    }
}
