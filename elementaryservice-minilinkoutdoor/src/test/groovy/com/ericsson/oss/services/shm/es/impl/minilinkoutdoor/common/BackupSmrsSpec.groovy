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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common

import com.ericsson.oss.services.shm.common.ResourceOperations
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.BackupActivityProperties

import static org.mockito.Mockito.when

import org.spockframework.util.Assert

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class BackupSmrsSpec extends CdiSpecification {

    @ObjectUnderTest
    BackupSmrs backupSmrs;

    @MockedImplementation
    ResourceOperations resourceOperations;

    @MockedImplementation
    BackupActivityProperties backupActivityProperties;

    private static final String NODE_NAME = "nodeName";
    private static final String BACKUP_NAME = "backupName";
    private static final String PATH_ON_SERVER = "pathOnFtpServer";
    private static final String CONFIG_FILE_EXTENSION = "cdb";
    private static final String DOT = ".";
    private static final String SLASH = "/";
    private static final String UNDERSCORE = "_";
    private static final String BACKUP_FILE_WITH_PATH = NODE_NAME + SLASH + BACKUP_NAME + UNDERSCORE + NODE_NAME + DOT + CONFIG_FILE_EXTENSION;
    SmrsAccountInfo smrsDetails;

    def setup() {
        smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(PATH_ON_SERVER);
    }

    def "testPrepareBackupDirectory" () {
        given: "initialize"
        backupActivityProperties.getNodeName() >> NODE_NAME
        resourceOperations.isDirectoryExistsWithWritePermissions(PATH_ON_SERVER, NODE_NAME) >> false
        when: "invoke prepare backup directory"
        backupSmrs.prepareBackupDirectory(backupActivityProperties, smrsDetails);
        then : "expect nothing"
    }

    def "testCheckExistenceOfBackupFileSuccess"() {
        given: "initialize"
        backupActivityProperties.getBackupFileWithPath() >> BACKUP_FILE_WITH_PATH
        resourceOperations.fileExists(PATH_ON_SERVER + BACKUP_FILE_WITH_PATH) >> true
        when: "invoke check existence of backup file"
        Boolean result = backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsDetails);
        then: "return value should not be null"
        Assert.notNull(result)
        result == true
    }

    def "testCheckExistenceOfBackupFileFailure"() {
        given: "initialize"
        backupActivityProperties.getNodeName() >> NODE_NAME
        backupActivityProperties.getBackupName() >> BACKUP_NAME
        resourceOperations.fileExists(PATH_ON_SERVER + BACKUP_FILE_WITH_PATH) >> true
        when: "invoke check existence of backup file"
        Boolean result = backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsDetails);
        then: "return value should not be null"
        Assert.notNull(result)
        result == false
    }
}
