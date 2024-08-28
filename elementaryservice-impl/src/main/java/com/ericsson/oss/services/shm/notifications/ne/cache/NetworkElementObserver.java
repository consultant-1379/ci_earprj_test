package com.ericsson.oss.services.shm.notifications.ne.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.EventType;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementCacheProvider;

public class NetworkElementObserver implements EMessageListener<DpsDataChangedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkElementObserver.class);

    private final NetworkElementCacheProvider networkElementCache;

    public NetworkElementObserver(final NetworkElementCacheProvider networkElementCache) {
        this.networkElementCache = networkElementCache;
    }

    @Override
    public void onMessage(final DpsDataChangedEvent message) {
        LOGGER.debug("Message output {}", message);

        final EventType eventType = message.getEventType();
        if (eventType == EventType.OBJECT_DELETED) {

            networkElementCache.remove(message);

        } else if (eventType == EventType.ATTRIBUTE_CHANGED) {
            networkElementCache.update(message);
        }

    }
}
