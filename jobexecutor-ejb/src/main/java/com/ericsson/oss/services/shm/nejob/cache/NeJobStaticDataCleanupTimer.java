/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.nejob.cache;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
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

import com.ericsson.oss.services.shm.activity.axe.cache.AxeUpgradeSynchronousActivityProvider;
import com.ericsson.oss.services.shm.job.axe.cache.AxeUpgradeJobStaticDataProvider;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;

/**
 * Timer will run every day at configured time, and time is a configurable parameter.
 * 
 * @author tcsgusw
 * 
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NeJobStaticDataCleanupTimer {

    private final static Logger LOGGER = LoggerFactory.getLogger(NeJobStaticDataCleanupTimer.class);

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private AxeUpgradeSynchronousActivityProvider axeActivityStaticDataProvider;

    @Resource
    private TimerService timerService;

    @Inject
    private NeJobStaticDataConfigListener jobStaticDataConfigListener;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private AxeUpgradeJobStaticDataProvider axeUpgradeJobStaticDataProvider;

    private static final String TIMER_INFO = "NeCacheCleanup";

    private Timer timer;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    public void startTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        final String dailyScheduleTime = jobStaticDataConfigListener.getDailyScheduleTimeForNeCacheCleanup();

        try {
            final ScheduleExpression scheduleExpression = new ScheduleExpression();
            final String[] timeArray = dailyScheduleTime.split(":");

            final int hour = Integer.parseInt(timeArray[0]);
            final int minute = Integer.parseInt(timeArray[1]);

            scheduleExpression.hour(hour).minute(minute);
            final TimerConfig timerConfig = new TimerConfig();
            timerConfig.setPersistent(false);
            timerConfig.setInfo(TIMER_INFO);
            timer = timerService.createCalendarTimer(scheduleExpression, timerConfig);
            LOGGER.debug("Timer started successfully");

        } catch (final Exception ex) {
            LOGGER.debug("Restart of timer failed due to invalid schedule time received: {}", dailyScheduleTime);
        }
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @Timeout
    public void timeout() {
        neJobStaticDataProvider.clearAll();
        jobStaticDataProvider.clearAll();
        axeUpgradeJobStaticDataProvider.clearAll();
        axeActivityStaticDataProvider.clearAll();
    }

    @PreDestroy
    public void destroy() {
        cancel();
    }

    public void restartTimer() {
        cancel();
        startTimer();
    }

    private void cancel() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
