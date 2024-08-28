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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;

@RunWith(MockitoJUnitRunner.class)
public class NetworkElementCacheProviderTest {

    @InjectMocks
    NetworkElementCacheProvider objectUnderTest;

    @Mock
    DpsAttributeChangedEvent message;

    @Mock
    Iterator<AttributeChangeData> attributeDataItr;

    @Mock
    AttributeChangeData attributeChangeData;

    @Mock
    @NamedCache("NeAttributesNotificationRegistryCache")
    private Cache<String, NetworkElementData> cache;

    @Mock
    NetworkElementData networkElementInfo;

    final static String networkElementName = "LTE01";
    final static String fdn = "NetworkElement=" + networkElementName;
    final static String revision = "R4D71";
    final static String identity = "CXP123";
    final static String neType = "ERBS";
    final static String nodeRootFdn = "MeContext=" + networkElementName;
    final static String ossModelIdentity = "123";
    final static String nodeModelIdentity = "123";
    final static String utcOffset = "2:00";
    final static String platform = "CPP";

    @Test
    public void testUpdateForNeProductVersionInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.NE_PRODUCT_VERSION);
        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        when(attributeChangeData.getNewValue()).thenReturn(productDetails);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setNeProductVersion(productDetails);
    }

    @Test
    public void testUpdateForNeTypeInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.NETYPE);
        when(attributeChangeData.getNewValue()).thenReturn(neType);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setNeType(neType);
    }

    @Test
    public void testUpdateForNeFdnInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.NE_FDN);
        when(attributeChangeData.getNewValue()).thenReturn(fdn);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setNeFdn(fdn);

    }

    @Test
    public void testUpdateForNodeRootFdnInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.NODE_ROOT_FDN);
        when(attributeChangeData.getNewValue()).thenReturn(nodeRootFdn);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setNodeRootFdn(nodeRootFdn);
    }

    @Test
    public void testUpdateForOssModelIdentityInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.OSS_MODEL_IDENTITY);
        when(attributeChangeData.getNewValue()).thenReturn(ossModelIdentity);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setOssModelIdentity(ossModelIdentity);
    }

    @Test
    public void testUpdateForNodeModelIdentityInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.NODE_MODEL_IDENTITY);
        when(attributeChangeData.getNewValue()).thenReturn(nodeModelIdentity);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setNodeModelIdentity(nodeModelIdentity);
    }

    @Test
    public void testUpdateForUtcOffsetInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.UTC_OFFSET);
        when(attributeChangeData.getNewValue()).thenReturn(utcOffset);

        objectUnderTest.update(dpsAttributeChangeEvent);
        verify(networkElementInfo).setUtcOffset(utcOffset);

    }

    @Test
    public void testUpdateForAnyOtherAttributeInNotification() {
        final Set<AttributeChangeData> attributeData = new HashSet<AttributeChangeData>();
        attributeData.add(attributeChangeData);
        final DpsAttributeChangedEvent dpsAttributeChangeEvent = new DpsAttributeChangedEvent("namespace", "type", "version", 123L, fdn, "bucketName", attributeData);

        when(message.getChangedAttributes()).thenReturn(attributeData);
        when(cache.get(networkElementName)).thenReturn(networkElementInfo);
        when(attributeChangeData.getName()).thenReturn(ShmCommonConstants.PLATFORM);
        when(attributeChangeData.getNewValue()).thenReturn(platform);

        objectUnderTest.update(dpsAttributeChangeEvent);

        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        verify(networkElementInfo, times(0)).setNeProductVersion(productDetails);
        verify(networkElementInfo, times(0)).setNeType(neType);
        verify(networkElementInfo, times(0)).setNeFdn(fdn);
        verify(networkElementInfo, times(0)).setNodeRootFdn(nodeRootFdn);
        verify(networkElementInfo, times(0)).setOssModelIdentity(ossModelIdentity);
        verify(networkElementInfo, times(0)).setNodeModelIdentity(nodeModelIdentity);
        verify(networkElementInfo, times(0)).setUtcOffset(utcOffset);
    }
}
