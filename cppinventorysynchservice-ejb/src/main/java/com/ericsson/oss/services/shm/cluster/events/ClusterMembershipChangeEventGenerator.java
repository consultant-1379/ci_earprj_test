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
package com.ericsson.oss.services.shm.cluster.events;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.ClusterMembershipChangeEvent;

/**
 * 
 * This class generates a SHM specific event when a MemberShipChangeEvent is received
 * 
 * @author xprapav
 * 
 * 
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ClusterMembershipChangeEventGenerator {
    private static final String CLUSTER_MEMBERSHIP_CHANGE_EVENT = "ClusterMembershipChangeEvent";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    Event<ClusterMembershipChangeEvent> clusterMembershipChangeEvent;

    @Asynchronous
    public void generateMemberShipChangeEvent() {
        logger.info("Generating ClusterMembershipChangeEvent");
        clusterMembershipChangeEvent.fire(new ClusterMembershipChangeEvent(CLUSTER_MEMBERSHIP_CHANGE_EVENT));
    }
}