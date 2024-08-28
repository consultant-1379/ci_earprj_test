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
package com.ericsson.oss.services.shm.notification.common;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.shm.backup.constants.CppBackupConstants;

@RunWith(PowerMockRunner.class)
public class RemoteActivityNotificationHelperTest {

    @InjectMocks
    private RemoteActivityNotificationHelper remoteActivityNotificationHelper;

    @Mock
    private ActivityUtils activityUtilsMock;

    @Mock
    private NotificationRegistry notificationRegistry;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private NotificationCallbackResult notificationCallBackResult;

    @Mock
    private NotificationInformation notificationInformationMock;

    @Mock
    private RemoteRequestTimeoutListener remoteRequestTimeoutListener;

    @Mock
    private EcimBackupInfo backupInfo;

    @Test
    public void test_subscribeToNotification() {
        JobActivityInfo jobActivityInfoMock = new JobActivityInfo(-1L, CppBackupConstants.CURRENT_DETAILED_ACTIVITY, JobTypeEnum.BACKUP,
                PlatformTypeEnum.ECIM);
        when(activityUtilsMock.getRemoteActivityInfo(-1, RemoteActivityCallBack.class)).thenReturn(jobActivityInfoMock);
        NotificationCallbackResult notificationCallbackResult1 = new NotificationCallbackResult();
        FdnNotificationSubject fdnNotificationSubject1 = new FdnNotificationSubject("SGSN-MME-15B-V502", jobActivityInfoMock,
                notificationCallbackResult1);
        remoteActivityNotificationHelper.subscribeToNotification("SGSN-MME-15B-V502", "SGSN-MME-15B-V502", RemoteActivityCallBack.class);
        Mockito.verify(notificationRegistry).register(fdnNotificationSubject1);

    }

    @Test
    public void test_subscribeToNotificationForRestore() {

        JobActivityInfo jobActivityInfoMock = new JobActivityInfo(-1L, CppBackupConstants.CURRENT_DETAILED_ACTIVITY, JobTypeEnum.BACKUP,
                PlatformTypeEnum.ECIM);
        when(activityUtilsMock.getRemoteActivityInfo(-1, RemoteActivityCallBack.class)).thenReturn(jobActivityInfoMock);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        NotificationCallbackResult notificationCallbackResult1 = new NotificationCallbackResult();
        FdnNotificationSubject fdnNotificationSubject1 = new FdnNotificationSubject("SGSN-MME-15B-V502", jobActivityInfoMock,
                notificationCallbackResult1);
        remoteActivityNotificationHelper.subscribeToNotification("SGSN-MME-15B-V502", "SGSN-MME-15B-V502", RemoteActivityCallBack.class, backupInfo);
        Mockito.verify(notificationRegistry).register(fdnNotificationSubject1);
    }

    @Test
    public void test_unSubscribeToNotification() {

        JobActivityInfo jobActivityInfoMock = new JobActivityInfo(-1L, CppBackupConstants.CURRENT_DETAILED_ACTIVITY, JobTypeEnum.BACKUP,
                PlatformTypeEnum.ECIM);
        when(activityUtilsMock.getRemoteActivityInfo(-1, RemoteActivityCallBack.class)).thenReturn(jobActivityInfoMock);
        remoteActivityNotificationHelper.unSubscribeToNotification(fdnNotificationSubject, "SGSN-MME-15B-V502");
        Mockito.verify(notificationRegistry).removeSubject(fdnNotificationSubject);
    }

    @SuppressWarnings("static-access")
    @Test
    public void test_waitForProcessNotifications_Failure() {
        final String moFdn = "SGSN-MME-15B-V502";
        NotificationInformation notificationInformation = new NotificationInformation(notificationCallBackResult, "SGSN-MME-15B-V502", null);
        remoteActivityNotificationHelper.setNotificationSubjectInformation(moFdn, notificationInformation);
        when(remoteRequestTimeoutListener.getRemoteRequestTimeoutValue()).thenReturn(5);
        when(notificationCallBackResult.isCompleted()).thenReturn(true);
        when(notificationInformation.getNotificationCallbackResult().isCompleted()).thenReturn(true);
        NotificationCallbackResult notificationCallBack = remoteActivityNotificationHelper.waitForProcessNotifications("SGSN-MME-15B-V502");
        Assert.assertEquals(false, notificationCallBack.isSuccess());

    }

    @SuppressWarnings("static-access")
    @Test
    public void test_waitForProcessNotifications_Success() {
        final String moFdn = "SGSN-MME-15B-V502";
        NotificationInformation notificationInformation = new NotificationInformation(notificationCallBackResult, "SGSN-MME-15B-V502", null);
        NotificationCallbackResult notificationCallBackResultLocal = new NotificationCallbackResult();
        notificationCallBackResultLocal.setCompleted(true);
        remoteActivityNotificationHelper.setNotificationSubjectInformation(moFdn, notificationInformation);
        when(remoteRequestTimeoutListener.getRemoteRequestTimeoutValue()).thenReturn(5);
        when(notificationCallBackResult.isCompleted()).thenReturn(true);
        when(notificationInformation.getNotificationCallbackResult().isCompleted()).thenReturn(true);
        when(notificationInformationMock.getNotificationCallbackResult()).thenReturn(notificationCallBackResultLocal);
        NotificationCallbackResult notificationCallBack = remoteActivityNotificationHelper.waitForProcessNotifications("SGSN-MME-15B-V502");
        Assert.assertEquals(false, notificationCallBack.isSuccess());

    }

}
