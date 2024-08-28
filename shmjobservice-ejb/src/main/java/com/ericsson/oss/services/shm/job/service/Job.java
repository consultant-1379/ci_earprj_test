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
package com.ericsson.oss.services.shm.job.service;

import java.util.Map;

public class Job {

    private long mainJobId;
    private long jobTemplateId;
    private String jobName;
    private String jobType;
    private String jobState;
    private int executionIndex;
    private String wfsId;
    private Map<String, Object> jobConfigurationDetails;

    public long getMainJobId() {
        return mainJobId;
    }

    public void setMainJobId(final long mainJobId) {
        this.mainJobId = mainJobId;
    }

    public long getJobTemplateId() {
        return jobTemplateId;
    }

    public void setJobTemplateId(final long jobTemplateId) {
        this.jobTemplateId = jobTemplateId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(final String jobType) {
        this.jobType = jobType;
    }

    public String getJobState() {
        return jobState;
    }

    public void setJobState(final String jobState) {
        this.jobState = jobState;
    }

    public int getExecutionIndex() {
        return executionIndex;
    }

    public void setExecutionIndex(final int executionIndex) {
        this.executionIndex = executionIndex;
    }

    public String getWfsId() {
        return wfsId;
    }

    public void setWfsId(final String wfsId) {
        this.wfsId = wfsId;
    }

    public Map<String, Object> getJobConfigurationDetails() {
        return jobConfigurationDetails;
    }

    public void setJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails) {
        this.jobConfigurationDetails = jobConfigurationDetails;
    }
}
