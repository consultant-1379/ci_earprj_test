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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.common.exception.MoActionRetryException;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepDurationsReportGenerator;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobConsolidationService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.constants.SchedulePropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.loadcontrol.local.api.LoadControllerLocalCache;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

import javax.ejb.EJBException;


@RunWith(PowerMockRunner.class)
@PrepareForTest({JobPropertyUtil.class, JobConsolidationService.class, ServiceFinderBean.class, JobStatusServiceImpl.class, RetryPolicy.class})
public class JobStatusServiceImplTest {

    @InjectMocks
    private JobStatusServiceImpl objectUnderTest;

    @Mock
    private PersistenceObject persistenceObjectMock;

    @Mock
    private DpsWriter dpsWriterMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private JobConfigurationService jobConfigurationServiceMock;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private MainJobsProgressUpdateService mainJobProgressNotifierMock;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    private RetryManager retryManagerMock;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private Map<String, Object> neJobAttrs;

    @Mock
    private Map<String, Object> mainJobAttrs;

    @Mock
    private ActivityStepDurationsReportGenerator activityStepDurationsReportGeneratorMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private MainJobProgressUpdaterRetryProxy mainJobProgressNotifier;

    @Mock
    private NeJobStaticDataProvider jobStaticDataCache;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private LoadControllerLocalCache loadControllerLocalCache;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private NetworkElementData networkElementInfo;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Mock
    private JobStaticDataProvider jobStaticDataCacheMock;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private NeJobDetailsInstrumentation neJobDetailsInstrumentation;

    @Mock
    private FaBuildingBlockResponseProcessor faBuildingBlockResponseProcessor;

    @Mock
    private ServiceFinderBean serviceFinderBeanMock;

    @Mock
    private JobConsolidationService jobConsolidationService;

    @Mock
    private DpsReader dpsReaderMock;

    @Mock
    RetryPolicy.RetryPolicyBuilder retryPolicyBuilderMock;

    private final long jobId = 1l;
    private final long mainJobId = 358467591;
    private final String wfsId = "5129e3a2-cf9f-11e9-8902-525400ecdd5e";
    private final long jobTemplateId = 987456122;
    private final String activityname = "verify";
    private static final String NODE_FDN = "NetworkElement=node1";
    private final Class<? extends Exception>[] exceptionsArray = new Class[]{DatabaseNotAvailableException.class, EJBException.class, MoActionRetryException.class};


