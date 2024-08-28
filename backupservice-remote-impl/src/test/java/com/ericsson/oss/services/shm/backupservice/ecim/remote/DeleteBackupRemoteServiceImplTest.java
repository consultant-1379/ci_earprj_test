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

import java.util.*;
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
public class DeleteBackupRemoteServiceImplTest {

    @InjectMocks
    private DeleteBackUpRemoteServiceImpl deleteBackUpRemoteServiceImplMock;

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
    private static final String backupType = "Deletebackup";
    private static final String MoFdn = "moFdn";

    @Test
    public void testPrecheck() throws BackupManagementServiceException, UnsupportedFragmentException, MoNotFoundException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final List<String> backupList = new ArrayList<String>();
        final List<String> validBackupList = new ArrayList<String>();
        backupList.add(backupName);
        validBackupList.add(backupName);
        when(brmMoServiceMock.getBackupDetails(backupList, nodeName, domainName, backupType)).thenReturn(validBackupList);
        Assert.assertEquals(true, deleteBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testPreCheckThrowMoNotFoundException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final List<String> backupList = new ArrayList<String>();
        backupList.add(backupName);
        when(brmMoServiceMock.getBackupDetails(backupList, nodeName, domainName, backupType)).thenThrow(MoNotFoundException.class);
        Assert.assertEquals(false, deleteBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));

    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testPreCheckThrowUnsupportedFragmentException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final List<String> backupList = new ArrayList<String>();
        backupList.add(backupName);
        when(brmMoServiceMock.getBackupDetails(backupList, nodeName, domainName, backupType)).thenThrow(UnsupportedFragmentException.class);
        Assert.assertEquals(false, deleteBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));

    }

    @Test
    public void testPrecheck_NoBackup() throws BackupManagementServiceException, UnsupportedFragmentException, MoNotFoundException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final List<String> backupList = new ArrayList<String>();
        final List<String> validBackupList = new ArrayList<String>();
        backupList.add(backupName);
        when(brmMoServiceMock.getBackupDetails(backupList, nodeName, domainName, backupType)).thenReturn(validBackupList);
        Assert.assertEquals(false, deleteBackUpRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testExecute() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, MoFdn, DeleteBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(deleteBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(MoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, MoFdn, EcimBackupConstants.DELETE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, MoFdn);
        final int resultstatus = deleteBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(1, resultstatus);

    }

    @Test
    public void testExecute_WhenBkpAlreadyDeleted() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, MoFdn, DeleteBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(deleteBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(MoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, MoFdn, EcimBackupConstants.DELETE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(brmMoServiceMock.isBackupDeletionCompleted(nodeName, ecimBackupInfo)).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, MoFdn);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.DELETE_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        final int resultstatus = deleteBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(1, resultstatus);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_Fail() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, MoFdn, DeleteBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(deleteBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(MoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, MoFdn, EcimBackupConstants.DELETE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(brmMoServiceMock.isBackupDeletionCompleted(nodeName, ecimBackupInfo)).thenReturn(false);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(MoFdn)).thenReturn(notificationInformation);
        when(brmMoServiceMock.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.DELETE_BACKUP, ecimBackupInfo)).thenReturn(progressReportMock);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, MoFdn);
        deleteBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_MoNull() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, null, DeleteBackUpRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        when(deleteBackUpRemoteServiceImplMock.getNotifiableFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(null);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, MoFdn, EcimBackupConstants.DELETE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, null);
        deleteBackUpRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo);

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
        notificationCallbackResultMock.setCompleted(true);
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(notificationInformation.getPermit()).thenReturn(semaphoreMock);
        Mockito.doNothing().when(semaphoreMock).release();
        deleteBackUpRemoteServiceImplMock.processNotification(notificationMock);
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
        notificationCallbackResultMock.setCompleted(true);
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.FAILURE);
        when(notificationInformation.getPermit()).thenReturn(semaphoreMock);
        Mockito.doNothing().when(semaphoreMock).release();
        deleteBackUpRemoteServiceImplMock.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(false);

    }

}
