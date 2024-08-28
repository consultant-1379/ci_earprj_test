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
package com.ericsson.oss.services.shm.system.restore.common;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

public class ActivityJob {

    private String name;
    private long poId;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the poId
     */
    public long getPoId() {
        return poId;
    }

    /**
     * @param poId
     *            the poId to set
     */
    public void setPoId(final long poId) {
        this.poId = poId;
    }

    /**
     * @return the state
     */
    public JobState getState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(final JobState state) {
        this.state = state;
    }

    private JobState state;

}
