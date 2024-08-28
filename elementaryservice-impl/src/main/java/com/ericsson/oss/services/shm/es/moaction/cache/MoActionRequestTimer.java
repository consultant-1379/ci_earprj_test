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
package com.ericsson.oss.services.shm.es.moaction.cache;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.es.moaction.MoActionRequestService;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;

/**
 * This is a timer class, it will manage the Mo Action response . This will iterate over the cluster cache entries whose time is Max wait time and issue a MO read request about the action triggered
 * status
 * 
 * @author tcssdas
 * 
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MoActionRequestTimer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoActionRequestTimer.class);

    @Resource
    private TimerService timerService;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Inject
    private MoActionRequestService moActionRequestService;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private SystemRecorder systemRecorder;

    private static final String MO_TIMER_INFO = "MoActionCacheTimer";

    /**
     * This method initializes the timer at application startup.
     */

    @PostConstruct
    public void initTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        final int initialDelay = pollingActivityConfiguration.getInitialDelayToStartTimerAfterServiceStartUp();
        final int intervalDelay = pollingActivityConfiguration.getIntervalTimeForMoActionCache();

        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(MO_TIMER_INFO);
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(initialDelay, intervalDelay, timerConfig);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        eventData.put(SHMEvents.ShmPostConstructConstants.MESSAGE, String.format("MOAction timer started successfully with Initial delay : %d  and interval delay: %d", initialDelay, intervalDelay));
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /**
     * will called on receiving of change event for INITIAL_DELAY_TO_START_POLLING_AFTER_TIMER_CREATED_IN_MILLISECONDS or INTERVAL_TIME_FOR_MO_ACTION_CACHE_ITERATION_IN_MILLISECONDS configuration
     * parameters.
     */
    public void restartTimer() {
        cancelTimer();

        initTimer();
    }

    /**
     * Will called at the time of container shutdown.
     */
    private void cancelTimer() {

        for (final Timer timer : timerService.getTimers()) {
            final String timerInfo = (String) timer.getInfo();
            if (MO_TIMER_INFO.equalsIgnoreCase(timerInfo)) {
                timer.cancel();
                LOGGER.info("MOAction Timer cancelled successfully");
            }
        }
    }

    /**
     * Will be fired at every timeout and triggers an MO read call to get the action triggered status on node.
     */

    @Timeout
    public void timeout() {
        if (membershipListenerInterface.isMaster()) {
            moActionRequestService.processMoActionRequests();
        }
    }
}
