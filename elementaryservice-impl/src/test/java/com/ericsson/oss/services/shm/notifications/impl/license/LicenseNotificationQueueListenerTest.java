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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;

@RunWith(MockitoJUnitRunner.class)
public class LicenseNotificationQueueListenerTest {

    @InjectMocks
    LicenseNotificationQueueListener licenseNotificationQueueListener;

    @Mock
    DpsDataChangedEvent message;

    @Mock
    DpsAttributeChangedEvent dpsAttributeChangedEvent;

    @Mock
    NotificationReciever notificationReciever;

    @Test
    public void onMessageTest() {
        licenseNotificationQueueListener.onMessage(message);
        verify(notificationReciever, times(0)).notify(dpsAttributeChangedEvent, new Date());
    }

    @Test
    public void onMessageTestwitDpsAttributeChangedEvent() throws Exception {
        licenseNotificationQueueListener.onMessage(dpsAttributeChangedEvent);
        verify(notificationReciever, times(1)).notify((DpsDataChangedEvent) Matchers.anyObject(), (Date) Matchers.anyObject());
    }
}
