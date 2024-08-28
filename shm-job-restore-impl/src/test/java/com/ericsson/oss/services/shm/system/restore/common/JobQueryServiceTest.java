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
package com.ericsson.oss.services.shm.system.restore.common;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;

@RunWith(MockitoJUnitRunner.class)
public class JobQueryServiceTest {

    private static final String JOB_NAME = "jobName";
    private static final int EXECUTION_INDEX = 1;
    private static final String JOB_STATE = "RUNNING";
    private static final String SOME_LOG = "someLogMessage";
    private static final String ADDITIONAL_DATA = "additionalData";

    @InjectMocks
    JobQueryService objectUnderTest;

    @Mock
    Query<TypeRestrictionBuilder> jobTypeQuery;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    DataBucket dataBucket;

    @Mock
    QueryExecutor queryExecutor;

    @Mock
    private Iterator<Object> iteratorMock;

    @Mock
    PersistenceObject persistenceObject;

    @Mock
    TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    JobLogUtil jobLogUtil;
    @Mock
    SystemRecorder systemRecorder;

    @Mock
    Query<TypeRestrictionBuilder> dpsQueryMock;

    @Mock
    QueryBuilder queryBuilderMock;

    @Mock
    List<WorkflowObject> workFlowObjectsMockList;

    @Mock
    WorkflowObject workflowObjectMock;

    @Mock
    List<PersistenceObject> persistenceObjectList;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    DpsReader dpsReader;

    @InjectMocks
    JobQueryService jobQueryService;

    @Mock
    DataPersistenceService dataPersistenceServiceMock;

    @Mock
    Iterator<Object> poIterator;

    @Mock
    TypeContainmentRestrictionBuilder typeContainmentRestrictionBuilderMock;

    @Mock
    QueryExecutor dpsQueryExecutorMock;

    @Mock
    DataBucket liveBucketMock;

    @Mock
    DpsWriter dpsWriter;

    @Mock
    WorkFlowQueryServiceImpl workFlowQueryServiceImpl;

    @Mock
    MainJob mainJob;

