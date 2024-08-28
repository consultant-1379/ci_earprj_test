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
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.*;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.notification.common.NotificationInformation;
import com.ericsson.oss.services.shm.notification.common.RemoteActivityNotificationHelper;
import com.ericsson.oss.services.shm.notifications.api.*;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(PowerMockRunner.class)
public class CreateBackUpRemoteServiceImplTest {

    @InjectMocks
    private CreateBackUpRemoteServiceImpl createBackUpRemoteServiceImplMock;

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
        Assert.assertEquals(true, createBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testExecute() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(createBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.CREATE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(moFdn)).thenReturn(notificationCallbackResultMock);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        final int resultstatus = createBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(1, resultstatus);

    }

    @Test
    public void testExecute_WhenBackupAlreadyExists() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(createBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.CREATE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(moFdn)).thenReturn(notificationCallbackResultMock);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        final int resultstatus = createBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(1, resultstatus);
    }

    @Test
    public void testExecute_WhenProgressReportIsNull() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(createBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.CREATE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(moFdn)).thenReturn(notificationCallbackResultMock);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo)).thenReturn(null);
        final int resultstatus = createBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(0, resultstatus);
    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_WhenBackupCreationFail() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(createBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.CREATE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(moFdn)).thenReturn(notificationCallbackResultMock);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(false);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        createBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecuteFailure() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(createBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.CREATE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(moFdn)).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        createBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_Fail_WhenMoIsNull() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, CreateBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(createBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(null);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        createBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
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
        createBackUpRemoteServiceImplMock.processNotification(notificationMock);
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
        createBackUpRemoteServiceImplMock.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(false);

    }

}
