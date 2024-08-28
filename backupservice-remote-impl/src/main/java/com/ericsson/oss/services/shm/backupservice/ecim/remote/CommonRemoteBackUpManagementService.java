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

import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;

/**
 * This Interface declares methods which are required to perform any MO related action on Ecim node.
 * 
 */

public interface CommonRemoteBackUpManagementService extends RemoteActivityCallBack {

    boolean precheck(String nodeName, EcimBackupInfo ecimBackupInfo) throws BackupManagementServiceException;

    int executeMoAction(String nodeName, EcimBackupInfo ecimBackupInfo) throws BackupManagementServiceException;

}
