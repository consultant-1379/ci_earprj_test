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
package com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsFactory;
import com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider;
import com.ericsson.oss.services.shm.activity.timeout.models.NodeHealthCheckJobActivityTimeouts;
import com.ericsson.oss.services.shm.activity.timeout.models.PrecheckConfigurationProvider;
import com.ericsson.oss.services.shm.activity.timeout.models.ShmJobDefaultActivityTimeouts;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * This class is used to fetch the neType specific timeout values for each activity
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
@Profiled
@Traceable
public class ActivityTimeoutsService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ActivityTimeoutsService.class);

    @Inject
    private ActivityTimeoutsFactory activityTimeoutsFactory;

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    @JobTypeAnnotation(jobType = JobType.NODE_HEALTH_CHECK)
    private NodeHealthCheckJobActivityTimeouts nodeHealthCheckJobActivityTimeouts;

    /**
     * This method is used to get the activity timeout value which will be used by work flows
     * 
     * @return String
     * 
     */
    public String getActivityTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        LOGGER.info("Inside getActivityTimeout() method with jobType : {} and neType : {}", jobTypeEnum, neType);
        String activityTimeout = null;
        final ActivityTimeoutsProvider activityTimeoutsProvider = activityTimeoutsFactory.getActivityTimeoutsProvider(JobType.getJobType(jobTypeEnum.toString()));
        if (activityTimeoutsProvider != null) {
            activityTimeout = activityTimeoutsProvider.getActivityTimeout(neType, platformTypeEnum, jobTypeEnum, activityName);
        } else {
            activityTimeout = shmJobDefaultActivityTimeouts.getActivityTimeout(neType, platformTypeEnum, jobTypeEnum, activityName);
        }
        LOGGER.info("Activity Timeout for {} _ {} is : {}", neType, activityName, activityTimeout);
        return activityTimeout;
    }

    /**
     * This method is used to get the activity timeout value which will be used to display in the job logs
     * 
     * @return Integer
     * 
     */
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        LOGGER.info("Inside getActivityTimeoutAsInteger() method with jobType : {} and neType : {}", jobType, neType);
        Integer activityTimeout;
        final ActivityTimeoutsProvider activityTimeoutsProvider = activityTimeoutsFactory.getActivityTimeoutsProvider(JobType.getJobType(jobType));
        if (activityTimeoutsProvider != null) {
            activityTimeout = activityTimeoutsProvider.getActivityTimeoutAsInteger(neType, platform, jobType, activityName);
        } else {
            activityTimeout = shmJobDefaultActivityTimeouts.getActivityTimeoutAsInteger(neType, platform, jobType, activityName);
        }
        LOGGER.info("Activity Timeout for {} _ {} is : {}", neType, activityName, activityTimeout);
        return activityTimeout;
    }

    /**
     * This method is used to get the precheck timeout value which will be used by work flows
     * 
     * @return String
     * 
     */
    public String getPrecheckTimeout() {
        return shmJobDefaultActivityTimeouts.getPrecheckTimeout();
    }

    /**
     * This method is used to get the precheck timeout value which will be used to display in the job logs
     * 
     * @return Integer
     * 
     */
    public Integer getPrecheckTimeoutAsInteger() {
        return shmJobDefaultActivityTimeouts.getPrecheckTimeoutAsInteger();
    }

    /**
     * This method is used to get the timeout for handleTimeout value which will be used by work flows
     * 
     * @return String
     * 
     */
    public String getTimeoutForHandleTimeout() {
        return shmJobDefaultActivityTimeouts.getTimeoutForHandleTimeout();
    }

    /**
     * This method is used to get the timeout for handleTimeout value which will be used to display in the job logs
     * 
     * @return Integer
     */
    public Integer getTimeoutForHandleTimeoutAsInteger() {
        return shmJobDefaultActivityTimeouts.getTimeoutForHandleTimeoutAsInteger();
    }

    /**
     * This method is used to get the wait interval before repeating precheck which will be used by workflows.
     * 
     * @return String
     * 
     */
    public String getRepeatPrecheckWaitInterval(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        String repeatPrecheckWaitInterval = null;
        final PrecheckConfigurationProvider repeatPrecheckConfigurationProvider = activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.getJobType(jobTypeEnum.toString()));
        if (repeatPrecheckConfigurationProvider != null) {
            repeatPrecheckWaitInterval = repeatPrecheckConfigurationProvider.getRepeatPrecheckWaitInterval(neType, platformTypeEnum, jobTypeEnum, activityName);
        } else {
            repeatPrecheckWaitInterval = shmJobDefaultActivityTimeouts.getRepeatPrecheckWaitInterval();
        }
        LOGGER.info("Repeat Precheck Wait Interval for jobType {} _ {} _ {} is : {}", jobTypeEnum, neType, activityName, repeatPrecheckWaitInterval);
        return repeatPrecheckWaitInterval;
    }

    /**
     * This method is used to get the wait interval before repeating precheck which will be used to display in the job logs.
     * 
     * @return Integer
     * 
     */
    public Integer getRepeatPrecheckWaitIntervalAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        Integer repeatPrecheckWaitInterval;
        final PrecheckConfigurationProvider repeatPrecheckConfigurationProvider = activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.getJobType(jobType));
        if (repeatPrecheckConfigurationProvider != null) {
            repeatPrecheckWaitInterval = repeatPrecheckConfigurationProvider.getPrecheckWaitInterval(neType, platform, jobType, activityName);
        } else {
            repeatPrecheckWaitInterval = shmJobDefaultActivityTimeouts.getRepeatPrecheckWaitIntervalAsInteger();
        }
        LOGGER.info("Repeat Precheck Wait Interval for  jobType {} _ {} _ {} is : {}", jobType, neType, activityName, repeatPrecheckWaitInterval);
        return repeatPrecheckWaitInterval;
    }

    /**
     * This method is used to get the retry attempt for repeating precheck which will be used to display in the job logs.
     * 
     * @return Integer
     * 
     */
    public int getRepeatPrecheckRetryAttempt(final String neType, final String platform, final String jobType, final String activityName) {
        Integer repeatPrecheckRetryAttempt;
        final PrecheckConfigurationProvider repeatPrecheckConfigurationProvider = activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.getJobType(jobType));
        if (repeatPrecheckConfigurationProvider != null) {
            repeatPrecheckRetryAttempt = repeatPrecheckConfigurationProvider.getPrecheckRetryAttempt(neType, platform, jobType, activityName);
        } else {
            repeatPrecheckRetryAttempt = shmJobDefaultActivityTimeouts.getRepeatPrecheckRetryAttempt();
        }
        LOGGER.info("Repeat Precheck Retry Attempt for jobType {} _ {} _ {} is : {}", jobType, neType, activityName, repeatPrecheckRetryAttempt);
        return repeatPrecheckRetryAttempt;
    }

    public String getBestTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        String bestTimeout = "";
        final ActivityTimeoutsProvider activityTimeoutsProvider = activityTimeoutsFactory.getActivityTimeoutsProvider(JobType.getJobType(jobTypeEnum.toString()));
        if (activityTimeoutsProvider != null) {
            bestTimeout = activityTimeoutsProvider.getActivityPollWaitTime(neType, platformTypeEnum, jobTypeEnum, activityName);
        } else {
            bestTimeout = shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatform(platformTypeEnum.toString());
        }
        LOGGER.info("BestTimeout for {} _ {} is : {}", neType, activityName, bestTimeout);
        return bestTimeout;
    }

    public Integer getBestTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        Integer bestTimeout;
        final ActivityTimeoutsProvider activityTimeoutsProvider = activityTimeoutsFactory.getActivityTimeoutsProvider(JobType.getJobType(jobType));
        if (activityTimeoutsProvider != null) {
            bestTimeout = activityTimeoutsProvider.getActivityPollWaitTimeAsInteger(neType, platform, jobType, activityName);
        } else {
            bestTimeout = shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(platform);
        }
        LOGGER.info("BestTimeout for {} _ {} as Integer is : {}", neType, activityName, bestTimeout);
        return bestTimeout;
    }

    public String getNodeSyncCheckWaitIntervalOrTimeOut(final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName, final String type) {
        return nodeHealthCheckJobActivityTimeouts.getNodeSyncCheckWaitIntervalOrTimeOut(platformTypeEnum,jobTypeEnum,activityName,type);
    }
}
