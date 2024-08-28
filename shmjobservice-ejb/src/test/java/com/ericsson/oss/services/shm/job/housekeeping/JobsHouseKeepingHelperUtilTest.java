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
package com.ericsson.oss.services.shm.job.housekeeping;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class JobsHouseKeepingHelperUtilTest {

    @InjectMocks
    JobsHouseKeepingHelperUtil objectUnderTest;

    @Mock
    DataBucket dataBucket;

    @Mock
    DataPersistenceService dataPersistenceService;

    @Mock
    HouseKeepingConfigParamChangeListener houseKeepingConfigParamChangeListener;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    TypeRestrictionBuilder typeRestrictionBuilder1;

    @Mock
    Restriction restriction;

    @Mock
    Restriction restriction1;

    @Mock
    QueryExecutor queryExecutor;

    @Mock
    JobParameterChangeListener jobParameterChangeListener;

    @Mock
    DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    final List<Object[]> datbaseEntries = new ArrayList<Object[]>();

    @SuppressWarnings("deprecation")
    @Test
    public void test_FetchJobTypeSpecificPoIdsByCount() {
        final List<Object> poIds = new ArrayList<Object>();
        poIds.add(12345L);
        final long poId1 = 12345L;
        final long poId2 = 23456L;
        final Date endTime1 = new Date("Sat Oct 15 13:46:10 2016");
        final Date endTime2 = null;
        final Object[] element1 = { poId1, endTime1 };
        final Object[] element2 = { poId2, endTime2 };
        datbaseEntries.add(element1);
        datbaseEntries.add(element2);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE)).thenReturn(typeQuery);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.JOB_TYPE, "BACKUP")).thenReturn(restriction);
        when(houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP)).thenReturn(2);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(poIds);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(datbaseEntries);
        final List<Object[]> fetchedPoIds = objectUnderTest.fetchJobTypeSpecificPoIdsByCount("BACKUP");
        Assert.assertNotNull(fetchedPoIds);
        Assert.assertEquals(2, fetchedPoIds.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_FetchJobTypeSpecificPoIdsByCount_EmptyResult() {
        final List<Object> poIds = new ArrayList<Object>();
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE)).thenReturn(typeQuery);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.JOB_TYPE, "BACKUP")).thenReturn(restriction);
        when(houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP)).thenReturn(2);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(poIds);
        final List<Object[]> fetchedPoIds = objectUnderTest.fetchJobTypeSpecificPoIdsByCount("BACKUP");
        Assert.assertNotNull(fetchedPoIds);
        Assert.assertEquals(0, fetchedPoIds.size());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test(expected = ServerInternalException.class)
    public void test_FetchJobTypeSpecificPoIdsByCount_ThrowsException() {
        final List<Object> poIds = new ArrayList<Object>();
        poIds.add(12345L);
        final long poId1 = 12345L;
        final long poId2 = 23456L;
        final Date endTime1 = new Date("Sat Oct 15 13:46:10 2016");
        final Date endTime2 = null;
        final Object[] element1 = { poId1, endTime1 };
        final Object[] element2 = { poId2, endTime2 };
        datbaseEntries.add(element1);
        datbaseEntries.add(element2);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE)).thenReturn(typeQuery);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.JOB_TYPE, "BACKUP")).thenReturn(restriction);
        when(houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP)).thenReturn(2);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(poIds);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenThrow(RuntimeException.class);
        final List<Object[]> fetchedPoIds = objectUnderTest.fetchJobTypeSpecificPoIdsByCount("BACKUP");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_FetchJobTypeSpecificPoIdsByAge() {
        final List<Object> poIds = new ArrayList<Object>();
        poIds.add(12345L);
        final long poId1 = 12345L;
        final long poId2 = 23456L;
        final Date endTime1 = new Date("Sat Oct 15 13:46:10 2016");
        final Date endTime2 = null;
        final Object[] element1 = { poId1, endTime1 };
        final Object[] element2 = { poId2, endTime2 };
        datbaseEntries.add(element1);
        datbaseEntries.add(element2);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE)).thenReturn(typeQuery);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.anyString())).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP)).thenReturn(2);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(poIds);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(datbaseEntries);
        final List<Long> fetchedPoIds = objectUnderTest.fetchJobTypeSpecificPoIdsByAge("BACKUP", 2);
        Assert.assertNotNull(fetchedPoIds);
        Assert.assertEquals(1, fetchedPoIds.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_FetchJobTypeSpecificPoIdsByAge_EmptyResult() {
        final List<Object> poIds = new ArrayList<Object>();
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE)).thenReturn(typeQuery);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.anyString())).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP)).thenReturn(2);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(poIds);
        final List<Long> fetchedPoIds = objectUnderTest.fetchJobTypeSpecificPoIdsByAge("BACKUP", 2);
        Assert.assertNotNull(fetchedPoIds);
        Assert.assertEquals(0, fetchedPoIds.size());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test(expected = ServerInternalException.class)
    public void test_FetchJobTypeSpecificPoIdsByAge_ThrowsException() {
        final List<Object> poIds = new ArrayList<Object>();
        poIds.add(12345L);
        final long poId1 = 12345L;
        final long poId2 = 23456L;
        final Date endTime1 = new Date("Sat Oct 15 13:46:10 2016");
        final Date endTime2 = null;
        final Object[] element1 = { poId1, endTime1 };
        final Object[] element2 = { poId2, endTime2 };
        datbaseEntries.add(element1);
        datbaseEntries.add(element2);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE)).thenReturn(typeQuery);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.anyString())).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP)).thenReturn(2);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenThrow(RuntimeException.class);
        final List<Long> fetchedPoIds = objectUnderTest.fetchJobTypeSpecificPoIdsByAge("BACKUP", 2);
    }

}
