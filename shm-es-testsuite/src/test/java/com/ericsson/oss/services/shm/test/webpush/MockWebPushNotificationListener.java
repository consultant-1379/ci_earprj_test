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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.services.shm.shared.NotificationListener;

public abstract class MockWebPushNotificationListener implements NotificationListener {

    private static final String WEB_PUSH_QUEUE = "jms:/queue/WebPushQueue";

    @Inject
    MockJobNotificationReciever mockJobNotificationReciever;

    private EventConsumerBean eventConsumerBean;

    protected abstract String getFilter();

    @PostConstruct
    protected void subscribeForDpsEvents() {
        startListener();
    }

    private void startListener() {
        eventConsumerBean = getEventConsumerBean();
        final boolean isListening = startListeningForJobNotifications(eventConsumerBean);
        System.out.println("Start Listening ? " + isListening);
    }

    @Override
    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    protected EventConsumerBean getEventConsumerBean() {
        final EventConsumerBean eventConsumerBean = new EventConsumerBean(WEB_PUSH_QUEUE);
        return eventConsumerBean;
    }

    protected boolean startListeningForJobNotifications(final EventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(new MockWebPushNotificationQueueListener(mockJobNotificationReciever));
    }

}
