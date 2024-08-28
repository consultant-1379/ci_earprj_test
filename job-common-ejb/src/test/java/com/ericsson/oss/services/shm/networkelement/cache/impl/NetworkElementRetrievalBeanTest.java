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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData;
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;

@RunWith(MockitoJUnitRunner.class)
public class NetworkElementRetrievalBeanTest {

    @Mock
    private NetworkElementCacheProvider networkElementCache;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Mock
    NetworkElementData networkElementInfo;

    @Mock
    NetworkElement networkElement;

    @Mock
    List<String> listOfNetworkElementNames;

    @Mock
    ProductData neProductVersion;

    @InjectMocks
    private NetworkElementRetrievalBean objectUnderTest;

    final static String networkElementName = "LTE01";
    final static String neType = "ERBS";
    final static String revision = "R4D71";
    final static String identity = "CXP123";
    final static String networkElementFdn = "NetworkElement=" + networkElementName;
    final static String nodeRootFdn = "MeContext=" + networkElementName;
    final static String ossModelIdentity = "123";
    final static String nodeModelIdentity = "2334";
    final static String utcOffset = "2:00";

    @Test
    public void testGetNeType() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final String actualNeType = objectUnderTest.getNeType(networkElementName);
        assertEquals(neType, actualNeType);

    }

    @Test(expected = MoNotFoundException.class)
    public void testGetNeTypeAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        objectUnderTest.getNeType(networkElementName);
    }

    @Test
    public void testGetUtcOffset() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final String actualUtcOffset = objectUnderTest.getUtcOffset(networkElementName);
        assertEquals(utcOffset, actualUtcOffset);
    }

    @Test(expected = MoNotFoundException.class)
    public void testGetUtcOffsetAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        objectUnderTest.getUtcOffset(networkElementName);
    }

    @Test
    public void testGetNodeModelIdentity() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final String actualNodeModelIdentity = objectUnderTest.getNodeModelIdentity(networkElementName);
        assertEquals(nodeModelIdentity, actualNodeModelIdentity);

    }

    @Test(expected = MoNotFoundException.class)
    public void testGetNodeModelIdentityAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        objectUnderTest.getNodeModelIdentity(networkElementName);
    }

    @Test
    public void testGetNeFdn() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final String actualNeFdn = objectUnderTest.getNeFdn(networkElementName);
        assertEquals(networkElementFdn, actualNeFdn);

    }

    @Test(expected = MoNotFoundException.class)
    public void testGetNeFdnAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        objectUnderTest.getNeFdn(networkElementName);
    }

    @Test
    public void testGetNodeRootFdn() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final String actualNodeRootFdn = objectUnderTest.getNodeRootFdn(networkElementName);
        assertEquals(nodeRootFdn, actualNodeRootFdn);

    }

    @Test(expected = MoNotFoundException.class)
    public void testGetNodeRootFdnAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        objectUnderTest.getNodeRootFdn(networkElementName);
    }

    @Test
    public void testGetOssModelIdentity() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final String actualOssModelIdentity = objectUnderTest.getOssModelIdentity(networkElementName);
        assertEquals(ossModelIdentity, actualOssModelIdentity);

    }

    @Test(expected = MoNotFoundException.class)
    public void testGetOssModelIdentityAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        objectUnderTest.getOssModelIdentity(networkElementName);
    }

    @Test
    public void testGetNeProductVersion() throws MoNotFoundException {
        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        getNetworkElement(networkElementName);
        final List<Map<String, String>> actualNeProductVersion = objectUnderTest.getNeProductVersion(networkElementName);
        assertEquals(productDetails, actualNeProductVersion);
    }

    @Test(expected = MoNotFoundException.class)
    public void testGetNeProductVersionAsNull() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        final List<Map<String, String>> actualNeProductVersion = objectUnderTest.getNeProductVersion(networkElementName);
        assertNull(actualNeProductVersion);
    }

    @Test
    public void testGetNetworkElementFromDps() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final NetworkElementData actualNetworkElementInfo = objectUnderTest.getNetworkElementData(networkElementName);
        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        final NetworkElementData expectedNetworkElementInfo = new NetworkElementAttributes(neType, productDetails, networkElementFdn, nodeRootFdn, ossModelIdentity, utcOffset, nodeModelIdentity);
        assertEquals(expectedNetworkElementInfo.toString(), actualNetworkElementInfo.toString());
        verify(networkElementCache).add(Matchers.any(String.class), Matchers.any(NetworkElementData.class));
    }

    @Test
    public void testGetNetworkElementFromCache() throws MoNotFoundException {
        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        final NetworkElementData expectedNetworkElementInfo = new NetworkElementAttributes(neType, productDetails, networkElementFdn, nodeRootFdn, ossModelIdentity, utcOffset, nodeModelIdentity);
        when(networkElementCache.get(networkElementName)).thenReturn(expectedNetworkElementInfo);
        final NetworkElementData actualNetworkElementInfo = objectUnderTest.getNetworkElementData(networkElementName);
        assertEquals(expectedNetworkElementInfo.toString(), actualNetworkElementInfo.toString());
        verify(networkElementCache, times(0)).add(Matchers.any(String.class), Matchers.any(NetworkElementData.class));
    }

    @Test
    public void testGetNetworkElementFromCache_whenCacheCreationFailed() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        final NetworkElementData expectedNetworkElementInfo = new NetworkElementAttributes(neType, productDetails, networkElementFdn, nodeRootFdn, ossModelIdentity, utcOffset, nodeModelIdentity);
        when(networkElementCache.get(networkElementName)).thenThrow(RuntimeException.class);
        final NetworkElementData actualNetworkElementInfo = objectUnderTest.getNetworkElementData(networkElementName);
        assertEquals(expectedNetworkElementInfo.toString(), actualNetworkElementInfo.toString());
        verify(networkElementCache, times(0)).add(Matchers.any(String.class), Matchers.any(NetworkElementData.class));
    }

    @Test
    public void testGetNetworkElementFromCache_whenFetchDataFromDBAndCacheUpdateFailed() throws MoNotFoundException {
        getNetworkElement(networkElementName);
        final List<Map<String, String>> productDetails = new ArrayList<Map<String, String>>();
        final Map<String, String> productVersion = new HashMap<String, String>();
        productVersion.put(ShmConstants.REVISION, revision);
        productVersion.put(ShmConstants.IDENTITY, identity);
        productDetails.add(productVersion);
        final NetworkElementData expectedNetworkElementInfo = new NetworkElementAttributes(neType, productDetails, networkElementFdn, nodeRootFdn, ossModelIdentity, utcOffset, nodeModelIdentity);
        when(networkElementCache.get(networkElementName)).thenReturn(expectedNetworkElementInfo);
        Mockito.doThrow(Exception.class).when(networkElementCache).add(networkElementName, expectedNetworkElementInfo);
        final NetworkElementData actualNetworkElementInfo = objectUnderTest.getNetworkElementData(networkElementName);
        assertEquals(expectedNetworkElementInfo.toString(), actualNetworkElementInfo.toString());
        verify(networkElementCache, times(0)).add(Matchers.any(String.class), Matchers.any(NetworkElementData.class));
    }

    @Test(expected = MoNotFoundException.class)
    public void testGetNetworkElementWhenNotFoundInDps() throws MoNotFoundException {
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(null);
        final NetworkElementData actualNetworkElementInfo = objectUnderTest.getNetworkElementData(networkElementName);
        assertNull(actualNetworkElementInfo);
        verify(networkElementCache, times(0)).add(Matchers.any(String.class), Matchers.any(NetworkElementData.class));
    }

    private void getNetworkElement(final String networkElementName) {
        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElement);
        when(fdnServiceBeanRetryHelper.getNetworkElementsWithoutPlatformInfo(Arrays.asList(networkElementName))).thenReturn(networkElements);
        final List<ProductData> productData = new ArrayList<ProductData>();
        productData.add(neProductVersion);
        when(networkElement.getNeProductVersion()).thenReturn(productData);
        when(neProductVersion.getRevision()).thenReturn(revision);
        when(neProductVersion.getIdentity()).thenReturn(identity);
        when(networkElement.getNeType()).thenReturn(neType);
        when(networkElement.getNetworkElementFdn()).thenReturn(networkElementFdn);
        when(networkElement.getNodeRootFdn()).thenReturn(nodeRootFdn);
        when(networkElement.getOssModelIdentity()).thenReturn(ossModelIdentity);
        when(networkElement.getUtcOffset()).thenReturn(utcOffset);
        when(networkElement.getNodeModelIdentity()).thenReturn(nodeModelIdentity);
    }
}