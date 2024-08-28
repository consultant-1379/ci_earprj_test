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
package com.ericsson.oss.services.shm.cpp.inventory.service.registration;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.annotation.ServiceCluster;
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.cluster.events.ClusterMembershipChangeEventGenerator;
import com.ericsson.oss.services.shm.loadcontrol.local.api.PrepareLoadControllerLocalCounterService;

@ApplicationScoped
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CppInventorySynchEventListenerRegistration implements MembershipListenerInterface {

    @Inject
    private ClusterMembershipChangeEventGenerator shmMembershipChangeEventGenerator;

    private volatile boolean isMaster = false;

    private volatile int currentMembersCount = 0;

    @Inject
    private PrepareLoadControllerLocalCounterService prepareLoadControllerLocalCounterService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CppInventorySynchEventListenerRegistration.class.getName());

    public void listenForMembershipChange(@Observes @ServiceCluster("CppInventorySynchEventListenerCluster") final MembershipChangeEvent changeEvent) {
        currentMembersCount = changeEvent.getCurrentNumberOfMembers();
        prepareLoadControllerLocalCounterService.prepareMaxCountMap(currentMembersCount);
        if (changeEvent.isMaster()) {
            LOGGER.info("Received membership change event [{}], setting current CppInventorySynchEventListener instance to master", changeEvent.isMaster());
            shmMembershipChangeEventGenerator.generateMemberShipChangeEvent();
        } else {
            LOGGER.info("Received membership change event [{}], setting current CppInventorySynchEventListener instance to redundant", changeEvent.isMaster());
        }
        setIsMaster(changeEvent.isMaster());
    }

    /**
     * @return boolean state of current CppInventorySynchEventListener Service instance
     */
    @Deprecated
    public boolean getMasterState() {
        return isMaster;
    }

    private void setIsMaster(final boolean isMaster) {
        this.isMaster = isMaster;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.cluster.MembershipListenerInterface#isMaster()
     */
    @Override
    public boolean isMaster() {
        return isMaster;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.cluster.MembershipListenerInterface#getCurrentMembersCount()
     */
    @Override
    public int getCurrentMembersCount() {
        return currentMembersCount;
    }

}
