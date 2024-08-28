/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.NODE_HEALTH_CHECK)
public class NodeHealthCheckJobActivityTimeouts implements ActivityTimeoutsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeHealthCheckJobActivityTimeouts.class);

    private static final Map<String, Integer> NHC_JOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<>();

    public static final String NODE_HEALTH_CHECK = "nodehealthcheck";
    public static final String ENM_HEALTH_CHECK = "enmhealthcheck";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    @Configured(propertyName = "ecimNodeHealthCheckActivityTimeOut")
    private int ecimNodeHealthCheckActivityTimeOut;

    @Inject
    @Configured(propertyName = "ecimNHCActivityWaitTimeToStartPollingInMinutes")
    private int ecimNHCActivityWaitTimeToStartPollingInMinutes;

    @Inject
    @Configured(propertyName = "radionodeNHCActivityTimeOut")
    private int radionodeNHCActivityTimeOut;

    @Inject
    @Configured(propertyName = "radionodeNHCActivityWaitTimeToStartPollingInMinutes")
    private int radionodeNHCActivityWaitTimeToStartPollingInMinutes;

    @Inject
    @Configured(propertyName = "cppEnmHealthCheckActivityTimeOut")
    private int cppEnmHealthCheckActivityTimeOut;

    @Inject
    @Configured(propertyName = "ecimEnmHealthCheckActivityTimeOut")
    private int ecimEnmHealthCheckActivityTimeOut;

    @Inject
    @Configured(propertyName = "cppNodeSyncCheckWaitIntervalInMin")
    private int cppNodeSyncCheckWaitIntervalInMin;

    @Inject
    @Configured(propertyName = "cppNodeSyncCheckTimeout")
    private int cppNodeSyncCheckTimeout;

    @Inject
    @Configured(propertyName = "ecimNodeSyncCheckWaitIntervalInMin")
    private int ecimNodeSyncCheckWaitIntervalInMin;

    @Inject
    @Configured(propertyName = "ecimNodeSyncCheckTimeout")
    private int ecimNodeSyncCheckTimeout;

    @Inject
    private SystemRecorder systemRecorder;

    public void listenEcimNodeHealthCheckActivityTimeOut(@Observes @ConfigurationChangeNotification(propertyName = "ecimNodeHealthCheckActivityTimeOut") final int ecimNodeHealthCheckActivityTimeOut) {
        LOGGER.info("ecimNodeHealthCheckActivityTimeOut value changed from {} to {} ", this.ecimNodeHealthCheckActivityTimeOut, ecimNodeHealthCheckActivityTimeOut);
        this.ecimNodeHealthCheckActivityTimeOut = ecimNodeHealthCheckActivityTimeOut;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK), ecimNodeHealthCheckActivityTimeOut);
    }

    public void listenRadionodeNHCActivityTimeOut(@Observes @ConfigurationChangeNotification(propertyName = "radionodeNHCActivityTimeOut") final int radionodeNHCActivityTimeOut) {
        LOGGER.info("radionodeNHCActivityTimeOut value changed from {} to {} ", this.radionodeNHCActivityTimeOut, radionodeNHCActivityTimeOut);
        this.radionodeNHCActivityTimeOut = radionodeNHCActivityTimeOut;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(NodeType.RADIONODE.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK), radionodeNHCActivityTimeOut);
    }

    public void listenRadionodeNHCActivityWaitTimeToStartPollingInMinutes(
            @Observes @ConfigurationChangeNotification(propertyName = "radionodeNHCActivityWaitTimeToStartPollingInMinutes") final int radionodeNHCActivityWaitTimeToStartPollingInMinutes) {
        LOGGER.info("radionodeNHCActivityWaitTimeToStartPollingInMinutes value changed from {} to {} ", this.radionodeNHCActivityWaitTimeToStartPollingInMinutes,
                radionodeNHCActivityWaitTimeToStartPollingInMinutes);
        this.radionodeNHCActivityWaitTimeToStartPollingInMinutes = radionodeNHCActivityWaitTimeToStartPollingInMinutes;

        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(NodeType.RADIONODE.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radionodeNHCActivityWaitTimeToStartPollingInMinutes);
    }

    public void listenEcimNodeHealthCheckActivityWaitTimeToStartPollingInMinutes(
            @Observes @ConfigurationChangeNotification(propertyName = "ecimNHCActivityWaitTimeToStartPollingInMinutes") final int ecimNHCActivityWaitTimeToStartPollingInMinutes) {
        LOGGER.info("ecimNHCActivityWaitTimeToStartPollingInMinutes value changed from {} to {} ", this.ecimNHCActivityWaitTimeToStartPollingInMinutes, ecimNHCActivityWaitTimeToStartPollingInMinutes);
        this.ecimNHCActivityWaitTimeToStartPollingInMinutes = ecimNHCActivityWaitTimeToStartPollingInMinutes;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimNHCActivityWaitTimeToStartPollingInMinutes);
    }

    public void listenCppEnmHealthCheckActivityTimeOut(@Observes @ConfigurationChangeNotification(propertyName = "cppEnmHealthCheckActivityTimeOut") final int cppEnmHealthCheckActivityTimeOut) {
        LOGGER.info("cppHealthCheckActivityTimeOut value changed from {} to {} ", this.cppEnmHealthCheckActivityTimeOut, cppEnmHealthCheckActivityTimeOut);
        this.cppEnmHealthCheckActivityTimeOut = cppEnmHealthCheckActivityTimeOut;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK), cppEnmHealthCheckActivityTimeOut);
    }

    public void listenEcimEnmHealthCheckActivityTimeOut(@Observes @ConfigurationChangeNotification(propertyName = "ecimEnmHealthCheckActivityTimeOut") final int ecimEnmHealthCheckActivityTimeOut) {
        LOGGER.info("ecimEnmHealthCheckActivityTimeOut value changed from {} to {} ", this.ecimEnmHealthCheckActivityTimeOut, ecimEnmHealthCheckActivityTimeOut);
        this.ecimEnmHealthCheckActivityTimeOut = ecimEnmHealthCheckActivityTimeOut;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK), ecimEnmHealthCheckActivityTimeOut);
    }

    public void listencppNodeSyncCheckWaitIntervalInMin(@Observes @ConfigurationChangeNotification(propertyName = "cppNodeSyncCheckWaitIntervalInMin") final int cppNodeSyncCheckWaitIntervalInMin) {
        LOGGER.info("cppNodeSyncCheckWaitIntervalInMin value changed from {} to {} ", this.cppNodeSyncCheckWaitIntervalInMin, cppNodeSyncCheckWaitIntervalInMin);
        this.cppNodeSyncCheckWaitIntervalInMin = cppNodeSyncCheckWaitIntervalInMin;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, cppNodeSyncCheckWaitIntervalInMin);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, cppNodeSyncCheckWaitIntervalInMin);
    }

    public void listencppNodeSyncCheckTimeout(@Observes @ConfigurationChangeNotification(propertyName = "cppNodeSyncCheckTimeout") final int cppNodeSyncCheckTimeout) {
        LOGGER.info("cppNodeSyncCheckTimeout value changed from {} to {} ", this.cppNodeSyncCheckTimeout, cppNodeSyncCheckTimeout);
        this.cppNodeSyncCheckTimeout = cppNodeSyncCheckTimeout;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, cppNodeSyncCheckTimeout);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, cppNodeSyncCheckTimeout);
    }

    public void listenECIMNodeSyncCheckWaitIntervalInMin(@Observes @ConfigurationChangeNotification(propertyName = "ecimNodeSyncCheckWaitIntervalInMin") final int ecimNodeSyncCheckWaitIntervalInMin) {
        LOGGER.info("ecimNodeSyncCheckWaitIntervalInMin value changed from {} to {} ", this.ecimNodeSyncCheckWaitIntervalInMin, ecimNodeSyncCheckWaitIntervalInMin);
        this.ecimNodeSyncCheckWaitIntervalInMin = ecimNodeSyncCheckWaitIntervalInMin;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, ecimNodeSyncCheckWaitIntervalInMin);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, ecimNodeSyncCheckWaitIntervalInMin);
    }

    public void listenECIMNodeSyncCheckTimeout(@Observes @ConfigurationChangeNotification(propertyName = "ecimNodeSyncCheckTimeout") final int ecimNodeSyncCheckTimeout) {
        LOGGER.info("ecimNodeSyncCheckTimeout value changed from {} to {} ", this.ecimNodeSyncCheckTimeout, ecimNodeSyncCheckTimeout);
        this.ecimNodeSyncCheckTimeout = ecimNodeSyncCheckTimeout;
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, ecimNodeSyncCheckTimeout);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, ecimNodeSyncCheckTimeout);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String neKey = generateKey(neType, jobType, activityName);
        final String platformkey = generateKey(platform.toUpperCase(), jobType, activityName);
        if (NHC_JOB_ACTIVITY_TIMEOUTS.containsKey(neKey)) {
            return NHC_JOB_ACTIVITY_TIMEOUTS.get(neKey);
        } else if (NHC_JOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return NHC_JOB_ACTIVITY_TIMEOUTS.get(platformkey);
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

    public String getNodeSyncCheckWaitIntervalOrTimeOut(final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName,final String type) {
        final String neKey = generateKey(platformTypeEnum.getName(), jobTypeEnum.getAttribute(), activityName) + DELIMETER_UNDERSCORE + type;
        return convertToIsoFormat(NHC_JOB_ACTIVITY_TIMEOUTS.get(neKey));
    }
    
    public Integer getNodeSyncCheckTimeOutAsInteger(final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName,final String type) {
        final String neKey = generateKey(platformTypeEnum.getName(), jobTypeEnum.getAttribute(), activityName) + DELIMETER_UNDERSCORE + type;
        return NHC_JOB_ACTIVITY_TIMEOUTS.get(neKey);
    }

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK), ecimNodeHealthCheckActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(NodeType.RADIONODE.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK), radionodeNHCActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimNHCActivityWaitTimeToStartPollingInMinutes);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(NodeType.RADIONODE.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radionodeNHCActivityWaitTimeToStartPollingInMinutes);

        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK), ecimNodeHealthCheckActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(NodeType.RADIONODE.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK), radionodeNHCActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimNHCActivityWaitTimeToStartPollingInMinutes);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(NodeType.RADIONODE.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radionodeNHCActivityWaitTimeToStartPollingInMinutes);

        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK), ecimNodeHealthCheckActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(NodeType.ERBS.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK), radionodeNHCActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(
                generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimNHCActivityWaitTimeToStartPollingInMinutes);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(NodeType.ERBS.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radionodeNHCActivityWaitTimeToStartPollingInMinutes);
        
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK), cppEnmHealthCheckActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK), ecimEnmHealthCheckActivityTimeOut);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, cppNodeSyncCheckWaitIntervalInMin);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, cppNodeSyncCheckTimeout);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, cppNodeSyncCheckWaitIntervalInMin);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.CPP.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, cppNodeSyncCheckTimeout);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, ecimNodeSyncCheckWaitIntervalInMin);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), NODE_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, ecimNodeSyncCheckTimeout);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME, ecimNodeSyncCheckWaitIntervalInMin);
        NHC_JOB_ACTIVITY_TIMEOUTS.put(generateKey(PlatformTypeEnum.ECIM.getName(), JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), ENM_HEALTH_CHECK) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.NODE_SYNC_TIMEOUT, ecimNodeSyncCheckTimeout);
        
        
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    @Override
    public String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer activityTimeout = getActivityPollWaitTimeAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(activityTimeout);
    }

    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String neKey = generateKey(neType, jobType, activityName) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        final String platformkey = generateKey(platform.toUpperCase(), jobType, activityName) + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        if (NHC_JOB_ACTIVITY_TIMEOUTS.containsKey(neKey)) {
            return NHC_JOB_ACTIVITY_TIMEOUTS.get(neKey);
        } else if (NHC_JOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return NHC_JOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(platform);
    }

    private String generateKey(final String primary, final String jobType, final String activityName) {
        return primary + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
    }
}
