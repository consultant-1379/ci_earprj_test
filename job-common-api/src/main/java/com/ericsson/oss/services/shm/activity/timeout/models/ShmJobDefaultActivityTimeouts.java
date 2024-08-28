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
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This class is used to listen and update the default timeouts
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
public class ShmJobDefaultActivityTimeouts {

    private static final String DEFAULT = "DEFAULT";

    private static final String DEFAULT_PRECHECK = "DEFAULT_PRECHECK";

    private static final String DEFAULT_HANDLE_TIMEOUT = "DEFAULT_HANDLE_TIMEOUT";

    private static final String DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL = "DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL";

    private static final String DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT = "DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT";

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    @Inject
    @Configured(propertyName = "SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUT")
    private int defaultActivityTimeout;

    @Inject
    @Configured(propertyName = "SHM_JOBS_DEFAULT_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int defaultActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_JOBS_CPP_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int defaultActivityCppPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_JOBS_ECIM_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int defaultActivityEcimPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_JOBS_DEFAULT_PRECHECK_TIMEOUT")
    private int defaultPrecheckTimeout;

    @Inject
    @Configured(propertyName = "SHM_JOBS_DEFAULT_TIMEOUT_FOR_HANDLE_TIMEOUT_IN_MINUTES")
    private int defaultTimeoutForHandleTimeout;

    @Inject
    @Configured(propertyName = "SHM_JOBS_CPP_DEFAULT_ACTIVITY_TIME_OUT")
    private int cppDefaultTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_JOBS_ECIM_DEFAULT_ACTIVITY_TIME_OUT")
    private int ecimDefaultTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_JOBS_MINI_LINK_INDOOR_DEFAULT_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorDefaultTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_JOBS_VRAN_DEFAULT_ACTIVITY_TIME_OUT")
    private int vranDefaultActivityTimeout;

    @Inject
    @Configured(propertyName = "AXE_UPGRADE_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES")
    private int axeUpgradeActivityHandleTimeout;

    @Inject
    @Configured(propertyName = "SHM_JOBS_DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL")
    private int defaultRepeatPrecheckWaitInterval;

    @Inject
    @Configured(propertyName = "SHM_JOBS_DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT")
    private int defaultRepeatPrecheckRetryAttempt;

    @Inject
    private SystemRecorder systemRecorder;

