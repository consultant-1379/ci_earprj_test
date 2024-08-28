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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

@RunWith(MockitoJUnitRunner.class)
public class JobCancelHandlerTest {

    private static final String BUSINESS_KEY = "businessKey";

    @InjectMocks
    private JobCancelHandler ObjectUnderTest;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifierMock;

    @Mock
    private WorkflowInstanceServiceLocal workflowInstanceServiceLocalMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private DpsReader dpsReaderMock;

    @Mock
    private WorkflowObject workFlowObjectMock;

    @Mock
    private Map<String, Object> mapMock;
    
    @Mock
    JobConfigurationService jobConfigurationServiceMock;

    @Test
    public void test_cancelNEJobWorkflows_correlationSUccess() {
        ObjectUnderTest.cancelNEJobWorkflows(BUSINESS_KEY);

        verify(workflowInstanceNotifierMock).sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY);
        verify(workflowInstanceNotifierMock, never()).executeWorkflowQuery(any(Query.class));
        verify(workflowInstanceNotifierMock, never()).cancelWorkflowInstance(anyString());
        verify(jobUpdateServiceMock, never()).updateJobAttributes(anyLong(), anyMap());
        verify(dpsReaderMock, never()).getProjectedAttributes(anyString(), anyString(), anyMap(), anyList());
        verify(systemRecorderMock, never()).recordEvent(anyString(), any(EventLevel.class), anyString(), anyString(), anyString());
    }

    @Test
    public void test_cancelNEJobWorkflows_whenCorrelationFails_noWorkflowsButCancelsNEjob() {
        final Map<Object, Object> neJobRestrictAttributes = new HashMap<Object, Object>();
        neJobRestrictAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(workflowInstanceNotifierMock.sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY)).thenThrow(WorkflowServiceInvocationException.class);
        ObjectUnderTest.cancelNEJobWorkflows(BUSINESS_KEY);

        verify(workflowInstanceNotifierMock).sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY);
        verify(workflowInstanceNotifierMock).executeWorkflowQuery(any(Query.class));
        verify(workflowInstanceNotifierMock, never()).cancelWorkflowInstance(anyString());
        verify(jobUpdateServiceMock, never()).updateJobAttributes(anyLong(), anyMap());
        verify(dpsReaderMock).getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, neJobRestrictAttributes, Arrays.asList(ShmConstants.NE_NAME));
        verify(systemRecorderMock).recordEvent(anyString(), any(EventLevel.class), anyString(), anyString(), anyString());
    }

    @Test
    public void test_cancelNEJobWorkflows_whenCorrelationFails() {
        when(workflowInstanceNotifierMock.sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY)).thenThrow(WorkflowServiceInvocationException.class);
        when(workflowInstanceNotifierMock.executeWorkflowQuery(any(Query.class))).thenReturn(Arrays.asList(workFlowObjectMock));
        ObjectUnderTest.cancelNEJobWorkflows(BUSINESS_KEY);

        verify(workflowInstanceNotifierMock).sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY);
        verify(workflowInstanceNotifierMock).executeWorkflowQuery(any(Query.class));
        verify(workflowInstanceNotifierMock).cancelWorkflowInstance(anyString());
        verify(jobUpdateServiceMock, never()).updateJobAttributes(anyLong(), anyMap());
        verify(dpsReaderMock).getProjectedAttributes(anyString(), anyString(), anyMap(), anyList());
        verify(systemRecorderMock).recordEvent(anyString(), any(EventLevel.class), anyString(), anyString(), anyString());
    }

    @Test
    public void test_cancelNEJobWorkflows_noRunningActivityJobs_cancelsOnlyNEJobs() {
        final Map<Object, Object> neJobRestrictAttributes = new HashMap<Object, Object>();
        neJobRestrictAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);

        final Map<Object, Object> activityJobRestrictAttributes = new HashMap<Object, Object>();
        activityJobRestrictAttributes.put(ShmConstants.NE_JOB_ID, 123l);
        activityJobRestrictAttributes.put(ShmConstants.STATE, JobStateEnum.RUNNING.name());

        when(dpsReaderMock.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, neJobRestrictAttributes, Arrays.asList(ShmConstants.NE_NAME))).thenReturn(Arrays.asList(mapMock));
        when(workflowInstanceNotifierMock.sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY)).thenThrow(WorkflowServiceInvocationException.class);
        when(workflowInstanceNotifierMock.executeWorkflowQuery(any(Query.class))).thenReturn(Arrays.asList(workFlowObjectMock));
        when(mapMock.get(ShmConstants.PO_ID)).thenReturn(123l);

        ObjectUnderTest.cancelNEJobWorkflows(BUSINESS_KEY);

        verify(workflowInstanceNotifierMock).sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY);
        verify(workflowInstanceNotifierMock).executeWorkflowQuery(any(Query.class));
        verify(workflowInstanceNotifierMock).cancelWorkflowInstance(anyString());
        verify(jobUpdateServiceMock).updateJobAttributes(anyLong(), anyMap());
        verify(dpsReaderMock).getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, neJobRestrictAttributes, Arrays.asList(ShmConstants.NE_NAME));
        verify(dpsReaderMock).getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, activityJobRestrictAttributes, Arrays.asList(ShmConstants.NAME));
        verify(systemRecorderMock).recordEvent(anyString(), any(EventLevel.class), anyString(), anyString(), anyString());
    }

    @Test
    public void test_cancelNEJobWorkflows_cancelesAllJobs() {
        final Map<Object, Object> neJobRestrictAttributes = new HashMap<Object, Object>();
        neJobRestrictAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);

        final Map<Object, Object> activityJobRestrictAttributes = new HashMap<Object, Object>();
        activityJobRestrictAttributes.put(ShmConstants.NE_JOB_ID, 123l);
        activityJobRestrictAttributes.put(ShmConstants.STATE, JobStateEnum.RUNNING.name());

        when(dpsReaderMock.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, neJobRestrictAttributes, Arrays.asList(ShmConstants.NE_NAME))).thenReturn(Arrays.asList(mapMock));
        when(dpsReaderMock.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, activityJobRestrictAttributes, Arrays.asList(ShmConstants.NAME))).thenReturn(
                Arrays.asList(mapMock));
        when(workflowInstanceNotifierMock.sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY)).thenThrow(WorkflowServiceInvocationException.class);
        when(workflowInstanceNotifierMock.executeWorkflowQuery(any(Query.class))).thenReturn(Arrays.asList(workFlowObjectMock));
        when(mapMock.get(ShmConstants.PO_ID)).thenReturn(123l);

        ObjectUnderTest.cancelNEJobWorkflows(BUSINESS_KEY);

        verify(workflowInstanceNotifierMock).sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, BUSINESS_KEY);
        verify(workflowInstanceNotifierMock).executeWorkflowQuery(any(Query.class));
        verify(workflowInstanceNotifierMock).cancelWorkflowInstance(anyString());
        verify(jobUpdateServiceMock, times(2)).updateJobAttributes(anyLong(), anyMap());
        verify(dpsReaderMock).getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, neJobRestrictAttributes, Arrays.asList(ShmConstants.NE_NAME));
        verify(dpsReaderMock).getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, activityJobRestrictAttributes, Arrays.asList(ShmConstants.NAME));
        verify(systemRecorderMock).recordEvent(anyString(), any(EventLevel.class), anyString(), anyString(), anyString());
    }

}
