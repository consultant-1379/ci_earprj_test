/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportStatusNotification;
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportResponse;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

public class LkfImportProcessNotificationImpl implements LkfImportStatusNotification {

    private LkfImportResponse lkfImportResponse;
    private DpsDataChangedEvent dataPersistanceServiceDataChangedEvent;
    private NotificationSubject notifySubject;
    private NotificationEventTypeEnum notificationEventTypeEnum;

    @Override
    public DpsDataChangedEvent getDpsDataChangedEvent() {
        return dataPersistanceServiceDataChangedEvent;
    }

    @Override
    public NotificationSubject getNotificationSubject() {
        return notifySubject;
    }

    @Override
    public NotificationEventTypeEnum getNotificationEventType() {
        return notificationEventTypeEnum;
    }

    public LkfImportResponse getLkfImportResponse() {
        return lkfImportResponse;
    }

    public void setLkfImportResponse(LkfImportResponse lkfImportResponse) {
        this.lkfImportResponse = lkfImportResponse;
    }

}
