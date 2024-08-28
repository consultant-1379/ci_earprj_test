/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivity;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityInfoProvider;
import com.ericsson.oss.services.shm.es.polling.api.ReadCallStatusEnum;

/**
 * This is a timer class, it will manage polling activity timer at application level. This will read data from cache and if activity eligible, then this will call corresponding elementary service to
 * check activity status.
 * 
 * @author tcsgusw
 * 
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ActivityPollingTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityPollingTimer.class);

    @Resource
    private TimerService timerService;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Inject
    private PollingActivityProvider pollingActivityProvider;

    @Inject
    private PollingActivityStatusManager pollingActivityStatusManager;

    @Inject
    private SystemRecorder systemRecorder;

    private static final String pollingTimerInfo = "CppActivityPollingTimer";

    /**
     * This method initialize the timer at application startup.
     */

    @PostConstruct
    public void initTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        final int initialDelay = pollingActivityConfiguration.getInitialDelayToStartTimerAfterServiceStartUp();
        final int intervalDelay = pollingActivityConfiguration.getPollingIntervalDelay();

        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(pollingTimerInfo);
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(initialDelay, intervalDelay, timerConfig);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        eventData.put(SHMEvents.ShmPostConstructConstants.MESSAGE, String.format("Polling timer started successfully with Initial delay : %d  and interval delay: %d", initialDelay, intervalDelay));
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /**
     * will called on receiving of change event for INITIAL_DELAY_TO_START_POLLING_AFTER_TIMER_CREATED_IN_MILLISECONDS or INTERVAL_TIME_FOR_POLLING_IN_MILLISECONDS configuration parameters.
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
            if (pollingTimerInfo.equalsIgnoreCase(timerInfo)) {
                timer.cancel();
                LOGGER.info("Polling Timer cancelled successfully");
            }
        }
    }

    /**
     * Will be fires at every timeout and triggers respective elementary service {@link PollingActivity.readActivityStatus(long, String,String)} method .
     */
    @Timeout
    public void timeout() {
        final Map<String, PollingActivityInfoProvider> pollingCache = pollingActivityStatusManager.getPollingActivitiesCache();
        final Iterator<String> iterator = pollingCache.keySet().iterator();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            try {
                final PollingActivityInfoProvider pollingActivityInfoProvider = pollingCache.get(key);

                if (isPollingEnabled(PlatformTypeEnum.getPlatform(pollingActivityInfoProvider.getPlatform()))) {
                    final String activityName = pollingActivityInfoProvider.getActivityName();
                    final String jobType = pollingActivityInfoProvider.getJobType();
                    final long activityJobId = pollingActivityInfoProvider.getActivityJobId();

                    if (pollingActivityInfoProvider.getPollingStartTime().before(new Date())) {
                        final PollingActivity activity = pollingActivityProvider.getPollingActivity(pollingActivityInfoProvider.getPlatform(), jobType, activityName);
                        if (activity != null) {
                            final ReadCallStatusEnum readCallStatus = pollingActivityStatusManager.getReadCallStatus(activityJobId, pollingActivityInfoProvider.getMoFdn());
                            if (ReadCallStatusEnum.COMPLETED.equals(readCallStatus) || ReadCallStatusEnum.NOT_TRIGGERED.equals(readCallStatus)) {
                                activity.readActivityStatus(activityJobId, pollingActivityInfoProvider.getMoFdn());
                                LOGGER.debug("Service method triggered successfully for the activityJobId: {}, activityName:{},platform:{} and jobType:{}", activityJobId, activityName,
                                        pollingActivityInfoProvider.getPlatform(), jobType);
                            }
                        } else {
                            LOGGER.error("Implementation not found for interface PollingActivity for the platform:{}, jobType: {} and activity: {}", pollingActivityInfoProvider.getPlatform(),
                                    jobType, activityName);
                        }
                    }
                }
            } catch (final Exception ex) {
                LOGGER.error("Exception occurred while iterating over the cache with cache key :{}, Exception is: ", key, ex);
            }
        }
    }

    private boolean isPollingEnabled(final PlatformTypeEnum platform) {
        boolean pollingEnabled = false;
        switch (platform) {
        case CPP:
            pollingEnabled = pollingActivityConfiguration.isPollingEnabledForJobsOnCppNodes();
            break;

        default:
            LOGGER.warn("Polling mechanism not in place for the platform : {}", platform);
            break;
        }
        return pollingEnabled;
    }

}