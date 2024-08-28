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
package com.ericsson.oss.services.shm.es.api;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

/**
 * Class that provides necessary information for the notifications received by SHM.
 * 
 * @author xyerrsr
 * 
 */

public class GenericNotification implements Notification {

    final private DpsDataChangedEvent dpsDataChangedEvent;

    final private NotificationSubject notificationSubject;

    final private NotificationEventTypeEnum notificationEventType;

    /**
     * @param dpsDataChangedEvent
     * @param notificationSubject
     */
    public GenericNotification(final DpsDataChangedEvent dataChangedEvent, final NotificationSubject notificationSubject, final NotificationEventTypeEnum notificationEventType) {
        super();
        this.dpsDataChangedEvent = dataChangedEvent;
        this.notificationSubject = notificationSubject;
        this.notificationEventType = notificationEventType;
    }

    /**
     * @return the dpsDataChangedEvent
     */
    public DpsDataChangedEvent getDpsDataChangedEvent() {
        return dpsDataChangedEvent;
    }

    /**
     * @return the notificationSubject
     */
    public NotificationSubject getNotificationSubject() {
        return notificationSubject;
    }

    /**
     * @return the notificationEventType
     */
    @Override
    public NotificationEventTypeEnum getNotificationEventType() {
        return notificationEventType;
    }

}
