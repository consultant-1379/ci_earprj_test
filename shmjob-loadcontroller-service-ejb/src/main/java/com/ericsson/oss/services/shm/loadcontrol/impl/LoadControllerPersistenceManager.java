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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Asynchronous;
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
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.SortDirection;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMStagedActivityRequest;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.loadcontrol.local.api.StagedActivityRequestBean;
import com.ericsson.oss.services.shm.loadcontrol.monitor.LoadControlQueueProducer;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LoadControllerPersistenceManager {

    private final Logger logger = LoggerFactory.getLogger(LoadControllerPersistenceManager.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private LoadControlQueueProducer loadControlQueueProducer;

    @Inject
    private ConfigurationParamProvider maxLoadCountProvider;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private LoadControlCounterManager counterManager;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private ConfigurationParamProvider configurationParamProvider;

    @Inject
    private SystemRecorder systemRecorder;

    private static final String LOAD_COUNTER_PREFIX = "LC_";

    public void keepRequestInDB(final SHMActivityRequest activityRequest) {
        try {
            logger.debug("Going to insert data into ShmStagedActivity PO for {}", activityRequest);
            final Map<String, Object> poAttributes = preparePOAttributes(activityRequest);
            createPO(poAttributes);
            logger.debug("Persisting of data in ShmStagedActivity PO is Completed for {}", activityRequest);
        } catch (final Exception e) {
            logger.error("Exception occured while preparing poAttributes for {}, Exception: {}", activityRequest, e);
        }
    }

    private Map<String, Object> preparePOAttributes(final SHMActivityRequest activityRequest) {
        final Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PLATFORM, activityRequest.getPlatformType());
        poAttributes.put(ShmConstants.JOB_TYPE, activityRequest.getJobType());
        poAttributes.put(ShmConstants.ACTIVITYNAME, activityRequest.getActivityName());
        poAttributes.put(ShmConstants.BUSINESS_KEY, activityRequest.getBusinessKey());
        poAttributes.put(ShmConstants.LC_COUNTER_KEY, activityRequest.getPlatformType() + activityRequest.getJobType() + activityRequest.getActivityName());
        poAttributes.put(ShmConstants.STAGED_ACTIVITY_WAIT_TIME, new Date());
        poAttributes.put(ShmConstants.WORKFLOW_INSTANCE_ID, activityRequest.getWorkflowInstanceId());
        poAttributes.put(ShmConstants.RETRY_COUNT, 0);
        poAttributes.put(ShmConstants.ACTIVITY_JOB_ID, activityRequest.getActivityJobId());
        return poAttributes;
    }

    public void createPO(final Map<String, Object> poAttributes) {
        try {
            final long stagedActivityPoId = createPOWithRetry(ShmConstants.NAMESPACE, ShmConstants.SHM_STAGED_ACTIVITY, ShmConstants.VERSION, poAttributes);
            logger.debug("PO created with the poId: {}", stagedActivityPoId);
        } catch (final RuntimeException exception) {
            logger.error("Exception occured while creating ShmStagedActivity PO for {}, Exception is: {}", poAttributes, exception);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(exception);
        }
    }

    private long createPOWithRetry(final String namespace, final String type, final String version, final Map<String, Object> stagedActivityAttributes) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new RetriableCommand<Long>() {
            @Override
            public Long execute(final RetryContext retryContext) {
                return createStagedActivityPO(namespace, type, version, stagedActivityAttributes);
            }

        });
    }

    private long createStagedActivityPO(final String namespace, final String type, final String version, final Map<String, Object> stagedActivityAttributes) {
        final PersistenceObject persistenceObject = dpsWriter.createPO(namespace, type, version, stagedActivityAttributes);
        final long poId = persistenceObject.getPoId();
        logger.debug("Created ShmStagedActivity PO, with PO Id is: {}", poId);
        return poId;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void readAndProcessStagedActivityPOs() {
        final long startTime = System.currentTimeMillis();
        try {
            logger.debug("Read And Processing of POs Started at {}", startTime);
            final List<PersistenceObject> allStagedActivities = retrieveAllStagedActivities();
            final Map<String, List<PersistenceObject>> groupedSatgedActivityPoMap = groupPersistencObjects(allStagedActivities);
            final Set<Entry<String, List<PersistenceObject>>> groupedSatgedActivityPoEntrySet = groupedSatgedActivityPoMap.entrySet();
            logger.debug("Grouped SatgedActivityPos EntrySet size {}", groupedSatgedActivityPoEntrySet.size());
            for (final Entry<String, List<PersistenceObject>> stagedActivityPoEntry : groupedSatgedActivityPoEntrySet) {
                processAndPlaceStagedActivitiesInQueue(stagedActivityPoEntry);
            }
        } catch (final Exception e) {
            logger.error("Exception occured in readAndProcessStagedActivityPOs {}", e);
        }
        final long endTime = System.nanoTime();
        logger.debug("Current Timer thread took {} seconds for processing staged Requests", ((endTime - startTime) * 1E-9));
    }

    private List<PersistenceObject> retrieveAllStagedActivities() {
        final long startTime = System.currentTimeMillis();
        final List<PersistenceObject> persistenceObjects = new ArrayList<>();
        final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.SHM_STAGED_ACTIVITY);
        query.addSortingOrder(ShmConstants.STAGED_ACTIVITY_WAIT_TIME, SortDirection.ASCENDING);
        final Iterator<PersistenceObject> pollingEntryPos = queryExecutor.execute(query);
        while (pollingEntryPos.hasNext()) {
            final PersistenceObject persistenceObject = pollingEntryPos.next();
            persistenceObjects.add(persistenceObject);
        }
        logger.debug("Retrieved {} StagedActivity PO's in {} millisecs.", persistenceObjects.size(), System.currentTimeMillis() - startTime);
        return persistenceObjects;
    }

    private Map<String, List<PersistenceObject>> groupPersistencObjects(final List<PersistenceObject> allPersistenceObjects) {
        final Map<String, List<PersistenceObject>> groupedPOs = new HashMap<>();
        for (final PersistenceObject persistenceObject : allPersistenceObjects) {
            try {
                final String counterKey = (String) persistenceObject.getAllAttributes().get(ShmConstants.LC_COUNTER_KEY);
                if (groupedPOs.containsKey(counterKey)) {
                    final List<PersistenceObject> persistenceObjects = groupedPOs.get(counterKey);
                    persistenceObjects.add(persistenceObject);
                    groupedPOs.put(counterKey, persistenceObjects);
                } else {
                    final List<PersistenceObject> persistenceObjects = new ArrayList<>();
                    persistenceObjects.add(persistenceObject);
                    groupedPOs.put(counterKey, persistenceObjects);
                }
            } catch (final Exception e) {
                logger.error("Exception occured while grouping of staged activities. Exception is:{}", e);
            }
        }
        return groupedPOs;
    }

    private void processAndPlaceStagedActivitiesInQueue(final Entry<String, List<PersistenceObject>> stagedActivityPoEntry) {
        try {
            final List<PersistenceObject> stagedActivityPoList = stagedActivityPoEntry.getValue();
            final String counterName = stagedActivityPoEntry.getKey();
            logger.debug("Processing of stagedActivityPoList size {} for counterName {}", stagedActivityPoList.size(), counterName);
            final Long maxCounterValue = maxLoadCountProvider.getMaximumCountByCounterKey(counterName);

            final Long currentRunningGlobalCount = counterManager.getGlobalCounter(counterName).get();
            final Long currentRunningLocalCount = counterManager.getCounter(getLocalCounter(counterName)).get();

            logger.info("In Timer: Current GLC value: {}, Current LC Value: {} and Max LC Value: {} for counterName {}", currentRunningGlobalCount, currentRunningLocalCount, maxCounterValue,
                    counterName);

            final Map<String, Object> eventData = new HashMap<>();
            eventData.put("CounterName", counterName);
            eventData.put("CurrentGlobalLoadControlValue", currentRunningGlobalCount);
            eventData.put("CurrentLocalLoadControlValue", currentRunningLocalCount);
            eventData.put("MaximumCounterValue", maxCounterValue);
            systemRecorder.recordEventData(SHMEvents.LOAD_CONTROL_STATE, eventData);
            int allowedCount = 0;
            if (currentRunningGlobalCount > currentRunningLocalCount) {
                allowedCount = (int) (maxCounterValue * membershipListenerInterface.getCurrentMembersCount() - currentRunningGlobalCount);
            } else {
                allowedCount = (int) (maxCounterValue * membershipListenerInterface.getCurrentMembersCount() - currentRunningLocalCount);
            }
            logger.debug("In Timer: allowedCount {} for counterName {}", allowedCount, counterName);
            if (allowedCount > 0) {
                final List<PersistenceObject> satgedActivityPoSubList = (stagedActivityPoList.size() > allowedCount) ? stagedActivityPoList.subList(0, allowedCount) : stagedActivityPoList;
                final List<List<PersistenceObject>> stagedAtivityPoBatchList = ListUtils.partition(satgedActivityPoSubList, configurationParamProvider.getstagedactivitiesBatchSize());
                logger.info("In Timer: placing {} stagedAtivityPos in Queue and counterName {}", stagedAtivityPoBatchList.size(), counterName);
                keepStagedActivitiesInQueue(stagedAtivityPoBatchList);
            }
        } catch (final Exception e) {
            logger.error("Exception occured in processAndPlaceStagedActivitiesInQueue, Exception is:{}", e);
        }
    }

    /**
     * @param stagedActivityPoBatchList
     */
    private void keepStagedActivitiesInQueue(final List<List<PersistenceObject>> stagedActivityPoBatchList) {
        for (final List<PersistenceObject> stagedActivityPersistenceObjects : stagedActivityPoBatchList) {
            try {
                logger.debug("Batch PersistenceObjects size {}", stagedActivityPersistenceObjects.size());
                final List<SHMStagedActivityRequest> stagedActivitesList = convertStagedActivityPoToObjectList(stagedActivityPersistenceObjects);
                if (!stagedActivitesList.isEmpty()) {
                    final StagedActivityRequestBean stagedActivityRequest = new StagedActivityRequestBean();
                    stagedActivityRequest.setShmStagedActivityRequest(stagedActivitesList);
                    loadControlQueueProducer.keepStagedActivitiesInQueue(stagedActivityRequest);
                    logger.debug("Placed {} stagedActivityRequest In Queue", stagedActivitesList.size());
                }
            } catch (final Exception e) {
                logger.error("Exception occured while convert StagedActivity PO To StagedActivityRequestList / keeping StagedActivities In Queue", e);
            }
        }
    }

    private List<SHMStagedActivityRequest> convertStagedActivityPoToObjectList(final List<PersistenceObject> stagedAtivityPersistenceObjects) {
        final List<SHMStagedActivityRequest> shmStagedActivityRequestList = new ArrayList<>();
        for (final PersistenceObject persistenceObject : stagedAtivityPersistenceObjects) {
            try {
                final SHMStagedActivityRequest shmStagedActivityRequest = new SHMStagedActivityRequest(persistenceObject.getAllAttributes());
                shmStagedActivityRequest.setStagedActivityPoId(persistenceObject.getPoId());
                shmStagedActivityRequestList.add(shmStagedActivityRequest);
                dataPersistenceService.getLiveBucket().deletePo(persistenceObject);
                logger.debug("{} deleted successfully, PO details {}", persistenceObject.getPoId(), shmStagedActivityRequest.getBusinessKey());
            } catch (final Exception e) {
                logger.error("Exception occured while preparing StagedActivitiesList", e);
            }
        }
        return shmStagedActivityRequestList;
    }

    private String getLocalCounter(final String counterName) {
        return LOAD_COUNTER_PREFIX + counterName;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteStagedActivityPOs(final long activityJobId) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.SHM_STAGED_ACTIVITY);

        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction restriction = restrictionBuilder.equalTo(ShmConstants.ACTIVITY_JOB_ID, activityJobId);
        query.setRestriction(restriction);

        final Iterator<PersistenceObject> shmStagedActivityPOs = queryExecutor.execute(query);
        while (shmStagedActivityPOs.hasNext()) {
            final int deleted = liveBucket.deletePo(shmStagedActivityPOs.next());
            logger.trace("deleted POs count {}",deleted);
        }
    }

}