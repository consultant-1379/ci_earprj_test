/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.instantaneouslicensing;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

@RunWith(MockitoJUnitRunner.class)
public class InstantaneousLicensingNesValidateServiceTest {

    @InjectMocks
    private InstantaneousLicensingNesValidateService objectUnderTest;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private QueryExecutor queryExecutorMock;

    @Mock
    private QueryBuilder queryBuilderMock;

    @Mock
    private Query<TypeRestrictionBuilder> queryMock;

    @Mock
    private Restriction restriction;

    @Mock
    private DataBucket dataBucketMock;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilderMock;

    @Test
    public void testFilterInstantaniousLicensingSupportedNes_5GSupported() {
        queryMock();
        when(queryExecutorMock.executeProjection(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(prepareNetworkElementResponse("5GS"));
        final Map<NetworkElement, String> networkElementResponse = objectUnderTest.filterInstantaneousLicensingSupportedNes(prepareNetworkElements());
        assertNotNull(networkElementResponse);
        verifyAsserts();
    }

    @Test
    public void testFilterInstantaniousLicensingSupportedNes_4GSupported() {
        queryMock();
        when(queryExecutorMock.executeProjection(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(prepareNetworkElementResponse("EPS"));
        final Map<NetworkElement, String> networkElementResponse = objectUnderTest.filterInstantaneousLicensingSupportedNes(prepareNetworkElements());
        assertNotNull(networkElementResponse);
        verifyAsserts();
    }

    @Test
    public void testFilterInstantaniousLicensingSupportedNes_4G_5G_Supported() {
        queryMock();
        when(queryExecutorMock.executeProjection(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(prepareGenericNetworkElementResponse());
        final Map<NetworkElement, String> networkElementResponse = objectUnderTest.filterInstantaneousLicensingSupportedNes(prepareNetworkElements());
        assertNotNull(networkElementResponse);
        verifyAsserts();
    }

    @Test
    public void testFilterInstantaniousLicensingSupportedNes_Empty_Domains() {
        queryMock();
        when(queryExecutorMock.executeProjection(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(prepareNetworkElementResponse(""));
        final Map<NetworkElement, String> networkElementResponse = objectUnderTest.filterInstantaneousLicensingSupportedNes(prepareNetworkElements());
        assertNotNull(networkElementResponse);
        verifyAsserts();
    }

    private List<Object[]> prepareNetworkElementResponse(final String technologyDomain) {
        final List<String> technologyDomains = new ArrayList<>();
        technologyDomains.add(technologyDomain);
        final List<Object[]> networkElementResponse = new ArrayList<Object[]>();
        final Object[] object1 = { "NetworkElement=supportedNode1", technologyDomains };
        final Object[] object2 = { "NetworkElement=supportedNode2", technologyDomains };
        final Object[] object3 = { "NetworkElement=supportedNode3", technologyDomains };
        networkElementResponse.add(object1);
        networkElementResponse.add(object2);
        networkElementResponse.add(object3);
        return networkElementResponse;
    }

    private List<Object[]> prepareGenericNetworkElementResponse() {
        final List<String> technologyDomains = new ArrayList<>();
        technologyDomains.add("EPS");
        technologyDomains.add("5GS");
        final List<Object[]> networkElementResponse = new ArrayList<Object[]>();
        final Object[] object1 = { "NetworkElement=supportedNode1", technologyDomains };
        final Object[] object2 = { "NetworkElement=supportedNode2", technologyDomains };
        final Object[] object3 = { "NetworkElement=supportedNode3", technologyDomains };
        networkElementResponse.add(object1);
        networkElementResponse.add(object2);
        networkElementResponse.add(object3);
        return networkElementResponse;
    }

    private List<NetworkElement> prepareNetworkElements() {
        final List<NetworkElement> networkElementsToFilter = new ArrayList<>();

        final NetworkElement networkElement1 = new NetworkElement();
        networkElement1.setName("supportedNode1");
        networkElement1.setNeType("RadioNode");
        networkElement1.setNetworkElementFdn("NetworkElement=supportedNode1");

        final NetworkElement networkElement2 = new NetworkElement();
        networkElement2.setName("supportedNode2");
        networkElement2.setName("RadioNode");
        networkElement2.setNetworkElementFdn("NetworkElement=supportedNode2");

        networkElementsToFilter.add(networkElement1);
        networkElementsToFilter.add(networkElement2);

        return networkElementsToFilter;
    }

    private void queryMock() {
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.getQueryExecutor()).thenReturn(queryExecutorMock);
        when(queryBuilderMock.createTypeQuery("OSS_NE_DEF", "NetworkElement")).thenReturn(queryMock);
        when(queryMock.getRestrictionBuilder()).thenReturn(typeRestrictionBuilderMock);
        when(typeRestrictionBuilderMock.in(Matchers.anyString(), Matchers.any())).thenReturn(restriction);
    }

    private void verifyAsserts() {
        verify(dataPersistenceService).getQueryBuilder();
        verify(dataPersistenceService).getLiveBucket();
        verify(dataBucketMock).getQueryExecutor();
        verify(queryMock).getRestrictionBuilder();
        verify(queryBuilderMock).createTypeQuery("OSS_NE_DEF", "NetworkElement");
        verify(queryExecutorMock).executeProjection(Matchers.any(), Matchers.any(), Matchers.any());
    }

}
