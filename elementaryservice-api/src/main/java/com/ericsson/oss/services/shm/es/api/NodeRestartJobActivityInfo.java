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
package com.ericsson.oss.services.shm.es.api;

import java.io.Serializable;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This bean is used to store the values for Node Restart Job Activity (Activity name, platform, jobType, max time for node restart, time interval etc.. )
 * 
 */
public class NodeRestartJobActivityInfo extends JobActivityInfo implements Serializable {

    private static final long serialVersionUID = 5206527454547944722L;

    private int maxTimeForNodeRestart;
    private int waitIntervalForEachRetry;
    private int timeElapsedForNodeRestart;
    final private int cppNodeRestartSleepTime;

    public NodeRestartJobActivityInfo(final long activityJobId, final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform, final int maxTimeForCppNodeRestart,
            final int waitIntervalForEachRetry, final int cppNodeRestartSleepTime) {
        super(activityJobId, activityName, jobType, platform);
        this.maxTimeForNodeRestart = maxTimeForCppNodeRestart;
        this.waitIntervalForEachRetry = waitIntervalForEachRetry;
        this.timeElapsedForNodeRestart = 0;
        this.cppNodeRestartSleepTime = cppNodeRestartSleepTime;
    }

    /**
     * @return the maxTimeForCppNodeRestart
     */
    public int getMaxTimeForCppNodeRestart() {
        return maxTimeForNodeRestart;
    }

    /**
     * @param maxTimeForCppNodeRestart
     *            the maxTimeForCppNodeRestart to set
     */
    public void setMaxTimeForCppNodeRestart(final int maxTimeForCppNodeRestart) {
        this.maxTimeForNodeRestart = maxTimeForCppNodeRestart;
    }

    /**
     * @return the waitIntervalForEachRetry
     */
    public int getWaitIntervalForEachRetry() {
        return waitIntervalForEachRetry;
    }

    /**
     * @param waitIntervalForEachRetry
     *            the waitIntervalForEachRetry to set
     */
    public void setWaitIntervalForEachRetry(final int waitIntervalForEachRetry) {
        this.waitIntervalForEachRetry = waitIntervalForEachRetry;
    }

    /**
     * @return the maxTimeWaitForCppNodeRestart
     */
    public int getTimeElapsedForCppNodeRestart() {
        return timeElapsedForNodeRestart;
    }

    /**
     * @param maxTimeWaitForCppNodeRestart
     *            the maxTimeWaitForCppNodeRestart to set
     */
    public void setTimeElapsedForCppNodeRestart(final int timeElapsedForCppNodeRestart) {
        this.timeElapsedForNodeRestart = timeElapsedForCppNodeRestart;
    }

    /**
     * @return the cppNodeRestartSleepTime
     */
    public int getCppNodeRestartSleepTime() {
        return cppNodeRestartSleepTime;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

}
