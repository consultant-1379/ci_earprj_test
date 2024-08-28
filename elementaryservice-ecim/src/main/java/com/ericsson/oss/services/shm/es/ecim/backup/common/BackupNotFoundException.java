/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT EricssonAB 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * EricssonAB Inc. The programs may be used and/or copied only with written
 * permission from EricssonAB Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.ecim.backup.common;

public class BackupNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public BackupNotFoundException() {
        super();
    }

    public BackupNotFoundException(final Throwable throwable) {
        super(throwable);
    }

    public BackupNotFoundException(final Exception cause) {
        super(cause);

    }

}