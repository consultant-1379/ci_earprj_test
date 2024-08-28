/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.backupservice.impl;

import java.io.Serializable;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.DpsNotificationConfiguration;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

/**
 * Listens for AVC events from Dps during the cpp upgrade related MO actions.
 */
@ApplicationScoped
@Profiled
@Traceable
public class BackupNotificationTopicObserver {

    @Inject
    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
    private NotificationReciever notificationReciever;

    private static final Logger logger = LoggerFactory.getLogger(BackupNotificationTopicObserver.class);

    /**
     * Listens on a jms queue : shm-notification-queue. Filter on ConfigurationVersion MO or BrmBackup MO or BrmBackupManager MO.
     */
    public void onEvent(
            @Observes @Consumes(endpoint = DpsNotificationConfiguration.DPS_EVENT_NOTIFICATION_CHANNEL_URI, filter = "type='ConfigurationVersion' OR type='BrmBackup' OR type='BrmBackupManager'") final Event message) {
        logger.trace("Notification Received through channel: {}", message);
        logger.info("Notification Received through channel: {}", message);
        final Date notificationReceivedDate = new Date();
        final Serializable payLoad = message.getPayload();
        if (!(payLoad instanceof DpsAttributeChangedEvent)) {
            logger.debug("discarded as its not an avc notification: {}", payLoad);
            return;
        }
        final DpsAttributeChangedEvent e = (DpsAttributeChangedEvent) payLoad;
        notificationReciever.notify(e, notificationReceivedDate);
    }
}