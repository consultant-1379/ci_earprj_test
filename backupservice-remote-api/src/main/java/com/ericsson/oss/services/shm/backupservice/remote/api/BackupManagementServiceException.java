/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.backupservice.remote.api;

import javax.ejb.ApplicationException;

/**
 * Thrown to indicate that an Exception has occurred while creating and uploading of backup to smrs location
 */
@ApplicationException(rollback = false)
public class BackupManagementServiceException extends Exception {

    private static final long serialVersionUID = -2844345720310785768L;

    public BackupManagementServiceException(final String message) {
        super(message);
    }

    public BackupManagementServiceException(final String message, final Exception cause) {
        super(message, cause);
    }

}