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
package com.ericsson.services.shm.loadcontrol.instrumentation;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.loadcontrol.instrumentation.LoadControlInstrumentationBean;

@RunWith(MockitoJUnitRunner.class)
public class LoadControlInstrumentationBeanTest {

    @InjectMocks
    LoadControlInstrumentationBean loadControlInstrumentationBean;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateCurrentCountJmx() {
        loadControlInstrumentationBean.updateCurrentCountJmx("test-counter", 5L);
    }

    @Test
    public void testSetCurrentThresholdCounts() {
        loadControlInstrumentationBean.setCurrentThresholdCounts("5L");
    }
    
    @Test
    public void test_getCurrentCounts() {
    	 final String response = loadControlInstrumentationBean.getCurrentCounts();
    	 assertNotNull(response);
    }

    @Test
    public void test_getCurrentThresholdCounts() {
    	 final String response = loadControlInstrumentationBean.getCurrentThresholdCounts();
        assertNotNull(response);
    }
}
