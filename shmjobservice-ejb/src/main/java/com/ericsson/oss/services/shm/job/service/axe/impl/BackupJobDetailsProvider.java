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
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of Backup Job when AXE specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.AXE, jobType = JobType.BACKUP)
public class BackupJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    private static final String INPUT_BACKUP_NAMES = "backupName";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupJobDetailsProvider.class);

    static {
        acitvityParameters.put(ShmConstants.CREATE_BACKUP,
                Arrays.asList(JobPropertyConstants.ROTATE, JobPropertyConstants.OVERWRITE, JobPropertyConstants.SECURE_BACKUP_KEY, JobPropertyConstants.USER_LABEL));
    }

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {
        final List<String> keyList = new ArrayList<>();
        keyList.add(INPUT_BACKUP_NAMES);
        keyList.add(JobPropertyConstants.UPLOAD_BACKUP_DETAILS);

        final Map<String, String> backupValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        String backupNames = backupValueMap.get(INPUT_BACKUP_NAMES);
        final List<Map<String, String>> backupJobDetailsList = new LinkedList<>();
        if (backupNames != null) {
            getBackupNames(backupNames, backupJobDetailsList);

        } else {
            backupNames = backupValueMap.get(JobPropertyConstants.UPLOAD_BACKUP_DETAILS);
            getBackupNames(backupNames, backupJobDetailsList);
        }
        LOGGER.debug("BackupJobDetailsProvider for AXE platform {}", backupJobDetailsList);
        return backupJobDetailsList;
    }

    private void getBackupNames(final String backupNames, final List<Map<String, String>> backupJobDetailsList) {
        if (backupNames != null && !backupNames.isEmpty()) {
            if (backupNames.contains(JobPropertyConstants.COMMA)) {
                final String[] backupFileNames = backupNames.split(JobPropertyConstants.COMMA);
                for (String backupFileName : backupFileNames) {
                    updateBackupJobDetails(backupFileName, backupJobDetailsList);
                }
            } else {
                updateBackupJobDetails(backupNames, backupJobDetailsList);
            }
        }
    }

    private void updateBackupJobDetails(final String backupFileName, final List<Map<String, String>> backupJobDetailsList) {
        final Map<String, String> backupJobDetailsMap = new TreeMap<>();
        backupJobDetailsMap.put(INPUT_BACKUP_NAMES, backupFileName);
        backupJobDetailsList.add(backupJobDetailsMap);
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfiguration, final String neType) {
        final JobConfigurationDetails jobConfigurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfiguration, neType, PlatformTypeEnum.AXE.name(), acitvityParameters);
        List<ActivityInfo> activityInfoList = jobConfigurationDetails.getActivityInfoList();
        for (ActivityInfo activityInfo : activityInfoList) {
            if (ShmConstants.CREATE_BACKUP.equals(activityInfo.getActivityName())) {
                List<JobProperty> jobPropertyList = activityInfo.getJobProperties();
                final List<JobProperty> jobProperties = new ArrayList<>(jobPropertyList);
                for (JobProperty jobProperty : jobProperties) {
                    if (JobPropertyConstants.SECURE_BACKUP_KEY.equals(jobProperty.getKey())) {
                        updateJobPropertiesforSecureBackup(jobProperty, jobPropertyList, ShmConstants.SECURE_BACKUP, "Yes");
                    } else if (JobPropertyConstants.USER_LABEL.equals(jobProperty.getKey())) {
                        updateJobPropertiesforSecureBackup(jobProperty, jobPropertyList, ShmConstants.DISPLAY_USER_LABEL, jobProperty.getValue());
                    }
                }
            }
        }
        return jobConfigurationDetails;
    }

    private void updateJobPropertiesforSecureBackup(final JobProperty jobProperty, final List<JobProperty> jobPropertyList, final String newKey, final String newValue) {
        final JobProperty newJobProperty = new JobProperty(newKey, newValue);
        jobPropertyList.remove(jobProperty);
        jobPropertyList.add(newJobProperty);
    }
}
