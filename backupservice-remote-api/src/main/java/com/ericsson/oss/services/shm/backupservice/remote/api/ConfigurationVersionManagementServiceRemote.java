package com.ericsson.oss.services.shm.backupservice.remote.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Provides functions to perform actions for CV management on node
 */

@EService
@Remote
public interface ConfigurationVersionManagementServiceRemote {

    /**
     * Method for creation of CV. neName-Network Element name, identity-Identifier for the CV, comment-Comment for the CV,
     * 
     * @deprecated this method to support the CPP createBackup activity in asynchronous way by using the method {@create(String neName, BackupInfo backupInfo)} of the remote interface
     *             {@BackupManagementServiceRemote}
     */
    @Deprecated
    boolean createCV(String neName, String cvName, String identity, String comment) throws CVOperationRemoteException;

    /**
     * upload CV
     */
    boolean uploadCV(String neName, String cvName) throws CVOperationRemoteException;

    /**
     * Setting CV as startable CV
     */
    boolean setStartableCV(String neName, String cvName) throws CVOperationRemoteException;

    /**
     * Setting CV first in rollback list
     */
    boolean setCVFirstInRollBackList(String neName, String cvName) throws CVOperationRemoteException;

}
