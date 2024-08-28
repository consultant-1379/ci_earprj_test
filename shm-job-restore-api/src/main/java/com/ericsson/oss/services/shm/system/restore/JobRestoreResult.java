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
package com.ericsson.oss.services.shm.system.restore;

public class JobRestoreResult {

    public long mainJobId;
    public int executionIndex;

    /**
     * @param mainJobId
     * @param executionIndex
     */
    public JobRestoreResult(final long mainJobId, final int executionIndex) {
        this.mainJobId = mainJobId;
        this.executionIndex = executionIndex;
    }

    /**
     * @return the mainJobId
     */
    public long getMainJobId() {
        return mainJobId;
    }

    /**
     * @return the executionIndex
     */
    public int getExecutionIndex() {
        return executionIndex;
    }

}
