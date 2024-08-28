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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;
import java.util.Map;

/**
 * This class is to get the details to be displayed on Node activities panel of job details page
 * 
 */
public class NetworkElementJobDetails {

    private List<ActivityDetails> activityDetails;
    private List<Map<String, String>> jobConfigurationDetails;

    public NetworkElementJobDetails() {
    }

    public NetworkElementJobDetails(final List<ActivityDetails> activityDetails, final List<Map<String, String>> jobConfigurationDetails) {
        this.activityDetails = activityDetails;
        this.jobConfigurationDetails = jobConfigurationDetails;
    }

    /**
     * @return the activityDetails
     */
    public List<ActivityDetails> getActivityDetails() {
        return activityDetails;
    }

    /**
     * @param activityDetails
     *            the activityDetails to set
     */
    public void setActivityDetails(final List<ActivityDetails> activityDetails) {
        this.activityDetails = activityDetails;
    }

    /**
     * @return the jobConfigurationDetails
     */
    public List<Map<String, String>> getJobConfigurationDetails() {
        return jobConfigurationDetails;
    }

    /**
     * @param jobConfigurationDetails
     *            the jobConfigurationDetails to set
     */
    public void setJobConfigurationDetails(final List<Map<String, String>> jobConfigurationDetails) {
        this.jobConfigurationDetails = jobConfigurationDetails;
    }

}
