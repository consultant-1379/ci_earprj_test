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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.notifications.api.*;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(PowerMockRunner.class)
public class ConfirmRestoreRemoteServiceImplTest {

    @InjectMocks
    private ConfirmRestoreRemoteServiceImpl confirmRestoreRemoteServiceImpl;

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
    private AsyncActionProgress asyncActionProgressMock;

    @Mock
    private SystemRecorder systemRecorder;

    private static final String nodeName = "NE_01";
    private static final String domainName = "domainName";
    private static final String backupName = "backupName";
    private static final String backupType = "confirmbackup";
    private static final String MoFdn = "moFdn";

    @Test
    public void testPreCheck_Success() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenReturn(true);
        Assert.assertEquals(true, confirmRestoreRemoteServiceImpl.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testPreCheck_LastRestoredBackupFailure() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenReturn(false);
        Assert.assertEquals(false, confirmRestoreRemoteServiceImpl.precheck(nodeName, ecimBackupInfo));
    }

    @Test
    public void testExecute() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.CONFIRM_RESTORE, nodeName, null)).thenReturn(MoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfo, MoFdn, EcimBackupConstants.CONFIRM_RESTORE)).thenReturn(0);
        final int result = confirmRestoreRemoteServiceImpl.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(0, result);
    }

    @Test(expected = BackupManagementServiceException.class)
    public void testExecute_ThrowsException() throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.CONFIRM_RESTORE, nodeName, null)).thenReturn(null);
        final int result = confirmRestoreRemoteServiceImpl.executeMoAction(nodeName, ecimBackupInfo);
        Assert.assertEquals(-1, result);

    }

}
