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
package com.ericsson.oss.services.shm.job.remote.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeQualifier;

@ApplicationScoped
public class NeTypePropertiesProviderFactory {

    @Inject
    @Any
    private Instance<NeTypePropertiesProvider> neTypePropertiesProvider;

    public NeTypePropertiesProvider getNeTypePropertiesProvider(final PlatformTypeEnum platformType, final JobType jobType) {
        final Instance<NeTypePropertiesProvider> provider = neTypePropertiesProvider.select(new PlatformJobTypeQualifier(platformType, jobType));
        if (provider != null && !provider.isUnsatisfied()) {
            return provider.get();
        } else {
            return null;
        }
    }

}
