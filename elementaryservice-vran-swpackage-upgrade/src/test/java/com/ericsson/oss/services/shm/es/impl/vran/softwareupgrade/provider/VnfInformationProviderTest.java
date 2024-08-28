/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class VnfInformationProviderTest extends ProviderTestBase {

    @InjectMocks
    private VnfInformationProvider vnfInformationProvider;

    @Mock
    private Iterator<Object> iterator;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private Query<ContainmentRestrictionBuilder> query;

    @Mock
    private Query<TypeRestrictionBuilder> typeRestrictionQuery;

    @Mock
    private ContainmentRestrictionBuilder containmentRestrictionBuilder;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private Restriction restriction;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private ManagedObject managedObject;

    @Mock
    private PersistenceObject persistentObject;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    private static final String NODE_NAME = "string";
    private static final String NODE_FDN = "NetworkElement=" + NODE_NAME;
    private static final String EXECUTION_RESOURCE_MO_TYPE = "ExecutionResource";

    @Test
    public void testGetVnfIdListOfNetworkElement() {
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("vran");
        networkElement.setPlatformType(PlatformTypeEnum.vRAN);
        networkElement.setNodeRootFdn(NODE_FDN);
        networkElements.add(networkElement);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createContainmentQuery(networkElements.get(0).getNodeRootFdn())).thenReturn(query);
        when(query.getRestrictionBuilder()).thenReturn(containmentRestrictionBuilder);
        when(containmentRestrictionBuilder.equalTo(ObjectField.TYPE, EXECUTION_RESOURCE_MO_TYPE)).thenReturn(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(query)).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false, true, false);
        when(iterator.next()).thenReturn(managedObject);
        when(managedObject.getAttribute("vnfIdentity")).thenReturn("vnfId234");

        vnfInformationProvider.getVnfId(networkElements);
    }

    @Test
    public void testGetVnfIdLongStringStringString() {

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        Map<String, Object> nejobPropertyForVnfPackageId = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        neJobProperties.add(nejobPropertyForVnfPackageId);
        neJobAttributes.put(ShmConstants.JOBPROPERTIES, neJobProperties);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB)).thenReturn(typeRestrictionQuery);
        when(typeRestrictionQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ObjectField.NAME, "neName")).thenReturn(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(typeRestrictionQuery)).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false, true, false);
        when(iterator.next()).thenReturn(persistentObject);
        when(persistentObject.getAllAttributes()).thenReturn(neJobAttributes);

        vnfInformationProvider.getVnfId(activityJobId, "vnfId234", "vnfId234", "neName");
    }

    @Test
    public void testFetchNetworkElements() {
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("vran");
        networkElement.setPlatformType(PlatformTypeEnum.vRAN);
        networkElement.setNodeRootFdn(NODE_FDN);
        networkElements.add(networkElement);
        final List<String> nodeNames = new ArrayList<>();
        nodeNames.add(jobEnvironment.getNodeName());
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(nodeNames, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(networkElements);
        vnfInformationProvider.fetchNetworkElements(jobEnvironment);
    }

}
