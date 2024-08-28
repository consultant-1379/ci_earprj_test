/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.common;

/**
 * This user-defined RunTimeException class is used to throw Exception to gracefully fail the ongoing Activity in the case of NodeReadAttributes call failure.
 *
 * @author xdinyar
 *
 */
public class NodeReadAttributeFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NodeReadAttributeFailedException(final String exception) {
        super(exception);
    }

    public NodeReadAttributeFailedException(final Exception cause) {
        super(cause);
    }
}
