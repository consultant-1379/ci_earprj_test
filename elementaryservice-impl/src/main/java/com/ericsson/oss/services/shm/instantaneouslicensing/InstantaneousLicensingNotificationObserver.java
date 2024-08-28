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
package com.ericsson.oss.services.shm.instantaneouslicensing;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.NotificationListener;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

/**
 * A On startup class which start InstantaneousLicensing MO's AVC listener and it will subscribe the listener for given notification channel using required filter
 * 
 * @author Team Royals
 *
 */
@Singleton
@Startup
public class InstantaneousLicensingNotificationObserver implements NotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstantaneousLicensingNotificationObserver.class);

    private EventConsumerBean eventConsumerBean;

    @Inject
    private InstantaneousLicensingMoMtrSender instantaneousLicensingMoMtrSender;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationReciever notificationReciever;

    @PostConstruct
    public void subscribeForDpsEvents() {
        eventConsumerBean = new EventConsumerBean(ShmCommonConstants.SHM_NOTIFICATION_CHANNEL_URI);
        eventConsumerBean.setFilter(InstantaneousLicensingMOConstants.Filters.INSTANTANEOUS_LICENSING.getFilter());
        final boolean isListening = eventConsumerBean.startListening(new InstantaneousLicensingQueueListener(instantaneousLicensingMoMtrSender,notificationReciever));
        LOGGER.info("is InstantaneousLicensing Queue Listener Started: {}", isListening);
    }

    @Override
    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

}
