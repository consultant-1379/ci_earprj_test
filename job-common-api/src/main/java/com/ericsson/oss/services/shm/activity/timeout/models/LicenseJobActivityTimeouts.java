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

import com.ericsson.nms.security.smrs.api.NodeType;
import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * This class is used to listen and update the neType specific timeouts of License Job
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.LICENSE)
public class LicenseJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> LICENSEJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    private static final String INSTALL = "install";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    @Configured(propertyName = "RADIONODE_LICENSEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int radioNodeLicenseInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int ecimLicenseInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_LICENSE_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorLicenseActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_AXE_LICENSEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int axeLicenseActivitytimeoutInterval;

    @Inject
    private SystemRecorder systemRecorder;

    public void listenForradioNodeLicenseInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_LICENSEJOB_INSTALL_ACTIVITY_TIME_OUT") final int radioNodeInstallActivitytimeoutInterval) {
        this.radioNodeLicenseInstallActivitytimeoutInterval = radioNodeInstallActivitytimeoutInterval;
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, (radioNodeInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode License Job Install activity is : {} minutes", radioNodeInstallActivitytimeoutInterval);
    }

    public void listenForEcimLicenseInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSEJOB_INSTALL_ACTIVITY_TIME_OUT") final int ecimLicenseInstallActivitytimeoutInterval) {
        this.ecimLicenseInstallActivitytimeoutInterval = ecimLicenseInstallActivitytimeoutInterval;
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, (ecimLicenseInstallActivitytimeoutInterval));
        LOGGER.info("Changed time out value for ecim License Job Install activity is: {} minutes", ecimLicenseInstallActivitytimeoutInterval);
    }

    public void listenForMiniLinkIndoorLicenseInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_LICENSE_ACTIVITY_TIME_OUT") final int miniLinkIndoorLicenseActivitytimeoutInterval) {
        this.miniLinkIndoorLicenseActivitytimeoutInterval = miniLinkIndoorLicenseActivitytimeoutInterval;
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, miniLinkIndoorLicenseActivitytimeoutInterval);
        LOGGER.info("Change in MINI-LINK-Indoor license job install timeout value : {} minutes", miniLinkIndoorLicenseActivitytimeoutInterval);
    }

    public void listenForAxeLicenseInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_LICENSEJOB_INSTALL_ACTIVITY_TIME_OUT") final int axeLicenseActivitytimeoutInterval) {
        this.axeLicenseActivitytimeoutInterval = axeLicenseActivitytimeoutInterval;
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, axeLicenseActivitytimeoutInterval);
        LOGGER.info("Change in AXE license job install timeout value : {} minutes", axeLicenseActivitytimeoutInterval);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        if (LICENSEJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return LICENSEJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (LICENSEJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return LICENSEJOB_ACTIVITY_TIMEOUTS.get(platformkey);
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
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, radioNodeLicenseInstallActivitytimeoutInterval);
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, ecimLicenseInstallActivitytimeoutInterval);
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, miniLinkIndoorLicenseActivitytimeoutInterval);
        LICENSEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE + DELIMETER_UNDERSCORE + INSTALL, axeLicenseActivitytimeoutInterval);
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
