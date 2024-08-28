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
package com.ericsson.oss.services.shm.es.api;

import java.io.Serializable;

public class SHMJobRequest implements Serializable {

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SHMJobRequest [workflowInstanceId=" + workflowInstanceId + ", businessKey=" + businessKey + ", jobType=" + jobType + "]";
    }

    private static final long serialVersionUID = 1L;

    private String workflowInstanceId;
    private String businessKey;
    private String jobType;

    /**
     * @param workflowInstanceId
     * @param businessKey
     * @param jobType
     */
    public SHMJobRequest(final String workflowInstanceId, final String businessKey, final String jobType) {
        super();
        this.workflowInstanceId = workflowInstanceId;
        this.businessKey = businessKey;
        this.jobType = jobType;
    }

    public SHMJobRequest() {
    }

    /**
     * @return the workflowInstanceId
     */
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    /**
     * @param workflowInstanceId
     *            the workflowInstanceId to set
     */
    public void setWorkflowInstanceId(final String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    /**
     * @return the businessKey
     */
    public String getBusinessKey() {
        return businessKey;
    }

    /**
     * @param businessKey
     *            the businessKey to set
     */
    public void setBusinessKey(final String businessKey) {
        this.businessKey = businessKey;
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
