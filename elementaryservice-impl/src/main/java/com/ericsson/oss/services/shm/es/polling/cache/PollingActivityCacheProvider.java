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
package com.ericsson.oss.services.shm.es.polling.cache;

import java.util.*;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.model.ShmPollingActivityData;

/**
 * This class provides methods to add and remove the data in the cache and get the data from the Cache
 * 
 * @author xprapav
 */
@ApplicationScoped
public class PollingActivityCacheProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingActivityCacheProvider.class);

    @Inject
    @NamedCache("ShmPollingActivityCache")
    private Cache<Long, ShmPollingActivityData> cache;

    public void add(final long activityJobId, final ShmPollingActivityData shmPollingActivityData) {
        try {
            cache.put(activityJobId, shmPollingActivityData);
        } catch (final Exception ex) {
            LOGGER.error("Failed to update ShmPollingActivityData in cluster cache for the activityJobId: {}. Exception is: ", activityJobId, ex);
        }
    }

    public List<ShmPollingActivityData> get() {
        final List<ShmPollingActivityData> shmPollingActivityDataList = new ArrayList<>();
        try {
            final Iterator<Entry<Long, ShmPollingActivityData>> iterator = cache.iterator();
            while (iterator.hasNext()) {
                final Entry<Long, ShmPollingActivityData> shmPollingActivityCacheEntry = iterator.next();
                shmPollingActivityDataList.add(shmPollingActivityCacheEntry.getValue());
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to get ShmPollingActivityData in cluster cache. Exception is: ", e);
        }
        return shmPollingActivityDataList;
    }

    public void remove(final long activityJobId) {
        try {
            if (cache.get(activityJobId) != null) {
                cache.remove(activityJobId);
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to get ShmPollingActivityData in cluster cache. Exception is: ", e);
        }
    }
}
