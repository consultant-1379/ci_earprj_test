/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.cache;

import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * provides Main job level static data
 * 
 * @author tcssbop
 * 
 */
public class JobStaticData {

    private final String owner;
    private final Map<String, Object> activitySchedules;
    private final String executionMode;
    private final JobType jobType;
    private final String jobExecutionUser;

    public JobStaticData(final String owner, final Map<String, Object> activitySchedules, final String executionMode, final JobType jobType, final String jobExecutionUser) {
        this.owner = owner;
        this.activitySchedules = activitySchedules;
        this.executionMode = executionMode;
        this.jobType = jobType;
        this.jobExecutionUser = jobExecutionUser;
    }

    public String getJobExecutionUser() {
        return jobExecutionUser;
    }

    public String getOwner() {
        return owner;
    }

    public Map<String, Object> getActivitySchedules() {
        return activitySchedules;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public JobType getJobType() {
        return jobType;
    }

    @Override
    public String toString() {
        return "JobStaticData [owner=" + owner + ", activitySchedules=" + activitySchedules + ", executionMode=" + executionMode + ", jobType=" + jobType + ", jobExecutionUser=" + jobExecutionUser
                + "]";
    }
}
