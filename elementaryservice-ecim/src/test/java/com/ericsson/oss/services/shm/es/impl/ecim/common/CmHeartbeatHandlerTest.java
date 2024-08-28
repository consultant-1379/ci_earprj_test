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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.core.events.OperationType;
import com.ericsson.oss.mediation.sdk.event.SupervisionMediationTaskRequest;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

@RunWith(MockitoJUnitRunner.class)
public class CmHeartbeatHandlerTest {

    @InjectMocks
    private CmHeartbeatHandler cmHeartbeatHandler;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Query<TypeContainmentRestrictionBuilder> query;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private EventSender<SupervisionMediationTaskRequest> eventSender;

    final int heartbeatInterval = 180;
    final long activityJobId = 1234l;

    @Test
    public void testSendHeartbeatIntervalChangeRequestWithValidNodeAddress_success() {

        final String nodeFdn = "NetworkElement=nodeName";
        final String cmSupervisionMo = "NetworkElement=nodeName, CmNodeHeartbeatSupervision=1";

        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmCommonConstants.OSS_NE_CM_DEF_NAMESPACE, ShmCommonConstants.CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE, nodeFdn)).thenReturn(query);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        List<Object> list = new ArrayList<Object>();
        list.add(cmSupervisionMo);
        when(queryExecutor.executeProjection(eq(query), any(Projection.class))).thenReturn(list);
        final SupervisionMediationTaskRequest mtr = buildMockMTR(cmSupervisionMo);
        cmHeartbeatHandler.sendHeartbeatIntervalChangeRequest(heartbeatInterval, activityJobId, nodeFdn);
        verify(eventSender, times(1)).send(mtr);
    }

    @Test
    public void testSendHeartbeatIntervalChangeRequestWithInValidNodeAddress_Failed() {
        final String nodeFdn = "NetworkElement=nodeName";
        final String cmSupervisionMo = "NetworkElement=nodeName, CmNodeHeartbeatSupervision=1";

        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmCommonConstants.OSS_NE_CM_DEF_NAMESPACE, ShmCommonConstants.CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE, nodeFdn)).thenReturn(query);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        List<Object> list = new ArrayList<Object>();
        list.add(null);
        when(queryExecutor.executeProjection(eq(query), any(Projection.class))).thenReturn(list);
        final SupervisionMediationTaskRequest mtr = buildMockMTR(cmSupervisionMo);
        cmHeartbeatHandler.sendHeartbeatIntervalChangeRequest(heartbeatInterval, activityJobId, nodeFdn);
        verify(eventSender, never()).send(mtr);
    }

    @Test
    public void testSendHeartbeatIntervalChangeRequest_Exception() {
        final String nodeFdn = "NetworkElement=nodeName";
        final String cmSupervisionMo = "NetworkElement=nodeName, CmNodeHeartbeatSupervision=1";

        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmCommonConstants.OSS_NE_CM_DEF_NAMESPACE, ShmCommonConstants.CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE, nodeFdn)).thenThrow(Exception.class);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        List<Object> list = new ArrayList<Object>();
        list.add(null);
        when(queryExecutor.executeProjection(eq(query), any(Projection.class))).thenReturn(list);
        final SupervisionMediationTaskRequest mtr = buildMockMTR(cmSupervisionMo);
        cmHeartbeatHandler.sendHeartbeatIntervalChangeRequest(heartbeatInterval, activityJobId, nodeFdn);
        verify(eventSender, never()).send(mtr);
    }

    private SupervisionMediationTaskRequest buildMockMTR(final String cmSupervisionMo) {
        final SupervisionMediationTaskRequest supervisionMTR = new SupervisionMediationTaskRequest();
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("heartbeatInterval", heartbeatInterval);
        supervisionMTR.setNodeAddress(cmSupervisionMo);
        supervisionMTR.setClientType(MediationClientType.SUPERVISION.name());
        supervisionMTR.setJobId(String.valueOf(activityJobId));
        supervisionMTR.setProtocolInfo(OperationType.CM.name());
        supervisionMTR.setSupervisionAttributes(attributes);
        return supervisionMTR;
    }
}
