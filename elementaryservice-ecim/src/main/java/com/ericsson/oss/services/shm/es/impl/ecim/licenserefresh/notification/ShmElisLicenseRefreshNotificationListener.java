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

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.ACTIVITY_NAME_REQUEST;
import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.LICENSE_REFRESH_JOB_TYPE;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.classic.ModeledEventConsumerBean;
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification;

@Singleton
@Startup
public class ShmElisLicenseRefreshNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmElisLicenseRefreshNotificationListener.class);

    @Inject
    private ShmElisLicenseRefreshNotificationObserver shmElisLicenseRefreshNotificationObserver;

    private ModeledEventConsumerBean modeledeventConsumerBean;

    @PostConstruct
    public void startListener() {
        LOGGER.info("Starting listener to listen ShmElisLicenseRefreshNotification sent from mediation");
        modeledeventConsumerBean = getModeledEventConsumerBean();
        final boolean isListening = startListeningForJobNotifications(modeledeventConsumerBean);
        LOGGER.info("Subscribed to Queue for receiving ShmElisLicenseRefreshNotification , is listener started: {} ", isListening);
    }

    public boolean stopNotificationListener() {
        return modeledeventConsumerBean.stopListening();
    }

    private ModeledEventConsumerBean getModeledEventConsumerBean() {
        return new ModeledEventConsumerBean.Builder(ShmElisLicenseRefreshNotification.class)
                .filter("(jobType = '" + LICENSE_REFRESH_JOB_TYPE + "' AND activityName = '" + ACTIVITY_NAME_REQUEST + "')").build();
    }

    private boolean startListeningForJobNotifications(final ModeledEventConsumerBean modeledeventConsumerBean) {
        LOGGER.debug("Listening for the ShmElisLicenseRefreshNotification {}", modeledeventConsumerBean);
        return modeledeventConsumerBean.startListening(shmElisLicenseRefreshNotificationObserver);
    }
}
