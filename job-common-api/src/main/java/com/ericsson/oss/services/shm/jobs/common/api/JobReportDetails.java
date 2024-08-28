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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class JobReportDetails implements Serializable {

    private static final long serialVersionUID = -5594318677084338694L;
    private String jobName;
    private String jobCreatedBy;
    private double jobProgress;
    private String jobStatus;
    private String jobType;
    private String jobResult;
    //private String jobGranularity;
    private String jobStartTime;
    private String jobEndTime;
    //private List<String> nodeNameList;
    private Map<String, Object> jobConfigurationDetails;
    //Added by Namrata for add comment
    private List<Map<String, Object>> jobComment;
    private String description;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    public String getJobCreatedBy() {
        return jobCreatedBy;
    }

    public void setJobCreatedBy(final String jobCreatedBy) {
        this.jobCreatedBy = jobCreatedBy;
    }

    public double getJobProgress() {
        return jobProgress;
    }

    public void setJobProgress(final double jobProgress) {
        this.jobProgress = jobProgress;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(final String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(final String jobType) {
        this.jobType = jobType;
    }

    public String getJobResult() {
        return jobResult;
    }

    public void setJobResult(final String jobResult) {
        this.jobResult = jobResult;
    }

    public String getJobStartTime() {
        return jobStartTime;
    }

    public void setJobStartTime(final String jobStartTime) {
        this.jobStartTime = jobStartTime;
    }

    public String getJobEndTime() {
        return jobEndTime;
    }

    public void setJobEndTime(final String jobEndTime) {
        this.jobEndTime = jobEndTime;
    }

    /**
     * @return the configuration
     */
    public Map<String, Object> getJobConfigurationDetails() {
        return jobConfigurationDetails;
    }

    /**
     * @param jobConfigurationDetails
     *            the configuration to set
     */
    public void setJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails) {
        this.jobConfigurationDetails = jobConfigurationDetails;
    }

    /**
     * @return the jobComment
     */
    public List<Map<String, Object>> getJobComment() {
        return jobComment;
    }

    /**
     * @param jobComment
     *            the jobComment to set
     */
    public void setJobComment(final List<Map<String, Object>> jobComment) {
        this.jobComment = jobComment;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

}
