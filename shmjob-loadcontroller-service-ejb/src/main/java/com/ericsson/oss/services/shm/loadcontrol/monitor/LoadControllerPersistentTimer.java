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
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.common.timer.AbstractTimerService;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * This class creates ejb timer and will read all stagedactivites from for every one min.
 * 
 * @author tcspavp
 */
@Singleton
@Startup
public class LoadControllerPersistentTimer extends AbstractTimerService {

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    @Inject
    private SystemRecorder systemRecorder;

    private final Logger logger = LoggerFactory.getLogger(LoadControllerPersistentTimer.class);

    @PostConstruct
    public void startTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        startTimerWithMin("LC_PersistDataTimer", 1);
        logger.info("Successfully started a LoadController PersistDataTimer");
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @Timeout
    public void readDBEntries(final Timer timer) {
        if (membershipListenerInterface.isMaster()) {
            logger.trace("Triggering reading of data(ShmStagedActivity) from Dps after timeout of {} occured ", timer.getInfo());
            loadControllerPersistenceManager.readAndProcessStagedActivityPOs();
        }
    }

}
