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

package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmDataDescriptorParser;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.ra.util.ArchiveExpandResponse;

/**
 * Uses the FileConnection API, and reads uplist.xml file
 * 
 * @author xrajeke
 */
@Stateless
public class EcimOssBackupItemsReader {

    private static final Logger logger = LoggerFactory.getLogger(EcimOssBackupItemsReader.class);

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private OssBackupInfoFileReader ossFileManager;

    @Inject
    private SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    private FileResource fileResource;

    /**
     * File name that needs to be parsed for OSS(SMRS) data
     */
    private static final String BACKUPINFO_XML = "backupinfo.xml";

    /**
     * returns the attributes map of the backup after parsing xml file from the smrs
     * 
     * @param networkElement
     * @param ossDataXmlParser
     * @param fileResource
     * @param backupName
     * @return ossAttributes
     */
    public Map<String, Object> getBackupItems(final NetworkElementData networkElement, final BrmDataDescriptorParser ossDataXmlParser, final String backupName, final String nodeName) {
        //collect all the backup files under node location from SMRS
        logger.debug("BackupName under node location from SMRS : {}", backupName);
        Map<String, Object> ossAttributes = new HashMap<String, Object>();
        final boolean isValidationRequired = false;
        final String backupInfoFilePath = getBackupsPresentInSmrs(networkElement, backupName, nodeName);
        if (backupInfoFilePath != null && (backupInfoFilePath.length() != 0)) {
            final String backupFileAbsolutePath = backupInfoFilePath;
            final ArchiveExpandResponse archiveExpandResponse = ossFileManager.extractBackupFile(backupFileAbsolutePath, BACKUPINFO_XML);
            if (archiveExpandResponse != null) {
                logger.debug("File[{}] is extracted into {}, from Archive[{}] ", BACKUPINFO_XML, archiveExpandResponse.getFileSavePath(), backupFileAbsolutePath);
                ossAttributes = ossDataXmlParser.parse(fileResource.getBytes(archiveExpandResponse.getFileSavePath()), isValidationRequired);
                ossFileManager.deleteTempFiles(archiveExpandResponse.getFileSavePath());
            }

        }
        return ossAttributes;
    }

    /**
     * finds the backup file corresponding to the selected backup and returns path of the backup file
     * 
     * @param networkElement
     * @param fileResource
     * @param backupName
     * @return backupFilePath
     */
    private String getBackupsPresentInSmrs(final NetworkElementData networkElement, final String backupName, final String nodeName) {
        String backupFilePath = "";
        String nodeFilePath = "";
        try {
            final String smrsHomeDirectory = smrsServiceUtil.getSmrsPath(networkElement.getNeType(), SmrsServiceConstants.BACKUP_ACCOUNT, smrsRetryPolicies.getSmrsImportRetryPolicy());
            nodeFilePath = FilenameUtils.concat(smrsHomeDirectory, nodeName);
            final List<String> backupFileNamesList = fileResource.getFileNamesFromDirectory(nodeFilePath);
            if (backupFileNamesList != null && !backupFileNamesList.isEmpty()) {
                for (final String backupFileName : backupFileNamesList) {
                    logger.debug("The backupfileresource is {}", backupFileName);
                    if (backupFileName.contains(backupName)) {
                        backupFilePath = (FilenameUtils.concat(nodeFilePath, backupFileName));
                    }
                }
            }
        } catch (final Exception e) {
            logger.error("Backup Items reading for {} from SMRS path {} failed , due to:{}", nodeName, nodeFilePath, e.getMessage());
        }
        return backupFilePath;
    }

    /**
     * converts the input date to the ECIM date format
     * 
     * @param createdDate
     * @return formatted createdDate
     */
    public static String formatDate(final Date createdDate) {
        String formattedDate = "";
        if (createdDate != null) {
            final DateFormat dateFormat = new SimpleDateFormat(EcimBackupConstants.ECIM_DATE_FORMAT);
            formattedDate = dateFormat.format(createdDate);
        }
        return formattedDate;
    }
}
