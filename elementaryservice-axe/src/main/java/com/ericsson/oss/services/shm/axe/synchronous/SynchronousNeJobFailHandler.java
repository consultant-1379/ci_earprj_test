/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.axe.synchronous;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryBuilder;
import com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import com.ericsson.oss.services.wfs.api.query.usertask.UsertaskQueryAttributes;

/**
 * Class contains implementation to fail the nejob when any activity gets failed ,as the activity has to be synchronous with other nejobs
 * 
 * @author ztamsra
 *
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SynchronousNeJobFailHandler {

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private AxeSynchronousActivityProxyService axeSynchronousActivityProxyService;

    @Inject
    private ActivityUtils activityUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousNeJobFailHandler.class);

    public void failSynchronousNeJobExplicitly(final String businessKey) {
        LOGGER.info("Querying to the WFS database for a businessKey:{}, to identify for any reported incidents, and to cancel the respective workflow.", businessKey);
        final QueryBuilder queryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
        final Query wfsQuery = queryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
        final com.ericsson.oss.services.wfs.api.query.RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
        final com.ericsson.oss.services.wfs.api.query.Restriction businessKeyRestriction = restrictionBuilder.isEqual(UsertaskQueryAttributes.QueryParameters.BUSINESS_KEY, businessKey);
        wfsQuery.setRestriction(businessKeyRestriction);

        final List<WorkflowObject> neJobWorkflowObjects = workflowInstanceNotifier.executeWorkflowQuery(wfsQuery);

        for (final WorkflowObject neJobWorkflow : neJobWorkflowObjects) {
            LOGGER.debug("NE Job workflow Object attributes::{}", neJobWorkflow.getAttributes());
            final String workflowInstanceId = (String) neJobWorkflow.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID);
            try {
                workflowInstanceNotifier.cancelWorkflowInstance(workflowInstanceId);
                LOGGER.info("Workflow cancelled explisitly for workflowInstanceId::{}", workflowInstanceId);
            } catch (WorkflowServiceInvocationException e) {
                LOGGER.error("Unable to cancel the Workflow for workflowInstanceId:{}, due to ::{}", workflowInstanceId, e.getMessage());
            }
        }
        updateJobEnd(axeSynchronousActivityProxyService.getNeJobId(businessKey));
    }

    private void updateJobEnd(final long jobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        activityUtils.addJobLog(String.format(JobLogConstants.AXE_SYNCHRONOUS_NE_FAIL_EXPLICITLY), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        if (jobUpdateService.readAndUpdateRunningJobAttributes(jobId, null, jobLogList, null)) {
            final Map<String, Object> failedJobAttributes = new HashMap<>();
            final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
            failedJobAttributes.put(ShmConstants.RESULT, JobResult.FAILED.name());
            failedJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
            failedJobAttributes.put(ShmConstants.ENDTIME, new Date());
            jobPropertiesList.add(failedJobAttributes);
            jobUpdateService.updateJobAttributes(jobId, failedJobAttributes);
        }

    }
}
