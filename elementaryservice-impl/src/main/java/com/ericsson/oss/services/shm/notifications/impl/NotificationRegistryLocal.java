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
package com.ericsson.oss.services.shm.notifications.impl;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

/**
 * Local cache used for registering local observers(non-job type) for avc notifications.
 * 
 */
@ApplicationScoped
@NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
public class NotificationRegistryLocal extends AbstractNotificationRegistry implements NotificationRegistry {

    @Inject
    @NamedCache("SHMAVCNotificationCacheLocal")
    private Cache<String, NotificationSubject> cache;

    /**
     * registration inserts the subject in to the cache;subject.getKey() is key , value is subject itself;
     */
    @Override
    public void register(final NotificationSubject subject) {
        register(cache, subject);
    }

    /**
     * return all subjects;
     */
    @Override
    public NotificationSubject getListener(final String key) {
        return getListener(cache, key);
    }

    @Override
    public boolean removeSubject(final NotificationSubject subject) {
        return removeSubject(cache, subject);
    }

}
