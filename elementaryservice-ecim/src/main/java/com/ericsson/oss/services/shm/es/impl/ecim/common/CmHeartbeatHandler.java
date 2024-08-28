/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.core.events.OperationType;
import com.ericsson.oss.mediation.sdk.event.SupervisionMediationTaskRequest;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

/**
 * Provides a API to manage heartbeatInterval time on CmNodeHeartbeatSupervision MO.
 * 
 * @author tcsgusw
 * 
 */
@Stateless
public class CmHeartbeatHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmHeartbeatHandler.class);

    @Inject
    @Modeled
    private EventSender<SupervisionMediationTaskRequest> eventSender;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    /**
     * Updates the heartbeatInterval time on CmNodeHeartbeatSupervision MO using MediationTaskRequest .
     * 
     * @param heartBeatInterval
     * @param activityJobId
     * @param nodeFdn
     */

    public void sendHeartbeatIntervalChangeRequest(final int heartbeatInterval, final long activityJobId, final String nodeFdn) {

        LOGGER.debug("Updating heartbeatInterval={} on CmNodeHeartbeatSupervision MO for the node: {} and activityJobId: {}", heartbeatInterval, nodeFdn, activityJobId);
        final String CmNodeSupervisionMOFDN = getCmNodeHeartbeatSupervisionMO(nodeFdn);
        if (CmNodeSupervisionMOFDN != null) {
            final SupervisionMediationTaskRequest supervisionMTR = buildMediationTaskRequest(heartbeatInterval, activityJobId, CmNodeSupervisionMOFDN);
            try {
                eventSender.send(supervisionMTR);

            } catch (final Exception ex) {
                LOGGER.warn("Exception occurred while sending MediationTaskRequest request for the node: {} and with uniqueId(activityJobId): {}. Exception is: {}", nodeFdn, activityJobId, ex);
            }
        } else {
            LOGGER.warn("CmNodeHeartbeatSupervision MO fdn returned as null for the node : {}", nodeFdn);
        }
    }

    /**
     * 
     * @param heartbeatInterval
     * @param activityJobId
     * @param supervisionMoFdn
     * @return SupervisionMediationTaskRequest
     */
    private SupervisionMediationTaskRequest buildMediationTaskRequest(final int heartbeatInterval, final long activityJobId, final String supervisionMoFdn) {
        final SupervisionMediationTaskRequest supervisionMTR = new SupervisionMediationTaskRequest();
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(ShmCommonConstants.CM_NODE_SUPERVISION_MO_HEARBEAT_INTERVAL, heartbeatInterval);
        supervisionMTR.setNodeAddress(supervisionMoFdn);
        supervisionMTR.setClientType(MediationClientType.SUPERVISION.name());
        supervisionMTR.setJobId(String.valueOf(activityJobId));
        supervisionMTR.setProtocolInfo(OperationType.CM.name());
        supervisionMTR.setSupervisionAttributes(attributes);
        return supervisionMTR;
    }

    /**
     * Retrieves the CmNodeHearbeatSupervision MO FDN by taking node FDN as input.
     * 
     * @param nodeFdn
     * @return
     */
    private String getCmNodeHeartbeatSupervisionMO(final String nodeFdn) {
        String supervisionMOFdn = null;
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final Query<TypeContainmentRestrictionBuilder> query = queryBuilder.createTypeQuery(ShmCommonConstants.OSS_NE_CM_DEF_NAMESPACE, ShmCommonConstants.CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE,
                    nodeFdn);
            final DataBucket dataBucket = dataPersistenceService.getLiveBucket();

            final Projection fdnProjection = ProjectionBuilder.field(ObjectField.MO_FDN);
            final List<String> filteredFdns = dataBucket.getQueryExecutor().executeProjection(query, fdnProjection);
            LOGGER.debug("CmNodeHearbeatSupervision FDN is {} for the node: {}", filteredFdns, nodeFdn);

            if (filteredFdns != null && !filteredFdns.isEmpty()) {
                supervisionMOFdn = filteredFdns.get(0);
            }
        } catch (final Exception ex) {
            LOGGER.warn("Failed to get CmNodeHeartbeatSupervision MO FDN for the node: {}. Exception is: ", nodeFdn, ex);
        }
        return supervisionMOFdn;
    }
}
