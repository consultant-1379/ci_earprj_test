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

import javax.inject.Inject;

/**
 * This class is used to fetch the TIME (Hours and Minutes) of the Daily Schedule for HouseKeeping of Jobs.
 * 
 * @author xsrakon
 * 
 */
public class HouseKeepingDailyScheduleParamProvider {

    // This class is added to remove cyclic dependency between JobsHouseKeepingScheduler class and HouseKeepingScheduleParameterChangeListener class
    @Inject
    HouseKeepingScheduleParameterChangeListener houseKeepingScheduleParameterChangeListener;

    public int getHouseKeepingScheduledTimeInHoursConfigParam() {
        return houseKeepingScheduleParameterChangeListener.getHoursOfDailyScheduleTimeForJobsHouseKeeping();
    }

    public int getHouseKeepingScheduledTimeInMinutesConfigParam() {
        return houseKeepingScheduleParameterChangeListener.getMinutesOfDailyScheduleTimeForJobsHouseKeeping();
    }
}
