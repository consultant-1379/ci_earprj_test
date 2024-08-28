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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.services.shm.notifications.api.*;

/**
 * extend and implement processPayLoad specific processing of notifications in a blocking call;
 * 
 */

public abstract class AbstractNotificationCallBack implements NotificationCallback {

    private static final long serialVersionUID = 5359686499956338890L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Semaphore permit = new Semaphore(0);

    protected final NotificationCallbackResult result = new NotificationCallbackResult();

    public static int acquireTimeToProcessNotification = 2;

    @Override
    public NotificationCallbackResult waitForProcessNotifications() {
        boolean acquire = false;
        boolean complete = false;
        try {
            while (acquire = permit.tryAcquire(acquireTimeToProcessNotification, TimeUnit.MINUTES) && !complete) {
                complete = result.isCompleted();
            }
            if (!acquire) {
                result.setMessage("timeout while processing notifications!");
                return result;
            }
        } catch (InterruptedException e) {
            logger.error("interrupted processing of notifications", e);
            result.setMessage("processing notifications interrupted");
        }
        return result;
    }

    @Override
    public void processNotification(final Notification notification) {
        permit.release();
        if (!(notification.getDpsDataChangedEvent() instanceof DpsAttributeChangedEvent)) {
            return;
        }
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();
        final FdnNotificationSubject subject = (FdnNotificationSubject) notification.getNotificationSubject();
        processPayLoad(subject, event);
    }

    public abstract void processPayLoad(FdnNotificationSubject subject, DpsAttributeChangedEvent event);

    /**
     * @param acquireTimeToProcessNotification
     *            the acquireTimeToProcessNotification to set
     */
    @SuppressWarnings("static-access")
    public void setAcquireTimeToProcessNotification(final int acquireTimeToProcessNotification) {
        this.acquireTimeToProcessNotification = acquireTimeToProcessNotification;
    }

}
