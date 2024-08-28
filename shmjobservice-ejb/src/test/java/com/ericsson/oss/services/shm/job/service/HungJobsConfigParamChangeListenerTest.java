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
package com.ericsson.oss.services.shm.job.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HungJobsConfigParamChangeListenerTest {

    @InjectMocks
    private HungJobsConfigParamChangeListener hungJobsConfigParamChangeListener;

    @Test
    public void testListenCountOfmaxTimeLimitForJobExecutionInHoursAttribute() {
        final int maxTimeLimitForJobExecutionInHours = 48;
        hungJobsConfigParamChangeListener.listenCountOfmaxTimeLimitForJobExecutionInHoursAttribute(maxTimeLimitForJobExecutionInHours);
        assertEquals(maxTimeLimitForJobExecutionInHours, hungJobsConfigParamChangeListener.getMaxTimeLimitForJobExecutionInHours());
    }
}
