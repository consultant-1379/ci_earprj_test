package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;

/**
 * This class holds the NE Name for NEJob PO and corresponding activity level log details
 * 
 */
public class NeJobLogDetails {

    private String neJobName;
    private List<JobLogDetails> jobLogDetails;
    private String error;
    private String nodeType;

    /**
     * @return the neJobName
     */
    public String getNeJobName() {
        return neJobName;
    }

    /**
     * @param neJobName
     *            the neJobName to set
     */
    public void setNeJobName(final String neJobName) {
        this.neJobName = neJobName;
    }

    /**
     * @return the jobLogDetails
     */
    public List<JobLogDetails> getJobLogDetails() {
        return jobLogDetails;
    }

    /**
     * @param jobLogDetails
     *            the jobLogDetails to set
     */
    public void setJobLogDetails(final List<JobLogDetails> jobLogDetails) {
        this.jobLogDetails = jobLogDetails;
    }

    /**
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * @param error
     *            the error to set
     */
    public void setError(final String error) {
        this.error = error;
    }

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @param nodeType
     *            the nodeType to set
     */
    public void setNodeType(final String nodeType) {
        this.nodeType = nodeType;
    }

}
