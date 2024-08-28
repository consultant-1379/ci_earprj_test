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
package com.ericsson.oss.services.shm.es.instrumentation.impl;

import static org.mockito.Mockito.doNothing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NeJobInstrumentationServiceTest {

    private static final String PLATFORM_TYPE = "CPP";

    private static final String JOB_TYPE = "CPP";

    @Mock
    private NeJobInstrumentaionBean neJobInstrumentaionBean;

    @InjectMocks
    NeJobInstrumentaionService neJobInstrumentaionService;

    @Test
    public void testPreStart() {
        doNothing().when(neJobInstrumentaionBean).actvityStart(PLATFORM_TYPE, JOB_TYPE);
        neJobInstrumentaionService.preStart(PLATFORM_TYPE, JOB_TYPE);
    }

    @Test
    public void testPostFinish() {
        doNothing().when(neJobInstrumentaionBean).activityEnd(PLATFORM_TYPE, JOB_TYPE);
        neJobInstrumentaionService.postFinish(PLATFORM_TYPE, JOB_TYPE);
    }
}
