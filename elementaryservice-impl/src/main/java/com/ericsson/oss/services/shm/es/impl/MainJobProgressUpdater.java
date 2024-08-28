/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * Updates the Main Job attributes.
 * 
 * @author xrajeke
 */
@Stateless
public class MainJobProgressUpdater {

    final static private Logger LOGGER = LoggerFactory.getLogger(MainJobProgressUpdater.class);

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private MainJobProgressCalculator progressCalculator;

    @Inject
    private MainJobProgressDPSHelper mainJobProgressDPSHelper;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    /**
     * To update the Main Job Progress Percentage.
     * 
     * @param mainJobId
     * @return void
     */
    public void updateMainJobProgress(final long mainJobId) {
        final Map<String, Object> mainJobAttributes = mainJobProgressDPSHelper.getMainJobAttributes(mainJobId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID);
        final int totalNEs = (int) mainJobAttributes.get(ShmConstants.NO_OF_NETWORK_ELEMENTS);
        final List<Map<String, Object>> neJobsProgress = mainJobProgressDPSHelper.getNeJobsProgress(mainJobId);
        final double currentProgress = progressCalculator.calculateMainJobProgressPercentage(mainJobId, totalNEs, neJobsProgress);
        final double previousProgress = (double) mainJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
        updateNeJobsCompletedCount(mainJobId, currentProgress, previousProgress, mainJobAttributes);
    }

    /**
     * @param mainJobId
     * @param mainJobPO
     */
    private void updateNeJobsCompletedCount(final long mainJobId, final double currentProgress, final double previousProgress,
                                            final Map<String, Object> mainJobAttributes) {
        int neSubmittedCount = 0;
        final int currentNeCompletedCount = getInActiveNeJobsCount(mainJobId);
        final List<Map<String, String>> mainJobPropertyList = (List<Map<String, String>>) mainJobAttributes.get(ShmConstants.JOBPROPERTIES);
        boolean isUpdateNeeded = false;
        if (mainJobPropertyList != null && !mainJobPropertyList.isEmpty()) {
            for (final Map<String, String> jobProperty : mainJobPropertyList) {
                if (ShmConstants.SUBMITTED_NES.equals(jobProperty.get(ShmConstants.KEY))) {
                    final String value = jobProperty.get(ShmConstants.VALUE);
                    neSubmittedCount = Integer.parseInt(value);
                }
                if (ShmConstants.NE_COMPLETED.equals(jobProperty.get(ShmConstants.KEY))) {
                    //This key is being inserted during NE Job creation itself (Refer to JobExecutionService.updateNeCompletedCount(), line 683).
                    //So definitely it will found here, directly updating with new value.
                    if (currentNeCompletedCount > Integer.parseInt(jobProperty.get(ShmConstants.VALUE))) {
                        jobProperty.put(ShmConstants.VALUE, Integer.toString(currentNeCompletedCount));
                        isUpdateNeeded = true;
                    }
                }
            }
        }
        mainJobProgressDPSHelper.updateProgressAndProperties(mainJobId, mainJobPropertyList, currentProgress, previousProgress, isUpdateNeeded);

        //Notify to WFS to end the Main job, if all submitted NE's are completed.
        if (neSubmittedCount != 0 && neSubmittedCount == currentNeCompletedCount) {
            LOGGER.debug("All submitted and valid NetworkElements[{}] are completed[{}] their NE Job execution.", neSubmittedCount,
                    currentNeCompletedCount);
            final long templateJobId = (long) mainJobAttributes.get(ShmConstants.JOB_TEMPLATE_ID);
            final boolean isMsgCorrelated = workflowInstanceNotifier.sendAllNeDone(Long.toString(templateJobId));
            LOGGER.debug("All NE Job Done message sent[isMsgCorrelated={}] to wrokflow for templateJobId: {}", isMsgCorrelated, templateJobId);
        }

    }

    /**
     * @param mainJobId
     * @return
     */
    private int getInActiveNeJobsCount(final long mainJobId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();

        final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.NE_JOB);
        final TypeRestrictionBuilder resrtictionBuilder = jobTypeQuery.getRestrictionBuilder();
        final Restriction jobTemplateRestriction = resrtictionBuilder.equalTo(ShmConstants.MAINJOBID, mainJobId);
        final Restriction jobStateRestriction = resrtictionBuilder.in(ShmJobConstants.STATE, (Object[]) JobStateEnum.getInactiveJobstates());
        jobTypeQuery.setRestriction(resrtictionBuilder.allOf(jobTemplateRestriction, jobStateRestriction));

        final List<Object> resultList = queryExecutor.getResultList(jobTypeQuery);
        return resultList.size();
    }

    /**
     * Updates the Main job with required attributes to fulfill the completion criteria (like ENDTIME,STATE,RESULT,PROGRESSPERCENTAGE )
     * 
     * @param mainJobPoId
     * @param mainJobEndAttributes
     * 
     */
    public void updateMainJobEndDetails(final long mainJobPoId, final Map<String, Object> mainJobEndAttributes) {
        final Map<String, Object> mainJobAttributes = mainJobProgressDPSHelper.getMainJobAttributes(mainJobPoId,ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.PROGRESSPERCENTAGE, ShmConstants.JOBPROPERTIES,
                ShmConstants.JOB_TEMPLATE_ID);
        final int totalNEs = (int) mainJobAttributes.get(ShmConstants.NO_OF_NETWORK_ELEMENTS);
        final List<Map<String, Object>> neJobsProgress = mainJobProgressDPSHelper.getNeJobsProgress(mainJobPoId);
        final double aggProgressPercentage = progressCalculator.calculateMainJobProgressPercentage(mainJobPoId, totalNEs, neJobsProgress);
        mainJobEndAttributes.put(ShmConstants.PROGRESSPERCENTAGE, aggProgressPercentage);
        mainJobProgressDPSHelper.updateMainJobAttributes(mainJobPoId, mainJobEndAttributes);
        LOGGER.debug("MainJob [id:{}] is ended with attributes:{}", mainJobPoId, mainJobEndAttributes);
    }

}
