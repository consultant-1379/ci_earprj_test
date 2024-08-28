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
package com.ericsson.oss.services.shm.system.restore.common;

import com.ericsson.oss.services.shm.jobs.common.modelentities.*;

public class MainJob {

    private String mainWorkflowInstanceId;
    private long templateJobId;
    private long mainJobId;
    private JobState mainJobState;
    private int executionIndex;
    private String JobName;
    private JobType JobType;
    private Schedule mainSchedule;

    /**
     * @return the mainWorkflowInstanceId
     */
    public String getMainWorkflowInstanceId() {
        return mainWorkflowInstanceId;
    }

    /**
     * @param mainWorkflowInstanceId
     *            the mainWorkflowInstanceId to set
     */
    public void setMainWorkflowInstanceId(final String mainWorkflowInstanceId) {
        this.mainWorkflowInstanceId = mainWorkflowInstanceId;
    }

    /**
     * @return the templateJobId
     */
    public long getTemplateJobId() {
        return templateJobId;
    }

    /**
     * @param templateJobId
     *            the templateJobId to set
     */
    public void setTemplateJobId(final long templateJobId) {
        this.templateJobId = templateJobId;
    }

    /**
     * @return the mainJobId
     */
    public long getMainJobId() {
        return mainJobId;
    }

    /**
     * @param mainJobId
     *            the mainJobId to set
     */
    public void setMainJobId(final long mainJobId) {
        this.mainJobId = mainJobId;
    }

    /**
     * @return the mainJobState
     */
    public JobState getMainJobState() {
        return mainJobState;
    }

    /**
     * @param mainJobState
     *            the mainJobState to set
     */
    public void setMainJobState(final JobState mainJobState) {
        this.mainJobState = mainJobState;
    }

    /**
     * @return the executionIndex
     */
    public int getExecutionIndex() {
        return executionIndex;
    }

    /**
     * @param executionIndex
     *            the executionIndex to set
     */
    public void setExecutionIndex(final int executionIndex) {
        this.executionIndex = executionIndex;
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return JobName;
    }

    /**
     * @param jobName
     *            the jobName to set
     */
    public void setJobName(final String jobName) {
        JobName = jobName;
    }

    /**
     * @return the jobType
     */
    public JobType getJobType() {
        return JobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final JobType jobType) {
        JobType = jobType;
    }

    /**
     * @return the mainSchedule
     */
    public Schedule getMainSchedule() {
        return mainSchedule;
    }

    /**
     * @param mainSchedule
     *            the mainSchedule to set
     */
    public void setMainSchedule(final Schedule mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

}
