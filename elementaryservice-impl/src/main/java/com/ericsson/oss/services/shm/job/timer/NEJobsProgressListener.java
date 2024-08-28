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
package com.ericsson.oss.services.shm.job.timer;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * Invokes a progress update timer , immediate to the application deployment. Also listens for the configured time out value and restarts the timer when new value notified.
 * 
 * @author xneranu
 * 
 */
@Singleton
@Startup
public class NEJobsProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NEJobsProgressListener.class);

    private static final String NE_JOB_PROGRESS_UPDATE_TIMER = "ne_job_progress_update_timer";

    @Inject
    private NEJobProgressTimerRetryProxy neJobProgressTimer;

    @Inject
    @Configured(propertyName = "NEJob_progressUpdate_timer_timeout_sec")
    private int NEJobProgressUpdateTimerTimeoutInSec;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Listener for shmJob_progressUpdate_timer_timeout_min attribute value
     * 
     * @param shmJobProgressUpdateTimerTimeoutInMin
     */
    public void listenForShmNEJobProgressUpdateTimerTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "NEJob_progressUpdate_timer_timeout_sec") final int NEJobProgressUpdateTimerTimeoutInSec) {

        LOGGER.info("An old timer [{}] will be cancelled, since the configured Schedular time out value was modified from {}min to {}min.", NE_JOB_PROGRESS_UPDATE_TIMER,
                this.NEJobProgressUpdateTimerTimeoutInSec, NEJobProgressUpdateTimerTimeoutInSec);

        this.NEJobProgressUpdateTimerTimeoutInSec = NEJobProgressUpdateTimerTimeoutInSec;

        neJobProgressTimer.reStartTimer(NE_JOB_PROGRESS_UPDATE_TIMER, NEJobProgressUpdateTimerTimeoutInSec);
    }

    /**
     * Starts the timer service on SHM service start up.
     * 
     */
    @PostConstruct
    public void startTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        neJobProgressTimer.startTimer(NE_JOB_PROGRESS_UPDATE_TIMER, NEJobProgressUpdateTimerTimeoutInSec);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }
}
