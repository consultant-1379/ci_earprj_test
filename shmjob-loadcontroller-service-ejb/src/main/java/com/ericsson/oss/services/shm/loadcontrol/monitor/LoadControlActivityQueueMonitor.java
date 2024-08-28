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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class will monitor all activity Queues read queue messages and persist them in DB and it is for maintaining backward compatibility.
 * 
 */

@Singleton
@Startup
public class LoadControlActivityQueueMonitor {

    @Inject
    private ProcessQueue processQueue;

    @Inject
    private SystemRecorder systemRecorder;

    private final Logger logger = LoggerFactory.getLogger(LoadControlActivityQueueMonitor.class);

    /**
     * This method will read all activity Queues messages and persist them in DB.
     * 
     */
    @PostConstruct
    public void startActivityQueueMonitors() {
        final long postConstructStarted = System.currentTimeMillis();
        logger.info("Load Controller Activity Queues monitors are initializing...");
        processQueue.persistQueueMessagesIntoDB();
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }
}
