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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.es.api.GenericNotification;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.*;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@ApplicationScoped
public class MockNotificationHandler implements NotificationHandler {

    public static DpsAttributeChangedEvent dps_event;

    public static NotificationSubject reg_subject;

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void reset() {
        counter.set(0);
        dps_event = null;
        reg_subject = null;
    }

    /**
     * Method to process notifications for DpsAttributeChangedEvent.
     * 
     * @param DpsDataChangedEvent
     * @param NotificationSubject
     */
    @Override
    public void processNotification(DpsDataChangedEvent event, NotificationSubject subject) {
        if (event instanceof DpsAttributeChangedEvent) {
            dps_event = (DpsAttributeChangedEvent) event;
            reg_subject = subject;
            counter.incrementAndGet();
            final FdnNotificationSubject sub = (FdnNotificationSubject) subject;
            if (sub.getObserverHandle() instanceof NotificationCallback) {
                final NotificationCallback callback = (NotificationCallback) sub.getObserverHandle();
                GenericNotification avc = new GenericNotification(event, subject, NotificationEventTypeEnum.AVC);
                callback.processNotification(avc);
            } else if (sub.getObserverHandle() instanceof Semaphore) {
                final Semaphore permit = (Semaphore) sub.getObserverHandle();
                permit.release();
            }
        }
    }

}
