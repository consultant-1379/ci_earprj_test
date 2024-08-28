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
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Mockito.*;

import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(PowerMockRunner.class)
public class CancelCompleteTimerTest {

    @InjectMocks
    CancelCompleteTimer objectUnderTest;

    @Mock
    TimerService timerService;

    @Mock
    Timer timerMock;

    @Mock
    ActivityUtils activityUtils;

    @Mock
    JobEnvironment jobEnvironment;

    long activityJobId = 1234;

    @Test
    public void testStartTimer() {
        final JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, "verify", JobTypeEnum.RESTORE, PlatformTypeEnum.CPP);
        objectUnderTest.startTimer(jobActivityInfo);
    }

    @Test
    public void testHandleTimeout() throws Exception {
        final JobActivityInfo jobInfo = new JobActivityInfo(activityJobId, "verify", JobTypeEnum.RESTORE, PlatformTypeEnum.CPP);

        when(timerMock.getInfo()).thenReturn(jobInfo);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        Whitebox.invokeMethod(objectUnderTest, "handleTimeout", timerMock);

        verify(activityUtils, times(1)).sendCancelMOActionDoneToWFS(Matchers.anyString());
    }
}
