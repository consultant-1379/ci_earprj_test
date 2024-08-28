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
package com.ericsson.oss.services.shm.es.impl;

import java.io.Serializable;

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.timer.AbstractTimerService;
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;

/**
 * Timer service to invoke the Main job progress updater.
 * 
 * @author xrajeke
 * 
 */
@Stateless
public class MainJobProgressUpdateInitiationTimer extends AbstractTimerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainJobProgressUpdateInitiationTimer.class);

    @Inject
    private MainJobsProgressUpdateService mainJobsProgressUpdateService;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    public void startProgressUpdateTimer(final Serializable timerInfo, final int timerTimeoutInSec) {
        startTimerWithSec(timerInfo, timerTimeoutInSec);
        LOGGER.info("Started a timer named: {} with timeout value of {} seconds.", timerInfo, timerTimeoutInSec);
    }

    public void reStartProgressUpdateTimer(final Serializable timerInfo, final int timerTimeoutInSec) {
        reStartTimerWithSec(timerInfo, timerTimeoutInSec);
        LOGGER.info("ReStarted a timer named: {} with new timeout value of {} seconds.", timerInfo, timerTimeoutInSec);
    }

    @Timeout
    public void invokeMainJobsProgressUpdateService(final Timer timer) {
        if (membershipListenerInterface.isMaster()) {
            LOGGER.trace("Timer [{}] is invoking SHM Main jobs progress Update.", timer);
            mainJobsProgressUpdateService.invokeMainJobsProgressUpdate();
        } else {
            LOGGER.debug("Skipping Main Job progress update, since this service instance is not a Master at the movement.");
        }
    }
}
