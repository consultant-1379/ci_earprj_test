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
package com.ericsson.oss.services.shm.notifications.impl;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;

@RunWith(MockitoJUnitRunner.class)
public class JobNotificationRecieverTest {

    private static final String FDN = "ERBS_01";

    @Mock
    private NotificationRegistry registry;

    @Mock
    private NotificationHandler notificationHandler;

    @InjectMocks
    JobNotificationReciever jobNotificationReciever;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Test
    public void testNotify() {
        DpsAttributeChangedEvent e = new DpsAttributeChangedEvent();
        e.setFdn(FDN);
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(registry.getListener(FDN)).thenReturn(notificationSubject);
        jobNotificationReciever.notify(e, new Date());
        Mockito.verify(notificationHandler, Mockito.atLeastOnce()).processNotification(e, notificationSubject);
    }

    @Test
    public void testNotifyWithCreateEvent() {
        final String fdnForCreateEvent = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final String parentFdnForCreateEvent = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        DpsObjectCreatedEvent e = new DpsObjectCreatedEvent();
        e.setFdn(fdnForCreateEvent);
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(parentFdnForCreateEvent, 123L, jobActivityInfoMock);
        Mockito.when(registry.getListener(parentFdnForCreateEvent)).thenReturn(notificationSubject);
        jobNotificationReciever.notify(e, new Date());
        Mockito.verify(notificationHandler, Mockito.atLeastOnce()).processNotification(e, notificationSubject);
    }

    @Test
    public void testNotifyWithoutHavingListnersRegistered() {
        DpsAttributeChangedEvent e = new DpsAttributeChangedEvent();
        e.setFdn(FDN);
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(registry.getListener(FDN)).thenReturn(null);
        jobNotificationReciever.notify(e, new Date());
        Mockito.verify(notificationHandler, Mockito.times(0)).processNotification(e, notificationSubject);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }
}