    @Mock
    NEJob neJob;

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveMainJob() {

        buildPersistenceObject();
        final List<String> wfsIdList = new ArrayList<String>();
        wfsIdList.add("wfsId1");
        wfsIdList.add("wfsId2");
        wfsIdList.add("wfsId3");

        when(workFlowQueryServiceImpl.getWorkFlowInstanceIdList(anyList())).thenReturn(wfsIdList);
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(queryBuilderMock.createTypeQuery(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE))).thenReturn(dpsQueryMock);
        when(dpsQueryMock.getRestrictionBuilder()).thenReturn(typeContainmentRestrictionBuilderMock);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(dpsQueryExecutorMock);
        when(dpsQueryExecutorMock.execute(any(Query.class))).thenReturn(poIterator);
        when(workflowObjectMock.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID)).thenReturn("wfsid");
        //        when(ListUtils.partition(wfsIdList, 20)).thenReturn(totalLists);
        when(dpsReader.findPOs(anyString(), anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(Arrays.asList(persistenceObject));
        when(poIterator.hasNext()).thenReturn(true, false);
        when(poIterator.next()).thenReturn(persistenceObject);
        when(persistenceObject.getPoId()).thenReturn(1234L);
        jobQueryService.retrieveMainJobAttributes(Arrays.asList(workflowObjectMock));

    }

    @Test
    public void testGetSuspendedNEJobDetailsJobStateActive() {

        buildPersistenceObject();

        final List<String> wfsIdList = new ArrayList<String>();
        wfsIdList.add("wfsId1");
        wfsIdList.add("wfsId2");
        wfsIdList.add("wfsId3");

        when(workFlowQueryServiceImpl.getSuspendedNEWorkflows()).thenReturn(Arrays.asList(workflowObjectMock));

        when(workflowObjectMock.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID)).thenReturn("WfsId");

        when(persistenceObjectList.get(0)).thenReturn(persistenceObject);

        when(dpsReader.findPOs(anyString(), anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(Arrays.asList(persistenceObject));

        jobQueryService.retrieveNEJobAttributes(12345L, wfsIdList);

    }

    @Test
    public void testGetMainJobsInActiveState() {
        buildPersistenceObject();
        final List<Long> poIdList = new ArrayList<Long>();
        poIdList.add((Long) persistenceObject.getAllAttributes().get(ShmConstants.JOBTEMPLATEID));
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.findPosByIds(poIdList)).thenReturn(persistenceObjectList);
        when(persistenceObjectList.get(0)).thenReturn(persistenceObject);
        when(queryBuilderMock.createTypeQuery(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOB))).thenReturn(dpsQueryMock);
        when(dpsQueryMock.getRestrictionBuilder()).thenReturn(typeContainmentRestrictionBuilderMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(dpsQueryExecutorMock);
        when(dpsQueryExecutorMock.execute(any(Query.class))).thenReturn(poIterator);
        when(poIterator.hasNext()).thenReturn(true, false);
        when(poIterator.next()).thenReturn(persistenceObject);
        when(persistenceObject.getPoId()).thenReturn(1234L);

        jobQueryService.getMainJobsInActiveState();

    }

    @Test
    public void testGetNEJobsInActiveState() {
        buildPersistenceObject();
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(queryBuilderMock.createTypeQuery(eq(ShmConstants.NAMESPACE), eq(ShmConstants.NE_JOB))).thenReturn(dpsQueryMock);
        when(dpsQueryMock.getRestrictionBuilder()).thenReturn(typeContainmentRestrictionBuilderMock);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(dpsQueryExecutorMock);
        when(dpsQueryExecutorMock.execute(any(com.ericsson.oss.itpf.datalayer.dps.query.Query.class))).thenReturn(poIterator);
        when(poIterator.hasNext()).thenReturn(true, false);
        when(poIterator.next()).thenReturn(persistenceObject);
        when(persistenceObject.getPoId()).thenReturn(1234L);

        jobQueryService.getNEJobsInActiveState(1234L);

    }

    /**
     * 
     */
    private void buildPersistenceObject() {
        final Map<String, Object> persistenceObjectAttributes = new HashMap<String, Object>();
        persistenceObjectAttributes.put("name", "name");
        persistenceObjectAttributes.put("jobType", "RESTORE");
        persistenceObjectAttributes.put("state", "RUNNING");
        persistenceObjectAttributes.put("executionIndex", 1);
        persistenceObjectAttributes.put("neName", "ERBS_TEST");
        persistenceObjectAttributes.put("mainJobId", 1234L);
        persistenceObjectAttributes.put(ShmConstants.JOBTEMPLATEID, 1234L);
        persistenceObjectAttributes.put(ShmConstants.WFS_ID, "WfsId");
        when(persistenceObject.getAllAttributes()).thenReturn(persistenceObjectAttributes);
    }

    @Test
    public void testCancelActivitiesAndUpdateState() {

        final List<Long> neJobIds = new ArrayList<Long>();
        neJobIds.add(123l);

        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB)).thenReturn(jobTypeQuery);
        when(jobTypeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);

        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(dataBucket);

        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(any(Query.class))).thenReturn(iteratorMock);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        //Mockito.doNothing().when(activityUtils).recordEvent(SHMEvents.CREATE_BACKUP_PRECHECK, neName, cvMoFdn, "SHM:" + activityJobId + ":" + neName);
        Mockito.doNothing().when(jobLogUtil).prepareJobLogAtrributesList(jobLogList, SOME_LOG, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        Mockito.doNothing().when(systemRecorder).recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, JOB_NAME, JOB_NAME, ADDITIONAL_DATA);
        when(iteratorMock.hasNext()).thenReturn(true, false);
        when(iteratorMock.next()).thenReturn(persistenceObject);
        when(persistenceObject.getAllAttributes()).thenReturn(getPOAttributesMock());

        objectUnderTest.cancelActivitiesAndUpdateState(neJobIds, JOB_NAME, EXECUTION_INDEX);
    }

    public Map<String, Object> getPOAttributesMock() {

        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(ShmConstants.NAME, JOB_NAME);
        poAttributes.put(ShmConstants.STATE, JOB_STATE);
        return poAttributes;
    }
}
