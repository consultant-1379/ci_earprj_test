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
package com.ericsson.oss.services.shm.backupservice.remote.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Remote Interface for performing backup management operations on a network element.
 */
@EService
@Remote
public interface BackupManagementServiceRemote {

    /**
     * Creates a backup on the specified Network Element.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for create backup operation.In case of CPP - the backupInfo must have the name set , identity and comment are optional. In case of ECIM - the
     *            backupInfo must have the domain and type set , name is optional.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     * @deprecated this method to support the createBackup activity in asynchronous way by using the method {@create(String neName, BackupInfo backupInfo)}
     */
    @Deprecated
    void createBackup(String neName, BackupInfo backupInfo) throws BackupManagementServiceException;

    /**
     * uploads the backup from the specified Network Element to ENM SMRS file store.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for upload backup operation.In case of CPP - the backupInfo must only have the name set.In case of ECIM - the backupInfo must have the name, domain
     *            and type set.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     * @deprecated this method to support the uploadBackup activity in asynchronous way by using the method {@export(String neName, BackupInfo backupInfo)}
     */
    @Deprecated
    void uploadBackup(String neName, BackupInfo backupInfo) throws BackupManagementServiceException;

    /**
     * Sets the specified backup as startable on the NE. Not supported for ECIM - invocation will result in BackupOperationException.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for upload backup operation.In case of CPP - the backupInfo must only have the name set.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     */
    void setStartableBackup(String neName, BackupInfo backupInfo) throws BackupManagementServiceException;

    /**
     * Sets the specified backup as first in the roll back list on the NE. Not supported for ECIM - invocation will result in BackupOperationException.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - the backupInfo must only have the name set.In case of CPP - the backupInfo must only have the name set.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     */
    void setBackupFirstInRollBackList(String neName, BackupInfo backupInfo) throws BackupManagementServiceException;

    /**
     * Deletes a backup on the specified Network Element.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for delete backup operation. In case of CPP - the backupInfo must have the name set. In case of ECIM - backupInfo must have the backup name, domain
     *            and type set.
     * @param deleteBackupOptions
     *            - Holds options for delete backup operation
     * @throws BackupManagementServiceException
     *             - in case where the operatio n has failed.
     */

    void deleteBackup(String neName, BackupInfo backupInfo, DeleteBackupOptions deleteBackupOptions) throws BackupManagementServiceException;

    /**
     * Restores the - previously created backup on NE and confirms the restore operation. Not supported for CPP - invocation will result in BackupOperationException.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for restore backup operation. In case of ECIM - the backupInfo must have the name , domain and type set.
     * @throws BackupManagementServiceException
     */
    void restoreBackup(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException;

    /**
     * Creates a backup on the specified Network Element in asynchronous way
     * <p>
     * If create backup request accepted, a unique message ID which is job name will be returned.If create backup request rejected, {@link BackupManagementServiceException} will be thrown that details
     * the reason for rejection.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for create backup operation.In case of CPP - the backupInfo must have the name set , identity and comment are optional. In case of ECIM - the
     *            backupInfo must have the domain and type set , name is optional.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     * @return JobName in response
     */

    String create(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException;

    /**
     * uploads the backup from the specified Network Element to ENM SMRS file store in asynchrnous way
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for upload backup operation.In case of CPP - the backupInfo must only have the name set.In case of ECIM - the backupInfo must have the name, domain
     *            and type set.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     * @return JobName in response
     */
    String export(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException;
}
