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
public class MainJobInstrumentationServiceTest {

    

    private static final String JOB_TYPE = "CPP";

    @Mock
    private MainJobInstrumentationBean mainJobInstrumentationBean;

    @InjectMocks
    MainJobInstrumentationService mainJobInstrumentationService;

    @Test
    public void testPreStart() {
        doNothing().when(mainJobInstrumentationBean).actvityStart( JOB_TYPE);
        mainJobInstrumentationService.preStart( JOB_TYPE);
    }

    @Test
    public void testPostFinish() {
        doNothing().when(mainJobInstrumentationBean).activityEnd( JOB_TYPE);
        mainJobInstrumentationService.postFinish( JOB_TYPE);
    }
}