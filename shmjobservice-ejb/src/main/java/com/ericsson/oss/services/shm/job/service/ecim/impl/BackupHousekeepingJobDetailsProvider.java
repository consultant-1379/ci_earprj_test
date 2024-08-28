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
package com.ericsson.oss.services.shm.job.service.ecim.impl;

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

@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.ECIM, jobType = JobType.BACKUP_HOUSEKEEPING)
public class BackupHousekeepingJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    private static final String DELETE_BACKUP = "deletebackup";

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();

    static {
        acitvityParameters.put(DELETE_BACKUP, Arrays.asList(JobPropertyConstants.MAX_BACKUPS_TO_KEEP_ON_NODE));
    }

    /**
     * 
     */
    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfiguration, final PlatformTypeEnum platformType, final String neType, final String neName) {
        final List<Map<String, String>> housekeepingJobDetailsList = new LinkedList<>();
        final Map<String, String> housekeepingJobDetailsMap = new TreeMap<String, String>();
        housekeepingJobDetailsList.add(housekeepingJobDetailsMap);
        return housekeepingJobDetailsList;
    }

    /**
     * 
     */
    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        final JobConfigurationDetails configurationDetails = activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.CPP.name(), acitvityParameters);
        return configurationDetails;
    }

}
