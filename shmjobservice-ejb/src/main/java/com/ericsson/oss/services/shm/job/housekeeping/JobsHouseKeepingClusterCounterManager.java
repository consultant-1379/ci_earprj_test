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
package com.ericsson.oss.services.shm.job.housekeeping;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;

/**
 * 
 * This class is used to retrieve Jobs Housekeeping counter value of a VM using cluster cache in distributed clustered environment.
 * 
 * @author xprapav
 * 
 */

@ApplicationScoped
public class JobsHouseKeepingClusterCounterManager {

    @Inject
    @NamedCache("JobsMetricsClusterCache")
    private Cache<String, Double> jobMetricsCache;

    private static final String SHM_JOBS_HOUSEKEEPING_COUNTER = "SHM_JOBS_HOUSEKEEPING_COUNTER";

    private static final int JOBS_HOUSEKEEPING_RUNNING = 1;

    private static final int JOBS_HOUSEKEEPING_NOT_RUNNING = 0;

    public long getClusterCounter() {
        return getJobsHouseKeepingCounter(SHM_JOBS_HOUSEKEEPING_COUNTER);
    }

    /**
     * Sets the counter value to 1 if any Housekeeping is running (so that other VM will not start new Jobs Housekeeping) otherwise it sets the counter value to 0.
     */
    public void setClusterCounter(final boolean isHousekeepingRunning) {
        if (isHousekeepingRunning) {
            updateJobsHouseKeepingCounter(SHM_JOBS_HOUSEKEEPING_COUNTER, JOBS_HOUSEKEEPING_RUNNING);
        } else {
            updateJobsHouseKeepingCounter(SHM_JOBS_HOUSEKEEPING_COUNTER, JOBS_HOUSEKEEPING_NOT_RUNNING);
        }
    }

    private int getJobsHouseKeepingCounter(final String jobsHousekeepingCounterKey) {
        final Double jobsHousekeepingCounter = jobMetricsCache.get(jobsHousekeepingCounterKey);
        if (jobsHousekeepingCounter != null) {
            return jobsHousekeepingCounter.intValue();
        }
        return JOBS_HOUSEKEEPING_NOT_RUNNING;
    }

    private void updateJobsHouseKeepingCounter(final String jobsHousekeepingCounterKey, final int jobsHousekeepingCounter) {
        final double jobsHousekeepingCounterValue = jobsHousekeepingCounter;
        jobMetricsCache.put(jobsHousekeepingCounterKey, jobsHousekeepingCounterValue);
    }
}
