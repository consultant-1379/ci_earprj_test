/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.minilink.backup;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.MINI_LINK_INDOOR;
import static com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants.BACKUP_ACCOUNT;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupActivityProperties;

/**
 * This bean gives an API for the SMRS related operations corresponding to creating a MINI-LINK backup.
 */
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BackupSmrs {

    @Inject
    private SmrsFileStoreService smrsFileStoreService;

    @Inject
    private ResourceOperations resourceOperations;

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    /**
     * Creates a directory with the node's name in the MINI-LINK backup directory if it does not exist.
     * 
     * @param backupActivityProperties
     */
    public void prepareBackupDirectory(final BackupActivityProperties backupActivityProperties) {
        final String nodeName = backupActivityProperties.getNodeName();
        final SmrsAccountInfo smrsAccountInfo = smrsFileStoreService.getSmrsDetails(BACKUP_ACCOUNT, MINI_LINK_INDOOR, nodeName);
        final String pathOnServer = smrsAccountInfo.getPathOnServer();
        if (!resourceOperations.isDirectoryExistsWithWritePermissions(pathOnServer, nodeName)) {
            resourceOperations.createDirectory(pathOnServer, nodeName);
        }
    }

    /**
     * Checks whether the bakcup file has been uploaded.
     * 
     * @param backupActivityProperties
     * @return
     */
    public boolean checkExistenceOfBackupFile(final BackupActivityProperties backupActivityProperties) {
        LOGGER.debug("In checkExistenceOfBackupFile");
        final SmrsAccountInfo smrsAccountInfo = smrsFileStoreService.getSmrsDetails(BACKUP_ACCOUNT, MINI_LINK_INDOOR, backupActivityProperties.getNodeName());
        final String fileUri = smrsAccountInfo.getPathOnServer() + backupActivityProperties.getBackupFileWithPath();
        LOGGER.debug("Backup file URI: {}", fileUri);
        return resourceOperations.fileExists(fileUri);
    }

}
