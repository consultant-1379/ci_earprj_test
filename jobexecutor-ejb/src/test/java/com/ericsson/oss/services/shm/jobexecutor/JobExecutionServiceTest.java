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
package com.ericsson.oss.services.shm.jobexecutor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.services.shm.jobexecutor.common.NeComponentsInfoBuilderImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.sdk.core.retry.*;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.shm.activities.*;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.wfs.WfsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProviderImpl;
import com.ericsson.oss.services.shm.jobexecutor.common.NeComponentsInfoBuilderImpl;
import com.ericsson.oss.services.shm.jobexecutor.ecim.EcimJobExecutionValidatorImpl;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobPropertyProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobConfigurationException;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.JobAdministratorTBACValidator;
import com.ericsson.oss.services.shm.tbac.TBACResponse;
import com.ericsson.oss.services.shm.tbac.models.TBACConfigurationProvider;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService;
import com.ericsson.oss.services.topologyCollectionsService.exception.PrivateCollectionException;
import com.ericsson.oss.services.topologyCollectionsService.exception.TopologyCollectionsServiceException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.CollectionNotFoundException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchQueryException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchServiceException;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.api.query.*;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import com.ericsson.oss.services.wfs.api.query.usertask.UsertaskQueryAttributes;

/**
 * Test class for JobExecution Service.
 * 
 * @author zkummad
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
@SuppressWarnings("unchecked")
public class JobExecutionServiceTest {

    private static final Long JOBTEMPLATEID = 0L;

    private static final String WFSID = "wfs_id";
    private static final String LOGIN_USER = "loginUser";
    private static final String NODE_TYPE = "ERBS";

    @Mock
    private JobTemplate jobTemplateMock;

    @Mock
    private JobMapper jobMapperMock;

    @Mock
    private JobConfiguration jobConfigurationMock;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private Activity activityMock;

    @Mock
    private DpsReader dpsReader;

    @Mock
    private Query wfsQueryMock;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private QueryBuilder queryBuilderMock;

    @Mock
    private List<WorkflowObject> workFlowObjectsMock;

    @Mock
    private WorkflowObject workFlowObject;

    @Mock
    private PersistenceObject persistenceObject;

    @InjectMocks
    private JobExecutionService jobExecutionService;

    @Mock
    private JobConfigurationService jobConfigurationService;

    @Mock
    private JobExecutorServiceHelper executorServiceHelper;

    @Mock
    private JobExecutionIndexAndState jobExecutionIndexAndStateMock;

    @Mock
    private Schedule scheduleMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private JobUpdateService jobUpdateServicemock;

    @Mock
    private JobPropertyBuilderFactory jobPropertyBuilderFactory;

    @Mock
    private JobPropertyProvider backupJobPropertyProvider;

    @Mock
    private RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private RetryManager retryManagerMock;

    @Mock
    private DpsRetryConfigurationParamProvider dpsConfigMock;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    private ScheduleProperty schedulePropertyMock;

    @Mock
    private NEInfo NEInfoMock;

    @Mock
    private NEJobProperty NEJobPropertyMock;

    @Mock
    private JobProperty jobPropertyMock;

    @Mock
    private final List<String> stringsMock = new ArrayList<String>();

    @Mock
    private List<Map<String, Object>> listofMapMock;

    @Mock
    private List<Map<String, String>> listofMapWithStringMock;

    @Mock
    private List<JobProperty> listOfJobPropMock;

    @Mock
    private Map<String, String> mapOfStrings;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private ProcessVariableBuilder processVariableBuilderMock;

    @Mock
    private WorkflowDefinitionsProvider workflowDefinitionsProviderMock;

    @Mock
    private TopologyCollectionsEjbService topologyCollectionsEjbServiceMock;

    @Mock
    private JobExecutionValidatorFactory jobExecutionValidationFactoryMock;

    @Mock
    private EcimJobExecutionValidatorImpl ecimJobExecutionValidatorImplMock;

    @Mock
    private JobExecutionValidator jobExecutionValidatorMock;

    @Mock
    private WfsRetryConfigurationParamProvider wfsRetryConfigurationParamProvider;

    @Mock
    private TargetResolverFactory targetResolverFactoryMock;

    @Mock
    private NetworkElementResolver networkElementResolverMock;

    @Mock
    private NFVOResolver nfvoResolverMock;

    @Mock
    private NetworkElementResponse networkElementResponseMock;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private JobCancelHandler jobCancellingHandlerMock;

    @Mock
    private JobAdministratorTBACValidator jobAdministratorTBACValidator;

    @Mock
    private TBACConfigurationProvider tbacConfigurationProvider;

    @Mock
    private TBACValidator tbacValidator;

    @Mock
    private SupportedNesListBuilder supportedNesListBuilder;

    @Mock
    private SynchronousActivityProvider synchronousActivityProvider;

    @Mock
    private JobBuilderRetryProxy builderRetryProxy;

    @Mock
    private NeComponentsInfoBuilderImpl neComponentsInfoBuilderImpl;

    @Mock
    private JobStaticDataProviderImpl jobStaticDataProviderImpl;

    @Mock
    private JobStaticDataProvider jobStaticDataProviderMock;

    @Mock
    private RestrictionBuilder restrictionBuilderMock;

    @Mock
    private Restriction restrictionMock;

    private static final long JOB_ID = 123456L;

    private static final long JOB_TEMPLATE_ID = 123456L;

    private static final String SHM_USER = "shmuser";

    private static final String NODE_NAME = "nodeName";

    final Map<String, Object> logEntry = new HashMap<String, Object>();

    private final Class<? extends Exception>[] exceptionsArray = new Class[] {};

    @Test
    public void testExecute_NoNeSubmitted() throws JobConfigurationException {
        setRetryPolicies();

        final List<Activity> activities = new ArrayList<>();
        NEInfo neInfo = new NEInfo();
        activities.add(activityMock);
        final List<Map<String, String>> mainJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobState = new HashMap<String, String>();
        jobState.put(ShmConstants.KEY, ShmConstants.STATE);
        jobState.put(ShmConstants.VALUE, JobState.SUBMITTED.getJobStateName());
        mainJobProperties.add(jobState);
        when(jobConfigurationService.fetchJobProperty(JOBTEMPLATEID)).thenReturn(mainJobProperties);
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(neInfo);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn("IMMEDIATE");
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(jobUpdateServicemock.updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        jobExecutionService.execute(WFSID, JOBTEMPLATEID);

        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "IMMEDIATE" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);

    }

    @Test
    public void testInvokeMainJobsMaually() throws JobDataNotFoundException {
        setRetryPolicies();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        final List<Map<String, Object>> neJobs = new ArrayList<>();
        final Map<String, Object> neJob = new HashMap<>();
        neJob.put(ShmConstants.PO_ID, 123456L);
        neJob.put(ShmConstants.WFS_ID, 123456L);
        neJob.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
        neJobs.add(neJob);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        List<WorkflowObject> listWrk = new ArrayList<>();
        listWrk.add(workFlowObject);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(listWrk);
        when(workFlowObject.getAttribute(Matchers.anyString())).thenReturn("Id");
        final Map<Object, Object> restrictions = new HashMap<>();
        restrictions.put(ObjectField.PO_ID, 123456L);
        final Map<String, Object> jobTemplate = getJobTemplateDetails();
        when(jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions, Arrays.asList(ShmConstants.NAME))).thenReturn(Arrays.asList(jobTemplate));
        final Map<String, Object> poAttributes = new HashMap<>();
        final JobStaticData jobStaticData = new JobStaticData("admin", null, "MANUAL", JobType.getJobType("UPGRADE"), LOGIN_USER);
        when(jobStaticDataProviderImpl.getJobStaticData(123456L)).thenReturn(jobStaticData);
        poAttributes.put(ShmConstants.SHM_JOB_EXEC_USER, LOGIN_USER);
        jobLogList.add(prepareJobLogs(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_MAINJOB, LOGIN_USER), new Date(), JobLogLevel.INFO.toString()));
        when(jobLogUtilMock.createNewLogEntry(anyString(), Matchers.any(Date.class), anyString())).thenReturn(logEntry);
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, JOB_TEMPLATE_ID, LOGIN_USER)).thenReturn(true);
        jobStaticDataProviderMock.put(JOB_ID, jobStaticData);
        jobExecutionService.invokeMainJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(workflowInstanceNotifier, times(1)).completeUserTask("Id");
        verify(jobUpdateServicemock, times(1)).updateJobAttributes(123456L, poAttributes);
        verify(jobUpdateServicemock, times(1)).updateRunningJobAttributes(123456L, jobPropertiesList, jobLogList);
    }

    @Test
    public void testInvokeMainJobsMauallyTBACFailure() throws JobDataNotFoundException {
        setRetryPolicies();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        final List<Map<String, Object>> neJobs = new ArrayList<>();
        final Map<String, Object> neJob = new HashMap<>();
        neJob.put(ShmConstants.PO_ID, 123456L);
        neJob.put(ShmConstants.WFS_ID, 123456L);
        neJob.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
        neJobs.add(neJob);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(workFlowObjectsMock);
        when(workFlowObjectsMock.get(0)).thenReturn(workFlowObject);
        when(workFlowObject.getAttribute(UsertaskQueryAttributes.QueryParameters.ID)).thenReturn("Id");
        final Map<Object, Object> restrictions = new HashMap<>();
        restrictions.put(ObjectField.PO_ID, 123456L);
        final Map<String, Object> jobTemplate = getJobTemplateDetails();
        when(jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions, Arrays.asList(ShmConstants.NAME))).thenReturn(Arrays.asList(jobTemplate));
        final Map<String, Object> poAttributes = new HashMap<>();
        final JobStaticData jobStaticData = new JobStaticData("admin", null, "MANUAL", JobType.getJobType("BACKUP"), LOGIN_USER);
        when(jobStaticDataProviderImpl.getJobStaticData(123456L)).thenReturn(jobStaticData);
        poAttributes.put(ShmConstants.SHM_JOB_EXEC_USER, LOGIN_USER);
        jobLogList.add(prepareJobLogs(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_MAINJOB, LOGIN_USER), new Date(), JobLogLevel.INFO.toString()));
        when(jobLogUtilMock.createNewLogEntry(anyString(), Matchers.any(Date.class), anyString())).thenReturn(logEntry);
        jobExecutionService.invokeMainJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(systemRecorderMock, times(1)).recordEvent(eq(SHMEvents.JOB_CONTINUE_SKIPPED), eq(EventLevel.COARSE), anyString(), eq(ShmConstants.JOB), anyString());
    }

    @Test
    public void testInvokeMainJobsMauallyNegative1() {
        setRetryPolicies();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJob = new HashMap<String, Object>();
        neJob.put(ShmConstants.PO_ID, 123456L);
        neJob.put(ShmConstants.WFS_ID, 123456L);
        neJob.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
        neJobs.add(neJob);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);

        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(null);
        when(workFlowObjectsMock.get(0)).thenReturn(mock(WorkflowObject.class));
        when(mock(WorkflowObject.class).getAttribute(UsertaskQueryAttributes.QueryParameters.ID)).thenReturn("Id");
        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        restrictions.put(ObjectField.PO_ID, 123456L);
        final Map<String, Object> jobTemplate = getJobTemplateDetails();
        when(jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions, Arrays.asList(ShmConstants.NAME))).thenReturn(Arrays.asList(jobTemplate));
        jobExecutionService.invokeMainJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(workflowInstanceNotifier, times(0)).completeUserTask(anyString());
    }

    @Test
    public void testInvokeMainJobsMauallyNegative2() {
        final List<Long> idList = new ArrayList<>();
        jobExecutionService.invokeMainJobsManually(idList, LOGIN_USER);
        verify(dpsReader, times(0)).findPOByPoId(anyLong());
    }

    @Test
    public void testInvokeMainJobsManuallyThrowsException() throws JobDataNotFoundException {
        setRetryPolicies();
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        final List<Map<String, Object>> neJobs = new ArrayList<>();
        final Map<String, Object> neJob = new HashMap<>();
        neJob.put(ShmConstants.PO_ID, 123456L);
        neJob.put(ShmConstants.WFS_ID, 123456L);
        neJob.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
        neJobs.add(neJob);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        List<WorkflowObject> listWrk = new ArrayList<>();
        listWrk.add(workFlowObject);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(listWrk);
        when(workFlowObject.getAttribute(UsertaskQueryAttributes.QueryParameters.ID)).thenReturn("Id");
        final Map<Object, Object> restrictions = new HashMap<>();
        restrictions.put(ObjectField.PO_ID, 123456L);
        final Map<String, Object> jobTemplate = getJobTemplateDetails();
        final JobStaticData jobStaticData = new JobStaticData("admin", null, "MANUAL", JobType.getJobType("UPGRADE"), LOGIN_USER);
        when(jobStaticDataProviderImpl.getJobStaticData(123456L)).thenReturn(jobStaticData);
        when(jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions, Arrays.asList(ShmConstants.NAME))).thenReturn(Arrays.asList(jobTemplate));
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, JOB_TEMPLATE_ID, LOGIN_USER)).thenReturn(true);
        doThrow(new WorkflowServiceInvocationException("WorkflowServiceInvocationException")).when(workflowInstanceNotifier).completeUserTask("Id");
        jobStaticDataProviderMock.put(JOB_ID, jobStaticData);
        jobExecutionService.invokeMainJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(workflowInstanceNotifier, times(1)).completeUserTask("Id");
        verify(jobUpdateServicemock, times(1)).updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());
    }

    @Test
    public void testInvokeNeJobsMaually() throws JobDataNotFoundException {
        setRetryPolicies();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        List<WorkflowObject> listWrk = new ArrayList<>();
        listWrk.add(workFlowObject);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(listWrk);
        when(mock(WorkflowObject.class).getAttribute(UsertaskQueryAttributes.QueryParameters.ID)).thenReturn("Id");
        final Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.SHM_JOB_EXEC_USER, LOGIN_USER);
        jobLogList.add(prepareJobLogs(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_NEJOB, LOGIN_USER, "activityName"), new Date(), JobLogLevel.INFO.toString()));
        when(jobLogUtilMock.createNewLogEntry(anyString(), Matchers.any(Date.class), anyString())).thenReturn(logEntry);
        when(jobAdministratorTBACValidator.validateTBACForNEJob(NODE_NAME, LOGIN_USER)).thenReturn(true);
        final JobStaticData jobStaticData = new JobStaticData("admin", null, "MANUAL", JobType.getJobType("Backup"), LOGIN_USER);
        when(jobStaticDataProviderImpl.getJobStaticData(123456L)).thenReturn(jobStaticData);
        jobStaticDataProviderMock.put(JOB_ID, jobStaticData);
        jobExecutionService.invokeNeJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(workflowInstanceNotifier, times(0)).completeUserTask("Id");
        verify(jobUpdateServicemock, times(1)).updateJobAttributes(123456L, poAttributes);
        verify(jobUpdateServicemock, times(1)).updateRunningJobAttributes(123456L, jobPropertiesList, jobLogList);
    }

    @Test
    public void testInvokeNeJobsMauallyTBACFailure() {

        setRetryPolicies();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(workFlowObjectsMock);
        when(workFlowObjectsMock.get(0)).thenReturn(mock(WorkflowObject.class));
        when(mock(WorkflowObject.class).getAttribute(UsertaskQueryAttributes.QueryParameters.ID)).thenReturn("Id");
        final Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.SHM_JOB_EXEC_USER, LOGIN_USER);
        jobLogList.add(prepareJobLogs(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_NEJOB, LOGIN_USER, "activityName"), new Date(), JobLogLevel.INFO.toString()));
        when(jobLogUtilMock.createNewLogEntry(anyString(), Matchers.any(Date.class), anyString())).thenReturn(logEntry);
        jobExecutionService.invokeNeJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(systemRecorderMock, times(1)).recordEvent(eq(SHMEvents.JOB_CONTINUE_SKIPPED), eq(EventLevel.COARSE), eq(NODE_NAME), eq(ShmConstants.NE_JOB), anyString());
    }

    @Test
    public void testInvokeNeJobsMauallyNegative1() {
        setRetryPolicies();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getNEJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(Collections.EMPTY_LIST);
        when(workFlowObjectsMock.get(0)).thenReturn(mock(WorkflowObject.class));
        when(mock(WorkflowObject.class).getAttribute(UsertaskQueryAttributes.QueryParameters.ID)).thenReturn("Id");
        final Map<String, Object> activityAttributes = new HashMap<String, Object>();
        activityAttributes.put(ShmConstants.NAME, "verify");
        activityAttributes.put(ShmConstants.PO_ID, 123L);
        when(jobConfigurationService.retrieveWaitingActivityDetails(123456L)).thenReturn(activityAttributes);
        when(jobUpdateServicemock.updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        jobExecutionService.invokeNeJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(workflowInstanceNotifier, times(0)).completeUserTask(anyString());
    }

    @Test
    public void testInvokeNeJobsMauallyNegative2() {
        final List<Long> idList = new ArrayList<Long>();
        jobExecutionService.invokeNeJobsManually(idList, LOGIN_USER);
        verify(dpsReader, times(0)).findPOByPoId(anyLong());
    }

    @Test
    public void testInvokeMainJobsMauallyWithJobStaticDataNotFoundEitherFromCacheOrDps() throws JobDataNotFoundException {
        setRetryPolicies();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        final List<Map<String, Object>> neJobs = new ArrayList<>();
        final Map<String, Object> neJob = new HashMap<>();
        neJob.put(ShmConstants.PO_ID, 123456L);
        neJob.put(ShmConstants.WFS_ID, 123456L);
        neJob.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
        neJobs.add(neJob);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(queryBuilderMock.createTypeQuery(QueryType.USERTASK_QUERY)).thenReturn(wfsQueryMock);
        List<WorkflowObject> listWrk = new ArrayList<>();
        listWrk.add(workFlowObject);
        when(workflowInstanceNotifier.executeWorkflowQueryForJobContinue(any(Query.class))).thenReturn(listWrk);
        when(workFlowObject.getAttribute(Matchers.anyString())).thenReturn("Id");
        final Map<Object, Object> restrictions = new HashMap<>();
        restrictions.put(ObjectField.PO_ID, 123456L);
        final Map<String, Object> jobTemplate = getJobTemplateDetails();
        when(jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions, Arrays.asList(ShmConstants.NAME))).thenReturn(Arrays.asList(jobTemplate));
        final Map<String, Object> poAttributes = new HashMap<>();
        final JobStaticData jobStaticData = new JobStaticData("admin", null, "MANUAL", JobType.getJobType("UPGRADE"), LOGIN_USER);
        when(jobStaticDataProviderImpl.getJobStaticData(123456L)).thenReturn(jobStaticData);
        poAttributes.put(ShmConstants.SHM_JOB_EXEC_USER, LOGIN_USER);
        jobLogList.add(prepareJobLogs(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_MAINJOB, LOGIN_USER), new Date(), JobLogLevel.INFO.toString()));
        when(jobLogUtilMock.createNewLogEntry(anyString(), Matchers.any(Date.class), anyString())).thenReturn(logEntry);
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, JOB_TEMPLATE_ID, LOGIN_USER)).thenReturn(true);
        doThrow(new JobDataNotFoundException()).when(jobStaticDataProviderImpl).getJobStaticData(anyLong());
        jobExecutionService.invokeMainJobsManually(Arrays.asList(123l), LOGIN_USER);
        verify(workflowInstanceNotifier, times(1)).completeUserTask(anyString());
    }

    @Test
    public void testExecute_NeNamesSubmitted_WithEcimFragmentSupport() throws JobConfigurationException, PrivateCollectionException, TopologyCollectionsServiceException, TopologySearchQueryException,
            SecurityViolationException, TopologySearchServiceException {
        setRetryPolicies();

        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        final List<Map<String, String>> mainJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobState = new HashMap<String, String>();
        jobState.put(ShmConstants.KEY, ShmConstants.STATE);
        jobState.put(ShmConstants.VALUE, JobState.SUBMITTED.getJobStateName());
        mainJobProperties.add(jobState);

        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        final Map<String, String> nePlatformType = new HashMap<String, String>();
        nePlatformType.put("selectedCppNE", "CPP");
        nePlatformType.put("selectedEcimNE", "ECIM");
        neJobPO.put(ShmConstants.PO_ID, 123456L);
        neJobPO.put("name", "name");
        neJobPO.put(ShmConstants.JOB_TYPE, "UPGRADE");

        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType("ERBS");
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final NetworkElement ecimNetworkElement = new NetworkElement();
        ecimNetworkElement.setNeType("SGSN-MME");
        ecimNetworkElement.setName("selectedEcimNE");
        ecimNetworkElement.setPlatformType(PlatformTypeEnum.ECIM);
        final Map<NetworkElement, String> NetworkElementMap = new HashMap<NetworkElement, String>();
        NetworkElementMap.put(ecimNetworkElement, Matchers.anyString());
        final Map<String, Object> poAttributes = poAttributeDetails();

        when(mapMock.get(ShmConstants.NETYPE)).thenReturn("ERBS");
        when(jobConfigurationService.fetchJobProperty(JOBTEMPLATEID)).thenReturn(mainJobProperties);
        when(mapMock.get("schedule")).thenReturn(mapMock);
        when(mapMock.get("execMode")).thenReturn("immediate");
        when(mapMock.get("name")).thenReturn("name");
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(new NEInfo());
        final List<NetworkElement> selectedNEList = new ArrayList<NetworkElement>();
        selectedNEList.add(cppNetworkElement);
        selectedNEList.add(ecimNetworkElement);

        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, neJobPO, neJobPO, neJobPO, neJobPO, neJobs, neJobPO, neJobPO,
                neJobPO, neJobPO, neJobPO, neJobs, neJobPO);
        when(jobExecutionValidationFactoryMock.getJobExecutionValidator(PlatformTypeEnum.CPP)).thenReturn(null);
        when(jobExecutionValidationFactoryMock.getJobExecutionValidator(PlatformTypeEnum.ECIM)).thenReturn(ecimJobExecutionValidatorImplMock);

        when(ecimJobExecutionValidatorImplMock.findUnSupportedNEs(JobTypeEnum.UPGRADE, Arrays.asList(ecimNetworkElement))).thenReturn(NetworkElementMap);

        Mockito.doNothing().when(jobUpdateServicemock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());

        final List<Map<String, Object>> emtpyListOfMaps = Collections.emptyList();
        when(jobConfigurationService.getProjectedAttributes(anyString(), anyString(), anyMap(), anyList())).thenReturn(emtpyListOfMaps);

        when(targetResolverFactoryMock.getTargetResolver(JobTypeEnum.UPGRADE.toString())).thenReturn(networkElementResolverMock);
        when(networkElementResolverMock.getNetworkElementResponse(Matchers.anyLong(), Matchers.anyList(), Matchers.anyLong(), Matchers.anyMap(), Matchers.any(JobTypeEnum.class),
                Matchers.anyBoolean())).thenReturn(networkElementResponseMock);
        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "exec" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testExecute_NeNamesSubmitted_WithoutEcimFragmentSupport() throws JobConfigurationException, PrivateCollectionException, TopologyCollectionsServiceException,
            TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        setRetryPolicies();

        final List<Map<String, String>> mainJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobState = new HashMap<String, String>();
        jobState.put(ShmConstants.KEY, ShmConstants.STATE);
        jobState.put(ShmConstants.VALUE, JobState.SUBMITTED.getJobStateName());
        mainJobProperties.add(jobState);
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        final Map<String, String> nePlatformType = new HashMap<String, String>();
        nePlatformType.put("selectedCppNE", "CPP");
        nePlatformType.put("selectedEcimNE", "ECIM");
        neJobPO.put(ShmConstants.PO_ID, 123456L);
        neJobPO.put("name", "name");
        neJobPO.put(ShmConstants.JOB_TYPE, "UPGRADE");
        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType("ERBS");
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final NetworkElement ecimNetworkElement = new NetworkElement();
        ecimNetworkElement.setNeType("SGSN-MME");
        ecimNetworkElement.setName("selectedEcimNE");
        ecimNetworkElement.setPlatformType(PlatformTypeEnum.ECIM);
        final Map<NetworkElement, String> networkElementMap = new HashMap<NetworkElement, String>();
        networkElementMap.put(ecimNetworkElement, Matchers.anyString());
        final Map<String, Object> poAttributes = poAttributeDetails();

        when(mapMock.get(ShmConstants.NETYPE)).thenReturn("ERBS");
        when(jobConfigurationService.fetchJobProperty(JOBTEMPLATEID)).thenReturn(mainJobProperties);
        when(mapMock.get("schedule")).thenReturn(mapMock);
        when(mapMock.get("execMode")).thenReturn("immediate");
        when(mapMock.get("name")).thenReturn("name");
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);

        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(new NEInfo());

        final List<NetworkElement> neList = new ArrayList<NetworkElement>();
        neList.add(cppNetworkElement);
        neList.add(ecimNetworkElement);

        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, neJobPO, neJobPO, neJobPO, neJobPO, neJobs, neJobPO);

        when(jobExecutionValidationFactoryMock.getJobExecutionValidator(PlatformTypeEnum.CPP)).thenReturn(null);
        when(jobExecutionValidationFactoryMock.getJobExecutionValidator(PlatformTypeEnum.ECIM)).thenReturn(ecimJobExecutionValidatorImplMock);

        when(ecimJobExecutionValidatorImplMock.findUnSupportedNEs(JobTypeEnum.UPGRADE, Arrays.asList(ecimNetworkElement))).thenReturn(networkElementMap);

        Mockito.doNothing().when(jobUpdateServicemock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());

        when(targetResolverFactoryMock.getTargetResolver(JobTypeEnum.UPGRADE.toString())).thenReturn(networkElementResolverMock);
        when(networkElementResolverMock.getNetworkElementResponse(Matchers.anyLong(), Matchers.anyList(), Matchers.anyLong(), Matchers.anyMap(), Matchers.any(JobTypeEnum.class),
                Matchers.anyBoolean())).thenReturn(networkElementResponseMock);

        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "exec" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testExecuteNeJobCreationFailed() {
        final long mainJobId = 1L;
        final String wfsId = "123";
        final long jobTemplateId = 2L;

        final Map<String, Object> jobTempalteAttributes = new HashMap<>();
        final List<NetworkElement> unAuthorizedNes = new ArrayList<>();
        setRetryPolicies();

        Map<String, Object> mainSchedule = new HashMap<>();
        mainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());

        final Map<String, Object> jobConfiguration = new HashMap<>();
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);

        Map<String, Object> mainJobAttributes = prepareMainJobAttributes(jobTemplateId, jobConfiguration);

        when(jobUpdateServicemock.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(jobTempalteAttributes);
        JobTemplate jobTemplateDetails = getJobTemplateDetails1();
        when(jobMapperMock.getJobTemplateDetails(mainJobAttributes, jobTemplateId)).thenReturn(jobTemplateDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);

        TBACResponse tBACResponse1 = prepareTbacResponse(unAuthorizedNes);

        NetworkElementResponse networkElementResponse = getNetworkElementsResponse();
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        when(executorServiceHelper.getNetworkElementDetails(mainJobId, jobTemplateDetails, jobProperties, neDetailsWithParentName)).thenReturn(networkElementResponse);
        when(tbacValidator.validateTBAC(networkElementResponse, jobTemplateDetails, mainJobId, mainJobAttributes)).thenReturn(tBACResponse1);

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mainJobAttributes);

        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType("ERBS");
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);

        final Map<NetworkElement, String> unSupportedNetworkElements = new HashMap<>();

        when(executorServiceHelper.getFilteredSupportedNodes(anyList(), anyList())).thenReturn(neList);
        when(workflowInstanceNotifier.sendAllNeDone(anyString())).thenReturn(true);
        when(executorServiceHelper.getFilteredUnSupportedNodes(anyMap(), anyList())).thenReturn(unSupportedNetworkElements);
        when(supportedNesListBuilder.buildSupportedNesListForNeJobsCreation(networkElementResponse.getNesWithComponents(), neList)).thenReturn(neList);
        final String logMessage = String.format("NEjob creation failed for the selected nodes: \"%s\" for mainJobId: \"%s\"", neList.get(0), mainJobId);
        jobExecutionService.execute(wfsId, mainJobId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "IMMEDIATEJOB", "Job",
                "SHM:JOB:Proceeding execution for mainJobId " + mainJobId + " and wfsId " + wfsId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.NEJOBS_CREATION_FAILED, EventLevel.COARSE, "mainJobId:" + mainJobId, "wfsId:" + wfsId, logMessage);

    }

    @Test
    public void testExecuteNeJobSubmissionFailed() {
        final long mainJobId = 1L;
        final String wfsId = "123";
        final long jobTemplateId = 2L;
        final long neJobId = 3L;

        final Map<String, Object> jobTempalteAttributes = new HashMap<>();
        final List<NetworkElement> unAuthorizedNes = new ArrayList<>();
        setRetryPolicies();

        Map<String, Object> mainSchedule = new HashMap<>();
        mainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());

        final Map<String, Object> jobConfiguration = new HashMap<>();
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);

        final Map<String, Object> mainJobAttributes = prepareMainJobAttributes(jobTemplateId, jobConfiguration);

        when(jobUpdateServicemock.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(jobTempalteAttributes);
        JobTemplate jobTemplateDetails = getJobTemplateDetails1();
        when(jobMapperMock.getJobTemplateDetails(mainJobAttributes, jobTemplateId)).thenReturn(jobTemplateDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        mainJobAttributes.put(ShmConstants.PO_ID, neJobId);

        TBACResponse tBACResponse1 = prepareTbacResponse(unAuthorizedNes);

        NetworkElementResponse networkElementResponse = getNetworkElementsResponse();
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        when(executorServiceHelper.getNetworkElementDetails(mainJobId, jobTemplateDetails, jobProperties, neDetailsWithParentName)).thenReturn(networkElementResponse);
        when(tbacValidator.validateTBAC(networkElementResponse, jobTemplateDetails, mainJobId, mainJobAttributes)).thenReturn(tBACResponse1);

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mainJobAttributes).thenReturn(mainJobAttributes).thenReturn(mainJobAttributes)
                .thenReturn(mainJobAttributes);

        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType("ERBS");
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);

        final Map<NetworkElement, String> unSupportedNetworkElements = new HashMap<>();
        final Map<String, Object> creationDetails = new HashMap<>();
        creationDetails.put("neJobId", neJobId);

        when(builderRetryProxy.createNeJob(Matchers.anyLong(), Matchers.anyString(), Matchers.any(NetworkElement.class), Matchers.anyMap(), Matchers.anyMap())).thenReturn(creationDetails);

        when(executorServiceHelper.getFilteredSupportedNodes(anyList(), anyList())).thenReturn(neList);
        when(workflowInstanceNotifier.sendAllNeDone(anyString())).thenReturn(true);
        when(executorServiceHelper.getFilteredUnSupportedNodes(anyMap(), anyList())).thenReturn(unSupportedNetworkElements);
        when(supportedNesListBuilder.buildSupportedNesListForNeJobsCreation(networkElementResponse.getNesWithComponents(), neList)).thenReturn(neList);
        final String logMessage = String.format("NEjob submission failed for the selected nodes: \"%s\" for mainJobId: \"%s\"", neList.get(0), mainJobId);

        jobExecutionService.execute(wfsId, mainJobId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "IMMEDIATEJOB", "Job",
                "SHM:JOB:Proceeding execution for mainJobId " + mainJobId + " and wfsId " + wfsId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.NEJOBS_SUBMISSION_FAILED, EventLevel.COARSE, "mainJobId:" + mainJobId, "wfsId:" + wfsId, logMessage);

    }

    @Test
    public void testExecute_NeNamesCompleted() throws JobConfigurationException, PrivateCollectionException, TopologyCollectionsServiceException, TopologySearchQueryException,
            SecurityViolationException, TopologySearchServiceException {
        setRetryPolicies();
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        final NEInfo neInfo = new NEInfo();
        final Map<String, String> mapLocal = new HashMap<String, String>();

        mapLocal.put("precheck", PlatformTypeEnum.CPP.toString());
        stringsMock.add("precheck");
        final Map<String, String> nePlatformType = new HashMap<String, String>();
        nePlatformType.put("precheck", "CPP");
        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ERBS");
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElements.add(networkElement);
        networkElement.setName("precheck");
        when(mapMock.get(ShmConstants.NETYPE)).thenReturn("ERBS");
        when(jobUpdateServicemock.retrieveJobWithRetry(JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(ExecMode.SCHEDULED.getMode());
        when(mapMock.get(ShmConstants.SELECTED_NES)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.NENAMES)).thenReturn(stringsMock);
        when(mapMock.get(ShmConstants.COLLECTION_NAMES)).thenReturn(stringsMock);
        when(mapMock.get(ShmConstants.SAVED_SEARCH_IDS)).thenReturn(stringsMock);
        when(mapMock.get(ShmConstants.PO_ID)).thenReturn(JOB_ID);
        when(mapMock.get(ShmConstants.JOB_TYPE)).thenReturn(JobTypeEnum.BACKUP.toString());
        when(mapMock.get(ShmConstants.ACTIVITIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn(ShmConstants.START_DATE);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(mapMock.get("schedule")).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn("scheduled");
        when(mapMock.get("scheduleAttributes")).thenReturn(Arrays.asList(mapMock));

        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);

        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(neInfo);

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, mapMock, mapMock, mapMock, mapMock, mapMock, listofMapMock, mapMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(networkElements);
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);
        when(jobExecutionValidationFactoryMock.getJobExecutionValidator(PlatformTypeEnum.CPP)).thenReturn(null);

        when(targetResolverFactoryMock.getTargetResolver(JobTypeEnum.BACKUP.toString())).thenReturn(networkElementResolverMock);
        when(networkElementResolverMock.getNetworkElementResponse(Matchers.anyLong(), Matchers.anyList(), Matchers.anyLong(), Matchers.anyMap(), Matchers.any(JobTypeEnum.class),
                Matchers.anyBoolean())).thenReturn(networkElementResponseMock);
        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "scheduled" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());

    }

    @Test
    public void testExecute_NeNamesScheduled() throws JobConfigurationException, PrivateCollectionException, TopologyCollectionsServiceException, TopologySearchQueryException,
            SecurityViolationException, TopologySearchServiceException {
        setRetryPolicies();
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        final List<Map<String, String>> mainJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobState = new HashMap<String, String>();
        jobState.put(ShmConstants.KEY, ShmConstants.STATE);
        jobState.put(ShmConstants.VALUE, JobState.SUBMITTED.getJobStateName());
        mainJobProperties.add(jobState);

        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        final List<Map<String, Object>> scheduleProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> schedules = new HashMap<String, Object>();
        final Map<String, String> nePlatformType = new HashMap<String, String>();
        nePlatformType.put("selectedNE", "CPP");
        schedules.put("name", "END_DATE");
        schedules.put("value", Long.toString(new Date().getTime()));
        neJobPO.put(ShmConstants.PO_ID, 123456L);
        neJobPO.put("name", "name");
        neJobPO.put(ShmConstants.JOB_TYPE, "UPGRADE");
        scheduleProperties.add(schedules);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ERBS");
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElement.setName("selectedNE");
        final Map<String, Object> poAttributes = poAttributeDetails();
        when(mapMock.get("schedule")).thenReturn(mapMock);
        when(mapMock.get("execMode")).thenReturn("scheduled");
        when(mapMock.get("name")).thenReturn("name");
        when(mapMock.get("scheduleAttributes")).thenReturn(scheduleProperties);

        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, neJobPO, neJobPO, neJobPO, neJobPO, neJobs, neJobPO);
        Mockito.doNothing().when(jobUpdateServicemock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);

        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(new NEInfo());

        when(jobExecutionValidationFactoryMock.getJobExecutionValidator(PlatformTypeEnum.CPP)).thenReturn(null);

        when(targetResolverFactoryMock.getTargetResolver(JobTypeEnum.UPGRADE.toString())).thenReturn(networkElementResolverMock);
        when(networkElementResolverMock.getNetworkElementResponse(Matchers.anyLong(), Matchers.anyList(), Matchers.anyLong(), Matchers.anyMap(), Matchers.any(JobTypeEnum.class),
                Matchers.anyBoolean())).thenReturn(networkElementResponseMock);

        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "exec" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);
        verify(jobUpdateServicemock, times(0)).updateJobAttributes(eq(0L), anyMap());
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());

    }

    public void testExecute_thowsExceptionWhenCollectionsNotPresent() throws JobConfigurationException, PrivateCollectionException, TopologyCollectionsServiceException {
        setRetryPolicies();
        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        neJobPO.put(ShmConstants.PO_ID, 12337829l);
        neJobPO.put("name", "name");
        neJobPO.put(ShmConstants.JOB_TYPE, "BACKUP");
        final Map<String, Object> poAttributes = poAttributeDetails();
        final List<String> collList = new ArrayList<String>();
        collList.add("collection1");
        when(mapMock.get(ShmConstants.NENAMES)).thenReturn(stringsMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(ExecMode.SCHEDULED.getMode());
        when(mapMock.get(ShmConstants.SELECTED_NES)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.COLLECTION_NAMES)).thenReturn(collList);
        when(mapMock.get(ShmConstants.SAVED_SEARCH_IDS)).thenReturn(stringsMock);
        when(jobMapperMock.getJobTemplateDetails(anyMap(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getOwner()).thenReturn("administrator");
        final Collection<CmObject> cmObj = new ArrayList<CmObject>();
        when(mapMock.get(ShmConstants.PO_LIST)).thenReturn(cmObj);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, neJobPO, neJobPO, neJobPO, neJobPO, neJobs, neJobPO);
        doThrow(CollectionNotFoundException.class).when(topologyCollectionsEjbServiceMock).getCollectionByID(anyString(), anyString());
        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(jobUpdateServicemock, times(1)).readAndUpdateRunningJobAttributes(anyLong(), null, anyList());
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());

    }

    @Test(expected = Exception.class)
    public void testExecute_thowsExceptionWhenSavedSearchesNotPresent() throws JobConfigurationException, PrivateCollectionException, TopologyCollectionsServiceException {
        setRetryPolicies();
        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        neJobPO.put(ShmConstants.PO_ID, 12337829l);
        neJobPO.put("name", "name");
        neJobPO.put(ShmConstants.JOB_TYPE, "BACKUP");
        final Map<String, Object> poAttributes = poAttributeDetails();
        when(mapMock.get(ShmConstants.NENAMES)).thenReturn(stringsMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(ExecMode.SCHEDULED.getMode());
        when(mapMock.get(ShmConstants.SELECTED_NES)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.COLLECTION_NAMES)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.SAVED_SEARCH_IDS)).thenReturn(stringsMock);
        when(jobMapperMock.getJobTemplateDetails(anyMap(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getOwner()).thenReturn("administrator");
        final Collection<CmObject> cmObj = new ArrayList<CmObject>();
        when(mapMock.get(ShmConstants.PO_LIST)).thenReturn(cmObj);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, neJobPO, neJobPO, neJobPO, neJobPO, neJobs, neJobPO);
        doThrow(Exception.class).when(topologyCollectionsEjbServiceMock).getSavedSearchesByName(anyString(), anyString());
        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(jobUpdateServicemock, times(1)).readAndUpdateRunningJobAttributes(anyLong(), null, anyList());
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());

    }

    @Test
    public void testExecute_NecreatePONull() throws JobConfigurationException {
        setRetryPolicies();
        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        neJobPO.put(ShmConstants.PO_ID, 123456L);
        neJobPO.put("name", "name");
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        final Map<String, Object> poAttributes = poAttributeDetails();
        when(mapMock.get("schedule")).thenReturn(mapMock);
        when(mapMock.get("execMode")).thenReturn("immediate");
        when(mapMock.get("name")).thenReturn("name");
        when(jobMapperMock.getJobTemplateDetails(anyMap(), eq(JOBTEMPLATEID))).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(new NEInfo());
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, null);
        Mockito.doNothing().when(jobUpdateServicemock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "exec" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testExecute_NeSubmittedFailed() throws JobConfigurationException {
        setRetryPolicies();
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        final List<Map<String, String>> mainJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobState = new HashMap<String, String>();
        jobState.put(ShmConstants.KEY, ShmConstants.STATE);
        jobState.put(ShmConstants.VALUE, JobState.SUBMITTED.getJobStateName());
        mainJobProperties.add(jobState);

        final Map<String, Object> neJobPO = new HashMap<String, Object>();
        final Map<String, String> nePlatformType = new HashMap<String, String>();
        nePlatformType.put("selectedNE", "CPP");
        neJobPO.put(ShmConstants.PO_ID, 123456L);
        neJobPO.put("name", "name");

        final Map<String, Object> poAttributes = poAttributeDetails();
        when(mapMock.get("schedule")).thenReturn(mapMock);
        when(mapMock.get("execMode")).thenReturn("immediate");
        when(mapMock.get("name")).thenReturn("name");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttributes, poAttributes, neJobPO);
        when(jobConfigurationService.fetchJobProperty(JOBTEMPLATEID)).thenReturn(mainJobProperties);
        Mockito.doNothing().when(jobUpdateServicemock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);

        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(new NEInfo());

        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(systemRecorderMock, atLeastOnce()).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "exec" + "JOB", "Job",
                "SHM:JOB" + ":Proceeding execution for mainJobId " + JOBTEMPLATEID + " and wfsId " + WFSID);
        verify(jobUpdateServicemock, times(2)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testExecuteWithJobStateNotNull() throws JobConfigurationException {
        setRetryPolicies();
        final Map<String, Object> jobState = new HashMap<String, Object>();
        final List<Activity> activities = new ArrayList<>();
        activities.add(activityMock);
        jobState.put(ShmConstants.STATE, JobState.CREATED.getJobStateName());
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(jobState);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.STATE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn("IMMEDIATE");
        when(jobUpdateServicemock.updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        when(jobMapperMock.getJobTemplateDetails(Matchers.anyMap(), Matchers.anyLong())).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        NEInfo neInfo = new NEInfo();
        neInfo.setCollectionNames(Collections.<String> emptyList());
        neInfo.setSavedSearchIds(Collections.<String> emptyList());
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(neInfo);

        jobExecutionService.execute(WFSID, JOBTEMPLATEID);
        verify(jobUpdateServicemock, times(3)).readAndUpdateRunningJobAttributes(Matchers.eq(JOBTEMPLATEID), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(jobUpdateServicemock, times(1)).retrieveJobWithRetry(Matchers.anyLong());
    }

    @Test
    public void testPrepareMainJob_Running() {
        setRetryPolicies();

        final JobExecutionIndexAndState latestExecutionIndexAndState = new JobExecutionIndexAndState();
        latestExecutionIndexAndState.setJobState(JobState.RUNNING);
        latestExecutionIndexAndState.setJobExecutionIndex(0);
        when(jobTemplateMock.getOwner()).thenReturn("name");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, latestExecutionIndexAndState);

        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobMapperMock.getJobTemplateDetails(new HashMap<String, Object>(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, new Date());
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    @Test
    public void testPrepareMainJob_Cancelling() {

        setRetryPolicies();
        final JobExecutionIndexAndState latestExecutionIndexAndState = new JobExecutionIndexAndState();
        latestExecutionIndexAndState.setJobState(JobState.CANCELLING);
        latestExecutionIndexAndState.setJobExecutionIndex(0);
        when(jobTemplateMock.getOwner()).thenReturn("name");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, latestExecutionIndexAndState);

        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobMapperMock.getJobTemplateDetails(new HashMap<String, Object>(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, new Date());
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    @Test
    public void testPrepareMainJob_System_Cancelling() {
        setRetryPolicies();
        final JobExecutionIndexAndState latestExecutionIndexAndState = new JobExecutionIndexAndState();
        latestExecutionIndexAndState.setJobState(JobState.SYSTEM_CANCELLING);
        latestExecutionIndexAndState.setJobExecutionIndex(0);
        when(jobTemplateMock.getOwner()).thenReturn("name");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, latestExecutionIndexAndState, null);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobMapperMock.getJobTemplateDetails(new HashMap<String, Object>(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, new Date());
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    @Test
    public void testPrepareMainJob_CompletedSchedule() {
        setRetryPolicies();

        final Schedule schedule = scheduleDetails();
        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Activity> activities = activityDetails(schedule);
        new ArrayList<String>();
        final List<NEJobProperty> nEJobPropertyList = new ArrayList<NEJobProperty>();
        final NEJobProperty nEJobProperty = new NEJobProperty();
        final List<JobProperty> jobProperties = new ArrayList<JobProperty>();
        final List<Map<String, String>> mainJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobmap = new HashMap<String, String>();
        mainJobProperties.add(mainJobmap);

        final JobProperty jobProperty = new JobProperty("key", "value");
        nEJobProperty.setNeName("name");
        jobProperties.add(jobProperty);
        nEJobProperty.setJobProperties(jobProperties);
        nEJobPropertyList.add(nEJobProperty);

        final JobExecutionIndexAndState latestExecutionIndexAndState = new JobExecutionIndexAndState();
        latestExecutionIndexAndState.setJobState(JobState.COMPLETED);
        latestExecutionIndexAndState.setJobExecutionIndex(0);
        when(jobTemplateMock.getOwner()).thenReturn("name");

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, latestExecutionIndexAndState, null);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(new NEInfo());
        when(jobConfigurationMock.getJobProperties()).thenReturn(jobProperties);
        when(jobConfigurationMock.getMainSchedule()).thenReturn(schedule);
        when(jobConfigurationMock.getActivities()).thenReturn(activities);
        when(jobConfigurationMock.getNeJobProperties()).thenReturn(nEJobPropertyList);
        when(jobMapperMock.getJobTemplateDetails(new HashMap<String, Object>(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobPropertyBuilderFactory.getProvider(JobType.UPGRADE)).thenReturn(backupJobPropertyProvider);
        when(backupJobPropertyProvider.prepareJobProperties(any(JobConfiguration.class))).thenReturn(mainJobProperties);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, new Date());
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    /*
     * @param schedule
     * 
     * @return
     */
    private List<com.ericsson.oss.services.shm.jobs.common.modelentities.Activity> activityDetails(final Schedule schedule) {
        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Activity> activities = new ArrayList<Activity>();
        final Activity activity = new Activity();
        activity.setName("name");
        activity.setSchedule(schedule);
        activity.setPlatform(PlatformTypeEnum.CPP);
        activity.setNeType("ERBS");
        activities.add(activity);
        return activities;
    }

    /*
     * @return
     */
    private NEInfo nEInfoDetails() {
        final NEInfo selectedNEs = new NEInfo();
        final List<String> neNames = new ArrayList<String>();
        final List<String> collectionNames = new ArrayList<String>();
        final List<String> savedSearchIds = new ArrayList<String>();
        neNames.add("nename");
        selectedNEs.setNeNames(neNames);
        collectionNames.add("collectionName");
        selectedNEs.setCollectionNames(collectionNames);
        savedSearchIds.add("savedSearch");
        selectedNEs.setSavedSearchIds(savedSearchIds);
        return selectedNEs;
    }

    /*
     * @return
     */
    private Schedule scheduleDetails() {
        final List<ScheduleProperty> scheduleAttributes = new ArrayList<ScheduleProperty>();
        final ScheduleProperty scheduleProperty = new ScheduleProperty();
        final Schedule schedule = new Schedule();
        scheduleProperty.setName("name");
        scheduleProperty.setValue("value");
        scheduleAttributes.add(scheduleProperty);
        schedule.setExecMode(ExecMode.IMMEDIATE);
        schedule.setScheduleAttributes(scheduleAttributes);
        return schedule;
    }

    @Test
    public void testPrepareMainJob_NoJobconfiguration() {
        setRetryPolicies();

        final JobExecutionIndexAndState latestExecutionIndexAndState = new JobExecutionIndexAndState();
        latestExecutionIndexAndState.setJobState(JobState.RUNNING);
        latestExecutionIndexAndState.setJobExecutionIndex(0);
        when(jobTemplateMock.getOwner()).thenReturn("name");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, latestExecutionIndexAndState);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(null);
        when(jobMapperMock.getJobTemplateDetails(new HashMap<String, Object>(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, new Date());
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    private Map<String, Object> poAttributeDetails() {
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final Map<String, Object> mainSchedule = new HashMap<String, Object>();
        final Map<String, Object> neInfo = new HashMap<String, Object>();
        final List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        final List<String> neNames = new ArrayList<String>();
        neNames.add("selectedCppNE");
        neNames.add("selectedEcimNE");
        neInfo.put(ShmConstants.NENAMES, neNames);
        neInfo.put(ShmConstants.COLLECTION_NAMES, neNames);
        neInfo.put(ShmConstants.SAVED_SEARCH_IDS, neNames);
        mainSchedule.put(ShmConstants.EXECUTION_MODE, "exec");
        activities.add(mapMock);
        activities.add(mapMock);
        activities.add(mapMock);
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);
        jobConfiguration.put(ShmConstants.SELECTED_NES, neInfo);
        jobConfiguration.put(ShmConstants.ACTIVITIES, activities);
        poAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        poAttributes.put(ShmConstants.JOBTEMPLATEID, 0L);
        poAttributes.put(ShmConstants.JOB_TYPE, "UPGRADE");
        poAttributes.put(ShmConstants.STATE, "CANCELLING");
        poAttributes.put(ShmConstants.STARTTIME, new Date());
        return poAttributes;
    }

    @Test
    public void test_cancelJobs() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "COMPLETED");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
    }

    @Test
    public void test_cancelJobs_JobStateRunning() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "RUNNING");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(true);
        when(jobAdministratorTBACValidator.validateTBACAtJobLevel(anyList(), anyString())).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        final String logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_JOB_LEVEL, SHM_USER);
        verify(systemRecorderMock, times(0)).recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, (String) jobmap.get(ShmConstants.NAME), ShmConstants.JOB, logMessage);
        verify(jobUpdateServicemock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
    }

    @Test
    public void test_cancelJobs_JobStateRunning_WhenTbacFailed() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "RUNNING");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(true);
        when(jobAdministratorTBACValidator.validateTBACAtJobLevel(anyList(), anyString())).thenReturn(false);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        final String logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_JOB_LEVEL, SHM_USER);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, (String) jobmap.get(ShmConstants.NAME), ShmConstants.JOB, logMessage);
        verify(jobUpdateServicemock, times(2)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
    }

    @Test
    public void test_cancelJobs_JobStateRunning_WhenTbacAtJobLevelNotEnabled() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "RUNNING");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(jobAdministratorTBACValidator.validateTBACAtJobLevel(anyList(), anyString())).thenReturn(false);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        final String logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_NE_LEVEL, SHM_USER);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, NODE_NAME, ShmConstants.NE_JOB, logMessage);
        verify(jobUpdateServicemock, times(2)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
    }

    @Test
    public void test_systemCancelJobs() {
        setRetryPolicies();
        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "SYSTEM_CANCELLED");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
    }

    @Test
    public void test_cancelWAIT_FOR_USER_INPUT() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobs.add(jobmap);
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, 123456L, SHM_USER)).thenReturn(true);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, true);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        verify(jobUpdateServicemock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
    }

    @Test
    public void test_cancelWAIT_FOR_USER_INPUT_WhenTbacFailed() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobs.add(jobmap);
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, 123456L, SHM_USER)).thenReturn(false);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, true);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        final String logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_JOB_LEVEL, SHM_USER);
        verify(systemRecorderMock).recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, (String) jobmap.get(ShmConstants.NAME), ShmConstants.JOB, logMessage);
        verify(jobUpdateServicemock, times(2)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
    }

    @Test(expected = WorkflowMessageCorrelationException.class)
    public void test_cancelCreated1() throws WorkflowMessageCorrelationException {
        setRetryPolicies();
        final List<Long> jobIds = new ArrayList<Long>();

        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobs.add(jobmap);
        Mockito.doThrow(WorkflowMessageCorrelationException.class).when(workflowInstanceNotifier).sendMessageToWaitingWFSInstance(Matchers.anyString(), Matchers.anyString());
        final List<Map<String, Object>> jobLogList = getJobLogList();
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, 123456L, SHM_USER)).thenReturn(true);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, true);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
    }

    @Test
    public void test_cancel_whenworkflowSuspended() {
        setRetryPolicies();
        mockWorkflowQuery_suspended("12345");
        final List<Long> jobIds = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.STATE, JobState.CANCELLING.getJobStateName());
        mainJobAttributes.put(ShmConstants.WFS_ID, "12345");
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, JobState.SCHEDULED.getJobStateName());
        jobmap.put(ShmConstants.WFS_ID, "12345");
        jobs.add(jobmap);
        Mockito.doThrow(WorkflowServiceInvocationException.class).when(workflowInstanceNotifier).sendMessageToWaitingWFSInstance(Matchers.anyString(), Matchers.anyString());
        final List<Map<String, Object>> jobLogList = getJobLogList();
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, 123456L, SHM_USER)).thenReturn(true);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOB_ID)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        verify(workflowInstanceNotifier, times(1)).cancelWorkflowInstance("12345");
        verify(jobUpdateServicemock, times(2)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());

    }

    @Test
    public void test_cancel_whenworkflowalive() {
        setRetryPolicies();
        mockWorkflowQuery_alive("12345");
        final List<Long> jobIds = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.STATE, JobState.CANCELLING.getJobStateName());
        mainJobAttributes.put(ShmConstants.WFS_ID, "12345");
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, JobState.SCHEDULED.getJobStateName());
        jobmap.put(ShmConstants.WFS_ID, "12345");
        jobs.add(jobmap);
        Mockito.doThrow(WorkflowServiceInvocationException.class).when(workflowInstanceNotifier).sendMessageToWaitingWFSInstance(Matchers.anyString(), Matchers.anyString());
        final List<Map<String, Object>> jobLogList = getJobLogList();
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, 123456L, SHM_USER)).thenReturn(true);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOB_ID)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        verify(workflowInstanceNotifier, times(1)).cancelWorkflowInstance("12345");
        verify(jobUpdateServicemock, times(2)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());

    }

    @Test
    public void test_cancel_whenworkflowthrowsEcxeption() {
        setRetryPolicies();
        mockWorkflowQuery_throwException("12345");
        final List<Long> jobIds = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.STATE, JobState.CANCELLING.getJobStateName());
        mainJobAttributes.put(ShmConstants.WFS_ID, "12345");
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, JobState.SCHEDULED.getJobStateName());
        jobmap.put(ShmConstants.WFS_ID, "12345");
        jobs.add(jobmap);
        Mockito.doThrow(WorkflowServiceInvocationException.class).when(workflowInstanceNotifier).sendMessageToWaitingWFSInstance(Matchers.anyString(), Matchers.anyString());
        final List<Map<String, Object>> jobLogList = getJobLogList();
        when(jobAdministratorTBACValidator.validateTBACForMainJob(JOB_ID, 123456L, SHM_USER)).thenReturn(true);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOB_ID)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
        verify(workflowInstanceNotifier, times(1)).cancelWorkflowInstance("12345");
        verify(jobUpdateServicemock, times(2)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());

    }

    @Test
    public void test_cancelCreated() {
        setRetryPolicies();
        final List<Long> jobIds = new ArrayList<Long>();

        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.STATE, "CREATED");
        jobs.add(jobmap);
        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Map<String, Object>> neJobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJob = new HashMap<String, Object>();
        neJob.put(ShmConstants.PO_ID, 123456L);
        neJob.put(ShmConstants.WFS_ID, 123456L);
        neJob.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
        neJobs.add(neJob);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs, neJobs);
        Mockito.doNothing().when(workflowInstanceNotifier).sendAsynchronousMsgToWaitingWFSInstance(Matchers.anyString(), Matchers.anyString());
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
    }

    @Test
    public void test_cancelJobsCompleted() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.NAMESPACE, "shms");
        jobmap.put(ShmConstants.STATE, "COMPLETED");
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
    }

    @Test
    public void test_cancelJobsNoMainJob() {
        setRetryPolicies();

        final List<Map<String, Object>> jobLogList = getJobLogList();
        final List<Long> jobIds = new ArrayList<Long>();
        jobIds.add(123456L);
        final List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobmap = getJobdetails();
        jobmap.put(ShmConstants.NAMESPACE, "shms");
        jobmap.put(ShmConstants.STATE, "RUNNING");
        jobmap.put(ShmConstants.WFS_ID, 123456L);
        jobs.add(jobmap);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobs);
        Mockito.doNothing().when(workflowInstanceNotifier).sendAsynchronousMsgToWaitingWFSInstance(Matchers.anyString(), Matchers.anyString());
        when(jobUpdateServicemock.updateRunningJobAttributes(123456L, null, jobLogList)).thenReturn(true);
        jobExecutionService.cancelJobs(jobIds, SHM_USER);
    }

    @Test
    public void test_PrepareMainJob_withStateNull() {

        setRetryPolicies();
        stringsMock.add("precheck");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, null);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(mapMock);
        when(jobMapperMock.getJobTemplateDetails(mapMock, JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(executorServiceHelper.getLatestJobExecutionIndexAndState(JOBTEMPLATEID)).thenReturn(null);
        when(jobConfigurationMock.getMainSchedule()).thenReturn(scheduleMock);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(NEInfoMock);
        when(jobConfigurationMock.getNeJobProperties()).thenReturn(Arrays.asList(NEJobPropertyMock));
        when(jobConfigurationMock.getJobProperties()).thenReturn(Arrays.asList(jobPropertyMock));
        when(jobConfigurationMock.getActivities()).thenReturn(Arrays.asList(activityMock));
        when(activityMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(activityMock.getName()).thenReturn("precheck");
        when(activityMock.getSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(scheduleMock.getScheduleAttributes()).thenReturn(Arrays.asList(schedulePropertyMock));
        when(schedulePropertyMock.getName()).thenReturn(ShmConstants.START_DATE);
        when(schedulePropertyMock.getValue()).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(jobPropertyBuilderFactory.getProvider(JobType.BACKUP)).thenReturn(backupJobPropertyProvider);
        when(executorServiceHelper.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.PO_ID)).thenReturn(-1l);

        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, null);
        assertNotNull(DEFAULT_MAIN_JOB_ID);
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    @Test
    public void testPrepareMainJob_PoCreated() {
        setRetryPolicies();
        final JobExecutionIndexAndState latestExecutionIndexAndState = new JobExecutionIndexAndState();
        latestExecutionIndexAndState.setJobState(JobState.COMPLETED);
        latestExecutionIndexAndState.setJobExecutionIndex(0);
        when(jobMapperMock.getJobTemplateDetails(new HashMap<String, Object>(), JOBTEMPLATEID)).thenReturn(jobTemplateMock);
        when(jobTemplateMock.getOwner()).thenReturn("Name");
        final Map<String, Object> jobPO = new HashMap<String, Object>();
        jobPO.put(ShmConstants.PO_ID, 123456L);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfigurationMock);
        when(jobConfigurationMock.getSelectedNEs()).thenReturn(nEInfoDetails());
        when(jobConfigurationMock.getJobProperties()).thenReturn(Arrays.asList(jobPropertyMock));
        when(jobConfigurationMock.getMainSchedule()).thenReturn(scheduleDetails());
        when(jobConfigurationMock.getActivities()).thenReturn(activityDetails(scheduleDetails()));
        when(jobConfigurationMock.getNeJobProperties()).thenReturn(Arrays.asList(NEJobPropertyMock));
        when(jobConfigurationMock.getPlatformJobProperties()).thenReturn(getPlatformJobProperties());
        when(jobConfigurationMock.getNeTypeJobProperties()).thenReturn(getNeTypeJobProperties());

        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobTemplateMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, latestExecutionIndexAndState, jobPO);
        when(jobPropertyBuilderFactory.getProvider(JobType.UPGRADE)).thenReturn(backupJobPropertyProvider);
        when(backupJobPropertyProvider.prepareJobProperties(any(JobConfiguration.class))).thenReturn(listofMapWithStringMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, new Date());
        assertEquals(DEFAULT_MAIN_JOB_ID, 123456L);
    }

    @Test
    public void testPrepareMainJob_NullJobTemplateAttributes() {
        setRetryPolicies();
        final Map<String, Object> jobTemplateAttributes = null;
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobTemplateAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(mapMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, null);
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    @Test
    public void testPrepareMainJob_EmptyJobTemplateAttributes() {
        setRetryPolicies();
        final Map<String, Object> jobTemplateAttributes = new HashMap<String, Object>();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobTemplateAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(mapMock);
        final long DEFAULT_MAIN_JOB_ID = jobExecutionService.prepareMainJob(WFSID, JOBTEMPLATEID, null);
        assertEquals(DEFAULT_MAIN_JOB_ID, -1);
    }

    @Test
    public void testGetUnsupportedNes() {
        final NetworkElement cppNetworkElement1 = new NetworkElement();
        cppNetworkElement1.setNeType("ERBS");
        cppNetworkElement1.setName("selectedCppNE");
        cppNetworkElement1.setPlatformType(PlatformTypeEnum.CPP);

        final NetworkElement mlNetworkElement1 = new NetworkElement();
        mlNetworkElement1.setNeType("MINI");
        mlNetworkElement1.setName("selectedMLINE");
        mlNetworkElement1.setPlatformType(PlatformTypeEnum.MINI_LINK_INDOOR);

        final NetworkElement ecimNetworkElement1 = new NetworkElement();
        ecimNetworkElement1.setNeType("SGSN-MME");
        ecimNetworkElement1.setName("selectedEcimNE");
        ecimNetworkElement1.setPlatformType(PlatformTypeEnum.ECIM);

        final NetworkElement cppNetworkElement2 = new NetworkElement();
        cppNetworkElement2.setNeType("ERBS");
        cppNetworkElement2.setName("selectedCppNE");
        cppNetworkElement2.setPlatformType(PlatformTypeEnum.CPP);

        final NetworkElement mlNetworkElement2 = new NetworkElement();
        mlNetworkElement2.setNeType("MINI");
        mlNetworkElement2.setName("selectedMLINE");
        mlNetworkElement2.setPlatformType(PlatformTypeEnum.MINI_LINK_INDOOR);

        final NetworkElement ecimNetworkElement2 = new NetworkElement();
        ecimNetworkElement2.setNeType("SGSN-MME");
        ecimNetworkElement2.setName("selectedEcimNE");
        ecimNetworkElement2.setPlatformType(PlatformTypeEnum.ECIM);

        final Map<NetworkElement, String> unsupportedNEMap1 = new HashMap<NetworkElement, String>();
        unsupportedNEMap1.put(ecimNetworkElement1, "skipped");
        unsupportedNEMap1.put(cppNetworkElement1, "skipped");
        unsupportedNEMap1.put(mlNetworkElement1, "skipped");

        final Map<NetworkElement, String> unsupportedNEMap2 = new HashMap<NetworkElement, String>();
        unsupportedNEMap2.put(ecimNetworkElement2, "skipped");
        unsupportedNEMap2.put(cppNetworkElement2, "skipped");
        unsupportedNEMap2.put(mlNetworkElement2, "skipped");

        final List<Map<NetworkElement, String>> unsupportedNEsList = new ArrayList<Map<NetworkElement, String>>();
        unsupportedNEsList.add(unsupportedNEMap1);
        unsupportedNEsList.add(unsupportedNEMap2);

        assertEquals(jobExecutionService.getUnsupportedNes(unsupportedNEsList).size(), 6);
    }

    private List<PlatformJobProperty> getPlatformJobProperties() {
        final PlatformJobProperty platformJobProperty = new PlatformJobProperty();
        final List<PlatformJobProperty> platformJobPropertyList = new ArrayList<PlatformJobProperty>();
        platformJobProperty.setPlatform("platform");
        platformJobProperty.setJobProperties(Arrays.asList(jobPropertyMock));
        platformJobPropertyList.add(platformJobProperty);
        return platformJobPropertyList;
    }

    private List<NeTypeJobProperty> getNeTypeJobProperties() {
        final NeTypeJobProperty NeTypeJobProperty = new NeTypeJobProperty();
        final List<NeTypeJobProperty> NeTypeJobPropertyList = new ArrayList<NeTypeJobProperty>();
        NeTypeJobProperty.setNeType("neType");
        NeTypeJobProperty.setJobProperties(Arrays.asList(jobPropertyMock));
        NeTypeJobPropertyList.add(NeTypeJobProperty);
        return NeTypeJobPropertyList;
    }

    /*
     * @return
     */
    private List<Map<String, Object>> getJobLogList() {
        final Map<String, Object> cancelJobSkipped = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobExecutorConstants.JOB_CANCEL_SKIPPED, "JOBNAME"));
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        jobLogList.add(cancelJobSkipped);
        return jobLogList;
    }

    /*
     * @return
     */
    private Map<String, Object> getJobdetails() {
        final Map<String, Object> jobmap = new HashMap<String, Object>();
        jobmap.put(ShmConstants.NAMESPACE, ShmConstants.NAMESPACE);
        jobmap.put(ShmConstants.TYPE, ShmConstants.JOB);
        jobmap.put(ShmConstants.STATE, "WAIT_FOR_USER_INPUT");
        jobmap.put(ShmConstants.PO_ID, 123456L);
        jobmap.put(ShmConstants.JOBNAME, "JOBNAME");
        jobmap.put(ShmConstants.JOB_TEMPLATE_ID, 123456L);
        jobmap.put(ShmConstants.NE_NAME, "nodeName");
        return jobmap;
    }

    private Map<String, Object> getNEJobdetails() {
        final Map<String, Object> jobmap = new HashMap<String, Object>();
        jobmap.put(ShmConstants.NAMESPACE, ShmConstants.NAMESPACE);
        jobmap.put(ShmConstants.TYPE, ShmConstants.NE_JOB);
        jobmap.put(ShmConstants.STATE, "WAIT_FOR_USER_INPUT");
        jobmap.put(ShmConstants.PO_ID, 123456L);
        jobmap.put(ShmConstants.JOBNAME, "JOBNAME");
        jobmap.put(ShmConstants.JOB_TEMPLATE_ID, 123456L);
        jobmap.put(ShmConstants.NE_NAME, "nodeName");
        return jobmap;
    }

    private Map<String, Object> getJobTemplateDetails() {
        final Map<String, Object> jobTemplate = new HashMap<String, Object>();
        jobTemplate.put(ShmConstants.NAME, ShmConstants.NAME + 1);

        return jobTemplate;
    }

    private void setRetryPolicies() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.MILLISECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(wfsRetryConfigurationParamProvider.getWfsRetryCount()).thenReturn(2);
        when(wfsRetryConfigurationParamProvider.getWfsWaitIntervalInMS()).thenReturn(1000);
    }

    private Map<String, Object> prepareJobLogs(final String logMessage, final Date notificationTime, final String logLevel) {
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, notificationTime);
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        return logEntry;
    }

    private NetworkElementResponse getNetworkElementsResponse() {
        final NetworkElementResponse networkElementResponse1 = new NetworkElementResponse();
        networkElementResponse1.setSupportedNes(getNetworkElementList());
        return networkElementResponse1;

    }

    private List<NetworkElement> getNetworkElementList() {
        final List<NetworkElement> neList = new ArrayList<>();
        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType("ERBS");
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        neList.add(cppNetworkElement);
        return neList;

    }

    private JobTemplate getJobTemplateDetails1() {
        Date creationTime = new Date();
        creationTime.setTime(2018);
        JobTemplate jobTemplate = new JobTemplate();
        jobTemplate.setJobConfigurationDetails(getJobConfigurationDetails());
        jobTemplate.setJobType(JobType.BACKUP);
        jobTemplate.setName("Backupjob");
        jobTemplate.setOwner("Adminisrator");
        jobTemplate.setJobCategory(JobCategory.UI);
        jobTemplate.setJobTemplateId(123L);
        jobTemplate.setCreationTime(creationTime);
        return jobTemplate;
    }

    private JobConfiguration getJobConfigurationDetails() {
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setActivities(getActivities());
        jobConfiguration.setMainSchedule(getSchedule());
        jobConfiguration.setSelectedNEs(getNEInfo());
        return jobConfiguration;
    }

    private NEInfo getNEInfo() {
        NEInfo neInfo = new NEInfo();
        neInfo.setNeNames(Arrays.asList("selectedCppNE", "selectedECIMNE"));
        return neInfo;
    }

    private List<Activity> getActivities() {
        List<Activity> activityList = new ArrayList<>();
        Activity activity = new Activity();
        activity.setName("create");
        activity.setNeType("ERBS");
        activity.setOrder(1);
        activity.setPlatform(PlatformTypeEnum.CPP);
        activity.setSchedule(getSchedule());
        activityList.add(activity);
        return activityList;
    }

    private Schedule getSchedule() {
        Schedule schedule = new Schedule();
        schedule.setExecMode(ExecMode.IMMEDIATE);
        schedule.setScheduleAttributes(getScheduleProperety());
        return schedule;
    }

    private List<ScheduleProperty> getScheduleProperety() {
        List<ScheduleProperty> schedulePropertyList = new ArrayList<>();
        ScheduleProperty scheduleProperty = new ScheduleProperty();
        scheduleProperty.setName("");
        scheduleProperty.setValue("");
        schedulePropertyList.add(scheduleProperty);
        return schedulePropertyList;

    }

    private TBACResponse prepareTbacResponse(final List<NetworkElement> unAuthorizedNes) {
        TBACResponse tBACResponse1 = new TBACResponse();
        tBACResponse1.setTBACValidationSuccess(true);
        tBACResponse1.setTbacValidationToBeDoneForAllNodesAsSingleTarget(true);
        tBACResponse1.setUnAuthorizedNes(unAuthorizedNes);
        return tBACResponse1;
    }

    private Map<String, Object> prepareMainJobAttributes(final long jobTemplateId, final Map<String, Object> jobConfiguration) {
        Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.STATE, JobState.RUNNING.toString());
        mainJobAttributes.put(ShmConstants.JOBTEMPLATEID, jobTemplateId);
        mainJobAttributes.put(ShmConstants.STARTTIME, new Date());
        mainJobAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        return mainJobAttributes;
    }

    @Test
    public void test_supportedAndUnSupportedNesCountNHC() {
        final List<String> neNames = new ArrayList<>();
        neNames.add("MSC07");
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        neNames.add("LTE05ERBS00001");
        neNames.add("LTE05ERBS00002");
        final String jobType = "NODE_HEALTH_CHECK";
        final List<Map<String, Object>> nesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        final NetworkElementResponse nhcnetworkElementsResponse = getNetworkElementResponse(jobType);
        when(executorServiceHelper.getSupportedAndUnSupportedNetworkElementDetails(neNames, jobTypeEnum, nesWithComponentInfo, neDetailsWithParentName)).thenReturn(nhcnetworkElementsResponse);
        final Map<String, Object> supportedUnsupportedMap = jobExecutionService.getSupportedNes(neNames, jobTypeEnum);
        assertEquals(((List<NetworkElement>) supportedUnsupportedMap.get(ShmConstants.SUPPORTED_NES)).size(), 2);
        assertEquals(((List<NetworkElement>) supportedUnsupportedMap.get(ShmConstants.UNSUPPORTED_NES)).size(), 3);
    }

    @Test
    public void test_supportedAndUnSupportedNesCountUPGRADE() {
        final List<String> neNames = new ArrayList<>();
        neNames.add("MSC07");
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        neNames.add("LTE05ERBS00001");
        neNames.add("LTE05ERBS00002");
        final String jobType = "UPGRADE";
        final List<Map<String, Object>> nesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        final NetworkElementResponse upgradenetworkElementsResponse = getNetworkElementResponse(jobType);
        when(executorServiceHelper.getSupportedAndUnSupportedNetworkElementDetails(neNames, jobTypeEnum, nesWithComponentInfo, neDetailsWithParentName)).thenReturn(upgradenetworkElementsResponse);
        final Map<String, Object> upgradesupportedUnsupportedMap = jobExecutionService.getSupportedNes(neNames, jobTypeEnum);
        assertEquals(((List<NetworkElement>) upgradesupportedUnsupportedMap.get(ShmConstants.SUPPORTED_NES)).size(), 5);
        assertEquals(((List<NetworkElement>) upgradesupportedUnsupportedMap.get(ShmConstants.UNSUPPORTED_NES)).size(), 0);
    }

    @Test
    public void test_supportedAndUnSupportedNesCountRESTORE() {
        final List<String> neNames = new ArrayList<>();
        neNames.add("MSC07");
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        neNames.add("LTE05ERBS00001");
        neNames.add("LTE05ERBS00002");
        final String jobType = "RESTORE";
        final List<Map<String, Object>> nesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        final NetworkElementResponse restorenetworkElementsResponse = getNetworkElementResponse(jobType);
        when(executorServiceHelper.getSupportedAndUnSupportedNetworkElementDetails(neNames, jobTypeEnum, nesWithComponentInfo, neDetailsWithParentName)).thenReturn(restorenetworkElementsResponse);
        final Map<String, Object> restoresupportedUnsupportedMap = jobExecutionService.getSupportedNes(neNames, jobTypeEnum);
        assertEquals(((List<NetworkElement>) restoresupportedUnsupportedMap.get(ShmConstants.SUPPORTED_NES)).size(), 4);
        assertEquals(((List<NetworkElement>) restoresupportedUnsupportedMap.get(ShmConstants.UNSUPPORTED_NES)).size(), 1);
    }

    @Test
    public void test_supportedAndUnSupportedNesCountNODERESTART() {
        final List<String> neNames = new ArrayList<>();
        neNames.add("MSC07");
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        neNames.add("LTE05ERBS00001");
        neNames.add("LTE05ERBS00002");
        final String jobType = "NODERESTART";
        final List<Map<String, Object>> nesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        final NetworkElementResponse restartenetworkElementsResponse = getNetworkElementResponse(jobType);
        when(executorServiceHelper.getSupportedAndUnSupportedNetworkElementDetails(neNames, jobTypeEnum, nesWithComponentInfo, neDetailsWithParentName)).thenReturn(restartenetworkElementsResponse);
        final Map<String, Object> restartsupportedUnsupportedMap = jobExecutionService.getSupportedNes(neNames, jobTypeEnum);
        assertEquals(((List<NetworkElement>) restartsupportedUnsupportedMap.get(ShmConstants.SUPPORTED_NES)).size(), 2);
        assertEquals(((List<NetworkElement>) restartsupportedUnsupportedMap.get(ShmConstants.UNSUPPORTED_NES)).size(), 3);
    }

    private NetworkElementResponse getNetworkElementResponse(final String jobType) {
        final List<NetworkElement> supportedNeList = new ArrayList<>();
        final Map<NetworkElement, String> unsupportedNes = new HashMap<>();
        final String invalidNe = "This is invalid ne";
        NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        if (jobType.equals("NODE_HEALTH_CHECK")) {
            supportedNeList.add(getNetworkElements().get("ecim1"));
            supportedNeList.add(getNetworkElements().get("ecim2"));
            unsupportedNes.put(getNetworkElements().get("cpp1"), invalidNe);
            unsupportedNes.put(getNetworkElements().get("cpp2"), invalidNe);
            unsupportedNes.put(getNetworkElements().get("axe1"), invalidNe);
            networkElementResponse.setSupportedNes(supportedNeList);
            networkElementResponse.setUnsupportedNes(unsupportedNes);
        } else if (jobType.equals("UPGRADE")) {
            supportedNeList.add(getNetworkElements().get("ecim1"));
            supportedNeList.add(getNetworkElements().get("ecim2"));
            supportedNeList.add(getNetworkElements().get("cpp1"));
            supportedNeList.add(getNetworkElements().get("cpp2"));
            supportedNeList.add(getNetworkElements().get("axe1"));
            networkElementResponse.setSupportedNes(supportedNeList);
            networkElementResponse.setUnsupportedNes(unsupportedNes);
        } else if (jobType.equals("RESTORE")) {
            supportedNeList.add(getNetworkElements().get("ecim1"));
            supportedNeList.add(getNetworkElements().get("ecim2"));
            supportedNeList.add(getNetworkElements().get("cpp1"));
            supportedNeList.add(getNetworkElements().get("cpp2"));
            unsupportedNes.put(getNetworkElements().get("axe1"), invalidNe);
            networkElementResponse.setSupportedNes(supportedNeList);
            networkElementResponse.setUnsupportedNes(unsupportedNes);
        } else if (jobType.equals("NODERESTART")) {
            supportedNeList.add(getNetworkElements().get("cpp1"));
            supportedNeList.add(getNetworkElements().get("cpp2"));
            unsupportedNes.put(getNetworkElements().get("ecim1"), invalidNe);
            unsupportedNes.put(getNetworkElements().get("ecim2"), invalidNe);
            unsupportedNes.put(getNetworkElements().get("axe1"), invalidNe);
            networkElementResponse.setSupportedNes(supportedNeList);
            networkElementResponse.setUnsupportedNes(unsupportedNes);
        }
        return networkElementResponse;
    }

    private Map<String, NetworkElement> getNetworkElements() {
        final Map<String, NetworkElement> networkElements = new HashMap<>();
        final NetworkElement ecimNetworkElement1 = new NetworkElement();
        ecimNetworkElement1.setNeType("ECIM");
        ecimNetworkElement1.setName("LTE06dg2ERBS00001");
        ecimNetworkElement1.setPlatformType(PlatformTypeEnum.ECIM);
        final NetworkElement ecimNetworkElement2 = new NetworkElement();
        ecimNetworkElement2.setNeType("ECIM");
        ecimNetworkElement2.setName("LTE06dg2ERBS00002");
        ecimNetworkElement2.setPlatformType(PlatformTypeEnum.ECIM);
        NetworkElementResponse networkElementResponse = new NetworkElementResponse();
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
        networkElements.put("ecim1", ecimNetworkElement1);
        networkElements.put("ecim2", ecimNetworkElement2);
        networkElements.put("cpp1", cppNetworkElement1);
        networkElements.put("cpp2", cppNetworkElement2);
        networkElements.put("axe1", gsmNetworkElement1);
        return networkElements;
    }

    @Test
    public void testExecuteForDuplicateNeJobs() {
        final long mainJobId = 1L;
        final String wfsId = "123";
        final long jobTemplateId = 2L;
        final long neJobId = 3L;

        final Map<String, Object> jobTempalteAttributes = new HashMap<>();
        final List<NetworkElement> unAuthorizedNes = new ArrayList<>();
        setRetryPolicies();

        Map<String, Object> mainSchedule = new HashMap<>();
        mainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());

        final Map<String, Object> jobConfiguration = new HashMap<>();
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);

        final Map<String, Object> mainJobAttributes = prepareMainJobAttributes(jobTemplateId, jobConfiguration);

        when(jobUpdateServicemock.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(jobTempalteAttributes);
        JobTemplate jobTemplateDetails = getJobTemplateDetails1();
        when(jobMapperMock.getJobTemplateDetails(mainJobAttributes, jobTemplateId)).thenReturn(jobTemplateDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        mainJobAttributes.put(ShmConstants.PO_ID, neJobId);

        TBACResponse tBACResponse1 = prepareTbacResponse(unAuthorizedNes);

        NetworkElementResponse networkElementResponse = getNetworkElementsResponse();
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        when(executorServiceHelper.getNetworkElementDetails(mainJobId, jobTemplateDetails, jobProperties, neDetailsWithParentName)).thenReturn(networkElementResponse);
        when(tbacValidator.validateTBAC(networkElementResponse, jobTemplateDetails, mainJobId, mainJobAttributes)).thenReturn(tBACResponse1);

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mainJobAttributes).thenReturn(mainJobAttributes).thenReturn(mainJobAttributes)
                .thenReturn(mainJobAttributes);

        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType(NODE_TYPE);
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final NetworkElement cppNetworkElemnt = new NetworkElement();
        cppNetworkElemnt.setNeType(NODE_TYPE);
        cppNetworkElemnt.setName("selectedCppNE2");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);
        neList.add(cppNetworkElemnt);

        final Map<NetworkElement, String> unSupportedNetworkElements = new HashMap<>();
        final Map<String, Object> creationDetails = new HashMap<>();
        creationDetails.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);

        when(builderRetryProxy.createNeJob(Matchers.anyLong(), Matchers.anyString(), Matchers.any(NetworkElement.class), Matchers.anyMap(), Matchers.anyMap())).thenReturn(creationDetails);
        final List<Map<String, Object>> existingNeJobs = new ArrayList<>();
        Map<String, Object> neMap = new HashMap<>();
        neMap.put(ShmConstants.NE_NAME, "selectedCppNE2");
        existingNeJobs.add(neMap);
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenReturn(existingNeJobs);
        when(executorServiceHelper.getFilteredSupportedNodes(anyList(), anyList())).thenReturn(neList);
        when(workflowInstanceNotifier.sendAllNeDone(anyString())).thenReturn(true);
        when(executorServiceHelper.getFilteredUnSupportedNodes(anyMap(), anyList())).thenReturn(unSupportedNetworkElements);
        when(supportedNesListBuilder.buildSupportedNesListForNeJobsCreation(networkElementResponse.getNesWithComponents(), neList)).thenReturn(neList);
        final String logMessage = String.format("NE level Jobs created and submitted for execution for nodes: [selectedCppNE]");
        final Map<String, String> selectedNeWithParentNameMap = new HashMap<>();
        selectedNeWithParentNameMap.put(ShmConstants.NE_NAME, "Parent_selectedCppNE");
        when(neComponentsInfoBuilderImpl.findParentNeNameForSelectedNe(Matchers.any(NetworkElement.class), Matchers.anyMap())).thenReturn(selectedNeWithParentNameMap);
        jobExecutionService.execute(wfsId, mainJobId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, "IMMEDIATEJOB", "Job",
                "SHM:JOB:Proceeding execution for mainJobId " + mainJobId + " and wfsId " + wfsId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.NEJOBS_SUBMITTED, EventLevel.COARSE, "mainJobId:" + mainJobId, "wfsId:" + wfsId, logMessage);

    }

    @Test
    public void testExecuteForDuplicateNeJobs_Exception() {
        final long mainJobId = 1L;
        final String wfsId = "123";
        final long jobTemplateId = 2L;
        final long neJobId = 3L;

        final Map<String, Object> jobTempalteAttributes = new HashMap<>();
        final List<NetworkElement> unAuthorizedNes = new ArrayList<>();
        setRetryPolicies();

        Map<String, Object> mainSchedule = new HashMap<>();
        mainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());

        final Map<String, Object> jobConfiguration = new HashMap<>();
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);

        final Map<String, Object> mainJobAttributes = prepareMainJobAttributes(jobTemplateId, jobConfiguration);

        when(jobUpdateServicemock.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttributes);
        when(jobUpdateServicemock.retrieveJobWithRetry(JOBTEMPLATEID)).thenReturn(jobTempalteAttributes);
        JobTemplate jobTemplateDetails = getJobTemplateDetails1();
        when(jobMapperMock.getJobTemplateDetails(mainJobAttributes, jobTemplateId)).thenReturn(jobTemplateDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        mainJobAttributes.put(ShmConstants.PO_ID, neJobId);

        TBACResponse tBACResponse1 = prepareTbacResponse(unAuthorizedNes);

        NetworkElementResponse networkElementResponse = getNetworkElementsResponse();
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        when(executorServiceHelper.getNetworkElementDetails(mainJobId, jobTemplateDetails, jobProperties, neDetailsWithParentName)).thenReturn(networkElementResponse);
        when(tbacValidator.validateTBAC(networkElementResponse, jobTemplateDetails, mainJobId, mainJobAttributes)).thenReturn(tBACResponse1);

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mainJobAttributes).thenReturn(mainJobAttributes).thenReturn(mainJobAttributes)
                .thenReturn(mainJobAttributes);

        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType(NODE_TYPE);
        cppNetworkElement.setName("selectedCppNE");
        cppNetworkElement.setPlatformType(PlatformTypeEnum.CPP);
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);
        final Map<NetworkElement, String> unSupportedNetworkElements = new HashMap<>();
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyList())).thenThrow(Exception.class);
        when(executorServiceHelper.getFilteredSupportedNodes(anyList(), anyList())).thenReturn(neList);
        when(workflowInstanceNotifier.sendAllNeDone(anyString())).thenReturn(true);
        when(executorServiceHelper.getFilteredUnSupportedNodes(anyMap(), anyList())).thenReturn(unSupportedNetworkElements);
        when(supportedNesListBuilder.buildSupportedNesListForNeJobsCreation(networkElementResponse.getNesWithComponents(), neList)).thenReturn(neList);
        jobExecutionService.execute(wfsId, mainJobId);
        final String logMessage = String.format("NEjob creation failed for the selected nodes: \"%s\" for mainJobId: \"%s\"", neList.get(0), mainJobId);
        jobExecutionService.execute(wfsId, mainJobId);
        verify(systemRecorderMock, times(2)).recordEvent(SHMEvents.NEJOBS_CREATION_FAILED, EventLevel.COARSE, "mainJobId:" + mainJobId, "wfsId:" + wfsId, logMessage);
    }

    private void mockWorkflowQuery_suspended(final String wfsId) {
        when(queryBuilderMock.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY)).thenReturn(wfsQueryMock);
        when(wfsQueryMock.getRestrictionBuilder()).thenReturn(restrictionBuilderMock);
        when(restrictionBuilderMock.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId)).thenReturn(restrictionMock);
        wfsQueryMock.setRestriction(restrictionMock);
    }

    private void mockWorkflowQuery_alive(final String wfsId) {
        List<WorkflowObject> listWrk = new ArrayList<WorkflowObject>();
        listWrk.add(workFlowObject);
        when(queryBuilderMock.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY)).thenReturn(wfsQueryMock);
        when(wfsQueryMock.getRestrictionBuilder()).thenReturn(restrictionBuilderMock);
        when(restrictionBuilderMock.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId)).thenReturn(restrictionMock);
        wfsQueryMock.setRestriction(restrictionMock);
        when(workflowInstanceNotifier.executeWorkflowQuery(any(Query.class))).thenReturn(listWrk);
    }

    private void mockWorkflowQuery_throwException(final String wfsId) {
        List<WorkflowObject> listWrk = new ArrayList<WorkflowObject>();
        listWrk.add(workFlowObject);
        when(queryBuilderMock.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY)).thenReturn(wfsQueryMock);
        when(wfsQueryMock.getRestrictionBuilder()).thenReturn(restrictionBuilderMock);
        when(restrictionBuilderMock.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId)).thenReturn(restrictionMock);
        wfsQueryMock.setRestriction(restrictionMock);
        when(workflowInstanceNotifier.executeWorkflowQuery(any(Query.class))).thenThrow(Exception.class);
    }

}
