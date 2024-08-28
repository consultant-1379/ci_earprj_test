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
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;

/**
 * 
 * This class is used to create Polling Timer.
 * 
 * @author xyogven
 * 
 */

@Startup
@Singleton
public class PollingTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingTimer.class);

    @Resource
    private TimerService timerService;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private PollingActivityRequestDataService pollingActivityService;

    @Inject
    private PollingActivityConfigurationImpl pollingActivityConfiguration;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    public void initTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        final TimerConfig timerConfig = new TimerConfig();
        final int initialDelay = pollingActivityConfiguration.getInitialDelayToStartTimerAfterServiceStartUp();
        final int intervalDelay = pollingActivityConfiguration.getPollingIntervalDelay();
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(initialDelay, intervalDelay, timerConfig);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        eventData.put(SHMEvents.ShmPostConstructConstants.MESSAGE, String.format("Polling Activity timer started successfully with Initial delay : %d  and interval delay: %d", initialDelay, intervalDelay));
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /**
     * will be called on receiving of change event for INITIAL_DELAY_TO_START_POLLING_AFTER_TIMER_CREATED_IN_MILLISECONDS or INTERVAL_TIME_FOR_POLLING_IN_MILLISECONDS configuration parameters.
     */
    public void restartTimer() {
        cancelTimer();

        initTimer();
    }

    /**
     * Will be called at the time of container shutdown.
     */
    private void cancelTimer() {

        for (final Timer timer : timerService.getTimers()) {
            timer.cancel();
            LOGGER.info("Polling Timer cancelled successfully");
        }
    }

    @Timeout
    public void timeout(final Timer timer) {
        if (membershipListenerInterface.isMaster()) {
            pollingActivityService.startPolling();
        }
    }

}
