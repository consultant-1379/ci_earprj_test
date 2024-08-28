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
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class MainJobProgressUpdaterTest {

    @InjectMocks
    private MainJobProgressUpdater objectUnderTest;

    @Mock
    private MainJobProgressCalculator progressCalculatorMock;

    @Mock
    private DataPersistenceService dpsMock;

    @Mock
    private PersistenceObject poMock;

    long mainJobId = 123l;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private Date dateMock;
    @Mock 
    private Map<String, Object> mainJobAttributes1;
    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private DataBucket dataBucketMock;
    @Mock
    private QueryBuilder queryBuilderMock;
    @Mock
    private QueryExecutor queryExecutorMock;
    @Mock
    private Query<TypeRestrictionBuilder> jobTypeQueryMock;
    @Mock
    private TypeRestrictionBuilder resrtictionBuilderMock;
    @Mock
    private Restriction jobTemplateRestrictionMock;
    @Mock
    private MainJobProgressDPSHelper mainJobProgressDPSHelper;

    @Test
    public void test_updateMainJobProgress_doesNotUpdate() {
        mockDps();
        when(dataBucketMock.findPoById(mainJobId)).thenReturn(poMock);
        when(poMock.getAttribute(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(poMock.getAttribute(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(50d);
        when(progressCalculatorMock.calculateMainJobProgressPercentage(mainJobId, 5,Arrays.asList(mapMock))).thenReturn(50d);
        Map<String, Object> mainJobAttributes=new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, 5);
        mainJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 50d);
        when(mainJobProgressDPSHelper.getMainJobAttributes(mainJobId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID)).thenReturn(mainJobAttributes);
        
        objectUnderTest.updateMainJobProgress(mainJobId);

        verify(workflowInstanceNotifier, never()).sendAllNeDone(anyString());
    }

    @Test
    public void test_updateMainJobProgress_updatesProgress() {
        mockDps();
        when(dataBucketMock.findPoById(mainJobId)).thenReturn(poMock);
        when(poMock.getAttribute(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(poMock.getAttribute(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(20d);
        when(progressCalculatorMock.calculateMainJobProgressPercentage(mainJobId, 5,Arrays.asList(mapMock))).thenReturn(50d);

        when(poMock.getAttribute(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("2");

        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("1");

        Map<String, Object> mainJobAttributes=new HashMap<String, Object>();
        when(mainJobAttributes1.get(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(mainJobAttributes1.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(20d);
        when(mainJobAttributes1.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mainJobProgressDPSHelper.getMainJobAttributes(mainJobId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID)).thenReturn(mainJobAttributes1);
        objectUnderTest.updateMainJobProgress(mainJobId);

       // verify(poMock, never()).setAttribute(eq(ShmConstants.JOBPROPERTIES), anyList());
        verify(workflowInstanceNotifier, never()).sendAllNeDone(anyString());
    }

    @Test
    public void test_updateMainJobProgress_updatesProgressAndProperties() {
        mockDps();
        when(dataBucketMock.findPoById(mainJobId)).thenReturn(poMock);
        when(poMock.getAttribute(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(poMock.getAttribute(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(20d);
        when(progressCalculatorMock.calculateMainJobProgressPercentage(mainJobId, 5,Arrays.asList(mapMock))).thenReturn(50d);
        when(queryExecutorMock.getResultList(jobTypeQueryMock)).thenReturn(Arrays.asList(new Object(), new Object()));

        when(poMock.getAttribute(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("2");

        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("1");
        
        when(mainJobAttributes1.get(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(mainJobAttributes1.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(20d);
        when(mainJobAttributes1.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mainJobProgressDPSHelper.getMainJobAttributes(mainJobId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID)).thenReturn(mainJobAttributes1);
        objectUnderTest.updateMainJobProgress(mainJobId);

        verify(workflowInstanceNotifier, never()).sendAllNeDone(anyString());
    }

    @Test
    public void test_updateMainJobProgress_updatesProgressAndProperties_notifiesWFS() {
        mockDps();
        when(dataBucketMock.findPoById(mainJobId)).thenReturn(poMock);
        when(poMock.getAttribute(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(poMock.getAttribute(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(20d);
        when(poMock.getAttribute(ShmConstants.JOB_TEMPLATE_ID)).thenReturn(20l);

        when(progressCalculatorMock.calculateMainJobProgressPercentage(mainJobId, 5,Arrays.asList(mapMock))).thenReturn(50d);
        when(queryExecutorMock.getResultList(jobTypeQueryMock)).thenReturn(Arrays.asList(new Object(), new Object()));

        when(poMock.getAttribute(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock, mapMock));
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES, ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("2", "1");
        
        when(mainJobAttributes1.get(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(mainJobAttributes1.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(20d);
        when(mainJobAttributes1.get(ShmConstants.JOB_TEMPLATE_ID)).thenReturn(20l);
        when(mainJobAttributes1.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mainJobProgressDPSHelper.getMainJobAttributes(mainJobId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID)).thenReturn(mainJobAttributes1);
        objectUnderTest.updateMainJobProgress(mainJobId);

        verify(workflowInstanceNotifier, times(1)).sendAllNeDone("20");
    }

    @Test
    public void test_updateMainJobEnd() {
        mockDps();
        when(dataBucketMock.findPoById(mainJobId)).thenReturn(poMock);
        when(poMock.getAttribute(ShmConstants.NO_OF_NETWORK_ELEMENTS)).thenReturn(5);
        when(poMock.getAttribute(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(25.00);
        List<Map<String, Object>> neJobsProgress=new ArrayList();

        Map<String, Object> mainJobAttributes=new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, 5);
        mainJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 25.00);
        when(mainJobProgressDPSHelper.getMainJobAttributes(mainJobId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID)).thenReturn(mainJobAttributes);
        when(progressCalculatorMock.calculateMainJobProgressPercentage(mainJobId, 5,neJobsProgress)).thenReturn(50d);
        objectUnderTest.updateMainJobEndDetails(mainJobId, new HashMap<String, Object>());

        final Map<String, Object> mainJobAttributes1 = new HashMap<String, Object>();
        mainJobAttributes1.put(ShmConstants.PROGRESSPERCENTAGE, 50.0);
        verify(mainJobProgressDPSHelper, times(1)).updateMainJobAttributes(mainJobId, mainJobAttributes1);
    }

    private void mockDps() {
        when(dpsMock.getLiveBucket()).thenReturn(dataBucketMock);
        when(dpsMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(dataBucketMock.getQueryExecutor()).thenReturn(queryExecutorMock);
        when(queryBuilderMock.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.NE_JOB)).thenReturn(jobTypeQueryMock);
        when(jobTypeQueryMock.getRestrictionBuilder()).thenReturn(resrtictionBuilderMock);
        when(resrtictionBuilderMock.equalTo(eq(ShmConstants.MAINJOBID), any(Object.class))).thenReturn(jobTemplateRestrictionMock);
        when(resrtictionBuilderMock.in(ShmJobConstants.STATE, (Object[]) JobStateEnum.getInactiveJobstates())).thenReturn(jobTemplateRestrictionMock);

    }
}
