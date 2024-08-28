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
package com.ericsson.oss.services.shm.job.service.axe.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of Upgrade Job when AXE specific node is selected, which to be displayed on Node activities panel of job details page, summary icon selected for
 * each NEjob selection
 * 
 */

@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.AXE, jobType = JobType.UPGRADE)
public class UpgradeJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private AxeActivityParamsMapper axeActivityParamMapper;

    public static final Logger LOGGER = LoggerFactory.getLogger(UpgradeJobDetailsProvider.class);

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {
        LOGGER.debug("Enter into jobconfigurationdetails for neName {}",neName);
        final List<Map<String, String>> upgradeJobDetailsList = new ArrayList<>();
        final List<Map<String, Object>> neJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NEJOB_PROPERTIES);
        for (final Map<String, Object> neJobProperty : neJobProperties) {
            if (neJobProperty.get(ShmConstants.NE_NAME).equals(neName)) {
                final Map<String, String> upgradeJobDetailsMap = new HashMap<>();
                final List<Map<String, Object>> jobPropertyList = (List<Map<String, Object>>) neJobProperty.get(ShmConstants.JOBPROPERTIES);
                for (final Map<String, Object> jobProperty : jobPropertyList) {
                    final String jobPropertyKey = (String) jobProperty.get(ShmConstants.KEY);
                    final String jobPropertyValue = (String) jobProperty.get(ShmConstants.VALUE);
                    upgradeJobDetailsMap.put(jobPropertyKey, jobPropertyValue);
                }
                upgradeJobDetailsList.add(upgradeJobDetailsMap);
                break;
            }
        }
        return upgradeJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        return axeActivityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.AXE.name());

    }

}
