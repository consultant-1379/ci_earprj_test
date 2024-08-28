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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.es.impl.RemoteActivityServiceProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallback;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(MockitoJUnitRunner.class)
public class NotificationHandlerBeanTest {

    private static final String FDN = "LTEERBS01";
    private static final String INSTALL = "install";
    private static final String CPP = "CPP";
    private static final long ACTIVITY_JOB_ID = 123L;

    @Mock
    private DpsReader dpsReader;

    @Mock
    JobsNotificationLoadCounter counterBean;

    @Mock
    NotificationSubject notificationSubject;

    @Mock
    PersistenceObject persistenceObject;

    @Mock
    ActivityServiceProvider activityServiceProvider;

    @Mock
    ActivityCallback activityCallback;

    @Mock
    NotificationCallback notificationCallback;

    @InjectMocks
    NotificationHandlerBean notificationHandlerBean;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    RemoteActivityCallBack activityImplMock;

    @Mock
    RemoteActivityServiceProvider remoteactivityServiceProviderMock;

    @Test
    public void testProcessJobNotification() {
        DpsAttributeChangedEvent event = new DpsAttributeChangedEvent();
        mockJobActivityInfo();
        FdnNotificationSubject subject = new FdnNotificationSubject(FDN, ACTIVITY_JOB_ID, jobActivityInfoMock);
        event.setNamespace(CPP);
        when(activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, INSTALL)).thenReturn(activityCallback);
        notificationHandlerBean.processNotification(event, subject);
    }

    @Test(expected = Exception.class)
    public void testProcessJobNotificationWithException() {
        DpsAttributeChangedEvent event = new DpsAttributeChangedEvent();
        mockJobActivityInfo();
        FdnNotificationSubject subject = new FdnNotificationSubject(FDN, ACTIVITY_JOB_ID, jobActivityInfoMock);
        event.setNamespace(CPP);
        Mockito.doThrow(Exception.class).when(activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, INSTALL));
        notificationHandlerBean.processNotification(event, subject);
    }

    @Test
    public void testProcessJobNotificationWithCreateEvent() {
        DpsObjectCreatedEvent event = new DpsObjectCreatedEvent();
        mockJobActivityInfo();
        FdnNotificationSubject subject = new FdnNotificationSubject(FDN, ACTIVITY_JOB_ID, jobActivityInfoMock);
        event.setNamespace(CPP);
        when(activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, INSTALL)).thenReturn(activityCallback);
        notificationHandlerBean.processNotification(event, subject);
    }

    @Test
    public void testProcessSyncRequestNotification() {
        JobActivityInfo jobActivityInfo = new JobActivityInfo(-1, BackupActivityConstants.ACTION_CREATE_CV, JobTypeEnum.BACKUP, PlatformTypeEnum.CPP);
        DpsAttributeChangedEvent event = new DpsAttributeChangedEvent();
        FdnNotificationSubject subject = new FdnNotificationSubject(FDN, jobActivityInfo, null);
        event.setNamespace(CPP);
        when(notificationSubject.getNotificationType()).thenReturn(NotificationType.SYNCHRONOUS_REQUEST);
        when(remoteactivityServiceProviderMock.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.BACKUP, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(activityImplMock);
        notificationHandlerBean.processNotification(event, subject);
    }

    @Test(expected = Exception.class)
    public void testProcessSyncRequestNotificationWithException() {
        JobActivityInfo jobActivityInfo = new JobActivityInfo(-1, BackupActivityConstants.ACTION_CREATE_CV, JobTypeEnum.BACKUP, PlatformTypeEnum.CPP);
        DpsAttributeChangedEvent event = new DpsAttributeChangedEvent();
        FdnNotificationSubject subject = new FdnNotificationSubject(FDN, jobActivityInfo, null);
        event.setNamespace(CPP);
        when(notificationSubject.getNotificationType()).thenReturn(NotificationType.SYNCHRONOUS_REQUEST);
        Mockito.doThrow(Exception.class).when(remoteactivityServiceProviderMock.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.BACKUP, BackupActivityConstants.ACTION_CREATE_CV));
        notificationHandlerBean.processNotification(event, subject);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.when(jobActivityInfoMock.getActivityName()).thenReturn(INSTALL);
    }
}
