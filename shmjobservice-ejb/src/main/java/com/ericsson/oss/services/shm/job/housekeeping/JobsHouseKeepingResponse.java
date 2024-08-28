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
package com.ericsson.oss.services.shm.job.housekeeping;

public class JobsHouseKeepingResponse {
    private int successfullyDeletedJobsCount;
    private int failedToDeleteJobsCount;
    private int jobsNotFoundCount;
    private String houseKeepingOfJobsStatus;

    /**
     * @return the successfullyDeletedJobsCount
     */
    public int getSuccessfullyDeletedJobsCount() {
        return successfullyDeletedJobsCount;
    }

    /**
     * @param successfullyDeletedJobsCount
     *            the successfullyDeletedJobsCount to set
     */
    public void setSuccessfullyDeletedJobsCount(final int successfullyDeletedJobsCount) {
        this.successfullyDeletedJobsCount = successfullyDeletedJobsCount;
    }

    /**
     * @return the failedToDeleteJobsCount
     */
    public int getFailedToDeleteJobsCount() {
        return failedToDeleteJobsCount;
    }

    /**
     * @param failedToDeleteJobsCount
     *            the failedToDeleteJobsCount to set
     */
    public void setFailedToDeleteJobsCount(final int failedToDeleteJobsCount) {
        this.failedToDeleteJobsCount = failedToDeleteJobsCount;
    }

    /**
     * @return the jobsNotFoundCount
     */
    public int getJobsNotFoundCount() {
        return jobsNotFoundCount;
    }

    /**
     * @param jobsNotFoundCount
     *            the jobsNotFoundCount to set
     */
    public void setJobsNotFoundCount(final int jobsNotFoundCount) {
        this.jobsNotFoundCount = jobsNotFoundCount;
    }

    /**
     * @return the houseKeepingOfJobsStatus
     */
    public String getHouseKeepingOfJobsStatus() {
        return houseKeepingOfJobsStatus;
    }

    /**
     * @param houseKeepingOfJobsStatus
     *            the houseKeepingOfJobsStatus to set
     */
    public void setHouseKeepingOfJobsStatus(final String houseKeepingOfJobsStatus) {
        this.houseKeepingOfJobsStatus = houseKeepingOfJobsStatus;
    }

}
