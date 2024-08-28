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

package com.ericsson.oss.services.shm.webpush.api;

import java.io.Serializable;
import java.util.Map;

/**
 * This class holds jobId as subscriptionId for web push, job attributes and UI application type.
 * 
 */

public class JobWebPushEvent implements Serializable {

    private static final long serialVersionUID = -3183819510000882902L;

    private String jobId;
    private Map<String, Object> attributeMap;
    private String applicationType;

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @param jobId
     *            the jobId to set
     */
    public void setJobId(final String jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the attributeMap
     */
    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    /**
     * @param attributeMap
     *            the attributeMap to set
     */
    public void setAttributeMap(final Map<String, Object> attributeMap) {
        this.attributeMap = attributeMap;
    }

    /**
     * @return the UI Application invoked
     */
    public String getApplicationType() {
        return applicationType;
    }

    /**
     * @param applicationType
     *            the UI application to set
     */
    public void setApplicationType(final String applicationType) {
        this.applicationType = applicationType;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Job details : [Job Attributes=").append(attributeMap).append(", Job id=").append(jobId).append(" , Application type=").append(applicationType).append("]");
        return builder.toString();
    }
}
