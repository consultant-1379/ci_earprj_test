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
package com.ericsson.oss.services.shm.job.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class will delete NE jobs and activity jobs.
 * 
 * @param jobPoId
 * @return boolean value
 */

@Stateless
public class JobDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(JobDeletionService.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private SHMTBACHandler shmTbacHandler;

    @Inject
    private HcJobRetryProxy hcJobRetryProxy;

    @Inject
    private UserContextBean userContextBean;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Method to retrieve activity JobPoIds from neJobId and deletes activityJob's and it's NeJob.
     * 
     * @param jobPoId
     * @return boolean value
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean deleteActivityAndNeJobAndReturnStatus(final Long jobPoId, final String jobType) {
        final long startTime = System.currentTimeMillis();
        boolean deletionStatus = false;
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        try {
            final List<Long> activityJobPoIdsList = getJobPoIdsFromJobId(ShmConstants.ACTIVITY_JOB, ShmConstants.NE_JOB_ID, jobPoId);
            final List<PersistenceObject> jobPOsList = liveBucket.findPosByIds(activityJobPoIdsList);
            int failureCount = 0;
            for (final PersistenceObject po : jobPOsList) {
                final long poId = po.getPoId();
                try {
                    deleteJobPo(po, liveBucket);
                } catch (final RuntimeException exception) {
                    logger.error("Exception occured while deleting ActivityJob with poId {} and its NeJobId {} - Exception {}", poId, jobPoId, exception);
                    dpsAvailabilityInfoProvider.checkDatabaseAvailability(exception);
                    failureCount++;
                }
            }
            if (failureCount == 0) {
                final PersistenceObject neJobPo = liveBucket.findPoById(jobPoId);
                final String nodeName = neJobPo.getAttribute(ShmConstants.NE_NAME);
                final String loggedInUser = userContextBean.getLoggedInUserName();
                if (JobType.NODE_HEALTH_CHECK.toString().equals(jobType) && (loggedInUser == null || shmTbacHandler.isAuthorized(loggedInUser, nodeName))) {
                    deleteHealthCheckJobFromNode(liveBucket, neJobPo);
                    deleteAlarms(liveBucket, neJobPo);
                }

                deleteJobPo(neJobPo, liveBucket);
                deletionStatus = true;
            }
        } catch (final RuntimeException exception) {
            logger.error("Exception occured while fecthing activityJobPoIds List for NeJobId {} - Exception {}", jobPoId, exception);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(exception);
        }
        logger.info("Total time taken for deletion of a NeJob & its ActivityJobs in {} miliseconds.", System.currentTimeMillis() - startTime);
        return deletionStatus;
    }

    private void deleteHealthCheckJobFromNode(final DataBucket liveBucket, final PersistenceObject neJobPo) {
        try {
            final List<Map<String, Object>> jobProperties = neJobPo.getAttribute(ShmConstants.JOBPROPERTIES);
            final String hcJobFdn = getPropertyValue(jobProperties, ShmConstants.HC_JOB_FDN);
            if (!hcJobFdn.isEmpty()) {
                final PersistenceObject hcJobMo = liveBucket.findMoByFdn(hcJobFdn);
                if(hcJobMo != null) {
                    final Map<String, Object> reportProgressAttributes = hcJobMo.getAttribute(ShmConstants.PROGRESS_REPORT);
                    if (!ShmConstants.RUNNING.equals(reportProgressAttributes.get(ShmConstants.STATE))) {
                        hcJobRetryProxy.deleteMoByFDN(hcJobFdn);
                    }
                }
            }
        } catch (final Exception exception) {
            logger.error("All retries exhausted while deleting HC Jobs from Node. Reason: ", exception);
            systemRecorder.recordEvent("NODE_HEALTH_CHECK.DELETE", EventLevel.COARSE, "NHC", "HcJob", exception.getMessage());
        }
    }

    private void deleteAlarms(final DataBucket liveBucket, final PersistenceObject neJobPo) {
        try {
            final List<Map<String, Object>> jobProperties = neJobPo.getAttribute(ShmConstants.JOBPROPERTIES);
            final String majorAlarmPoIds = getPropertyValue(jobProperties, ShmConstants.MAJOR_ALARM_DATA);
            final String criticalAlarmPoIds = getPropertyValue(jobProperties, ShmConstants.CRITICAL_ALARM_DATA);
            List<Long> totalAlarmPoIds = new ArrayList<>();
            List<String> majorAlarmPoIdsList = convertStringToList(majorAlarmPoIds);
            List<String> criticalAlarmPoIdsList = convertStringToList(criticalAlarmPoIds);
            if (majorAlarmPoIdsList != null) {
                prepareAlarmsData(totalAlarmPoIds, majorAlarmPoIdsList);
            }
            if (criticalAlarmPoIdsList != null) {
                prepareAlarmsData(totalAlarmPoIds, criticalAlarmPoIdsList);
            }
            
            logger.debug("Total Alarms to be deleted {} " , totalAlarmPoIds);
            if (!totalAlarmPoIds.isEmpty()) {
                final List<PersistenceObject> alarmPos = liveBucket.findPosByIds(totalAlarmPoIds);
                for (PersistenceObject persistenceObject : alarmPos) {
                    logger.debug("Deleting Alarm PO with PO ID {}" , persistenceObject.getPoId());
                    hcJobRetryProxy.deletePo(persistenceObject, liveBucket);
                }
            }
        } catch (final RetriableCommandException exception) {
            logger.error("All retries exhausted while deleting Alarms. Reason: ", exception);
            systemRecorder.recordEvent("NODE_HEALTH_CHECK.DELETE", EventLevel.COARSE, "NHC", "NodeHealthCheckAlarm", exception.getMessage());
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(exception);
        }
    }

    private List<String> convertStringToList(final String alarmPoIdString) {
        List<String> alarmPoIdsList = null;
        if(alarmPoIdString != null && !alarmPoIdString.isEmpty()){
            alarmPoIdsList = Arrays.asList(alarmPoIdString.split(JobPropertyConstants.COMMA));
        }
        return alarmPoIdsList;
    }

    private void prepareAlarmsData(List<Long> alarmPoIds, List<String> alarmsToBeProcessed) {
        for (String alarmPoId : alarmsToBeProcessed) {
            alarmPoIds.add(Long.parseLong(alarmPoId));
        }
    }

    private int deleteJobPo(final PersistenceObject jobPo, final DataBucket liveBucket) {
        return liveBucket.deletePo(jobPo);
    }

    /**
     * Method to retrieve JobPoIds by passing MainJob or NeJobId
     * 
     * @param typeOfJob
     *            it is either Job/NEJob/ActivityJob
     * @param restrictionAttribute
     *            it is either neJobId or mainJobId
     * @param jobId
     *            jobId of NeJob or MainJobId
     * 
     *            { "neJobIds":"1125899906855281,1125899906855277,1", "orderBy":"asc", "sortBy":"neName", "offset":"1", "limit":"3" }
     * @return Response object having Job Log details
     */
    public List<Long> getJobPoIdsFromJobId(final String typeOfJob, final String restrictionAttribute, final Long jobId) {

        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();

        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(restrictionAttribute, jobId);

        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = buildRestrictions(ShmConstants.NAMESPACE, typeOfJob, restrictions);

        final List<Long> neJobProjector = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID));

        final List<Long> neJobIdList = new ArrayList<Long>();
        for (final Long eachNeJobProjector : neJobProjector) {
            neJobIdList.add(eachNeJobProjector);
        }

        logger.info("List containing NE job ids is retrieved {} and Size {}", neJobIdList, neJobIdList.size());
        return neJobIdList;

    }

    private Query<TypeRestrictionBuilder> buildRestrictions(final String nameSpace, final String type, final Map<String, Object> restrictions) {

        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery(nameSpace, type);

        Restriction queryRestriction;
        final List<Restriction> restrictionList = new ArrayList<Restriction>();

        for (final Map.Entry<String, Object> entry : restrictions.entrySet()) {

            queryRestriction = query.getRestrictionBuilder().equalTo(entry.getKey(), entry.getValue());
            restrictionList.add(queryRestriction);
        }
        if (!restrictionList.isEmpty()) {
            Restriction finalRestriction = null;
            int index = 0;
            for (final Restriction restriction : restrictionList) {
                if (index == 0) {
                    finalRestriction = query.getRestrictionBuilder().allOf(restriction);
                } else {
                    finalRestriction = query.getRestrictionBuilder().allOf(finalRestriction, restriction);
                }
                index++;
            }
            query.setRestriction(finalRestriction);
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    private static String getPropertyValue(final List<Map<String, Object>> jobProperties, final String propertyName) {
        if (jobProperties != null) {
            for (final Map<String, Object> jobProperty : jobProperties) {
                if (propertyName != null && propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    logger.debug("Requested Property entry found {}", jobProperty);
                    return (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                }
            }
        }
        return "";
    }
}