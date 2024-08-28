/*------------------------------------------------------------------------------
 *******************************************************************************
 /* * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.backupservice.remote.api;

import java.io.Serializable;

/**
 * Holds options for delete backup operations.
 */
public class DeleteBackupOptions implements Serializable {

    private static final long serialVersionUID = -8018986181829233898L;

    private boolean removeFromRollbackList;

    public DeleteBackupOptions(final boolean removeFromRollbackList) {

        this.removeFromRollbackList = removeFromRollbackList;
    }

    public DeleteBackupOptions removeFromRollbackList(final boolean removeFromRollbackList) {
        this.removeFromRollbackList = removeFromRollbackList;
        return this;
    }

    public boolean isRemoveFromRollbackList() {
        return removeFromRollbackList;
    }
}
