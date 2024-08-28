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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

/**
 * POJO class to fetch noderestart job configuration data from external services and use it for noderestart job creation
 * 
 * @author tcskaki
 * 
 */

public class ShmNodeRestartJobData extends ShmRemoteJobData implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String restartRank;
    private String restartReason;
    private String restartInfo;
    private String activity;

    /**
     * @return the restartRank
     */
    public String getRestartRank() {
        return restartRank;
    }

    /**
     * @param restartRank
     *            the restartRank to set
     */
    public void setRestartRank(final String restartRank) {
        this.restartRank = restartRank;
    }

    /**
     * @return the restartReason
     */
    public String getRestartReason() {
        return restartReason;
    }

    /**
     * @param restartReason
     *            the restartReason to set
     */
    public void setRestartReason(final String restartReason) {
        this.restartReason = restartReason;
    }

    /**
     * @return the restartInfo
     */
    public String getRestartInfo() {
        return restartInfo;
    }

    /**
     * @param restartInfo
     *            the restartInfo to set
     */
    public void setRestartInfo(final String restartInfo) {
        this.restartInfo = restartInfo;
    }

    /**
     * @return the activity
     */
    public String getActivity() {
        return activity;
    }

    /**
     * @param activity
     *            the activity to set
     */
    public void setActivity(final String activity) {
        this.activity = activity;
    }

    /**
     * Return ShmNodeRestartJobData as String
     * 
     * @return String
     */
    @Override
    public String toString() {
        return "[" + "restart Rank :" + restartRank + " restart Reason :" + restartReason + "Restart Info : " + restartInfo + "]";
    }

}
