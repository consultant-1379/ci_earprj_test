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

import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

@Stateless
@Traceable
@NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
public class RemoteRequestNotificationReciever implements NotificationReciever {

    private static final Logger logger = LoggerFactory.getLogger(RemoteRequestNotificationReciever.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
    private NotificationRegistry registry;

    @Inject
    private NotificationHandler notificationHandler;

    /**
     * Method to notify the appropriate handler about the incoming AVC notification for synchronous requests.
     * 
     * @param DpsDataChangedEvent
     */
    @Override
    public void notify(final DpsDataChangedEvent e, final Date notificationReceivedDate) { //Changing only the parameter type to parent of DpsAttributeChangedEvent. i.e., DpsDataChangedEvent
        if (e instanceof DpsAttributeChangedEvent) { //Implementation will consider only DpsAttributeChangedEvent., same as the existing implementation.
            final String fdn = e.getFdn();
            final NotificationSubject listener = registry.getListener(fdn);
            if (listener == null) {
                logger.info("RemoteRequestNotification - No listeners for AVC event {}", e);
                return;
            }
            logger.info("Sending notification {} to {}", e, listener);
            listener.setTimeStamp(notificationReceivedDate);

            notificationHandler.processNotification(e, listener);
        }

    }

}
