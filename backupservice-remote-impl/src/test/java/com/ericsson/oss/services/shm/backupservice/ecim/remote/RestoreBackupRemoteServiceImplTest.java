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

import java.util.HashMap;
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
import com.ericsson.oss.services.shm.backupservice.remote.impl.RemoteActivityCompleteTimer;
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
public class RestoreBackupRemoteServiceImplTest {

    @InjectMocks
    private RestoreBackupRemoteServiceImpl restoreBackupRemoteServiceImpl;

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

    @InjectMocks
    private NotificationCallbackResult notificationCallbackResultMock1;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private AsyncActionProgress asyncActionProgressMock;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private Notification notificationMock;

    @Mock
    private NotificationInformation notificationInformationMock;

    @Mock
    private EcimBackupInfo ecimBackupInfoMock;

    @Mock
    private Semaphore permit;

    @Mock
    RemoteActivityNotificationHelper remoteActivityNotificationHelperMock;

    @Mock
    private RemoteActivityCompleteTimer remoteActivityCompleteTimeMockr;

    @Mock
    private ConfirmRestoreRemoteServiceImpl confirmRestoreRemoteServiceImplMock;

    @Mock
    private Map<String, AttributeChangeData> modifiedAttributes;

    private static final String nodeName = "NE_01";
    private static final String domainName = "domainName";
    private static final String backupName = "backupName";
    private static final String backupType = "RestoreBackup";
    private static final String moFdn = "moFdn";

    @Test
    public void testPreCheck_Success() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        Assert.assertEquals(true, restoreBackupRemoteServiceImpl.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testPreCheck_Failure() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(false);
        Assert.assertEquals(false, restoreBackupRemoteServiceImpl.precheck(nodeName, ecimBackupInfo));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testPreCheck_ThrowsMoNotFoundException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenThrow(MoNotFoundException.class);
        Assert.assertEquals(false, restoreBackupRemoteServiceImpl.precheck(nodeName, ecimBackupInfo));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testPreCheck_ThrowsUnSupportedFragmentException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isBackupExist(nodeName, ecimBackupInfo)).thenThrow(UnsupportedFragmentException.class);
        Assert.assertEquals(false, restoreBackupRemoteServiceImpl.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testExecute() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, RestoreBackupRemoteServiceImpl.class, ecimBackupInfo)).thenReturn(fdnNotificationSubjectMock);
        when(restoreBackupRemoteServiceImpl.getNotifiableFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(1);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        final int result = restoreBackupRemoteServiceImpl.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(1, result);
    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_WhenMoNull() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(restoreBackupRemoteServiceImpl.getNotifiableFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(null);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        restoreBackupRemoteServiceImpl.executeMoAction(nodeName, ecimBackupInfo);

    }

    @SuppressWarnings("unchecked")
    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_ThrowsException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(remoteActivityNotificationHelperMock.subscribeToNotification(nodeName, moFdn, RestoreBackupRemoteServiceImpl.class, ecimBackupInfo)).thenReturn(fdnNotificationSubjectMock);
        when(restoreBackupRemoteServiceImpl.getNotifiableFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(moFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, moFdn, EcimBackupConstants.RESTORE_BACKUP)).thenThrow(BackupManagementServiceException.class);
        when(remoteActivityNotificationHelperMock.waitForProcessNotifications(Matchers.anyString())).thenReturn(notificationCallbackResultMock);
        Mockito.doNothing().when(remoteActivityNotificationHelperMock).unSubscribeToNotification(fdnNotificationSubjectMock, moFdn);
        restoreBackupRemoteServiceImpl.executeMoAction(nodeName, ecimBackupInfo);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotification_Running() throws UnsupportedFragmentException, MoNotFoundException {
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(brmMoServiceMock.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(asyncActionProgressMock);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(event);
        when(notificationMock.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notificationInformationMock.getNotificationCallbackResult()).thenReturn(notificationCallbackResultMock);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.RUNNING);
        when(asyncActionProgressMock.getActionName()).thenReturn(backupType);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(50);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformationMock);
        when(notificationInformationMock.getNodeName()).thenReturn(nodeName);
        when(notificationInformationMock.getPermit()).thenReturn(permit);
        Mockito.doNothing().when(permit).release();
        restoreBackupRemoteServiceImpl.processNotification(notificationMock);
        Assert.assertEquals(asyncActionProgressMock.getProgressPercentage(), 50);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotification_Finished_ActionSuccess() throws UnsupportedFragmentException, MoNotFoundException, BackupManagementServiceException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final Map<String, String> domainTypeMap = new HashMap<String, String>();
        domainTypeMap.put(EcimBackupConstants.BACKUP_DOMAIN, "Domain");
        domainTypeMap.put(EcimBackupConstants.BACKUP_TYPE, "Type");
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(event);
        when(notificationMock.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notificationInformationMock.getNotificationCallbackResult()).thenReturn(notificationCallbackResultMock);
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(brmMoServiceMock.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(asyncActionProgressMock);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgressMock.getActionName()).thenReturn(backupType);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(50);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformationMock);
        when(notificationInformationMock.getNodeName()).thenReturn(nodeName);
        when(notificationInformationMock.getPermit()).thenReturn(permit);
        Mockito.doNothing().when(permit).release();
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(notificationInformationMock.getEcimBackupInfo()).thenReturn(ecimBackupInfo);
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenReturn(true);
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenReturn(true);
        when(confirmRestoreRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(confirmRestoreRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(0);
        restoreBackupRemoteServiceImpl.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(true);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotification_Finished_ActionFailure() throws UnsupportedFragmentException, MoNotFoundException, BackupManagementServiceException {
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final Map<String, String> domainTypeMap = new HashMap<String, String>();
        domainTypeMap.put(EcimBackupConstants.BACKUP_DOMAIN, "Domain");
        domainTypeMap.put(EcimBackupConstants.BACKUP_TYPE, "Type");
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(event);
        when(notificationMock.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notificationInformationMock.getNotificationCallbackResult()).thenReturn(notificationCallbackResultMock);
        when(activityUtils.getModifiedAttributes(event)).thenReturn(modifiedAttributes);
        when(brmMoServiceMock.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(asyncActionProgressMock);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgressMock.getActionName()).thenReturn(backupType);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(50);
        when(remoteActivityNotificationHelperMock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformationMock);
        when(notificationInformationMock.getNodeName()).thenReturn(nodeName);
        when(notificationInformationMock.getPermit()).thenReturn(permit);
        Mockito.doNothing().when(permit).release();
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.FAILURE);
        when(notificationInformationMock.getEcimBackupInfo()).thenReturn(ecimBackupInfo);
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenReturn(true);
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenReturn(true);
        when(confirmRestoreRemoteServiceImplMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(confirmRestoreRemoteServiceImplMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(0);
        restoreBackupRemoteServiceImpl.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(false);

    }

}
