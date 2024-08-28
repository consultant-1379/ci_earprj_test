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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import javax.resource.ResourceException;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FileDeleteUtilBean;
import com.ericsson.oss.services.shm.ra.FileConnection;
import com.ericsson.oss.services.shm.ra.FileConnectionFactory;
import com.ericsson.oss.services.shm.ra.exception.ArchiveEntryNotFoundException;
import com.ericsson.oss.services.shm.ra.exception.InvalidFileFormatException;
import com.ericsson.oss.services.shm.ra.util.ArchiveExpandResponse;

/**
 * Uses the FileConnection API, and handles the zip file extractions
 * 
 * @author xrajeke
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class OssBackupInfoFileReader {

    @Resource(shareable = false, lookup = "java:/app/eis/shmJobFileConnector")
    private FileConnectionFactory fileConnectionFactory;

    @Inject
    private FileDeleteUtilBean fileDeleteUtilBean;

    private static final Logger logger = LoggerFactory.getLogger(OssBackupInfoFileReader.class);

    /**
     * Extracts the given file name from the given zip file and puts into the same location of the zip
     * 
     * @param backupFileAbsolutePath
     * @param fileNameTobeExtracted
     * @return
     */
    public ArchiveExpandResponse extractBackupFile(final String backupFileAbsolutePath, final String fileNameTobeExtracted) {
        ArchiveExpandResponse archiveExpandResponse = null;
        FileConnection fileConnection = null;
        try {
            fileConnection = fileConnectionFactory.getConnection();
            final Set<String> fileTobeExtracted = new HashSet<String>();
            fileTobeExtracted.add(fileNameTobeExtracted);
            archiveExpandResponse = fileConnection.extractFileFromArchive(backupFileAbsolutePath, FilenameUtils.getName(backupFileAbsolutePath), fileTobeExtracted);
        } catch (ResourceException | IOException | InvalidFileFormatException | ArchiveEntryNotFoundException e) {
            logger.error("Unable to parse backupInfo file{{}} due to :{}", backupFileAbsolutePath, e);
            logger.warn("Data from file {{}} will be skipped, as its parsing failed.", backupFileAbsolutePath);
        } finally {
            try {
                if (fileConnection != null) {
                    fileConnection.close();
                }
            } catch (final ResourceException e) {
                logger.error("The FileConnection was not returned to the pool. Cause:", e);
            }
        }
        return archiveExpandResponse;
    }

    /**
     * Deletes the given file
     * 
     * @param absoluteFilePath
     */
    public void deleteTempFiles(final String absoluteFilePath) {
        fileDeleteUtilBean.deleteFile(absoluteFilePath);
    }
}
