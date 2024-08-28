package com.ericsson.oss.services.shm.notifications.api;

import java.util.Date;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;

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

/**
 * Interface for notification receivers that listen for data change notifications (which can be AVC or Object Creation) from DPS.
 * 
 */
public interface NotificationReciever {

    void notify(final DpsDataChangedEvent e, final Date notificationReceivedDate);

}
