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
package com.ericsson.oss.services.shm.webpush.notifications.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;

/**
 * Listens Job POs change Notifications
 * 
 * @author xcharoh
 * 
 * 
 */
public class ShmJobNotificationQueueListener implements EMessageListener<DpsDataChangedEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JobNotificationUtil jobNotificationUtil;

    public ShmJobNotificationQueueListener(final JobNotificationUtil jobNotificationUtil) {
        this.jobNotificationUtil = jobNotificationUtil;
    }

    @Override
    /**
     * Delete webpush handled in deleteJobs function (SHMJobServiceImpl.java)
     */
    public void onMessage(final DpsDataChangedEvent message) {

        logger.info(" Notification Received through ShmJobNotification Queue: {}", message);

        if (message instanceof DpsObjectCreatedEvent) {
            jobNotificationUtil.notifyAsCreate((DpsObjectCreatedEvent) message);
        }
        if (message instanceof DpsAttributeChangedEvent) {
            jobNotificationUtil.notifyAsUpdate((DpsAttributeChangedEvent) message);
        }
    }
}
