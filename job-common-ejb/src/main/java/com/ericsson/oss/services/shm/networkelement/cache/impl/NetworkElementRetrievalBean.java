/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.networkelement.cache.impl;

import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData;
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;

public class NetworkElementRetrievalBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkElementRetrievalBean.class);

    @Inject
    private NetworkElementCacheProvider networkElementCache;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    public String getNeType(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);

        return networkElement != null ? networkElement.getNeType() : null;
    }

    public List<Map<String, String>> getNeProductVersion(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);
        return networkElement != null ? networkElement.getNeProductVersion() : null;
    }

    public String getUtcOffset(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);
        return networkElement != null ? networkElement.getUtcOffset() : null;
    }

    public String getNodeModelIdentity(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);
        return networkElement != null ? networkElement.getNodeModelIdentity() : null;
    }

    public String getNeFdn(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);
        return networkElement != null ? networkElement.getNeFdn() : null;
    }

    public String getNodeRootFdn(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);
        return networkElement != null ? networkElement.getNodeRootFdn() : null;
    }

    public String getOssModelIdentity(final String networkElementName) throws MoNotFoundException {

        final NetworkElementData networkElement = getNetworkElementData(networkElementName);
        return networkElement != null ? networkElement.getOssModelIdentity() : null;
    }

    public NetworkElementData getNetworkElementData(final String networkElementName) throws MoNotFoundException {

        NetworkElementData networkElementData = getNeDataFromCache(networkElementName);

        //if networkElementData from cache is null then read it from DB
        if (networkElementData == null) {
            LOGGER.info("Cache created but NetworkElement data not exists in Cache. Hence fetching from Database by using node name: {}", networkElementName);
            networkElementData = getNeDataFromDB(networkElementName);
            networkElementCache.add(networkElementName, networkElementData);
        }
        return networkElementData;
    }

    private NetworkElementData getNeDataFromCache(final String networkElementName) throws MoNotFoundException {
        try {

            return networkElementCache.get(networkElementName);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to fetch NetworkElementData from cluster cache for the node  {}. Exception is: {} ", networkElementName, ex.getMessage());
            // should not update in cache as cache itself is not created.
            return getNeDataFromDB(networkElementName);
        }
    }

    private NetworkElementData getNeDataFromDB(final String networkElementName) throws MoNotFoundException {

        NetworkElementData networkElementData = null;
        try {
            final List<NetworkElement> networkElements = fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName));
            if (networkElements != null && !networkElements.isEmpty()) {
                final NetworkElement networkElement = networkElements.get(0);
                final List<ProductData> productData = networkElement.getNeProductVersion();
                final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
                for (final ProductData neProductVersion : productData) {
                    final Map<String, String> productVersion = new HashMap<String, String>();
                    productVersion.put(ShmConstants.REVISION, neProductVersion.getRevision());
                    productVersion.put(ShmConstants.IDENTITY, neProductVersion.getIdentity());
                    productDetails.add(productVersion);
                }
                networkElementData = new NetworkElementAttributes(networkElement.getNeType(), productDetails, networkElement.getNetworkElementFdn(), networkElement.getNodeRootFdn(),
                        networkElement.getOssModelIdentity(), networkElement.getUtcOffset(), networkElement.getNodeModelIdentity());
            } else {
                LOGGER.error("NetworkElement not found for the node name: {}", networkElementName);
                throw new MoNotFoundException(String.format("NetworkElement not found for the node name %s", networkElementName));
            }
        } catch (final Exception ex) {
            LOGGER.error(String.format("Exception occurred while fetching Network Element MO for node %s. ", networkElementName) + "Exception is: {}", ex);
            throw new MoNotFoundException(String.format("Exception occurred while fetching Network Element MO for node %s", networkElementName));
        }
        return networkElementData;
    }

}
