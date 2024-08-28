/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.BackupActivityProperties;

@RunWith(MockitoJUnitRunner.class)
public class BackupSmrsTest {

    private static final String NODE_NAME = "nodeName";
    private static final String BACKUP_NAME = "backupName";
    private static final String PATH_ON_SERVER = "pathOnFtpServer";
    private static final String CONFIG_FILE_EXTENSION = "cdb";
    private static final String DOT = ".";
    private static final String SLASH = "/";
    private static final String UNDERSCORE = "_";
    private static final String BACKUP_FILE_WITH_PATH = NODE_NAME + SLASH + BACKUP_NAME + UNDERSCORE + NODE_NAME + DOT + CONFIG_FILE_EXTENSION;

    @InjectMocks
    private BackupSmrs objectUnderTest;

    @Mock
    private ResourceOperations resourceOperations;

    @Mock
    private BackupActivityProperties backupActivityProperties;

    @Test
    public void testPrepareBackupDirectory() {
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(PATH_ON_SERVER);
        when(backupActivityProperties.getNodeName()).thenReturn(NODE_NAME);
        doNothing().when(resourceOperations).createDirectory(PATH_ON_SERVER, NODE_NAME);
        when(resourceOperations.isDirectoryExistsWithWritePermissions(PATH_ON_SERVER, NODE_NAME)).thenReturn(false);
        objectUnderTest.prepareBackupDirectory(backupActivityProperties, smrsDetails);
    }

    @Test
    public void testCheckExistenceOfBackupFileSuccess() {
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(PATH_ON_SERVER);
        final String fileUri = PATH_ON_SERVER + BACKUP_FILE_WITH_PATH;
        when(backupActivityProperties.getBackupFileWithPath()).thenReturn(BACKUP_FILE_WITH_PATH);
        when(resourceOperations.fileExists(fileUri)).thenReturn(true);
        assertTrue(objectUnderTest.checkExistenceOfBackupFile(backupActivityProperties, smrsDetails));
    }

    @Test
    public void testCheckExistenceOfBackupFileFailure() {
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(PATH_ON_SERVER);
        when(backupActivityProperties.getNodeName()).thenReturn(NODE_NAME);
        when(backupActivityProperties.getBackupName()).thenReturn(BACKUP_NAME);
        final String fileUri = PATH_ON_SERVER + BACKUP_FILE_WITH_PATH;
        when(resourceOperations.fileExists(fileUri)).thenReturn(true);
        assertFalse(objectUnderTest.checkExistenceOfBackupFile(backupActivityProperties, smrsDetails));
    }

}
