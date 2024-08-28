/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

/**
 * This class holds the main job static data and provides methods to get job cache data during job execution.
 * 
 * @author tcssbop
 * 
 */
@ApplicationScoped
public class JobStaticDataCache {

    private final Map<Long, JobStaticData> jobStaticCache = new ConcurrentHashMap<Long, JobStaticData>();

    protected void put(final long mainJobId, final JobStaticData jobStaticContext) {
        jobStaticCache.put(mainJobId, jobStaticContext);
    }

    protected JobStaticData get(final long mainJobId) {
        return jobStaticCache.get(mainJobId);
    }

    protected void clear(final long mainJobId) {
        if (jobStaticCache.get(mainJobId) != null) {
            jobStaticCache.remove(mainJobId);
        }
    }

    protected void clearAll() {
        jobStaticCache.clear();
    }
}
