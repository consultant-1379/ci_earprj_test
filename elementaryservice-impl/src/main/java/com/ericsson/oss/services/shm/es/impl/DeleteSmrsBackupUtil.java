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
package com.ericsson.oss.services.shm.es.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;

/**
 * This class performs the Delete Backup Activity on SMRS location based on user request.
 * 
 */

@Stateless
@Traceable
public class DeleteSmrsBackupUtil {

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private FileResource fileResource;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSmrsBackupUtil.class);

    /**
     * This method performs the Delete operation on Backup File stored in Smrs location.
     * 
     * @param nodeName
     * @param backupName
     * @return boolean
     */

    public boolean deleteBackupOnSmrs(final String nodeName, final String backupName, final String neType) {
        String backupFilePath = "";
        String filePath = "";
        boolean isBackupDeleted = false;
        try {
            final String smrsHomeDirectory = smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, smrsRetryPolicies.getSmrsImportRetryPolicy());
            filePath = FilenameUtils.concat(smrsHomeDirectory, nodeName);
            LOGGER.debug("SmrsHomeDirectory {} And FilePath {} for the Backup to be Deleted", smrsHomeDirectory, filePath);
            final List<String> backupFileNames = fileResource.getFileNamesFromDirectory(filePath);
            for (final String backupFileName : backupFileNames) {
                if (backupName.equals(backupFileName)) {
                    backupFilePath = FilenameUtils.concat(filePath, backupFileName);
                    LOGGER.debug("AbsoluteFilePath for the Backup to be Deleted {}", backupFilePath);
                    isBackupDeleted = fileResource.delete(backupFilePath);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Delete Backup for {} from SMRS path {} failed , due to:{}", nodeName, filePath, e);
        }
        LOGGER.debug("Backup [{}] deletion status {} from Smrs", backupName, isBackupDeleted);
        return isBackupDeleted;
    }

    /**
     * This method checks whether backup exists in Smrs location.
     * 
     * @param nodeName
     * @param backupName
     * @return boolean
     */

    public boolean isBackupExistsOnSmrs(final String nodeName, final String backupName, final String neType) {
        String filePath = "";
        boolean isBackupExists = false;
        try {
            final String smrsHomeDirectory = smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, smrsRetryPolicies.getSmrsImportRetryPolicy());
            filePath = FilenameUtils.concat(smrsHomeDirectory, nodeName);
            LOGGER.debug("Checking whether the backup:{} is present under directory:{}", backupName, filePath);
            final List<String> backupFileNames = fileResource.getFileNamesFromDirectory(filePath);
            for (final String backupFileName : backupFileNames) {
                if (backupName.equals(backupFileName)) {
                    isBackupExists = true;
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Backup Items reading for {} from SMRS path {} failed , due to:{}", nodeName, filePath, e);
        }
        LOGGER.info("Is backup file:{} under directory:{} present ?:{}", backupName, filePath, isBackupExists);
        return isBackupExists;
    }

    /**
     * This method returns the NetowrkElement object corresponding to the input node name.
     * 
     * @param nodeName
     * @return netowrkElement
     * @throws MoNotFoundException
     */
    public NetworkElement getNetworkElement(final String nodeName) throws MoNotFoundException {
        final List<String> neNames = new ArrayList<String>();
        neNames.add(nodeName);
        final List<NetworkElement> networkElements = fdnServiceBean.getNetworkElementsByNeNames(neNames, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
        if (networkElements.isEmpty()) {
            throw new MoNotFoundException("Network element is not found for the node name=" + nodeName);
        }
        return networkElements.get(0);
    }
}
