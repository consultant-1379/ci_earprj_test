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

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ActvityInstrumentationServiceTest {

    private static final String PLATEFORM_TYPE = "CPP";

    private static final String JOB_TYPE = "CPP";

    private static final String NAME = "Job01";

    @Mock
    ActivityInstrumentationBean activityInstrumentationBean;

    @InjectMocks
    ActvityInstrumentationService actvityInstrumentationService;

    @Test
    public void testPreStart() {
        doNothing().when(activityInstrumentationBean).actvityStart(PLATEFORM_TYPE, JOB_TYPE, NAME);
        actvityInstrumentationService.preStart(PLATEFORM_TYPE, JOB_TYPE, NAME);
    }
    
    @Test
    public void testPostFinish() {
        doNothing().when(activityInstrumentationBean).activityEnd(PLATEFORM_TYPE, JOB_TYPE, NAME);
        actvityInstrumentationService.postFinish(PLATEFORM_TYPE, JOB_TYPE, NAME);
    }
}
