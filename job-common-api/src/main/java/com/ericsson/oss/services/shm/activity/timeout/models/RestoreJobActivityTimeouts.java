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
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * This class is used to listen and update the neType specific timeouts of Restore Job
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.RESTORE)
@SuppressWarnings({ "PMD.TooManyFields", "PMD.ExcessivePublicCount", "PMD.TooManyMethods" })
public class RestoreJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> RESTOREJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    private static final String DOWNLOAD_CV = "download";
    private static final String DOWNLOAD = "download";
    private static final String VERIFY = "verify";
    private static final String CONFIRM = "confirm";
    private static final String RESTORE = "restore";
    private static final String INSTALL = "install";
    private static final String DOWNLOAD_BACKUP = "downloadbackup";
    private static final String RESTORE_BACKUP = "restorebackup";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    /*********************** CPP Restore Job Download Activity Timeout and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SHM_ERBS_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsRestoreDownloadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "MGW_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int mgwRestoreDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int mrsRestoreDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int erbsRestoreDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RNC_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int rncRestoreDownloadActivitytimeoutInterval;

    /*********************** CPP Restore Job Verify Activity Timeout and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SHM_ERBS_RESTOREJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsRestoreVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "MGW_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int mgwRestoreVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int mrsRestoreVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int erbsRestoreVerifyActivitytimeoutInterval;

    /*********************** CPP Restore Job Install Activity Timeout and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SHM_ERBS_RESTOREJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsRestoreInstallActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "MGW_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int mgwRestoreInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int mrsRestoreInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int erbsRestoreInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RNC_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int rncRestoreInstallActivitytimeoutInterval;

    /*********************** CPP Restore Job Restore Activity Timeout and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SHM_ERBS_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsRestoreActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "MGW_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT")
    private int mgwRestoreActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT")
    private int mrsRestoreActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT")
    private int erbsRestoreActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RNC_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT")
    private int rncRestoreActivitytimeoutInterval;

    /*********************** CPP Restore Job Confirm Activity Timeout and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SHM_ERBS_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsRestoreConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "MGW_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int mgwRestoreConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int mrsRestoreConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int erbsRestoreConfirmActivitytimeoutInterval;

    /*********************** ECIM Restore Job Restore Activity Timeouts and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SGSN_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int sgsnRestoreActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int radioNodeRestoreActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnRestoreActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeRestoreActivityPollinWaitTime;

    /*********************** ECIM Restore Job Download Activity Timeouts and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SGSN_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int sgsnRestoreDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int radioNodeRestoreDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnRestoreDownloadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeRestoreDownloadActivityPollingWaitTime;

    /*********************** ECIM Restore Job Confirm Activity Timeouts and Polling Wait Times **************************/
    @Inject
    @Configured(propertyName = "SGSN_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int sgsnRestoreConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int radioNodeRestoreConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnRestoreConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeRestoreConfirmActivityPollingWaitTime;

    /******************** C P P - R E S T O R E J O B - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int cppRestoreDownloadActivityTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int cppRestoreVerifyActivityTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int cppRestoreInstallActivityTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT")
    private int cppRestoreActivityTimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int cppRestoreConfirmActivityTimeoutInterval;

    /********************** CPP PLATFORM POLLING WAIT TIMES ************************/

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppRestoreDownloadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppRestoreVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppRestoreInstallActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppRestoreActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppRestoreConfirmActivityPollingWaitTime;

    /******************** E C I M - R E S T O R E J O B - T I M E O U T S *****************************/

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int ecimRestoreDownloadBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int ecimRestoreRestoreBackupActivitytimeoutInterval;

    /************************************* ECIM PLATFORM POLLING WAIT TIMES ************************************/

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimRestoreDownloadBackupActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimRestoreRestoreBackupActivityPollingWaitTime;

    /*
     * Please note : ECIM Restore Confirm activity is synchronous action. So, it uses default activity timeout.
     */

    /******************** M I N I - L I N K - R E S T O R E J O B - T I M E O U T S *****************************/

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorRestoreDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorRestoreVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorRestoreRestoreActivitytimeoutInterval;

    /** RNNODE RESTORE JOB ACTIVITY TIMEOUTS **/
    @Inject
    @Configured(propertyName = "RNNODE_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int rnNodeRestoreActivityTimeout;

    @Inject
    @Configured(propertyName = "RNNODE_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int rnNodeRestoreDownloadActivityTimeout;

    @Inject
    @Configured(propertyName = "RNNODE_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int rnNodeRestoreConfirmActivityTimeout;

    /** 5GRADIONODE RESTORE JOB ACTIVITY TIMEOUTS **/
    @Inject
    @Configured(propertyName = "NSANR_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int nrNodeRestoreActivityTimeout;

    @Inject
    @Configured(propertyName = "NSANR_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int nrNodeRestoreDownloadActivityTimeout;

    @Inject
    @Configured(propertyName = "NSANR_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int nrNodeRestoreConfirmActivityTimeout;

    /** VRC RESTORE JOB ACTIVITY TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VRC_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int vrcRestoreActivityTimeout;

    @Inject
    @Configured(propertyName = "VRC_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vrcRestoreDownloadActivityTimeout;

    @Inject
    @Configured(propertyName = "VRC_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vrcRestoreConfirmActivityTimeout;

    /** VPP RESTORE JOB ACTIVITY TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VPP_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int vppRestoreActivityTimeout;

    @Inject
    @Configured(propertyName = "VPP_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vppRestoreDownloadActivityTimeout;

    @Inject
    @Configured(propertyName = "VPP_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vppRestoreConfirmActivityTimeout;

    /** VSD RESTORE JOB ACTIVITY TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VSD_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int vsdRestoreActivityTimeout;

    @Inject
    @Configured(propertyName = "VSD_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vsdRestoreDownloadActivityTimeout;

    @Inject
    @Configured(propertyName = "VSD_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vsdRestoreConfirmActivityTimeout;

    /** VTFRadioNode RESTORE JOB ACTIVITY TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VTFRADIONODE_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeRestoreActivityTimeout;

    @Inject
    @Configured(propertyName = "VTFRADIONODE_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeRestoreDownloadActivityTimeout;

    @Inject
    @Configured(propertyName = "VTFRADIONODE_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeRestoreConfirmActivityTimeout;

    @Inject
    private SystemRecorder systemRecorder;

    public void listenForRncRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int rncDownloadActivitytimeoutInterval) {
        this.rncRestoreDownloadActivitytimeoutInterval = rncDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, (rncDownloadActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RNC Restore Job Download Backup activity is : {} minutes", rncDownloadActivitytimeoutInterval);
    }

    public void listenForErbsRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int erbsDownloadActivitytimeoutInterval) {
        this.erbsRestoreDownloadActivitytimeoutInterval = erbsDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, (erbsDownloadActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Restore Job Download Backup activity is : {} minutes", erbsDownloadActivitytimeoutInterval);
    }

    public void listenForMgwRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int mgwDownloadActivitytimeoutInterval) {
        this.mgwRestoreDownloadActivitytimeoutInterval = mgwDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, (mgwDownloadActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Restore Job Download Backup activity is : {} minutes", mgwDownloadActivitytimeoutInterval);
    }

    public void listenForMrsRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int mrsDownloadActivitytimeoutInterval) {
        this.mrsRestoreDownloadActivitytimeoutInterval = mrsDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, (mrsDownloadActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MRS Restore Job Download Backup activity is : {} minutes", mrsDownloadActivitytimeoutInterval);
    }

    public void listenForErbsRestoreVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT") final int erbsVerifyActivitytimeoutInterval) {
        this.erbsRestoreVerifyActivitytimeoutInterval = erbsVerifyActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, (erbsVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Restore Job Verify activity is : {} minutes", erbsVerifyActivitytimeoutInterval);
    }

    public void listenForMgwRestoreVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT") final int mgwVerifyActivitytimeoutInterval) {
        this.mgwRestoreVerifyActivitytimeoutInterval = mgwVerifyActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, (mgwVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Restore Job Verify activity is : {} minutes", mgwVerifyActivitytimeoutInterval);
    }

    public void listenForMrsRestoreVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT") final int mrsVerifyActivitytimeoutInterval) {
        this.mrsRestoreVerifyActivitytimeoutInterval = mrsVerifyActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, (mrsVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MRS Restore Job Verify activity is : {} minutes", mrsVerifyActivitytimeoutInterval);
    }

    public void listenForRncRestoreInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT") final int rncInstallActivitytimeoutInterval) {
        this.rncRestoreInstallActivitytimeoutInterval = rncInstallActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, (rncInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RNC Restore Job Install activity is : {} minutes", rncInstallActivitytimeoutInterval);
    }

    public void listenForErbsRestoreInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT") final int erbsInstallActivitytimeoutInterval) {
        this.erbsRestoreInstallActivitytimeoutInterval = erbsInstallActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, (erbsInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Restore Job Install activity is : {} minutes", erbsInstallActivitytimeoutInterval);
    }

    public void listenForMgwRestoreInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT") final int mgwInstallActivitytimeoutInterval) {
        this.mgwRestoreInstallActivitytimeoutInterval = mgwInstallActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, (mgwInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Restore Job Install activity is : {} minutes", mgwInstallActivitytimeoutInterval);
    }

    public void listenForMrsRestoreInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT") final int mrsInstallActivitytimeoutInterval) {
        this.mrsRestoreInstallActivitytimeoutInterval = mrsInstallActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, (mrsInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MRS Restore Job Install activity is : {} minutes", mrsInstallActivitytimeoutInterval);
    }

    public void listenForRncRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT") final int rncRestoreActivitytimeoutInterval) {
        this.rncRestoreActivitytimeoutInterval = rncRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, (rncRestoreActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RNC Restore Job Restore Backup activity is : {} minutes", rncRestoreActivitytimeoutInterval);
    }

    public void listenForErbsRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT") final int erbsRestoreActivitytimeoutInterval) {
        this.erbsRestoreActivitytimeoutInterval = erbsRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, (erbsRestoreActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Restore Job Restore Backup activity is : {} minutes", erbsRestoreActivitytimeoutInterval);
    }

    public void listenForMgwRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT") final int mgwRestoreActivitytimeoutInterval) {
        this.mgwRestoreActivitytimeoutInterval = mgwRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, (mgwRestoreActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Restore Job Restore Backup activity is : {} minutes", mgwRestoreActivitytimeoutInterval);
    }

    public void listenForMrsRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT") final int mrsRestoreActivitytimeoutInterval) {
        this.mrsRestoreActivitytimeoutInterval = mrsRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, (mrsRestoreActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MRS Restore Job Restore Backup activity is : {} minutes", mrsRestoreActivitytimeoutInterval);
    }

    public void listenForErbsRestoreConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int erbsConfirmActivitytimeoutInterval) {
        this.erbsRestoreConfirmActivitytimeoutInterval = erbsConfirmActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (erbsConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Restore Job Confirm Restore activity is : {} minutes", erbsConfirmActivitytimeoutInterval);
    }

    public void listenForMgwRestoreConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int mgwConfirmActivitytimeoutInterval) {
        this.mgwRestoreConfirmActivitytimeoutInterval = mgwConfirmActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (mgwConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Restore Job Confirm Restore activity is : {} minutes", mgwConfirmActivitytimeoutInterval);
    }

    public void listenForMrsRestoreConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int mrsConfirmActivitytimeoutInterval) {
        this.mrsRestoreConfirmActivitytimeoutInterval = mrsConfirmActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (mrsConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MRS Restore Job Confirm Restore activity is : {} minutes", mrsConfirmActivitytimeoutInterval);
    }

    public void listenForSgsnRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int sgsnRestoreActivitytimeoutInterval) {
        this.sgsnRestoreActivitytimeoutInterval = sgsnRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (sgsnRestoreActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Restore Job Restore Backup activity is : {} minutes", sgsnRestoreActivitytimeoutInterval);
    }

    public void listenForRadioNodeRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int radioNodeRestoreActivitytimeoutInterval) {
        this.radioNodeRestoreActivitytimeoutInterval = radioNodeRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (radioNodeRestoreActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Restore Job Restore Backup activity is : {} minutes", radioNodeRestoreActivitytimeoutInterval);
    }

    public void listenForSgsnRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int sgsnDownloadActivitytimeoutInterval) {
        this.sgsnRestoreDownloadActivitytimeoutInterval = sgsnDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (sgsnDownloadActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Restore Job Download Backup activity is : {} minutes", sgsnDownloadActivitytimeoutInterval);
    }

    public void listenForRadioNodeRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int radioNodeDownloadActivitytimeoutInterval) {
        this.radioNodeRestoreDownloadActivitytimeoutInterval = radioNodeDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS
                .put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (radioNodeDownloadActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Restore Job Restore Backup activity is : {} minutes", radioNodeDownloadActivitytimeoutInterval);
    }

    public void listenForSgsnRestoreConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int sgsnConfirmActivitytimeoutInterval) {
        this.sgsnRestoreConfirmActivitytimeoutInterval = sgsnConfirmActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (sgsnConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Restore Job Confirm activity is : {} minutes", sgsnConfirmActivitytimeoutInterval);
    }

    public void listenForRadioNodeRestoreConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int radioNodeConfirmActivitytimeoutInterval) {
        this.radioNodeRestoreConfirmActivitytimeoutInterval = radioNodeConfirmActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (radioNodeConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Restore Job Confirm activity is : {} minutes", radioNodeConfirmActivitytimeoutInterval);
    }

    public void listenForCppDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int downloadActivitytimeoutInterval) {
        this.cppRestoreDownloadActivityTimeoutInterval = downloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, cppRestoreDownloadActivityTimeoutInterval);
        LOGGER.info("Changed Cpp Restore download timeout interval value : {}", cppRestoreDownloadActivityTimeoutInterval);
    }

    public void listenForCppRestoreVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT") final int verifyActivityTimeoutInterval) {
        this.cppRestoreVerifyActivityTimeoutInterval = verifyActivityTimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, cppRestoreVerifyActivityTimeoutInterval);
        LOGGER.info("Changed Cpp Restore Verify timeout interval value : {}", cppRestoreVerifyActivityTimeoutInterval);
    }

    public void listenForCppRestoreInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_INSTALL_ACTIVITY_TIME_OUT") final int installActivitytimeoutInterval) {
        this.cppRestoreInstallActivityTimeoutInterval = installActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, cppRestoreInstallActivityTimeoutInterval);
        LOGGER.info("Changed Cpp Restore Install timeout interval value : {}", cppRestoreInstallActivityTimeoutInterval);
    }

    public void listenForCppRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT") final int restoreActivityTimeoutInterval) {
        this.cppRestoreActivityTimeoutInterval = restoreActivityTimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, cppRestoreActivityTimeoutInterval);
        LOGGER.info("Changed Cpp Restore timeout interval value : {}", cppRestoreActivityTimeoutInterval);
    }

    public void listenForCppRestoreConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int confirmActivityTimeoutInterval) {
        this.cppRestoreConfirmActivityTimeoutInterval = confirmActivityTimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, cppRestoreConfirmActivityTimeoutInterval);
        LOGGER.info("Changed Cpp Restore Confirm timeout interval value : {}", cppRestoreConfirmActivityTimeoutInterval);
    }

    public void listenForEcimRestoreDownloadBackupActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int ecimRestoreDownloadBackupActivitytimeoutInterval) {
        this.ecimRestoreDownloadBackupActivitytimeoutInterval = ecimRestoreDownloadBackupActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP,
                (ecimRestoreDownloadBackupActivitytimeoutInterval));
        LOGGER.info("Changed ecim restore job Download Backup timeout value : {} minutes", ecimRestoreDownloadBackupActivitytimeoutInterval);
    }

    public void listenForEcimRestoreRestoreBackupActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int ecimRestoreRestoreBackupActivitytimeoutInterval) {
        this.ecimRestoreRestoreBackupActivitytimeoutInterval = ecimRestoreRestoreBackupActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (ecimRestoreRestoreBackupActivitytimeoutInterval));
        LOGGER.info("Changed ecim restore job Restore Backup timeout value : {} minutes", ecimRestoreRestoreBackupActivitytimeoutInterval);
    }

    public void listenForMinilinkIndoorRestoreDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int miniLinkIndoorRestoreDownloadActivitytimeoutInterval) {
        this.miniLinkIndoorRestoreDownloadActivitytimeoutInterval = miniLinkIndoorRestoreDownloadActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD,
                (miniLinkIndoorRestoreDownloadActivitytimeoutInterval));
        LOGGER.info("Changed MINI-LINK-Indoor restore job Download timeout value : {} minutes", miniLinkIndoorRestoreDownloadActivitytimeoutInterval);
    }

    public void listenForMinilinkIndoorRestoreVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_VERIFY_ACTIVITY_TIME_OUT") final int miniLinkIndoorRestoreVerifyActivitytimeoutInterval) {
        this.miniLinkIndoorRestoreVerifyActivitytimeoutInterval = miniLinkIndoorRestoreVerifyActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY,
                (miniLinkIndoorRestoreVerifyActivitytimeoutInterval));
        LOGGER.info("Changed MINI-LINK-Indoor restore job Verify timeout value : {} minutes", miniLinkIndoorRestoreVerifyActivitytimeoutInterval);
    }

    public void listenForMinilinkIndoorRestoreRestoreActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_RESTORE_ACTIVITY_TIME_OUT") final int miniLinkIndoorRestoreRestoreActivitytimeoutInterval) {
        this.miniLinkIndoorRestoreRestoreActivitytimeoutInterval = miniLinkIndoorRestoreRestoreActivitytimeoutInterval;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE,
                (miniLinkIndoorRestoreRestoreActivitytimeoutInterval));
        LOGGER.info("Changed MINI-LINK-Indoor restore job Restore timeout value : {} minutes", miniLinkIndoorRestoreRestoreActivitytimeoutInterval);
    }

    public void listenForChangeInRnNodeRestoreActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int rnNodeRestoreActivityTimeout) {
        this.rnNodeRestoreActivityTimeout = rnNodeRestoreActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (rnNodeRestoreActivityTimeout));
        LOGGER.info("Changed timeout value for RnNode Restore Job Restore Backup activity is : {} minutes", rnNodeRestoreActivityTimeout);
    }

    public void listenForChangeInRnNodeRestoreDownloadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int rnNodeRestoreDownloadActivityTimeout) {
        this.rnNodeRestoreDownloadActivityTimeout = rnNodeRestoreDownloadActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (rnNodeRestoreDownloadActivityTimeout));
        LOGGER.info("Changed timeout value for RnNode Restore Job Download activity is : {} minutes", rnNodeRestoreDownloadActivityTimeout);
    }

    public void listenForChangeInRnNodeRestoreConfirmActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int rnNodeRestoreConfirmActivityTimeout) {
        this.rnNodeRestoreConfirmActivityTimeout = rnNodeRestoreConfirmActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (rnNodeRestoreConfirmActivityTimeout));
        LOGGER.info("Changed timeout value for RnNode Restore Job Confirm activity is : {} minutes", rnNodeRestoreConfirmActivityTimeout);
    }

    public void listenForChangeIn5GRadioNodeRestoreActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int nrNodeRestoreActivityTimeout) {
        this.nrNodeRestoreActivityTimeout = nrNodeRestoreActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (nrNodeRestoreActivityTimeout));
        LOGGER.info("Changed timeout value for 5GRadioNode Restore Job Restore Backup activity is : {} minutes", nrNodeRestoreActivityTimeout);
    }

    public void listenForChangeIn5GRadioNodeRestoreDownloadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int nrNodeRestoreDownloadActivityTimeout) {
        this.nrNodeRestoreDownloadActivityTimeout = nrNodeRestoreDownloadActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (nrNodeRestoreDownloadActivityTimeout));
        LOGGER.info("Changed timeout value for 5GRadioNode Restore Job Download activity is : {} minutes", nrNodeRestoreDownloadActivityTimeout);
    }

    public void listenForChangeIn5GRadioNodeRestoreConfirmActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int nrNodeRestoreConfirmActivityTimeout) {
        this.nrNodeRestoreConfirmActivityTimeout = nrNodeRestoreConfirmActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (nrNodeRestoreConfirmActivityTimeout));
        LOGGER.info("Changed timeout value for 5GRadioNode Restore Job Confirm activity is : {} minutes", nrNodeRestoreConfirmActivityTimeout);
    }

    public void listenForChangeInVrcRestoreActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int vrcRestoreActivityTimeout) {
        this.vrcRestoreActivityTimeout = vrcRestoreActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (vrcRestoreActivityTimeout));
        LOGGER.info("Changed timeout value for vRC Restore Job Restore Backup activity is : {} minutes", vrcRestoreActivityTimeout);
    }

    public void listenForChangeInVrcRestoreDownloadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int vrcRestoreDownloadActivityTimeout) {
        this.vrcRestoreDownloadActivityTimeout = vrcRestoreDownloadActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (vrcRestoreDownloadActivityTimeout));
        LOGGER.info("Changed timeout value for vRC Restore Job Download activity is : {} minutes", vrcRestoreDownloadActivityTimeout);
    }

    public void listenForChangeInVrcRestoreConfirmActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vrcRestoreConfirmActivityTimeout) {
        this.vrcRestoreConfirmActivityTimeout = vrcRestoreConfirmActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (vrcRestoreConfirmActivityTimeout));
        LOGGER.info("Changed timeout value for vRC Restore Job Confirm activity is : {} minutes", vrcRestoreConfirmActivityTimeout);
    }

    public void listenForChangeInVppRestoreActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int vppRestoreActivityTimeout) {
        this.vppRestoreActivityTimeout = vppRestoreActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (vppRestoreActivityTimeout));
        LOGGER.info("Changed timeout value for vPP Restore Job Restore Backup activity is : {} minutes", vppRestoreActivityTimeout);
    }

    public void listenForChangeInVppRestoreDownloadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int vppRestoreDownloadActivityTimeout) {
        this.vppRestoreDownloadActivityTimeout = vppRestoreDownloadActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (vppRestoreDownloadActivityTimeout));
        LOGGER.info("Changed timeout value for vPP Restore Job Download activity is : {} minutes", vppRestoreDownloadActivityTimeout);
    }

    public void listenForChangeInVppRestoreConfirmActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vppRestoreConfirmActivityTimeout) {
        this.vppRestoreConfirmActivityTimeout = vppRestoreConfirmActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (vppRestoreConfirmActivityTimeout));
        LOGGER.info("Changed timeout value for vPP Restore Job Confirm activity is : {} minutes", vppRestoreConfirmActivityTimeout);
    }

    public void listenForChangeInVsdRestoreActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int vsdRestoreActivityTimeout) {
        this.vsdRestoreActivityTimeout = vsdRestoreActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (vsdRestoreActivityTimeout));
        LOGGER.info("Changed timeout value for vSD Restore Job Restore Backup activity is : {} minutes", vsdRestoreActivityTimeout);
    }

    public void listenForChangeInVsdRestoreDownloadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int vsdRestoreDownloadActivityTimeout) {
        this.vsdRestoreDownloadActivityTimeout = vsdRestoreDownloadActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, (vsdRestoreDownloadActivityTimeout));
        LOGGER.info("Changed timeout value for vSD Restore Job Download activity is : {} minutes", vsdRestoreDownloadActivityTimeout);
    }

    public void listenForChangeInVsdRestoreConfirmActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vsdRestoreConfirmActivityTimeout) {
        this.vsdRestoreConfirmActivityTimeout = vsdRestoreConfirmActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (vsdRestoreConfirmActivityTimeout));
        LOGGER.info("Changed timeout value for vSD Restore Job Confirm activity is : {} minutes", vsdRestoreConfirmActivityTimeout);
    }

    public void listenForChangeInVTFRadioNodeRestoreActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_RESTOREJOB_RESTOREBACKUP_ACTIVITY_TIME_OUT") final int vtfRadioNodeRestoreActivityTimeout) {
        this.vtfRadioNodeRestoreActivityTimeout = vtfRadioNodeRestoreActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, (vtfRadioNodeRestoreActivityTimeout));
        LOGGER.info("Changed timeout value for VTFRadioNode Restore Job Restore Backup activity is : {} minutes", vtfRadioNodeRestoreActivityTimeout);
    }

    public void listenForChangeInVTFRadioNodeRestoreDownloadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_RESTOREJOB_DOWNLOADBACKUP_ACTIVITY_TIME_OUT") final int vtfRadioNodeRestoreDownloadActivityTimeout) {
        this.vtfRadioNodeRestoreDownloadActivityTimeout = vtfRadioNodeRestoreDownloadActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP,
                (vtfRadioNodeRestoreDownloadActivityTimeout));
        LOGGER.info("Changed timeout value for VTFRadioNode Restore Job Download activity is : {} minutes", vtfRadioNodeRestoreDownloadActivityTimeout);
    }

    public void listenForChangeInVTFRadioNodeRestoreConfirmActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_RESTOREJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vtfRadioNodeRestoreConfirmActivityTimeout) {
        this.vtfRadioNodeRestoreConfirmActivityTimeout = vtfRadioNodeRestoreConfirmActivityTimeout;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, (vtfRadioNodeRestoreConfirmActivityTimeout));
        LOGGER.info("Changed timeout value for VTFRadioNode Restore Job Confirm activity is : {} minutes", vtfRadioNodeRestoreConfirmActivityTimeout);
    }

    //Listeners for Polling Wait Times 

    public void listenForErbsRestoreDownloadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsDownloadActivityPollingWaitTime) {
        this.erbsRestoreDownloadActivityPollingWaitTime = erbsDownloadActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsDownloadActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Restore Job Download Backup activity is : {} minutes", erbsDownloadActivityPollingWaitTime);
    }

    public void listenForErbsRestoreVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_RESTOREJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsRestoreVerifyActivityPollingWaitTime) {
        this.erbsRestoreVerifyActivityPollingWaitTime = erbsRestoreVerifyActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling wait Time value for ERBS Restore Job Verify activity is : {} minutes", erbsRestoreVerifyActivityPollingWaitTime);
    }

    public void listenForErbsRestoreInstallActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_RESTOREJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsRestoreInstallActivityPollingWaitTime) {
        this.erbsRestoreInstallActivityPollingWaitTime = erbsRestoreInstallActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreInstallActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Restore Job Install activity is : {} minutes", erbsRestoreInstallActivityPollingWaitTime);
    }

    public void listenForErbsRestoreActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsRestoreActivityPollingWaitTime) {
        this.erbsRestoreActivityPollingWaitTime = erbsRestoreActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Restore Job Restore Backup activity is : {} minutes", erbsRestoreActivityPollingWaitTime);
    }

    public void listenForErbsRestoreConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsRestoreConfirmActivityPollingWaitTime) {
        this.erbsRestoreConfirmActivityPollingWaitTime = erbsRestoreConfirmActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Restore Job Confirm Restore activity is : {} minutes", erbsRestoreConfirmActivityPollingWaitTime);
    }

    public void listenForSgsnRestoreActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnRestoreActivityPollingWaitTime) {
        this.sgsnRestoreActivityPollingWaitTime = sgsnRestoreActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (sgsnRestoreActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Restore Job Restore Backup activity is : {} minutes", sgsnRestoreActivityPollingWaitTime);
    }

    public void listenForRadioNodeRestoreActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeRestoreActivityPollinWaitTime) {
        this.radioNodeRestoreActivityPollinWaitTime = radioNodeRestoreActivityPollinWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (radioNodeRestoreActivityPollinWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Restore Job Restore Backup activity is : {} minutes", radioNodeRestoreActivityPollinWaitTime);
    }

    public void listenForSgsnRestoreDownloadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnRestoreDownloadActivityPollingWaitTime) {
        this.sgsnRestoreDownloadActivityPollingWaitTime = sgsnRestoreDownloadActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (sgsnRestoreDownloadActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Restore Job Download Backup activity is : {} minutes", sgsnRestoreDownloadActivityPollingWaitTime);
    }

    public void listenForRadioNodeRestoreDownloadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeRestoreDownloadActivityPollingWaitTime) {
        this.radioNodeRestoreDownloadActivityPollingWaitTime = radioNodeRestoreDownloadActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (radioNodeRestoreDownloadActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Restore Job Download activity is : {} minutes", radioNodeRestoreDownloadActivityPollingWaitTime);
    }

    public void listenForSgsnRestoreConfirmActivityPollinWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnRestoreConfirmActivityPollingWaitTime) {
        this.sgsnRestoreConfirmActivityPollingWaitTime = sgsnRestoreConfirmActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (sgsnRestoreConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Restore Job Confirm activity is : {} minutes", sgsnRestoreConfirmActivityPollingWaitTime);
    }

    public void listenForRadioNodeRestoreConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeRestoreConfirmActivityPollingWaitTime) {
        this.radioNodeRestoreConfirmActivityPollingWaitTime = radioNodeRestoreConfirmActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (radioNodeRestoreConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Restore Job Confirm activity is : {} minutes", radioNodeRestoreConfirmActivityPollingWaitTime);
    }

    public void listenForCppDownloadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppRestoreDownloadActivityPollingWaitTime) {
        this.cppRestoreDownloadActivityPollingWaitTime = cppRestoreDownloadActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, cppRestoreDownloadActivityPollingWaitTime);
        LOGGER.info("Changed Cpp Restore download Polling Wait Time interval value : {}", cppRestoreDownloadActivityPollingWaitTime);
    }

    public void listenForCppRestoreVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppRestoreVerifyActivityPollingWaitTime) {
        this.cppRestoreVerifyActivityPollingWaitTime = cppRestoreVerifyActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreVerifyActivityPollingWaitTime);
        LOGGER.info("Changed Cpp Restore Verify Polling Wait Time interval value : {}", cppRestoreVerifyActivityPollingWaitTime);
    }

    public void listenForCppRestoreInstallActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppRestoreInstallActivityPollingWaitTime) {
        this.cppRestoreInstallActivityPollingWaitTime = cppRestoreInstallActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreInstallActivityPollingWaitTime);
        LOGGER.info("Changed Cpp Restore Install Polling Wait Time interval value : {}", cppRestoreInstallActivityPollingWaitTime);
    }

    public void listenForCppRestoreActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppRestoreActivityPollingWaitTime) {
        this.cppRestoreActivityPollingWaitTime = cppRestoreActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreActivityPollingWaitTime);
        LOGGER.info("Changed Cpp Restore Polling Wait Time interval value : {}", cppRestoreActivityPollingWaitTime);
    }

    public void listenForCppRestoreConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppRestoreConfirmActivityPollingWaitTime) {
        this.cppRestoreConfirmActivityPollingWaitTime = cppRestoreConfirmActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreConfirmActivityPollingWaitTime);
        LOGGER.info("Changed Cpp Restore Confirm Polling Wait Time interval value : {}", cppRestoreConfirmActivityPollingWaitTime);
    }

    public void listenForEcimRestoreDownloadBackupActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_DOWNLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimRestoreDownloadBackupActivityPollingWaitTime) {
        this.ecimRestoreDownloadBackupActivityPollingWaitTime = ecimRestoreDownloadBackupActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (ecimRestoreDownloadBackupActivityPollingWaitTime));
        LOGGER.info("Changed ecim restore job Download Backup Polling Wait Time  value : {} minutes", ecimRestoreDownloadBackupActivityPollingWaitTime);
    }

    public void listenForEcimRestoreRestoreBackupActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimRestoreRestoreBackupActivityPollingWaitTime) {
        this.ecimRestoreRestoreBackupActivityPollingWaitTime = ecimRestoreRestoreBackupActivityPollingWaitTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (ecimRestoreRestoreBackupActivityPollingWaitTime));
        LOGGER.info("Changed ecim restore job Restore Backup Polling Wait Time value : {} minutes", ecimRestoreRestoreBackupActivityPollingWaitTime);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        if (RESTOREJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return RESTOREJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (RESTOREJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return RESTOREJOB_ACTIVITY_TIMEOUTS.get(platformkey);
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
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, erbsRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, erbsRestoreInstallActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, erbsRestoreVerifyActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, erbsRestoreActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, erbsRestoreConfirmActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, rncRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, rncRestoreInstallActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, rncRestoreActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, mgwRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, mgwRestoreInstallActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, mgwRestoreVerifyActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, mgwRestoreActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, mgwRestoreConfirmActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, mrsRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, mrsRestoreInstallActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, mrsRestoreVerifyActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, mrsRestoreActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, mrsRestoreConfirmActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, sgsnRestoreActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, radioNodeRestoreActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, sgsnRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP,
                radioNodeRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, sgsnRestoreConfirmActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, radioNodeRestoreConfirmActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV, cppRestoreDownloadActivityTimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL, cppRestoreInstallActivityTimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY, cppRestoreVerifyActivityTimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE, cppRestoreActivityTimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, cppRestoreConfirmActivityTimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, ecimRestoreDownloadBackupActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, ecimRestoreRestoreBackupActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD,
                miniLinkIndoorRestoreDownloadActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY,
                miniLinkIndoorRestoreVerifyActivitytimeoutInterval);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE,
                miniLinkIndoorRestoreRestoreActivitytimeoutInterval);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, rnNodeRestoreDownloadActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, rnNodeRestoreActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, rnNodeRestoreConfirmActivityTimeout);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, nrNodeRestoreDownloadActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, nrNodeRestoreActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, nrNodeRestoreConfirmActivityTimeout);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, vrcRestoreDownloadActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, vrcRestoreActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, vrcRestoreConfirmActivityTimeout);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, vppRestoreDownloadActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, vppRestoreActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, vppRestoreConfirmActivityTimeout);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP, vsdRestoreDownloadActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, vsdRestoreActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, vsdRestoreConfirmActivityTimeout);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP,
                vtfRadioNodeRestoreDownloadActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP, vtfRadioNodeRestoreActivityTimeout);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM, vtfRadioNodeRestoreConfirmActivityTimeout);

        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreDownloadActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreVerifyActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreInstallActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsRestoreConfirmActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (sgsnRestoreActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (radioNodeRestoreActivityPollinWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (sgsnRestoreDownloadActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (radioNodeRestoreDownloadActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (sgsnRestoreConfirmActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (radioNodeRestoreConfirmActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_CV + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, cppRestoreDownloadActivityPollingWaitTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreVerifyActivityPollingWaitTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreInstallActivityPollingWaitTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreActivityPollingWaitTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppRestoreConfirmActivityPollingWaitTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + DOWNLOAD_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (ecimRestoreDownloadBackupActivityPollingWaitTime));
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE + DELIMETER_UNDERSCORE + RESTORE_BACKUP + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING, (ecimRestoreRestoreBackupActivityPollingWaitTime));
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
        final Integer activityTimeout = getActivityPollWaitTimeAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(activityTimeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider#getActivityPollWaitTimeAsInteger(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase() + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase() + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING;
        if (RESTOREJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return RESTOREJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (RESTOREJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return RESTOREJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(platform);
    }
}
