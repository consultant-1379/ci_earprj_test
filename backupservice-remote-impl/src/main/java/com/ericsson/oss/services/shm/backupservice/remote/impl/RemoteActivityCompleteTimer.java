package com.ericsson.oss.services.shm.backupservice.remote.impl;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.notification.common.RemoteActivityNotificationHelper;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * Once after receiving final notification, this service will call registered elementary service's "onActionComplete" method after the configured delay time elapsed. This delay ensures that all the
 * required attributes synched in DPS after final notification received.
 * 
 * 
 * @author xsrabop
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RemoteActivityCompleteTimer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    TimerService timerService;

    @Inject
    private RemoteActivityNotificationHelper remoteActivityNotificationHelper;

    /**
     * TODO Change the hard coded timeout value to configurable parameter in shm-models so that it can be changed at runtime.
     */
    private final static long defaultTimeout = 600000; //In milliseconds = 10 minutes

    public void startTimer(final FdnNotificationSubject fdnNotificationSubject) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(fdnNotificationSubject);
        timerService.createSingleActionTimer(defaultTimeout, timerConfig);
        logger.debug("Started timer with timeout value of {} milliseconds", defaultTimeout);
    }

    /**
     * @param timer
     * 
     *            This method is called by the container after the timer has expired. This is private because elementary services are not supposed to call this method directly.
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @Timeout
    private void handleTimeout(final Timer timer) {

        final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) timer.getInfo();
        logger.debug("Woke up from timeout. FdnNotificationSubject {} ", fdnNotificationSubject);
        if (!fdnNotificationSubject.getNotificationCallBackResult().isCompleted()) {
            remoteActivityNotificationHelper.unSubscribeToNotification(fdnNotificationSubject, fdnNotificationSubject.getFdn());
            logger.debug("Returned back after unsubscribing to notification.");
        }
    }

}
