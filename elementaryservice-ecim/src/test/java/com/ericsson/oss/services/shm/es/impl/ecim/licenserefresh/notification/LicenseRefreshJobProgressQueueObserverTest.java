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

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;

@RunWith(MockitoJUnitRunner.class)
public class LicenseRefreshJobProgressQueueObserverTest {

    @InjectMocks
    LicenseRefreshJobProgressQueueObserver licenseRefreshJobProgressQueueObserverTest;

    @Mock
    Event message;

    @Mock
    DpsAttributeChangedEvent event;

    @Mock
    NotificationSubject notificationSubject;

    @Mock
    NotificationRegistry registry;

    @Mock
    Map<String, AttributeChangeData> modifiedAttributes;

    @Mock
    AttributeChangeData correlationIdAttributeChangeData;

    @Mock
    ActivityUtils activityUtils;

    @Mock
    NotificationHandler notificationHandler;

    @Test
    public void testOnEvent() {
        when(message.getPayload()).thenReturn(event);
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(modifiedAttributes.get(LicenseRefreshConstants.CORRELATION_ID)).thenReturn(correlationIdAttributeChangeData);
        when(correlationIdAttributeChangeData.getNewValue()).thenReturn("39017@4");
        when(registry.getListener((String) correlationIdAttributeChangeData.getNewValue())).thenReturn(notificationSubject);
        licenseRefreshJobProgressQueueObserverTest.onEvent(message);
    }

    @Test
    public void testOnEventWhenNotHavingAVCNotification() {
        licenseRefreshJobProgressQueueObserverTest.onEvent(message);
    }

    @Test
    public void testOnEventWhenCorrelationIdIsNull() {
        when(message.getPayload()).thenReturn(event);
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(modifiedAttributes.get(LicenseRefreshConstants.CORRELATION_ID)).thenReturn(correlationIdAttributeChangeData);
        when(correlationIdAttributeChangeData.getNewValue()).thenReturn(null);
        licenseRefreshJobProgressQueueObserverTest.onEvent(message);
    }
}
