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
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;

/**
 * Utility class to prepare configuration properties to create backup job when external request is received for job creation
 * 
 * @author tcsmaup
 * 
 */

public class NeTypePropertiesUtil {

    @Inject
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    @Inject
    NeTypePropertiesProviderFactory neTypePropertiesProviderFactory;

    /**
     * function to prepare configuration properties when external request is received for job creation
     * 
     * @param platformTypeEnum
     * @param neType
     * @param jobType
     * @param shmRemoteJobData
     * @return
     */
    public List<Map<String, Object>> prepareNeTypeProperties(final PlatformTypeEnum platformTypeEnum, final String neType, final String jobType, final ShmRemoteJobData shmRemoteJobData) {
        final List<Map<String, Object>> neTypeProperties = new ArrayList<Map<String, Object>>();
        final List<String> activityProperties = jobActivitiesProviderImpl.getActivityProperties(platformTypeEnum.getName(), neType, jobType);
        final NeTypePropertiesProvider neTypePropertiesProvider = neTypePropertiesProviderFactory.getNeTypePropertiesProvider(platformTypeEnum, JobType.fromValue(jobType.toUpperCase()));
        if (neTypePropertiesProvider != null) {
            neTypeProperties.addAll(neTypePropertiesProvider.getNeTypeProperties(activityProperties, shmRemoteJobData));
        }
        return neTypeProperties;
    }
}
