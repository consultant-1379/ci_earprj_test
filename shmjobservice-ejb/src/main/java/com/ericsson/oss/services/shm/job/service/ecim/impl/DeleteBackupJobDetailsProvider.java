/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.ecim.impl;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.shm.backup.constants.CppBackupConstants;

/**
 * This class is to get the job configuration details of Delete Backup Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.ECIM, jobType = JobType.DELETEBACKUP)
public class DeleteBackupJobDetailsProvider implements JobTypeDetailsProvider {

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityParamMapper activityParamMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupJobDetailsProvider.class);

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {
        final List<Map<String, String>> deleteBackupJobDetailsList = new LinkedList<>();
        final List<String> keyList = new ArrayList<String>();
        keyList.add(CppBackupConstants.BACKUP_NAME_PROPERTY);
        final Map<String, String> deleteBackupValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        final String deleteBackupInfo = deleteBackupValueMap.get(CppBackupConstants.BACKUP_NAME_PROPERTY);
        try {
            if (deleteBackupInfo != null && !deleteBackupInfo.isEmpty() && deleteBackupInfo.contains(JobPropertyConstants.COMMA)) {
                final String deleteBackupNameandLocations[] = deleteBackupInfo.split(JobPropertyConstants.COMMA);
                for (int key = 0; key < deleteBackupNameandLocations.length; key++) {
                    final String deleteBackupNameandLocation = deleteBackupNameandLocations[key];
                    getBackupNameAndLocation(deleteBackupNameandLocation, deleteBackupJobDetailsList);
                }
            } else {
                getBackupNameAndLocation(deleteBackupInfo, deleteBackupJobDetailsList);
            }
        } catch (RuntimeException runtimeException) {
            LOGGER.error("Exception occured while fetching delete Backup details in ECIM Node activites {} ", runtimeException.getMessage());
        }
        LOGGER.debug("deleteBackup ecim Node activites Details {} ", deleteBackupJobDetailsList);
        return deleteBackupJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.ECIM.name(), acitvityParameters);
        for (final JobProperty jobProperty : configurationDetails.getJobProperties()) {
            if (jobProperty.getKey().equals(ActivityConstants.ROLL_BACK)) {
                configurationDetails.setJobProperties(Arrays.asList(new JobProperty(jobProperty.getKey(), "NA")));
            }
        }
        return configurationDetails;
    }

    private static void getBackupNameAndLocation(final String deleteBackupNameandLocation, final List<Map<String, String>> deleteBackupJobDetailsList) {
        final Map<String, String> deleteBackupJobDetailsMap = new TreeMap<String, String>();
        final String BackupNameandLocation[] = deleteBackupNameandLocation.split("\\|");
        final String deleteBackupName = BackupNameandLocation[0];
        final String deleteBackupLocation = BackupNameandLocation[3];
        if (deleteBackupLocation.equalsIgnoreCase(InventoryConstants.LOCATION_ENM)) {
            deleteBackupJobDetailsMap.put(JobPropertyConstants.DELETED_BACKUP_FILE, deleteBackupName);
        } else {
            deleteBackupJobDetailsMap.put(JobPropertyConstants.DELETED_BACKUP_NAME, deleteBackupName);
        }

        deleteBackupJobDetailsMap.put(JobPropertyConstants.LOCATION, deleteBackupLocation);
        deleteBackupJobDetailsList.add(deleteBackupJobDetailsMap);
    }

}
