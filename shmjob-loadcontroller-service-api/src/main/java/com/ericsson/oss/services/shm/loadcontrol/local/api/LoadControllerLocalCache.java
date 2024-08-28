/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.local.api;

import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;

/**
 * Its responsibility is to add activity job Id into local cache when the activity job starts, send the request to load controller topic when the activity job ends and remove the activity job id from
 * local cache if exists .
 * 
 * @author tcsvnag
 * 
 */
public interface LoadControllerLocalCache {

    void addActivityJobIdToCache(long activityJobId);

    void removeActivityJobIdFromCacheAndDecrementLocalCounter(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest);

    void keepMessageInTopic(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest);

}
