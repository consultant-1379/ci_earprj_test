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

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(MockitoJUnitRunner.class)
public class RemoteRequestNotificationRecieverTest {

    private static final String FDN = "ERBS_01";

    @Mock
    private NotificationRegistry registry;

    @Mock
    private NotificationHandler notificationHandler;

    @InjectMocks
    RemoteRequestNotificationReciever serviceRequestNotificationReciever;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Test
    public void testNotify() {
        DpsAttributeChangedEvent e = new DpsAttributeChangedEvent();
        e.setFdn(FDN);
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(registry.getListener(FDN)).thenReturn(notificationSubject);
        serviceRequestNotificationReciever.notify(e, new Date());
        Mockito.verify(notificationHandler, Mockito.atLeastOnce()).processNotification(e, notificationSubject);
    }

    @Test
    public void testNotifyWithoutHavingListnersRegistered() {
        DpsAttributeChangedEvent e = new DpsAttributeChangedEvent();
        e.setFdn(FDN);
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(registry.getListener(FDN)).thenReturn(null);
        serviceRequestNotificationReciever.notify(e, new Date());
        Mockito.verify(notificationHandler, Mockito.times(0)).processNotification(e, notificationSubject);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

}
