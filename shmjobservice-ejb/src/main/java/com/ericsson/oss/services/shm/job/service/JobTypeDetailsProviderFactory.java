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
package com.ericsson.oss.services.shm.job.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeQualifier;

@ApplicationScoped
public class JobTypeDetailsProviderFactory {

    @Inject
    @Any
    private Instance<JobTypeDetailsProvider> jobTypeDetailsProvider;

    public JobTypeDetailsProvider getJobTypeDetailsProvider(final PlatformTypeEnum platformType, final JobType jobType) {
        final Instance<JobTypeDetailsProvider> jobTypeDetailProvider = jobTypeDetailsProvider.select(new PlatformJobTypeQualifier(platformType, jobType));
        if (jobTypeDetailProvider != null && !jobTypeDetailProvider.isUnsatisfied()) {
            return jobTypeDetailProvider.get();
        } else {
            return null;
        }

    }

}
