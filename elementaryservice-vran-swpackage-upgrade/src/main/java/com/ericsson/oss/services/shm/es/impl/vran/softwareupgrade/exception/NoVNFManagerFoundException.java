/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception;

/**
 * Exception to be thrown when no VirtualNetworkFunctionManager MO is found for the selected network element.
 *
 * @author xeswpot
 *
 */
public class NoVNFManagerFoundException extends RuntimeException {

    private static final long serialVersionUID = 6480350172470773285L;

    public NoVNFManagerFoundException(final String errorMessage) {
        super(errorMessage);
    }

    public NoVNFManagerFoundException(final String errorMessage, final Exception cause) {
        super(errorMessage, cause);
    }
}
