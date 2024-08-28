/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.axe.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.inventory.constants.InventoryConstants;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of Backup Job when AXE specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.AXE, jobType = JobType.DELETEBACKUP)
public class DeleteBackupJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    public static final String BACKUP_NAME = "BACKUP_NAME";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupJobDetailsProvider.class);

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> deleteBackupJobDetailsList = new LinkedList<>();

        final Map<String, String> deleteBackupValueMap = jobPropertyUtils.getPropertyValue(Collections.singletonList(BACKUP_NAME), jobConfigurationDetails, neName, neType, platformType.toString());
        final String deleteBackupInfo = deleteBackupValueMap.get(BACKUP_NAME);
        try {
            if (deleteBackupInfo != null && deleteBackupInfo.contains(JobPropertyConstants.COMMA)) {
                final String[] deleteBackupNameandLocations = deleteBackupInfo.split(JobPropertyConstants.COMMA);
                for (int key = 0; key < deleteBackupNameandLocations.length; key++) {
                    final String deleteBackupNameandLocation = deleteBackupNameandLocations[key];
                    getBackupNameAndLocation(deleteBackupNameandLocation, deleteBackupJobDetailsList);
                }
            } else {
                getBackupNameAndLocation(deleteBackupInfo, deleteBackupJobDetailsList);
            }
        } catch (RuntimeException runtimeException) {
            LOGGER.error("Exception occured while fetching delete Backup details in AXE Node activites {} ", runtimeException.getMessage());
        }
        
        LOGGER.debug("deleteBackupJobDetailsList for AXE platform {}", deleteBackupJobDetailsList);
        return deleteBackupJobDetailsList;
    }

    private static void getBackupNameAndLocation(final String deleteBackupNameandLocation, final List<Map<String, String>> deleteBackupJobDetailsList) {
        final Map<String, String> deleteBackupJobDetailsMap = new TreeMap<>();
        final String[] backupNameandLocation = deleteBackupNameandLocation.split(ShmConstants.BACKUP_LOCATION_SPLIT_DELIMITER);
        final String deleteBackupName = backupNameandLocation[0];
        final String deleteBackupLocation = backupNameandLocation[1];
        if (deleteBackupLocation.equalsIgnoreCase(InventoryConstants.LOCATION_ENM)) {
            deleteBackupJobDetailsMap.put(JobPropertyConstants.DELETED_BACKUP_FILE, deleteBackupName);
        } else {
            deleteBackupJobDetailsMap.put(JobPropertyConstants.DELETED_BACKUP_NAME, deleteBackupName);
        }

        deleteBackupJobDetailsMap.put(JobPropertyConstants.LOCATION, deleteBackupLocation);
        deleteBackupJobDetailsList.add(deleteBackupJobDetailsMap);
    }
    
    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {
        return activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.AXE.name(), acitvityParameters);
    }

}
