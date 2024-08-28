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
package com.ericsson.oss.services.shm.loadcontrol.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LoadControlCounterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadControlCounterManager.class);

    private static final String LOAD_COUNTER_PREFIX = "LC_";

    private final Map<String, AtomicLong> counterMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> globalCounterMap = new ConcurrentHashMap<>();

    public AtomicLong getCounter(final String counterName) {
        AtomicLong counter = counterMap.get(counterName);
        if (counter == null) {
            counter = new AtomicLong();
            counterMap.put(counterName, counter);
        }
        return counter;
    }

    public String getCounterName(final String platform, final String jobType, final String name) {
        return LOAD_COUNTER_PREFIX + platform + jobType + name;

    }

    public AtomicLong getGlobalCounter(final String counterName) {
        AtomicLong globalCounter = globalCounterMap.get(counterName);
        if (globalCounter == null) {
            globalCounter = new AtomicLong();
            globalCounterMap.put(counterName, globalCounter);
        }
        return globalCounter;
    }

    public void resetCounter(final String counterName) {
        AtomicLong counter = counterMap.get(LOAD_COUNTER_PREFIX + counterName);
        if (counter != null) {
            final long oldValue = counter.get();
            counter = new AtomicLong();
            counterMap.put(LOAD_COUNTER_PREFIX + counterName, counter);
            LOGGER.info("For Local Counter {}, Value before Reset {}; Value after Reset {}", counterName, oldValue, counterMap.get(LOAD_COUNTER_PREFIX + counterName));
        } else {
            LOGGER.info("Local Counter {} is null.", counterName);
        }

    }

    public void resetGlobalCounter(final String counterName) {
        AtomicLong globalCounter = globalCounterMap.get(counterName);
        if (globalCounter != null) {
            final long oldValue = globalCounter.get();
            globalCounter = new AtomicLong();
            globalCounterMap.put(counterName, globalCounter);
            LOGGER.info("For Global Counter {}, Value before Reset {}; Value after Reset {}", counterName, oldValue, counterMap.get(counterName));
        } else {
            LOGGER.info("Global Counter {} is null.", counterName);
        }

    }
}
