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
package com.ericsson.oss.services.shm.es.polling.api;

import java.util.Date;

/**
 * Manages polling activity information.
 * 
 * @author tcsgusw
 * 
 */
public class PollingActivityInfoProvider {

    private String activityName;
    private String jobType;
    private String platform;
    private Long activityJobId;
    private String moFdn;
    private Date pollingStartTime;
    private ReadCallStatusEnum readCallStatus;

    public PollingActivityInfoProvider(final String activityName, final String jobType, final String platform, final Long activityJobId, final String moFdn, final Date pollingStartTime,
            final ReadCallStatusEnum readCallStatus) {
        this.activityName = activityName;
        this.jobType = jobType;
        this.platform = platform;
        this.activityJobId = activityJobId;
        this.moFdn = moFdn;
        this.pollingStartTime = pollingStartTime;
        this.readCallStatus = readCallStatus;
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
     * @return the platform
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * @param platform
     *            the platform to set
     */
    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    /**
     * @return the activityJobId
     */
    public Long getActivityJobId() {
        return activityJobId;
    }

    /**
     * @param activityJobId
     *            the activityJobId to set
     */
    public void setActivityJobId(final Long activityJobId) {
        this.activityJobId = activityJobId;
    }

    /**
     * @return the moFdn
     */
    public String getMoFdn() {
        return moFdn;
    }

    /**
     * @param moFdn
     *            the moFdn to set
     */
    public void setMoFdn(final String moFdn) {
        this.moFdn = moFdn;
    }

    /**
     * @return the pollingStartTime
     */
    public Date getPollingStartTime() {
        return pollingStartTime;
    }

    /**
     * @param pollingStartTime
     *            the pollingStartTime to set
     */
    public void setPollingStartTime(final Date pollingStartTime) {
        this.pollingStartTime = pollingStartTime;
    }

    /**
     * @return the readCallStatus
     */
    public ReadCallStatusEnum getReadCallStatus() {
        return readCallStatus;
    }

    /**
     * @param readCallStatus
     *            the readCallStatus to set
     */
    public void setReadCallStatus(final ReadCallStatusEnum readCallStatus) {
        this.readCallStatus = readCallStatus;
    }
}