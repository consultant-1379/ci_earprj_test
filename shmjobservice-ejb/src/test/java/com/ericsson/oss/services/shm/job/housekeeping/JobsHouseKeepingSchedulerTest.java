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
package com.ericsson.oss.services.shm.job.housekeeping;

import java.util.Arrays;
import java.util.Collection;

import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;


import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.job.service.HungJobsMarkerService;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobUtilConstants;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ JobsHouseKeepingScheduler.class, TimerConfig.class, ScheduleExpression.class })
public class JobsHouseKeepingSchedulerTest {

    @InjectMocks
    private JobsHouseKeepingScheduler jobsHouseKeepingScheduler;

    @Mock
    private TimerService timerService;

    @Mock
    private Timer mockTimer;

    @Mock
    Collection<Timer> Timer;

    @Mock
    MembershipListenerInterface membershipListenerInterface;

    @Mock
    private JobsHouseKeepingService jobsHouseKeepingService;

    @Mock
    private HouseKeepingDailyScheduleParamProvider houseKeepingDailyScheduleParameter;

    @Mock
    private HungJobsMarkerService hungJobsMarkerService;

    @Mock
    private SystemRecorder systemRecorder;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStartMonitors() throws Exception {
        jobsHouseKeepingScheduler.initTimer();
        Mockito.verify(timerService, Mockito.atLeastOnce()).createCalendarTimer(Mockito.any(ScheduleExpression.class), Mockito.any(TimerConfig.class));

    }

    @Test
    public void testCancelTimer() {
        Mockito.when(mockTimer.getInfo()).thenReturn(SHMJobUtilConstants.HOUSEKEEPING_TIMER);
        Mockito.when(timerService.getTimers()).thenReturn(Arrays.asList(mockTimer));
        jobsHouseKeepingScheduler.cancelTimer();
        Mockito.verify(mockTimer, Mockito.atMost(1)).cancel();
    }

    @Test
    public void testexecuteHouseKeepingOfJobs() {
        Mockito.when(membershipListenerInterface.isMaster()).thenReturn(true);
        Mockito.when(timerService.getTimers()).thenReturn(Arrays.asList(mockTimer));
        jobsHouseKeepingScheduler.executeHouseKeepingOfJobs(mockTimer);
        Mockito.verify(jobsHouseKeepingService, Mockito.atMost(1)).triggerHouseKeepingOfJobs();
        Mockito.verify(hungJobsMarkerService, Mockito.atMost(1)).updateHungJobsToSystemCancelled();
        Mockito.verify(hungJobsMarkerService, Mockito.atMost(1)).deleteStagedActivityPOs();
    }

}
