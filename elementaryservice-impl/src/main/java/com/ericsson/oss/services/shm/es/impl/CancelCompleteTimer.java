package com.ericsson.oss.services.shm.es.impl;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CancelCompleteTimer {

    private final Logger logger = LoggerFactory.getLogger(CancelCompleteTimer.class);

    @Resource
    TimerService timerService;

    @Inject
    private ActivityUtils activityUtils;

    private final static long defaultTimeout = 15000; //In milliseconds = 15 seconds

    @Asynchronous
    public void startTimer(final JobActivityInfo jobActivityInfo) {
        logger.info("Timer Starting");
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(jobActivityInfo);
        timerService.createSingleActionTimer(defaultTimeout, timerConfig);
        logger.debug("Started timer with timeout value of {} milliseconds", defaultTimeout);
        logger.info("Timer Started");
    }

    /**
     * @param timer
     * 
     *            This method is called by the container after the timer has expired. This is private because elementary services are not supposed to call this method directly.
     */
    @Timeout
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void handleTimeout(final Timer timer) {

        final JobActivityInfo jobActivityInfo = (JobActivityInfo) timer.getInfo();
        logger.debug("Woke up from timeout. Timed activity context {} ", jobActivityInfo);

        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        logger.debug("Resolved elementary service impl with activityJobId:{}", activityJobId);

        activityUtils.sendCancelMOActionDoneToWFS((String) jobEnvironment.getNeJobAttributes().get(ShmConstants.BUSINESS_KEY));

    }
}
