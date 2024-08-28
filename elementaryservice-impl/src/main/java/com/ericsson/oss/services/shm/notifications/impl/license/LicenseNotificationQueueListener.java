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
package com.ericsson.oss.services.shm.notifications.impl.license;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;

/**
 * Listens License Mo change Notifications
 * 
 * @author tcskrna
 * 
 * 
 */
public class LicenseNotificationQueueListener implements EMessageListener<DpsDataChangedEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NotificationReciever notificationReciever;

    public LicenseNotificationQueueListener(final NotificationReciever notificationReciever) {
        this.notificationReciever = notificationReciever;
    }

    @Override
    public void onMessage(final DpsDataChangedEvent message) {
        final Date notificationReceivedDate = new Date();
        logger.trace("Notification Received : {}", message);
        logger.info("====Notification Received through Shm Queue: {}", message);
        // discarding non avc events;
        if (!(message instanceof DpsAttributeChangedEvent)) {
            logger.debug("discarded as its not an avc notification: {}", message);
            return;
        }
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = (DpsAttributeChangedEvent) message;

        notificationReciever.notify(dpsAttributeChangedEvent, notificationReceivedDate);

        logger.info("Delegating to License Inventory...{}", message);
    }
}
