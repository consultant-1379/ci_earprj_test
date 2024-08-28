package com.ericsson.oss.services.shm.es.api;

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

public class SHMActivityRequest extends SHMJobRequest {

    private static final long serialVersionUID = 1L;

    private String activityName;
    private String platformType;
    private int retryCount;
    private long activityJobId;

    /**
     * @param workflowInstanceId
     * @param businessKey
     * @param jobType
     */
    public SHMActivityRequest(final String workflowInstanceId, final String businessKey, final String jobType) {
        super(workflowInstanceId, businessKey, jobType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SHMActivityRequest [activityName=" + activityName + ", platformType=" + platformType + ", retryCount=" + retryCount + ", activityJobId=" + activityJobId + ", toString()="
                + super.toString() + "]";
    }

    /**
	 * 
	 */
    public SHMActivityRequest() {
        super();
        // TODO Auto-generated constructor stub
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
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(final int retryCount) {
        this.retryCount = retryCount;
    }

    public long getActivityJobId() {
        return activityJobId;
    }

    public void setActivityJobId(final long activityJobId) {
        this.activityJobId = activityJobId;
    }

}