    @Test
    public void test_updateJob() {

        objectUnderTest.updateJob(jobId, mapMock);
        verify(jobUpdateServiceMock, times(1)).updateJobAttributes(jobId, mapMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateJobStart() {
        objectUnderTest.updateNeJobStart(jobId, JobState.RUNNING);
        verify(jobUpdateServiceMock, times(1)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
    }

    @Test
    public void test_recordJobEndEvent() {

        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(ExecMode.IMMEDIATE.getMode());
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);
        objectUnderTest.recordJobEndEvent(jobId);
        verify(systemRecorderMock, times(1)).recordEvent(SHMEvents.JOB_END, EventLevel.COARSE, "ImmediateJOB", "Job", "SHM:JOB" + ":execution ending for mainJobId " + jobId);
    }

    @Test
    public void test_updateNEJobEnd() {

        objectUnderTest.updateNEJobEnd(jobId);
        verify(jobUpdateServiceMock, times(1)).updateNEJobEndAttributes(jobId);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_updateActivityJobEnd_withExecutionSuccess() throws JobDataNotFoundException {

        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(jobId);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.NAME)).thenReturn("DummyName");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(ActivityConstants.ACTIVITY_RESULT);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(JobResult.SUCCESS.getJobResult());
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(jobId);
        when(jobConfigurationServiceMock.getActivitiesCount(jobId)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.TOTAL_ACTIVITIES)).thenReturn(2);
        when(mapMock.get(ActivityConstants.COMPLETED_ACTIVITIES)).thenReturn(2);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);
        when(jobStaticDataCache.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(jobId);
        when(neJobStaticData.getPlatformType()).thenReturn("CPP");
        final ActivityStepResultEnum activityStepResultEnum = objectUnderTest.updateActivityJobEnd(jobId);

        assertNotNull(activityStepResultEnum);
        assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, activityStepResultEnum);

    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_updateActivityJobEnd_withExcecutionFailed() throws JobDataNotFoundException {

        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(jobId);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.NAME)).thenReturn("DummyName");
        when(mapMock.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(0.0);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(ActivityConstants.ACTIVITY_RESULT);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(JobResult.FAILED.getJobResult());
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(jobId);
        when(jobConfigurationServiceMock.getActivitiesCount(jobId)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.TOTAL_ACTIVITIES)).thenReturn(2);
        when(mapMock.get(ActivityConstants.COMPLETED_ACTIVITIES)).thenReturn(2);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);
        when(jobStaticDataCache.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(jobId);
        when(neJobStaticData.getPlatformType()).thenReturn("CPP");
        final ActivityStepResultEnum activityStepResultEnum = objectUnderTest.updateActivityJobEnd(jobId);

        assertNotNull(activityStepResultEnum);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, activityStepResultEnum);

    }

    @Test
    public void test_updateJobProgress() {
        when(jobUpdateServiceMock.retrieveJobWithRetry(jobId)).thenReturn(mapMock);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(jobPropertyList);

        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.EXECUTIONINDEX)).thenReturn(1);
        when(mapMock.get(ShmConstants.JOBTEMPLATEID)).thenReturn(1L);
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(new Date());
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(JobResult.SUCCESS.getJobResult(), Arrays.asList(JobState.COMPLETED.getJobStateName()));

        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.ENDTIME, new Date());
        mainJobAttributes.put(ShmConstants.RESULT, JobResult.SUCCESS);
        mainJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());

        objectUnderTest.updateJobProgress(jobId);

        verify(mainJobProgressNotifier, times(1)).updateMainJobEndDetails(eq(jobId), anyMap());
        verify(neJobDetailsInstrumentation).recordNeJobResultBasedOnNeType(eq(jobId), anyMap());
        verify(activityStepDurationsReportGeneratorMock, times(1)).generateJobReportAndUpdateMainJob(eq(jobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_propagateCancelToMainJob_withCancelling() {

        final Object obj = "CANCELLING";
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(jobId);
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.STATE)).thenReturn(JobStateEnum.CANCELLING.toString());
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(Arrays.asList(obj));
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, mapMock, Arrays.asList(obj));
        objectUnderTest.propagateCancelToMainJob(jobId);

        verify(retryManagerMock, times(3)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_propagateCancelToMainJob_withCANCELLED() {

        final Object obj = "CANCELLED";

        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(jobId);
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.STATE)).thenReturn(JobStateEnum.CANCELLING.toString());
        when(mapMock.get(ShmConstants.JOBTEMPLATEID)).thenReturn(jobId);
        when(dpsReaderMock.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(Arrays.asList(obj));
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock, mapMock, Arrays.asList(obj));

        objectUnderTest.propagateCancelToMainJob(jobId);

        verify(retryManagerMock, times(3)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }

    @Test
    public void testPropagateCancelToMainJobShouldUpdateNeCompletedCountInMainJobAttributesBeforeCorrelatingToMainJobWorkflow(){
        retryMock();
        List<Object> neJobStates = Arrays.asList(JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName());
        final String mainJobState = JobStateEnum.CANCELLING.toString();
        mockCancellingMainJobAndNeJobAttributes(mainJobState,neJobStates);
        objectUnderTest.propagateCancelToMainJob(123456789);
        final Map<String, Object> modifiedJobAttributes = new HashMap<>();
        modifiedJobAttributes.put(ShmConstants.KEY, ShmConstants.NE_COMPLETED);
        modifiedJobAttributes.put(ShmConstants.VALUE, "1");
        List<Map<String, Object>> modifiedJobAttributesList = new ArrayList<>();
        modifiedJobAttributesList.add(modifiedJobAttributes);
        final Map<String, Object> modifiedMainJobAttributes = new HashMap<>();
        modifiedMainJobAttributes.put(ShmConstants.JOBPROPERTIES, modifiedJobAttributesList);
        verify(dpsWriterMock, times(1)).update(mainJobId, modifiedMainJobAttributes);
    }

    @Test
    public void testPropagateCancelToMainJobShouldCorrelateCancelMsgWhenNeJobsAreCompletedAndMainJobStateIsInCancelling() {
        retryMock();
        List<Object> neJobStates = Arrays.asList(JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName());
        final String mainJobState = JobStateEnum.CANCELLING.toString();
        mockCancellingMainJobAndNeJobAttributes(mainJobState,neJobStates);
        objectUnderTest.propagateCancelToMainJob(123456789);
        verify(workflowInstanceNotifier, times(1)).asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, Long.toString(jobTemplateId), wfsId, null);
    }



    @Test
    public void testPropagateCancelToMainJobShouldNotCorrelateCancelMsgWhenNeJobsAreNotCompletedAndMainJobStateIsInCancelling() {
        retryMock();
        List<Object> neJobStates = Arrays.asList(JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName(), JobState.CANCELLING.getJobStateName());
        final String mainJobState = JobStateEnum.CANCELLING.toString();
        mockCancellingMainJobAndNeJobAttributes(mainJobState,neJobStates);
        objectUnderTest.propagateCancelToMainJob(123456789);
        verify(workflowInstanceNotifier, times(0)).asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, Long.toString(jobTemplateId), wfsId, null);
    }



    @Test
    //This scenario will be seen if Ne jobs are cancelled from job details page.
    public void testPropagateCancelToMainJobShouldNotCorrelateAllNeDoneWhenAllNeJobsAreNotCompletedAndMainJobStateIsNotInCancelling() {
        retryMock();
        List<Object> neJobStates = Arrays.asList(JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName(), JobState.CANCELLING.getJobStateName());
        final String mainJobState = JobStateEnum.RUNNING.toString();
        mockCancellingMainJobAndNeJobAttributes(mainJobState,neJobStates);
        objectUnderTest.propagateCancelToMainJob(123456789);
        verify(workflowInstanceNotifier, times(0)).sendAllNeDone(Long.toString(jobTemplateId));
        verify(workflowInstanceNotifier, times(0)).asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, Long.toString(jobTemplateId), wfsId, null);
    }

    @Test
    //This scenario will be seen if Ne jobs are cancelled from job details page.
    public void testPropagateCancelToMainJobShouldCorrelateAllNeDoneWhenAllNeJobsAreCompletedAndMainJobStateIsNotInCancelling() {
        retryMock();
        List<Object> neJobStates = Arrays.asList(JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName(), JobState.COMPLETED.getJobStateName());
        final String mainJobState = JobStateEnum.RUNNING.toString();
        mockCancellingMainJobAndNeJobAttributes(mainJobState,neJobStates);
        objectUnderTest.propagateCancelToMainJob(123456789);
        verify(workflowInstanceNotifier, times(1)).sendAllNeDone(Long.toString(jobTemplateId));
        verify(workflowInstanceNotifier, times(0)).asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, Long.toString(jobTemplateId), wfsId, null);
    }
    private void mockCancellingMainJobAndNeJobAttributes(final String mainJobState,final List neJobStates) {
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        mainJobAttributes.put(ShmConstants.STATE, mainJobState);
        mainJobAttributes.put(ShmConstants.JOBTEMPLATEID, jobTemplateId);
        mainJobAttributes.put(ShmConstants.WFS_ID, wfsId);
        final Map<String, String> mainJobProperties = new HashMap<>();
        mainJobProperties.put(ShmConstants.KEY, ShmConstants.NE_COMPLETED);
        mainJobProperties.put(ShmConstants.VALUE, "0");
        List<Map<String, String>> mainJobPropertyList = new ArrayList<>();
        mainJobPropertyList.add(mainJobProperties);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, mainJobPropertyList);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(neJobAttributes, mainJobAttributes, neJobStates);
    }

    private void retryMock() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.MILLISECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
    }


    @Test
    public void test_updateActivityJobAsFailed() throws JobDataNotFoundException {

        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(jobId);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.NAME)).thenReturn("DummyName");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(ActivityConstants.ACTIVITY_RESULT);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(JobResult.FAILED.getJobResult());
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(jobId);
        when(jobConfigurationServiceMock.getActivitiesCount(jobId)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.TOTAL_ACTIVITIES)).thenReturn(2);
        when(mapMock.get(ActivityConstants.COMPLETED_ACTIVITIES)).thenReturn(2);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);
        when(jobStaticDataCache.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticData);
        Mockito.doNothing().when(faBuildingBlockResponseProcessor).sendFaResponse(Matchers.anyLong(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        when(neJobStaticData.getMainJobId()).thenReturn(jobId);
        when(neJobStaticData.getPlatformType()).thenReturn("CPP");
        objectUnderTest.updateActivityJobAsFailed(jobId);

        verify(jobUpdateServiceMock, times(2)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateJobAsCancelled_withisActivityJobUpdateAsTrue() {

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);

        objectUnderTest.updateJobAsCancelled(jobId, mapMock, Boolean.TRUE);

        verify(retryManagerMock, times(1)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateJobAsCancelled_withisActivityJobUpdateAsFalseAndActivityResultAsSuccess() {
        final Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ShmConstants.ENDTIME, new Date());
        Map<String, Object> jobAttributesMapMock = new HashMap<>();
        jobAttributesMapMock.put(ShmConstants.MAIN_JOB_ID, jobId);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobAttributesMapMock);
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn(JobResult.CANCELLED.getJobResult());
        objectUnderTest.updateJobAsCancelled(jobId, attrs, Boolean.FALSE);
        verify(jobUpdateServiceMock, times(1)).updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());

        attrs.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        attrs.put(ShmConstants.RESULT, JobResult.CANCELLED.toString());
        verify(jobUpdateServiceMock).updateJobAttributes(jobId, attrs);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateJobAsCancelled_withisActivityJobUpdateAsFalseAndActivityResultAsFailed() {
        final Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ShmConstants.ENDTIME, new Date());
        Map<String, Object> jobAttributesMapMock = new HashMap<>();
        jobAttributesMapMock.put(ShmConstants.MAIN_JOB_ID, jobId);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobAttributesMapMock);
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn(JobResult.CANCELLED.getJobResult());
        objectUnderTest.updateJobAsCancelled(jobId, attrs, Boolean.FALSE);
        verify(jobUpdateServiceMock, times(1)).updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());

        attrs.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        attrs.put(ShmConstants.RESULT, JobResult.FAILED.toString());
        verify(jobUpdateServiceMock).updateJobAttributes(jobId, attrs);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateNHCJobAsCancelled_withisActivityJobUpdateAsFalseAndActivityResultAsFailed() throws JobDataNotFoundException {
        final Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ShmConstants.ENDTIME, new Date());
        consolidateNHCNeProperties();
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn(JobResult.CANCELLED.getJobResult());
        objectUnderTest.updateJobAsCancelled(jobId, attrs, Boolean.FALSE);
        verify(jobUpdateServiceMock, times(1)).updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());

        attrs.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        attrs.put(ShmConstants.RESULT, JobResult.FAILED.toString());
        verify(jobUpdateServiceMock).updateJobAttributes(jobId, attrs);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateNHCJobAsCancelled_withisActivityJobUpdateAsFalseAndActivityResultAsSuccess() throws JobDataNotFoundException {
        final Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ShmConstants.ENDTIME, new Date());
        consolidateNHCNeProperties();
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn(JobResult.CANCELLED.getJobResult());
        objectUnderTest.updateJobAsCancelled(jobId, attrs, Boolean.FALSE);
        verify(jobUpdateServiceMock, times(1)).updateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());

        attrs.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        attrs.put(ShmConstants.RESULT, JobResult.CANCELLED.toString());
        verify(jobUpdateServiceMock).updateJobAttributes(jobId, attrs);
    }

    @SuppressWarnings("unchecked")
    private void consolidateNHCNeProperties() throws JobDataNotFoundException {
        Map<String, Object> jobAttributesMapMock = new HashMap<>();
        jobAttributesMapMock.put(ShmConstants.MAIN_JOB_ID, jobId);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobAttributesMapMock);
        when(jobStaticDataCacheMock.getJobStaticData(jobId)).thenReturn(jobStaticData);
        when(jobStaticData.getJobType()).thenReturn(JobType.NODE_HEALTH_CHECK);
        serviceFinderBeanMock = PowerMockito.mock(ServiceFinderBean.class);
        jobConsolidationService = PowerMockito.mock(JobConsolidationService.class);
        try {
            PowerMockito.whenNew(ServiceFinderBean.class).withNoArguments().thenReturn(serviceFinderBeanMock);
            PowerMockito.when(serviceFinderBeanMock.find(JobConsolidationService.class, JobType.NODE_HEALTH_CHECK.name())).thenReturn(jobConsolidationService);
            Mockito.when(jobConsolidationService.consolidateNeJobData(Matchers.anyLong())).thenReturn(getConsolidatedData());
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    private Map<String, Object> getConsolidatedData() {
        final Map<String, Object> map = new HashMap<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        map.put(ShmConstants.JOBPROPERTIES, jobProperties);
        map.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        return map;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_createSkipJob() {

        when(persistenceObjectMock.getPoId()).thenReturn(jobId);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(persistenceObjectMock);

        objectUnderTest.createSkipJob(jobId, ExecMode.IMMEDIATE.getMode());
        verify(retryManagerMock, times(1)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsExecuteCompletedReturningTrue() {

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);

        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.IS_ACTIVITY_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "true");
        activityJobProperties.add(jobProperty);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobProperties);

        assertTrue(objectUnderTest.isExecuteCompleted(jobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsExecuteCompletedReturningFalse() {

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);

        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(null);

        assertFalse(objectUnderTest.isExecuteCompleted(jobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateCancelSkipped() {
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mapMock);

        objectUnderTest.abortActivity(jobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_updateJobStartWhenActivityScheduled() throws MoNotFoundException {
        final String nodeName = "node1";
        when(networkElementInfo.getNeType()).thenReturn("ERBS");
        when(networkElementRetrievalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        getJobConfiguration(jobId);
        objectUnderTest.updateActivityJobStart(jobId, JobState.SCHEDULED);
        verify(jobUpdateServiceMock, times(2)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        verify(jobUpdateServiceMock, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());
    }

    private void getJobConfiguration(final long jobId) {
        when(activityUtils.getPoAttributes(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.ACTIVITY_NAME)).thenReturn(activityname);
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(2L);
        when(mapMock.get(ShmConstants.ORDER)).thenReturn(1);
        when(activityUtils.getPoAttributes(2L)).thenReturn(neJobAttrs);
        when(jobConfigurationServiceRetryProxy.getNeJobAttributes(2L)).thenReturn(neJobAttrs);
        when(neJobAttrs.get(ShmConstants.MAIN_JOB_ID)).thenReturn(4L);
        when(neJobAttrs.get(ShmCommonConstants.NE_NAME)).thenReturn("node1");
        when(activityUtils.getMainJobAttributesByNeJobId(2L)).thenReturn(mainJobAttrs);
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activitySchedule = new HashMap<String, Object>();
        final List<Map<String, Object>> actvityScheduleAttributes = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actvitySchedules = new HashMap<String, Object>();
        final Map<String, Object> activity = new HashMap<String, Object>();
        activity.put(SchedulePropertyConstants.NAME, SchedulePropertyConstants.START_DATE);
        activity.put(SchedulePropertyConstants.VALUE, "2016-08-10 08:04:10 GMT+0530");
        actvityScheduleAttributes.add(activity);
        activitySchedule.put(ShmConstants.ACTIVITY_NAME, "verify");
        activitySchedule.put(ShmCommonConstants.NETYPE, "ERBS");
        activitySchedule.put(ShmConstants.ACTIVITY_SCHEDULE, actvitySchedules);
        actvitySchedules.put(SchedulePropertyConstants.SCHEDULE_ATTRIBUTES, actvityScheduleAttributes);
        activities.add(activitySchedule);
        jobConfiguration.put(ShmConstants.JOB_ACTIVITIES, activities);
        when(mainJobAttrs.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(jobConfiguration);
    }

    @Test
    public void testUpdateMainJobAsCancelledWhenAllNeJobsAreCompletedSuccesfully() {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.ENDTIME, new Date());

        when(jobConfigurationServiceMock.retrieveNeJobResult(jobId)).thenReturn(JobResult.SUCCESS.toString());
        objectUnderTest.updateMainJobAsCancelled(jobId, mainJobAttributes);

        mainJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        mainJobAttributes.put(ShmConstants.RESULT, JobResult.SUCCESS.toString());
        verify(mainJobProgressNotifier).updateMainJobEndDetails(jobId, mainJobAttributes);
        verify(neJobDetailsInstrumentation).recordNeJobResultBasedOnNeType(eq(jobId), anyMap());
        verify(activityStepDurationsReportGeneratorMock, times(1)).generateJobReportAndUpdateMainJob(eq(jobId));
    }

    @Test
    public void testUpdateMainJobAsCancelledWhenAtleastOneNeJobFailed() {
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.ENDTIME, new Date());

        when(jobConfigurationServiceMock.retrieveNeJobResult(jobId)).thenReturn(JobResult.CANCELLED.toString());
        objectUnderTest.updateMainJobAsCancelled(jobId, mainJobAttributes);

        mainJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        mainJobAttributes.put(ShmConstants.RESULT, JobResult.FAILED.toString());
        verify(mainJobProgressNotifier).updateMainJobEndDetails(jobId, mainJobAttributes);
        verify(neJobDetailsInstrumentation).recordNeJobResultBasedOnNeType(eq(jobId), anyMap());
    }

}
