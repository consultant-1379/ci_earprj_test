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
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.NetworkElementFinder;
import com.ericsson.oss.services.shm.common.enums.NetworkElementNamePattern;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;

/**
 * Helper class to retrieve the NetworkElements based on search criteria
 * 
 * @author tcsmaup
 * 
 */
@Stateless
public class NetworkElementFinderHelper {

    @Inject
    private NetworkElementFinder networkElementFinder;

    /**
     * function to retrieve the NetworkElements based on search criteria
     * 
     * @param shmRemoteJobData
     */
    public void findAndAddNeNamesFromSearchScopes(final ShmRemoteJobData shmRemoteJobData) {
        Set<String> neNames = new HashSet<String>();
        if (shmRemoteJobData.getNeNames() != null) {
            neNames = shmRemoteJobData.getNeNames();
        }
        for (Entry<String, NetworkElementNamePattern> neSearchScope : shmRemoteJobData.getNetworkElementSearchScopes().entrySet()) {
            neNames.addAll(networkElementFinder.search(neSearchScope.getKey(), neSearchScope.getValue()));
        }
        shmRemoteJobData.setNeNames(neNames);
    }
}
