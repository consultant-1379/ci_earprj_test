package com.ericsson.oss.services.shm.job.impl;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.services.shm.activity.timeout.models.NodeHealthCheckJobActivityTimeouts;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.activity.JobType;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@RunWith(MockitoJUnitRunner.class)
public class JobConfigurationServiceImplTest {

    @Mock
    @Inject
    private DpsWriter dpsWriterMock;

    @Mock
    @Inject
    private DpsReader dpsReaderMock;

    @Mock
    private PersistenceObject activityJobPo;

    @Mock
    private PersistenceObject neJobPo;

    @Mock
    private PersistenceObject mainJobPo;

    @Mock
    private PersistenceObject templateJobPo;

    @InjectMocks
    private JobConfigurationServiceImpl objectUnderTest;

    @Mock
    private List<PersistenceObject> persistenceObjectListMock;

    @Mock
    private PersistenceObject persistenceObjectMock;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Restriction restriction;

    @Mock
    private Query<TypeRestrictionBuilder> typeRestrictionQuery;

    @Mock
    private Query<TypeRestrictionBuilder> typeRestrictionQuery1;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder1;

    @Mock
    private ProjectionBuilder projectionBuilder;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    private Projection projection;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    long jobId = 1;
    private final String neName = "ne name";
    private Map<String, Object> activityJobAttr;
    private final long activityJobId = 1;
    private List<Map<String, Object>> jobLogList;

    private final long neJobId = 2l;

    @Mock
    JobConfigurationService jobConfigurationService;

