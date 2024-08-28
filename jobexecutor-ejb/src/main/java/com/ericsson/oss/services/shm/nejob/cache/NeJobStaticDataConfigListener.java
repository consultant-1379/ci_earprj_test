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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * Listen for NE job cache configuration parameters and restarts the NeJobDataCacheCleanupTimer.
 * 
 * @author tcsgusw
 * 
 */
@ApplicationScoped
public class NeJobStaticDataConfigListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(NeJobStaticDataCleanupTimer.class);

    @Inject
    private NeJobStaticDataCleanupTimer neJobCacheCleanupTimer;

    @Inject
    @Configured(propertyName = "neCacheCleanup_scheduleTime_HH_colon_MM")
    private String neCacheCleanUpTime;

    /**
     * Listener for neCacheCleanup_scheduleTime_HH_colon_MM attribute value
     * 
     * @param neCacheCleanUpTime
     */
    void listenForCleanupNeCacheScheduleTimeHoursAttribute(@Observes @ConfigurationChangeNotification(propertyName = "neCacheCleanup_scheduleTime_HH_colon_MM") final String neCacheCleanUpTime) {
        this.neCacheCleanUpTime = neCacheCleanUpTime;
        LOGGER.debug("Daily schedule time for cleanup of ne job static from cache  {}", neCacheCleanUpTime);
        neJobCacheCleanupTimer.restartTimer();
    }

    public String getDailyScheduleTimeForNeCacheCleanup() {
        return this.neCacheCleanUpTime;
    }

}
