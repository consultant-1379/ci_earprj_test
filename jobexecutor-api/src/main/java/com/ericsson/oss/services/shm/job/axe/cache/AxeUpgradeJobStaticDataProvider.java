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
package com.ericsson.oss.services.shm.job.axe.cache;

import java.util.Map;

public interface AxeUpgradeJobStaticDataProvider {

    /**
     * This method will be called from elementary services to get the AxeNeUpgradeCacheData from cache. It will get JobStaticData from DPS if not exists in cache.
     * 
     * @param mainJobId
     * @return AxeNeUpgradeCacheData
     */

    Map<String, Map<String, AxeNeUpgradeCacheData>> getAxeNeUpgradeCacheData(final long mainJobId);

    /**
     * This method will clear the cache object mapped with given mainJobId.
     * 
     * @param mainJobId
     */
    void clear(final long mainJobId);

    /**
     * This method will clear the cache.
     * 
     */
    void clearAll();

    /**
     * This method adds the AxeNeUpgradeCacheData into cache.
     * 
     * @param mainJobId
     * @param AxeNeUpgradeCacheData
     */

    void put(final long mainJobId, final Map<String, Map<String, AxeNeUpgradeCacheData>> axeNeUpgradeCacheData);

}
