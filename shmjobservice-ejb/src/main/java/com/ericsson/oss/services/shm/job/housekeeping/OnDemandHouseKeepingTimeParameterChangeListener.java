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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * 
 * This class is used to listen and update the OnDemand housekeeping timeout in minutes.
 * 
 * @author xsrakon
 * 
 */
@ApplicationScoped
public class OnDemandHouseKeepingTimeParameterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Configured(propertyName = "ONDEMAND_JOB_HOUSEKEEPING_SCHEDULE_INTERVAL_IN_MINUTES")
    private int onDemandScheduleTimeForHouseKeeping;

    @Inject
    private OnDemandHouseKeepingTimerService onDemandHouseKeepingTimerService;

    /**
     * Listener for ONDEMAND_JOB_HOUSEKEEPING_SCHEDULE_INTERVAL_IN_MINUTES attribute value
     * 
     * @param onDemandScheduleTimeForHouseKeeping
     */
    void listenForOnDemandHouseKeepingScheduleTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ONDEMAND_JOB_HOUSEKEEPING_SCHEDULE_INTERVAL_IN_MINUTES") final int onDemandScheduleTimeForHouseKeeping) {
        this.onDemandScheduleTimeForHouseKeeping = onDemandScheduleTimeForHouseKeeping;
        logger.info("On Demand Schedule Time For House Keeping {}", onDemandScheduleTimeForHouseKeeping);
        onDemandHouseKeepingTimerService.initTimer(onDemandScheduleTimeForHouseKeeping);
    }

    public int getOnDemandScheduleTimeForHouseKeepingOfJobs() {
        return onDemandScheduleTimeForHouseKeeping;
    }

}
