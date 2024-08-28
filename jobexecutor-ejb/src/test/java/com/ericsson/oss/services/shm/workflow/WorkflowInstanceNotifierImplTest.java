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
package com.ericsson.oss.services.shm.workflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.ericsson.oss.services.wfs.api.query.*;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import com.ericsson.oss.services.wfs.jee.api.WorkflowQueryServiceLocal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.services.shm.common.wfs.WfsRetryPolicies;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.wfs.api.MessageDispatchInvalidException;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
@SuppressWarnings("unchecked")
public class WorkflowInstanceNotifierImplTest {

    @Mock
    @Inject
    WorkflowInstanceServiceLocal workflowInstanceServiceLocal;

    @InjectMocks
    WorkflowInstanceNotifierImpl objectUnderTest;

    @Mock
    WfsRetryPolicies wfsRetryPolicies;

    @Mock
    RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    private QueryBuilder queryBuilderMock;

    @Mock
    Query wfsQueryMock;

    @Mock
    WorkflowQueryServiceLocal workflowQueryServiceLocalMock;

    @Mock
    RestrictionBuilder restrictionBuilderMock;

    @Mock
    Restriction restrictionMock;

    @Mock
    private WorkflowObject workFlowObject;

    @Mock
    private RetryManager retryManagerMock;

    long activityJobId = 1;
    String businessKey = "Some Business Key";
    final String wfsId = "5129e3a2-cf9f-11e9-8902-525400ecdd5e";
    Map<String, Object> processVariables = new HashMap<String, Object>();
    private final Class<? extends Exception>[] exceptionsArray = new Class[]{IllegalStateException.class, WorkflowMessageCorrelationException.class};
    public static final String ACTIVATE = "Activate";
    public static final String FAILED = "FAILED";

    public static final int threadSleepInterval = 0;

    public static final int retryCount = 1;

    @Test
    public void testSendActivate() throws WorkflowMessageCorrelationException {
        Mockito.doNothing().when(workflowInstanceServiceLocal).correlateMessage(ACTIVATE, businessKey, processVariables);
        processVariables.put("activityResult", "SUCCESS");
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        assertTrue(objectUnderTest.sendActivate(businessKey, processVariables));
    }

    @Test
    public void testSendActivateWithWorkflowMessageCorrelationException() throws WorkflowMessageCorrelationException {
        Mockito.doThrow(WorkflowMessageCorrelationException.class).when(workflowInstanceServiceLocal).correlateMessage(ACTIVATE, businessKey, processVariables);
        processVariables.put("activityResult", "SUCCESS");
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(false);
        assertFalse(objectUnderTest.sendActivate(businessKey, processVariables));
    }

    @Test
    public void testSendActivateWithException() throws WorkflowMessageCorrelationException {
        Mockito.doThrow(Exception.class).when(workflowInstanceServiceLocal).correlateMessage(ACTIVATE, businessKey, processVariables);
        processVariables.put("activityResult", "SUCCESS");
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(false);
        assertFalse(objectUnderTest.sendActivate(businessKey, processVariables));
    }

