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
package com.ericsson.oss.services.shm.activity.axe.cache;

import java.util.List;
import java.util.Map;

/**
 * Interface to operate the cache
 * 
 * @author ztamsra
 *
 */
public interface AxeUpgradeSynchronousActivityProvider {

    /**
     * @param mainJobId
     * @param neType
     * @param synchronousActivities--Updated
     *            synchronous Activities List
     */
    void put(long mainJobId, String neType, List<AxeSynchronousActivityData> synchronousActivities);

    /**
     * @param mainJobId
     * @param neType
     * @return Map<String, List<AxeSynchronousActivityData>> with id combination of mainJob ,neType and Synchronous Activities list
     */
    Map<String, List<AxeSynchronousActivityData>> getSynchronousAxeActivities(long mainJobId, String neType);

    /**
     * @param mainJobId
     * @param neType
     * @return Synchronous Activities List for given id
     */
    List<AxeSynchronousActivityData> get(long mainJobId, String neType);

    void updateAxeSynchronousActivites(String key, List<AxeSynchronousActivityData> updatedAxeStaticList);

    /**
     * Update the Cache after all the sync activities has been completed under given mainJob and neType
     * 
     * @param mainJobId
     * 
     * @param cacheKey
     * @param isSyncCompleted
     */
    void updateSyncCompleted(long mainJobId, String neType);

    /**
     * Clear the Synchronous activities for given id
     * 
     * @param mainJobId
     * @param neType
     */
    void clear(long mainJobId, String neType);

    /**
     * Clear both synchronousActivityCache synchronousActivityCacheStatus
     */
    void clearAll();

    /**
     * Update the Cache after sync activity notified under given mainJob and neType
     * 
     * @param mainJobId
     * 
     * @param cacheKey
     * @param isSyncCompleted
     */
    void updateSyncCompleted(long mainJobId, String neType, int orderId);

    boolean getSyncCompletedStatusFromCache(long mainJobId, String neType);

    /**
     * @param cacheKey
     */
    void updateSyncCompletedStatusInCache(String cacheKey);

}
