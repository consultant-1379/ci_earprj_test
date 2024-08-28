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
package com.ericsson.oss.services.shm.backupservice.ecim.remote;

import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.notification.common.NotificationInformation;
import com.ericsson.oss.services.shm.notification.common.RemoteActivityNotificationHelper;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallback;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(PowerMockRunner.class)
public class UploadBackUpRemoteServiceImplTest {

    @InjectMocks
    private UploadBackUpRemoteServiceImpl uploadBackUpRemoteServiceImplMock;

    @Mock
    private FdnNotificationSubject fdnNotificationSubjectMock;

    @Mock
    private BrmMoServiceRetryProxy brmMoServiceMock;

    @Mock
    private DpsAttributeChangedEvent event;

    @Mock
    private NotificationRegistry notificationRegistry;

    @Mock
    private NotificationCallback notificationCallback;

    @Mock
    private NotificationCallbackResult notificationCallbackResultMock;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private Notification notificationMock;

    @Mock
    private RemoteActivityNotificationHelper remoteActivityNotificationHelperMock;

    @Mock
    private DpsAttributeChangedEvent dpsAttributeChangedEventMock;

    @Mock
    private NotificationInformation notificationInformation;

    @Mock
    private Map<String, AttributeChangeData> modifiedAttributes;

    @Mock
    private AsyncActionProgress progressReportMock;

    @Mock
    private Semaphore semaphoreMock;

    private static final String nodeName = "NE_01";
    private static final String domainName = "domainName";
    private static final String backupName = "backupName";
    private static final String backupType = "Uploadbackup";
    private static final String moFdn = "moFdn";

    @Test
    public void testPreCheck() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        Assert.assertEquals(true, uploadBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testPreCheck_Fail() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(false);
        Assert.assertEquals(false, uploadBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testPreCheckThrowMoNotFoundException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenThrow(MoNotFoundException.class);
        uploadBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testPreCheckThrowUnsupportedFragmentException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenThrow(UnsupportedFragmentException.class);
        uploadBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo);
    }

    @Test
    public void testExecute() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(uploadBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        final int resultstatus = uploadBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(1, resultstatus);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecuteWhenMoIsNull() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(uploadBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(null);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, null, EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, null);
        uploadBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);

    }

    @Test
    public void testExecute_Upload_Fail() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(uploadBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.UPLOAD_BACKUP, ecimBackupInfo)).thenReturn(null);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        Assert.assertEquals(0, uploadBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo));

    }

    @Test
    public void testExecute_WhenBkpAlreadyUploaded() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(uploadBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.UPLOAD_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        Assert.assertEquals(1, uploadBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo));

    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_Upload_Fail_WhenNoNotifications() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(uploadBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.UPLOAD_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.FAILURE);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        uploadBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testprocessNotification() throws UnsupportedFragmentException, MoNotFoundException {

        when(notificationMock.getDpsDataChangedEvent()).thenReturn(event);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsAttributeChangedEventMock);
        when(notificationMock.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        when(notificationInformation.getNotificationCallbackResult()).thenReturn(notificationCallbackResultMock);
        when(notificationInformation.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(brmMoServiceMock.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(progressReportMock);
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(notificationInformation.getPermit()).thenReturn(semaphoreMock);
        Mockito.doNothing().when(semaphoreMock).release();
        uploadBackUpRemoteServiceImplMock.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(true);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testprocessNotification_Fail() throws UnsupportedFragmentException, MoNotFoundException {

        when(notificationMock.getDpsDataChangedEvent()).thenReturn(event);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsAttributeChangedEventMock);
        when(notificationMock.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        when(notificationInformation.getNotificationCallbackResult()).thenReturn(notificationCallbackResultMock);
        when(notificationInformation.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(brmMoServiceMock.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(progressReportMock);
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.FAILURE);
        when(notificationInformation.getPermit()).thenReturn(semaphoreMock);
        Mockito.doNothing().when(semaphoreMock).release();
        uploadBackUpRemoteServiceImplMock.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(false);

    }

}
