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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of Upgrade Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */

@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = JobType.UPGRADE)
public class UpgradeJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    private static final String INSTALL = "install";
    private static final String UPGRADE = "upgrade";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    static {
        acitvityParameters.put(INSTALL, Arrays.asList(JobPropertyConstants.FORCE_INSTALL, JobPropertyConstants.SELECTIVE_INSTALL));
        acitvityParameters.put(UPGRADE, Arrays.asList(JobPropertyConstants.REBOOTNODEUPGRADEPARAM));
    }

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> upgradeJobDetailsList = new LinkedList<>();
        final Map<String, String> upgradeJobDetailsMap = new TreeMap<String, String>();
        upgradeJobDetailsList.add(upgradeJobDetailsMap);
        return upgradeJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.CPP.name(), acitvityParameters);

        return configurationDetails;
    }
}
