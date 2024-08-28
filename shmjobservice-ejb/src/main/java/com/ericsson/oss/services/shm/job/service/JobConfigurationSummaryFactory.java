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

import javax.inject.Inject;

import com.ericsson.oss.services.shm.job.service.vran.impl.OnboardJobConfigurationDetailsProvider;
import com.ericsson.oss.services.shm.jobs.vran.constants.VranConstants;

/**
 * Factory class to provide JobConfigurationSummary implementation based on JobType
 * 
 * @author xeswpot
 * 
 */
public class JobConfigurationSummaryFactory {

    @Inject
    private DefaultJobConfigurationDetailsProvider defaultJobConfigurationDetailsProvider;

    @Inject
    private OnboardJobConfigurationDetailsProvider onboardJobConfigurationSummaryProvider;

    /**
     * Provides appropriate JobConfigurationSummaryProvider
     * 
     * @param jobType
     * @return
     */
    public JobConfigurationSummary getJobConfigurationSummaryProvider(final String jobType) {

        JobConfigurationSummary jobConfigurationSummary = defaultJobConfigurationDetailsProvider;

        if (jobType != null) {
            switch (jobType) {
            case VranConstants.ONBOARD:
                jobConfigurationSummary = onboardJobConfigurationSummaryProvider;
                break;
            case VranConstants.DELETE_SOFTWAREPACKAGE:
                jobConfigurationSummary = onboardJobConfigurationSummaryProvider;
                break;
            default:
                jobConfigurationSummary = defaultJobConfigurationDetailsProvider;
                break;
            }
        }
        return jobConfigurationSummary;
    }
}