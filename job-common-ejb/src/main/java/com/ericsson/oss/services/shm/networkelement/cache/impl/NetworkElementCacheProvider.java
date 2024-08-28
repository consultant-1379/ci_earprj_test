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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectDeletedEvent;
import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;

@ApplicationScoped
public class NetworkElementCacheProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkElementCacheProvider.class);

    @Inject
    @NamedCache("NetworkElementMOCache")
    private Cache<String, NetworkElementData> cache;

    public void add(final String networkElementName, final NetworkElementData networkElementData) {
        try {
            cache.put(networkElementName, networkElementData);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to update NetworkElementData in cluster cache for the node: {}. Exception is: {} ", networkElementName, ex.getMessage());
        }
    }

    public NetworkElementData get(final String networkElementName) {
        return cache.get(networkElementName);
    }

    public void remove(final DpsDataChangedEvent message) {
        final DpsObjectDeletedEvent deletedEvent = (DpsObjectDeletedEvent) message;
        final String nodeName = FdnUtils.getNodeName(deletedEvent.getFdn());
        if (cache != null && cache.containsKey(nodeName)) {
            cache.remove(nodeName);
        }

    }

    @SuppressWarnings("unchecked")
    public void update(final DpsDataChangedEvent message) {
        final DpsAttributeChangedEvent changeEvent = (DpsAttributeChangedEvent) message;
        final String nodeName = FdnUtils.getNodeName(changeEvent.getFdn());
        final Set<AttributeChangeData> attributeData = changeEvent.getChangedAttributes();
        final Iterator<AttributeChangeData> attributeDataItr = attributeData.iterator();

        while (attributeDataItr.hasNext()) {
            final AttributeChangeData attributeChangeData = attributeDataItr.next();

            final NetworkElementData networkElementData = get(nodeName);

            if (networkElementData != null) {
                switch (attributeChangeData.getName()) {
                case ShmCommonConstants.NE_PRODUCT_VERSION:

                    final List<Map<String, String>> neProductVersion = (List<Map<String, String>>) attributeChangeData.getNewValue();
                    networkElementData.setNeProductVersion(neProductVersion);
                    break;

                case ShmCommonConstants.NETYPE:
                    final String neType = (String) attributeChangeData.getNewValue();
                    networkElementData.setNeType(neType);
                    break;

                case ShmCommonConstants.NE_FDN:
                    final String neFdn = (String) attributeChangeData.getNewValue();
                    networkElementData.setNeFdn(neFdn);
                    break;

                case ShmCommonConstants.NODE_ROOT_FDN:
                    final String nodeRootFdn = (String) attributeChangeData.getNewValue();
                    networkElementData.setNodeRootFdn(nodeRootFdn);

                    break;

                case ShmCommonConstants.OSS_MODEL_IDENTITY:
                    final String ossModelIdentity = (String) attributeChangeData.getNewValue();
                    networkElementData.setOssModelIdentity(ossModelIdentity);
                    break;

                case ShmCommonConstants.NODE_MODEL_IDENTITY:
                    final String nodeModelIdentity = (String) attributeChangeData.getNewValue();
                    networkElementData.setNodeModelIdentity(nodeModelIdentity);
                    break;

                case ShmCommonConstants.UTC_OFFSET:
                    final String utcOffset = (String) attributeChangeData.getNewValue();
                    networkElementData.setUtcOffset(utcOffset);
                    break;

                default:
                    LOGGER.warn("AVC event received for attribute {} and Event is {}", attributeChangeData.getName(), attributeChangeData);

                }
            }

            else {
                LOGGER.trace("Network Element not found in NE cache for the node: {}", nodeName);
            }

        }
    }
}
