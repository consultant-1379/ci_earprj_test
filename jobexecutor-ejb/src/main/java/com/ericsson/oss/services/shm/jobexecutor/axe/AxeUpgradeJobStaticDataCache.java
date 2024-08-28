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
package com.ericsson.oss.services.shm.jobexecutor.axe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.services.shm.job.axe.cache.AxeNeUpgradeCacheData;
import com.ericsson.oss.services.shm.job.axe.cache.AxeUpgradeJobStaticDataProvider;

/**
 * This class holds Axe UpgradeJob Static Data for Cache which contains the main jobid against each netype and against which all AXE nes under it with their cpfunctions and number of APGS information
 * 
 * @author tcsanpr
 * 
 */
@ApplicationScoped
public class AxeUpgradeJobStaticDataCache implements AxeUpgradeJobStaticDataProvider {

    /**
     * axeUpgradeJobStaticData map has <mainJobId, <neType <AxeNeName, AxeNeUpgradeCacheData>>> and AxeNeUpgradeCacheData object has cpFunction name and number of Apgs as attributes
     */
    private final Map<Long, Map<String, Map<String, AxeNeUpgradeCacheData>>> axeUpgradeJobStaticData = new ConcurrentHashMap<>();

    @Override
    public void put(final long mainJobId, final Map<String, Map<String, AxeNeUpgradeCacheData>> jobStaticContext) {
        axeUpgradeJobStaticData.put(mainJobId, jobStaticContext);
    }

    @Override
    public Map<String, Map<String, AxeNeUpgradeCacheData>> getAxeNeUpgradeCacheData(final long mainJobId) {
        return axeUpgradeJobStaticData.get(mainJobId);
    }

    @Override
    public void clear(final long mainJobId) {
        if (axeUpgradeJobStaticData.get(mainJobId) != null) {
            axeUpgradeJobStaticData.remove(mainJobId);
        }
    }

    @Override
    public void clearAll() {
        axeUpgradeJobStaticData.clear();
    }
}
