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
package com.ericsson.oss.services.shm.job.remote.api.errorcodes;

/**
 * Enum to fetch Error codes and messages for DomainType CLI command.
 * 
 * @author xmadupp
 * 
 */
public enum DomainTypeResponseCode {
    UNSUPPORTED_NODETYPE(13100, "listdomaintype command is not supported for the provided Node."), NO_NE_EXCEPTION(13101, "No network Element found with given input : %s"), DEFAULT_DOMAINTYPE_ERROR(13105,
            "Unable to retrieve the Domain and Type at the movement.");

    private final int errorCode;
    private final String errorMessage;

    private DomainTypeResponseCode(final int errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
