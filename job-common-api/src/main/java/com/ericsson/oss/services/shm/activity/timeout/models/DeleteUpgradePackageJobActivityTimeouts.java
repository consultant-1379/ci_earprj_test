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

import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * Class which provides activity timeout values for DeleteUpgardePackage job.
 * 
 * @author xthagan
 * 
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.DELETE_UPGRADEPACKAGE)
public class DeleteUpgradePackageJobActivityTimeouts extends AbstractActivityTimeoutsProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePackageJobActivityTimeouts.class);
    private static final Map<String, Integer> DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    private static final String DELETEUPGRADEPACKAGE = "deleteupgradepackage";

    @Inject
    @Configured(propertyName = "MGW_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT")
    private int mgwDeleteUPDeleteupgradepackageActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT")
    private int mrsDeleteUPDeleteupgradepackageActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_DELETEUPGRADEPACKAGEJOB_DELETEUPGRDAEPACKAGE_ACTIVITY_TIME_OUT")
    private int erbsDeleteUPDeleteupgradepackageActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_CPP_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT")
    private int cppDeleteUPDeleteupgradepackageActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT")
    private int radioNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SGSN_MME_DELETEUPGRADEPACKAGEJOB_DELETEUPGRDAEPACKAGE_ACTIVITY_TIME_OUT")
    private int sgsnNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT")
    private int ecimDeleteUPDeleteupgradepackageActivityTimeoutValue;

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(PlatformTypeEnum.CPP.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                cppDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.ERBS.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                erbsDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.MGW.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                mgwDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.MRS.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                mrsDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                ecimDeleteUPDeleteupgradepackageActivityTimeoutValue);
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.RADIONODE.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                radioNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.SGSN_MME.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                sgsnNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    public void listenForCppDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT") final int cppDeleteUPDeleteupgradepackageActivitytimeoutInterval) {
        this.cppDeleteUPDeleteupgradepackageActivitytimeoutInterval = cppDeleteUPDeleteupgradepackageActivitytimeoutInterval;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(PlatformTypeEnum.CPP.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                cppDeleteUPDeleteupgradepackageActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for CPP DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", cppDeleteUPDeleteupgradepackageActivitytimeoutInterval);
    }

    public void listenForErbsNodeDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_DELETEUPGRADEPACKAGEJOB_DELETEUPGRDAEPACKAGE_ACTIVITY_TIME_OUT") final int erbsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue) {
        this.radioNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval = erbsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.ERBS.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                (erbsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue));
        LOGGER.info("Changed timeout value for erbsNode DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", erbsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue);
    }

    public void listenForMgwNodeDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT") final int mgwNodeDeleteUPDeleteupgradepackageActivityTimeoutValue) {
        this.sgsnNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval = mgwNodeDeleteUPDeleteupgradepackageActivityTimeoutValue;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.MGW.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                (mgwNodeDeleteUPDeleteupgradepackageActivityTimeoutValue));
        LOGGER.info("Changed timeout value for mgwNode DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", mgwNodeDeleteUPDeleteupgradepackageActivityTimeoutValue);
    }

	public void listenForMrsNodeDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT") final int mrsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue) {
        this.sgsnNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval = mrsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.MRS.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                (mrsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue));
        LOGGER.info("Changed timeout value for mrsNode DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", mrsNodeDeleteUPDeleteupgradepackageActivityTimeoutValue);
    }

    public void listenForEcimDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT") final int ecimDeleteUPDeleteupgradepackageActivityTimeoutValue) {
        this.ecimDeleteUPDeleteupgradepackageActivityTimeoutValue = ecimDeleteUPDeleteupgradepackageActivityTimeoutValue;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                ecimDeleteUPDeleteupgradepackageActivityTimeoutValue);
        LOGGER.info("Changed timeout value for ECIM DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", ecimDeleteUPDeleteupgradepackageActivityTimeoutValue);
    }

    public void listenForradioNodeDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT") final int radioNodeDeleteUPDeleteupgradepackageActivityTimeoutValue) {
        this.radioNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval = radioNodeDeleteUPDeleteupgradepackageActivityTimeoutValue;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.RADIONODE.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                (radioNodeDeleteUPDeleteupgradepackageActivityTimeoutValue));
        LOGGER.info("Changed timeout value for RadioNode DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", radioNodeDeleteUPDeleteupgradepackageActivityTimeoutValue);
    }

    public void listenForsgsnNodeDeleteUPDeleteupgradepackageActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_MME_DELETEUPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_TIME_OUT") final int sgsnNodeDeleteUPDeleteupgradepackageActivityTimeoutValue) {
        this.sgsnNodeDeleteUPDeleteupgradepackageActivitytimeoutInterval = sgsnNodeDeleteUPDeleteupgradepackageActivityTimeoutValue;
        DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.put(prepareKey(NodeType.SGSN_MME.getName(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), DELETEUPGRADEPACKAGE),
                (sgsnNodeDeleteUPDeleteupgradepackageActivityTimeoutValue));
        LOGGER.info("Changed timeout value for sgsnNode DeleteUpgradePackage Job deleteupgradepackage activity is : {} minutes", sgsnNodeDeleteUPDeleteupgradepackageActivityTimeoutValue);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {

        final String neKey = prepareKey(neType, jobType, activityName);
        final String platformkey = prepareKey(platform, jobType, activityName);

        if (DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.containsKey(neKey)) {
            return DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.get(neKey);
        } else if (DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return DELETEUPGRADEPACKAGE_JOB_ACTIVITY_TIMEOUTS.get(platformkey);
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
