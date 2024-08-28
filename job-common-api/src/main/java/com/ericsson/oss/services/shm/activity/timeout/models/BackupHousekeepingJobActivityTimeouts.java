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
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * Class which provides activity timeout values for Backup housekeeping job.
 *
 * @author xeswpot
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.BACKUP_HOUSEKEEPING)
public class BackupHousekeepingJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(BackupHousekeepingJobActivityTimeouts.class);
    private static final Map<String, Integer> BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    private static final String CLEAN_CV = "cleancv";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPHOUSEKEEPINGJOB_CLEANCV_ACTIVITY_TIME_OUT")
    private int cppBackuphousekeepingCleancvActivityTimeoutValue;

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP_HOUSEKEEPING + DELIMETER_UNDERSCORE + CLEAN_CV,
                cppBackuphousekeepingCleancvActivityTimeoutValue);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    public void listenForCppBackupHousekeepingCleancvActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPHOUSEKEEPINGJOB_CLEANCV_ACTIVITY_TIME_OUT") final int cppBackuphousekeepingCleancvActivityTimeoutValue) {
        this.cppBackuphousekeepingCleancvActivityTimeoutValue = cppBackuphousekeepingCleancvActivityTimeoutValue;
        BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP_HOUSEKEEPING + DELIMETER_UNDERSCORE + CLEAN_CV,
                cppBackuphousekeepingCleancvActivityTimeoutValue);
        LOGGER.info("Changed timeout value for CPP backup housekeeping Job cleancv activity is : {} minutes", cppBackuphousekeepingCleancvActivityTimeoutValue);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {

        final String neKey = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();

        if (BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS.containsKey(neKey)) {
            return BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS.get(neKey);
        } else if (BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return BACKUPHOUSEKEEPING_JOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityTimeoutBasedOnPlatform(platform);

    }

    @Override
    public String getActivityTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer timeout = getActivityTimeoutAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(timeout);
    }

    private String convertToIsoFormat(final int timeout) {
        return "PT" + timeout + "M";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTime(java.lang.String, com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum,
     * com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum, java.lang.String)
     */
    @Override
    public String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        // Implement when Polling Times are cofigured for this job
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTimeAsInteger(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        // Implement when Polling Times are cofigured for this job
        return null;
    }

}
