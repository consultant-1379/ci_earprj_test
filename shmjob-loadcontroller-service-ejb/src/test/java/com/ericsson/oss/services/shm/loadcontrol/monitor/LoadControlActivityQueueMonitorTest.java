/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.loadcontrol.schedule.TimerContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Date.class, TimerContext.class, LoadControlActivityQueueMonitor.class })
public class LoadControlActivityQueueMonitorTest {

    @InjectMocks
    private LoadControlActivityQueueMonitor obejctUnderTest;

    @Mock
    private ProcessQueue processQueue;

    @Mock
    private TimerContext timerContext;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadControlActivityQueueMonitorTest.class);

    @Test
    public void testStartActivityQueueMonitors() {
        try {
            PowerMockito.whenNew(TimerContext.class).withNoArguments().thenReturn(timerContext);
            obejctUnderTest.startActivityQueueMonitors();
            verify(processQueue, times(41)).persistQueueMessagesIntoDB();
        } catch (Exception e) {
            LOGGER.error("Exception occured in testStartTimer {}", e);
        }
    }
}
