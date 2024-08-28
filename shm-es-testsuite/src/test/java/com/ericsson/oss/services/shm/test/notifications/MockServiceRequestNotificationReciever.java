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
package com.ericsson.oss.services.shm.test.notifications;

import java.util.Date;
import java.util.concurrent.Semaphore;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

@ApplicationScoped
@NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
public class MockServiceRequestNotificationReciever implements NotificationReciever {

    private static Semaphore lock;

    public static void reset() {
        lock = new Semaphore(0);
        result = null;
    }

    /**
     * @return the lock
     */
    public static Semaphore getLock() {
        return lock;
    }

    private static DpsAttributeChangedEvent result;

    /**
     * @return the result
     */
    public static DpsAttributeChangedEvent getResult() {
        return result;
    }

    /**
     * Call back method for DpsDataChangedEvent.
     * 
     * @param DpsDataChangedEvent
     */
    @Override
    public void notify(DpsDataChangedEvent e, final Date notificationReceivedDate) {
        if (e instanceof DpsAttributeChangedEvent) {
            result = (DpsAttributeChangedEvent) e;
            lock.release();
        }

    }

}
