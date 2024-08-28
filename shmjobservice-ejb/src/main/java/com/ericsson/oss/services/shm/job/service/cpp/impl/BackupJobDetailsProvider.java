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
package com.ericsson.oss.services.shm.job.service.cpp.impl;

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
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of Backup Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = JobType.BACKUP)
public class BackupJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    private static final String CREATE_CV = "createcv";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupJobDetailsProvider.class);

    static {
        acitvityParameters.put(CREATE_CV, Arrays.asList(JobPropertyConstants.CV_NAME, JobPropertyConstants.CV_IDENTITY, JobPropertyConstants.CV_TYPE, JobPropertyConstants.CV_COMMENT));
    }

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> backupJobDetailsList = new LinkedList<>();

        final List<String> keyList = new ArrayList<String>();
        keyList.add(JobPropertyConstants.BACKUP_NAME);
        keyList.add(JobPropertyConstants.CV_NAME);
        keyList.add(JobPropertyConstants.STARTABLE_CV_NAME);
        keyList.add(JobPropertyConstants.ROLLBACK_CV_NAME);
        keyList.add(JobPropertyConstants.UPLOAD_CV_NAME);

        final Map<String, String> backupValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());

        String backupInfo = null;
        for (int key = 0; key < keyList.size(); key++) {
            if (backupValueMap.containsKey(keyList.get(key)) && backupValueMap.get(keyList.get(key)) != null) {
                backupInfo = backupValueMap.get(keyList.get(key));
                break;
            }
        }

        try {
            if (backupInfo != null && !backupInfo.isEmpty() && backupInfo.contains(JobPropertyConstants.COMMA)) {
                final String backupFileNames[] = backupInfo.split(JobPropertyConstants.COMMA);
                for (int value = 0; value < backupFileNames.length; value++) {
                    final String backupFileName = getBackpNameWithoutSysData(backupFileNames[value]);
                    updateBackupJobDetails(backupFileName, backupJobDetailsList);
                }
            } else {
                final String backupFileName = getBackpNameWithoutSysData(backupInfo);
                updateBackupJobDetails(backupFileName, backupJobDetailsList);
            }
        } catch (RuntimeException ex) {
            LOGGER.error("Exception occured while fetching delete Backup details in Node activites ", ex);
        }
        LOGGER.debug("deleteBackup details for cpp Node activites {} ", backupJobDetailsList.get(0).toString());
        return backupJobDetailsList;
    }

    public String getBackpNameWithoutSysData(String backupFileNameWithSystemData) {
        if (backupFileNameWithSystemData.contains("/")) {
            backupFileNameWithSystemData = backupFileNameWithSystemData.split("\\/")[0];
        } else if (backupFileNameWithSystemData.contains("|")) {
            backupFileNameWithSystemData = backupFileNameWithSystemData.split("\\|")[0];
        }
        return backupFileNameWithSystemData;
    }

    private void updateBackupJobDetails(final String backupFileName, final List<Map<String, String>> backupJobDetailsList) {
        final Map<String, String> backupJobDetailsMap = new TreeMap<String, String>();
        backupJobDetailsMap.put(JobPropertyConstants.DELETED_BACKUP_NAME, backupFileName);
        backupJobDetailsList.add(backupJobDetailsMap);
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.CPP.name(), acitvityParameters);

        return configurationDetails;
    }

}
