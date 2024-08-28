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
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;

/**
 * Service Bean implementing Job Restore operations.
 * 
 * @author tcsvisr
 * 
 */
@Traceable
@Profiled
@Stateless
public class JobRestoreHandlingServiceUtil {

    private static final Logger logger = LoggerFactory.getLogger(JobRestoreHandlingServiceUtil.class);

    @Inject
    WorkFlowQueryServiceImpl workFlowQueryServiceImpl;

    @Inject
    private DpsReader dpsReader;

    @EServiceRef
    DataPersistenceService dataPersistenceService;

    @Inject
    DpsWriter dpsWriter;

    @Inject
    JobQueryService jobQueryService;

    /**
     * This method retrieves the suspended Job details.
     * 
     * @return JobAttributes
     */
    public List<MainJob> getSuspendedJobDetails() {
        logger.debug("Retrieving suspended batch workflows");

        List<MainJob> jobAttributesList = new ArrayList<MainJob>();

        //Fetching list of Work Flow Objects having suspended batch work flows
        final List<WorkflowObject> workFlowObjectList = workFlowQueryServiceImpl.getSuspendedBatchWorkflows();

        if (workFlowObjectList != null && !workFlowObjectList.isEmpty()) {

            //Retrieving work flow instance Id and main job Id for provided work flow object 
            jobAttributesList = jobQueryService.retrieveMainJobAttributes(workFlowObjectList);
            logger.debug("Suspended batches : {}", jobAttributesList.size());
        } else {
            logger.debug("No Batch work flow are in suspended state.");
        }
        return jobAttributesList;
    }

    /**
     * @param neNames
     * @return
     */
    public List<NEJob> getSuspendedNEJobDetails(final long mainJobId) {
        logger.debug("Retrieving suspended NE workflows for the main job : {}", mainJobId);

        List<NEJob> neJobAttributes = null;
        final List<WorkflowObject> workFlowObjectList = workFlowQueryServiceImpl.getSuspendedNEWorkflows();

        if (workFlowObjectList != null && !workFlowObjectList.isEmpty()) {

            final List<String> wfsIdList = workFlowQueryServiceImpl.getWorkFlowInstanceIdList(workFlowObjectList);

            neJobAttributes = jobQueryService.retrieveNEJobAttributes(mainJobId, wfsIdList);

        } else {
            logger.debug("No Batch work flow are in suspended state.");
        }
        return neJobAttributes;
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateJobsInBatch(final Map<Long, Map<String, Object>> batchJobsToBeUpdated) {
        logger.debug("Inside updateJobsInBatch");
        final Set<Long> jobIdSet = batchJobsToBeUpdated.keySet();
        final List<Long> jobIdList = new ArrayList<Long>(jobIdSet);
        final List<PersistenceObject> jobPOsList = dpsReader.findPOsByPoIds(jobIdList);
        for (final PersistenceObject jobPo : jobPOsList) {

            final Map<String, Object> attributeMap = batchJobsToBeUpdated.get(jobPo.getPoId());

            final Map<String, Object> poAttributes = jobPo.getAllAttributes();
            if (batchJobsToBeUpdated.get(jobPo.getPoId()).get(ShmConstants.LOG) != null) {
                final List<Map<String, Object>> jobLogList = (List<Map<String, Object>>) batchJobsToBeUpdated.get(jobPo.getPoId()).get(ShmConstants.LOG);
                if ((!poAttributes.isEmpty()) && (poAttributes != null)) {

                    List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
                    if (poAttributes.get(ActivityConstants.JOB_LOG) != null) {
                        activityJobLogList = (List<Map<String, Object>>) poAttributes.get(ActivityConstants.JOB_LOG);
                    }
                    activityJobLogList.addAll(jobLogList);
                    attributeMap.put(ShmConstants.LOG, activityJobLogList);

                }
            }
            logger.debug("Updating Job with PO Id {} to SYSTEM_CANCELLED ", jobPo.getPoId());
            dpsWriter.update(jobPo.getPoId(), attributeMap);
        }
    }
}
