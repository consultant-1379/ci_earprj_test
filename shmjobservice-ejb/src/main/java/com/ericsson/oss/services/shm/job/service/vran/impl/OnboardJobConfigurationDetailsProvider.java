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
package com.ericsson.oss.services.shm.job.service.vran.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.service.JobConfigurationSummary;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProviderFactory;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * 
 * @author xeswpot
 * 
 */
public class OnboardJobConfigurationDetailsProvider implements JobConfigurationSummary {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardJobConfigurationDetailsProvider.class);

    @Inject
    private ActivityParamMapper activityParamMapper;

    @Inject
    private JobTypeDetailsProviderFactory jobTypeDetailsProviderFactory;

    /**
     * 
     */
    @Override
    public List<JobConfigurationDetails> getJobConfigurationDetails(final JobTemplate jobTemplate, final JobConfiguration jobConfiguration) {
        final List<JobConfigurationDetails> JobConfigurationDetails = new ArrayList<JobConfigurationDetails>();
        final Set<String> neTypes = activityParamMapper.getNeTypes(jobConfiguration.getActivities());
        LOGGER.debug("netypes for the job with template Id: {}", neTypes, jobTemplate.getJobTemplateId());
        for (final String neType : neTypes) {

            final com.ericsson.oss.services.shm.job.activity.JobType jobTypeValue = com.ericsson.oss.services.shm.job.activity.JobType.fromValue(jobTemplate.getJobType().getJobTypeName());
            final JobTypeDetailsProvider jobTypeDetailsProvider = jobTypeDetailsProviderFactory.getJobTypeDetailsProvider(PlatformTypeEnum.vRAN, jobTypeValue);

            if (jobTypeDetailsProvider != null) {
                final JobConfigurationDetails neJobConfigurationDetail = jobTypeDetailsProvider.getJobConfigParamDetails(jobConfiguration, neType);
                neJobConfigurationDetail.setNeType(neType);
                JobConfigurationDetails.add(neJobConfigurationDetail);
            }
        }
        return JobConfigurationDetails;
    }

    /**
     * 
     */
    @Override
    public List<NetworkElement> getNetworkElementsByNeNames(final List<String> neNames, final String capability) {

        LOGGER.debug("Retrieving NFVO(s) : {}", neNames);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        if (neNames != null && !neNames.isEmpty()) {
            LOGGER.debug("Building NetworkElements for : {}", neNames);

            for (final String neName : neNames) {
                final NetworkElement networkElement = new NetworkElement();
                networkElement.setName(neName);
                networkElement.setPlatformType(PlatformTypeEnum.vRAN);
                networkElement.setNeType("NFVO");
                networkElementsList.add(networkElement);
            }
        }
        return networkElementsList;
    }
}
