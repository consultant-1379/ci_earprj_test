/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.service.shm.loadcontrol.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlCounterManager;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;

@RunWith(MockitoJUnitRunner.class)
public class LoadControllerPersistenceManagerTest {

    @InjectMocks
    LoadControllerPersistenceManager loadControllerPersistenceManager;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Mock
    private LoadControlCounterManager counterManager;

    @Mock
    private PersistenceObject persistenceObject;

    @Mock
    private DataPersistenceService dataPersistenceServiceMock;

    @Mock
    private DataBucket liveBucketMock;

    @Mock
    private PersistenceObjectBuilder poBuilder;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    private Restriction restriction;

    @Mock
    private DpsWriter dpsWriter;

    @Mock
    private RetryManager retryManager;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    private DpsRetryPolicies dpsRetryPoliciesMock;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    private Iterator<Object> shmStagedActivityPOs;

    private static final String WORKFLOW_INSTANCE_ID = "123456789";
    private static final String BUSINESS_KEY = "1234567@LTE02ERBS0001";
    private static final String JOBTYPE = "BACKUP";
    private static final String PLATFORMT_TYPE = "CPP";
    private static final String ACTIVITY_NAME = "exprtcv";
    private static final String COUNTER_KEY = "LC_CPPBACKUPexprtcv";
    private static final long ACTIVITY_JOB_ID = 123l;

    @Test
    public void testKeepRequestInDB() {
        final SHMActivityRequest activityRequest = new SHMActivityRequest(WORKFLOW_INSTANCE_ID, BUSINESS_KEY, JOBTYPE);
        activityRequest.setPlatformType(PLATFORMT_TYPE);
        activityRequest.setActivityName(ACTIVITY_NAME);
        Mockito.when(counterManager.getCounterName(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(COUNTER_KEY);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getPersistenceObjectBuilder()).thenReturn(poBuilder);
        when(poBuilder.namespace(ShmConstants.NAMESPACE)).thenReturn(poBuilder);
        when(poBuilder.type(ShmConstants.SHM_STAGED_ACTIVITY)).thenReturn(poBuilder);
        when(poBuilder.version(ShmConstants.VERSION)).thenReturn(poBuilder);
        when(poBuilder.addAttributes(Matchers.anyMap())).thenReturn(poBuilder);
        when(poBuilder.create()).thenReturn(persistenceObject);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(123456l);
        loadControllerPersistenceManager.keepRequestInDB(activityRequest);
        verify(retryManager, times(1)).executeCommand(any(RetryPolicy.class), any(RetriableCommand.class));
    }

    @Test
    public void testKeepRequestInDBWhenExceptionOccured() {
        final SHMActivityRequest activityRequest = new SHMActivityRequest(WORKFLOW_INSTANCE_ID, BUSINESS_KEY, JOBTYPE);
        activityRequest.setPlatformType(PLATFORMT_TYPE);
        activityRequest.setActivityName(ACTIVITY_NAME);
        Mockito.when(counterManager.getCounterName(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(COUNTER_KEY);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getPersistenceObjectBuilder()).thenReturn(poBuilder);
        when(poBuilder.namespace(ShmConstants.NAMESPACE)).thenReturn(poBuilder);
        when(poBuilder.type(ShmConstants.SHM_STAGED_ACTIVITY)).thenReturn(poBuilder);
        when(poBuilder.version(ShmConstants.VERSION)).thenReturn(poBuilder);
        when(poBuilder.addAttributes(Matchers.anyMap())).thenReturn(poBuilder);
        when(poBuilder.create()).thenReturn(persistenceObject);
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        loadControllerPersistenceManager.keepRequestInDB(activityRequest);
        verify(dpsAvailabilityInfoProvider, times(1)).checkDatabaseAvailability(Matchers.any(RuntimeException.class));
    }

    @Test
    public void testReadAndProcessStagedActivityPOs() {
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.SHM_STAGED_ACTIVITY)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STAGED_ACTIVITY_STATUS, ShmConstants.READY_STATE)).thenReturn(restriction);
        loadControllerPersistenceManager.readAndProcessStagedActivityPOs();
    }

    @Test
    public void testDeleteStagedPOs() {
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.SHM_STAGED_ACTIVITY)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID)).thenReturn(restriction);
        when(queryExecutor.execute(typeQuery)).thenReturn(shmStagedActivityPOs);
        when(shmStagedActivityPOs.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(shmStagedActivityPOs.next()).thenReturn(persistenceObject).thenReturn(persistenceObject);
        loadControllerPersistenceManager.deleteStagedActivityPOs(ACTIVITY_JOB_ID);
        verify(typeQuery).setRestriction(restriction);
        verify(liveBucketMock, times(2)).deletePo(persistenceObject);
    }
}
