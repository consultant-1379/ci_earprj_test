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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.job.service.BatchParameterChangeListener;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

@Stateless
public class AxeSynchronousActivityProxyService {

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private BatchParameterChangeListener batchParameterChangeListener;

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeSynchronousActivityProxyService.class);

    public Map<Long, Map<String, String>> getNeJobForMainJobByNeType(final Long mainJobId, final String neType) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE);
        final Restriction restriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.MAIN_JOB_ID, mainJobId);
        final Restriction neTypeRestriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.NETYPE, neType);
        typeQuery.setRestriction(typeQuery.getRestrictionBuilder().allOf(restriction, neTypeRestriction));
        return queryForJobIdAndNames(typeQuery, queryExecutor, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute(ShmConstants.NE_NAME),
                ProjectionBuilder.attribute(ShmConstants.RESULT));
    }

    public Map<Long, Map<String, String>> queryForJobIdAndNames(final Query<TypeRestrictionBuilder> typeQuery, final QueryExecutor queryExecutor, final Projection idProjection,
            final Projection nameProjection, final Projection resultProjection) {
        List<Object[]> jobProjector = new ArrayList<>();
        try {
            jobProjector = queryExecutor.executeProjection(typeQuery, idProjection, nameProjection, resultProjection);
        } catch (final RuntimeException runtimeException) {
            LOGGER.error("Exception occurred while querying NE Jobs. Reason: ", runtimeException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(runtimeException);
        }
        final Map<Long, Map<String, String>> jobDetails = new HashMap<>();
        for (final Object[] eachNeJobProjector : jobProjector) {
            final Map<String, String> nameAndStatus = new HashMap<>();
            nameAndStatus.put((String) eachNeJobProjector[1], (String) eachNeJobProjector[2]);
            jobDetails.put((Long) eachNeJobProjector[0], nameAndStatus);
        }
        return jobDetails;
    }

    public Map<Long, String> getCompletedActivityDetails(final List<Long> neJobPoIds, final int orderId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final List<List<Long>> batchedNeJobIds = ListUtils.partition(neJobPoIds, batchParameterChangeListener.getJobDetailsQueryBatchSize());
        final Map<Long, String> activityDetails = new HashMap<>();
        for (final List<Long> eachBatchOfNeJobIds : batchedNeJobIds) {
            final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB);
            final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
            final Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.ACTIVITY_NE_JOB_ID, eachBatchOfNeJobIds.toArray());
            final Restriction orderRestriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.ORDER, orderId);
            final Restriction stateRestriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.ACTIVITY_NE_STATUS, JobState.COMPLETED.toString());
            final Restriction allOfRestriction = typeQuery.getRestrictionBuilder().allOf(restriction, orderRestriction, stateRestriction);
            typeQuery.setRestriction(allOfRestriction);
            final List<Object[]> result = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.attribute(ShmConstants.ACTIVITY_NE_JOB_ID), ProjectionBuilder.attribute(ShmConstants.RESULT));
            for (final Object[] eachProjector : result) {
                activityDetails.put((Long) eachProjector[0], (String) eachProjector[1]);
            }
        }
        return activityDetails;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public long getNeJobId(final String businessKey) {
        long neJobId = -1l;
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> neJobTypeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);
        final com.ericsson.oss.itpf.datalayer.dps.query.Restriction bussinessKeyrestriction = neJobTypeQuery.getRestrictionBuilder().equalTo(ShmConstants.BUSINESS_KEY, businessKey);
        neJobTypeQuery.setRestriction(bussinessKeyrestriction);

        List<Object[]> jobProjector = new ArrayList<>();
        try {
            jobProjector = queryExecutor.executeProjection(neJobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute(ShmConstants.NE_NAME));
        } catch (final RuntimeException runtimeException) {
            LOGGER.error("Exception occurred while querying NE Jobs. Reason: ", runtimeException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(runtimeException);
        }
        final Map<Long, String> neJobAttributesMap = new HashMap<>();
        for (final Object[] eachNeJobProjector : jobProjector) {
            neJobAttributesMap.put((Long) eachNeJobProjector[0], (String) eachNeJobProjector[1]);
        }

        for (final Entry<Long, String> neJobAttributes : neJobAttributesMap.entrySet()) {
            neJobId = neJobAttributes.getKey();
            final String neName = neJobAttributes.getValue();
            LOGGER.warn("NEJob on Node:{} is going to be cancelled explisitly.", neName);
        }
        return neJobId;
    }

}
