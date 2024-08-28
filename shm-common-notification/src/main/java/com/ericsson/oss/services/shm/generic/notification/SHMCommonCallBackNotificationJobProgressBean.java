/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.generic.notification;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

/**
 * Class that implements Notification interface and will provide methods to get SHMCommonCallBackNotification Object and overrides the existing notification interface.
 */
public class SHMCommonCallBackNotificationJobProgressBean implements Notification {

    SHMCommonCallbackNotification shmCommonCallbackNotification;
    DpsDataChangedEvent dpsDataChangedEvent;
    NotificationSubject notificationSubject;
    NotificationEventTypeEnum notificationEventType;

    /**
     * @return the dpsDataChangedEvent
     */
    @Override
    public DpsDataChangedEvent getDpsDataChangedEvent() {
        return dpsDataChangedEvent;
    }
    /**
     * @return the notificationSubject
     */
    @Override
    public NotificationSubject getNotificationSubject() {
        return notificationSubject;
    }

    /**
     * @return the notificationEventTypeEnum
     */
    @Override
    public NotificationEventTypeEnum getNotificationEventType() {
        return notificationEventType;
    }

    /**
     * @return the notificationExtJob
     */
    public SHMCommonCallbackNotification getCommonNotification() {
        return shmCommonCallbackNotification;
    }

    /**
     * set the notificationExtJob
     */
    public void setCommonNotification(final SHMCommonCallbackNotification shmCommonCallbackNotification) {
        this.shmCommonCallbackNotification = shmCommonCallbackNotification;
    }

}
