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
package com.ericsson.oss.services.shm.notifications.impl;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectDeletedEvent;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

/**
 * For sending notifications for job type observer.
 */
@Traceable
@NotificationTypeQualifier(type = NotificationType.JOB)
public class JobNotificationReciever implements NotificationReciever {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String COMMA_SEPARATOR_FOR_MO_FDN = ",";

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry registry;

    @Inject
    private NotificationHandler notificationHandler;

    /**
     * notify the event to interested listeners;
     */
    @Override
    public void notify(final DpsDataChangedEvent e, final Date notificationReceivedDate) {
        if (e instanceof DpsAttributeChangedEvent) {
            logger.debug("JobNotificationReceiver - considering DpsAttributeChangedEvent");
            final String fdn = e.getFdn();

            getListenerAndProcessNotification(fdn, e, notificationReceivedDate);
        } else if (e instanceof DpsObjectCreatedEvent) {
            logger.debug("JobNotificationReceiver - considering DpsObjectCreatedEvent");
            String parentFdnForCreatedObject = null;

            //In case of Object creation, its parent will be subscribed to the notifications. so, parent's FDN should be used to get the listener.
            final String fdnForCreatedObject = e.getFdn();
            logger.debug("fdnForCreatedObject : {}", fdnForCreatedObject);
            final int lastIndexOfCommaToDetermineParent = fdnForCreatedObject.lastIndexOf(COMMA_SEPARATOR_FOR_MO_FDN);
            if (lastIndexOfCommaToDetermineParent != -1) {
                parentFdnForCreatedObject = fdnForCreatedObject.substring(0, lastIndexOfCommaToDetermineParent);
            }

            getListenerAndProcessNotification(parentFdnForCreatedObject, e, notificationReceivedDate);
        }
        
        else if (e instanceof DpsObjectDeletedEvent) {
            logger.debug("JobNotificationReceiver - considering DpsObjectDeletedEvent");
            //In case of Object deletion, its parent will be subscribed to the notifications. so, parent's FDN should be used to get the listener.
            final String fdnForDeletedObject = e.getFdn();
            logger.debug("fdnForDeletedObject : {}", fdnForDeletedObject);
            getListenerAndProcessNotification(fdnForDeletedObject, e, notificationReceivedDate);
        }
    }

    /**
     * Method to retrieve the listener for the input event on the specified input FDN.
     * 
     * @param fdn
     * @param event
     */
    private void getListenerAndProcessNotification(final String fdn, final DpsDataChangedEvent event, final Date notificationReceivedDate) {
        logger.debug("Retrieving listener for fdn : {}", fdn);
        final NotificationSubject listener = registry.getListener(fdn);

        if (listener == null) {
            logger.info("No listeners for the event - {} ", event);
            return;
        }
        logger.info("Sending notification {} to {}", event, listener);
        listener.setTimeStamp(notificationReceivedDate);

        notificationHandler.processNotification(event, listener);
    }
}
