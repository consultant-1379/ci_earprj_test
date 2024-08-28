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
package com.ericsson.oss.services.shm.system.restore.common;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.jobs.common.constants.BatchWorkFlowDefinitions;
import com.ericsson.oss.services.shm.jobs.common.constants.LatestNeWorkFlowDefinitions;
import com.ericsson.oss.services.shm.jobs.common.constants.OldWorkFlowDefinitions;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryBuilder;
import com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.Restriction;
import com.ericsson.oss.services.wfs.api.query.RestrictionBuilder;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;

/**
 * Service Bean implementing Work flow Query Service operations.
 * 
 * @author tcsvisr
 * 
 */
@Traceable
@Profiled
@Stateless
public class WorkFlowQueryServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(WorkFlowQueryServiceImpl.class);
    @Inject
    private WorkflowInstanceNotifier workflowInstanceHelper;

    /**
     * This method queries the work flow service for suspended batch work flows.
     * 
     * @return List<WorkflowObject>
     */
    public List<WorkflowObject> getSuspendedBatchWorkflows() {
        final List<WorkflowObject> allBatchWorkFlowList = new ArrayList<WorkflowObject>();
        final QueryBuilder batchWorkflowQueryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
        final Query query = batchWorkflowQueryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
        final RestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction suspendedStateWorkflows = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.STATE, WorkflowInstanceQueryAttributes.State.SUSPENDED);

        final List<String> batchWorkflowList = new ArrayList<String>();
        batchWorkflowList.addAll(BatchWorkFlowDefinitions.getBatchWorkflowList());

        for (final String workflowDefId : batchWorkflowList) {
            final Restriction batchWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID, workflowDefId);
            final Restriction allRestrictions = restrictionBuilder.allOf(suspendedStateWorkflows, batchWorkflowRestriction);
            query.setRestriction(allRestrictions);
            List<WorkflowObject> batchWorkFlowList = new ArrayList<WorkflowObject>();
            try {
                batchWorkFlowList = workflowInstanceHelper.executeWorkflowQuery(query);
            } catch (final WorkflowServiceInvocationException ex) {
                logger.error("Exception occurred while retrieving batch workflow objects:{}. Exception is: ", workflowDefId, ex);
            }
            allBatchWorkFlowList.addAll(batchWorkFlowList);
            logger.info("Number of all suspended batch work flows: {}", batchWorkflowList.size());
        }
        return allBatchWorkFlowList;
    }

    /**
     * This method queries work flow service for suspended batch work flows.
     * 
     * @return List<WorkflowObject>
     */
    public List<WorkflowObject> getSuspendedNEWorkflows() {
        final List<WorkflowObject> allNeWorkFlowList = new ArrayList<WorkflowObject>();
        final QueryBuilder queryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
        final Query query = queryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
        final RestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction suspendedStateRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.STATE, WorkflowInstanceQueryAttributes.State.SUSPENDED);
        Restriction allRestrictions = null;

        final List<String> neWorkflowList = new ArrayList<String>();
        neWorkflowList.addAll(LatestNeWorkFlowDefinitions.getNeWorkflowList());
        neWorkflowList.addAll(OldWorkFlowDefinitions.getOldWorkflowList());

        for (final String workflowDefId : neWorkflowList) {
            final Restriction neWorkflowRestriction = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_DEFINITION_ID, workflowDefId);
            allRestrictions = restrictionBuilder.allOf(suspendedStateRestriction, neWorkflowRestriction);
            query.setRestriction(allRestrictions);
            List<WorkflowObject> neWorkFlowList = new ArrayList<WorkflowObject>();
            try {
                neWorkFlowList = workflowInstanceHelper.executeWorkflowQuery(query);
            } catch (final WorkflowServiceInvocationException e) {
                logger.error("{}", e);
            }
            logger.debug("Number of suspended NE work flows: {}", neWorkFlowList.size());
            allNeWorkFlowList.addAll(neWorkFlowList);
            logger.debug("Number of suspended All NE work flows: {}", allNeWorkFlowList.size());
        }

        return allNeWorkFlowList;
    }

    /**
     * This method retrieves the work flow instance ID.
     * 
     * @param workFlowObjectList
     * @return List<String>
     */
    public List<String> getWorkFlowInstanceIdList(final List<WorkflowObject> workFlowObjectList) {
        final List<String> wfsIdList = new ArrayList<String>();
        for (final WorkflowObject workflowObject : workFlowObjectList) {

            final String wfsInstanceId = (String) workflowObject.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID);
            logger.debug("Work Flow Instance Id : {}", wfsInstanceId);
            wfsIdList.add(wfsInstanceId);
        }
        return wfsIdList;
    }

}
