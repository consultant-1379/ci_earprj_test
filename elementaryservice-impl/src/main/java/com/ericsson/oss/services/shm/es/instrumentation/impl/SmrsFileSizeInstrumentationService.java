/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.instrumentation.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

public class SmrsFileSizeInstrumentationService {

    private static final Logger logger = LoggerFactory.getLogger(SmrsFileSizeInstrumentationService.class);

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private FileResource fileResource;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    /**
     * Preparing for SMRS file size instrumentation
     * 
     * @param activityJobId
     * @param neType
     * @param backupName
     * @return
     */
    public void addInstrumentationForBackupFileSize(final String nodeName, final String component, final String backupName) {
        try {
            final String neType = networkElementRetrivalBean.getNeType(nodeName);
            final String backupFilePath = getAbsoluteBackupFilePathInSmrs(neType, nodeName, component, backupName);
            logger.debug("NeType:{} Uploaded backupFileName: {}", neType, backupFilePath);
            if (backupFilePath != null && !backupFilePath.isEmpty()) {
                instrumentBackupFileSize(neType, component, backupFilePath);
            } else {
                logger.error("Unable to get the backup file path to instrument the backup filesize, backupFilePath: {} nodeName: {} ,backupName:{} neType: {}", backupFilePath, nodeName, backupName,
                        neType);
            }
        } catch (Exception e) {
            logger.error("Error while instrumenting the backup file size for nodeName: {} backupName:{} due to: {}", nodeName, backupName, e);
        }
    }

    /**
     * SMRS file size instrumentation
     * 
     * @param activityJobId
     * @param neType
     * @param absoluteBackupFilepath
     * @return
     */
    public void instrumentBackupFileSize(final String neType, final String component, final String absoluteBackupFilepath) {
        try {
            final long fileSize = fileResource.getResourceSize(absoluteBackupFilepath);
            if (fileSize != -1) {
                final Map<String, Object> recordEventData = new HashMap<>();
                recordEventData.put(ShmConstants.NETYPE, neType);
                recordEventData.put(ShmConstants.COMPONENT, (component != null ? component : ShmConstants.EMPTY));
                recordEventData.put(ShmConstants.FILE_SIZE, fileSize);
                systemRecorder.recordEventData(SHMEvents.SMRS_BACKUP_FILE_SIZE, recordEventData);
            } else {
                logger.error("Unable to get the backup fileSize from SMRS to instrument for neType: {} absoluteFileURI:{}", neType, absoluteBackupFilepath);
            }
        } catch (Exception ex) {
            logger.error("An exception occured while instrumenting BackupFile Size for neType : {} and absoluteBackupFilepath: {} , Exception is : {}", neType, absoluteBackupFilepath, ex);
        }
    }

    /**
     * finds the backup file corresponding to the selected backup and returns path of the backup file
     * 
     * @param networkElement
     * @param fileResource
     * @param backupName
     * @return backupFilePath
     */
    public String getAbsoluteBackupFilePathInSmrs(final String neType, final String nodeName, final String component, final String backupName) {
        String backupFilePath = "";
        String nodeNameLocation = "";
        String backupLocation = "";
        try {
            final String smrsLocationForNeTypeBackup = smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, smrsRetryPolicies.getSmrsImportRetryPolicy());
            if (smrsLocationForNeTypeBackup != null && !smrsLocationForNeTypeBackup.isEmpty()) {
                if (component != null) {
                    nodeNameLocation = FilenameUtils.concat(smrsLocationForNeTypeBackup, nodeName);
                    backupLocation = FilenameUtils.concat(nodeNameLocation, component);
                } else {
                    backupLocation = FilenameUtils.concat(smrsLocationForNeTypeBackup, nodeName);
                }
                backupFilePath = getBackupFilePath(backupLocation, backupName);
            } else {
                logger.error("Unable to get the SMRS Location For neType: {} , Failed to instrument Backup file size for node name: {} ", neType, nodeName);
            }
        } catch (final Exception e) {
            logger.error("Backup Items reading for {} from SMRS path {} failed , due to:{}", nodeName, nodeNameLocation, e);
        }
        return backupFilePath;
    }

    public String getBackupFilePath(final String backupLocation, final String backupName) {
        final List<String> backupFileNamesList = fileResource.getFileNamesFromDirectory(backupLocation);
        String backupFilePath = "";
        if (backupFileNamesList != null && !backupFileNamesList.isEmpty()) {
            for (final String backupFileName : backupFileNamesList) {
                if (backupFileName.contains(backupName)) {
                    backupFilePath = FilenameUtils.concat(backupLocation, backupFileName);
                    break;
                }
            }
        }
        return backupFilePath;
    }

}
