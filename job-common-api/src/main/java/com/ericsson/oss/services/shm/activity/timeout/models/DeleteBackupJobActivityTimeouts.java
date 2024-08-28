/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
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

@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.DELETEBACKUP)
public class DeleteBackupJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> DELETEBACKUPJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    public static final String DELETE_BACKUP = "deletebackup";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    private SystemRecorder systemRecorder;

    /******************** MINI-LINK DELETEBACKUP TIMEOUTS **************************************************/
    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "AXE_DELETEBACKUP_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES")
    private int axeDeleteBackupJobDeleteBackupActivitytimeoutInterval;
    
    public void listenForMiniLinkIndoorDeleteBackupActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_TIME_OUT") final int miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval) {
        this.miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval = miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval;
        DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP + DELIMETER_UNDERSCORE + DELETE_BACKUP,
                miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval);
        LOGGER.info("Change in MINI-LINK-Indoor backup job deletebackup timeout value : {} minutes", miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval);
    }

    public void listenForAxeIndoorDeleteBackupActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "AXE_DELETEBACKUP_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES") final int axeDeleteBackupJobDeleteBackupActivitytimeoutInterval) {
        this.axeDeleteBackupJobDeleteBackupActivitytimeoutInterval = axeDeleteBackupJobDeleteBackupActivitytimeoutInterval;
        DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP + DELIMETER_UNDERSCORE + DELETE_BACKUP,
                axeDeleteBackupJobDeleteBackupActivitytimeoutInterval);
    }
    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        if (DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityTimeoutBasedOnPlatform(platform);
    }

    @Override
    public String getActivityTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer activityTimeout = getActivityTimeoutAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(activityTimeout);
    }

    private String convertToIsoFormat(final int timeout) {
        return "PT" + timeout + "M";
    }

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP + DELIMETER_UNDERSCORE + DELETE_BACKUP,
                miniLinkIndoorDeleteBackupJobDeleteBackupActivitytimeoutInterval);
        DELETEBACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP + DELIMETER_UNDERSCORE + DELETE_BACKUP,
                axeDeleteBackupJobDeleteBackupActivitytimeoutInterval);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();

        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTime(java.lang.String, com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum,
     * com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum, java.lang.String)
     */
    @Override
    public String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTimeAsInteger(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        // TODO Auto-generated method stub
        return null;
    }
}
