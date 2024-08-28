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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
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
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityJobDetail;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class SHMJobServiceImplHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobServiceImplHelper.class);

    private static final List<String> requiredActivityJobAttributesAtNeLevel = Arrays.asList(ShmConstants.ACTIVITY_NE_STATUS, ShmConstants.ACTIVITY_ORDER, ShmConstants.ACTIVITY_NE_JOB_ID,
            ShmConstants.ACTIVITY_NAME, ShmConstants.LAST_LOG_MESSAGE);

    private static final List<String> requiredActivityJobAttributesForJobLogs = Arrays.asList(ShmConstants.ACTIVITY_NE_JOB_ID, ShmConstants.ACTIVITY_NAME, ShmConstants.LOG);

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private SHMJobServiceHelper shmJobServiceHelper;

    @Inject
    private ContextService contextService;

    @Inject
    private JobParameterChangeListener jobParameterChangeListener;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    private List<PersistenceObject> queryEachBatchOfActivityJobs(final QueryExecutor queryExecutor, final QueryBuilder queryBuilder, final List<Long> eachBatchOfNeJobIds) {
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE);
        final Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray());
        typeQuery.setRestriction(restriction);
        return queryExecutor.getResultList(typeQuery);
    }

    /**
     * This method is used to get the Activity Job attributes which required for Job Details Page.
     * 
     */
    public Map<Long, List<ActivityJobDetail>> getActivityJobAttributesIncludingLastLogMessage(final List<Long> eachBatchOfNeJobIds) {
        try {
            final Map<Long, List<ActivityJobDetail>> response = getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE, requiredActivityJobAttributesAtNeLevel,
                    eachBatchOfNeJobIds);
            return response;
        } catch (final EJBException ejbException) {
            LOGGER.error("Exception occurred while querying Activity Jobs. Reason: ", ejbException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ejbException);
            throw ejbException;
        }

    }

    private Map<Long, List<ActivityJobDetail>> getProjectedAttributes(final String namespace, final String type, final List<String> projectedAttributes, final List<Long> eachBatchOfNeJobIds) {
        final Projection attributesArray[] = new Projection[projectedAttributes.size()];
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(namespace, type);
        final Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.ACTIVITY_NE_JOB_ID, eachBatchOfNeJobIds.toArray());
        typeQuery.setRestriction(restriction);
        int projectionIndex = 0;
        for (final String attribute : projectedAttributes) {
            attributesArray[projectionIndex] = ProjectionBuilder.attribute(attribute);
            projectionIndex++;
        }
        final List<Object[]> datbaseEntries = getLiveBucket().getQueryExecutor().executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID), attributesArray);

        return getNeActivityJobDetails(datbaseEntries);

    }

    private Map<Long, List<ActivityJobDetail>> getNeActivityJobDetails(final List<Object[]> datbaseEntries) {
        LOGGER.debug("Total no. of Database entires for Activity Job : {}", datbaseEntries.size());
        final Map<String, ActivityJobDetail> activityJobsWithoutLastLog = new HashMap<String, ActivityJobDetail>();
        final Map<Long, List<ActivityJobDetail>> neActivityJobDetails = new HashMap<Long, List<ActivityJobDetail>>();
        final DataBucket liveBucket = getLiveBucket();
        for (final Object[] poObject : datbaseEntries) {
            boolean islastLogMessageAvailable = true;
            LOGGER.debug(" Updating retrieved projected attributes {}", poObject);
            final ActivityJobDetail activityJobDetail = new ActivityJobDetail();

            activityJobDetail.setActivityJobIdAsString(poObject[0].toString());
            activityJobDetail.setState((String) poObject[1]);
            activityJobDetail.setActivityOrder((int) poObject[2]);

            final Long neJobId = (Long) poObject[3];
            activityJobDetail.setNeJobId(neJobId);

            activityJobDetail.setActivityName((String) poObject[4]);
            if (poObject[5] != null) {
                activityJobDetail.setLastLogMessage((String) poObject[5]);
            } else {
                //Populate list of activity IDs if lastlogMessage is not available. Later, fetch these messages from joblog PO.
                activityJobsWithoutLastLog.put(activityJobDetail.getActivityJobIdAsString(), activityJobDetail);
                islastLogMessageAvailable = false;
            }
            if (neActivityJobDetails.containsKey(neJobId)) {
                neActivityJobDetails.get(neJobId).add(activityJobDetail);
            } else {
                final List<ActivityJobDetail> activityJobDetails = new ArrayList<ActivityJobDetail>(Arrays.asList(activityJobDetail));
                neActivityJobDetails.put(neJobId, activityJobDetails);
            }
            if (!islastLogMessageAvailable) {
                final PersistenceObject persistenceObject = findPOByPoId(liveBucket, (long) poObject[0]);
                LOGGER.info("ActivityJob with PO {} doesn't contain the lastLogMessage.", persistenceObject.getPoId());
                final HashMap<String, Object> jobLogDetails = new HashMap<String, Object>();
                jobLogDetails.put(ShmConstants.LOG, persistenceObject.getAttribute(ShmConstants.LOG));

                final ActivityJobDetail activityJobDetailWithoutLastLog = activityJobsWithoutLastLog.get(Long.toString(persistenceObject.getPoId()));

                for (final ActivityJobDetail neActivityJobDetail : neActivityJobDetails.get(neJobId)) {
                    if (neActivityJobDetail.getActivityJobIdAsString().equals(activityJobDetailWithoutLastLog.getActivityJobIdAsString())) {
                        neActivityJobDetail.setLog(jobLogDetails);
                    }
                }
            }
        }

        return neActivityJobDetails;
    }

    private DataBucket getLiveBucket() {
        try {
            return dataPersistenceService.getLiveBucket();
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving live bucket. Reason : {}", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw new ServerInternalException("Exception while reading the inventory. Please try again.");
        }
    }

    private PersistenceObject findPOByPoId(final DataBucket liveBucket, final long poId) {
        return liveBucket.findPoById(poId);
    }

    /**
     * This method is used to get the Activity Job attributes which required for View Job Logs Page.
     * 
     */
    public Map<Long, List<Map<String, Object>>> getActivityJobPoAttributes(final QueryExecutor queryExecutor, final QueryBuilder queryBuilder, final List<Long> eachBatchOfNeJobIds) {
        final Map<Long, List<Map<String, Object>>> activityJobList = new HashMap<Long, List<Map<String, Object>>>();
        try {
            final List<PersistenceObject> batchedActivityJobs = queryEachBatchOfActivityJobs(queryExecutor, queryBuilder, eachBatchOfNeJobIds);
            for (final PersistenceObject persistenceObject : batchedActivityJobs) {
                final Map<String, Object> attributes = persistenceObject.getAttributes(requiredActivityJobAttributesForJobLogs);
                attributes.put(ShmConstants.PO_ID, persistenceObject.getPoId());
                final Long activityNeJobId = (Long) attributes.get(ShmConstants.ACTIVITY_NE_JOB_ID);
                if (activityJobList.get(activityNeJobId) != null) {
                    final List<Map<String, Object>> activityJob = activityJobList.get(attributes.get(ShmConstants.ACTIVITY_NE_JOB_ID));
                    activityJob.add(attributes);
                    activityJobList.put(activityNeJobId, activityJob);
                } else {
                    final ArrayList<Map<String, Object>> activityJob = new ArrayList<Map<String, Object>>();
                    activityJob.add(attributes);
                    activityJobList.put(activityNeJobId, activityJob);
                }
            }
        } catch (final EJBException ejbException) {
            LOGGER.error("Exception occurred while querying Activity Jobs. Reason: ", ejbException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ejbException);
            throw ejbException;
        }

        return activityJobList;
    }

    @Asynchronous
    public void deleteJobs(final List<Long> poIdList, final String loggedInUser) {
        final List<List<Long>> batchedJobIds = ListUtils.partition(poIdList, jobParameterChangeListener.getJobBatchSize());
        if (loggedInUser != null) {
            /* Creating Context as the user name is not available in the Asynchronous thread. */
            contextService.setContextValue(ShmConstants.USER_ID_KEY, loggedInUser);
        }
        for (final List<Long> eachBatchOfJobIds : batchedJobIds) {
            shmJobServiceHelper.deleteJobs(eachBatchOfJobIds);

        }
    }
}
