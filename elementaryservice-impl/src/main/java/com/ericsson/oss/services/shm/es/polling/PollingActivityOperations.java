/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;


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
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.es.polling.api.PollingType;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;


/**
 * This class is used to perform subscription, un-subscription and retrieval of PollingActivity PO entries from DB.
 * 
 * @author xsrabop
 * 
 */
@Stateless
public class PollingActivityOperations {

    final static private Logger LOGGER = LoggerFactory.getLogger(PollingActivityOperations.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    /**
     * Creates PO with SHM name-space for PollingActivity with the passed attributes
     * 
     * @param pollingEntry
     * @return PersistenceObject
     */
    public long createPO(final Map<String, Object> pollingEntry) {
        LOGGER.debug("Creating polling entry with : {}", pollingEntry);
        final PersistenceObject persistenceObject = getLiveBucket().getPersistenceObjectBuilder().namespace(ShmConstants.NAMESPACE).type(ShmConstants.POLLING_ACTIVITY).version(ShmConstants.VERSION)
                .addAttributes(pollingEntry).create();
        LOGGER.debug("Created Polling POId is: {}", persistenceObject.getPoId());
        return persistenceObject.getPoId();
    }

    /**
     * This method deletes PO with the given poId.
     * 
     * @param poId
     */
    public void deletePOByPOId(final long poId) {
        LOGGER.debug("Deleting PO with poId: {}", poId);
        final DataBucket dataBucket = getLiveBucket();
        final PersistenceObject persistenceObject = dataBucket.findPoById(poId);
        dataBucket.deletePo(persistenceObject);
        LOGGER.info("Polling PO with poId : {} deleted successfuly", poId);
    }

    /**
     * Fetches the poId of the polling entry for the given activityJobId and deletes the PO.
     * 
     * @param activityJobId
     */
    public void deletePOByActivityJobId(final long activityJobId) {
        LOGGER.debug("Deleting polling activity PO for activityJobId: {}", activityJobId);
        final DataBucket dataBucket = getLiveBucket();
        final Query<TypeRestrictionBuilder> pollingActivityQuery = buildActivityJobIdRestriction(activityJobId);
        final Iterator<PersistenceObject> pollingEntryPos = dataBucket.getQueryExecutor().execute(pollingActivityQuery);
        while (pollingEntryPos.hasNext()) {
            final PersistenceObject persistenceObject = pollingEntryPos.next();
            final long pollInitiatedTime = persistenceObject.getAttribute(PollingActivityConstants.POLL_INITIATED_TIME);
            dataBucket.deletePo(persistenceObject);
            LOGGER.info("Polling entry for activityJobId : {} deleted successfully. PollInitiatedTime : {} and Activity completion time : {} ", activityJobId, new Date(pollInitiatedTime), new Date());
        }
    }

    /**
     * Fetches the PollingActivity poId for the given activityJobId;
     * 
     * @param poId
     * @param attributes
     */
    public long getPoIdByActivityJobId(final long activityJobId) {
        LOGGER.debug("Fetching polling entry poId for activityJobId: {}", activityJobId);
        final Query<TypeRestrictionBuilder> pollingActivityQuery = buildActivityJobIdRestriction(activityJobId);
        final List<Long> pollingActivityPOIds = getReadOnlyLiveBucket().getQueryExecutor().executeProjection(pollingActivityQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        if (pollingActivityPOIds != null && !pollingActivityPOIds.isEmpty()) {
            return pollingActivityPOIds.get(0);
        }
        return 0;
    }

    /**
     * Updates the PollingActivity PO with the passed attributes.
     * 
     * @param poId
     * @param attributes
     */
    public void updatePollingAttributes(final long poId, final Map<String, Object> attributes) {
        LOGGER.debug("Inside PollingActivityManager:updatePollingAttributes() for updating to PO Id {} with attributes {}", poId, attributes);
        if (attributes != null && !attributes.isEmpty()) {
            final PersistenceObject persistenceObject = getLiveBucket().findPoById(poId);
            if (persistenceObject != null) {
                persistenceObject.setAttributes(attributes);
            } else {
                LOGGER.error("PO not found with Id:{}, and skipping the attributes Update", poId);
            }
        } else {
            LOGGER.debug("Discarding the PO : {} update as the attributes :{} not valid", poId, attributes);
        }
    }

    /**
     * Updates the PollingActivity PO for the given activityJobId with the passed attributes.
     * 
     * @param activityJobId
     * @param attributes
     */
    public void updatePollingAttributesByActivityJobId(final long activityJobId, final Map<String, Object> attributes) {
        LOGGER.debug("Inside PollingActivityManager:updatePollingAttributesByActivityJobId() for updating to activityJob Id {} with attributes {}", activityJobId, attributes);
        if (attributes != null && !attributes.isEmpty()) {
            final DataBucket dataBucket = getLiveBucket();
            final Query<TypeRestrictionBuilder> pollingActivityQuery = buildActivityJobIdRestriction(activityJobId);
            final Iterator<PersistenceObject> pollingActivityPOs = dataBucket.getQueryExecutor().execute(pollingActivityQuery);
            while (pollingActivityPOs.hasNext()) {
                final PersistenceObject persistenceObject = pollingActivityPOs.next();
                persistenceObject.setAttributes(attributes);
            }
        } else {
            LOGGER.debug("Discarding the PO : {} update as the attributes :{} not valid", activityJobId, attributes);
        }
    }

    /**
     * This method gets required Attributes from the PollingActivity PO
     * 
     * @param pollingActivityPoId
     * @param requiredAttributes
     */
    public Map<String, Object> getPollingActivityAttributes(final long pollingActivityPoId, final List<String> requiredAttributes) {
        final PersistenceObject persistenceObject = getLiveBucket().findPoById(pollingActivityPoId);
        if (persistenceObject != null) {
            return persistenceObject.getAttributes(requiredAttributes);
        }
        return new HashMap<>();
    }

    /**
     * Gets all the Polling Entries from DPS
     * 
     */
   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Map<String,Object>> getPollingActivityPOs(){
        final List<Map<String,Object>> persistenceObjectList = new ArrayList<>();
        final Date queryStartTime = new Date();
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.POLLING_ACTIVITY);
        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction pollingTypeNodeRestriction = restrictionBuilder.equalTo(PollingActivityConstants.POLLING_TYPE, PollingType.NODE.name());
        final Restriction pollingTypeNullRestriction = restrictionBuilder.nullValue(PollingActivityConstants.POLLING_TYPE);
        final Restriction finalRestriction = restrictionBuilder.anyOf(pollingTypeNodeRestriction, pollingTypeNullRestriction);
        query.setRestriction(finalRestriction);
        final Iterator<PersistenceObject> pollingEntryPos = queryExecutor.execute(query);
        while (pollingEntryPos.hasNext()) {
            final PersistenceObject persistenceObject = pollingEntryPos.next();
              final Map<String, Object> attributes = persistenceObject.getAllAttributes();
              attributes.put(ShmConstants.PO_ID, persistenceObject.getPoId());
              persistenceObjectList.add(attributes);
        }
        LOGGER.debug("Time taken to retrieve {} pollentry details is : {} milliseconds", persistenceObjectList.size(), (new Date().getTime() - queryStartTime.getTime()));
        return persistenceObjectList;
    }

    private DataBucket getLiveBucket() {
        return dataPersistenceService.getLiveBucket();
    }

    private DataBucket getReadOnlyLiveBucket() {
        dataPersistenceService.setWriteAccess(Boolean.FALSE);
        return dataPersistenceService.getLiveBucket();
    }

    private Query<TypeRestrictionBuilder> buildActivityJobIdRestriction(final long activityJobId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> pollingActivityQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, ShmConstants.POLLING_ACTIVITY);
        final TypeRestrictionBuilder restrictionBuilder = pollingActivityQuery.getRestrictionBuilder();
        final Restriction activityJobIdRestriction = restrictionBuilder.equalTo(PollingActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        pollingActivityQuery.setRestriction(activityJobIdRestriction);
        return pollingActivityQuery;
    }
}
