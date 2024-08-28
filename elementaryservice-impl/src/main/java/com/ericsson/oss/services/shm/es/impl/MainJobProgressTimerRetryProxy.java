/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
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

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.timer.TimerServiceRetryPolicies;

/**
 * Timer service Retry Proxy to start / restart the timers .
 * 
 * @author xrajeke
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Deprecated
public class MainJobProgressTimerRetryProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainJobProgressTimerRetryProxy.class);
    private static final String TIMER_EVENT = "SHM.TIMER_SERVICE";

    @Inject
    private MainJobProgressUpdateInitiationTimer mainJobProgressTimer;

    @Inject
    private RetryManager retryManager;

    @Inject
    private TimerServiceRetryPolicies timerRetryPolocies;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Creates a Calendar Timer and Starts with in the retry block. This method made to be @Asynchronous, since any failure in the timer creation should not impact the application deployment as this
     * is being invoked in @PostConstruct
     * 
     * @param timerConfig
     * @param scheduleExpression
     */
    @Asynchronous
    public void startProgressUpdateTimer(final Serializable timerInfo, final int timerTimeoutInMin) {
        try {
            retryManager.executeCommand(timerRetryPolocies.getTimerRetryPolicy(), new RetriableCommand<Void>() {
                public Void execute(final RetryContext retryContext) {
                    mainJobProgressTimer.startProgressUpdateTimer(timerInfo, timerTimeoutInMin);
                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to create a new Timer, with timout of " + timerTimeoutInMin + " minutes. due to {}", e.getMessage());
            systemRecorder.recordEvent(TIMER_EVENT, EventLevel.COARSE, "", timerInfo.toString(), "Failed to create a new Timer, with timout of " + timerTimeoutInMin + " minutes");
        }
    }

    /**
     * Cancels the Timer, whichever first matches with the given timerInfo, and then creates a new Calendar Timer and Starts with in the retry block
     * 
     * @param timerConfig
     * @param scheduleExpression
     */
    public void reStartProgressUpdateTimer(final Serializable timerInfo, final int timerTimeoutInMin) {
        try {
            retryManager.executeCommand(timerRetryPolocies.getTimerRetryPolicy(), new RetriableCommand<Void>() {
                public Void execute(final RetryContext retryContext) {
                    mainJobProgressTimer.reStartProgressUpdateTimer(timerInfo, timerTimeoutInMin);
                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to restart a Timer, with new timout of " + timerTimeoutInMin + " minutes. due to {}", e.getMessage());
            systemRecorder.recordEvent(TIMER_EVENT, EventLevel.COARSE, "", timerInfo.toString(), "Failed to restart a Timer, with new timout of " + timerTimeoutInMin + " minutes");
        }
    }
}
