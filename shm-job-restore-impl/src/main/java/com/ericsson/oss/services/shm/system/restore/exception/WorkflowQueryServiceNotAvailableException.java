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
package com.ericsson.oss.services.shm.system.restore.exception;

import javax.ejb.ApplicationException;

import com.ericsson.oss.services.shm.common.exception.ShmException;


@ApplicationException(rollback = false)
public class WorkflowQueryServiceNotAvailableException extends ShmException {

    private static final long serialVersionUID = 1L;

    public WorkflowQueryServiceNotAvailableException() {
        super();
    }

    public WorkflowQueryServiceNotAvailableException(final String message) {
        super(message);
    }

    public WorkflowQueryServiceNotAvailableException(final Exception cause) {
        super(cause);
    }

    public WorkflowQueryServiceNotAvailableException(final String message, final Exception cause) {
        super(message, cause);
    }
}
