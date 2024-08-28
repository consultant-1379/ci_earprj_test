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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * Test class for {@link calculateActivityStepDurationMetricsForMainJob}
 * 
 * @author xarirud
 */
@RunWith(MockitoJUnitRunner.class)
public class MainJobStepDurationsCalculatorTest {
    @InjectMocks
    private MainJobStepDurationsCalculator objectUnderTest;

    @Mock
    private DataPersistenceService dpsMock;

    @Mock
    private PersistenceObject poMock;

    @Mock
    private DataBucket dataBucketMock;

    @Mock
    private QueryBuilder queryBuilderMock;

    @Mock
    private QueryExecutor queryExecutorMock;

    @Mock
    private Query<TypeRestrictionBuilder> jobTypeQueryMock;

    @Mock
    private TypeRestrictionBuilder restrictionBuilderMock;

    @Mock
    private Restriction jobTemplateRestrictionMock;

    final long mainJobId = 123L;

    /**
     * Test method for {@link com.ericsson.oss.services.shm.es.impl.MainJobStepDurationsCalculator#calculateMetrics(long, java.util.Map)}.
     */
    @Test
    public void testCalculateActivityStepDurationMetricsForMainJob() {
        final List<Object> neJobIds = new ArrayList<Object>();
        neJobIds.add(123l);
        neJobIds.add(852l);
        neJobIds.add(154l);

        final String delimiterEqual = "=";

        String precheckDuration = ActivityStepsEnum.PRECHECK + delimiterEqual + Double.toString(1.50);
        String executeDuration = ActivityStepsEnum.EXECUTE + delimiterEqual + Double.toString(1.70);
        String notificationDuration = ActivityStepsEnum.PROCESS_NOTIFICATION + delimiterEqual + Double.toString(2.00);
        String timeOutDuration = ActivityStepsEnum.HANDLE_TIMEOUT + delimiterEqual + Double.toString(3.50);

        final List<String> stepDurationsList = new ArrayList<String>();

        final Object[] array1 = new Object[2];
        array1[0] = "INSTALL";
        stepDurationsList.add(precheckDuration);
        stepDurationsList.add(executeDuration);
        stepDurationsList.add(notificationDuration);
        stepDurationsList.add(timeOutDuration);
        array1[1] = stepDurationsList.toString();
        final Object[] array2 = new Object[2];
        array2[0] = "UPGRADE";
        executeDuration = ActivityStepsEnum.EXECUTE + delimiterEqual + Double.toString(7.70);
        notificationDuration = ActivityStepsEnum.PROCESS_NOTIFICATION + delimiterEqual + Double.toString(9.00);
        timeOutDuration = ActivityStepsEnum.HANDLE_TIMEOUT + delimiterEqual + Double.toString(10.50);
        stepDurationsList.clear();
        stepDurationsList.add(executeDuration);
        stepDurationsList.add(notificationDuration);
        stepDurationsList.add(timeOutDuration);
        array2[1] = stepDurationsList.toString();
        final List<Object[]> activityAttributes1 = new ArrayList<Object[]>();
        activityAttributes1.add(array1);
        activityAttributes1.add(array2);

        final Object[] array3 = new Object[2];
        array3[0] = "RESTORE";
        stepDurationsList.clear();
        precheckDuration = ActivityStepsEnum.PRECHECK + delimiterEqual + Double.toString(10.90);
        executeDuration = ActivityStepsEnum.EXECUTE + delimiterEqual + Double.toString(17.70);
        timeOutDuration = ActivityStepsEnum.HANDLE_TIMEOUT + delimiterEqual + Double.toString(20.50);
        stepDurationsList.add(precheckDuration);
        stepDurationsList.add(executeDuration);
        stepDurationsList.add(timeOutDuration);
        array3[1] = stepDurationsList.toString();

        final Object[] array4 = new Object[2];
        array4[0] = "UPLOADCV";
        executeDuration = ActivityStepsEnum.EXECUTE + delimiterEqual + Double.toString(5.70);
        notificationDuration = ActivityStepsEnum.PROCESS_NOTIFICATION + delimiterEqual + Double.toString(14.00);
        timeOutDuration = ActivityStepsEnum.HANDLE_TIMEOUT + delimiterEqual + Double.toString(15.50);
        stepDurationsList.clear();
        stepDurationsList.add(executeDuration);
        stepDurationsList.add(notificationDuration);
        stepDurationsList.add(timeOutDuration);
        array4[1] = stepDurationsList.toString();

        final List<Object[]> activityAttributes2 = new ArrayList<Object[]>();
        activityAttributes2.add(array1);
        activityAttributes2.add(array2);

        mockDps();
        //when(dataBucketMock.findPoById(mainJobId)).thenReturn(poMock);
        when(queryBuilderMock.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.ACTIVITY_JOB)).thenReturn(jobTypeQueryMock);
        when(queryExecutorMock.executeProjection(eq(jobTypeQueryMock), any(Projection.class))).thenReturn(neJobIds);
        when(queryExecutorMock.executeProjection(eq(jobTypeQueryMock), any(Projection.class), any(Projection.class))).thenReturn(activityAttributes1, activityAttributes2);

        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.STEP_DURATIONS, mainJobAttributes);
        objectUnderTest.calculateMetrics(mainJobId, mainJobAttributes);
    }

    private void mockDps() {
        when(dpsMock.getLiveBucket()).thenReturn(dataBucketMock);
        when(dpsMock.getQueryBuilder()).thenReturn(queryBuilderMock);
        when(dataBucketMock.getQueryExecutor()).thenReturn(queryExecutorMock);
        when(queryBuilderMock.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.NE_JOB)).thenReturn(jobTypeQueryMock);
        when(jobTypeQueryMock.getRestrictionBuilder()).thenReturn(restrictionBuilderMock);
        when(restrictionBuilderMock.equalTo(eq(ShmConstants.MAINJOBID), any(Object.class))).thenReturn(jobTemplateRestrictionMock);
        when(restrictionBuilderMock.in(ShmJobConstants.STATE, (Object[]) JobStateEnum.getInactiveJobstates())).thenReturn(jobTemplateRestrictionMock);
    }

}
