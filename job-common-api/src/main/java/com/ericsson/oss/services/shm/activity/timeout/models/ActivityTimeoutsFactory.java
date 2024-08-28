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
package com.ericsson.oss.services.shm.activity.timeout.models;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeQualifier;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * This class is used to select a Job Type specific implementation based on JobTypeAnnotation
 * 
 * @author xsrabop
 */
@ApplicationScoped
public class ActivityTimeoutsFactory {

    @Inject
    @Any
    private Instance<ActivityTimeoutsProvider> activityTimeoutsProvider;
    
    @Inject
    @Any
    private Instance<PrecheckConfigurationProvider> repeatPrecheckConfigurationProvider;

    public ActivityTimeoutsProvider getActivityTimeoutsProvider(final JobType jobType) {
        final Instance<ActivityTimeoutsProvider> responseModifier = activityTimeoutsProvider.select(new JobTypeQualifier(jobType));
        if (responseModifier != null && !responseModifier.isUnsatisfied()) {
            return responseModifier.get();
        } else {
            return null;
        }
    }
    
    public PrecheckConfigurationProvider getRepeatPrecheckConfigurationProvider(final JobType jobType) {
        final Instance<PrecheckConfigurationProvider> responseModifier = repeatPrecheckConfigurationProvider.select(new JobTypeQualifier(jobType));
        if (responseModifier != null && !responseModifier.isUnsatisfied()) {
            return responseModifier.get();
        } else {
            return null;
        }
    }
}
