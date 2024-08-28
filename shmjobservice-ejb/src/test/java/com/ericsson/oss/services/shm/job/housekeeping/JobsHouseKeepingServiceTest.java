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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.cluster.counter.NamedCounter;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener;

@RunWith(MockitoJUnitRunner.class)
public class JobsHouseKeepingServiceTest {

    @InjectMocks
    JobsHouseKeepingService objectUnderTest;

    @Mock
    DataBucket dataBucket;

    @Mock
    DataPersistenceService dataPersistenceService;

    @Mock
    JobsHouseKeepingClusterCounterManager jobsHouseKeepingClusterCounterManager;

    @Mock
    JobsHouseKeepingDelegator jobsHouseKeepingDelegator;

    @Mock
    HouseKeepingConfigParamChangeListener houseKeepingConfigParamChangeListener;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    Query<TypeRestrictionBuilder> typeQuery1;

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
    NamedCounter namedCounter;

    @Mock
    List<Object[]> mainJobIds;

    @Mock
    Future<JobsHouseKeepingResponse> backupJobsHouseKeepingResponseMock;

    @Mock
    JobParameterChangeListener jobParameterChangeListener;

    @Mock
    JobsHouseKeepingHelperUtil jobsHouseKeepingHelperUtil;

    List<Object> po_IDs = new ArrayList<Object>();
    final List<Object[]> datbaseEntries = new ArrayList<Object[]>();

    protected static String jobTemplatePoId = "jobTemplatePoId";
    protected static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    protected static String activityJobPoId = "activityJobPoId";
    protected String state;
    protected static Map<String, Long> poIds = new HashMap<String, Long>();
    protected static Map<String, Long> poIdsWithoutActivity = new HashMap<String, Long>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        createPOIds();
    }

    private void createPOIds() {
        po_IDs.add(12345L);
        po_IDs.add(23456L);
        po_IDs.add(34567L);
        po_IDs.add(45678L);
        po_IDs.add(56789L);

    }

    @Test
    public void testTriggerHouseKeepingWhenNoJobs() throws InterruptedException, ExecutionException {
        final List<Object> emptypoIds = new ArrayList<Object>();
        JobsHouseKeepingResponse jobsHouseKeepingResponse = new JobsHouseKeepingResponse();
        jobsHouseKeepingResponse.setSuccessfullyDeletedJobsCount(1);
        final Future<JobsHouseKeepingResponse> backupJobsHouseKeepingResponse = new AsyncResult<JobsHouseKeepingResponse>(jobsHouseKeepingResponse);
        final long clusterCount = 0;
        when(jobsHouseKeepingHelperUtil.fetchJobTypeSpecificPoIdsByCount(JobType.BACKUP.name())).thenReturn(datbaseEntries);
        objectUnderTest.triggerHouseKeepingOfJobs();
        verify(jobsHouseKeepingDelegator, times(0)).houseKeepingOfJobs(Matchers.anyList(), Matchers.anyString());
    }

    @Test
    public void testHouseKeepingAllScheduledJobs() throws InterruptedException, ExecutionException {
        final List<Object> poIds = new ArrayList<Object>();
        poIds.add(12345L);
        final long poId1 = 12345L;
        final String endTime = null;
        final Object[] element = { poId1, endTime, };
        datbaseEntries.add(element);
        JobsHouseKeepingResponse jobsHouseKeepingResponse = new JobsHouseKeepingResponse();
        jobsHouseKeepingResponse.setSuccessfullyDeletedJobsCount(1);
        List<List<Long>> templateIds = new ArrayList<List<Long>>();
        List<Long> tempIds = new ArrayList<Long>();
        tempIds.add(1234L);
        templateIds.add(tempIds);
        objectUnderTest.triggerHouseKeepingOfJobs();
        verify(jobsHouseKeepingDelegator, times(0)).houseKeepingOfJobs(Matchers.anyList(), Matchers.anyString());
    }

    @Test
    public void testHouseKeepingSomeScheduledJobs() throws InterruptedException, ExecutionException {
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
        JobsHouseKeepingResponse jobsHouseKeepingResponse = new JobsHouseKeepingResponse();
        jobsHouseKeepingResponse.setSuccessfullyDeletedJobsCount(1);
        when(jobsHouseKeepingHelperUtil.fetchJobTypeSpecificPoIdsByCount(JobType.BACKUP.name())).thenReturn(datbaseEntries);
        objectUnderTest.triggerHouseKeepingOfJobs();
        verify(jobsHouseKeepingDelegator, times(1)).houseKeepingOfJobs(Matchers.anyList(), Matchers.anyString());
    }

    @Test
    public void testTriggerHouseKeepingOfBackUpJobs() throws InterruptedException, ExecutionException {
        final List<Object> poIds = new ArrayList<Object>();
        poIds.add(12345L);
        final long poId = 12345L;
        final Date endTime = new Date("Sat Oct 15 13:46:10 2016");
        final Object[] element = { poId, endTime };
        datbaseEntries.add(element);
        JobsHouseKeepingResponse jobsHouseKeepingResponse = new JobsHouseKeepingResponse();
        jobsHouseKeepingResponse.setSuccessfullyDeletedJobsCount(1);
        final Future<JobsHouseKeepingResponse> backupJobsHouseKeepingResponse = new AsyncResult<JobsHouseKeepingResponse>(jobsHouseKeepingResponse);
        final long clusterCount = 0;
        when(jobsHouseKeepingHelperUtil.fetchJobTypeSpecificPoIdsByCount(JobType.BACKUP.name())).thenReturn(datbaseEntries);
        objectUnderTest.triggerHouseKeepingOfJobs();
        verify(jobsHouseKeepingDelegator, times(1)).houseKeepingOfJobs(Matchers.anyList(), Matchers.anyString());
    }

}
