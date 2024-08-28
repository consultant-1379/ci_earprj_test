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
package com.ericsson.oss.services.shm.backupservice.impl;

import static org.mockito.Mockito.when;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

@RunWith(MockitoJUnitRunner.class)
public class BackupNotificationTopicObserverTest {

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
    private NotificationReciever notificationReciever;

    @InjectMocks
    BackupNotificationTopicObserver objectUnderTest;

    @Mock
    Event message;

    @Test
    public void testOnEvent() {
        final Serializable payLoad = new DpsAttributeChangedEvent();
        when(message.getPayload()).thenReturn(payLoad);
        objectUnderTest.onEvent(message);
    }

    @Test
    public void testOnEventWhenNotHavingAVCNotification() {
        objectUnderTest.onEvent(message);
    }

}
