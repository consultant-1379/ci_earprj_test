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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;

@RunWith(MockitoJUnitRunner.class)
public class JobRestoreHandlingServiceUtilTest {

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
    JobRestoreHandlingServiceUtil jobRestoreHandlingServiceUtil;

    @Mock
    PersistenceObject persistenceObject;

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
    JobQueryService jobQueryService;

    @Mock
    MainJob mainJob;

    @Mock
    NEJob neJob;

    @Test
    public void testGetSuspendedJobDetailsJobStateCompleted() {

        buildPersistenceObject();

        when(workFlowQueryServiceImpl.getSuspendedBatchWorkflows()).thenReturn(Arrays.asList(workflowObjectMock));
        when(dataPersistenceServiceMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(queryBuilderMock.createTypeQuery(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE))).thenReturn(dpsQueryMock);
        when(dpsQueryMock.getRestrictionBuilder()).thenReturn(typeContainmentRestrictionBuilderMock);
        when(dataPersistenceServiceMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(liveBucketMock.getQueryExecutor()).thenReturn(dpsQueryExecutorMock);
        when(dpsQueryExecutorMock.execute(any(Query.class))).thenReturn(poIterator);
        when(workflowObjectMock.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID)).thenReturn("wfsid");
        when(dpsReader.findPOs(anyString(), anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(Arrays.asList(persistenceObject));
        when(poIterator.hasNext()).thenReturn(true, false);
        when(poIterator.next()).thenReturn(persistenceObject);
        when(persistenceObject.getPoId()).thenReturn(1234L);
        when(jobQueryService.retrieveMainJobAttributes(Arrays.asList(workflowObjectMock))).thenReturn(Arrays.asList(mainJob));
        jobRestoreHandlingServiceUtil.getSuspendedJobDetails();

    }

    @Test
    public void testGetSuspendedNEJobDetailsJobStateActive() {

        buildPersistenceObject();

        when(workFlowQueryServiceImpl.getSuspendedNEWorkflows()).thenReturn(Arrays.asList(workflowObjectMock));

        when(workflowObjectMock.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID)).thenReturn("WfsId");

        when(persistenceObjectList.get(0)).thenReturn(persistenceObject);

        when(dpsReader.findPOs(anyString(), anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(Arrays.asList(persistenceObject));

        when(jobQueryService.retrieveNEJobAttributes(any(long.class), anyListOf(String.class))).thenReturn(Arrays.asList(neJob));

        jobRestoreHandlingServiceUtil.getSuspendedNEJobDetails(12345L);

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
        persistenceObjectAttributes.put(ShmConstants.WFS_ID, "WfsId");
        when(persistenceObject.getAllAttributes()).thenReturn(persistenceObjectAttributes);
    }

    @Test
    public void testUpdateJobsInBatch() {
        final Map<Long, Map<String, Object>> batchJobsToBeUpdated = new HashMap<Long, Map<String, Object>>();
        final Long mainJobId = 123L;
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLog = new HashMap<String, Object>();
        jobLog.put(ShmConstants.MESSAGE, "Some Message");
        jobLog.put(ShmConstants.ENTRY_TIME, new Date());
        jobLog.put(ShmConstants.TYPE, JobLogType.SYSTEM.toString());
        jobLogList.add(jobLog);
        attributeMap.put(ShmConstants.LOG, jobLogList);
        batchJobsToBeUpdated.put(mainJobId, attributeMap);

        final List<Long> mainJobIdList = new ArrayList<Long>();
        mainJobIdList.add(mainJobId);
        final List<PersistenceObject> poList = new ArrayList<PersistenceObject>();
        poList.add(persistenceObject);
        final Map<String, Object> poAttributeMap = new HashMap<String, Object>();
        final List<Map<String, Object>> poJobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poJobLog = new HashMap<String, Object>();
        poJobLog.put(ShmConstants.MESSAGE, "First Message");
        poJobLog.put(ShmConstants.ENTRY_TIME, new Date());
        poJobLog.put(ShmConstants.TYPE, JobLogType.SYSTEM.toString());
        poJobLogList.add(poJobLog);
        poAttributeMap.put(ShmConstants.LOG, poJobLogList);
        when(persistenceObject.getPoId()).thenReturn(mainJobId);
        when(persistenceObject.getAllAttributes()).thenReturn(poAttributeMap);

        jobRestoreHandlingServiceUtil.updateJobsInBatch(batchJobsToBeUpdated);

        verify(dpsReader).findPOsByPoIds(mainJobIdList);

    }
}
