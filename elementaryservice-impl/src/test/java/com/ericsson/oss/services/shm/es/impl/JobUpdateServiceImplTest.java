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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class JobUpdateServiceImplTest {

    private static final long JOB_ID = 1234;
    private static final long NE_JOB_ID = 2345;
    private static final long ACTIVITY_JOB_ID = 5678;

    private static final long JOB_TEMPLATE_ID = 123;

    private List<Map<String, Object>> jobPropertyList;

    private List<Map<String, Object>> jobLogList;

    @Mock
    private Map<String, Object> neJobPoAttributesMock;

    @Mock
    private DpsWriter dpsWriter;

    @Mock
    private JobConfigurationService jobConfigurationService;

    @Mock
    private JobStatusUpdateService jobStatusUpdateService;

    @Mock
    private NEJobStatusUpdater neJobStatusUpdater;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @InjectMocks
    private JobUpdateServiceImpl jobUpdateServiceImpl;

    @Mock
    private RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    private DpsRetryConfigurationParamProvider dpsConfigMock;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private RetryManager retryManagerMock;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    private final Class<? extends Exception>[] exceptionsArray = new Class[] { IllegalStateException.class, EJBException.class };

    @Before
    public void setup() {
        when(dpsConfigMock.getDpsOptimisticLockWaitIntervalInMS()).thenReturn(1000);
    }

    @Test
    public void testUpdateRunningJobAttributes() {
        when((jobConfigurationService).persistRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList)).thenReturn(true);
        jobUpdateServiceImpl.updateRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList);
    }

    @Test
    public void testUpdateRunningJobAttributesExceptionHandling() {
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(jobConfigurationService).persistRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList);
        jobUpdateServiceImpl.updateRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList);
    }

    @Test
    public void testUpdateJobAttributes() {
        final Map<String, Object> jobAttributes = new HashMap<>();
        Mockito.doNothing().when(dpsWriter).update(JOB_ID, jobAttributes);
        jobUpdateServiceImpl.updateJobAttributes(JOB_ID, jobAttributes);
    }

    @Test
    public void testUpdateJobAttributesExceptionHandling() {
        final Map<String, Object> jobAttributes = new HashMap<>();
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(dpsWriter).update(JOB_ID, jobAttributes);
        jobUpdateServiceImpl.updateJobAttributes(JOB_ID, jobAttributes);
    }

    @Test
    public void testUpdateNEJobsCompletedCount() {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, JOB_TEMPLATE_ID);
        Mockito.doReturn(true).when(jobStatusUpdateService).updateNEJobsCompletedCount(JOB_ID);
        //Mockito.doReturn(mainJobAttributes).when(jobConfigurationService).retrieveJob(JOB_ID);
        setRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mainJobAttributes);

        jobUpdateServiceImpl.updateNEJobsCompletedCount(JOB_ID);
    }

    @Test
    public void testUpdateNEJobsCompletedCountExceptionHandling() {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, JOB_TEMPLATE_ID);
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(jobStatusUpdateService).updateNEJobsCompletedCount(JOB_ID);
        //Mockito.doReturn(mainJobAttributes).when(jobConfigurationService).retrieveJob(JOB_ID);
        setRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(mainJobAttributes);

        jobUpdateServiceImpl.updateNEJobsCompletedCount(JOB_ID);
    }

    @Test
    public void testReadAndUpdateRunningJobAttributes() {
        when((jobConfigurationService).readAndPersistRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList, null)).thenReturn(true);
        final boolean Result = jobUpdateServiceImpl.readAndUpdateRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList);
        assertTrue(Result);
    }

    @Test
    public void testReadAndUpdateRunningJobAttributesExceptionHandling() {
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(jobConfigurationService).readAndPersistRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList, null);
        final boolean Result = jobUpdateServiceImpl.readAndUpdateRunningJobAttributes(JOB_ID, jobPropertyList, jobLogList);
        assertFalse(Result);
    }

    @Test
    public void testupdateNEJobEndAttributes() {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, JOB_TEMPLATE_ID);
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, JOB_ID);
        when(neJobStatusUpdater.updateNEJobEndAttributes(NE_JOB_ID)).thenReturn(false);
        when(jobUpdateServiceImpl.retrieveJobWithRetry(NE_JOB_ID)).thenReturn(neJobAttributes);
        when(jobUpdateServiceImpl.retrieveJobWithRetry(JOB_ID)).thenReturn(mainJobAttributes);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(neJobAttributes, mainJobAttributes);

        jobUpdateServiceImpl.updateNEJobEndAttributes(NE_JOB_ID);
    }

    @Test
    public void testupdateNEJobEndAttributesExceptionhandling() {
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(neJobStatusUpdater).updateNEJobEndAttributes(NE_JOB_ID);
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, JOB_TEMPLATE_ID);
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, JOB_ID);
        when(jobUpdateServiceImpl.retrieveJobWithRetry(NE_JOB_ID)).thenReturn(neJobAttributes);
        when(jobUpdateServiceImpl.retrieveJobWithRetry(JOB_ID)).thenReturn(mainJobAttributes);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(neJobAttributes, mainJobAttributes);
        jobUpdateServiceImpl.updateNEJobEndAttributes(NE_JOB_ID);
    }

    private void setRetryPolicies() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.SECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
    }

    @Test
    public void testReadAndUpdateRunningJobAttributesForCancel() {
        when(jobConfigurationService.readAndPersistJobAttributesForCancel(JOB_ID, jobPropertyList, jobLogList)).thenReturn(true);
        jobUpdateServiceImpl.readAndUpdateJobAttributesForCancel(JOB_ID, jobPropertyList, jobLogList);

    }

    @Test
    public void testReadAndUpdateRunningJobAttributesForCancelExceptionHandling() {
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(jobConfigurationService).readAndPersistJobAttributesForCancel(JOB_ID, jobPropertyList, jobLogList);
        final boolean Result = jobUpdateServiceImpl.readAndUpdateJobAttributesForCancel(JOB_ID, jobPropertyList, jobLogList);
        assertFalse(Result);
    }

    @Test
    public void testAddOrUpdateOrRemoveJobPropertiesUpdateSuccess() {
        final Map<String, String> propertyTobeAdded = new HashMap<>();
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        final boolean value = jobUpdateServiceImpl.addOrUpdateOrRemoveJobProperties(NE_JOB_ID, propertyTobeAdded, jobLogList);
        assertTrue(value);
    }

    @Test
    public void testAddOrUpdateOrRemoveJobPropertiesUpdateFailed() {
        final Map<String, String> propertyTobeAdded = new HashMap<>();
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(false);
        final boolean value = jobUpdateServiceImpl.addOrUpdateOrRemoveJobProperties(NE_JOB_ID, propertyTobeAdded, jobLogList);
        assertFalse(value);
    }

    @Test
    public void testReadAndUpdateStepDurations() {
        final String stepName = ActivityStepsEnum.EXECUTE.getStep();
        final String stepNameAndDurationToPersist = stepName + "=" + 10.0;
        //when(jobConfigurationService.readAndPersistRunningJobStepDuration(JOB_ID, stepNameAndDurationToPersist, stepName)).thenReturn(true);
        final boolean value = jobUpdateServiceImpl.readAndUpdateStepDurations(JOB_ID, stepNameAndDurationToPersist, stepName);
        Mockito.verify(jobConfigurationService, Mockito.atLeastOnce()).readAndPersistRunningJobStepDuration(JOB_ID, stepNameAndDurationToPersist, stepName);
        //assertTrue(value);
        assertFalse(value);
    }

    @Test
    public void testupdateActivityAsSkipped() {
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put(ShmConstants.RESULT, JobResult.SKIPPED.getJobResult());
        jobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        jobAttributes.put(ShmConstants.ENDTIME, new Date());
        Mockito.doThrow(EJBTransactionRolledbackException.class).when(dpsWriter).update(ACTIVITY_JOB_ID, jobAttributes);
        jobUpdateServiceImpl.updateActivityAsSkipped(ACTIVITY_JOB_ID);
        Mockito.verify(dpsWriter, Mockito.atLeastOnce()).update(Matchers.anyLong(), Matchers.anyMap());
    }

}
