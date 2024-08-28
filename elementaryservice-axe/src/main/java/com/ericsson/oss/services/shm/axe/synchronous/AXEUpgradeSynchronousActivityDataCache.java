/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.axe.synchronous;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.services.shm.activity.axe.cache.AxeSynchronousActivityData;

/**
 * Class defines two cache's one to store list of synchronous activities in given mainJob and neType ,another to store cache status of given given mainJob and neType
 * 
 * @author ztamsra
 *
 */
@ApplicationScoped
public class AXEUpgradeSynchronousActivityDataCache {

    private final Map<String, List<AxeSynchronousActivityData>> synchronousActivityCache = new ConcurrentHashMap<>();

    private final Map<String, Boolean> synchronousActivityCacheStatus = new ConcurrentHashMap<>();

    /**
     * id is combination of mainJobId_NeType,
     */
    public void put(final String id, final List<AxeSynchronousActivityData> activityStaticDataList) {
        this.synchronousActivityCache.put(id, activityStaticDataList);
    }

    public void putAll(final Map<String, List<AxeSynchronousActivityData>> activityStaticCache) {
        this.synchronousActivityCache.putAll(activityStaticCache);
    }

    public List<AxeSynchronousActivityData> get(final String id) {
        return synchronousActivityCache.get(id);
    }

    public Boolean getSyncCompletionStatus(final String id) {
        if (synchronousActivityCacheStatus.get(id) == null) {
            return false;
        }
        return synchronousActivityCacheStatus.get(id);
    }

    public void clear(final String id) {
        if (synchronousActivityCache.get(id) != null) {
            synchronousActivityCache.remove(id);
        }
    }

    public void updateSyncCompletionStatus(final String id, final boolean isSyncCompleted) {
        this.synchronousActivityCacheStatus.put(id, isSyncCompleted);
    }

    public void clearAll() {
        synchronousActivityCache.clear();
        synchronousActivityCacheStatus.clear();
    }

    public Set<String> getCacheKeys() {
        return synchronousActivityCache.keySet();
    }

}
