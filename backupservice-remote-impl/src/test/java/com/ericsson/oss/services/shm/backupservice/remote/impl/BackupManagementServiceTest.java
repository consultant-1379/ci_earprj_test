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
package com.ericsson.oss.services.shm.backupservice.remote.impl;

import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.services.shm.backupservice.ecim.remote.CommonRemoteBackUpManagementService;
import com.ericsson.oss.services.shm.backupservice.ecim.remote.CreateBackUpRemoteServiceImpl;
import com.ericsson.oss.services.shm.backupservice.remote.api.*;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoService;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;

@RunWith(PowerMockRunner.class)
public class BackupManagementServiceTest {

    @Mock
    BackupManagementServiceFactory backupManagementServiceFactoryMock;

    @Mock
    BrmMoService brmMoServiceMock;

    @InjectMocks
    BackupManagementService backupManagementServiceMock;

    @Mock
    private CommonRemoteBackUpManagementService CommonRemoteBackUpManagementServiceMock;

    @Mock
    CreateBackUpRemoteServiceImpl createBackUpRemoteServiceImplMock;

    @Mock
    EcimBackupInfo ecimBackupInfo;

    @Mock
    DeleteBackupOptions deleteBackupOptions;

    final String neName = "erbs";
    final String nodeName = "node1";

    @Test(expected = BackupManagementServiceException.class)
    public void createBackupTest() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.CREATE_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.createBackup(neName, backupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void createBackupTestFail() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.CREATE_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.createBackup(null, backupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void uploadBackupTest() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.uploadBackup(neName, backupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void uploadBackupTestFail() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.UPLOAD_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.uploadBackup(null, backupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void deleteBackupTest() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.DELETE_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.deleteBackup(neName, backupInfo,deleteBackupOptions);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void deleteBackupTestFail() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.DELETE_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.deleteBackup(null, backupInfo,deleteBackupOptions);

    }
    
    @Test(expected = BackupManagementServiceException.class)
    public void restoreBackupTest() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.RESTORE_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.restoreBackup(neName, backupInfo);

    }

    @Test(expected = BackupManagementServiceException.class)
    public void restoreBackupTestFail() throws BackupManagementServiceException {
        final BackupInfo backupInfo = new BackupInfo("bacup1", "domain", "Type", "", "");
        when(backupManagementServiceFactoryMock.getBackUpManagementService(EcimBackupConstants.RESTORE_BACKUP)).thenReturn(
                CommonRemoteBackUpManagementServiceMock);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
        when(CommonRemoteBackUpManagementServiceMock.precheck(nodeName, ecimBackupInfo)).thenReturn(true);
        when(CommonRemoteBackUpManagementServiceMock.executeMoAction(nodeName, ecimBackupInfo)).thenReturn(1);
        backupManagementServiceMock.restoreBackup(null, backupInfo);

    }

}
