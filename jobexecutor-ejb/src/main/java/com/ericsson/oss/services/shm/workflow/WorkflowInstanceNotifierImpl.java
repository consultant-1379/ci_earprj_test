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

import javax.ejb.*;
import javax.inject.Inject;

import com.ericsson.oss.services.wfs.api.query.*;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.wfs.WfsRetryPolicies;
import com.ericsson.oss.services.shm.jobs.common.constants.BatchWorkFlowDefinitions;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.WorkFlowObject;
import com.ericsson.oss.services.shm.jobs.common.retry.ShmWfsRetriableCommand;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;
import com.ericsson.oss.services.wfs.jee.api.WorkflowQueryServiceLocal;
import com.ericsson.oss.services.wfs.jee.api.WorkflowUsertaskServiceLocal;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Traceable
public class WorkflowInstanceNotifierImpl implements WorkflowInstanceNotifier {

    private static final String WORKFLOW_SERVICE_INVOCATION_FAILED = "Workflow service invocation failed";

    private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceNotifierImpl.class);

    @EServiceRef
    private WorkflowInstanceServiceLocal workflowInstanceServiceLocal;

    @EServiceRef
    private WorkflowQueryServiceLocal workflowQueryServiceLocal;

    @EServiceRef
    private WorkflowUsertaskServiceLocal workflowUsertaskServiceLocal;

    @Inject
    private RetryManager retryManager;

    @Inject
    private WfsRetryPolicies wfsRetryPolicies;

    @Override
    public boolean sendActivate(final String businessKey, final Map<String, Object> processVariables) {
        try {
            return correlateWithRetry(WorkFlowConstants.ACTIVATE_WFMESSAGE, businessKey, processVariables);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }

    @Override
    public boolean sendCancelMOActionDone(final String businessKey, final Map<String, Object> processVariables) {
        try {
            return correlateWithRetry(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey, processVariables);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }

    /**
     * This method correlates the All NE Done message after execution of all NEs are finished.
     *
     * @param businessKey - businessKey of NEJob
     * @return boolean isNotified
     */
    @Override
    public boolean sendAllNeDone(final String businessKey) {
        try {
            return correlateWithRetry(WorkFlowConstants.ALL_NE_DONE, businessKey, null);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            return false;
        }
    }

    @Override
    public boolean sendLoadControlMessage(final String message, final String businessKey) {
        try {
            return correlateWithRetry(message, businessKey, null);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            return false;
        }
    }

    /**
     * Cancels NE Job Workflow instance.
     *
     * @param businessKey              - businessKey of NEJob
     * @param propagateCancelToMainJob - If set true, sends a cancel message to the main Job WFS instance
     */
    @Asynchronous
    @Override
    public void sendAsynchronousMsgToWaitingWFSInstance(final String wfsMessage, final String businessKey) {
        try {
            correlateWithRetry(wfsMessage, businessKey, null);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }
    }

    @Override
    public boolean sendMessageToWaitingWFSInstance(final String wfsMessage, final String businessKey) {
        try {
            return correlateWithRetry(wfsMessage, businessKey, null);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }
    }

    /**
     * @param processVariables
     */
    @Override
    public String submitWorkFlowInstance(final Map<String, Object> processVariables, final String businessKey) {
        try {
            return startWorkflowInstanceWithRetry(processVariables, businessKey).getId();
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }

    /**
     * @param processVariables
     */
    @Override
    public WorkflowInstance submitAndGetWorkFlowInstance(final String businessKey, final Map<String, Object> processVariables) {
        try {
            return startWorkflowInstanceWithRetry(processVariables, businessKey);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }

    /**
     * @param processVariables
     */
    @Override
    public String submitWorkFlowInstance(final String businessKey, final WorkFlowObject workFlowObject) {
        try {
            return startWorkflowInstanceWithRetry(businessKey, workFlowObject);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }

    @Override
    public List<WorkflowObject> executeWorkflowQuery(final Query wfsQuery) {
        List<WorkflowObject> computedValue;
        try {
            computedValue = retryManager.executeCommand(wfsRetryPolicies.getWfsGeneralRetryPolicy(), new ShmWfsRetriableCommand<List<WorkflowObject>>() {
                @Override
                public List<WorkflowObject> execute() {
                    return workflowQueryServiceLocal.executeQuery(wfsQuery);
                }
            });
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }
        return computedValue;
    }

    @Override
    public List<WorkflowObject> executeWorkflowQueryForJobContinue(final Query wfsQuery) {
        return workflowQueryServiceLocal.executeQuery(wfsQuery);
    }

    @Override
    public void completeUserTask(final String workflowId) {
        try {
            retryManager.executeCommand(wfsRetryPolicies.getWfsGeneralRetryPolicy(), new ShmWfsRetriableCommand<Void>() {
                @Override
                public Void execute() {
                    workflowUsertaskServiceLocal.completeUsertask(workflowId);
                    return null;
                }
            });
            return;
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.workflow.WorkflowInstanceHelper#cancelWorkflowInstance(java.lang.String)
     */
    @Override
    public void cancelWorkflowInstance(final String workflowInstanceId) {
        try {
            cancelWorkflowInstanceWithRetry(workflowInstanceId);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exhausted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }
    }

    private void cancelWorkflowInstanceWithRetry(final String workflowInstanceId) {
        retryManager.executeCommand(wfsRetryPolicies.getWfsSubmitRetryPolicy(), new ShmWfsRetriableCommand<Void>() {
            @Override
            public Void execute() {
                workflowInstanceServiceLocal.cancelWorkflowInstance(workflowInstanceId);
                return null;
            }
        });
        return;
    }

    private WorkflowInstance startWorkflowInstanceWithRetry(final Map<String, Object> processVariables, final String businessKey) {
        final WorkflowInstance workflowInstance = retryManager.executeCommand(wfsRetryPolicies.getWfsSubmitRetryPolicy(), new ShmWfsRetriableCommand<WorkflowInstance>() {
            @Override
            public WorkflowInstance execute() {
                final WorkflowInstance workflowInstance = workflowInstanceServiceLocal
                        .startWorkflowInstanceByDefinitionId(BatchWorkFlowDefinitions.BATCH_WORKFLOW_ID_LATEST.getBatchWorkFlowDefinition(), businessKey, processVariables);
                logger.debug("Workflow instance is submitted for over all batch execution with wfs id:{} & job template id:{}", workflowInstance.getId(),
                        processVariables.get(WorkFlowConstants.TEMPLATE_JOB_ID));
                return workflowInstance;
            }
        });
        return workflowInstance;
    }

    private String startWorkflowInstanceWithRetry(final String businessKey, final WorkFlowObject workFlowObject) {
        final String wfsId = retryManager.executeCommand(wfsRetryPolicies.getWfsGeneralRetryPolicy(), new ShmWfsRetriableCommand<String>() {
            @Override
            public String execute() {
                final WorkflowInstance workflowInstance = workflowInstanceServiceLocal.startWorkflowInstanceByDefinitionId(workFlowObject.getWorkflowDefinitionId(), businessKey,
                        workFlowObject.getProcessVariables());
                return workflowInstance.getId();
            }
        });
        return wfsId;
    }

    private boolean correlateWithRetry(final String message, final String businessKey, final Map<String, Object> processVariables) {
        boolean isMsgCorrelated = false;
        isMsgCorrelated = retryManager.executeCommand(wfsRetryPolicies.getWfsGeneralRetryPolicy(), new ShmWfsRetriableCommand<Boolean>() {
            @Override
            public Boolean execute() throws WorkflowMessageCorrelationException {
                if (processVariables != null && !processVariables.isEmpty()) {
                    workflowInstanceServiceLocal.correlateMessage(message, businessKey, processVariables);
                    logger.debug("Correlating  message {} businessKey {} processVariables {}", message, businessKey, processVariables);
                } else {
                    workflowInstanceServiceLocal.correlateMessage(message, businessKey);
                    logger.debug("Correlating  message {} businessKey {} ", message, businessKey);
                }
                logger.info("Successfully correlated message = {} , for businesskey = {} processVariables {}", message, businessKey, processVariables);
                return true;
            }
        });
        return isMsgCorrelated;
    }

    @Asynchronous
    @Override
    public void asyncCorrelateActiveWorkflow(final String message, final String businessKey, final String wfsId, final Map<String, Object> processVariables) {
        try {
            if (processVariables != null && !processVariables.isEmpty()) {
                workflowInstanceServiceLocal.correlateMessage(message, businessKey, processVariables);
                logger.debug("Correlating  message {} businessKey {} processVariables {}", message, businessKey, processVariables);
            } else {
                workflowInstanceServiceLocal.correlateMessage(message, businessKey);
                logger.debug("Correlating  message {} businessKey {} ", message, businessKey);
            }
        } catch (final Exception e) {
            logger.error("Workflow Message Correlation failed for businessKey = {}, wfsId={}  due to::{}", businessKey, wfsId, e.getMessage());
            //In case of cancel mainjob correlation, the workflow correlation will be done synchronously and hence workflow instance won't be present if correlation was done by other thread.
            if (isWFSInstanceAlive(wfsId)) {
                logger.debug("Workflow instance for businessKey::{} is still active,so retrying to correlate.", businessKey);
                correlateWithRetry(message, businessKey, processVariables);
            }
        }
    }

    //This method will check whether workflow instance is present in DB or not.
    private boolean isWFSInstanceAlive(final String wfsId) {
        try {
            if (wfsId != null) {
                final QueryBuilder batchWorkflowQueryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
                final Query wfsQuery = batchWorkflowQueryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
                final RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
                final Restriction restrictionOnWfsId = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId);
                wfsQuery.setRestriction(restrictionOnWfsId);
                final List<WorkflowObject> wfsInstances = executeWorkflowQuery(wfsQuery);
                logger.info("Result for WFS query::{}  is::{}", wfsQuery, wfsInstances);
                return wfsInstances != null && !wfsInstances.isEmpty();
            } else {
                logger.error("Wfs Id is null and hence cannot retrieve workflow instance");
                return false;
            }
        } catch (final Exception e) {
            logger.error("Workflow execution query failed for wfsID:{}, due to :", wfsId, e);
            return false;
        }

    }

    @Override
    public boolean sendPrecheckOrTimeoutMsgToWfsInstance(final String businessKey, final Map<String, Object> processVariables, final String message) {
        try {
            return correlateWithRetry(message, businessKey, processVariables);
        } catch (final RetriableCommandException e) {
            logger.error("All retries are exauhsted and failed to connect to WFS, due to [cause={}] ::{}", e.getCause(), e);
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }

    @Override
    public void notifySyncActivity(final String businessKey, final Map<String, Object> processVariables) {
        try {
            workflowInstanceServiceLocal.correlateMessage(WorkFlowConstants.SYNCHRONOUS_ACTIVATE_WFMESSAGE, businessKey);
        } catch (WorkflowMessageCorrelationException e) {
            throw new WorkflowServiceInvocationException(WORKFLOW_SERVICE_INVOCATION_FAILED, e);
        }

    }
}
