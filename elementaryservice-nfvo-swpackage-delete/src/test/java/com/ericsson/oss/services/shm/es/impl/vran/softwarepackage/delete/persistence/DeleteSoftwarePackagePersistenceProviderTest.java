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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.*;

import javax.resource.ResourceException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

@RunWith(MockitoJUnitRunner.class)
public class DeleteSoftwarePackagePersistenceProviderTest {

    @InjectMocks
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Mock
    private DataPersistenceService dataPersistenceServiceMock;

    @Mock
    private QueryBuilder queryBuilderMock;

    @Mock
    private Query<TypeRestrictionBuilder> jobQueryMock;

    @Mock
    private Query<TypeRestrictionBuilder> upPkgQueryMock;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilderMock;

    @Mock
    private Restriction restrictionMock;

    @Mock
    private DataBucket liveBucketMock;

    @Mock
    private QueryExecutor queryExecutorMock;

    @Mock
    private PersistenceObject persistenceObjectMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    private PersistenceObject persistenceObject;

    @Mock
    private PersistenceObject persistenceObject2;

    @Mock
    private DpsReader dpsReader;

    private static final String SOFTWARE_PACKAGE_NAME = "CXP1020511_R4D26_";

    private List<Object> persistentObjectList = null;

    private static final long TEMPLATE_ID = 0;

    @Test
    public void testDeleteSwPkgs_JobConfigDetails() throws ResourceException, IOException {
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<String> requiredJobPropertyKeys = new ArrayList<String>();
        final Map<String, String> propertyMap = new HashMap<String, String>();
        requiredJobPropertyKeys.add(VranJobConstants.VRAN_SOFTWAREPACKAGE_NAME);
        propertyMap.put(VranJobConstants.VRAN_SOFTWAREPACKAGE_NAME, SOFTWARE_PACKAGE_NAME);
        when(jobPropertyUtils.getPropertyValue(requiredJobPropertyKeys, jobConfigurationDetails)).thenReturn(propertyMap);

        when(persistenceObject.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(jobConfigurationDetails);
        when(persistenceObject.getAttribute(ShmConstants.JOBTEMPLATEID)).thenReturn(TEMPLATE_ID);
        when(dpsReader.findPOByPoId(TEMPLATE_ID)).thenReturn(persistenceObject);
        when(dpsReader.findPOByPoId(TEMPLATE_ID + 1)).thenReturn(persistenceObject2);
        when(persistenceObject.getAttribute(ShmConstants.NAME)).thenReturn("VppJob");
        when(persistenceObject2.getAttribute(ShmConstants.NAME)).thenReturn("secondVppJob");
        when(persistenceObject2.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(jobConfigurationDetails);
        when(persistenceObject2.getAttribute(ShmConstants.JOBTEMPLATEID)).thenReturn(TEMPLATE_ID + 1);
        persistentObjectList = new ArrayList<Object>(Arrays.asList(persistenceObject, persistenceObject2));

        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(queryBuilderMock.createTypeQuery(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE)).thenReturn(upPkgQueryMock);
        when(upPkgQueryMock.getRestrictionBuilder()).thenReturn(typeRestrictionBuilderMock);
        when(typeRestrictionBuilderMock.equalTo(eq(UpgradeActivityConstants.UP_PO_PACKAGE_NAME), anyString())).thenReturn(restrictionMock);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(queryExecutorMock);
        when(queryExecutorMock.execute(upPkgQueryMock)).thenReturn(persistentObjectList.iterator());
        when(queryBuilderMock.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(jobQueryMock);
        when(jobQueryMock.getRestrictionBuilder()).thenReturn(typeRestrictionBuilderMock);
        when(typeRestrictionBuilderMock.in(ShmConstants.STATE, JobStateEnum.getInactiveJobstates())).thenReturn(restrictionMock);
        when(typeRestrictionBuilderMock.not(restrictionMock)).thenReturn(restrictionMock);
        when(queryExecutorMock.execute(jobQueryMock)).thenReturn(persistentObjectList.iterator());
        deleteSoftwarePackagePersistenceProvider.isSoftwarePackageInUse(SOFTWARE_PACKAGE_NAME);
        verify(dataPersistenceServiceMock).getQueryBuilder();

    }

    @Test
    public void testDeleteSwPkgs_WithoutJobConfigDetails() throws ResourceException, IOException {
        persistentObjectList = new ArrayList<Object>(Arrays.asList(persistenceObject, persistenceObject2));
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(queryBuilderMock.createTypeQuery(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE)).thenReturn(upPkgQueryMock);
        when(upPkgQueryMock.getRestrictionBuilder()).thenReturn(typeRestrictionBuilderMock);
        when(typeRestrictionBuilderMock.equalTo(eq(UpgradeActivityConstants.UP_PO_PACKAGE_NAME), anyString())).thenReturn(restrictionMock);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(queryExecutorMock);
        when(queryExecutorMock.execute(upPkgQueryMock)).thenReturn(persistentObjectList.iterator());
        when(queryBuilderMock.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(jobQueryMock);
        when(jobQueryMock.getRestrictionBuilder()).thenReturn(typeRestrictionBuilderMock);
        when(typeRestrictionBuilderMock.in(ShmConstants.STATE, JobStateEnum.getInactiveJobstates())).thenReturn(restrictionMock);
        when(typeRestrictionBuilderMock.not(restrictionMock)).thenReturn(restrictionMock);
        when(queryExecutorMock.execute(jobQueryMock)).thenReturn(persistentObjectList.iterator());
        deleteSoftwarePackagePersistenceProvider.isSoftwarePackageInUse(SOFTWARE_PACKAGE_NAME);
    }
}
