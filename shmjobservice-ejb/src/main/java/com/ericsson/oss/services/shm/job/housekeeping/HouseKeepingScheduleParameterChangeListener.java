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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * 
 * This class is used to listen and update the daily schedule time for housekeeping.
 * 
 * @author xsrakon
 * 
 */
@ApplicationScoped
public class HouseKeepingScheduleParameterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Configured(propertyName = "DAILY_SCHEDULE_TIME_IN_HOURS_FOR_JOB_HOUSEKEEPING")
    private int hoursOfDailyScheduleTimeForHouseKeepingOfJobs;

    @Inject
    @Configured(propertyName = "DAILY_SCHEDULE_TIME_IN_MINUTES_FOR_JOB_HOUSEKEEPING")
    private int minutesOfDailyScheduleTimeForHouseKeepingOfJobs;

    @Inject
    JobsHouseKeepingScheduler jobsHouseKeepingScheduler;

    /**
     * Listener for DAILY_SCHEDULE_TIME_IN_HOURS_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param hoursOfDailyScheduleTimeForHouseKeepingOfJobs
     */
    void listenForHouseKeepingScheduleTimeHoursAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DAILY_SCHEDULE_TIME_IN_HOURS_FOR_JOB_HOUSEKEEPING") final int hoursOfDailyScheduleTimeForHouseKeepingOfJobs) {
        this.hoursOfDailyScheduleTimeForHouseKeepingOfJobs = hoursOfDailyScheduleTimeForHouseKeepingOfJobs;
        logger.info("Daily Schedule Time For HouseKeeping of Jobs is {}", hoursOfDailyScheduleTimeForHouseKeepingOfJobs);
        jobsHouseKeepingScheduler.cancelTimer();
    }

    public int getHoursOfDailyScheduleTimeForJobsHouseKeeping() {
        return hoursOfDailyScheduleTimeForHouseKeepingOfJobs;
    }

    /**
     * Listener for DAILY_SCHEDULE_TIME_IN_MINUTES_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param dailyScheduleTimeForHouseKeepingOfJobs
     */
    void listenForHouseKeepingScheduleTimeMinutesAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DAILY_SCHEDULE_TIME_IN_MINUTES_FOR_JOB_HOUSEKEEPING") final int minutesOfDailyScheduleTimeForHouseKeepingOfJobs) {
        this.minutesOfDailyScheduleTimeForHouseKeepingOfJobs = minutesOfDailyScheduleTimeForHouseKeepingOfJobs;
        logger.info("Daily Schedule Time For HouseKeeping of Jobs is {}", minutesOfDailyScheduleTimeForHouseKeepingOfJobs);
        jobsHouseKeepingScheduler.cancelTimer();
    }

    public int getMinutesOfDailyScheduleTimeForJobsHouseKeeping() {
        return minutesOfDailyScheduleTimeForHouseKeepingOfJobs;
    }
}
