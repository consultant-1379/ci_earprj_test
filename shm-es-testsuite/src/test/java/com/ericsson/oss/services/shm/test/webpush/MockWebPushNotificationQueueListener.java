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
package com.ericsson.oss.services.shm.test.webpush;

import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;

public class MockWebPushNotificationQueueListener implements EMessageListener<Event> {

    private final MockJobNotificationReciever mockJobNotificationReciever;

    public MockWebPushNotificationQueueListener(final MockJobNotificationReciever mockJobNotificationReciever) {
        this.mockJobNotificationReciever = mockJobNotificationReciever;
    }

    @Override
    public void onMessage(final Event message) {

        System.out.println("====Notification Received through Shm Queue: {}" + message);

        mockJobNotificationReciever.notify();
    }

}
