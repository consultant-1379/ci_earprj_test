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
package com.ericsson.oss.services.shm.es.api;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

/**
 * The number of on-going processes will be captured
 * 
 * @author tcskrna
 * 
 */
@ApplicationScoped
public class JobsNotificationLoadCounter {

    private final AtomicInteger notificationsInProcess = new AtomicInteger(0);

    /**
     * Increments the counter by 1, and returns its value.
     * 
     * @return - notificationsInProcess
     */
    public int increment() {
        return this.notificationsInProcess.incrementAndGet();
    }

    /**
     * Decrements the counter by 1, and returns its value.
     * 
     * @return -notificationsInProcess
     */
    public int decrement() {
        return this.notificationsInProcess.decrementAndGet();
    }

    /**
     * Gets the present counter value
     * 
     * @return - notificationsInProcess
     */

    public int getCounter() {
        return this.notificationsInProcess.get();
    }
}
