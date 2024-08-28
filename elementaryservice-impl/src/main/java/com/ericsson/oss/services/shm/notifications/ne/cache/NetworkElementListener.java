package com.ericsson.oss.services.shm.notifications.ne.cache;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementCacheProvider;
import com.ericsson.oss.services.shm.shared.NotificationListener;

@Singleton
@Startup
public class NetworkElementListener implements NotificationListener {


    @Inject
    private NetworkElementCacheProvider networkElementCache;

    private EventConsumerBean eventConsumerBean;

    @Inject
    private SystemRecorder systemRecorder;

    protected String getFilter() {
        return ShmCommonConstants.NETWORK_ELEMENT_FILTER;
    }

    @Override
    public boolean stopNotificationListener() {
        return eventConsumerBean.stopListening();
    }

    @PostConstruct
    public void init() {
        final long postConstructStarted = System.currentTimeMillis();
        eventConsumerBean = new EventConsumerBean(ShmCommonConstants.SHM_JOB_NOTIFICATION_CHANNEL_URI);
        eventConsumerBean.setFilter(getFilter());
        eventConsumerBean.startListening(new NetworkElementObserver(networkElementCache));
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }
}
