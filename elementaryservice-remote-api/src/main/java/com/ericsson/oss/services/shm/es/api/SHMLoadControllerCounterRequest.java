/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.api;

import java.io.Serializable;

public class SHMLoadControllerCounterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private long activityJobId;
    private String activityName;
    private String platformType;
    private String jobType;
    private String activityStatus;

    /**
     * @return the activityStatus
     */
    public String getActivityStatus() {
        return activityStatus;
    }

    /**
     * @param activityStatus
     *            the activityStatus to set
     */
    public void setActivityStatus(final String activityStatus) {
        this.activityStatus = activityStatus;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName
     *            the activityName to set
     */
    public void setActivityName(final String activityName) {
        this.activityName = activityName;
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

    /**
     * @return the activityJobId
     */
    public long getActivityJobId() {
        return activityJobId;
    }

    /**
     * @param activityJobId
     *            the activityJobId to set
     */
    public void setActivityJobId(final long activityJobId) {
        this.activityJobId = activityJobId;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SHMLoadControllerCounterRequest : [Attributes=").append(", activityJobId=").append(activityJobId).append(", platformType=").append(platformType).append(", jobType=")
                .append(jobType).append(", activityName=").append(activityName).append(",currentRunningActivitiesCount=").append(activityStatus).append("]");
        return builder.toString();
    }

}