    public void listenForDefaultActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_DEFAULT_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int defaultActivityPollingWaitTime) {
        this.defaultActivityPollingWaitTime = defaultActivityPollingWaitTime;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (defaultActivityPollingWaitTime));
        LOGGER.info("Changed Default activity Polling Wait Time value : {}", this.defaultActivityPollingWaitTime);
    }

    public void listenForDefaultActivityTimeoutAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUT") final int defaultActivityTimeout) {
        this.defaultActivityTimeout = defaultActivityTimeout;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT, (defaultActivityTimeout));
        LOGGER.info("Changed Default activity timeout value : {}", this.defaultActivityTimeout);
    }

    public void listenForDefaultCppPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_CPP_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int defaultActivityCppPollingWaitTime) {
        this.defaultActivityCppPollingWaitTime = defaultActivityCppPollingWaitTime;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP.toString() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (defaultActivityCppPollingWaitTime));
        LOGGER.info("Changed Default Cpp Polling Wait Time value : {}", this.defaultActivityCppPollingWaitTime);
    }

    public void listenForDefaultECIMPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_ECIM_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int defaultActivityEcimPollingWaitTime) {
        this.defaultActivityEcimPollingWaitTime = defaultActivityEcimPollingWaitTime;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM.toString() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (defaultActivityEcimPollingWaitTime));
        LOGGER.info("Changed Default ECIM Polling Wait Time value : {}", this.defaultActivityEcimPollingWaitTime);
    }

    public void listenForDefaultPrecheckTimeoutAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_DEFAULT_PRECHECK_TIMEOUT") final int defaultPrecheckTimeout) {
        this.defaultPrecheckTimeout = defaultPrecheckTimeout;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_PRECHECK, (defaultPrecheckTimeout));
        LOGGER.info("Changed Default precheck timeout value : {}", this.defaultPrecheckTimeout);
    }

    public void listenForDefaultTimeoutForHandleTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_DEFAULT_TIMEOUT_FOR_HANDLE_TIMEOUT_IN_MINUTES") final int defaultTimeoutForHandleTimeout) {
        this.defaultTimeoutForHandleTimeout = defaultTimeoutForHandleTimeout;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_HANDLE_TIMEOUT, (defaultTimeoutForHandleTimeout));
        LOGGER.info("Changed Default timeout for handle timeout value : {}", this.defaultTimeoutForHandleTimeout);
    }

    public void listenForCppDefaultActivityTimeoutAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_CPP_DEFAULT_ACTIVITY_TIME_OUT") final int cppDefaultTimeoutInterval) {
        this.cppDefaultTimeoutInterval = cppDefaultTimeoutInterval;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP.toString(), cppDefaultTimeoutInterval);
        LOGGER.info("Changed CPP default activity timeout value : {}", this.cppDefaultTimeoutInterval);
    }

    public void listenForEcimDefaultActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_ECIM_DEFAULT_ACTIVITY_TIME_OUT") final int ecimDefaultTimeoutInterval) {
        this.ecimDefaultTimeoutInterval = ecimDefaultTimeoutInterval;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM.toString(), ecimDefaultTimeoutInterval);
        LOGGER.info("Changed ECIM default activity timeout value : {}", this.ecimDefaultTimeoutInterval);
    }

    public void listenForMiniLinkIndoorDefaultActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_MINI_LINK_INDOOR_DEFAULT_ACTIVITY_TIME_OUT") final int miniLinkIndoorDefaultTimeoutInterval) {
        this.miniLinkIndoorDefaultTimeoutInterval = miniLinkIndoorDefaultTimeoutInterval;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR.toString(), miniLinkIndoorDefaultTimeoutInterval);
        LOGGER.info("Changed MINI_LINK_INDOOR default activity timeout value : {}", this.miniLinkIndoorDefaultTimeoutInterval);
    }

    public void listenForVranDefaultActivityTimeoutValue(@Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_VRAN_DEFAULT_ACTIVITY_TIME_OUT") final int vranDefaultActivityTimeout) {
        this.vranDefaultActivityTimeout = vranDefaultActivityTimeout;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN.toString(), vranDefaultActivityTimeout);
        LOGGER.info("Changed vRAN default activity timeout value : {}", this.vranDefaultActivityTimeout);
    }

    public void listenForAxeUpgradeActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "AXE_UPGRADE_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES") final int axeUpgradeActivityHandleTimeout) {
        this.axeUpgradeActivityHandleTimeout = axeUpgradeActivityHandleTimeout;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE.toString(), axeUpgradeActivityHandleTimeout);
        LOGGER.info("Change in AXE upgrade job handle timeout value : {} minutes", axeUpgradeActivityHandleTimeout);
    }

    public void listenForDefaultRepeatPrecheckWaitIntervalAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL") final int defaultRepeatPrecheckWaitInterval) {
        this.defaultRepeatPrecheckWaitInterval = defaultRepeatPrecheckWaitInterval;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL, (defaultRepeatPrecheckWaitInterval));
        LOGGER.info("Changed Default Repeat Precheck Wait Interval value : {}", this.defaultRepeatPrecheckWaitInterval);
    }

    public void listenForDefaultRepeatPrecheckRetryAttemptAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_JOBS_DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT") final int defaultRepeatPrecheckRetryAttempt) {
        this.defaultRepeatPrecheckRetryAttempt = defaultRepeatPrecheckRetryAttempt;
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT, (defaultRepeatPrecheckRetryAttempt));
        LOGGER.info("Changed Default Repeat Precheck Retry Attempt value : {}", this.defaultRepeatPrecheckRetryAttempt);
    }

    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        return getDefaultActivityTimeoutBasedOnPlatform(platform);
    }

    public String getActivityTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer activityTimeout = getDefaultActivityTimeoutBasedOnPlatform(platformTypeEnum.name());
        return convertToIsoFormat(activityTimeout);
    }

    public String getPrecheckTimeout() {
        return convertToIsoFormat(SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT_PRECHECK));
    }

    public Integer getPrecheckTimeoutAsInteger() {
        return SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT_PRECHECK);
    }

    public String getTimeoutForHandleTimeout() {
        return convertToIsoFormat(getTimeoutForHandleTimeoutAsInteger());
    }

    public Integer getTimeoutForHandleTimeoutAsInteger() {
        return SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT_HANDLE_TIMEOUT);
    }

    private String convertToIsoFormat(final int timeout) {
        return "PT" + timeout + "M";
    }

    public Integer getDefaultActivityTimeoutBasedOnPlatform(final String platform) {
        final String key = platform.toUpperCase();
        return SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.containsKey(key) ? SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(key) : SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT);
    }

    public String getRepeatPrecheckWaitInterval() {
        return convertToIsoFormat(SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL));
    }

    public Integer getRepeatPrecheckWaitIntervalAsInteger() {
        return SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL);
    }

    public int getRepeatPrecheckRetryAttempt() {
        return SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT);
    }

    public String getDefaultActivityPollingWaitTimeOnPlatform(final String platform) {
        final String key = platform.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        return convertToIsoFormat(SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.containsKey(key) ? SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(key)
                : SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING));
    }

    public Integer getDefaultActivityPollingWaitTimeOnPlatformAsInteger(final String platform) {
        final String key = platform.toUpperCase() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        return SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.containsKey(key) ? SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(key)
                : SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.get(DEFAULT + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING);
    }

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, defaultActivityPollingWaitTime);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP.toString() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, defaultActivityCppPollingWaitTime);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM.toString() + ActivityTimeoutConstants.DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, defaultActivityEcimPollingWaitTime);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP.toString(), cppDefaultTimeoutInterval);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM.toString(), ecimDefaultTimeoutInterval);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR.toString(), miniLinkIndoorDefaultTimeoutInterval);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN.toString(), vranDefaultActivityTimeout);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE.toString(), axeUpgradeActivityHandleTimeout);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT, defaultActivityTimeout);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_PRECHECK, defaultPrecheckTimeout);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_HANDLE_TIMEOUT, defaultTimeoutForHandleTimeout);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_REPEAT_PRECHECK_WAIT_INTERVAL, defaultRepeatPrecheckWaitInterval);
        SHM_JOBS_DEFAULT_ACTIVITY_TIMEOUTS.put(DEFAULT_REPEAT_PRECHECK_RETRY_ATTEMPT, defaultRepeatPrecheckRetryAttempt);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }
}
