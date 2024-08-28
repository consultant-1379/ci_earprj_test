/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.software.upgrade.notification;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranUpgradeJobNotification;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

/**
 * Class that implements VranJobProgressNotification interface and will provide methods to get vranNotification Object and overrides the existing notification interface.
 */
public class VranNotificationJobProgressBean implements VranUpgradeJobNotification {

    private VranSoftwareUpgradeJobResponse vranNotification;
    private DpsDataChangedEvent dpsDataChangedEvent;
    private NotificationSubject notificationSubject;
    private NotificationEventTypeEnum notificationEventType;

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
    @Override
    public VranSoftwareUpgradeJobResponse getVranNotification() {
        return vranNotification;
    }

    /**
     * set the notificationExtJob
     */
    @Override
    public void setVranNotification(final VranSoftwareUpgradeJobResponse vranNotification) {
        this.vranNotification = vranNotification;

    }

}
