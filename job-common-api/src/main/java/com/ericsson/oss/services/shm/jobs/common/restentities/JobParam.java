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

import java.util.List;
import java.util.Map;

public class JobParam {

    String neType;
    List<ActivityInfo> activityInfoList;

    List<Map<String, String>> jobParameterAttributes;

    /**
     * @return the jobParameterAttributes
     */
    public List<Map<String, String>> getJobParameterAttributes() {
        return jobParameterAttributes;
    }

    /**
     * @param jobParameterAttributes
     *            the jobParameterAttributes to set
     */
    public void setJobParameterAttributes(final List<Map<String, String>> jobParameterAttributes) {
        this.jobParameterAttributes = jobParameterAttributes;
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JobParam [activityInfoList=" + activityInfoList + ", jobParameterAttributes=" + jobParameterAttributes + "]";
    }
}
