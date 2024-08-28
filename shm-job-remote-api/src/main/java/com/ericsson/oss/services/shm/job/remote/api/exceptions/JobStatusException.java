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
package com.ericsson.oss.services.shm.job.remote.api.exceptions;

/**
 * This exception will be thrown when requested job entry not found or any other internal errors
 * 
 * @author xgudpra
 */
public class JobStatusException extends Exception {

    private static final long serialVersionUID = 1L;

    public JobStatusException(final String errorMessage) {
        super(errorMessage);
    }

}
