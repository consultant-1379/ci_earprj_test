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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
public class RestoreJobHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestoreJobHandler.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobConfigurationService jobConfigurationService;

    public boolean determineActivityCompletionAndUpdateCurrentProperty(final long activityJobId, final String nodeName, final String propertyToBeUpdated) {

        logger.debug("Going to determine whether download backup activity is completed or not for node {} having activity job id {} and propertyToBeUpdated {}", nodeName, activityJobId,
                propertyToBeUpdated);
        final PersistenceObject activityJob = retrieveJob(activityJobId);
        final List<Map<String, Object>> activityJobProperties = fetchActivityJobProperties(activityJob);
        logger.info("Stored ActivityJobProperties and propertyToBeUpdated for node {} and activityJobId {} are {} and {} respectively.", nodeName, activityJob, activityJobProperties,
                propertyToBeUpdated);
        final boolean isActivityCompleted = checkActivityCompletion(activityJobProperties, propertyToBeUpdated);
        updateActivityJobProperties(activityJob, propertyToBeUpdated, activityJobProperties);
        logger.info("ActivityCompletionStatus for node {} is {}", nodeName, isActivityCompleted);
        return isActivityCompleted;
    }

    private PersistenceObject retrieveJob(final long activityJobId) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final PersistenceObject activityJob = liveBucket.findPoById(activityJobId);
        return activityJob;
    }

    private List<Map<String, Object>> fetchActivityJobProperties(final PersistenceObject activityJob) {
        final List<Map<String, Object>> activityJobProperties = activityJob.getAttribute(ActivityConstants.JOB_PROPERTIES);
        return activityJobProperties;

    }

    private boolean checkActivityCompletion(final List<Map<String, Object>> activityJobProperties, final String propertyToBeUpdated) {
        String propertyToBeChecked = "";

        switch (propertyToBeUpdated) {
        case EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL:
            propertyToBeChecked = EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED;
            break;
        case EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED:
            propertyToBeChecked = EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL;
        }

        if (activityJobProperties != null) {
            for (final Map<String, Object> activityJobProperty : activityJobProperties) {
                if (propertyToBeChecked != null && propertyToBeChecked.equals(activityJobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void updateActivityJobProperties(final PersistenceObject activityJob, final String propertyToBeUpdated, final List<Map<String, Object>> storedActivityJobProperties) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, propertyToBeUpdated, Boolean.toString(true));

        final List<Map<String, Object>> updatedJobPropertyList = jobConfigurationService.getUpdatedJobProperties(jobPropertyList, storedActivityJobProperties);

        activityJob.setAttribute(ActivityConstants.JOB_PROPERTIES, updatedJobPropertyList);

    }
}
