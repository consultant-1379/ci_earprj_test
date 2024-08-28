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
package com.ericsson.oss.services.shm.jobs.common.api;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

public class NEJob {

    private String neWorkflowInstanceId;
    private long neJobId;
    private JobState state;
    private String nodeName;
    private String platformType;
    private String jobType;

    /**
     * @return the neWorkflowInstanceId
     */
    public String getNeWorkflowInstanceId() {
        return neWorkflowInstanceId;
    }

    /**
     * @param neWorkflowInstanceId
     *            the neWorkflowInstanceId to set
     */
    public void setNeWorkflowInstanceId(final String neWorkflowInstanceId) {
        this.neWorkflowInstanceId = neWorkflowInstanceId;
    }

    /**
     * @return the neJobId
     */
    public long getNeJobId() {
        return neJobId;
    }

    /**
     * @param neJobId
     *            the neJobId to set
     */
    public void setNeJobId(final long neJobId) {
        this.neJobId = neJobId;
    }

    /**
     * @return the state
     */
    public JobState getState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(final JobState state) {
        this.state = state;
    }

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName
     *            the nodeName to set
     */
    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * @return the platformType
     */
    public String getPlatformType() {
        return platformType;
    }

    /**
     * @param platformType
     *            the platformType to set
     */
    public void setPlatformType(final String platformType) {
        this.platformType = platformType;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final String jobType) {
        this.jobType = jobType;
    }

}
