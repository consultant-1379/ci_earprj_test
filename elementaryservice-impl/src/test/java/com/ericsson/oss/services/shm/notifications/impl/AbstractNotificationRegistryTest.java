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

import javax.cache.Cache;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;

@RunWith(MockitoJUnitRunner.class)
public class AbstractNotificationRegistryTest {

    private static final String FDN = "ERBS_01";

    @Mock
    Cache<String, NotificationSubject> cache;

    @Mock
    private NotificationSubject subjectMock;

    @InjectMocks
    AbstractNotificationRegistry abstractNotificationRegistry = new NotificationRegistryLocal();

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Test
    public void testRegister() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        abstractNotificationRegistry.register(cache, notificationSubject);
    }

    @Test
    public void testGetListner() {
        abstractNotificationRegistry.getListener(cache, FDN);
        Mockito.verify(cache, Mockito.times(2)).get(FDN);
    }

    @Test
    public void testRemoveSubject() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(cache.get(FDN)).thenReturn(notificationSubject);
        abstractNotificationRegistry.removeSubject(cache, notificationSubject);
        Mockito.verify(cache, Mockito.atLeastOnce()).remove(FDN);
    }

    @Test
    public void testDoNotRemoveSubject() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(cache.get(FDN)).thenReturn(subjectMock);
        boolean isNotificationRemoved = abstractNotificationRegistry.removeSubject(cache, notificationSubject);
        Assert.assertFalse(isNotificationRemoved);
        Mockito.verify(cache, Mockito.never()).remove(FDN);
    }

    @Test
    public void testDoNotRemoveSubject2() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        NotificationSubject notificationSubject2 = new FdnNotificationSubject(FDN, 456L, jobActivityInfoMock);
        Mockito.when(cache.get(FDN)).thenReturn(notificationSubject2);
        boolean isNotificationRemoved = abstractNotificationRegistry.removeSubject(cache, notificationSubject);
        Assert.assertFalse(isNotificationRemoved);
        Mockito.verify(cache, Mockito.never()).remove(FDN);
    }

    @Test
    public void testDoNotRemoveSubject3() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        NotificationSubject notificationSubject2 = new FdnNotificationSubject(FDN, 456L, new JobActivityInfo(456l, "install", JobTypeEnum.UPGRADE, PlatformTypeEnum.CPP));
        Mockito.when(cache.get(FDN)).thenReturn(notificationSubject2);
        boolean isNotificationRemoved = abstractNotificationRegistry.removeSubject(cache, notificationSubject);
        Assert.assertFalse(isNotificationRemoved);
        Mockito.verify(cache, Mockito.never()).remove(FDN);
    }

    @Test
    public void testRemoveSubjectWithoutHavingRegistration() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(cache.get(FDN)).thenReturn(null);
        abstractNotificationRegistry.removeSubject(cache, notificationSubject);
        Mockito.verify(cache, Mockito.times(0)).remove(FDN);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

}
