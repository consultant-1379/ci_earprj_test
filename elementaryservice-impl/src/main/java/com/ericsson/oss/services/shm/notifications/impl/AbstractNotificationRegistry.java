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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.model.NotificationSubject;

public abstract class AbstractNotificationRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * registration inserts the subject in to the cache;subject.getKey() is key , value is subject itself;
     */
    public void register(final Cache<String, NotificationSubject> cache, final NotificationSubject subject) {
        if (subject != null) {
            logger.info("The subject for registering is={}", subject);
            cache.put(subject.getKey(), subject);
            logger.info("The cache that has registered subject is={}", cache);
        } else {
            logger.error("No Subject found");
        }
    }

    /**
     * return all subjects;
     */
    public NotificationSubject getListener(final Cache<String, NotificationSubject> cache, final String key) {
        logger.info("The listener is={}", cache.get(key));
        return cache.get(key);
    }

    /**
     * Removes the cache entry for given subject. If the saved subject for an FDN is not same as the given subject then it ignores to remove.
     * 
     * @param cache
     * @param subject
     * @return boolean, true if it is removed.
     */
    public boolean removeSubject(final Cache<String, NotificationSubject> cache, final NotificationSubject subject) {
        if (subject == null || subject.getKey() == null) {
            logger.error("Invalid NotificationSubject : {}", subject);
            return false;
        }
        final NotificationSubject entryValue = cache.get(subject.getKey());

        if (entryValue == null || !entryValue.equals(subject)) {
            logger.error("Entry missing for subject::{}, instead it found a subject as::{}", subject, entryValue);
            return false;
        }

        return cache.remove(subject.getKey());

    }

}
