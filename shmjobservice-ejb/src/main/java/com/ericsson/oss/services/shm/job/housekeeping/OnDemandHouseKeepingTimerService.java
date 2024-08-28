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

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.job.service.HungJobsMarkerService;

/**
 * 
 * This class is used to create OnDemand HouseKeeping Timer.
 * 
 * @author xsrakon
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OnDemandHouseKeepingTimerService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private TimerService timerService;

    @Inject
    private JobsHouseKeepingService jobsHouseKeepingService;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private HungJobsMarkerService hungJobsMarkerService;

    public void initTimer(final int onDemandScheduleTimeForHouseKeeping) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createSingleActionTimer(getScheduleTimeInMilliSeconds(onDemandScheduleTimeForHouseKeeping), timerConfig);
    }

    @Timeout
    public void executeHouseKeepingOfJobs(final Timer timer) {
        if (membershipListenerInterface.isMaster()) {
            logger.info("Timeout happened in OnDemandHouseKeepingTimerService {}", timer.isPersistent());
            jobsHouseKeepingService.triggerHouseKeepingOfJobs();

            //Triggering housekeeping of ShmStagedActivity PO's
            hungJobsMarkerService.deleteStagedActivityPOs();
        }
    }

    public Long getScheduleTimeInMilliSeconds(final int onDemandScheduleTimeForHouseKeeping) {
        return (long) (onDemandScheduleTimeForHouseKeeping * 60 * 1000);
    }
}
