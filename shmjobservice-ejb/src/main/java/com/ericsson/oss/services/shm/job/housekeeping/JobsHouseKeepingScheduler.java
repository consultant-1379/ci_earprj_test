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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.job.service.HungJobsMarkerService;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobUtilConstants;

/**
 * 
 * This class is used to create Daily Schedule HouseKeeping Timer.
 * 
 * @author xsrakon
 * 
 */

@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobsHouseKeepingScheduler {

    @Inject
    private HouseKeepingDailyScheduleParamProvider houseKeepingDailyScheduleParameter;

    @Resource
    private TimerService timerService;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private JobsHouseKeepingService jobsHouseKeepingService;

    @Inject
    private HungJobsMarkerService hungJobsMarkerService;

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHouseKeepingScheduler.class);

    @PostConstruct
    public void initTimer() {
        final long postConstructStarted = System.currentTimeMillis();
        LOGGER.info("Intiating DailySchedule Timer for HouseKeeping");
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(SHMJobUtilConstants.HOUSEKEEPING_TIMER);
        timerService.createCalendarTimer(
                new ScheduleExpression().hour(houseKeepingDailyScheduleParameter.getHouseKeepingScheduledTimeInHoursConfigParam()).minute(
                        houseKeepingDailyScheduleParameter.getHouseKeepingScheduledTimeInMinutesConfigParam()), timerConfig);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @Timeout
    public void executeHouseKeepingOfJobs(final Timer timer) {
        if (membershipListenerInterface.isMaster()) {
            LOGGER.info("Triggering DailySchedule HouseKeeping after timeout of {} occured ", timer.getInfo());
            jobsHouseKeepingService.triggerHouseKeepingOfJobs();
            //Triggering DailySchedule hung job update to System cancelled
            hungJobsMarkerService.updateHungJobsToSystemCancelled();
            //Triggering housekeeping of ShmStagedActivity PO's
            hungJobsMarkerService.deleteStagedActivityPOs();
        }
    }

    public void cancelTimer() {
        for (final Timer timer : timerService.getTimers()) {
            if (SHMJobUtilConstants.HOUSEKEEPING_TIMER.equals(timer.getInfo())) {
                timer.cancel();
                LOGGER.info("Cancelled HouseKeeping Timer");
            }
        }
        initTimer();
    }
}
