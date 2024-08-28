/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.entities;

import java.io.Serializable;

public class CancelResponse implements Serializable {

    private static final long serialVersionUID = 1283415749457226510L;

    private String message;
    private String status;

    public final String getMessage() {

        return message;
    }

    public final void setMessage(final String message) {

        this.message = message;
    }

    public final String getStatus() {

        return status;
    }

    public final void setStatus(final String status) {

        this.status = status;
    }

}
