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
package com.ericsson.oss.services.shm.jobservice.common;

public enum JobHandlerErrorCodes {
    SUCCESS(200, "success"), JOB_CFGN_PERSISTENCE_FAILED(201, "Error: Job configuration persistence failed"), JOB_PERSISTENCE_FAILED(204, "Error: Job persistence failed");

    private final int responseCode;
    private final String responseDescription;

    JobHandlerErrorCodes(final int responseCode, final String responseDescription) {
        this.responseCode = responseCode;
        this.responseDescription = responseDescription;
    }

    /**
     * @return the errorCode
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * @return the errorDescription
     */
    public String getResponseDescription() {
        return responseDescription;
    }

}
