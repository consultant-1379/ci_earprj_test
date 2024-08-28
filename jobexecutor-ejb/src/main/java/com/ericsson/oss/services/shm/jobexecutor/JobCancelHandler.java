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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryBuilder;
import com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.Restriction;
import com.ericsson.oss.services.wfs.api.query.RestrictionBuilder;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import com.ericsson.oss.services.wfs.api.query.usertask.UsertaskQueryAttributes;

/**
 * To Cancel the Jobs by correlating the relevant messages to the actively waiting WFS instances. It also cancels the parent workflows explicitly in case the message correlation fails(here parent
 * workflow will go into hanged state).
 * 
 * @author xrajeke
 * 
 */
@Traceable
public class JobCancelHandler {

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private DpsReader dpsReader;
    
    @Inject
    JobConfigurationService jobConfigurationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCancelHandler.class);

    /**
     * Cancels the NE Jobs and running activity jobs. The cancel activity will be done as it, Correlates the 'cancel' message to the given business key workflow. If correlation fails, then the
     * workflow will be cancelled explicitly.
     * 
     * @param businessKey
     */
    public void cancelNEJobWorkflows(final String businessKey) {
        try {
            workflowInstanceNotifier.sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELNEJOB_WFMESSAGE, businessKey);
        } catch (WorkflowServiceInvocationException e) {
            LOGGER.error("Message '{}' correlation failed for businessKey:{}, Now the Workflows will be cancelled explisitly.", WorkFlowConstants.CANCELNEJOB_WFMESSAGE, businessKey);

            cancelNEWorkFlowExplicitly(businessKey);
            systemRecorder.recordEvent("SHM.JOB.CANCEL", EventLevel.COARSE, "CANCEL Job", businessKey, "SHM Job for businessKey:" + businessKey + " cancelled explisitly.");
        }
    }

    private void cancelNEWorkFlowExplicitly(final String businessKey) {
        LOGGER.info("Querying to the WFS database for a businessKey:{}, to identify for any reported incidents, and to cancel the respective workflow.", businessKey);
        final QueryBuilder queryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
        final Query wfsQuery = queryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
        final RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
        final Restriction businessKeyRestriction = restrictionBuilder.isEqual(UsertaskQueryAttributes.QueryParameters.BUSINESS_KEY, businessKey);
        wfsQuery.setRestriction(businessKeyRestriction);

        final List<WorkflowObject> neJobWorkflowObjects = workflowInstanceNotifier.executeWorkflowQuery(wfsQuery);

        for (WorkflowObject neJobWorkflow : neJobWorkflowObjects) {
            LOGGER.debug("NE Job workflow Object attributes::{}", neJobWorkflow.getAttributes());
            final String workflowInstanceId = (String) neJobWorkflow.getAttribute(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID);

            try {
                workflowInstanceNotifier.cancelWorkflowInstance(workflowInstanceId);
                LOGGER.info("Workflow cancelled explisitly for workflowInstanceId::{}", workflowInstanceId);
            } catch (WorkflowServiceInvocationException e) {
                LOGGER.error("Unable to cancel the Workflow for workflowInstanceId:{}, due to ::{}", workflowInstanceId, e.getMessage());
            }
        }

        updateJobsAsCancelled(businessKey);

    }

    private void updateJobsAsCancelled(final String businessKey) {
        final Map<Object, Object> neJobRestrictionAttributes = new HashMap<>();
        neJobRestrictionAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        final List<Map<String, Object>> neJobAttributesList = dpsReader.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, neJobRestrictionAttributes,
                Arrays.asList(ShmConstants.NE_NAME));

        for (final Map<String, Object> neJobAttributes : neJobAttributesList) {
            final long neJobId = (long) neJobAttributes.get(ShmConstants.PO_ID);
            final String neName = (String) neJobAttributes.get(ShmConstants.NE_NAME);

            final Map<Object, Object> activityJobRestrictionAttributes = new HashMap<>();
            activityJobRestrictionAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
            activityJobRestrictionAttributes.put(ShmConstants.STATE, JobStateEnum.RUNNING.name());
            //get only the RUNNING job to be updated as CANCELLED, and leave the rest of the activity jobs as is.
            final List<Map<String, Object>> activityJobAttributes = dpsReader.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, activityJobRestrictionAttributes,
                    Arrays.asList(ShmConstants.NAME));
            for (final Map<String, Object> activityJobAttributesMap : activityJobAttributes) {
                final long activityJobId = (long) activityJobAttributesMap.get(ShmConstants.PO_ID);
                final String activityName = (String) neJobAttributes.get(ShmConstants.NAME);
                LOGGER.warn("{} Activity on Node:{} will be cancelled explicitly.", activityName, neName);
                updateJobEnd(activityJobId);
            }

            LOGGER.warn("NEJob on Node:{} is going to be cancelled explisitly.", neName);
            updateJobEnd(neJobId);
        }
    }

    private void updateJobEnd(final long jobId) {
        if (!jobConfigurationService.isJobResultEvaluated(jobId)) {
            final Map<String, Object> cancelledJobAttributes = new HashMap<>();
            cancelledJobAttributes.put(ShmConstants.RESULT, JobResult.CANCELLED.name());
            cancelledJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
            cancelledJobAttributes.put(ShmConstants.ENDTIME, new Date());
            jobUpdateService.updateJobAttributes(jobId, cancelledJobAttributes);
        }else {
            LOGGER.warn("Update Job End is skipping for {} as result is already Evaluated.", jobId);
        }
    }
}
