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
package com.ericsson.oss.service.shm.loadcontrol.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlCounterManager;

@RunWith(MockitoJUnitRunner.class)
public class LoadControlCounterManagerTest {

    @InjectMocks
    LoadControlCounterManager loadControlcounterManager;

    @Mock
    private ConcurrentHashMap<String, AtomicLong> mapMock;

    @Test
    public void testGetCounter() {
        final String counterName = loadControlcounterManager.getCounterName("Platform", "Job", "Activity");
        final AtomicLong counter = loadControlcounterManager.getCounter(counterName);
        Assert.assertEquals(counter.get(), 0l);
    }

    @Test
    public void testGetGlobalCounter() {
        final String counterName = "CPPBACKUPexportcv";
        final AtomicLong counter = loadControlcounterManager.getGlobalCounter(counterName);
        Assert.assertEquals(counter.get(), 0l);
    }

    @Test
    public void testGetCounterName() {
        final String expectedString = "LC_" + "CPP" + "BACKUP" + "exportcv";
        final String counterName = loadControlcounterManager.getCounterName("CPP", "BACKUP", "exportcv");
        Assert.assertEquals(expectedString, counterName);
    }

    @Test
    public void testResetCounterWhenItIsAlreadyHavingSomeCount() {
        final AtomicLong counter = loadControlcounterManager.getCounter("LC_CPPBACKUPexportcv");
        counter.incrementAndGet();
        loadControlcounterManager.resetCounter("CPPBACKUPexportcv");
        final AtomicLong finalCounter = loadControlcounterManager.getCounter("LC_CPPBACKUPexportcv");
        Assert.assertEquals(counter.get(), 1l);
        Assert.assertEquals(finalCounter.get(), 0l);
    }

    @Test
    public void testResetGlobalCounterWhenItIsAlreadyHavingSomeCount() {
        final AtomicLong counter = loadControlcounterManager.getGlobalCounter("CPPBACKUPexportcv");
        counter.incrementAndGet();
        loadControlcounterManager.resetGlobalCounter("CPPBACKUPexportcv");
        final AtomicLong finalCounter = loadControlcounterManager.getGlobalCounter("CPPBACKUPexportcv");
        Assert.assertEquals(counter.get(), 1l);
        Assert.assertEquals(finalCounter.get(), 0l);
    }
}
