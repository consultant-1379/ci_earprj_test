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
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.DELIMITER_UNDERSCORE;

/**
 * This class is used to listen and update the License refresh Job activity timeout values.
 *
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.LICENSE_REFRESH)
public class LicenseRefreshJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<>();

    private static final String REFRESH = "refresh";
    private static final String REQUEST = "request";
    private static final String INSTALL = "install";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REFRESH_ACTIVITY_TIME_OUT")
    private int ecimLicenseRefreshJobRefreshActivityTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REQUEST_ACTIVITY_TIME_OUT")
    private int ecimLicenseRefreshJobRequestActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_INSTALL_ACTIVITY_TIME_OUT")
    private int ecimLicenseRefreshJobInstallActivitytimeoutInterval;

    @Inject
    private SystemRecorder systemRecorder;

    public void listenForEcimLicenseRefreshJobRefreshActivityTimeoutInterval(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REFRESH_ACTIVITY_TIME_OUT") final int ecimLicenseRefreshJobRefreshActivityTimeoutInterval) {
        this.ecimLicenseRefreshJobRefreshActivityTimeoutInterval = ecimLicenseRefreshJobRefreshActivityTimeoutInterval;
        LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMITER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH + DELIMITER_UNDERSCORE + REFRESH,
                ecimLicenseRefreshJobRefreshActivityTimeoutInterval);
        logger.info("Changed timeout value for ECIM License Refresh Job Refresh activity is : {} minutes", ecimLicenseRefreshJobRefreshActivityTimeoutInterval);
    }

    public void listenForEcimLicenseRefreshJobRequestActivitytimeoutInterval(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REQUEST_ACTIVITY_TIME_OUT") final int ecimLicenseRefreshJobRequestActivitytimeoutInterval) {
        this.ecimLicenseRefreshJobRequestActivitytimeoutInterval = ecimLicenseRefreshJobRequestActivitytimeoutInterval;
        LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMITER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH + DELIMITER_UNDERSCORE + REQUEST,
                ecimLicenseRefreshJobRequestActivitytimeoutInterval);
        logger.info("Changed time out value for ECIM License Refresh Job Request activity is: {} minutes", ecimLicenseRefreshJobRequestActivitytimeoutInterval);
    }

    public void listenForEcimLicenseRefreshJobInstallActivitytimeoutInterval(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_INSTALL_ACTIVITY_TIME_OUT") final int ecimLicenseRefreshJobInstallActivitytimeoutInterval) {
        this.ecimLicenseRefreshJobInstallActivitytimeoutInterval = ecimLicenseRefreshJobInstallActivitytimeoutInterval;
        LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMITER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH + DELIMITER_UNDERSCORE + INSTALL,
                ecimLicenseRefreshJobInstallActivitytimeoutInterval);
        logger.info("Changed time out value for ECIM License Refresh Job install timeout value : {} minutes", ecimLicenseRefreshJobInstallActivitytimeoutInterval);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMITER_UNDERSCORE + jobType.toUpperCase() + DELIMITER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMITER_UNDERSCORE + jobType.toUpperCase() + DELIMITER_UNDERSCORE + activityName.toLowerCase();
        if (LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.get(platformkey);
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
        LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMITER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH + DELIMITER_UNDERSCORE + REFRESH,
                ecimLicenseRefreshJobRefreshActivityTimeoutInterval);
        LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMITER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH + DELIMITER_UNDERSCORE + REQUEST,
                ecimLicenseRefreshJobRequestActivitytimeoutInterval);
        LICENSE_REFRESH_JOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMITER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH + DELIMITER_UNDERSCORE + INSTALL,
                ecimLicenseRefreshJobInstallActivitytimeoutInterval);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @Override
    public String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        return null;
    }

    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        return null;
    }
}
