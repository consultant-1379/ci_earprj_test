/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;

import javax.enterprise.event.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsIllegalStateException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.BadRequestException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.instrumentation.MainJobInstrumentation;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobExecutorLocal;
import com.ericsson.oss.services.shm.jobs.common.api.JobConfigurationException;
import com.ericsson.oss.services.shm.jobs.common.api.JobQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobLogMapper;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.jobservice.common.BuilderFactory;
import com.ericsson.oss.services.shm.jobservice.common.CommentInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobBuilder;
import com.ericsson.oss.services.shm.jobservice.common.JobFactory;
import com.ericsson.oss.services.shm.jobservice.common.JobHandlerErrorCodes;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobPropertyBuilder;
import com.ericsson.oss.services.shm.jobservice.common.ShmJobHandler;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobWebPushEvent.class, SHMJobServiceImpl.class})
public class SHMJobServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobServiceImplTest.class);

    @Mock
    JobConfigurationService jobConfigurationService;

    @Mock
    JobOutput joboutput;

    @Mock
    JobBuilder jobBuilder;

    @Mock
    BuilderFactory builderFactory;

    @Mock
    List<SHMJobData> shmJobDataList;

    @Mock
    SHMJobUtil shmJobUtil;

    @Mock
    ShmJobs shmJobsMock;

    @InjectMocks
    SHMJobServiceImpl objectUnderTest;

    @Mock
    ShmJobHandler shmJobHandler;

    @Mock
    DpsWriter persistenceAdapter;

    @Mock
    UserContextBean userContextBean;

    @Mock
    DpsReader dpsReader;

    @Mock
    SystemRecorder systemRecorder;

    @Mock
    JobsDeletionReport jobsDeletionReport;

    @Mock
    private JobFactory jobFactory;

    @Mock
    JobOutput jobOutput;

    @Mock
    List<JobLogResponse> responseList;

    NeJobInput jobInput;

    @Mock
    JobReportData jobReportDataMock;

    @Mock
    List<JobTemplate> jobTemplateMock;

    @Mock
    JobTemplate jobTemplate;

    @Mock
    JobQuery jobConfigQueryMock;

    @Mock
    SHMJobService jobExecutorLocalMock;

    @Mock
    CommentInfo commentInfoMock;

    @Mock
    Object objMock;

    @Mock
    JobMapper shmJobsMapper;

    @Mock
    ExportJobLogRequest exportJobLogRequestMock;

    @Mock
    JobLogResponse jobLogResponseMock;

    @Mock
    Map<Long, JobDetails> jobDetailsMapMock;

    @Mock
    PersistenceObject mainJobPo;

    @Mock
    PersistenceObject templateJobPo;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    JobExecutorLocal jobExecutorLocal;

    @Mock
    private WorkflowInstanceNotifier localWorkflowQueryServiceProxy;

    @Mock
    JobPropertyBuilder jobPropertyBuilder;

    @Mock
    Map<Long, Map<String, Object>> attributesHolderListMock;

    @Mock
    PoAttributesHolder poAttributesHolder;

    private List<Long> po_IDs = null;

    @Mock
    Map<String, Object> attributesMapMock;

    @Mock
    PersistenceObject poMock;

    @Mock
    PersistenceObject secondPoMock;

    @Mock
    DataPersistenceService dataPersistenceService;

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
    DataBucket dataBucket;

    @Mock
    QueryExecutor queryExecutor;

    @Mock
    Projection projection;

    @Mock
    JobParameterChangeListener jobParameterChangeListener;

    @Mock
    PersistenceObject firstActivityJob;

    @Mock
    PersistenceObject secondActivityJob;

    @Mock
    private JobReportDetails jobReportDetails;

    @Mock
    List<Map<String, Object>> jobCommentObjects;

    @Mock
    private List<Map<String, Object>> listMapMock;

    @Mock
    private SHMJobServiceHelper shmJobServiceHelper;

    @Mock
    private List<Object> listMock;

    @Mock
    private NeDetails neDetails;

    @Mock
    private RetryManager retryManager;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    private DpsRetryPolicies dpsRetryPoliciesMock;

    @Mock
    private JobLogMapper jobLogMapper;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private SHMJobServiceRetryProxy shmJobServiceRetryProxyMock;

    @Mock
    private JobsDeletionService jobsDeletionService;

    @Mock
    private JobsDeletionReport jobsDeletionReported;

    @Mock
    private SHMJobServiceImplHelper SHMjobServiceImplHelper;

    @Mock
    private MainJobInstrumentation mainJobInstrumentation;

    @Mock
    private Event<JobWebPushEvent> eventSender;

    @Mock
    private JobWebPushEvent jobWebPushEventMock;

    @Mock
    private ActiveSessionsController activeSessionsControllerMock;

    @Mock
    BatchParameterChangeListener batchParameterChangeListener;

    @Mock
    JobsReader jobsReader;

    final Map<String, Object> attributes = new HashMap<String, Object>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        createPOIds();
    }

    private void createPOIds() {
        po_IDs = new ArrayList<Long>();
        po_IDs.add(12345L);
        po_IDs.add(23456L);
        po_IDs.add(34567L);
        po_IDs.add(45678L);
        po_IDs.add(56789L);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getJobDetailstestReturnNotNull() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("packageName");
        jobInput.setSortBy("asc");
        final ShmJobs shmJobs = new ShmJobs();
        final SHMJobData shmJobData = new SHMJobData();
        shmJobData.setJobName("TEST");
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        when(shmJobUtil.sortAndGetPageData(shmJobsDataList, jobInput)).thenReturn(joboutput);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobsMock);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(1);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(poAttributesHolder.getTemplateJobDetails(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, jobInput)).thenReturn(jobConfigurationAttributesHolder);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(100);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);
        verify(systemRecorder, times(1)).recordEvent(Matchers.anyString(), (EventLevel) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getJobDetails_success() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("packageName");
        jobInput.setSortBy("asc");
        final SHMJobData shmJobData = new SHMJobData();
        shmJobData.setJobName("TEST");

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        when(shmJobUtil.sortAndGetPageData(shmJobsDataList, jobInput)).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobsMock);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(attributesHolderListMock);
        when(poAttributesHolder.findPOsByPoIds(po_IDs)).thenReturn(attributesHolderListMock);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);
        verify(systemRecorder, times(1)).recordEvent(Matchers.anyString(), (EventLevel) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        verify(activeSessionsControllerMock, times(1)).decrementAndGet(anyString());
    }

    @Test
    public void test_get_Filter_JobDetails_For_JobName_Success() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("jobName");
        jobInput.setSortBy("asc");
        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("jobName");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("CreateBackup");
        objFilterDetails.add(objFilterDetail);
        jobInput.setFilterDetails(objFilterDetails);

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final SHMJobData objShmJobData = new SHMJobData();
        objShmJobData.setJobName("CreateBackup");
        objShmJobData.setJobType("Backup");
        shmJobsDataList.add(objShmJobData);

        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        joboutput = new JobOutput();
        when(shmJobUtil.sortAndGetPageData(anyList(), any(JobInput.class))).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobs);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);

    }

    @Test
    public void test_get_Filter_JobDetails_For_JobType_Success() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("jobName");
        jobInput.setSortBy("asc");
        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("jobName");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("CPP");
        objFilterDetails.add(objFilterDetail);
        jobInput.setFilterDetails(objFilterDetails);

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final SHMJobData objShmJobData = new SHMJobData();
        objShmJobData.setJobName("CreateBackup");
        objShmJobData.setJobType("CPP");
        shmJobsDataList.add(objShmJobData);

        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        joboutput = new JobOutput();
        when(shmJobUtil.sortAndGetPageData(anyList(), any(JobInput.class))).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobs);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);

    }

    @Test
    public void test_get_Filter_JobDetails_For_CreatedBy_Success() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("jobName");
        jobInput.setSortBy("asc");
        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("createdBy");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("shmuser");
        objFilterDetails.add(objFilterDetail);
        jobInput.setFilterDetails(objFilterDetails);

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final SHMJobData objShmJobData = new SHMJobData();
        objShmJobData.setJobName("CreateBackup");
        objShmJobData.setJobType("CPP");
        shmJobsDataList.add(objShmJobData);

        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        joboutput = new JobOutput();
        when(shmJobUtil.sortAndGetPageData(anyList(), any(JobInput.class))).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobs);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);

    }

    @Test
    public void test_get_Filter_JobDetails_For_Progress_Success() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("jobName");
        jobInput.setSortBy("asc");
        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("progress");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("100.0");
        objFilterDetails.add(objFilterDetail);
        jobInput.setFilterDetails(objFilterDetails);

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final SHMJobData objShmJobData = new SHMJobData();
        objShmJobData.setJobName("CreateBackup");
        objShmJobData.setJobType("CPP");
        objShmJobData.setProgress(100.0);

        shmJobsDataList.add(objShmJobData);

        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        joboutput = new JobOutput();
        when(shmJobUtil.sortAndGetPageData(anyList(), any(JobInput.class))).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobs);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);

    }

    @Test
    public void test_get_Filter_JobDetails_For_Stataus_Success() {
        final JobInput jobInput = new JobInput();
        final List<Map<String, Object>> objInputMap = new ArrayList<Map<String, Object>>();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("jobName");
        jobInput.setSortBy("asc");
        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("status");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("COMPLETED");
        objFilterDetails.add(objFilterDetail);
        jobInput.setFilterDetails(objFilterDetails);

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final SHMJobData objShmJobData = new SHMJobData();
        objShmJobData.setJobName("CreateBackup");
        objShmJobData.setJobType("CPP");
        objShmJobData.setProgress(100.0);
        objShmJobData.setStatus("COMPLETED");

        shmJobsDataList.add(objShmJobData);

        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        joboutput = new JobOutput();
        when(shmJobUtil.sortAndGetPageData(anyList(), any(JobInput.class))).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobs);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);

    }

    @Test
    public void test_get_Filter_JobDetails_For_Result_Success() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("jobName");
        jobInput.setSortBy("asc");
        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("status");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("SUCCESS");
        objFilterDetails.add(objFilterDetail);
        jobInput.setFilterDetails(objFilterDetails);

        final ShmJobs shmJobs = new ShmJobs();
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final SHMJobData objShmJobData = new SHMJobData();
        objShmJobData.setJobName("CreateBackup");
        objShmJobData.setJobType("CPP");
        objShmJobData.setProgress(100.0);
        objShmJobData.setStatus("COMPLETED");
        objShmJobData.setStatus("SUCCESS");

        shmJobsDataList.add(objShmJobData);

        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        joboutput = new JobOutput();
        when(shmJobUtil.sortAndGetPageData(anyList(), any(JobInput.class))).thenReturn(joboutput);
        when(jobConfigurationService.getJobTemplateDetails(shmJobDataList)).thenReturn(shmJobsDataList);
        when(shmJobsMapper.getSHMJobsDetails(anyList(), any(shmJobs.getClass()))).thenReturn(shmJobs);
        when(shmJobsMock.getJobDetailsMap()).thenReturn(jobDetailsMapMock);
        when(jobDetailsMapMock.size()).thenReturn(2);
        final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<Long, Map<String, Object>>();
        jobConfigurationAttributesHolder.put(2l, new HashMap<String, Object>());
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        when(dpsRetryPoliciesMock.getJobDetailsRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(jobConfigurationAttributesHolder, response);
        when(shmJobsMapper.getJobConfigurationDetails(attributesHolderListMock, shmJobsMock)).thenReturn(shmJobsMock);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        final JobOutput jobOutput = objectUnderTest.getJobDetails(jobInput);
        assertNotNull(jobOutput);

    }

    @Test
    public void testCreateShmJob() throws Exception {
        final JobInfo job = new JobInfo();
        final Map<String, String> listOfPkgNames = new HashMap<String, String>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<Map<String, Object>> fdnNames = new ArrayList<Map<String, Object>>();
        final HashMap<String, Object> fdns = new HashMap<String, Object>();
        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        final Map<String, Object> configurationsMap = new HashMap<String, Object>();
        final List<Map<String, Object>> propertiesMapList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> propertiesMap = new HashMap<String, Object>();
        final Map<String, Object> response = new HashMap<String, Object>();
        jobAttributesForBackup(job, listOfPkgNames, collectionNames, fdnNames, fdns);
        jobAttributesForUpgrade(job, listOfPkgNames, collectionNames, fdnNames, fdns);
        configurationsForBackup(job, configurations, configurationsMap, propertiesMapList, propertiesMap);
        configurationsForUpgrade(job, configurations, configurationsMap, propertiesMapList, propertiesMap);
        response.put(ShmConstants.JOBCONFIGID, 1);
        response.put("jobName", "jobName");
        response.put("errorCode", JobHandlerErrorCodes.SUCCESS.getResponseDescription());
        when(shmJobHandler.populateAndPersistJobConfigurationData(Matchers.any(JobInfo.class))).thenReturn(response);
        when(builderFactory.selectJobBuilder(Matchers.any(String.class))).thenReturn(jobBuilder);

        when(jobFactory.createJob(Matchers.any(JobInfo.class))).thenReturn(job);

        assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), objectUnderTest.createShmJob(job).get("errorCode").toString());

    }

    @Test(expected = BadRequestException.class)
    public void testCreateShmJob_throwsException() throws Exception {
        assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), objectUnderTest.createShmJob(null).get("errorCode").toString());
    }

    /**
     * @param job
     * @param configurations
     * @param configurationsMap
     * @param propertiesMapList
     * @param propertiesMap
     */
    private void configurationsForBackup(final JobInfo job, final List<Map<String, Object>> configurations, final Map<String, Object> configurationsMap,
                                         final List<Map<String, Object>> propertiesMapList, final Map<String, Object> propertiesMap) {
        propertiesMap.put("key", "keyname");
        propertiesMap.put("value", "valuename");
        propertiesMapList.add(propertiesMap);
        configurationsMap.put("platform", "CPP");
        configurationsMap.put("properties", propertiesMapList);
        configurations.add(configurationsMap);
        job.setConfigurations(configurations);
    }

    /**
     * @param job
     * @param configurations
     * @param configurationsMap
     * @param propertiesMapList
     * @param propertiesMap
     */
    private void configurationsForUpgrade(final JobInfo job, final List<Map<String, Object>> configurations, final Map<String, Object> configurationsMap,
                                          final List<Map<String, Object>> propertiesMapList, final Map<String, Object> propertiesMap) {
        propertiesMap.put("key", "keyname");
        propertiesMap.put("value", "valuename");
        propertiesMapList.add(propertiesMap);
        configurationsMap.put("platform", "CPP");
        configurationsMap.put("properties", propertiesMapList);
        configurations.add(configurationsMap);
        job.setConfigurations(configurations);
    }

    /**
     * @param job
     * @param listOfPkgNames
     * @param collections
     * @param collectionNames
     * @param fdnNames
     * @param fdns
     */
    private void jobAttributesForBackup(final JobInfo job, final Map<String, String> listOfPkgNames, final List<String> collectionNames, final List<Map<String, Object>> fdnNames,
                                        final Map<String, Object> fdns) {
        collectionNames.add("123456");
        fdns.put("name", "ne1");
        fdnNames.add(fdns);
        listOfPkgNames.put("CPP", "CPPSoftwarePackage");
        listOfPkgNames.put("ECIM", "ECIMSoftwarePackage");
        job.setPackageNames(listOfPkgNames);
        job.setJobType(JobTypeEnum.BACKUP);
        job.setName("jobName");
        job.setOwner("shmtest");
        job.setcollectionNames(collectionNames);
        job.setNeNames(fdnNames);
    }

    /**
     * @param job
     * @param listOfPkgNames
     * @param collections
     * @param collectionNames
     * @param fdnNames
     * @param fdns
     */
    private void jobAttributesForUpgrade(final JobInfo job, final Map<String, String> listOfPkgNames, final List<String> collectionNames, final List<Map<String, Object>> fdnNames,
            final Map<String, Object> fdns) {
        collectionNames.add("00383484");
        fdns.put("name", "ne1");
        fdnNames.add(fdns);
        listOfPkgNames.put("CPP", "CPPSoftwarePackage");
        listOfPkgNames.put("ECIM", "ECIMSoftwarePackage");
        job.setPackageNames(listOfPkgNames);
        job.setJobType(JobTypeEnum.UPGRADE);
        job.setName("jobName");
        job.setOwner("shmtest");
        job.setcollectionNames(collectionNames);
        job.setNeNames(fdnNames);
    }

    @Test
    public void testCreateShmJobFail() throws ZipException, Exception {
        final JobInfo job = new JobInfo();
        final Map<String, String> listOfPkgNames = new HashMap<String, String>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<Map<String, Object>> fdnNames = new ArrayList<Map<String, Object>>();
        final HashMap<String, Object> fdns = new HashMap<String, Object>();
        collectionNames.add("73739393");
        fdns.put("name", "ne1");
        fdnNames.add(fdns);
        listOfPkgNames.put("CPP", "CPPSoftwarePackage");
        listOfPkgNames.put("ECIM", "ECIMSoftwarePackage");
        job.setPackageNames(listOfPkgNames);
        job.setJobType(JobTypeEnum.UPGRADE);
        job.setName("jobName");
        job.setOwner("shmtest");
        job.setcollectionNames(collectionNames);
        job.setNeNames(fdnNames);
        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("CPP.CREATE_CV", "createcv1");
        configurations.add(map);
        job.setConfigurations(configurations);

        final Map<String, Object> response = new HashMap<String, Object>();
        response.put("errorCode", JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());

        when(shmJobHandler.populateAndPersistJobConfigurationData(Matchers.any(JobInfo.class))).thenReturn(response);
        when(jobFactory.createJob(Matchers.any(JobInfo.class))).thenReturn(job);
        when(builderFactory.selectJobBuilder(Matchers.any(String.class))).thenReturn(jobBuilder);

        assertEquals(JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription(), objectUnderTest.createShmJob(job).get("errorCode").toString());

    }

    @Test
    public void testCreateShmJobFailNull() throws ZipException, Exception {
        final JobInfo job = new JobInfo();
        final Map<String, String> listOfPkgNames = new HashMap<String, String>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<Map<String, Object>> fdnNames = new ArrayList<Map<String, Object>>();
        final HashMap<String, Object> fdns = new HashMap<String, Object>();
        collectionNames.add("73739393");
        fdns.put("name", "ne1");
        fdnNames.add(fdns);
        listOfPkgNames.put("CPP", "CPPSoftwarePackage");
        listOfPkgNames.put("ECIM", "ECIMSoftwarePackage");
        job.setPackageNames(listOfPkgNames);
        job.setJobType(JobTypeEnum.UPGRADE);
        job.setName("jobName");
        job.setOwner("shmtest");
        job.setcollectionNames(null);
        job.setNeNames(fdnNames);
        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("CPP.CREATE_CV", "createcv1");
        configurations.add(map);
        job.setConfigurations(configurations);

        final Map<String, Object> response = new HashMap<String, Object>();
        response.put("errorCode", JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());

        when(shmJobHandler.populateAndPersistJobConfigurationData(Matchers.any(JobInfo.class))).thenReturn(response);
        when(jobFactory.createJob(Matchers.any(JobInfo.class))).thenReturn(job);
        when(builderFactory.selectJobBuilder(Matchers.any(String.class))).thenReturn(jobBuilder);

        assertEquals(JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription(), objectUnderTest.createShmJob(job).get("errorCode").toString());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveJobLogsWithMainJobId() {
        final long mainJobId = 123;
        final JobLogRequest jobLogRequest = new JobLogRequest();
        jobLogRequest.setMainJobId(mainJobId);

        final long firstNeJobId = 2341;
        final String firstNodeName = "FirstNodeName";
        final long secondNeJobId = 2342;
        final String secondNodeName = "SecondNodeName";
        final String firstNodeType = "ERBS";
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.MAIN_JOB_ID, mainJobId)).thenReturn(restriction);

        final List<Object[]> neJobProjector = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[2];
        firstObject[0] = firstNeJobId;
        firstObject[1] = firstNodeName;
        neJobProjector.add(firstObject);
        final Object[] secondObject = new Object[2];
        secondObject[0] = secondNeJobId;
        secondObject[1] = secondNodeName;
        neJobProjector.add(secondObject);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(neJobProjector);

        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery);
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(3);

        final List<Long> eachBatchOfNeJobIds = new ArrayList<Long>();
        eachBatchOfNeJobIds.add(firstNeJobId);
        eachBatchOfNeJobIds.add(secondNeJobId);
        when(typeRestrictionBuilder.in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray())).thenReturn(restriction);

        final List<Object> batchedActivityJobs = new ArrayList<Object>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(batchedActivityJobs);

        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);

        final JobLogDetails firstActivityJobDetails = new JobLogDetails();
        when(jobLogMapper.mapJobAttributesToJobLogDetails(Matchers.anyMap())).thenReturn(firstActivityJobDetails);

        final List<JobLogDetails> firstNeJobList = new ArrayList<JobLogDetails>();
        firstNeJobList.add(firstActivityJobDetails);
        final NeJobLogDetails firstNeJobDetailsWithLogs = new NeJobLogDetails();
        when(jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(firstNeJobList, firstNodeName, firstNodeType)).thenReturn(firstNeJobDetailsWithLogs);

        final List<JobLogResponse> firstNeJobLogResponse = new ArrayList<JobLogResponse>();
        final JobLogResponse objJobLogResponse = new JobLogResponse();
        objJobLogResponse.setNeName("FirstNodeName");
        firstNeJobLogResponse.add(objJobLogResponse);
        when(jobLogMapper.getNEJobLogResponse(firstNeJobDetailsWithLogs)).thenReturn(firstNeJobLogResponse);

        when(shmJobUtil.getJobLogResponse(responseList, jobLogRequest)).thenReturn(jobOutput);

        final List<FilterDetails> objFilterDetails = new ArrayList<FilterDetails>();
        final FilterDetails objFilterDetail = new FilterDetails();
        objFilterDetail.setColumnName("neName");
        objFilterDetail.setFilterOperator("*");
        objFilterDetail.setFilterText("FirstNodeName");
        objFilterDetails.add(objFilterDetail);

        jobLogRequest.setFilterDetails(objFilterDetails);
        final Map<Long, List<Map<String, Object>>> batchedActivityJobs2Map = new HashMap<Long, List<Map<String, Object>>>();
        final ArrayList<Map<String, Object>> batchedActivityJobs2 = new ArrayList<Map<String, Object>>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(shmJobServiceRetryProxyMock.getActivityJobPoAttributes(queryExecutor, queryBuilder, eachBatchOfNeJobIds)).thenReturn(batchedActivityJobs2Map);
        attributes.put(ShmConstants.NE_JOB_ID, firstNeJobId);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2Map.put(firstNeJobId, batchedActivityJobs2);
        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);
        objectUnderTest.retrieveJobLogs(jobLogRequest);

        verify(jobLogMapper, times(2)).mapJobAttributesToJobLogDetails(Matchers.anyMap());
        verify(jobLogMapper, times(2)).mapNEJobLogDetailsFromJobLogDetails(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
        verify(jobLogMapper, times(2)).getNEJobLogResponse(Matchers.any(NeJobLogDetails.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveJobLogsWithNeJobIds() {
        final JobLogRequest jobLogRequest = new JobLogRequest();
        jobLogRequest.setNeJobIds("2341,2342");

        final long firstNeJobId = 2341;
        final String firstNodeName = "FirstNodeName";
        final long secondNeJobId = 2342;
        final String secondNodeName = "SecondNodeName";
        final String firstNodeType = "ERBS";
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);

        final Set<Long> neJobIdList = new HashSet<Long>();
        neJobIdList.add(firstNeJobId);
        neJobIdList.add(secondNeJobId);
        when(shmJobUtil.getNEJobIdList(jobLogRequest)).thenReturn(neJobIdList);
        final List<Long> poIdList = new ArrayList<>(neJobIdList);
        final List<PersistenceObject> persistenceObjectList = new ArrayList<>();
        persistenceObjectList.add(poMock);
        persistenceObjectList.add(secondPoMock);
        when(dpsReader.findPOsByPoIds(poIdList)).thenReturn(persistenceObjectList);
        when(dataBucket.findPoById(2341)).thenReturn(poMock);
        when(dataBucket.findPoById(2342)).thenReturn(secondPoMock);

        when(poMock.getPoId()).thenReturn(firstNeJobId);
        when(poMock.getAttribute(ShmConstants.NE_NAME)).thenReturn(firstNodeName);

        when(secondPoMock.getAttribute(ShmConstants.NE_NAME)).thenReturn(secondNodeName);
        when(secondPoMock.getPoId()).thenReturn(secondNeJobId);

        final List<Object[]> neJobProjector = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[2];
        firstObject[0] = firstNeJobId;
        firstObject[1] = firstNodeName;
        neJobProjector.add(firstObject);
        final Object[] secondObject = new Object[2];
        secondObject[0] = secondNeJobId;
        secondObject[1] = secondNodeName;
        neJobProjector.add(secondObject);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(neJobProjector);

        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(3);

        final List<Long> eachBatchOfNeJobIds = new ArrayList<Long>();
        eachBatchOfNeJobIds.add(firstNeJobId);
        eachBatchOfNeJobIds.add(secondNeJobId);
        when(typeRestrictionBuilder.in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray())).thenReturn(restriction);

        final List<Object> batchedActivityJobs = new ArrayList<Object>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(batchedActivityJobs);

        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);

        final JobLogDetails firstActivityJobDetails = new JobLogDetails();
        when(jobLogMapper.mapJobAttributesToJobLogDetails(Matchers.anyMap())).thenReturn(firstActivityJobDetails);

        final List<JobLogDetails> firstNeJobList = new ArrayList<JobLogDetails>();
        firstNeJobList.add(firstActivityJobDetails);
        final NeJobLogDetails firstNeJobDetailsWithLogs = new NeJobLogDetails();
        when(jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(firstNeJobList, firstNodeName, firstNodeType)).thenReturn(firstNeJobDetailsWithLogs);

        final List<JobLogResponse> firstNeJobLogResponse = new ArrayList<JobLogResponse>();
        when(jobLogMapper.getNEJobLogResponse(firstNeJobDetailsWithLogs)).thenReturn(firstNeJobLogResponse);

        when(shmJobUtil.getJobLogResponse(responseList, jobLogRequest)).thenReturn(jobOutput);
        final Map<Long, List<Map<String, Object>>> batchedActivityJobs2Map = new HashMap<Long, List<Map<String, Object>>>();
        final ArrayList<Map<String, Object>> batchedActivityJobs2 = new ArrayList<Map<String, Object>>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(shmJobServiceRetryProxyMock.getActivityJobPoAttributes(queryExecutor, queryBuilder, eachBatchOfNeJobIds)).thenReturn(batchedActivityJobs2Map);
        attributes.put(ShmConstants.NE_JOB_ID, firstNeJobId);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2Map.put(firstNeJobId, batchedActivityJobs2);
        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);
        objectUnderTest.retrieveJobLogs(jobLogRequest);

        verify(jobLogMapper, times(4)).mapJobAttributesToJobLogDetails(Matchers.anyMap());
        verify(jobLogMapper, times(2)).mapNEJobLogDetailsFromJobLogDetails(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
        verify(jobLogMapper, times(2)).getNEJobLogResponse(Matchers.any(NeJobLogDetails.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveJobLogsWhenThereIsNoActivityJobPresent() {
        final long mainJobId = 123;
        final JobLogRequest jobLogRequest = new JobLogRequest();
        jobLogRequest.setMainJobId(mainJobId);

        final long firstNeJobId = 2341;
        final String firstNodeName = "FirstNodeName";
        final long secondNeJobId = 2342;
        final String secondNodeName = "SecondNodeName";
        final String firstNodeType = "ERBS";
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.MAIN_JOB_ID, mainJobId)).thenReturn(restriction);

        final List<Object[]> neJobProjector = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[2];
        firstObject[0] = firstNeJobId;
        firstObject[1] = firstNodeName;
        neJobProjector.add(firstObject);
        final Object[] secondObject = new Object[2];
        secondObject[0] = secondNeJobId;
        secondObject[1] = secondNodeName;
        neJobProjector.add(secondObject);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(neJobProjector);

        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery);
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(3);

        final List<Long> eachBatchOfNeJobIds = new ArrayList<Long>();
        eachBatchOfNeJobIds.add(firstNeJobId);
        eachBatchOfNeJobIds.add(secondNeJobId);
        when(typeRestrictionBuilder.in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray())).thenReturn(restriction);

        final List<Object> batchedActivityJobs = new ArrayList<Object>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(batchedActivityJobs);

        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(12345l);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);

        final JobLogDetails firstActivityJobDetails = new JobLogDetails();
        when(jobLogMapper.mapJobAttributesToJobLogDetails(Matchers.anyMap())).thenReturn(firstActivityJobDetails);

        when(dataBucket.findPoById(12345l)).thenReturn(poMock);
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        final List<JobLogDetails> firstNeJobList = new ArrayList<JobLogDetails>();
        firstNeJobList.add(firstActivityJobDetails);
        final NeJobLogDetails firstNeJobDetailsWithLogs = new NeJobLogDetails();
        when(jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(firstNeJobList, firstNodeName, firstNodeType)).thenReturn(firstNeJobDetailsWithLogs);

        final List<JobLogResponse> firstNeJobLogResponse = new ArrayList<JobLogResponse>();
        when(jobLogMapper.getNEJobLogResponse(firstNeJobDetailsWithLogs)).thenReturn(firstNeJobLogResponse);

        when(shmJobUtil.getJobLogResponse(responseList, jobLogRequest)).thenReturn(jobOutput);
        final Map<Long, List<Map<String, Object>>> batchedActivityJobs2Map = new HashMap<Long, List<Map<String, Object>>>();
        final ArrayList<Map<String, Object>> batchedActivityJobs2 = new ArrayList<Map<String, Object>>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(shmJobServiceRetryProxyMock.getActivityJobPoAttributes(queryExecutor, queryBuilder, eachBatchOfNeJobIds)).thenReturn(batchedActivityJobs2Map);
        attributes.put(ShmConstants.NE_JOB_ID, firstNeJobId);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2Map.put(firstNeJobId, batchedActivityJobs2);
        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);
        objectUnderTest.retrieveJobLogs(jobLogRequest);

        verify(typeRestrictionBuilder, times(1)).equalTo(ShmConstants.MAIN_JOB_ID, mainJobId);
        //verify(jobLogMapper, times(1)).mapJobAttributesToJobLogDetails(Matchers.anyMap());
        verify(jobLogMapper, times(2)).mapNEJobLogDetailsFromJobLogDetails(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
        verify(jobLogMapper, times(2)).getNEJobLogResponse(Matchers.any(NeJobLogDetails.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveJobLogsWithNeJobIdsWhoseCorrespondingJobsAreNotPresent() {
        final JobLogRequest jobLogRequest = new JobLogRequest();
        jobLogRequest.setNeJobIds("2341,2342");

        final long firstNeJobId = 2341;
        final String firstNodeName = "FirstNodeName";
        final long secondNeJobId = 2342;
        final String secondNodeName = "SecondNodeName";
        final String firstNodeType = "ERBS";
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);

        final Set<Long> neJobIdList = new HashSet<Long>();
        neJobIdList.add(firstNeJobId);
        neJobIdList.add(secondNeJobId);
        when(shmJobUtil.getNEJobIdList(jobLogRequest)).thenReturn(neJobIdList);

        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(ObjectField.PO_ID, neJobIdList.toArray())).thenReturn(restriction);

        final List<PersistenceObject> poList = new ArrayList<>();
        poList.add(poMock);
        poList.add(secondPoMock);
        final List<Long> poIdsList = new ArrayList<>(neJobIdList);
        when(dpsReader.findPOsByPoIds(poIdsList)).thenReturn(poList);
        when(poMock.getAttribute(ShmConstants.NE_NAME)).thenReturn(firstNodeName);
        when(poMock.getPoId()).thenReturn(firstNeJobId);

        when(secondPoMock.getAttribute(ShmConstants.NE_NAME)).thenReturn(secondNodeName);
        when(secondPoMock.getPoId()).thenReturn(secondNeJobId);

        final List<Object[]> neJobProjector = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[2];
        firstObject[0] = firstNeJobId;
        firstObject[1] = firstNodeName;
        neJobProjector.add(firstObject);
        final Object[] secondObject = new Object[2];
        secondObject[0] = secondNeJobId;
        secondObject[1] = secondNodeName;
        neJobProjector.add(secondObject);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(neJobProjector);

        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(3);
        when(dataBucket.findPoById(2341)).thenReturn(poMock);
        when(dataBucket.findPoById(2342)).thenReturn(secondPoMock);
        final List<Long> eachBatchOfNeJobIds = new ArrayList<Long>();
        eachBatchOfNeJobIds.add(firstNeJobId);
        eachBatchOfNeJobIds.add(secondNeJobId);
        when(typeRestrictionBuilder.in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray())).thenReturn(restriction);

        final List<Object> batchedActivityJobs = new ArrayList<Object>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(batchedActivityJobs);

        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);

        final JobLogDetails firstActivityJobDetails = new JobLogDetails();
        when(jobLogMapper.mapJobAttributesToJobLogDetails(Matchers.anyMap())).thenReturn(firstActivityJobDetails);

        final List<JobLogDetails> firstNeJobList = new ArrayList<JobLogDetails>();
        firstNeJobList.add(firstActivityJobDetails);
        final NeJobLogDetails firstNeJobDetailsWithLogs = new NeJobLogDetails();
        when(jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(firstNeJobList, firstNodeName, firstNodeType)).thenReturn(firstNeJobDetailsWithLogs);

        final List<JobLogResponse> firstNeJobLogResponse = new ArrayList<JobLogResponse>();
        when(jobLogMapper.getNEJobLogResponse(firstNeJobDetailsWithLogs)).thenReturn(firstNeJobLogResponse);

        when(shmJobUtil.getJobLogResponse(responseList, jobLogRequest)).thenReturn(jobOutput);

        final Map<Long, List<Map<String, Object>>> batchedActivityJobs2Map = new HashMap<Long, List<Map<String, Object>>>();
        final ArrayList<Map<String, Object>> batchedActivityJobs2 = new ArrayList<Map<String, Object>>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(shmJobServiceRetryProxyMock.getActivityJobPoAttributes(queryExecutor, queryBuilder, eachBatchOfNeJobIds)).thenReturn(batchedActivityJobs2Map);
        attributes.put(ShmConstants.NE_JOB_ID, firstNeJobId);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2.add(attributes);
        batchedActivityJobs2Map.put(firstNeJobId, batchedActivityJobs2);
        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);
        objectUnderTest.retrieveJobLogs(jobLogRequest);

        verify(jobLogMapper, times(4)).mapJobAttributesToJobLogDetails(Matchers.anyMap());
        verify(jobLogMapper, times(2)).mapNEJobLogDetailsFromJobLogDetails(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
        verify(jobLogMapper, times(2)).getNEJobLogResponse(Matchers.any(NeJobLogDetails.class));
    }

    @Test(expected = ServerInternalException.class)
    public void testRetrieveJobLogsWhenMaxUsersReached() {
        doThrow(ServerInternalException.class).when(activeSessionsControllerMock).exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_JOB_LOGS);
        objectUnderTest.retrieveJobLogs(null);
        verify(activeSessionsControllerMock, never()).decrementAndGet(anyString());
        verify(shmJobUtil, never()).validateShmJobData(any(JobInput.class));
    }

    @Test
    public void shouldGetJobProgressDetailsTest() {
        final List<Long> poIds = new ArrayList<Long>();

        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> jobs = new ArrayList<com.ericsson.oss.services.shm.jobs.common.modelentities.Job>();
        jobs.add(new com.ericsson.oss.services.shm.jobs.common.modelentities.Job());
        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> jobProgressDetails = objectUnderTest.getJobProgressDetails(poIds);

        assertEquals(poIds.size(), jobProgressDetails.size());
    }

    @Test
    public void shouldGetZeroJobProgressDetailsWhenPoIdListIsEmptyTest() {
        final List<Long> poIds = new ArrayList<Long>();

        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> jobProgressDetails = objectUnderTest.getJobProgressDetails(poIds);

        assertEquals(poIds.size(), jobProgressDetails.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getJobReportDetails_returnsNotNull() {
        jobInput = Mockito.mock(NeJobInput.class);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        final JobReportData reportData = objectUnderTest.getJobReportDetails(jobInput);
        assertNotNull(reportData);
        verify(shmJobsMapper, never()).getJobReportRefined(Matchers.anyMap(), eq("RESTORE"), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getJobReportDetails_noMainJobFound() {
        jobInput = Mockito.mock(NeJobInput.class);
        when(jobInput.getJobIdsList()).thenReturn(Arrays.asList(123l));
        when(jobsReader.getMainJob(123L)).thenReturn(null);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        final JobReportData reportData = objectUnderTest.getJobReportDetails(jobInput);

        assertNotNull(reportData);
        verify(shmJobsMapper, never()).getJobReportRefined(Matchers.anyMap(), eq("RESTORE"), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getJobReportDetails_returnsMainJobDetails_whenOnlyMainJobFound() {
        jobInput = Mockito.mock(NeJobInput.class);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(jobInput.getJobIdsList()).thenReturn(Arrays.asList(123l));
        when(dataBucket.findPoById(123l)).thenReturn(poMock);
        when(poMock.getPoId()).thenReturn(111l, 222l, 333l, 444l);
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(attributesMapMock.get(ShmConstants.JOBTEMPLATEID)).thenReturn(555l);
        when(attributesMapMock.get(ShmConstants.MAINJOBID)).thenReturn(123l);
        when(dataBucket.findPoById(555l)).thenReturn(poMock);
        when(jobsReader.getMainJob(123l)).thenReturn(attributesMapMock);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(ShmConstants.MAIN_JOB_ID, 111l)).thenReturn(restriction);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(getPersistanceObject("dummy"));
        when(queryExecutor.getResultList(typeQuery)).thenReturn(getPersistanceObject("dummy"));
        final Object[] element = {444l, 4442l, "NeName", new Date().toString(), new Date().toString(), "10", "Status"};
        final List<Object[]> datbaseEntries = new ArrayList<Object[]>();
        datbaseEntries.add(element);
        when(jobInput.getOffset()).thenReturn(1);
        when(jobInput.getLimit()).thenReturn(1);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(300);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery1);
        when(typeQuery1.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder1);
        when(typeRestrictionBuilder1.in(ShmConstants.ACTIVITY_NE_JOB_ID, 111l)).thenReturn(restriction1);
        final Object[] element1 = {444l, 4442l, "ActivityName", "ActivityResult"};
        final List<Object[]> datbaseEntries1 = new ArrayList<Object[]>();
        datbaseEntries1.add(element1);

        when(shmJobsMapper.getMainJobDeatils(attributesMapMock, attributesMapMock)).thenReturn(jobReportDetails);
        when(shmJobsMapper.getJobReportRefined(Matchers.anyMap(), Matchers.anyString(), Matchers.anyMap())).thenReturn(neDetails);
        when(jobReportDetails.getJobType()).thenReturn("RESTORE");
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(10);
        final JobReportData reportData = objectUnderTest.getJobReportDetails(jobInput);
        assertNotNull(reportData);
        verify(shmJobsMapper, times(1)).getJobReportRefined(Matchers.anyMap(), eq("RESTORE"), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getJobReportDetails_returnsMainJobDetails_License() {
        jobInput = Mockito.mock(NeJobInput.class);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(jobInput.getJobIdsList()).thenReturn(Arrays.asList(123l));
        when(dataBucket.findPoById(123l)).thenReturn(poMock);
        when(poMock.getPoId()).thenReturn(111l, 222l, 333l, 444l);
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(attributesMapMock.get(ShmConstants.JOBTEMPLATEID)).thenReturn(555l);
        when(attributesMapMock.get(ShmConstants.MAINJOBID)).thenReturn(123l);
        when(dataBucket.findPoById(555l)).thenReturn(poMock);
        when(jobsReader.getMainJob(123L)).thenReturn(attributesMapMock);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(ShmConstants.MAIN_JOB_ID, 111l)).thenReturn(restriction);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(getPersistanceObject("dummy"));
        final Object[] element = {444l, 4442l, "NeName", new Date().toString(), new Date().toString(), "10", "Status"};
        final List<Object[]> datbaseEntries = new ArrayList<Object[]>();
        datbaseEntries.add(element);
        when(jobInput.getOffset()).thenReturn(1);
        when(jobInput.getLimit()).thenReturn(1);

        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(100);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery1);
        when(typeQuery1.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder1);
        when(typeRestrictionBuilder1.in(ShmConstants.ACTIVITY_NE_JOB_ID, 111l)).thenReturn(restriction1);
        final Object[] element1 = {444l, 4442l, "ActivityName", "ActivityResult"};
        final List<Object[]> datbaseEntries1 = new ArrayList<Object[]>();
        datbaseEntries1.add(element1);
        when(shmJobsMapper.getMainJobDeatils(attributesMapMock, attributesMapMock)).thenReturn(jobReportDetails);
        final NeDetails neDetails = new NeDetails();
        when(shmJobsMapper.getJobReportRefined(Matchers.anyMap(), Matchers.anyString(), Matchers.anyMap())).thenReturn(neDetails);
        when(jobReportDetails.getJobType()).thenReturn("LICENSE");
        final JobReportData reportData = objectUnderTest.getJobReportDetails(jobInput);
        assertNotNull(reportData);
        verify(shmJobsMapper, times(1)).getJobReportRefined(Matchers.anyMap(), eq("LICENSE"), anyMap());
    }

    private List<Object> getPersistanceObject(final String nodeName) {
        final List<Object> persistenceObjects = new ArrayList<Object>();
        final PersistenceObject persistenceObject = new AbstractPersistenceObject(null, null, null, null) {

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getType() {
                return type;
            }

            @Override
            public String getNamespace() {
                return namespace;
            }

            @Override
            public Map<String, Object> getAllAttributes() {
                return attributesMap;
            }

            @Override
            public Map<String, Object> getAttributes(final Collection<String> arg0) {
                return attributes;
            }

            @Override
            public PersistenceObject getTarget() {
                return null;
            }

            @Override
            public void setTarget(final PersistenceObject arg0) {
            }

            @Override
            public int getAssociatedObjectCount(final String arg0) throws NotDefinedInModelException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Map<String, Object> readAttributesFromDelegate(final String... arg0) throws DpsIllegalStateException {
                // TODO Auto-generated method stub
                return null;
            }
        };
        persistenceObjects.add(persistenceObject);
        return persistenceObjects;
    }

    @Test
    public void testgetJobConfiguration_returnsEmptyList_whenRequestNull() throws JobConfigurationException {
        final List<JobTemplate> jobTemplatesList = objectUnderTest.getJobTemplates(null);
        assertEquals(0, jobTemplatesList.size());
    }

    @Test
    public void testgetJobConfiguration_verifyMapperCall() throws JobConfigurationException {

        when(jobConfigQueryMock.getPoIds()).thenReturn(Arrays.asList(1234l));
        when(dpsReader.findPOByPoId(1234l)).thenReturn(poMock);
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        final List<JobTemplate> jobTemplatesList = objectUnderTest.getJobTemplates(jobConfigQueryMock);
        assertNotNull(jobTemplatesList);
        assertEquals(1, jobTemplatesList.size());
        verify(shmJobsMapper, atLeastOnce()).getJobTemplateDetails(attributesMapMock, 1234l);
    }

    @Test
    public void testgetJobConfiguration_returnsEmptyList_whenNoDataFound() throws JobConfigurationException {
        when(jobConfigQueryMock.getPoIds()).thenReturn(Arrays.asList(1234l));
        when(dpsReader.findPOByPoId(1234l)).thenReturn(null);
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        final List<JobTemplate> jobTemplatesList = objectUnderTest.getJobTemplates(jobConfigQueryMock);
        assertNotNull(jobTemplatesList);
        assertEquals(0, jobTemplatesList.size());
        verify(shmJobsMapper, times(0)).getJobTemplateDetails(attributesMapMock, 1234l);
    }

    @Test
    public void testAddJobComment() {
        final CommentInfo commentInfo = new CommentInfo();
        final String jobId = "123456789";
        commentInfo.setComment("This is a job comment");
        commentInfo.setJobId(jobId);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(JobModelConstants.JOB_COMMENT, "This is a job comment");
        final long mainJobId = Long.parseLong(jobId);
        when(userContextBean.getLoggedInUserName()).thenReturn("userName");
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> commentList = new ArrayList<Map<String, Object>>();
        mainJobAttributes.put(ShmConstants.COMMENT, commentList);
        when(poMock.getAllAttributes()).thenReturn(mainJobAttributes);
        when(dpsReader.findPOByPoId(mainJobId)).thenReturn(poMock);
        persistenceAdapter.update(mainJobId, attributes);
        objectUnderTest.addJobComment(commentInfo);
    }

    @Test
    public void testAddJobCommentWhenJobIdIsNull() {
        final CommentInfo commentInfo = new CommentInfo();
        final String jobId = null;
        commentInfo.setComment("This is a job comment");
        commentInfo.setJobId(jobId);
        objectUnderTest.addJobComment(commentInfo);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_exportJobLogs_withNoInput() {
        final String response = objectUnderTest.exportJobLogs(new ExportJobLogRequest());
        assertNotNull(response);
        assertEquals("Logs cannot be retrieved as the neJobs list is null", response);
        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
        verify(jobLogMapper, never()).mapNEJobLogDetailsFromJobLogDetails(anyList(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_exportJobLogs_returnsEmptyMsg() {
        when(shmJobUtil.getJobLogResponse(anyList(), any(JobLogRequest.class))).thenReturn(joboutput);
        getJobLog();
        final String response = objectUnderTest.exportJobLogs(exportJobLogRequestMock);
        assertNotNull(response);
        assertEquals("", response);
        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_exportJobLogs_returnsNull() {
        when(shmJobUtil.getJobLogResponse(anyList(), any(JobLogRequest.class))).thenReturn(joboutput);
        getJobLog();
        when(joboutput.getResult()).thenReturn(Arrays.asList(jobLogResponseMock));
        final String response = objectUnderTest.exportJobLogs(exportJobLogRequestMock);
        assertNotNull(response);
        assertEquals("null\tnull\tnull\tnull\tnull\tnull\tnull\tnull\t\r\n", response);

        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_exportJobLogs_returnsEmpty_whenNumberFormatException() {
        when(exportJobLogRequestMock.getJobName()).thenReturn("dummyJob");
        when(exportJobLogRequestMock.getJobType()).thenReturn("UPGRADE");
        when(shmJobUtil.getJobLogResponse(anyList(), any(JobLogRequest.class))).thenReturn(joboutput);
        when(joboutput.getResult()).thenReturn(Arrays.asList(jobLogResponseMock));
        when(jobLogResponseMock.getNeName()).thenReturn("dummyNE");
        when(jobLogResponseMock.getEntryTime()).thenReturn("kjhui");
        getJobLog();
        final String response = objectUnderTest.exportJobLogs(exportJobLogRequestMock);
        assertNull(response);

        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_exportJobLogs_returnsLogHeaders_noLogs() {
        when(exportJobLogRequestMock.getJobName()).thenReturn("dummyJob");
        when(exportJobLogRequestMock.getJobType()).thenReturn("UPGRADE");
        when(shmJobUtil.getJobLogResponse(anyList(), any(JobLogRequest.class))).thenReturn(joboutput);
        when(joboutput.getResult()).thenReturn(Arrays.asList(jobLogResponseMock));
        when(jobLogResponseMock.getEntryTime()).thenReturn(null);
        when(jobLogResponseMock.getNeName()).thenReturn("dummyNE");
        when(jobLogResponseMock.getActivityName()).thenReturn("dummyActivity");
        when(jobLogResponseMock.getMessage()).thenReturn("Message1");
        when(jobLogResponseMock.getLogLevel()).thenReturn("INFO");
        when(jobLogResponseMock.getNodeType()).thenReturn("ERBS");
        getJobLog();
        final String response = objectUnderTest.exportJobLogs(exportJobLogRequestMock);
        assertNotNull(response);
        assertEquals("dummyJob\tUPGRADE\tINFO\tdummyNE\tERBS\tdummyActivity\tnull\tMessage1\t\r\n", response);

        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_constructCsvDataOfMainJobLogs() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.findPoById(12345L)).thenReturn(templateJobPo);
        when(exportJobLogRequestMock.getJobName()).thenReturn("dummyJob");
        when(exportJobLogRequestMock.getJobType()).thenReturn("UPGRADE");
        when(exportJobLogRequestMock.getMainJobId()).thenReturn(12345L);
        when(shmJobUtil.getJobLogResponse(anyList(), any(JobLogRequest.class))).thenReturn(joboutput);
        when(joboutput.getResult()).thenReturn(Arrays.asList(jobLogResponseMock));
        when(jobLogResponseMock.getEntryTime()).thenReturn(null);
        when(jobLogResponseMock.getNeName()).thenReturn("dummyNE");
        when(jobLogResponseMock.getActivityName()).thenReturn("dummyActivity");
        when(jobLogResponseMock.getMessage()).thenReturn("Message1");
        when(jobLogResponseMock.getLogLevel()).thenReturn("INFO");
        when(jobLogResponseMock.getNodeType()).thenReturn("ERBS");
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> commentList = new ArrayList<Map<String, Object>>();
        mainJobAttributes.put(ShmConstants.LOG, commentList);
        when(poMock.getAllAttributes()).thenReturn(mainJobAttributes);
        final JobLogDetails mainJobDetails = new JobLogDetails();
        when(jobLogMapper.mapJobAttributesToJobLogDetails(Matchers.anyMap())).thenReturn(mainJobDetails);
        final List<JobLogDetails> mainJobList = new ArrayList<JobLogDetails>();
        mainJobList.add(mainJobDetails);
        final MainJobLogDetails mainJobLogDetails = new MainJobLogDetails();
        when(jobLogMapper.mapMainJobLogDetailsFromJobLogDetails(mainJobList)).thenReturn(mainJobLogDetails);
        when(jobLogMapper.getMainJobLogResponse(mainJobLogDetails)).thenReturn(Arrays.asList(jobLogResponseMock));
        final String response = objectUnderTest.exportMainJobLogs(exportJobLogRequestMock);
        assertNotNull(response);
        assertEquals("dummyJob\tUPGRADE\tINFO\tdummyNE\tERBS\tdummyActivity\tnull\tMessage1\t\r\n", response);

        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_constructCsvDataOfMainJobLogs_withNoInput() {
        final String response = objectUnderTest.exportMainJobLogs(new ExportJobLogRequest());
        assertNotNull(response);
        assertEquals("", response);
        verify(dpsReader, never()).findPOsByPoIds(anyList());
        verify(dpsReader, never()).findPOs(anyString(), anyString(), anyMap());
        verify(jobLogMapper, never()).mapNEJobLogDetailsFromJobLogDetails(anyList(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    private void getJobLog() {
        final long mainJobId = 123;
        final JobLogRequest jobLogRequest = new JobLogRequest();
        jobLogRequest.setMainJobId(mainJobId);

        final long firstNeJobId = 2341;
        final String firstNodeName = "FirstNodeName";
        final long secondNeJobId = 2342;
        final String secondNodeName = "SecondNodeName";
        final String firstNodeType = "ERBS";
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.MAIN_JOB_ID, mainJobId)).thenReturn(restriction);

        final List<Object[]> neJobProjector = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[2];
        firstObject[0] = firstNeJobId;
        firstObject[1] = firstNodeName;
        neJobProjector.add(firstObject);
        final Object[] secondObject = new Object[2];
        secondObject[0] = secondNeJobId;
        secondObject[1] = secondNodeName;
        neJobProjector.add(secondObject);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class))).thenReturn(neJobProjector);

        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE)).thenReturn(typeQuery);
        when(jobParameterChangeListener.getJobBatchSize()).thenReturn(3);
        when(batchParameterChangeListener.getJobDetailsQueryBatchSize()).thenReturn(3);

        final List<Long> eachBatchOfNeJobIds = new ArrayList<Long>();
        eachBatchOfNeJobIds.add(firstNeJobId);
        eachBatchOfNeJobIds.add(secondNeJobId);
        when(typeRestrictionBuilder.in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray())).thenReturn(restriction);

        final List<Object> batchedActivityJobs = new ArrayList<Object>();
        batchedActivityJobs.add(firstActivityJob);
        batchedActivityJobs.add(secondActivityJob);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(batchedActivityJobs);

        when(firstActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(firstNeJobId);
        when(secondActivityJob.getAttribute(ShmConstants.NE_JOB_ID)).thenReturn(secondNeJobId);

        final JobLogDetails firstActivityJobDetails = new JobLogDetails();
        when(jobLogMapper.mapJobAttributesToJobLogDetails(Matchers.anyMap())).thenReturn(firstActivityJobDetails);

        final List<JobLogDetails> firstNeJobList = new ArrayList<JobLogDetails>();
        firstNeJobList.add(firstActivityJobDetails);
        final NeJobLogDetails firstNeJobDetailsWithLogs = new NeJobLogDetails();
        when(jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(firstNeJobList, firstNodeName, firstNodeType)).thenReturn(firstNeJobDetailsWithLogs);

        final List<JobLogResponse> firstNeJobLogResponse = new ArrayList<JobLogResponse>();
        when(jobLogMapper.getNEJobLogResponse(firstNeJobDetailsWithLogs)).thenReturn(firstNeJobLogResponse);

        when(shmJobUtil.getJobLogResponse(responseList, jobLogRequest)).thenReturn(jobOutput);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_exportJobLogs_returnsLogHeaders_noActivityLogs() {
        when(exportJobLogRequestMock.getJobName()).thenReturn("dummyJob");
        when(exportJobLogRequestMock.getJobType()).thenReturn("UPGRADE");
        when(shmJobUtil.getJobLogResponse(anyList(), any(JobLogRequest.class))).thenReturn(joboutput);
        when(joboutput.getResult()).thenReturn(Arrays.asList(jobLogResponseMock));
        when(jobLogResponseMock.getNeName()).thenReturn("dummyNE");
        when(jobLogResponseMock.getActivityName()).thenReturn("dummyActivity");
        when(jobLogResponseMock.getMessage()).thenReturn("Message1");
        when(jobLogResponseMock.getEntryTime()).thenReturn(null);
        when(jobLogResponseMock.getLogLevel()).thenReturn("INFO");
        when(jobLogResponseMock.getNodeType()).thenReturn("ERBS");
        final Set<Long> neJobIdList = new HashSet<Long>();
        neJobIdList.add(123l);
        when(shmJobUtil.getNEJobIdList(any(JobLogRequest.class))).thenReturn(neJobIdList);
        getJobLog();
        when(dpsReader.findPOsByPoIds(Arrays.asList(123l))).thenReturn(Arrays.asList(poMock));

        final String response = objectUnderTest.exportJobLogs(exportJobLogRequestMock);
        assertNotNull(response);
        assertEquals("dummyJob\tUPGRADE\tINFO\tdummyNE\tERBS\tdummyActivity\tnull\tMessage1\t\r\n", response);

        verify(dpsReader, times(1)).findPOsByPoIds(Matchers.anyList());
        verify(dpsReader, never()).findPOs(eq(ShmConstants.NAMESPACE), eq(ShmConstants.ACTIVITYJOB_TYPE), anyMap());
    }

    @Test
    public void test_getJobTemplateByNeJobId() {
        when(dpsReader.findPOByPoId(anyLong())).thenReturn(poMock);
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(attributesMapMock.get("mainJobId")).thenReturn(1223l);
        when(attributesMapMock.get("mainJobId")).thenReturn(1223l);
        when(attributesMapMock.get("templateJobId")).thenReturn(654l);

        when(shmJobsMapper.getJobTemplateDetails(attributesMapMock, 654l)).thenReturn(jobTemplate);
        final JobTemplate response = objectUnderTest.getJobTemplateByNeJobId(123456l);
        assertEquals(jobTemplate, response);

    }

    @Test
    public void test_getJobProgressDetails() {

        when(dpsReader.findPOsByPoIds(Arrays.asList(99999l))).thenReturn(Arrays.asList(poMock));
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(attributesMapMock.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(65.00);

        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> response = objectUnderTest.getJobProgressDetails(Arrays.asList(99999l));
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(65.00, response.get(0).getProgressPercentage(), 0.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getJobTemplate() {
        when(dpsReader.findPOs(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE), anyMap())).thenReturn(Arrays.asList(poMock));
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(shmJobsMapper.getJobTemplateDetails(attributesMapMock, poMock.getPoId())).thenReturn(jobTemplate);
        final JobTemplate response = objectUnderTest.getJobTemplate("name");
        assertEquals(jobTemplate, response);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getStartTimeForImmediateAndManualJobs() {
        when(dpsReader.findPOs(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOB), anyMap())).thenReturn(Arrays.asList(poMock));
        final Map<String, Object> map = new HashMap<>();
        final Date date = new Date();
        map.put(ShmConstants.STARTTIME, date);
        when(poMock.getAllAttributes()).thenReturn(map);
        final String response = objectUnderTest.getJobStartTime(99999l);
        assertNotNull(response);

    }

    @Test
    public void test_cancelJobs() {
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123l);
        Mockito.doNothing().when(jobExecutorLocal).cancelJobs(jobIds, "shmuser");
        final Map<String, Object> Response = objectUnderTest.cancelJobs(jobIds);
        assertNotNull(Response);
        assertEquals("success", Response.get(ShmConstants.STATUS));

    }

    @Test
    public void testDeleteJobs() {
        final String message = "3 job(s) submitted for deletion.1 job(s) not found.1 job(s) is/are still active.";
        final Map<String, Object> expectedOutput = new HashMap<String, Object>();
        expectedOutput.put(ShmConstants.MESSAGE, message);
        expectedOutput.put(ShmConstants.STATUS, ShmConstants.ERROR);

        final long firstJobId = 1;
        final long secondJobId = 2;
        final long fourthJobId = 4;
        final long fifthJobId = 5;

        final long firstAndFourthJobTemplateId = 11;
        final long secondJobTemplateId = 12;
        final long fifthJobTemplateId = 15;

        final String firstJobState = "COMPLETED";
        final String secondJobState = "RUNNING";
        final String fourthJobState = "SYSTEM_CANCELLED";
        final String fifthJobState = "COMPLETED";

        final int executionIndex = 1;
        final String firstJobName = "firstJobName";
        final String secondJobName = "secondJobName";
        final String fourthJobName = "fourthJobName";
        final String fifthJobName = "fifthJobName";

        final String jobType = "UPGRADE";

        final String firstWfsId = "firstWfsId";
        final String secondWfsId = "secondWfsId";
        final String fourthWfsId = "fourthWfsId";
        final String fifthWfsId = "fifthWfsId";

        final Map<String, Object> fourthJobConfiguration = new HashMap<String, Object>();
        final Map<String, Object> fourthMainSchedule = new HashMap<String, Object>();
        fourthMainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.SCHEDULED.toString());
        final List<Map<String, Object>> fourthScheduleParameters = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatType = new HashMap<String, Object>();
        repeatType.put(ShmConstants.NAME, ShmConstants.REPEAT_TYPE);
        repeatType.put(ShmConstants.VALUE, "DAILY");
        fourthScheduleParameters.add(repeatType);
        fourthMainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, fourthScheduleParameters);
        fourthJobConfiguration.put(ShmConstants.MAIN_SCHEDULE, fourthMainSchedule);

        final List<String> deleteJobRequest = new ArrayList<String>();
        deleteJobRequest.add(Long.toString(firstJobId));
        deleteJobRequest.add(Long.toString(secondJobId));
        deleteJobRequest.add(Long.toString(fourthJobId));
        deleteJobRequest.add(Long.toString(fifthJobId));

        final Set<Long> poIdSet = new HashSet<Long>();

        // Converting Job Ids from String to Long.
        for (final String poId : deleteJobRequest) {
            poIdSet.add(Long.valueOf(poId));
        }

        final List<Long> templateJobIds = new ArrayList<Long>();
        templateJobIds.add(firstAndFourthJobTemplateId);
        templateJobIds.add(secondJobTemplateId);
        templateJobIds.add(fifthJobTemplateId);

        final List<Object[]> jobProjection = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[4];
        firstObject[0] = firstJobId;
        firstObject[1] = firstAndFourthJobTemplateId;
        firstObject[2] = firstJobState;
        firstObject[3] = executionIndex;
        final Object[] secondObject = new Object[4];
        secondObject[0] = secondJobId;
        secondObject[1] = secondJobTemplateId;
        secondObject[2] = secondJobState;
        secondObject[3] = executionIndex;
        final Object[] fourthObject = new Object[4];
        fourthObject[0] = fourthJobId;
        fourthObject[1] = firstAndFourthJobTemplateId;
        fourthObject[2] = fourthJobState;
        fourthObject[3] = executionIndex;
        final Object[] fifthObject = new Object[4];
        fifthObject[0] = fifthJobId;
        fifthObject[1] = fifthJobTemplateId;
        fifthObject[2] = fifthJobState;
        fifthObject[3] = executionIndex;

        jobProjection.add(firstObject);
        jobProjection.add(secondObject);
        jobProjection.add(fourthObject);
        jobProjection.add(fifthObject);

        final List<Map<String, Object>> jobsDetailForDeletion = new ArrayList<Map<String, Object>>();
        final Map<String, Object> firstJobTemplateAttributes = new HashMap<String, Object>();
        firstJobTemplateAttributes.put(ShmConstants.NAME, firstJobName);
        firstJobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        firstJobTemplateAttributes.put(ShmConstants.WFS_ID, firstWfsId);
        firstJobTemplateAttributes.put(ShmConstants.JOBTEMPLATEID, firstAndFourthJobTemplateId);
        jobsDetailForDeletion.add(firstJobTemplateAttributes);

        final Map<String, Object> secondJobTemplateAttributes = new HashMap<String, Object>();
        secondJobTemplateAttributes.put(ShmConstants.NAME, secondJobName);
        secondJobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        secondJobTemplateAttributes.put(ShmConstants.WFS_ID, secondWfsId);
        secondJobTemplateAttributes.put(ShmConstants.JOBTEMPLATEID, secondJobTemplateId);
        jobsDetailForDeletion.add(secondJobTemplateAttributes);

        final Map<String, Object> fourthJobTemplateAttributes = new HashMap<String, Object>();
        fourthJobTemplateAttributes.put(ShmConstants.NAME, fourthJobName);
        fourthJobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        fourthJobTemplateAttributes.put(ShmConstants.WFS_ID, fourthWfsId);
        fourthJobTemplateAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, fourthJobConfiguration);
        fourthJobTemplateAttributes.put(ShmConstants.JOBTEMPLATEID, firstAndFourthJobTemplateId);
        jobsDetailForDeletion.add(fourthJobTemplateAttributes);

        final Map<String, Object> fifthJobTemplateAttributes = new HashMap<String, Object>();
        fifthJobTemplateAttributes.put(ShmConstants.NAME, fifthJobName);
        fifthJobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        fifthJobTemplateAttributes.put(ShmConstants.WFS_ID, fifthWfsId);
        fifthJobTemplateAttributes.put(ShmConstants.JOBTEMPLATEID, fifthJobTemplateId);
        jobsDetailForDeletion.add(fifthJobTemplateAttributes);

        final Job firstJobsDeletionAttributes = new Job();
        firstJobsDeletionAttributes.setExecutionIndex(executionIndex);
        firstJobsDeletionAttributes.setJobName(firstJobName);
        firstJobsDeletionAttributes.setJobState(firstJobState);
        firstJobsDeletionAttributes.setJobTemplateId(firstAndFourthJobTemplateId);
        firstJobsDeletionAttributes.setJobType(jobType);
        firstJobsDeletionAttributes.setMainJobId(firstJobId);
        firstJobsDeletionAttributes.setWfsId(firstWfsId);

        final Job secondJobsDeletionAttributes = new Job();
        secondJobsDeletionAttributes.setExecutionIndex(executionIndex);
        secondJobsDeletionAttributes.setJobName(secondJobName);
        secondJobsDeletionAttributes.setJobState(secondJobState);
        secondJobsDeletionAttributes.setJobTemplateId(secondJobTemplateId);
        secondJobsDeletionAttributes.setJobType(jobType);
        secondJobsDeletionAttributes.setMainJobId(secondJobId);
        secondJobsDeletionAttributes.setWfsId(secondWfsId);

        final Job fourthJobsDeletionAttributes = new Job();
        fourthJobsDeletionAttributes.setExecutionIndex(executionIndex);
        fourthJobsDeletionAttributes.setJobConfigurationDetails(fourthJobConfiguration);
        fourthJobsDeletionAttributes.setJobName(fourthJobName);
        fourthJobsDeletionAttributes.setJobState(fourthJobState);
        fourthJobsDeletionAttributes.setJobTemplateId(firstAndFourthJobTemplateId);
        fourthJobsDeletionAttributes.setJobType(jobType);
        fourthJobsDeletionAttributes.setMainJobId(fourthJobId);
        fourthJobsDeletionAttributes.setWfsId(fourthWfsId);

        final List<WorkflowObject> batchWorkFlowList = new ArrayList<WorkflowObject>();
        when(localWorkflowQueryServiceProxy.executeWorkflowQuery(Matchers.any(com.ericsson.oss.services.wfs.api.query.Query.class))).thenReturn(batchWorkFlowList);

        final Job fifthJobsDeletionAttributes = new Job();
        fifthJobsDeletionAttributes.setExecutionIndex(executionIndex);
        fifthJobsDeletionAttributes.setJobName(fifthJobName);
        fifthJobsDeletionAttributes.setJobState(fifthJobState);
        fifthJobsDeletionAttributes.setJobTemplateId(fifthJobTemplateId);
        fifthJobsDeletionAttributes.setJobType(jobType);
        fifthJobsDeletionAttributes.setMainJobId(fifthJobId);
        fifthJobsDeletionAttributes.setWfsId(fifthWfsId);
        final List<Job> fifthJob = new ArrayList<Job>();
        fifthJob.add(fifthJobsDeletionAttributes);

        final Map<Long, List<Job>> jobDetails = new HashMap<Long, List<Job>>();
        final List<Job> firstAndFourthJobs = new ArrayList<Job>();
        firstAndFourthJobs.add(firstJobsDeletionAttributes);
        firstAndFourthJobs.add(fourthJobsDeletionAttributes);

        jobDetails.put(firstAndFourthJobTemplateId, firstAndFourthJobs);
        jobDetails.put(fifthJobTemplateId, fifthJob);
        final List<Job> allJobsList = new ArrayList<Job>();
        allJobsList.add(firstJobsDeletionAttributes);
        allJobsList.add(secondJobsDeletionAttributes);
        allJobsList.add(fourthJobsDeletionAttributes);
        allJobsList.add(fifthJobsDeletionAttributes);

        final Set<Long> poIdSets = new HashSet<Long>();

        // Converting Job Ids from String to Long.
        for (final String poId : deleteJobRequest) {
            poIdSets.add(Long.valueOf(poId));
        }
        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(5);
        jobsDeletionReport.incrementJobsNotFoundCount(1);
        jobsDeletionReport.incrementJobsDeletedCount();
        jobsDeletionReport.setJobPoIdsFailedForDeletion(poIdSet);
        final List<Object[]> persistenceObjectList = new ArrayList<>();
        when(jobsDeletionService.retrieveJobDetails(Matchers.anySet())).thenReturn(persistenceObjectList);
        when(shmJobServiceHelper.isJobActive(JobState.RUNNING.toString())).thenReturn(true);
        when(shmJobServiceHelper.deleteJobHirerachy(Matchers.any(Job.class))).thenReturn(1);
        when(jobsDeletionService.fetchJobTemplateAttributes(templateJobIds)).thenReturn(jobsDetailForDeletion);
        when(shmJobServiceHelper.fetchJobDetails(Matchers.anySet(), Matchers.any(JobsDeletionReport.class))).thenReturn(jobDetails);
        when(shmJobServiceHelper.extractListOfJobs(Matchers.anyMap())).thenReturn(allJobsList);
        when(jobsDeletionService.deleteJobHierarchyWithoutJobTemplate(Matchers.any(Job.class))).thenReturn(1);
        when(jobsDeletionService.deleteJobHierarchyWithJobTemplate(Matchers.any(Job.class))).thenReturn(1);
        jobsDeletionReport.incrementJobsDeletedCount();
        jobsDeletionReport.incrementJobsDeletedCount();
        jobsDeletionReport.incrementActiveJobsCount();
        final List<Long> poIdList = new ArrayList<Long>(poIdSets);
        when(shmJobServiceHelper.updateJobStatusAndGetJobDeletionReport(poIdList)).thenReturn(jobsDeletionReport);
        doThrow(RuntimeException.class).when(jobsDeletionService).deleteJobHierarchyWithoutJobTemplate(Matchers.any(Job.class));

        doNothing().when(systemRecorder).recordEvent(SHMEvents.DELETE_JOBS, EventLevel.COARSE, "", "", message);
        final Map<String, String> actualResponse = objectUnderTest.deleteShmJobs(deleteJobRequest);
        assertNotNull(actualResponse);
        assertEquals(expectedOutput.get(ShmConstants.MESSAGE), actualResponse.get(ShmConstants.MESSAGE));
        assertEquals(expectedOutput.get(ShmConstants.STATUS), actualResponse.get(ShmConstants.STATUS));

        verify(eventSender, never()).fire(Matchers.any(JobWebPushEvent.class));
    }

    @Test
    public void testDeleteShmJobsNoJobCategoryNoWebpush() {
        when(shmJobServiceHelper.updateJobStatusAndGetJobDeletionReport(Arrays.asList(123l))).thenReturn(jobsDeletionReport);
        objectUnderTest.deleteShmJobs(Arrays.asList("123"));

    }

    @Test
    public void testDeleteShmJobsShmJobCategorySendWebpush() {
        when(shmJobServiceHelper.updateJobStatusAndGetJobDeletionReport(Arrays.asList(123l))).thenReturn(jobsDeletionReport);
        when(jobConfigurationService.getJobCategory(123l)).thenReturn("SHM");
        try {
            PowerMockito.whenNew(JobWebPushEvent.class).withNoArguments().thenReturn(jobWebPushEventMock);
        } catch (Exception e) {
            LOGGER.info("SHM Exception while testing delete : {}", e);
        }

        objectUnderTest.deleteShmJobs(Arrays.asList("123"));

    }

    @Test
    public void testDeleteNhcJobsNhcJobCategorySendWebpush() {
        when(shmJobServiceHelper.updateJobStatusAndGetJobDeletionReport(Arrays.asList(123l))).thenReturn(jobsDeletionReport);
        when(jobConfigurationService.getJobCategory(123l)).thenReturn("NHC_UI");
        try {
            PowerMockito.whenNew(JobWebPushEvent.class).withNoArguments().thenReturn(jobWebPushEventMock);
        } catch (Exception e) {
            LOGGER.info("NHC Exception while testing delete is : {}", e);
        }

        objectUnderTest.deleteShmJobs(Arrays.asList("123"));

    }

    @Test
    public void testDeleteJobsAllCategoriesSendsWebpush() {
        when(shmJobServiceHelper.updateJobStatusAndGetJobDeletionReport(anyList())).thenReturn(jobsDeletionReport);
        when(jobConfigurationService.getJobCategory(anyLong())).thenReturn("NHC_UI").thenReturn("SHM").thenReturn(null);
        try {
            PowerMockito.whenNew(JobWebPushEvent.class).withNoArguments().thenReturn(jobWebPushEventMock);
        } catch (Exception e) {
            LOGGER.info("Exception while testing for all categories delete is : {}", e);
        }
        objectUnderTest.deleteShmJobs(Arrays.asList("123", "456", "789"));

        final Map<String, Object> deleteJobAttributes = new HashMap<>();
        deleteJobAttributes.put(WebPushConstants.JOB_ID, 123l);
        deleteJobAttributes.put(WebPushConstants.JOB_EVENT, WebPushConstants.DELETE_JOB);

        deleteJobAttributes.put(WebPushConstants.JOB_ID, 456l);

    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void test_getJobTemplateIfExceptionThrworn() {
        when(dpsReader.findPOs(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE), anyMap())).thenThrow(new RuntimeException());

        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(shmJobsMapper.getJobTemplateDetails(attributesMapMock, poMock.getPoId())).thenReturn(jobTemplate);
        final JobTemplate response = objectUnderTest.getJobTemplate("name");
        assertEquals(jobTemplate, response);

    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testAddJobCommentIfExceptionThrown() {

        final CommentInfo commentInfo = new CommentInfo();
        final String jobId = "123456789";
        commentInfo.setComment("This is a job comment");
        commentInfo.setJobId(jobId);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(JobModelConstants.JOB_COMMENT, "This is a job comment");
        final long mainJobId = Long.parseLong(jobId);
        when(userContextBean.getLoggedInUserName()).thenReturn("userName");
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> commentList = new ArrayList<Map<String, Object>>();
        mainJobAttributes.put(ShmConstants.COMMENT, commentList);
        when(poMock.getAllAttributes()).thenReturn(mainJobAttributes);
        when(dpsReader.findPOs(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE), anyMap())).thenThrow(new RuntimeException());
        persistenceAdapter.update(mainJobId, attributes);
        objectUnderTest.addJobComment(commentInfo);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testAddJobCommentifExceptionThrown() {
        final CommentInfo commentInfo = new CommentInfo();
        final String jobId = "123456789";
        commentInfo.setComment("This is a job comment");
        commentInfo.setJobId(jobId);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(JobModelConstants.JOB_COMMENT, "This is a job comment");
        final long mainJobId = Long.parseLong(jobId);
        when(userContextBean.getLoggedInUserName()).thenReturn("userName");
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> commentList = new ArrayList<Map<String, Object>>();
        mainJobAttributes.put(ShmConstants.COMMENT, commentList);
        when(poMock.getAllAttributes()).thenReturn(mainJobAttributes);
        when(dpsReader.findPOs(eq(ShmConstants.NAMESPACE), eq(ShmConstants.JOBTEMPLATE), anyMap())).thenThrow(new RuntimeException());
        persistenceAdapter.update(mainJobId, attributes);
        objectUnderTest.addJobComment(commentInfo);
    }

    @Test(expected = RuntimeException.class)
    public void testGetJobDetailsWhenExceptionIsThrown() {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(10);
        jobInput.setOffset(1);
        jobInput.setOrderBy("packageName");
        jobInput.setSortBy("asc");
        final ShmJobs shmJobs = new ShmJobs();
        final SHMJobData shmJobData = new SHMJobData();
        shmJobData.setJobName("TEST");
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        when(shmJobUtil.getJobDetailsList(shmJobs)).thenReturn(shmJobsDataList);
        objectUnderTest.getJobDetails(jobInput);

    }

    @Test(expected = ServerInternalException.class)
    public void testGetJobDetailsWhenmaxUsersReached() {
        doThrow(ServerInternalException.class).when(activeSessionsControllerMock).exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
        objectUnderTest.getJobDetails(null);

        verify(activeSessionsControllerMock, never()).decrementAndGet(anyString());
        verify(shmJobUtil, never()).validateShmJobData(any(JobInput.class));
    }

    @Test(expected = NullPointerException.class)
    public void testGetJobDetailsWithDecrementCounterWhenExceptionthrown() {
        objectUnderTest.getJobDetails(null);
        verify(activeSessionsControllerMock, times(1)).decrementAndGet(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testAddJobCommentWhenExceptionIsThrown() {
        final CommentInfo commentInfo = new CommentInfo();
        final String jobId = "123456789";
        commentInfo.setComment("This is a job comment");
        commentInfo.setJobId(jobId);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(JobModelConstants.JOB_COMMENT, "This is a job comment");
        final long mainJobId = Long.parseLong(jobId);
        when(userContextBean.getLoggedInUserName()).thenReturn("userName");
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> commentList = new ArrayList<Map<String, Object>>();
        mainJobAttributes.put(ShmConstants.COMMENT, commentList);
        when(poMock.getAllAttributes()).thenReturn(mainJobAttributes);
        when(dpsReader.findPOByPoId(mainJobId)).thenThrow(new RuntimeException());
        persistenceAdapter.update(mainJobId, attributes);
        objectUnderTest.addJobComment(commentInfo);
    }

    @Test(expected = RuntimeException.class)
    public void test_getJobTemplateByNeJobIdWhenExceptionIsThrown() {
        when(dpsReader.findPOByPoId(anyLong())).thenThrow(new RuntimeException());
        when(poMock.getAllAttributes()).thenReturn(attributesMapMock);
        when(attributesMapMock.get("mainJobId")).thenReturn(1223l);
        when(attributesMapMock.get("mainJobId")).thenReturn(1223l);
        when(attributesMapMock.get("templateJobId")).thenReturn(654l);

        when(shmJobsMapper.getJobTemplateDetails(attributesMapMock, 654l)).thenReturn(jobTemplate);
        final JobTemplate response = objectUnderTest.getJobTemplateByNeJobId(123456l);
        assertEquals(jobTemplate, response);

    }

    @Test
    public void test_retrieveJobComments() {
        final long mainJobId = 123L;

        final Map<String, Object> jobComment = new HashMap<String, Object>();
        jobComment.put(ShmConstants.USERNAME, "administrator");
        jobComment.put(ShmConstants.COMMENT, "comment 1");
        jobComment.put(ShmConstants.DATE, new Date());

        final List<Map<String, Object>> jobCommentObjects = new ArrayList<Map<String, Object>>();
        jobCommentObjects.add(jobComment);

        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.findPoById(mainJobId)).thenReturn(templateJobPo);
        when(templateJobPo.getAttribute(JobModelConstants.JOB_COMMENT)).thenReturn(jobCommentObjects);

        final List<JobComment> Response = objectUnderTest.retrieveJobComments(mainJobId);
        assertNotNull(Response);

    }

    @Test
    public void test_getRunningMainJobs_noJobsFound() {
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        doNothing().when(typeQuery).setRestriction(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);

        final List<Long> response = objectUnderTest.getMainJobIds();
        assertNotNull(response);
        assertEquals(0, response.size());
    }

    @Test
    public void test_getRunningMainJobs_returnsOneJob() {
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        doNothing().when(typeQuery).setRestriction(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        final Object[] poIds = {123l, 456l};
        final List<Object[]> poIdsList = new ArrayList<Object[]>();
        poIdsList.add(poIds);
        when(queryExecutor.executeProjection(eq(typeQuery), any(Projection.class), any(Projection.class))).thenReturn(poIdsList);
        final List<Long> response = objectUnderTest.getMainJobIds(JobStateEnum.RUNNING.name(), JobStateEnum.CANCELLING.name());

        assertEquals(1, response.size());
        for (final Object mainJobId : response) {
            assertNotNull(mainJobId);
        }
    }

    @Test
    public void test_getRunningMainJobs_handlesMultipleStatesQuery() {
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        doNothing().when(typeQuery).setRestriction(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        final Object[] poIds = {123l, 456l};
        final List<Object[]> poIdsList = new ArrayList<Object[]>();
        poIdsList.add(poIds);
        when(queryExecutor.executeProjection(eq(typeQuery), any(Projection.class), any(Projection.class))).thenReturn(poIdsList);

        final List<Long> response = objectUnderTest.getMainJobIds(JobStateEnum.RUNNING.name(), JobStateEnum.CANCELLING.name(), JobStateEnum.SCHEDULED.name(), JobStateEnum.SUBMITTED.name());

        assertEquals(1, response.size());
        for (final Object mainJobId : response) {
            assertNotNull(mainJobId);
        }
        verify(typeRestrictionBuilder).in(ShmJobConstants.STATE, JobStateEnum.RUNNING.name(), JobStateEnum.CANCELLING.name(), JobStateEnum.SCHEDULED.name(), JobStateEnum.SUBMITTED.name());
    }

    @Test
    public void testGetSkippedNeJobCount() {
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        doNothing().when(typeQuery).setRestriction(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        final String jobId = "123456789";
        final List<Object> mainJobProjection = new ArrayList<Object>();
        mainJobProjection.add(Long.parseLong(jobId));
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(mainJobProjection);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE)).thenReturn(typeQuery1);
        when(typeQuery1.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder1);
        doNothing().when(typeQuery1).setRestriction(restriction1);
        when(queryExecutor.getResultList(typeQuery1)).thenReturn(listMock);
        when(listMock.size()).thenReturn(1);
        assertEquals(1, objectUnderTest.getSkippedNeJobCount(1l));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateNodesCountOfJob() {
        final int nodeCount = 5;
        final Long mainJobPoId = 8978997L;
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobAttributes.put(JobModelConstants.NUMBER_OF_NETWORK_ELEMENTS, nodeCount);
        when(dpsRetryPoliciesMock.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(true);
        assertEquals(true, objectUnderTest.updateNodeCountOfJob(mainJobPoId, nodeCount));
    }

    @Test
    public void test_getsupportedNes() {
        final List<String> neNames = new ArrayList<String>();
        neNames.add("MSC07");
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        neNames.add("LTE05ERBS00001");
        neNames.add("LTE05ERBS00002");
        final String jobType = "NODE_HEALTH_CHECK";
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        final Map<String, Object> supportedUnsupporteMockMap = new HashMap<>();
        supportedUnsupporteMockMap.put(ShmConstants.SUPPORTED_NES, getSupportedNeList());
        Set<NetworkElement> unSupportedNes = new HashSet<>(getunSupportedNeList().keySet());
        final List<NetworkElement> unSupportNeList = new ArrayList(unSupportedNes);
        supportedUnsupporteMockMap.put(ShmConstants.UNSUPPORTED_NES, unSupportNeList);
        when(jobExecutorLocal.getSupportedNes(neNames, jobTypeEnum)).thenReturn(supportedUnsupporteMockMap);

        final Map<String, Object> supportedUnsupportedMap = objectUnderTest.getSupportedNes(neNames, jobType);
        final List<NetworkElement> supportedList = (List<NetworkElement>) supportedUnsupportedMap.get(ShmConstants.SUPPORTED_NES);
        final List<NetworkElement> unsupportedList = (List<NetworkElement>) supportedUnsupportedMap.get(ShmConstants.UNSUPPORTED_NES);
        assertEquals(supportedList.size(), 2);
        assertEquals(unsupportedList.size(), 3);

    }

    private List<NetworkElement> getSupportedNeList() {
        final List<NetworkElement> supportedNeList = new ArrayList<>();

        final NetworkElement ecimNetworkElement1 = new NetworkElement();
        ecimNetworkElement1.setNeType("ECIM");
        ecimNetworkElement1.setName("LTE06dg2ERBS00001");
        ecimNetworkElement1.setPlatformType(PlatformTypeEnum.ECIM);
        final NetworkElement ecimNetworkElement2 = new NetworkElement();
        ecimNetworkElement2.setNeType("ECIM");
        ecimNetworkElement2.setName("LTE06dg2ERBS00002");
        ecimNetworkElement2.setPlatformType(PlatformTypeEnum.ECIM);

        supportedNeList.add(ecimNetworkElement1);
        supportedNeList.add(ecimNetworkElement2);
        return supportedNeList;
    }

    private Map<NetworkElement, String> getunSupportedNeList() {
        final Map<NetworkElement, String> unsupportedNes = new HashMap<>();
        final String invalidNe = "This is invalid ne";
        final NetworkElement cppNetworkElement1 = new NetworkElement();
        cppNetworkElement1.setNeType("ERBS");
        cppNetworkElement1.setName("LTE05ERBS00001");
        cppNetworkElement1.setPlatformType(PlatformTypeEnum.CPP);
        final NetworkElement cppNetworkElement2 = new NetworkElement();
        cppNetworkElement2.setNeType("ERBS");
        cppNetworkElement2.setName("LTE05ERBS00002");
        cppNetworkElement2.setPlatformType(PlatformTypeEnum.CPP);
        final NetworkElement gsmNetworkElement1 = new NetworkElement();
        gsmNetworkElement1.setNeType("AXE");
        gsmNetworkElement1.setName("MSC07");
        gsmNetworkElement1.setPlatformType(PlatformTypeEnum.AXE);
        unsupportedNes.put(cppNetworkElement1, invalidNe);
        unsupportedNes.put(cppNetworkElement2, invalidNe);
        unsupportedNes.put(gsmNetworkElement1, invalidNe);
        return unsupportedNes;
    }
}
