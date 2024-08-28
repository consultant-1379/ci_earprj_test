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

import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;

import com.ericsson.oss.services.shm.jobs.common.modelentities.WorkFlowObject;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;

/**
 * Common Helper class to contact the WFS, and WFS related Instances should be injected no where directly.
 * 
 * @author xrajeke
 * 
 */
public interface WorkflowInstanceNotifier {

    /**
     * Method to call to WFS with retries if its not available
     * 
     * @param businessKey
     * @param processVariables
     * @return
     */
    boolean sendActivate(final String businessKey, final Map<String, Object> processVariables);

    /**
     * This method correlates the All NE Done message after execution of all NEs are finished.
     * 
     * @param businessKey
     *            - businessKey of NEJob
     * @return boolean isNotified
     */
    boolean sendAllNeDone(final String businessKey);

    /**
     * 
     * @param message
     * @param businessKey
     * @return
     */
    boolean sendLoadControlMessage(final String message, final String businessKey);

    /**
     * * Cancels NE Job Workflow instance.
     * 
     * @param businessKey
     *            - businessKey of NEJob
     * 
     * @param propagateCancelToMainJob
     *            - If set true, sends a cancel message to the main Job WFS instance
     */
    @Asynchronous
    void sendAsynchronousMsgToWaitingWFSInstance(final String wfsMessage, final String businessKey);

    /**
     * @param processVariables
     */
    String submitWorkFlowInstance(final Map<String, Object> processVariables, final String businessKey);

    /**
     * @param businessKey
     * @param workFlowObject
     * @return
     */
    String submitWorkFlowInstance(String businessKey, WorkFlowObject workFlowObject);

    /**
     * retries for the WFS correlateMessage method incase wfs service is unavailable
     * 
     * @param wfsMessage
     * @param businessKey
     * @return
     */
    boolean sendMessageToWaitingWFSInstance(String wfsMessage, String businessKey);

    /**
     * retries for the WFS completeUsertask method incase wfs service is unavailable
     * 
     * @param workflowId
     */
    void completeUserTask(String workflowId);

    /**
     * retries for the WFS executeQuery method incase wfs service is unavailable
     * 
     * @param wfsQuery
     * @return
     */
    List<WorkflowObject> executeWorkflowQuery(Query wfsQuery);

    /**
     * Execute WFS executeQuery method and returns empty if work flow service not available.
     * 
     * @param wfsQuery
     * @return
     */
    List<WorkflowObject> executeWorkflowQueryForJobContinue(final Query wfsQuery);

    /**
     * @param businessKey
     * @param processVariables
     * @return
     */
    WorkflowInstance submitAndGetWorkFlowInstance(String businessKey, Map<String, Object> processVariables);

    /**
     * @param attribute
     */
    void cancelWorkflowInstance(String workflowInstanceId);

    /**
     * @param businessKey
     * @param processVariables
     * @return
     */
    boolean sendPrecheckOrTimeoutMsgToWfsInstance(String businessKey, Map<String, Object> processVariables, String message);

    /**
     * @param businessKey
     * @param processVariables
     * @return
     */
    boolean sendCancelMOActionDone(String businessKey, Map<String, Object> processVariables);

    void notifySyncActivity(String businessKey, Map<String, Object> processVariables);

    void asyncCorrelateActiveWorkflow(final String message, final String businessKey, final String wfsId, final Map<String, Object> processVariables);
}
