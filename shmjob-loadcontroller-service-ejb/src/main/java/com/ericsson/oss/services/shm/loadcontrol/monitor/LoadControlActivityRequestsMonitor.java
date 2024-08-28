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
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.services.shm.loadcontrol.schedule.TimerContext;

/**
 * To Maintain the state of the activity request timer, either the timer thread is busy(processing requests) or free to process the requests.
 * 
 * @author xrajeke
 * 
 */
@ApplicationScoped
public class LoadControlActivityRequestsMonitor {

    /**
     * Multiple timer threads can read from and write to it without the chance of receiving out-of-date or corrupted data
     */
    private final ConcurrentHashMap<String, Boolean> activityRequestStatusMap = new ConcurrentHashMap<String, Boolean>();

    /**
     * Updates the current timer state to the activityRequestStatusMap with a key generated from {@link TimerContext}.
     * 
     * @param timerContext
     * @param isProcessingMessages
     */
    public void updateActivityRequestStatus(final TimerContext timerContext, final boolean isProcessingMessages) {
        activityRequestStatusMap.put(getRequestKey(timerContext), isProcessingMessages);
    }

    /**
     * To find the current state for the given {@link TimerContext}.
     * 
     * @param timerContext
     * @return Boolean - whether an earlier Timer thread is already processing the requests or not.
     */
    public boolean isProcessingMessages(final TimerContext timerContext) {
        final Boolean isActivtyRequestInProcess = activityRequestStatusMap.get(getRequestKey(timerContext));
        return isActivtyRequestInProcess == null ? false : isActivtyRequestInProcess;
    }

    private String getRequestKey(final TimerContext timerContext) {
        return timerContext.getPlatform() + "_" + timerContext.getJobType() + "_" + timerContext.getActivity();
    }

}
