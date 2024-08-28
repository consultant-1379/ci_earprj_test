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
package com.ericsson.oss.services.shm.es.upgrade.api;

public class ActivityResult {

    private boolean isActivityCompleted;
    private boolean isActivitySuccess;

    public boolean isActivityCompleted() {
        return isActivityCompleted;
    }

    public void setActivityCompleted(final boolean isActivityCompleted) {
        this.isActivityCompleted = isActivityCompleted;
    }

    public boolean isActivitySuccess() {
        return isActivitySuccess;
    }

    public void setActivitySuccess(final boolean isActivitySuccess) {
        this.isActivitySuccess = isActivitySuccess;
    }

}
