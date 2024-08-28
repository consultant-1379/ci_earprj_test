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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

/**
 * POJO class to hold job status query attributes
 * 
 * @author xmadupp
 */
public class JobStatusQuery implements Serializable {

    private static final long serialVersionUID = -6724607545731830037L;
    private JobTypeEnum jobType;
    private JobState jobState;
    private String userName;
    private String jobName;
    private JobCategory jobCategory;

    /**
     * @return the jobCategory
     */
    public JobCategory getJobCategory() {
        return jobCategory;
    }

    /**
     * @param jobCategory
     *            the jobCategory to set
     */
    public void setJobCategory(final JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName
     *            the jobName to set
     */
    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    /**
     * @return the jobType
     */
    public JobTypeEnum getJobType() {
        return jobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final JobTypeEnum jobType) {
        this.jobType = jobType;
    }

    /**
     * @return the jobState
     */
    public JobState getJobState() {
        return jobState;
    }

    /**
     * @param jobState
     *            the jobState to set
     */
    public void setJobState(final JobState jobState) {
        this.jobState = jobState;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName
     *            the userName to set
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JobStatusQuery [jobType=" + jobType + ", jobState=" + jobState + ", userName=" + userName + ", jobName=" + jobName + "]";
    }
}