    @Test
    public void testSendNeJobDone() throws WorkflowMessageCorrelationException {
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        Mockito.doNothing().when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.ALL_NE_DONE, businessKey);
        assertTrue(objectUnderTest.sendAllNeDone(businessKey));
    }

    @Test
    public void testSendNeJobDoneWithWorkflowMessageCorrelationException() throws WorkflowMessageCorrelationException {
        Mockito.doThrow(WorkflowMessageCorrelationException.class).when(workflowInstanceServiceLocal).correlateMessage(ACTIVATE, businessKey, processVariables);
        processVariables.put("activityResult", "SUCCESS");
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(false);
        assertFalse(objectUnderTest.sendActivate(businessKey, processVariables));
    }

    @Test
    public void testSendNeJobDoneWithException() throws WorkflowMessageCorrelationException {
        Mockito.doThrow(Exception.class).when(workflowInstanceServiceLocal).correlateMessage(ACTIVATE, businessKey, processVariables);
        processVariables.put("activityResult", "SUCCESS");
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(false);
        assertFalse(objectUnderTest.sendActivate(businessKey, processVariables));
    }

    @Test
    public void testSendCancelMessage() throws MessageDispatchInvalidException {
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        Mockito.doNothing().when(workflowInstanceServiceLocal).messageReceived(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
        objectUnderTest.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
    }

    @Test
    public void testSendCancelMessageExceptionHandling() throws WorkflowMessageCorrelationException {
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        Mockito.doThrow(WorkflowMessageCorrelationException.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
        objectUnderTest.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
    }

    @Test
    public void testInteruptedExceptionHandlingInMessageCorrelation() throws WorkflowMessageCorrelationException {
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        Mockito.doThrow(InterruptedException.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
        objectUnderTest.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
    }

    @Test
    public void testSendCancelMOActionDone() throws WorkflowMessageCorrelationException {
        Mockito.doNothing().when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey, processVariables);
        processVariables.put("activityResult", FAILED);
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(true);
        assertTrue(objectUnderTest.sendCancelMOActionDone(businessKey, processVariables));
    }

    @Test
    public void testSendCancelMOActionDoneWithWorkflowMessageCorrelationException() throws WorkflowMessageCorrelationException {
        Mockito.doThrow(WorkflowMessageCorrelationException.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey, processVariables);
        processVariables.put("activityResult", FAILED);
        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(false);
        assertFalse(objectUnderTest.sendCancelMOActionDone(businessKey, processVariables));
    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldCorrelateMsgWhenProcessVariablesAreNull() throws WorkflowMessageCorrelationException {
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, null);
        verify(workflowInstanceServiceLocal, times(1)).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldCorrelateMsgWhenProcessVariablesAreEmpty() throws WorkflowMessageCorrelationException {
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, processVariables);
        verify(workflowInstanceServiceLocal, times(1)).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldCorrelateMsgAndProcessVariablesWhenProcessVariablesArePresent() throws WorkflowMessageCorrelationException {
        processVariables.put("activityResult", "CANCELLED");
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, processVariables);
        verify(workflowInstanceServiceLocal, times(1)).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, processVariables);

    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldNotRetryIfWorkFlowInstanceIsEmpty() throws Exception {
        retryMock();
        mockWorkflowQuery(wfsId);
        List<WorkflowObject> workflowObjects = new ArrayList<>();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(workflowObjects,true);
        Mockito.doThrow(Exception.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, null);
        //This retry verification is for DB query to retrieve workflow instances.
        verify(retryManagerMock, times(1)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }
    @Test
    public void testAsyncCorrelateActiveWorkflowShouldNotRetryIfWfsIdIsNull() throws Exception {
        retryMock();
        Mockito.doThrow(Exception.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, null, null);
        //This retry verification is for DB query to retrieve workflow instances.
        verify(retryManagerMock, times(0)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldNotRetryIfWfsInstanceRetrievalFailed() throws Exception {
        retryMock();
        Mockito.doThrow(Exception.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
        Mockito.doThrow(Exception.class).when(retryManagerMock).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));

        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, null);
        //This retry verification is for DB query to retrieve workflow instances.
        verify(retryManagerMock, times(1)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldNotRetryIfWorkFlowInstanceIsNull() throws Exception {
        retryMock();
        mockWorkflowQuery(wfsId);
        when(workflowQueryServiceLocalMock.executeQuery(wfsQueryMock)).thenReturn(null);
        Mockito.doThrow(Exception.class).when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, null);
        //This retry verification is for DB query to retrieve workflow instances.
        verify(retryManagerMock, times(1)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }

    @Test
    public void testAsyncCorrelateActiveWorkflowShouldRetryIfWorkFlowInstanceIsPresent() throws Exception {
        retryMock();
        mockWorkflowQuery(wfsId);
        List<WorkflowObject> workflowObjects = new ArrayList<>();
        workflowObjects.add(workFlowObject);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(workflowObjects,true);
        Mockito.doThrow(Exception.class)
                .doNothing()
                .when(workflowInstanceServiceLocal).correlateMessage(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey);
        objectUnderTest.asyncCorrelateActiveWorkflow(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, businessKey, wfsId, null);
        verify(retryManagerMock, times(2)).executeCommand(eq(retryPolicyMock), any(RetriableCommand.class));
    }


    private void mockWorkflowQuery(final String wfsId) {
        when(queryBuilderMock.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY)).thenReturn(wfsQueryMock);
        when(wfsQueryMock.getRestrictionBuilder()).thenReturn(restrictionBuilderMock);
        when(restrictionBuilderMock.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId)).thenReturn(restrictionMock);
        wfsQueryMock.setRestriction(restrictionMock);
    }

    private void retryMock() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(wfsRetryPolicies.getWfsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.MILLISECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
    }
}
