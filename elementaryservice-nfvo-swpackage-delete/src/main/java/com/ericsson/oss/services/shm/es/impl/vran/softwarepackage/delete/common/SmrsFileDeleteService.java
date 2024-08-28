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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

import java.io.IOException;
import java.util.*;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.resource.ResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.ra.FileConnection;
import com.ericsson.oss.services.shm.ra.FileConnectionFactory;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

/**
 * Service to delete software package from SMRS
 * 
 * @author xsripod
 * 
 */
@Stateless
public class SmrsFileDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmrsFileDeleteService.class);

    @Resource(shareable = false, lookup = "java:/app/eis/shmJobFileConnector")
    private FileConnectionFactory fileConnectionFactory;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private FileResource fileResource;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    /**
     * Method to perform delete software package from ENM
     * 
     * @param filePath
     * @param activityJobId
     * 
     */
    public void delete(final String filePath, final long activityJobId) throws ResourceException, IOException {
        FileConnection fileConnection = null;
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        String jobLogMessage;
        try {
            final boolean isSoftwarePackagePresentInEnm = fileResource.isDirectoryExists(filePath);
            LOGGER.debug("ActivityJob ID - [{}] : Is the software package present in ENM : {[]}", activityJobId, isSoftwarePackagePresentInEnm);
            fileConnection = fileConnectionFactory.getConnection();
            if (isSoftwarePackagePresentInEnm) {
                fileConnection.deleteEntity(filePath);
                jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_DELETED_FROM_ENM_LOCATION, filePath);
                jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));
            } else {
                systemRecorder.recordEvent("Software package does not exist in ENM", EventLevel.COARSE, "", "", "Software Package might have been deleted accidentally from smrs");
                LOGGER.warn("ActivityJob ID - [{}] : Software Package {} can't be deleted from ENM as it doesn't exist there, smrs directory: {}", activityJobId, filePath,
                        isSoftwarePackagePresentInEnm);
                jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_NOT_IN_ENM_LOCATION, filePath);
                jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));
            }
        } catch (Exception e) {
            jobLogMessage = String.format("Failed to delete software package from ENM. ENM location: \"%s\". ", filePath);
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            LOGGER.error("Failed to delete the software package from ENM {[]}", e.getMessage(), e);
        } finally {
            cleanupFileConnection(fileConnection);
        }
        jobAttributesPersistenceProvider.persistJobLogs(activityJobId, jobLogs);
    }

    private void cleanupFileConnection(final FileConnection fileConnection) {
        if (fileConnection != null) {
            try {
                fileConnection.close();
            } catch (final ResourceException e) {
                LOGGER.error("The FileConnection was not returned to the pool. Cause:", e);
                throw new ServerInternalException("Something went wrong. Please try again later.");
            }
        }
    }

}
