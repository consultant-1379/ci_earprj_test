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
package com.ericsson.oss.services.shm.webpush.notifications.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.*;

@RunWith(MockitoJUnitRunner.class)
public class JobPOsNotificationQueueListenerTest {

    DpsDataChangedEvent message;

    @Mock
    JobNotificationUtil jobNotificationUtil;

    @Mock
    DpsAttributeChangedEvent dpsAttributeChangedEvent;

    @Mock
    DpsObjectCreatedEvent dpsObjectCreatedEvent;

    @Mock
    DpsObjectDeletedEvent dpsObjectDeletedEvent;

    ShmJobNotificationQueueListener objectUnderTest;

    @Test
    public void testOnMessageForObjectCreateEvent() {
        objectUnderTest = new ShmJobNotificationQueueListener(jobNotificationUtil);
        message = new DpsObjectCreatedEvent();
        objectUnderTest.onMessage(message);
    }

    @Test
    public void testOnMessageForObjectDeleteEvent() {
        objectUnderTest = new ShmJobNotificationQueueListener(jobNotificationUtil);
        message = new DpsObjectDeletedEvent();
        objectUnderTest.onMessage(message);
    }

    @Test
    public void testOnMessageForAttributeChangedEvent() {
        objectUnderTest = new ShmJobNotificationQueueListener(jobNotificationUtil);
        message = new DpsAttributeChangedEvent();
        objectUnderTest.onMessage(message);
    }
}
