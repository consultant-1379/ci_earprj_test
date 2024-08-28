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
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

@RunWith(MockitoJUnitRunner.class)
public class BackupMOInformationProviderTest {

    @InjectMocks
    private BackupMOInformationProvider backupMOInformationProvider;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Query<ContainmentRestrictionBuilder> query;

    @Mock
    private ContainmentRestrictionBuilder containmentRestrictionBuilder;

    @Mock
    private Restriction restriction;

    @Mock
    private ManagedObject managedObject;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private NetworkElementData networkElement;

    @Mock
    private EcimBackupInfo ecimBackupInfo;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private EcimBackupUtils ecimBackupUtils;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Test
    public void testGetswVersionsListFromBrmBackupMOsList() throws MoNotFoundException, UnsupportedFragmentException {
        List<Map<String, String>> swVersions = new ArrayList<>();
        Map<String, String> swVersionMap = new HashMap<>();
        List<Object> backupMOsList = new ArrayList<>();
        final String backUpName = "backupTest";
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(networkElement.getNodeRootFdn()).thenReturn("ManagedElement=LTE62VTFRadioNode00001,SystemFunctions=1,BrM=1,BrmBackupManager=1,BrmBackup=1");
        when(queryBuilder.createContainmentQuery(networkElement.getNodeRootFdn())).thenReturn(query);
        when(query.getRestrictionBuilder()).thenReturn(containmentRestrictionBuilder);
        when(containmentRestrictionBuilder.equalTo(ObjectField.TYPE, "BrmBackup")).thenReturn(restriction);
        when(managedObject.getAttribute("backupName")).thenReturn(backUpName);
        when(ecimBackupInfo.getBackupName()).thenReturn(backUpName);
        backupMOsList.add(managedObject);
        when(managedObject.getAttribute("swVersion")).thenReturn(swVersions);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.getResultList(query)).thenReturn(backupMOsList);
        swVersionMap.put("productRevision", "RA1234");
        swVersionMap.put("productNumber", "CXP2010055/1");
        swVersions.add(swVersionMap);
        when(networkElement.getNeProductVersion()).thenReturn(swVersions);
        backupMOInformationProvider.getswVersionsListFromBrmBackupMOsList(networkElement, ecimBackupInfo);
        verify(dataPersistenceService, times(1)).getQueryBuilder();
        verify(queryBuilder, times(1)).createContainmentQuery(networkElement.getNodeRootFdn());
        verify(query, times(1)).getRestrictionBuilder();
        verify(dataPersistenceService, times(1)).getLiveBucket();
        verify(dataBucket, times(1)).getQueryExecutor();
        verify(queryExecutor, times(1)).getResultList(query);
    }

    @Test(expected = ServerInternalException.class)
    public void testServerInternalExceptionWhenDpsBeanIsNotCreated() {
        backupMOInformationProvider.getswVersionsListFromBrmBackupMOsList(networkElement, ecimBackupInfo);
    }

}
