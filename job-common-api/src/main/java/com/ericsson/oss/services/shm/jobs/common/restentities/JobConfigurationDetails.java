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
package com.ericsson.oss.services.shm.jobs.common.restentities;

import java.io.Serializable;
import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;

public class JobConfigurationDetails implements Serializable {

    private static final long serialVersionUID = 1234567L;

    private List<ActivityInfo> activityInfoList;
    private List<JobProperty> jobProperties;
    private String neType;

    /**
     * @return the activityInfoList
     */
    public List<ActivityInfo> getActivityInfoList() {
        return activityInfoList;
    }

    /**
     * @param activityInfoList
     *            the activityInfoList to set
     */
    public void setActivityInfoList(final List<ActivityInfo> activityInfoList) {
        this.activityInfoList = activityInfoList;
    }

    /**
     * @return the jobProperties
     */
    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

    /**
     * @param jobProperties
     *            the jobProperties to set
     */
    public void setJobProperties(final List<JobProperty> jobProperties) {
        this.jobProperties = jobProperties;
    }

    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }

    /**
     * @param neType
     *            the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }

}
