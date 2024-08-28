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
package com.ericsson.oss.services.shm.job.housekeeping;

import javax.ejb.*;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.ClusterMembershipChangeEvent;


/**
 * This class is used to fetch the cluster count of a VM and checks the counter value, if it is 1 trigger housekeeping.
 * 
 * @author xprapav
 * 
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ClusterMembershipChangeEventListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private JobsHouseKeepingService jobsHouseKeepingService;

    @Inject
    JobsHouseKeepingClusterCounterManager jobsHouseKeepingClusterCounterManager;

    @Asynchronous
    public void listenForClusterMembershipChangeEvent(@Observes final ClusterMembershipChangeEvent clusterMembershipChangeEvent) {
        logger.debug("Fetching changeEvent from {} ", clusterMembershipChangeEvent.getMessage());
        final long counter = jobsHouseKeepingClusterCounterManager.getClusterCounter();
        logger.debug("Cluster Counter in ClusterMembershipChangeEventListener {} ", counter);
        if (counter == 1) {
            // here we are setting counter value to 0 by passing boolean.
            jobsHouseKeepingClusterCounterManager.setClusterCounter(false);
            jobsHouseKeepingService.triggerHouseKeepingOfJobs();
        }
    }
}