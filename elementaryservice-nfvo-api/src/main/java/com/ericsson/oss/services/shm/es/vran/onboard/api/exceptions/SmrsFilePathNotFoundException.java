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
package com.ericsson.oss.services.shm.es.vran.onboard.api.exceptions;

/**
 * Exception to be thrown when the software package path is not found in ENM
 * 
 * @author xjhosye
 *
 */
public class SmrsFilePathNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -4715770955771856958L;

    public SmrsFilePathNotFoundException(final String errorMessage) {
        super(errorMessage);
    }

    public SmrsFilePathNotFoundException(final String errorMessage, final Exception cause) {
        super(errorMessage, cause);
    }

}
