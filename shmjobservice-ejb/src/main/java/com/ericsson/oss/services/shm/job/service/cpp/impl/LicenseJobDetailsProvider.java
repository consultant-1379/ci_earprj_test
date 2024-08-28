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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
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
 * This class is to get the job configuration details of License Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = JobType.LICENSE)
public class LicenseJobDetailsProvider implements JobTypeDetailsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseJobDetailsProvider.class);

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityParamMapper activityParamMapper;

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> licenseJobDetailsList = new LinkedList<>();
        final Map<String, String> licenseJobDetailsMap = new TreeMap<String, String>();
        final List<String> keyList = new ArrayList<String>();

        keyList.add(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH);
        final Map<String, String> licenseValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        String licenseKeyFilePath = licenseValueMap.get(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH);
        if (licenseKeyFilePath == null) {
            final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmJobConstants.JOBPROPERTIES);
            for (Map<String, Object> JobProperty : neJobPropertyList) {
                if (JobProperty.get(JobPropertyConstants.KEY) == JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH) {
                    licenseKeyFilePath = (String) JobProperty.get(JobPropertyConstants.VALUE);
                }
            }
        }
        if (licenseKeyFilePath != null) {
            final String licenseKeyFileName = licenseKeyFilePath.substring(licenseKeyFilePath.lastIndexOf("/") + 1, licenseKeyFilePath.length());
            LOGGER.debug("CPP Node activites licenseKeyFileName {} ", licenseKeyFileName);
            licenseJobDetailsMap.put(JobPropertyConstants.DISPLAYNAME_FOR_LICENSE_KEY_FILE, licenseKeyFileName);
            licenseJobDetailsList.add(licenseJobDetailsMap);
        }
        return licenseJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.CPP.name(), acitvityParameters);
        return configurationDetails;
    }

}
