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
package com.ericsson.oss.services.shm.axe.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobservice.axe.OpsSessionAndClusterIdInfo;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
public class ShmAxeServiceImplHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmAxeServiceImplHelper.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    private DataBucket getLiveBucket() {
        try {
            return dataPersistenceService.getLiveBucket();
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving live bucket. Reason : {}", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw new ServerInternalException("Exception while reading the inventory. Please try again.");
        }
    }

    public Map<Long, OpsSessionAndClusterIdInfo> getSessionIdAndClusterId(final List<Long> neJobIdList) {
        LOGGER.info("batch of NeJobIds{}: ", neJobIdList);
        return getSessionIdAndClusterIdInfo(getActivityJobPO(neJobIdList));
    }

    /**
     * method to populate session id and cluster id from list of persistence objects
     * 
     * @param listOfPersistenceObjects
     * @return
     */
    private Map<Long, OpsSessionAndClusterIdInfo> getSessionIdAndClusterIdInfo(final List<PersistenceObject> listOfPersistenceObjects) {
        final Map<Long, OpsSessionAndClusterIdInfo> mapOfNeJobIdToOpsSessionAndClusterIdInfo = new HashMap<>();
        if (listOfPersistenceObjects == null || listOfPersistenceObjects.isEmpty()) {
            return mapOfNeJobIdToOpsSessionAndClusterIdInfo;
        }
        for (final PersistenceObject persistenceObject : listOfPersistenceObjects) {
            final Map<String, Object> attributes = persistenceObject.getAttributes(Arrays.asList(ShmConstants.ACTIVITY_NE_JOB_ID, ShmConstants.JOBPROPERTIES));
            final long neJobId = (Long) attributes.get(ShmConstants.ACTIVITY_NE_JOB_ID);
            mapOfNeJobIdToOpsSessionAndClusterIdInfo.put(neJobId,
                    getRequiredAttributesForOpsGuiLaunch((List<Map<String, Object>>) attributes.get(ShmConstants.JOBPROPERTIES), persistenceObject.getPoId()));
        }
        LOGGER.debug("map Of NeJobIds To OpsSessionAndClusterIdInfo: {}", mapOfNeJobIdToOpsSessionAndClusterIdInfo);
        return mapOfNeJobIdToOpsSessionAndClusterIdInfo;
    }

    /**
     * method to populate required attibutes from activity job properties to launch OPS GUI
     * 
     * @param jobProperties
     * @param activitiyJobId
     * @return
     */
    private OpsSessionAndClusterIdInfo getRequiredAttributesForOpsGuiLaunch(final List<Map<String, Object>> jobProperties, final long activityJobId) {
        LOGGER.debug("jobProperties: {}", jobProperties);

        final OpsSessionAndClusterIdInfo opsSessionAndClusterIdInfo = new OpsSessionAndClusterIdInfo();
        if (null != jobProperties && !jobProperties.isEmpty()) {
            for (final Map<String, Object> jobProperty : jobProperties) {
                if (ActivityConstants.OPS_CLUSTER_ID.equals(jobProperty.get(ShmConstants.KEY))) {
                    opsSessionAndClusterIdInfo.setClusterID((String) jobProperty.get(ShmConstants.VALUE));
                } else if (ActivityConstants.OPS_SESSION_ID.equals(jobProperty.get(ShmConstants.KEY))) {
                    opsSessionAndClusterIdInfo.setSessionID((String) jobProperty.get(ShmConstants.VALUE));
                }
            }
        }
        opsSessionAndClusterIdInfo.setActivityJobId(String.valueOf(activityJobId));
        return opsSessionAndClusterIdInfo;
    }

    private List<PersistenceObject> getActivityJobPO(final List<Long> neJobIdList) {
        List<PersistenceObject> listOfPersistenceObjects = null;
        final DataBucket dataBucket = getLiveBucket();
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
        try {
            final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE);
            final Restriction activityJobsRestriction = query.getRestrictionBuilder().in(ShmConstants.NE_JOB_ID, neJobIdList.toArray());
            final Restriction activityJobRunningStateRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
            query.setRestriction(query.getRestrictionBuilder().allOf(activityJobsRestriction, activityJobRunningStateRestriction));
            listOfPersistenceObjects = queryExecutor.getResultList(query);
        } catch (final EJBException ejbException) {
            LOGGER.error("Exception occurred while querying Activity Jobs for ops gui sessionId and ClusterID. Reason: ", ejbException);
            throw ejbException;
        }
        return listOfPersistenceObjects;
    }

}