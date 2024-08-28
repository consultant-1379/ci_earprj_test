/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.nejob.cache;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;

/**
 * Provides method/methods to get NE Job static data.
 * 
 * @author tcsgusw
 * 
 */
public interface NeJobStaticDataProvider {

    /**
     * This method will be called from elementary services to get the NeJobStaticData from cache. It will get NeJobStaticData from DPS if not exists in cache.
     * 
     * @param activityJobId
     * @param capability
     * @return
     * @throws JobDataNotFoundException
     */

    NEJobStaticData getNeJobStaticData(final long activityJobId, final String capability) throws JobDataNotFoundException;

    /**
     * This method will be called from job execution while updating activity state to running to update activityStartTime in NeJobStaticData cache. If NeJobStaticData not exists in cache, this method
     * will fetch the static data and updates in cache.
     * 
     * @param activityJobId
     * @param platformCapbility
     * @param activityStartTime
     * @return
     * @throws JobDataNotFoundException
     */
    void updateNeJobStaticDataCache(final long activityJobId, final String platformCapbility, final long activityStartTime) throws JobDataNotFoundException;

    /**
     * This method will clear the cache object mapped with given activityJobId.
     * 
     * @param activityJobId
     */
    void clear(final long activityJobId);

    /**
     * This method will clear the cache.
     * 
     */
    void clearAll();

    /**
     * This method adds the neJobStaticData into cache.
     * 
     * @param activityJobId
     * @param jobStaticContext
     */

    void put(final long activityJobId, final NEJobStaticData neJobStaticData);

    /**
     * @param activityJobId
     * @return ActivityStartTime
     */
    long getActivityStartTime(final long activityJobId) throws MoNotFoundException;

}
