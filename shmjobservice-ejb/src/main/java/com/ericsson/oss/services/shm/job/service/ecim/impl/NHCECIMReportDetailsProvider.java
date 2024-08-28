/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.ecim.impl;


import java.util.List;
import java.util.Map;
import java.util.LinkedList;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.AbstractNHCJobDetailsProvider;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of Node health check Job when ECIM specific node(s) selected, which has to be displayed as Node Activities under Configuration Details panel of
 * Job summary on nhc main page
 * 
 * @author xkalkil
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.ECIM, jobType = JobType.NODE_HEALTH_CHECK)
public class NHCECIMReportDetailsProvider extends AbstractNHCJobDetailsProvider {

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        return activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.ECIM.name(), acitvityParameters);
    }


    @Override
    public List<Map<String, String>> getJobConfigurationDetails(Map<String, Object> jobConfiguration, PlatformTypeEnum platformType, String neType, String neName) {
        return new LinkedList<>();
    }


}