    @Test
    public void testFetchJobProperty() {
        final Map<String, Object> poAttr = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "some key");
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "some value");
        jobPropertiesList.add(jobProperty);
        poAttr.put(ActivityConstants.JOB_PROPERTIES, jobPropertiesList);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(poAttr);
        when(dpsReaderMock.findPOByPoId(jobId)).thenReturn(persistenceObjectMock);
        assertEquals(jobPropertiesList, objectUnderTest.fetchJobProperty(jobId));
    }

    @Test
    public void testFetchJobPropertyNotHavingJobPropertyList() {
        final Map<String, Object> poAttr = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        poAttr.put(ActivityConstants.JOB_PROPERTIES, null);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(poAttr);
        when(dpsReaderMock.findPOByPoId(jobId)).thenReturn(persistenceObjectMock);
        objectUnderTest.fetchJobProperty(jobId);
        assertEquals(jobPropertiesList, objectUnderTest.fetchJobProperty(jobId));
    }

    @Test
    public void testRetrieveJob() {
        final Map<String, Object> poAttr = new HashMap<String, Object>();
        when(persistenceObjectMock.getAllAttributes()).thenReturn(poAttr);
        when(dpsReaderMock.findPOByPoId(jobId)).thenReturn(persistenceObjectMock);
        assertEquals(poAttr, objectUnderTest.retrieveJob(jobId));
    }

    @Test
    public void testRetrieveJobWithPoNull() {
        when(dpsReaderMock.findPOByPoId(jobId)).thenReturn(null);
        final Map<String, Object> poAttr = objectUnderTest.retrieveJob(jobId);
        assertTrue(poAttr.isEmpty());
    }

    @Test
    public void testRetrieveWorkflowAttributes() {
        final long activityJobId = 1;
        final long neJobId = 2;

        final String businessKey = "Some Business Key";
        final String activityResult = "Some Result";

        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, activityResult);
        jobPropertiesList.add(jobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobPropertiesList);
        when(activityJobPo.getAllAttributes()).thenReturn(activityJobAttr);
        when(dpsReaderMock.findPOByPoId(activityJobId)).thenReturn(activityJobPo);

        final Map<String, Object> neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(neJobPo.getAllAttributes()).thenReturn(neJobAttr);
        when(dpsReaderMock.findPOByPoId(neJobId)).thenReturn(neJobPo);

        final Map<String, Object> workflowAttributes = new HashMap<String, Object>();
        workflowAttributes.put("result", activityResult);
        workflowAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        assertEquals(workflowAttributes, objectUnderTest.retrieveWorkflowAttributes(activityJobId));
    }

    @Test
    public void testRetrieveJobs() {
        final List<Long> jobIds = new ArrayList<Long>();
        final List<PersistenceObject> persistenceObjectList = new ArrayList<PersistenceObject>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(ShmConstants.PO_ID, 123L);
        poAttributes.put(ShmConstants.NAMESPACE, "shm");
        poAttributes.put(ShmConstants.TYPE, "job");
        when(persistenceObjectMock.getAllAttributes()).thenReturn(poAttributes);
        when(persistenceObjectMock.getPoId()).thenReturn(123L);
        persistenceObjectList.add(persistenceObjectMock);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(poAttributes);
        when(dpsReaderMock.findPOsByPoIds(jobIds)).thenReturn(persistenceObjectList);
        assertNotNull(objectUnderTest.retrieveJobs(jobIds));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveActivityJobResult() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        when(neJobPo.getAttribute(ShmConstants.STATE)).thenReturn(JobState.RUNNING.name());
        final String jobResult = objectUnderTest.retrieveActivityJobResult(123456L);
        assertEquals(jobResult, JobResult.SUCCESS.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveActivityJobFailed() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.FAILED);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        when(neJobPo.getAttribute(ShmConstants.STATE)).thenReturn(JobState.RUNNING.name());
        final String jobResult = objectUnderTest.retrieveActivityJobResult(123456L);
        assertEquals(jobResult, JobResult.FAILED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveActivityJobFailedForNull() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJob.put(ShmConstants.RESULT, null);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        when(neJobPo.getAttribute(ShmConstants.STATE)).thenReturn(JobState.RUNNING.name());
        final String jobResult = objectUnderTest.retrieveActivityJobResult(123456L);
        assertEquals(jobResult, JobResult.FAILED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveActivityJobAllSuccess() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        when(neJobPo.getAttribute(ShmConstants.STATE)).thenReturn(JobState.RUNNING.name());
        final String jobResult = objectUnderTest.retrieveActivityJobResult(123456L);
        assertEquals(jobResult, JobResult.SUCCESS.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveActivityJob1() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJob.put(ShmConstants.RESULT, JobResult.FAILED);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        when(neJobPo.getAttribute(ShmConstants.STATE)).thenReturn(JobState.RUNNING.name());
        final String jobResult = objectUnderTest.retrieveActivityJobResult(123456L);
        assertEquals(jobResult, JobResult.FAILED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveActivityJobSkipped() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SKIPPED);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        when(neJobPo.getAttribute(ShmConstants.STATE)).thenReturn(JobState.RUNNING.name());
        final String jobResult = objectUnderTest.retrieveActivityJobResult(123456L);
        assertEquals(jobResult, JobResult.SKIPPED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_allSuccessReturnsSuccess() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.SUCCESS.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_nullAndSuccessReturnsSuccess() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { null, null, JobResult.SUCCESS };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.SUCCESS.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_allSkippedReturnsSkipped() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.SKIPPED, null, JobResult.SKIPPED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.SKIPPED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_skippedNullAndSuccessReturnsSuccess() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.SKIPPED, null, JobResult.SUCCESS };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.SUCCESS.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_skippedAndSuccessReturnsSuccess() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.SKIPPED, JobResult.SUCCESS, JobResult.SKIPPED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.SUCCESS.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_skippedFailedAndSuccessReturnsFailed() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.FAILED, JobResult.SUCCESS, JobResult.SKIPPED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.FAILED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_allNullReturnsSkipped() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { null, null, null };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.SKIPPED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_cancelledAndSuccessReturnsCancelled() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.CANCELLED, JobResult.SUCCESS };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.CANCELLED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_skippedAndCancelledReturnsCancelled() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.CANCELLED, JobResult.SKIPPED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.CANCELLED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_successSkippedAndCancelledReturnsCancelled() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.CANCELLED, JobResult.SUCCESS, JobResult.SKIPPED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.CANCELLED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_failedSkippedAndCancelledReturnsCancelled() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.CANCELLED, JobResult.FAILED, JobResult.SKIPPED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.CANCELLED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_successFailedAndCancelledReturnsCancelled() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.CANCELLED, JobResult.FAILED, JobResult.SUCCESS };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.CANCELLED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_failedAndCancelledReturnsCancelled() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.CANCELLED, JobResult.FAILED };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.CANCELLED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobResult_successFailedAndSuccessReturnsFailed() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final JobResult[] result = { JobResult.SUCCESS, JobResult.FAILED, JobResult.SUCCESS };
        addActivityJobs(activityJobPos, result);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.FAILED.getJobResult());
    }

    private void addActivityJobs(final List<Map<String, Object>> activityJobPos, final JobResult[] result) {
        for (int i = 0; i < result.length; i++) {
            final Map<String, Object> activityJob = new HashMap<String, Object>();
            activityJob.put(ShmConstants.RESULT, result[i]);
            activityJobPos.add(activityJob);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void retrieveJobDetailsSuccess() {

        final String jobName = "backupjob";
        final String user = "admnistrator";
        final Date creationDate = new Date();
        final String jobType = "Backup";
        final Long mainJobId = 12345L;
        final List<Object[]> objectArray = new ArrayList<Object[]>();
        final List<Object> list = new ArrayList<Object>();
        list.add(123456L);
        list.add(new Date());
        final Object[] projectionArray = list.toArray();
        objectArray.add(projectionArray);

        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();

        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeRestrictionQuery);
        when(typeRestrictionQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ObjectField.PO_ID, mainJobId)).thenReturn(restriction);
        when(queryExecutor.executeProjection(eq(typeRestrictionQuery), any(Projection.class), any(Projection.class))).thenReturn(objectArray);

        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SUCCESS);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(mainJobPo);
        final Map<String, Object> mainJobAttributes = new HashMap<>();

        mainJobAttributes.put(ShmConstants.STARTTIME, creationDate);
        mainJobAttributes.put(ShmConstants.JOBTEMPLATEID, 1542L);
        when(mainJobPo.getAllAttributes()).thenReturn(mainJobAttributes);
        when(mainJobPo.getAttribute(ShmConstants.JOBTEMPLATEID)).thenReturn(1542L);

        when(dpsReaderMock.findPOByPoId(1542L)).thenReturn(templateJobPo);

        when(templateJobPo.getAttribute(ShmConstants.NAME)).thenReturn(jobName);
        when(templateJobPo.getAttribute(ShmConstants.OWNER)).thenReturn(user);
        when(mainJobPo.getAttribute(ShmConstants.CREATION_TIME)).thenReturn(creationDate);
        when(templateJobPo.getAttribute(ShmConstants.JOB_TYPE)).thenReturn(jobType);

        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final Map<String, Object> jobResult = objectUnderTest.getJobDetailsToRaiseAlarm(123456L);
        assertEquals(jobResult.get("TOTAL_NES"), Integer.valueOf(1));
        assertEquals(jobResult.get("owner"), String.valueOf("admnistrator"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getJobTypeToRaiseAlarmSuccess() {
        final Long mainJobId = 12345L;
        final Long templateJobId = 1234L;
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        final Map<String, Object> templateJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, templateJobId);
        templateJobAttributes.put(ShmConstants.JOB_TYPE, "BACKUP");
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttributes);
        when(jobUpdateService.retrieveJobWithRetry(templateJobId)).thenReturn(templateJobAttributes);
        when(dpsReaderMock.findPOByPoId(mainJobId)).thenReturn(mainJobPo);
        when(mainJobPo.getAllAttributes()).thenReturn(mainJobAttributes);
        when(dpsReaderMock.findPOByPoId(templateJobId)).thenReturn(templateJobPo);
        when(templateJobPo.getAllAttributes()).thenReturn(templateJobAttributes);

        final String jobType = objectUnderTest.getJobType(mainJobId);
        assertEquals(jobType, "BACKUP");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void retrieveJobDetailsFailed() {

        final String jobName = "backupjob";
        final String user = "admnistrator";
        final Date creationDate = new Date();
        final String jobType = "Backup";
        final Long mainJobId = 12345L;
        final List<Object[]> objectArray = new ArrayList<Object[]>();
        final List<Object> list = new ArrayList<Object>();
        list.add(123456L);
        list.add(new Date());
        final Object[] projectionArray = list.toArray();
        objectArray.add(projectionArray);

        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();

        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeRestrictionQuery);
        when(typeRestrictionQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ObjectField.PO_ID, mainJobId)).thenReturn(restriction);
        when(queryExecutor.executeProjection(eq(typeRestrictionQuery), any(Projection.class), any(Projection.class))).thenReturn(objectArray);

        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.FAILED);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(mainJobPo);
        when(mainJobPo.getAttribute(ShmConstants.JOBTEMPLATEID)).thenReturn(1542L);
        when(dpsReaderMock.findPOByPoId(1542L)).thenReturn(templateJobPo);
        when(templateJobPo.getAttribute(ShmConstants.NAME)).thenReturn(jobName);
        when(templateJobPo.getAttribute(ShmConstants.OWNER)).thenReturn(user);
        when(templateJobPo.getAttribute(ShmConstants.CREATION_TIME)).thenReturn(creationDate);
        when(templateJobPo.getAttribute(ShmConstants.JOB_TYPE)).thenReturn(jobType);

        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final Map<String, Object> jobResult = objectUnderTest.getJobDetailsToRaiseAlarm(123456L);
        assertNotNull(jobResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void retrieveJobDetailsSkipped() {

        final String jobName = "backupjob";
        final String user = "admnistrator";
        final Date creationDate = new Date();
        final String jobType = "Backup";
        final Long mainJobId = 12345L;
        final List<Object[]> objectArray = new ArrayList<Object[]>();
        final List<Object> list = new ArrayList<Object>();
        list.add(123456L);
        list.add(new Date());
        final Object[] projectionArray = list.toArray();
        objectArray.add(projectionArray);

        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();

        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeRestrictionQuery);
        when(typeRestrictionQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ObjectField.PO_ID, mainJobId)).thenReturn(restriction);
        when(queryExecutor.executeProjection(eq(typeRestrictionQuery), any(Projection.class), any(Projection.class))).thenReturn(objectArray);

        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.SKIPPED);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(mainJobPo);
        when(mainJobPo.getAttribute(ShmConstants.JOBTEMPLATEID)).thenReturn(1542L);
        when(dpsReaderMock.findPOByPoId(1542L)).thenReturn(templateJobPo);
        when(templateJobPo.getAttribute(ShmConstants.NAME)).thenReturn(jobName);
        when(templateJobPo.getAttribute(ShmConstants.OWNER)).thenReturn(user);
        when(templateJobPo.getAttribute(ShmConstants.CREATION_TIME)).thenReturn(creationDate);
        when(templateJobPo.getAttribute(ShmConstants.JOB_TYPE)).thenReturn(jobType);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final Map<String, Object> jobResult = objectUnderTest.getJobDetailsToRaiseAlarm(123456L);
        assertNotNull(jobResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveNeJobFailed() {
        final List<Map<String, Object>> activityJobPos = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJob = new HashMap<String, Object>();
        activityJob.put(ShmConstants.RESULT, JobResult.FAILED);
        activityJobPos.add(activityJob);
        when(dpsReaderMock.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(activityJobPos);
        final String jobResult = objectUnderTest.retrieveNeJobResult(123456L);
        assertEquals(jobResult, JobResult.FAILED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetActivitiesCount() {
        final List<PersistenceObject> activityJobPos = new ArrayList<PersistenceObject>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ShmConstants.STATE, JobState.COMPLETED);
        activityJobPos.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(activityJobPos);
        final Map<String, Object> activityCount = objectUnderTest.getActivitiesCount(123456L);
        Assert.assertTrue((Integer) activityCount.get("totalActivities") == 1);
        Assert.assertTrue((Integer) activityCount.get("completedActivities") == 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetActivitiesCountCancelled() {
        final List<PersistenceObject> activityJobPos = new ArrayList<PersistenceObject>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ShmConstants.STATE, JobState.COMPLETED);
        activityJobPos.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(activityJobPos);
        final Map<String, Object> activityCount = objectUnderTest.getActivitiesCount(123456L);
        Assert.assertTrue((Integer) activityCount.get("totalActivities") == 1);
        Assert.assertTrue((Integer) activityCount.get("completedActivities") == 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetActivitiesCountSystemCancelled() {
        final List<PersistenceObject> activityJobPos = new ArrayList<PersistenceObject>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ShmConstants.STATE, JobState.SYSTEM_CANCELLED);
        activityJobPos.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(activityJobPos);
        final Map<String, Object> activityCount = objectUnderTest.getActivitiesCount(123456L);
        Assert.assertTrue((Integer) activityCount.get("totalActivities") == 1);
        Assert.assertTrue((Integer) activityCount.get("completedActivities") == 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetActivitiesCountCancelling() {
        final List<PersistenceObject> activityJobPos = new ArrayList<PersistenceObject>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ShmConstants.STATE, JobState.CANCELLING);
        activityJobPos.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(activityJobPos);
        final Map<String, Object> activityCount = objectUnderTest.getActivitiesCount(123456L);
        Assert.assertTrue((Integer) activityCount.get("totalActivities") == 1);
        Assert.assertTrue((Integer) activityCount.get("completedActivities") == 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetActivitiesCountSystemCancelling() {
        final List<PersistenceObject> activityJobPos = new ArrayList<PersistenceObject>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ShmConstants.STATE, JobState.SYSTEM_CANCELLING);
        activityJobPos.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(activityJobPos);
        final Map<String, Object> activityCount = objectUnderTest.getActivitiesCount(123456L);
        Assert.assertTrue((Integer) activityCount.get("totalActivities") == 1);
        Assert.assertTrue((Integer) activityCount.get("completedActivities") == 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetJobTemplateDetailsCancelled() {
        List<SHMJobData> shmJobDataList = new ArrayList<SHMJobData>();
        final List<PersistenceObject> jobTemplatePOs = new ArrayList<PersistenceObject>();
        final Map<String, Object> attributeMap = PersistanceDetails();
        attributeMap.put(ShmConstants.JobTemplateConstants.ISCANCELLED, true);
        jobTemplatePOs.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(activityJobPo.getPoId()).thenReturn(12345L);
        final SHMJobData sHMJobData = new SHMJobData();
        sHMJobData.setJobTemplateId(123456L);
        shmJobDataList.add(sHMJobData);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(jobTemplatePOs);
        shmJobDataList = objectUnderTest.getJobTemplateDetails(shmJobDataList);
    }

    /**
     * @param attributeMap
     * @return
     */
    private Map<String, Object> PersistanceDetails() {
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationdetails = new HashMap<String, Object>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<String> neNames = new ArrayList<String>();
        final Map<String, Object> neInfo = new HashMap<String, Object>();
        final Map<String, Object> schedule = new HashMap<String, Object>();
        final List<Map<String, Object>> schedulePropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> scheduleProperty = new HashMap<String, Object>();
        scheduleProperty.put(ShmConstants.NAME, ShmConstants.START_DATE);
        scheduleProperty.put(ShmConstants.VALUE, new Date().toString());
        schedulePropertyList.add(scheduleProperty);
        schedule.put(ShmConstants.SCHEDULINGPROPERTIES, schedulePropertyList);
        schedule.put(ShmConstants.JOB_ACTIVITY_SCHEDULE, ExecMode.SCHEDULED.getMode());
        neInfo.put(ShmConstants.COLLECTION_NAMES, collectionNames);
        neInfo.put(ShmConstants.NENAMES, neNames);
        jobConfigurationdetails.put(ShmConstants.SELECTED_NES, neInfo);
        attributeMap.put(ShmConstants.NAME, "name");
        attributeMap.put(ShmConstants.JOB_TYPE, "jobType");
        attributeMap.put(ShmConstants.OWNER, "owner");
        attributeMap.put(ShmConstants.JobTemplateConstants.ISDELETABLE, false);
        attributeMap.put(ShmConstants.CREATION_TIME, new Date());
        jobConfigurationdetails.put(ShmConstants.SELECTED_NES, neInfo);
        jobConfigurationdetails.put(ShmConstants.MAINSCHEDULE, schedule);
        attributeMap.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationdetails);
        return attributeMap;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetJobTemplateDetailsScheduled() {
        List<SHMJobData> shmJobDataList = new ArrayList<SHMJobData>();
        final List<PersistenceObject> jobTemplatePOs = new ArrayList<PersistenceObject>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<String> neNames = new ArrayList<String>();
        final Map<String, Object> neInfo = new HashMap<String, Object>();
        neInfo.put(ShmConstants.COLLECTION_NAMES, collectionNames);
        neInfo.put(ShmConstants.NENAMES, neNames);
        final Map<String, Object> attributeMap = PersistanceDetails();
        attributeMap.put(ShmConstants.JobTemplateConstants.ISCANCELLED, false);
        jobTemplatePOs.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(activityJobPo.getPoId()).thenReturn(12345L);
        final SHMJobData sHMJobData = new SHMJobData();
        sHMJobData.setJobTemplateId(123456L);
        shmJobDataList.add(sHMJobData);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(jobTemplatePOs);
        shmJobDataList = objectUnderTest.getJobTemplateDetails(shmJobDataList);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetJobTemplateDetailssubmitted() {
        List<SHMJobData> shmJobDataList = new ArrayList<SHMJobData>();
        final List<PersistenceObject> jobTemplatePOs = new ArrayList<PersistenceObject>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<String> neNames = new ArrayList<String>();
        final Map<String, Object> neInfo = new HashMap<String, Object>();
        neInfo.put(ShmConstants.COLLECTION_NAMES, collectionNames);
        neInfo.put(ShmConstants.NENAMES, neNames);
        final Map<String, Object> attributeMap = PersistanceDetails();
        attributeMap.put(ShmConstants.NAME, null);
        attributeMap.put(ShmConstants.JOB_TYPE, null);
        attributeMap.put(ShmConstants.OWNER, null);
        attributeMap.put(ShmConstants.JobTemplateConstants.ISDELETABLE, true);
        attributeMap.put(ShmConstants.JobTemplateConstants.ISCANCELLED, false);
        jobTemplatePOs.add(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(attributeMap);
        when(activityJobPo.getPoId()).thenReturn(12345L);
        final SHMJobData sHMJobData = new SHMJobData();
        sHMJobData.setJobTemplateId(123456L);
        shmJobDataList.add(sHMJobData);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(jobTemplatePOs);
        shmJobDataList = objectUnderTest.getJobTemplateDetails(shmJobDataList);
    }

    @Test
    public void testPersistRunningJobAttributes() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = null;
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final List<Map<String, Object>> jobLogList = null;
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.ACTIVITY_RESULT);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        objectUnderTest.persistRunningJobAttributes(123456L, jobPropertyList, jobLogList);
    }

    @Test
    public void testPersistRunningJobAttributesFailed() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
        jobPropertyList.add(jobPropertyMap);
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, "log Message");
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(logEntry);
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.CHECK_FALSE);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        jobAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        objectUnderTest.persistRunningJobAttributes(123456L, jobPropertyList, jobLogList);
    }

    @Test
    public void testGetupdatedJobProperties() {
        final List<Map<String, Object>> storedJobproperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> updatedpropertiesList = new ArrayList<Map<String, Object>>();

        storedJobproperties.add(createMap("actionid", "value1"));
        storedJobproperties.add(createMap("restore", "value2"));

        jobProperties.add(createMap("actionid", "value1Updated"));
        jobProperties.add(createMap("verify", "value3"));

        updatedpropertiesList.add(createMap("actionid", "value1Updated"));
        updatedpropertiesList.add(createMap("restore", "value2"));
        updatedpropertiesList.add(createMap("verify", "value3"));

        org.junit.Assert.assertEquals(updatedpropertiesList, objectUnderTest.getUpdatedJobProperties(jobProperties, storedJobproperties));

    }

    @Test
    public void testGetupdatedJobPropertiesWhenStoredPropertiesNullOrEmpty() {
        final List<Map<String, Object>> storedJobproperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> updatedpropertiesList = new ArrayList<Map<String, Object>>();

        jobProperties.add(createMap("key1", "value1Updated"));
        jobProperties.add(createMap("key3", "value3"));

        updatedpropertiesList.add(createMap("key1", "value1Updated"));
        updatedpropertiesList.add(createMap("key3", "value3"));

        org.junit.Assert.assertEquals(updatedpropertiesList, objectUnderTest.getUpdatedJobProperties(jobProperties, storedJobproperties));

    }

    @Test
    public void testGetupdatedJobPropertiesWhenPropertiesNullOrEmpty() {
        final List<Map<String, Object>> storedJobproperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> updatedpropertiesList = new ArrayList<Map<String, Object>>();

        storedJobproperties.add(createMap("key1", "value1"));
        storedJobproperties.add(createMap("key2", "value2"));

        updatedpropertiesList.add(createMap("key1", "value1"));
        updatedpropertiesList.add(createMap("key2", "value2"));

        org.junit.Assert.assertEquals(updatedpropertiesList, objectUnderTest.getUpdatedJobProperties(jobProperties, storedJobproperties));

    }

    @Test
    public void getProjectAttributes() {
        final String namespace = "";
        final String type = "";
        final Map<Object, Object> restrictionAttributes = new HashMap<Object, Object>();
        final List<String> projectedAttributes = new ArrayList<String>();
        final List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>();
        when(dpsReaderMock.getProjectedAttributes(namespace, type, restrictionAttributes, projectedAttributes)).thenReturn(expected);
        final List<Map<String, Object>> actual = objectUnderTest.getProjectedAttributes(namespace, type, restrictionAttributes, projectedAttributes);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void readAndPersistRunningJobAttributesFailed() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = null;
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final List<Map<String, Object>> jobLogList = null;
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.ACTIVITY_RESULT);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        final boolean actual = objectUnderTest.readAndPersistRunningJobAttributes(123456L, jobPropertyList, jobLogList, null);
        Assert.assertEquals(false, actual);
    }

    @Test
    public void readAndPersistRunningJobAttributes_DuplicateMessages() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogListDB = new ArrayList<Map<String, Object>>();
        final Map<String, Object> logEntryDb = new HashMap<String, Object>();
        logEntryDb.put(ActivityConstants.JOB_LOG_MESSAGE, "log Message");
        logEntryDb.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntryDb.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        jobLogListDB.add(logEntryDb);
        jobPropertyList.add(jobPropertyMap);
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, "log Message");
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        jobLogList.add(logEntry);
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.CHECK_FALSE);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        jobAttributes.put(ActivityConstants.JOB_LOG, jobLogListDB);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        final boolean updated = objectUnderTest.readAndPersistRunningJobAttributes(123456L, jobPropertyList, jobLogList, null);
        Assert.assertEquals(true, updated);
    }

    @Test
    public void testReadAndPersistRunningJobAttributesShouldReturnTrueIfEitherOfJobPropertyListOrJobLogListOrActivityProgressPercentageAreNonEmpty() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
        jobPropertyList.add(jobPropertyMap);
        //testReadAndPersistRunningJobAttributesShouldReturnTrueIfEitherOfJobPropertyListOrJobLogListOrActivityProgressPercentageAreNonEmpty should return true when jobPropertyList is non-empty and jobLogList is empty.
        Assert.assertTrue(objectUnderTest.readAndPersistRunningJobAttributes(123456L, jobPropertyList, jobLogList, null));
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, "log Message");
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        jobLogList.add(logEntry);
        final List<Map<String, Object>> emptyJobPropertyList = new ArrayList<>();
        //testReadAndPersistRunningJobAttributesShouldReturnTrueIfEitherOfJobPropertyListOrJobLogListOrActivityProgressPercentageAreNonEmpty should return true when jobPropertyList is empty and jobLogList is non-empty.
        Assert.assertTrue(objectUnderTest.readAndPersistRunningJobAttributes(123456L, emptyJobPropertyList, jobLogList, null));
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.CHECK_FALSE);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        jobAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(123456L)).thenReturn(neJobPo);
        final boolean actual = objectUnderTest.readAndPersistRunningJobAttributes(123456L, jobPropertyList, jobLogList, null);
        Assert.assertEquals(true, actual);
    }

    @Test
    public void testReadAndPersistRunningJobAttributesShouldReturnFalseIfAllJobPropertyListAndJobLogListAndActivityProgressPercentageAreEmptyOrNull() {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        Assert.assertFalse(objectUnderTest.readAndPersistRunningJobAttributes(123456L, jobPropertyList, jobLogList, null));
        Assert.assertFalse(objectUnderTest.readAndPersistRunningJobAttributes(123456L, null, null, null));

    }

    @Test
    public void testReadAndPersistRunningJobAttributesWithInvalidProgressValue() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
        jobPropertyList.add(jobPropertyMap);
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        final Date date = new Date();
        final String logMessage = "log Message";
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, date);
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        jobLogList.add(logEntry);
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.CHECK_FALSE);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        jobAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
        jobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 99.9);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(activityJobId)).thenReturn(neJobPo);
        final boolean actual = objectUnderTest.readAndPersistRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 111.23);
        Assert.assertEquals(true, actual);
        final Map<String, Object> validatedAttributes = new HashMap<String, Object>();
        validatedAttributes.put(ShmConstants.LAST_LOG_MESSAGE, logMessage + ShmConstants.DELIMITER_PIPE + date.getTime());
        validatedAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        validatedAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 100.0);
        validatedAttributes.put(ActivityConstants.JOB_LOG, jobLogList);
        verify(dpsWriterMock).update(activityJobId, validatedAttributes);
    }

    @Test
    public void testreadAndPersistRunningJobAttributesWithValidProgressValue() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
        jobPropertyList.add(jobPropertyMap);
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList1 = new ArrayList<Map<String, String>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Date date = new Date();
        final String logMessage = "log Message";
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, date);
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        jobLogList.add(logEntry);
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.CHECK_FALSE);
        jobPropertyList1.add(jobProperty);
        jobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        jobAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
        jobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 88.8);
        when(neJobPo.getAllAttributes()).thenReturn(jobAttributes);
        when(dpsReaderMock.findPOByPoId(activityJobId)).thenReturn(neJobPo);
        activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        final boolean actual = objectUnderTest.readAndPersistRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 99.9);
        Assert.assertEquals(true, actual);
        final Map<String, Object> validatedAttributes = new HashMap<String, Object>();
        validatedAttributes.put(ShmConstants.LAST_LOG_MESSAGE, logMessage + ShmConstants.DELIMITER_PIPE + date.getTime());
        validatedAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList1);
        validatedAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 99.9);
        validatedAttributes.put(ActivityConstants.JOB_LOG, jobLogList);
        verify(dpsWriterMock).update(activityJobId, validatedAttributes);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void TestAddOrUpdateOrRemoveJobProperties() {
        final List<Map<String, Object>> JobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "NE NAME");
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, neName);
        JobPropertyList.add(jobProperty);
        activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, JobPropertyList);
        when(dpsReaderMock.findPOByPoId(activityJobId)).thenReturn(persistenceObjectMock);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(activityJobAttr);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttr);

        final Map<String, String> propertyTobeAdded = new HashMap<String, String>();

        propertyTobeAdded.put("NE NAME", neName);
        jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, "log Message");
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, "INFO");
        activityJobAttr.put(ActivityConstants.JOB_LOG, jobLogList);
        jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(logEntry);
        objectUnderTest.addOrUpdateOrRemoveJobProperties(activityJobId, propertyTobeAdded, jobLogList);
        verify(dpsWriterMock, times(1)).update(anyLong(), anyMap());
    }

    @Test
    public void testRetrieveWaitingActivityDetails() {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(ShmConstants.NE_JOB_ID, 2l);
        restrictions.put(ShmConstants.STATE, JobState.WAIT_FOR_USER_INPUT.name());
        when(dpsReaderMock.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions)).thenReturn(Arrays.asList(persistenceObjectMock));
        when(persistenceObjectMock.getAttribute(ShmConstants.NAME)).thenReturn("upload");
        when(persistenceObjectMock.getPoId()).thenReturn(2l);
        final Map<String, Object> activityDetails = objectUnderTest.retrieveWaitingActivityDetails(2l);
        assertNotNull(activityDetails);
        assertEquals(activityDetails.size(), 2);
    }

    @Test
    public void testReadAndPersistRunningJobStepDuration() {
        final String stepName = "EXECUTE";
        final String stepNameAndDurationToPersist = stepName + "=" + 10.0;
        when(dpsReaderMock.findPOByPoId(activityJobId)).thenReturn(persistenceObjectMock);
        when(persistenceObjectMock.getAttribute(ShmConstants.STEP_DURATIONS)).thenReturn("[PRECHECK=8.0]");
        objectUnderTest.readAndPersistRunningJobStepDuration(activityJobId, stepNameAndDurationToPersist, stepName);
        verify(dpsWriterMock, times(1)).update(eq(activityJobId), anyMap());
    }

    @Test
    public void testIsNEJobProceedsForCancel() {
        final Map<String, Object> neJobPoAttributes = new HashMap<String, Object>();
        neJobPoAttributes.put(ShmConstants.PO_ID, 123L);
        neJobPoAttributes.put(ShmConstants.MAIN_JOB_ID, 111L);
        neJobPoAttributes.put(ShmConstants.NE_NAME, "MSC27");
        neJobPoAttributes.put(ShmConstants.STATE, "RUNNING");
        when(persistenceObjectMock.getAllAttributes()).thenReturn(neJobPoAttributes);
        when(dpsReaderMock.findPOByPoId(123L)).thenReturn(activityJobPo);
        when(activityJobPo.getAllAttributes()).thenReturn(neJobPoAttributes);

        final Map<String, Object> mainJobPoAttributes = new HashMap<String, Object>();
        mainJobPoAttributes.put(ShmConstants.JOB_TEMPLATE_ID, 1234L);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(mainJobPoAttributes);
        when(dpsReaderMock.findPOByPoId(111L)).thenReturn(mainJobPo);
        when(mainJobPo.getAllAttributes()).thenReturn(mainJobPoAttributes);

        final Map<String, Object> jobTempPoAttributes = new HashMap<String, Object>();
        jobTempPoAttributes.put(ShmConstants.JOB_TYPE, JobType.UPGRADE.toString());
        when(persistenceObjectMock.getAllAttributes()).thenReturn(jobTempPoAttributes);
        when(dpsReaderMock.findPOByPoId(1234L)).thenReturn(persistenceObjectMock);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(jobTempPoAttributes);

        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.AXE);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList(), Matchers.anyString())).thenReturn(networkElementsList);
        assertEquals(true, objectUnderTest.isNEJobProceedsForCancel(123L));

    }

    @Test
    public void testActivityDetailsByNeJobId() {

        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> activityJobDetails = new HashMap<String, Object>();
        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "some key");
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "some value");

        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(jobProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);
        jobProperties.add(executionNameProperty);
        activityJobDetails.put(ShmJobConstants.ACTIVITY_NAME, "nodehealthcheck");
        activityJobDetails.put(ShmConstants.LAST_LOG_MESSAGE, "Activity has completed successfully");
        activityJobDetails.put(ShmJobConstants.JOBPROPERTIES, jobProperties);

        List<PersistenceObject> listPo = new ArrayList<>();
        listPo.add(persistenceObjectMock);
        when(persistenceObjectMock.getPoId()).thenReturn(123l);
        when(dpsReaderMock.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions)).thenReturn(listPo);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(activityJobDetails);
        assertEquals(123l, objectUnderTest.getActivityAttributesByNeJobId(neJobId, restrictions).get(0).get(ShmConstants.ACTIVITY_JOB_ID));
    }

    /**
     * @param string
     * @param string2
     * @return
     */
    private Map<String, Object> createMap(final String key, final String value) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ActivityConstants.JOB_PROP_KEY, key);
        map.put(ActivityConstants.JOB_PROP_VALUE, value);
        return map;
    }
}
