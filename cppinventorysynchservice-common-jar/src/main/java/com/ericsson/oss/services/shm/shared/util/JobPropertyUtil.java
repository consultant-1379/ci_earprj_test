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
package com.ericsson.oss.services.shm.shared.util;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class JobPropertyUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobPropertyUtil.class);

    private JobPropertyUtil() {
    }

    /**
     * This will return the value if the given property exists in Map. Otherwise the value will be null
     * 
     * @param jobProperties
     * @param propertyName
     * @return
     */
    public static String getProperty(final List<Map<String, String>> jobProperties, final String propertyName) {

        LOGGER.debug("Getting job property [{}] from job property list: {}", propertyName, jobProperties);
        String propertyValue = null;
        if (jobProperties != null && !jobProperties.isEmpty()) {
            for (Map<String, String> jobInfo : jobProperties) {
                if (jobInfo.get(ShmConstants.KEY) != null && propertyName.equalsIgnoreCase(jobInfo.get(ShmConstants.KEY))) {
                    propertyValue = jobInfo.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        LOGGER.debug("Resolved job property {}: {}", propertyName, propertyValue);
        return propertyValue;
    }
    /**
     * This will update the value if the given property exists in Map.
     * 
     * @param jobProperties
     * @param propertyName
     * @param newPropertyValue
     * @return
     */
    public static void updateJobProperty(final List<Map<String, String>> jobProperties, final String propertyName, final String newPropertyValue) {

        LOGGER.debug("Getting job property [{}] from job property list: {}", propertyName, jobProperties);
        if (jobProperties != null && !jobProperties.isEmpty()) {
            for (Map<String, String> jobProperty : jobProperties) {
                if (jobProperty.get(ShmConstants.KEY) != null && propertyName.equalsIgnoreCase(jobProperty.get(ShmConstants.KEY))) {
                    jobProperty.put(ShmConstants.VALUE, newPropertyValue);
                    break;
                }
            }
        }
        LOGGER.debug("Update job property {} :with new value {}", propertyName, newPropertyValue);
    }

}
