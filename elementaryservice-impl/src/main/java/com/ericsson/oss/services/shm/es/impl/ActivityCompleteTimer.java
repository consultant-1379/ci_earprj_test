package com.ericsson.oss.services.shm.es.impl;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;

/**
 * Once after receiving final notification, this service will call registered elementary service's "onActionComplete" method after the configured delay time elapsed. This delay ensures that all the
 * required attributes synched in DPS after final notification received.
 * 
 * 
 * @author tcsravg
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ActivityCompleteTimer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    TimerService timerService;

    @Inject
    private ActivityCompleteCallBackProvider activityCompleteCallBackProvider;

    /**
     * TODO Change the hard coded timeout value to configurable parameter in shm-models so that it can be changed at runtime.
     */
    private final static long defaultTimeout = 15000; // In milliseconds = 15
                                                      // seconds

    public void startTimer(final JobActivityInfo jobActivityInfo) {
        startTimer(defaultTimeout, jobActivityInfo);
    }

    public void startTimer(final long duration, final JobActivityInfo jobActivityInfo) {

        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(jobActivityInfo);
        timerService.createSingleActionTimer(duration, timerConfig);
        logger.debug("Started timer with timeout value of {} milliseconds", duration);
    }

    /**
     * @param timer
     * 
     *            This method is called by the container after the timer has expired. This is private because elementary services are not supposed to call this method directly.
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @Timeout
    private void handleTimeout(final Timer timer) {

        final JobActivityInfo jobActivityInfo = (JobActivityInfo) timer.getInfo();
        logger.debug("Woke up from timeout. Timed activity context {} ", jobActivityInfo);

        final ActivityCompleteCallBack activityImpl = activityCompleteCallBackProvider.onActionCompleteHandler(jobActivityInfo.getPlatform(), jobActivityInfo.getJobType(),
                jobActivityInfo.getActivityName());
        logger.debug("Resolved elementary service impl with activityJobId:{}", jobActivityInfo.getActivityJobId());
        if (activityImpl != null) {
            activityImpl.onActionComplete(jobActivityInfo.getActivityJobId());
            logger.trace("Returned back after invoking the asynchronous method");
        } else {
            logger.error("Implementation class not found for interface ActivityCompleteCallBack with activity name = {}, job type = {} and platform = {} ", jobActivityInfo.getActivityName(),
                    jobActivityInfo.getJobType(), jobActivityInfo.getPlatform());
        }

    }

}
