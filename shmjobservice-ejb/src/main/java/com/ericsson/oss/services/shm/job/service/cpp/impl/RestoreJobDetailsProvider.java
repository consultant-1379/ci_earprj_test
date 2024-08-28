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

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobConstants;

/**
 * This class is to get the job configuration details of Restore Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = JobType.RESTORE)
public class RestoreJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    private static final String CV_LOCATION = "CV_LOCATION";
    private static final String CV_NAME = "CV_NAME";
    private static final String CV_FILE_NAME = "CV_FILE_NAME";

    private static final String INSTALL = "install";
    private static final String RESTORE = "restore";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    static {

        acitvityParameters.put(INSTALL, Arrays.asList(JobPropertyConstants.INSTALL_MISSING_UPGRADE_PACKAGES, JobPropertyConstants.REPLACE_CORRUPTED_UPGRADE_PACKAGES));
        acitvityParameters.put(RESTORE, Arrays.asList(JobPropertyConstants.AUTO_CONFIGURATION, JobPropertyConstants.FORCED_RESTORE));
    }

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> restoreJobDetailsList = new LinkedList<>();
        final List<String> keyList = new ArrayList<String>();
        final Map<String, String> restoreJobDetailsMap = new TreeMap<String, String>();
        keyList.add(CV_NAME);
        keyList.add(CV_LOCATION);
        keyList.add(CV_FILE_NAME);
        final Map<String, String> backupValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        final String backupName = backupValueMap.get(CV_NAME);
        final String backupLocation = backupValueMap.get(CV_LOCATION);
        final String backupFileName = backupValueMap.get(CV_FILE_NAME);
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

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.CPP.name(), acitvityParameters);
        return configurationDetails;
    }

}
