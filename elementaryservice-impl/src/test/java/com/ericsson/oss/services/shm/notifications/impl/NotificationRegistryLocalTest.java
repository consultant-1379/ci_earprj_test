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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;

@RunWith(MockitoJUnitRunner.class)
public class NotificationRegistryLocalTest {

    private static final String FDN = "ERBS_01";

    @Mock
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    @InjectMocks
    NotificationRegistryLocal notificationRegistryLocal;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Test
    public void testRegisterWithNotification() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        notificationRegistryLocal.register(notificationSubject);
        Mockito.verify(cache).put(FDN, notificationSubject);
    }

    @Test
    public void testGetListner() {
        notificationRegistryLocal.getListener(FDN);
        Mockito.verify(cache, Mockito.times(2)).get(FDN);
    }

    @Test
    public void testRemoveSubject() {
        mockJobActivityInfo();
        NotificationSubject notificationSubject = new FdnNotificationSubject(FDN, 123L, jobActivityInfoMock);
        Mockito.when(cache.get(FDN)).thenReturn(notificationSubject);
        notificationRegistryLocal.removeSubject(notificationSubject);
        Mockito.verify(cache, Mockito.atLeastOnce()).remove(FDN);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }
}
