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

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

public interface JobTypeDetailsProvider {

    /**
     * @param jobConfiguration
     * @param neName
     * @param neType
     * @param platformType
     * @return
     */
    List<Map<String, String>> getJobConfigurationDetails(Map<String, Object> jobConfiguration, PlatformTypeEnum platformType, String neType, String neName);

    /**
     * This API will provide the job configuration details
     * 
     * @param jobConfigurationDetails
     * @param neType
     * @return
     */

    JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType);

}
