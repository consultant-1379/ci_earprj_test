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
package com.ericsson.oss.services.shm.jobs.common.mapper;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants.ATTRIBUTE_NAME;
import static com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants.ATTRIBUTE_VALUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobParam;

/**
 * To provide the grouping of attributes collection based on ECIM platform type
 * 
 */
@Stateless
public class JobParamMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobParamMapper.class);

    public JobParam createJobparam(final List<JobProperty> jobProperties, final List<ActivityInfo> activityInfo) {
        JobParam jobParam = null;
        final List<Map<String, String>> jobParameterAttributes = new ArrayList<Map<String, String>>();
        for (final JobProperty jobProperty : jobProperties) {
            final String key = jobProperty.getKey();
            final String value = jobProperty.getValue();
            LOGGER.debug("ecimJobParameterAttributes- {} and {}", key, value);
            if (key != null) {
                jobParameterAttributes.add(updateJobParam(key, value));
            }

        }
        jobParam = newJobParam(jobParameterAttributes, activityInfo);
        return jobParam;
    }

    /**
     * @param jobParameterAttributes
     * @return
     */
    private JobParam newJobParam(final List<Map<String, String>> jobParameterAttributes, final List<ActivityInfo> activityInfo) {
        final JobParam jobParam = new JobParam();
        if ((activityInfo != null && activityInfo.size() != 0) || (jobParameterAttributes != null && jobParameterAttributes.size() != 0)) {
            jobParam.setJobParameterAttributes(jobParameterAttributes);
            sortActivities(activityInfo);
            jobParam.setActivityInfoList(activityInfo);
        }
        return jobParam;

    }

    /**
     * Create a new jobParameters map for given Key, value
     * 
     * @param key
     * @param value
     * @return Map
     */
    private Map<String, String> updateJobParam(final String key, final String value) {
        final Map<String, String> jobParameters = new HashMap<String, String>();
        jobParameters.put(ATTRIBUTE_NAME, key);
        if (value != null) {
            jobParameters.put(ATTRIBUTE_VALUE, value);
        }
        return jobParameters;
    }

    /**
     * Method to sort the activities in the list according to its order
     * 
     * @param activityDetailsList
     */
    private void sortActivities(final List<ActivityInfo> activityDetailsList) {
        if (activityDetailsList != null) {
            Collections.sort(activityDetailsList, new Comparator<ActivityInfo>() {
                @Override
                public int compare(final ActivityInfo activityDetailsList1, final ActivityInfo activityDetailsList2) {

                    return activityDetailsList1.getOrder() - activityDetailsList2.getOrder();

                }
            });
        }
    }

    /**
     * Sorts the JobParams based on the neType
     * 
     * @param jobParamsList
     */
    public void sortJobConfigurationDetails(final List<JobConfigurationDetails> jobParamsList) {
        Collections.sort(jobParamsList, new Comparator<JobConfigurationDetails>() {
            @Override
            public int compare(final JobConfigurationDetails jobParamsList1, final JobConfigurationDetails jobParamsList2) {
                return jobParamsList1.getNeType().compareTo(jobParamsList2.getNeType());
            }
        });
    }

}
