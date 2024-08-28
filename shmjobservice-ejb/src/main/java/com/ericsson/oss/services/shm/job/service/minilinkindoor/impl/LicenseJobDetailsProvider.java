package com.ericsson.oss.services.shm.job.service.minilinkindoor.impl;

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

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
 * This class is to get the job configuration details of License Job when Mini-Link-Indoor specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.MINI_LINK_INDOOR, jobType = JobType.LICENSE)
public class LicenseJobDetailsProvider implements JobTypeDetailsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseJobDetailsProvider.class);

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityParamMapper activityParamMapper;

    private static final Map<String, List<String>> acitvityParameters = new HashMap<>();

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> licenseJobDetailsList = new LinkedList<>();
        final Map<String, String> licenseJobDetailsMap = new TreeMap<>();
        final List<String> keyList = new ArrayList<>();

        keyList.add(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH);
        final Map<String, String> licenseValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        final String licenseKeyFilePath = licenseValueMap.get(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH);
        final String licenseKeyFileName = licenseKeyFilePath.substring(licenseKeyFilePath.lastIndexOf('/') + 1, licenseKeyFilePath.length());
        LOGGER.debug("Mini-Link-Indoor Node activites licenseKeyFileName {} ", licenseKeyFileName);
        licenseJobDetailsMap.put(JobPropertyConstants.DISPLAYNAME_FOR_LICENSE_KEY_FILE, licenseKeyFileName);
        licenseJobDetailsList.add(licenseJobDetailsMap);
        return licenseJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {
        return activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.MINI_LINK_INDOOR.name(), acitvityParameters);
    }

}

