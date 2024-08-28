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

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;

/**
 * Timer service to invoke the NE job progress updater.
 * 
 * @author xneranu , xthagan
 * 
 */
@Stateless
public class NEJobProgressUpdateInitiationTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NEJobProgressUpdateInitiationTimer.class);

    @Inject
    private TimerService timerService;

    @Inject
    private NEJobProgressPercentageCache jobProgressPercentageCache;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    public static int noOfRetries = 2;

    public void startTimer(final Serializable timerInfo, final int timerTimeoutInSec) {
        final TimerConfig timerConfig = new TimerConfig(timerInfo, false);
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.hour("*").minute("*").second("*/" + timerTimeoutInSec);
        timerService.createCalendarTimer(scheduleExpression, timerConfig);
        LOGGER.info("Started a timer named: {} with timeout value of {} seconds.", timerInfo, timerTimeoutInSec);
    }

    public void reStartTimer(final Serializable timerInfo, final int timerTimeoutInSec) {
        final Collection<Timer> timerCollection = timerService.getTimers();
        if (timerCollection != null) {
            for (Timer timer : timerCollection) {
                if (timerInfo.equals(timer.getInfo())) {
                    timer.cancel();
                    startTimer(timerInfo, timerTimeoutInSec);
                    LOGGER.trace("ReStarted a timer named: {} with new timeout value of {} seconds.", timerInfo, timerTimeoutInSec);
                    break;
                }
            }
        }
    }

    /**
     * Will be invoked for each defined time interval by a Timer service.
     * <p>
     * For every invocation finds all the running NEjobs and updates the progress percentage asynchronously for all NE jobs.
     */
    @Timeout
    @AccessTimeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void timeout(final Timer timer) {
        Object[] neJobList;

        try {
            neJobList = jobProgressPercentageCache.retrieveNEJobs();
            if (neJobList != null && neJobList.length != 0) {
                persistNEJobs(neJobList);
            } else {
                LOGGER.trace("neJobList is either empty or null -{} ", neJobList);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception occurred while retrieving the NE jobs from Cache {}", ex);
        }
    }

    private void persistNEJobs(final Object[] neList) {
        for (Object key : neList) {
            try {
                activityAndNEJobProgressCalculator.updateNEJobProgressWithRetry((Long) key, noOfRetries);
            } catch (Exception ex) {
                LOGGER.error("Exception occurred while updating the NE jobs from Cache {} So Placing failed NE jobs again into Cache", ex);
                jobProgressPercentageCache.bufferNEJobs((long) key);
            }
        }

    }
}
