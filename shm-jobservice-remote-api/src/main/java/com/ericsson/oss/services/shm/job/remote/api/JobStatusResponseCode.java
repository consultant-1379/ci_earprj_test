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

/**
 * Enum to fetch Error codes and messages for Job status CLI command.
 * 
 * @author xmadupp
 * 
 */
public enum JobStatusResponseCode {
    INVALID_JOBTYPE(13150, "Invalid Job type."), INVALID_JOBSTATUS(13151, "Invalid Job status."), DEFAULT_JOB_STATUS(13155,
            "Unable to retrieve the job status at the moment.");

    private final int errorCode;
    private final String errorMessage;

    private JobStatusResponseCode(final int errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
