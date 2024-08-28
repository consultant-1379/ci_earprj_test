/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * 
 * @author xeswpot
 * 
 */
public class DefaultJobConfigurationDetailsProvider implements JobConfigurationSummary {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobConfigurationDetailsProvider.class);

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private ActivityParamMapper activityParamMapper;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    private JobTypeDetailsProviderFactory jobTypeDetailsProviderFactory;

    @Inject
    private JobCapabilityProvider capabilityProvider;

    /**
     * Returns list of {@link JobConfigurationDetails} for the selected job.
     * 
     * @param jobTemplate
     *            {@link JobTemplate}
     * @param jobConfiguration
     *            {@link JobConfiguration}
     * @param jobType
     *            JobType
     * @return List of {@link JobConfigurationDetails}
     */
    @Override
    public List<JobConfigurationDetails> getJobConfigurationDetails(final JobTemplate jobTemplate, final JobConfiguration jobConfiguration) {

        LOGGER.debug("Retrieving job configuration details for the job with ID: {} and jobType: {}", jobTemplate.getJobTemplateId(), jobTemplate.getJobType());
        final List<JobConfigurationDetails> jobConfigurationDetails = new ArrayList<JobConfigurationDetails>();
        final Set<String> neTypes = activityParamMapper.getNeTypes(jobConfiguration.getActivities());

        for (final String neType : neTypes) {
            final String capability = capabilityProvider.getCapability(JobTypeEnum.getJobType(jobTemplate.getJobType().name()));
            final PlatformTypeEnum platformTypeEnum = platformTypeProviderImpl.getPlatformTypeBasedOnCapability(neType, capability);

            final com.ericsson.oss.services.shm.job.activity.JobType jobTypeValue = com.ericsson.oss.services.shm.job.activity.JobType.fromValue(jobTemplate.getJobType().getJobTypeName());
            final JobTypeDetailsProvider jobTypeDetailsProvider = jobTypeDetailsProviderFactory.getJobTypeDetailsProvider(platformTypeEnum, jobTypeValue);

            if (jobTypeDetailsProvider != null) {
                final JobConfigurationDetails neJobConfigurationDetail = jobTypeDetailsProvider.getJobConfigParamDetails(jobConfiguration, neType);
                neJobConfigurationDetail.setNeType(neType);
                jobConfigurationDetails.add(neJobConfigurationDetail);
            }
        }
        return jobConfigurationDetails;
    }

    /**
     * Returns list of {@link NetworkElement}s for selected network elements and capability based on SHM functionality.
     * 
     * @param neNames
     *            Network element names
     * @param capability
     */
    @Override
    public List<NetworkElement> getNetworkElementsByNeNames(final List<String> neNames, final String capability) {

        LOGGER.debug("Retrieving NetworkElements : {}", neNames);

        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        if (neNames != null && !neNames.isEmpty()) {

            networkElements = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNames, capability);
        }
        LOGGER.debug("Retrieved NetworkElements : {}", networkElements.size());
        return networkElements;
    }

}
