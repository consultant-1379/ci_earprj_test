/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.activity.timeout.models;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.job.cpp.activity.NodeRestartActivityConstants;

/**
 * This class is used to listen and update the neType specific timeouts of NodeRestart Job
 *
 * @author zjansag
 *
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.NODERESTART)
public class NodeRestartJobActivityTimeouts implements ActivityTimeoutsProvider{

    private final Logger loggerLogger = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> NODERESTARTJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<>();


    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    private SystemRecorder systemRecorder;

    /*********************** CPP NodeRestart Job ManualRestart Activity Timeout **************************/
    @Inject
    @Configured(propertyName = "RNC_NODERESTARTJOB_MANUALRESTART_ACTIVITY_TIME_OUT")
    private int rncNodeRestartManualRestartActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "cppNodeRestartMaxWaitTime_ms")
    private int cppNodeRestartMaxWaitTime;

    public void listenForRncNodeRestartManualRestartActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_NODERESTARTJOB_MANUALRESTART_ACTIVITY_TIME_OUT") final int rncManualRestartActivitytimeoutInterval) {
        this.rncNodeRestartManualRestartActivitytimeoutInterval = rncManualRestartActivitytimeoutInterval;
        NODERESTARTJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + JobTypeEnum.NODERESTART + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + NodeRestartActivityConstants.MANUALRESTART, (rncManualRestartActivitytimeoutInterval));
        loggerLogger.info("Changed timeout value for RNC NodeRestart Job ManualRestart activity is : {} minutes", rncManualRestartActivitytimeoutInterval);
    }

    public void listenForCppNodeRestartManualRestartActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "cppNodeRestartMaxWaitTime_ms") final int cppManualRestartActivitytimeoutInterval) {
        this.cppNodeRestartMaxWaitTime = cppManualRestartActivitytimeoutInterval;
        NODERESTARTJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + JobTypeEnum.NODERESTART + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + NodeRestartActivityConstants.MANUALRESTART, cppManualRestartActivitytimeoutInterval);
        loggerLogger.info("Changed timeout value for CPP NodeRestart Job ManualRestart activity is : {} minutes", cppManualRestartActivitytimeoutInterval);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + jobType.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + jobType.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + activityName.toLowerCase();
        if (NODERESTARTJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return NODERESTARTJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (NODERESTARTJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return NODERESTARTJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityTimeoutBasedOnPlatform(platform);
    }

    /*
     *
     *
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTimeAsInteger(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + jobType.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + activityName.toLowerCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        final String platformkey = platform.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + jobType.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + activityName.toLowerCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING;
        if (NODERESTARTJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return NODERESTARTJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (NODERESTARTJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return NODERESTARTJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(platform);
    }

    @Override
    public String getActivityTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer activityTimeout = getActivityTimeoutAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(activityTimeout);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTime(java.lang.String, com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum,
     * com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum, java.lang.String)
     */
    @Override
    public String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer activityTimeout = getActivityPollWaitTimeAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(activityTimeout);
    }

    private String convertToIsoFormat(final int timeout) {
        return "PT" + timeout + "M";
    }

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        NODERESTARTJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + JobTypeEnum.NODERESTART + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + NodeRestartActivityConstants.MANUALRESTART, rncNodeRestartManualRestartActivitytimeoutInterval);
        NODERESTARTJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + JobTypeEnum.NODERESTART + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + NodeRestartActivityConstants.MANUALRESTART, cppNodeRestartMaxWaitTime);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

}
