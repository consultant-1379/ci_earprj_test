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
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;

/**
 * 
 * @author xarirud
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class ActivityStepDurationsReportGeneratorImplTest {

    @InjectMocks
    private ActivityStepDurationsReportGeneratorImpl objectUnderTest;

    @Mock
    private DataPersistenceService dpsMock;

    @Mock
    private DataBucket dataBucketMock;

    @Mock
    private QueryBuilder queryBuilderMock;

    @Mock
    private Query<TypeRestrictionBuilder> typeRestrictionQuery;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private Restriction restriction;

    @Mock
    private QueryExecutor queryExecutorMock;

    @Mock
    private Iterator<Object> jobIteratorMock;

    @Mock
    private MainJobStepDurationsCalculator mainJobStepDurationsCalculatorMock;

    @Mock
    private MainJobProgressUpdaterRetryProxy mainJobProgressUpdaterRetryProxyMock;

    @Mock
    private PersistenceObject persistenceObject;

    final long mainJobPoId = 123L;
    final String jobName = "RANDOM_TEST_JOB_NAME";

    /**
     * Test method for {@link com.ericsson.oss.services.shm.es.impl.ActivityStepDurationsReportGeneratorImpl#triggerJobReportGenerationThroughPibScript(java.lang.String)}.
     */
    @Test
    public void testTriggerJobReportGenerationThroughPibScript() {
        final List<Object[]> objectArray = new ArrayList<Object[]>();
        final List<Object> list = new ArrayList<Object>();
        list.add(123456L);
        list.add(new Date());
        final Object[] projectionArray = list.toArray();
        objectArray.add(projectionArray);

        when(dpsMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(dpsMock.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.getQueryExecutor()).thenReturn(queryExecutorMock);

        when(queryBuilderMock.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeRestrictionQuery);
        when(typeRestrictionQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ObjectField.PO_ID, mainJobPoId)).thenReturn(restriction);
        when(queryExecutorMock.executeProjection(eq(typeRestrictionQuery), any(Projection.class), any(Projection.class))).thenReturn(objectArray);
        when(queryExecutorMock.execute(any(Query.class))).thenReturn(jobIteratorMock);

        objectUnderTest.triggerJobReportGenerationThroughPibScript(jobName);
    }

    /**
     * Test method for {@link com.ericsson.oss.services.shm.es.impl.ActivityStepDurationsReportGeneratorImpl#triggerJobReportGenerationByMainJobId(long)}.
     */
    @Test
    public void testGenerateJobReportAndUpdateMainJob() {
        when(mainJobStepDurationsCalculatorMock.calculateMetrics(eq(mainJobPoId), Matchers.anyMap())).thenReturn(true);
        when(dpsMock.getLiveBucket()).thenReturn(dataBucketMock);
        when(dataBucketMock.findPoById(mainJobPoId)).thenReturn(persistenceObject);
        objectUnderTest.generateJobReportAndUpdateMainJob(mainJobPoId);
    }

}
