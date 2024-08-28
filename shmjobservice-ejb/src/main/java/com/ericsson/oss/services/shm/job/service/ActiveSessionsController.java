/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.ServerInternalException;

/**
 * To control the SHM User active sessions. This has been introduced to avoid VM clean start since the SHM service is getting overloaded sometimes when only 1 VM available.
 * 
 * @author xrajeke
 *
 */
@ApplicationScoped
public class ActiveSessionsController {

    protected static final String VIEW_MAIN_JOBS = "viewMainJobs";
    protected static final String VIEW_JOB_LOGS = "viewJobLogs";

    private static final String MAX_SERVER_CONNECTIONS_REACHED = "Server is busy. Please try after sometime";

    private static final Map<String, AtomicInteger> applicationContextController = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveSessionsController.class);

    @Inject
    private JobParameterChangeListener listener;

    static {
        applicationContextController.put(VIEW_MAIN_JOBS, new AtomicInteger(0));
        applicationContextController.put(VIEW_JOB_LOGS, new AtomicInteger(0));
    }

    /**
     * Finds the current active user sessions and check if user is allowed to access the applicationContext.
     * <p>
     * If sessions reached maximum limit throws the exception without incrementing anything. <br>
     * Otherwise increments the active sessions.
     * 
     * @param applicationContext
     */
    public void exitIfMaxActiveSessionsReached(final String applicationContext) {
        final AtomicInteger activeSessions = applicationContextController.get(applicationContext);
        LOGGER.debug("{} has {} active user sessions currently", applicationContext, activeSessions);
        final boolean allowApplicationAccess = activeSessions != null && activeSessions.get() < listener.getActiveUserSessionsMaxLimit();
        if (allowApplicationAccess) {
            activeSessions.incrementAndGet();
        } else {
            throw new ServerInternalException(MAX_SERVER_CONNECTIONS_REACHED);
        }
    }

    public int decrementAndGet(final String applicationContext) {
        if (applicationContextController.containsKey(applicationContext)) {
            return applicationContextController.get(applicationContext).decrementAndGet();
        }
        return -1;
    }
}
