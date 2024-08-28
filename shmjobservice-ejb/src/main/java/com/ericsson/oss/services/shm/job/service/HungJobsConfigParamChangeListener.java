/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * This class is used to listen configuration parameter for the maximum time limit for job execution
 * 
 * @author xghamdg
 * 
 */
@ApplicationScoped
public class HungJobsConfigParamChangeListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Configured(propertyName = "MAX_TIME_LIMIT_FOR_JOB_EXECUTION_IN_HOURS")
    private int maxTimeLimitForJobExecutionInHours;

    @Inject
    @Configured(propertyName = "MAX_TIME_LIMIT_FOR_AXE_UPGRADE_JOB_EXECUTION_IN_HOURS")
    private int maxTimeLimitForAxeUpgradeJobExecutionInHours;

    @Inject
    @Configured(propertyName = "MAX_TIME_FOR_HOUSEKEEPING_OF_STAGED_ACTIVITIES_IN_HOURS")
    private int maxTimeForHousekeepingOfStagedActivitesInHours;

    /**
     * Listener for RUNNING_TIME_IN_DAYS_FOR_HANGING_JOBS attribute value
     * 
     * @param runnigTimeInDaysForHangingJobs
     */
    void listenCountOfmaxTimeLimitForJobExecutionInHoursAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MAX_TIME_LIMIT_FOR_JOB_EXECUTION_IN_HOURS") final int maxTimeLimitForJobExecutionInHours) {
        this.maxTimeLimitForJobExecutionInHours = maxTimeLimitForJobExecutionInHours;
        logger.info("maximum time limit for job execution in hours {}", maxTimeLimitForJobExecutionInHours);
    }

    public int getMaxTimeLimitForJobExecutionInHours() {
        return maxTimeLimitForJobExecutionInHours;
    }

    /**
     * Listener for MAX_TIME_LIMIT_FOR_AXE_JOB_EXECUTION_IN_HOURS attribute value
     * 
     * @param runnigTimeInDaysForHandingJobs
     */
    void listenCountOfmaxTimeLimitForAxeUpgradeJobExecutionInHoursAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MAX_TIME_LIMIT_FOR_AXE_UPGRADE_JOB_EXECUTION_IN_HOURS") final int maxTimeLimitForAxeUpgradeJobExecutionInHours) {
        this.maxTimeLimitForAxeUpgradeJobExecutionInHours = maxTimeLimitForAxeUpgradeJobExecutionInHours;
        logger.info("maximum time limit for job execution in hours {}", maxTimeLimitForAxeUpgradeJobExecutionInHours);
    }

    public int getMaxTimeLimitForAxeUpgradeJobExecutionInHours() {
        return maxTimeLimitForAxeUpgradeJobExecutionInHours;
    }

    /**
     * Listener for MAX_TIME_FOR_HOUSEKEEPING_OF_STAGED_ACTIVITIES_IN_HOURS attribute value
     * 
     * @param runnigTimeInDaysForHandingJobs
     */
    void listenCountOfmaxTimeLimitForStagedActivitiesInHoursAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MAX_TIME_FOR_HOUSEKEEPING_OF_STAGED_ACTIVITIES_IN_HOURS") final int maxTimeLimitForstagedActivitiesInHours) {
        this.maxTimeForHousekeepingOfStagedActivitesInHours = maxTimeLimitForstagedActivitiesInHours;
        logger.info("maximum time limit for Staged Activites in hours {}", maxTimeLimitForstagedActivitiesInHours);
    }

    public int getMaxTimeLimitForStagedActivitiesInHours() {
        return maxTimeForHousekeepingOfStagedActivitesInHours;
    }

}
