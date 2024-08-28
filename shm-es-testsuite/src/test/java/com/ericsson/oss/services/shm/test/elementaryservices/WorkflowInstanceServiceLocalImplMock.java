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
package com.ericsson.oss.services.shm.test.elementaryservices;

import java.util.Collection;
import java.util.Map;

import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.Clustered;

import com.ericsson.oss.services.wfs.api.MessageDispatchInvalidException;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

@Stateless
@Clustered
public class WorkflowInstanceServiceLocalImplMock implements WorkflowInstanceServiceLocal {

    public static String MESSAGE;
    public static String BUSINESS_KEY;
    public static Map<String, Object> PROCESS_VARIABLES;

    /**
     * 
     */
    public static void reset() {
        MESSAGE = null;
        BUSINESS_KEY = null;
        PROCESS_VARIABLES = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#startWorkflowInstanceByDefinitionId(java.lang.String)
     */
    @Override
    public WorkflowInstance startWorkflowInstanceByDefinitionId(final String workflowDefinitionId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#startWorkflowInstanceByDefinitionId(java.lang.String, java.util.Map)
     */
    @Override
    public WorkflowInstance startWorkflowInstanceByDefinitionId(final String workflowDefinitionId, final Map<String, Object> variables) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#startWorkflowInstanceByDefinitionId(java.lang.String, java.lang.String)
     */
    @Override
    public WorkflowInstance startWorkflowInstanceByDefinitionId(final String workflowDefinitionId, final String businessKey) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#startWorkflowInstanceByDefinitionId(java.lang.String, java.lang.String, java.util.Map)
     */
    @Override
    public WorkflowInstance startWorkflowInstanceByDefinitionId(final String workflowDefinitionId, final String businessKey, final Map<String, Object> variables) {
        final WorkflowInstance wfi = new WorkflowInstance("wfDefId", "wfInstId", "busKey");
        return wfi;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#getVariables(java.lang.String)
     */
    @Override
    public Map<String, Object> getVariables(final String executionId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#getVariable(java.lang.String, java.lang.String)
     */
    @Override
    public Object getVariable(final String executionId, final String variableName) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#cancelWorkflowInstance(java.lang.String)
     */
    @Override
    public void cancelWorkflowInstance(final String workflowInstanceId) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#messageReceived(java.lang.String, java.lang.String)
     */
    @Override
    public void messageReceived(final String messageName, final String workflowInstanceId) throws MessageDispatchInvalidException {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#messageReceived(java.lang.String, java.lang.String, java.util.Map)
     */
    @Override
    public void messageReceived(final String messageName, final String workflowInstanceId, final Map<String, Object> variables) throws MessageDispatchInvalidException {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#correlateMessage(java.lang.String, java.lang.String)
     */
    @Override
    public void correlateMessage(final String messageName, final String businessKey) throws WorkflowMessageCorrelationException {
        MESSAGE = messageName;
        BUSINESS_KEY = businessKey;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#correlateMessage(java.lang.String, java.lang.String, java.util.Map)
     */
    @Override
    public void correlateMessage(final String messageName, final String businessKey, final Map<String, Object> variables) throws WorkflowMessageCorrelationException {
        MESSAGE = messageName;
        BUSINESS_KEY = businessKey;
        PROCESS_VARIABLES = variables;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#correlateMessage(java.lang.String, java.lang.String, java.util.Map, java.util.Map)
     */
    @Override
    public void correlateMessage(final String messageName, final String businessKey, final Map<String, Object> correlationKeys, final Map<String, Object> variables)
            throws WorkflowMessageCorrelationException {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#activateInstance(java.lang.String)
     */
    @Override
    public void activateInstance(final String workflowInstanceId) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#getVariables(java.lang.String, java.util.Collection)
     */
    @Override
    public Map<String, Object> getVariables(final String arg0, final Collection<String> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.wfs.api.WorkflowInstanceService#getVariableTyped(java.lang.String, java.lang.String)
     */
    @Override
    public Object getVariableTyped(String arg0, String arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
