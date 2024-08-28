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
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobConstants;

/**
 * This class is to get the job configuration details of Restore Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.ECIM, jobType = JobType.RESTORE)
public class RestoreJobDetailsProvider implements JobTypeDetailsProvider {

    private static final String BACKUP_NAME = "BACKUP_NAME";
    private static final String BACKUP_FILE_NAME = "BACKUP_FILE_NAME";
    private static final String BACKUP_LOCATION = "BACKUP_LOCATION";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityParamMapper activityParamMapper;

    static {

        acitvityParameters.put(ShmConstants.RESTORE_BACKUP, Arrays.asList(JobPropertyConstants.SECURE_BACKUP_KEY));
    }

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {
        final List<Map<String, String>> restoreJobDetailsList = new LinkedList<>();
        final List<String> keyList = new ArrayList<String>();
        final Map<String, String> restoreJobDetailsMap = new TreeMap<String, String>();
        keyList.add(BACKUP_NAME);
        keyList.add(BACKUP_LOCATION);
        keyList.add(BACKUP_FILE_NAME);
        final Map<String, String> backupValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        final String backupName = backupValueMap.get(BACKUP_NAME);
        final String backupLocation = backupValueMap.get(BACKUP_LOCATION);
        final String backupFileName = backupValueMap.get(BACKUP_FILE_NAME);
        if (backupLocation.equalsIgnoreCase(SHMJobConstants.ENM_LOCATION)) {
            restoreJobDetailsMap.put(SHMJobConstants.BACKUP, backupFileName);
        } else {
            restoreJobDetailsMap.put(SHMJobConstants.BACKUP, backupName);
        }
        restoreJobDetailsMap.put(SHMJobConstants.BACKUPLOCATION, backupLocation);
        restoreJobDetailsList.add(restoreJobDetailsMap);
        return restoreJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.ECIM.name(), acitvityParameters);
        List<ActivityInfo> activityInfoList = configurationDetails.getActivityInfoList();
        if (!activityInfoList.isEmpty()) {
            for (ActivityInfo activityInfo : activityInfoList) {
                if (ShmConstants.RESTORE_BACKUP.equals(activityInfo.getActivityName())) {
                    List<JobProperty> jobPropertyList = activityInfo.getJobProperties();
                    final List<JobProperty> jobProperties = new ArrayList<>(jobPropertyList);
                    for (JobProperty jobProperty : jobProperties) {
                        if (JobPropertyConstants.SECURE_BACKUP_KEY.equals(jobProperty.getKey())) {
                            updateJobPropertiesforSecureRestoreBackup(jobProperty, jobPropertyList, ShmConstants.SECURE_BACKUP, "Yes");
                        }
                    }
                }
            }
        }
        return configurationDetails;
    }

    private void updateJobPropertiesforSecureRestoreBackup(final JobProperty jobProperty, final List<JobProperty> jobPropertyList, final String newKey, final String newValue) {
        final JobProperty newJobProperty = new JobProperty(newKey, newValue);
        jobPropertyList.remove(jobProperty);
        jobPropertyList.add(newJobProperty);
    }

}
