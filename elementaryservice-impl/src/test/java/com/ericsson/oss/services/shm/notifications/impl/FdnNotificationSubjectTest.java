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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallback;

@RunWith(MockitoJUnitRunner.class)
public class FdnNotificationSubjectTest {

    private static final String FDN = "ERBS01";

    private static final String FDN_2 = "ERBS02";

    private static final long jobId = 123L;

    private static final String CV_NOTIFICATION_KEY = "CreateCV";

    @Mock
    private NotificationCallback notificationCallback;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Test
    public void testIfFdnNotificationObjectsEqual() {
        mockJobActivityInfo();
        FdnNotificationSubject fdnNotificationSubject1 = new FdnNotificationSubject(FDN, jobId, jobActivityInfoMock);
        FdnNotificationSubject fdnNotificationSubject2 = new FdnNotificationSubject(FDN, jobId, jobActivityInfoMock);

        FdnNotificationSubject fdnNotificationSubject5 = new FdnNotificationSubject(FDN, CV_NOTIFICATION_KEY);
        FdnNotificationSubject fdnNotificationSubject6 = new FdnNotificationSubject(FDN, CV_NOTIFICATION_KEY);
        FdnNotificationSubject fdnNotificationSubject7 = fdnNotificationSubject1;
        FdnNotificationSubject fdnNotificationSubject8 = new FdnNotificationSubject(FDN_2, jobId, jobActivityInfoMock);
        Assert.assertTrue(fdnNotificationSubject1.equals(fdnNotificationSubject2));

        Assert.assertTrue(fdnNotificationSubject5.equals(fdnNotificationSubject6));
        Assert.assertTrue(fdnNotificationSubject1.equals(fdnNotificationSubject7));
        Assert.assertFalse(fdnNotificationSubject1.equals(fdnNotificationSubject8));
        Assert.assertFalse(fdnNotificationSubject1.equals(null));
        Assert.assertNotNull(fdnNotificationSubject1.hashCode());
        Assert.assertEquals(jobId, fdnNotificationSubject1.getObserverHandle());
        Assert.assertEquals(FDN, fdnNotificationSubject1.getFdn());
        Assert.assertEquals(FDN, fdnNotificationSubject1.getKey());
        Assert.assertEquals(NotificationType.JOB, fdnNotificationSubject1.getNotificationType());
        Assert.assertEquals("NotificationSubject [ fdn=" + FDN + " ,  activity job id=" + jobId +  "]", fdnNotificationSubject1.toString());
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testFdnNotificationObjectsEqual() {
        mockJobActivityInfo();
        FdnNotificationSubject fdnNotificationSubject1 = new FdnNotificationSubject(FDN, 123l, jobActivityInfoMock);
        FdnNotificationSubject fdnNotificationSubject2 = new FdnNotificationSubject(FDN, 456l, jobActivityInfoMock);
        Assert.assertFalse(fdnNotificationSubject1.equals(fdnNotificationSubject2));

        FdnNotificationSubject fdnNotificationSubject3 = new FdnNotificationSubject(FDN, 123l, jobActivityInfoMock);
        FdnNotificationSubject fdnNotificationSubject4 = new FdnNotificationSubject("ERBS2", 123l, jobActivityInfoMock);
        Assert.assertFalse(fdnNotificationSubject3.equals(fdnNotificationSubject4));

        FdnNotificationSubject fdnNotificationSubject5 = new FdnNotificationSubject(FDN, 123l, jobActivityInfoMock);
        FdnNotificationSubject fdnNotificationSubject6 = new FdnNotificationSubject(FDN, 123l, jobActivityInfoMock);
        Assert.assertTrue(fdnNotificationSubject5.equals(fdnNotificationSubject6));

        Assert.assertFalse(fdnNotificationSubject5.equals(null));
    }
}
