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
package com.ericsson.oss.services.shm.job.timer;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is to buffer all the NEJobs when activity job progress is updating.
 * 
 * @author xthagan, xneranu
 * 
 */

public class NEJobProgressPercentageCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(NEJobProgressPercentageCache.class);
    static List<Long> cache = Collections.synchronizedList(new ArrayList<Long>());

    public void bufferNEJobs(final long neJobId) {
        if (!cache.contains(neJobId)) {
            cache.add(neJobId);
        } else {
            LOGGER.debug("NEJobId: {} is already cached", neJobId);
        }
    }

    /**
     * Retrieves the List of NEJobs.
     * 
     * @return List of NE Jobs
     */

    public Object[] retrieveNEJobs() {
        synchronized (cache) {
        final Object[] temp = cache.toArray(new Long[cache.size()]);
        cache.clear();
        return temp;
    }
    }

}
