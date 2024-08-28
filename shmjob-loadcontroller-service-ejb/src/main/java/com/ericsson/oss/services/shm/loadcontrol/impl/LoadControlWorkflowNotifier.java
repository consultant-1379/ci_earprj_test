/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.impl;

import java.util.List;

import javax.ejb.EJBException;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.wfs.WfsRetryPolicies;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.api.WorkflowServiceException;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryBuilder;
import com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.Restriction;
import com.ericsson.oss.services.wfs.api.query.RestrictionBuilder;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

public class LoadControlWorkflowNotifier {

    private final Logger logger = LoggerFactory.getLogger(LoadControlWorkflowNotifier.class);

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @EServiceRef
    private WorkflowInstanceServiceLocal workflowInstanceServiceLocal;

    @Inject
    private WfsRetryPolicies wfsRetryPolicies;

    @Inject
    private RetryManager retryManager;

    public boolean correlate(final String businessKey, final String wfsId) {
        boolean isMsgCorrelated = false;
        try {
            workflowInstanceServiceLocal.correlateMessage(WorkFlowConstants.RESOURCE_AVAILABLE, businessKey);
            isMsgCorrelated = true;
        } catch (final WorkflowMessageCorrelationException e) {
            logger.error("Workflow Message Correlation failed for businessKey={}, wfsId={}  due to::{}", businessKey, wfsId, e.getMessage());
            if (isWFSInstanceAlive(wfsId)) {
                logger.debug("Workflow instance for businessKey::{} is still active,so retrying to correlate.", businessKey);
                isMsgCorrelated = correlateWithRetry(businessKey);
            }
        } catch (final Exception e) {
            logger.error("Workflow Message Correlation failed for businessKey::{}, wfsId::{}  due to::{}", businessKey, wfsId, e.getMessage());
            isMsgCorrelated = false;
        }
        return isMsgCorrelated;
    }

    /**
     * 
     * Queries the postgres DB , whether the workflow instance exists for given wfsID or not.
     * <p>
     * Returns:-
     * <li>TRUE - If workflow instance is found in postgres DB. (OR ) Incase of any exception occurs (assumes the Workflow is active for retrying).
     * <li>FALSE - If workflow instance is not found.
     * 
     * @param wfsId
     * @return
     *         <li>Boolean
     */
    public boolean isWFSInstanceAlive(final String wfsId) {
        try {
            final QueryBuilder batchWorkflowQueryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
            final Query wfsQuery = batchWorkflowQueryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
            final RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
            final Restriction restrictionOnWfsId = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId);
            wfsQuery.setRestriction(restrictionOnWfsId);
            final List<WorkflowObject> wfsInstances = workflowInstanceNotifier.executeWorkflowQuery(wfsQuery);
            logger.trace("Result for WFS query::{}  is::{}", wfsQuery, wfsInstances);
            return wfsInstances != null && !wfsInstances.isEmpty();
        } catch (final Exception e) {
            logger.error("Workflow execution query failed for wfsID:{}, due to :", wfsId, e);
            return true;
        }

    }

    private boolean correlateWithRetry(final String businessKey) {
        try {
            return retryManager.executeCommand(wfsRetryPolicies.getWfsLoadControlRetryPolicy(), new RetriableCommand<Boolean>() {
                @Override
                public Boolean execute(final RetryContext retryContext) {
                    logger.info("Retrying WFS call for Resource available correlation ({}) times for businessKey : {}", retryContext.getCurrentAttempt(), businessKey);
                    try {
                        workflowInstanceServiceLocal.correlateMessage(WorkFlowConstants.RESOURCE_AVAILABLE, businessKey);
                        return true;
                    } catch (WorkflowMessageCorrelationException e) {
                        throw new WorkflowServiceInvocationException(ShmCommonConstants.WORKFLOW_SERVICE_INTERNAL_ERROR, e);
                    } catch (RuntimeException e) {
                        logger.error("call to WFS failed in retrymanager due to {}, ", e);
                        throwWFSException(e);
                        throw e;
                    }
                }
            });
        } catch (final Exception e) {
            logger.error("Workflow Message Correlation Retry failed for businessKey:{} due to::{}", businessKey, e.getMessage());
            return false;
        }
    }

    private void throwWFSException(final RuntimeException e) {
        if (e instanceof IllegalStateException || e instanceof EJBException || e instanceof WorkflowServiceException) {
            throw new WorkflowServiceInvocationException(ShmCommonConstants.WORKFLOW_SERVICE_INTERNAL_ERROR, e);
        }
    }

}