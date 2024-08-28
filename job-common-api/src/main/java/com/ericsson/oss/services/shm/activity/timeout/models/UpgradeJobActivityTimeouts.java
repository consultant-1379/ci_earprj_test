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
 * This class is used to listen and update the neType specific timeouts of Upgrade Job
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.UPGRADE)
@SuppressWarnings({ "PMD", "PMD.TooManyFields", "PMD.ExcessivePublicCount", "PMD.TooManyMethods" })
public class UpgradeJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> UPGRADEJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    private static final String INSTALL = "install";
    private static final String VERIFY = "verify";
    private static final String UPGRADE = "upgrade";
    private static final String CONFIRM = "confirm";
    private static final String PREPARE = "prepare";
    private static final String ACTIVATE = "activate";
    private static final String DOWNLOAD = "download";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    private SystemRecorder systemRecorder;

    /*********************** CPP Upgrade Job Install Activity Timeout **************************/
    @Inject
    @Configured(propertyName = "ERBS_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int erbsUpgradeInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MGW_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int mgwUpgradeInstallActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ERBS_UPGRADEJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsUpgradeInstallActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "RNC_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int rncUpgradeInstallActivitytimeoutInterval;

    /*********************** CPP Upgrade Job Verify Activity Timeout **************************/
    @Inject
    @Configured(propertyName = "ERBS_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int erbsUpgradeVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MGW_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int mgwUpgradeVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ERBS_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsUpgradeVerifyActivityPollingWaitTime;

    /*********************** CPP Upgrade Job Upgrade Activity Timeout **************************/
    @Inject
    @Configured(propertyName = "ERBS_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT")
    private int erbsUpgradeUpgradeActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MGW_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT")
    private int mgwUpgradeUpgradeActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ERBS_UPGRADEJOB_UPGRADE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsUpgradeUpgradeActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "RNC_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT")
    private int rncUpgradeUpgradeActivitytimeoutInterval;

    /*********************** CPP Upgrade Job Confirm Activity Timeout **************************/
    @Inject
    @Configured(propertyName = "ERBS_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int erbsUpgradeConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MGW_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int mgwUpgradeConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RBS_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int rbsUpgradeConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RNC_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int rncUpgradeConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ERBS_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsUpgradeConfirmActivityPollingWaitTime;

    /*********************** ECIM Upgrade Job Prepare Activity Timeouts **************************/
    @Inject
    @Configured(propertyName = "SGSN_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int sgsnUpgradePrepareActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int radioNodeUpgradePrepareActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnUpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeUpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "ROUTER6274_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int router6274UpgradePrepareActivitytimeoutInterval;

    /*********************** ECIM Upgrade Job Verify Activity Timeouts **************************/
    @Inject
    @Configured(propertyName = "SGSN_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int sgsnUpgradeVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int radioNodeUpgradeVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeUpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_SGSN_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnUpgradeVerifyActivityPollingWaitTime;

    /*********************** ECIM Upgrade Job Activate Activity Timeouts **************************/
    @Inject
    @Configured(propertyName = "SGSN_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int sgsnUpgradeActivateActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int radioNodeUpgradeActivateActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MSRBS_V1_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int msrbsv1UpgradeActivateActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnUpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeUpgradeActivateActivityPollingWaitTime;

    /*********************** ECIM Upgrade Job Confirm Activity Timeouts **************************/
    @Inject
    @Configured(propertyName = "SGSN_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int sgsnUpgradeConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int radioNodeUpgradeConfirmActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeUpgradeConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_SGSN_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnUpgradeConfirmActivityPollingWaitTime;

    /******************** C P P - U P G R A D E - T I M E O U T S *****************************/

    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT")
    private int cppUpgradeInstallActivitytimeoutInterval;
    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int cppUpgradeVerifyActivitytimeoutInterval;
    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT")
    private int cppUpgradeActivitytimeoutInterval;
    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int cppUpgradeConfirmActivitytimeoutInterval;

    //For Polling 

    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppUpgradeInstallActivityPollingWaitTime;
    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppUpgradeVerifyActivityPollingWaitTime;
    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_UPGRADE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppUpgradeActivityPollingWaitTime;
    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppUpgradeConfirmActivityPollingWaitTime;

    /******************** E C I M - U P G R A D E J O B - T I M E O U T S *****************************/

    /*
     * Confirm Activity uses Default Activity Timeout of 10 minutes.
     */

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int ecimUpgradePrepareActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int ecimUpgradeVerifyActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int ecimUpgradeActivateActivitytimeoutInterval;

    //For Polling Wait Times

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimUpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimUpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimUpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimUpgradeConfirmActivityPollingWaitTime;

    /******************** M I N I - L I N K - U P G R A D E - T I M E O U T S *****************************/

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_DOWNLOAD_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorUpgradeDownloadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorUpgradeActivateActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorUpgradeConfirmActivitytimeoutInterval;

    /******************** V R A N - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "VRAN_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int vranUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "VRAN_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int vranUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "VRAN_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int vranUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "VRAN_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vranUpgradeConfirmActivityTimeout;

    /******************** R N N O D E - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "RNNODE_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int rnNodeUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "RNNODE_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int rnNodeUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "RNNODE_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int rnNodeUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "RNNODE_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int rnNodeUpgradeConfirmActivityTimeout;

    /******************** N R N O D E - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "NSANR_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int nrNodeUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "NSANR_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int nrNodeUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "NSANR_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int nrNodeUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "NSANR_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int nrNodeUpgradeConfirmActivityTimeout;

    /******************** V R C - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "VRC_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int vrcUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "VRC_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int vrcUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "VRC_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int vrcUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "VRC_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vrcUpgradeConfirmActivityTimeout;

    /******************** V P P - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "VPP_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int vppUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "VPP_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int vppUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "VPP_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int vppUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "VPP_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vppUpgradeConfirmActivityTimeout;

    /******************** V S D - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "VSD_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int vsdUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "VSD_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int vsdUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "VSD_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int vsdUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "VSD_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vsdUpgradeConfirmActivityTimeout;

    /******************** Verizon TF RadioNode - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "VTFRADIONODE_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeUpgradePrepareActivityTimeout;

    @Inject
    @Configured(propertyName = "VTFRADIONODE_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeUpgradeVerifyActivityTimeout;

    @Inject
    @Configured(propertyName = "VTFRADIONODE_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeUpgradeActivateActivityTimeout;

    @Inject
    @Configured(propertyName = "VTFRADIONODE_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeUpgradeConfirmActivityTimeout;

    /*********************** Router Upgrade Job Polling Timeouts **************************/
    @Inject
    @Configured(propertyName = "SHM_ROUTER6672_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6672UpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6672_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6672UpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6672_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6672UpgradeConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6672_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6672UpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6675_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6675UpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6675_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6675UpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6675_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6675UpgradeConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6675_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6675UpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6x71UpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6x71UpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6x71UpgradeConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6x71UpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6274_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6274UpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6274_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6274UpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6274_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6274UpgradeConfirmActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6274_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6274UpgradeActivateActivityPollingWaitTime;

    /*********************** Fronthaul Upgrade Job Polling Timeouts **************************/

    @Inject
    @Configured(propertyName = "SHM_FRONTHAUL6080_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int fronthaul6080UpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_FRONTHAUL6080_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int fronthaul6080UpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_FRONTHAUL6080_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int fronthaul6080UpgradeVerifyActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_FRONTHAUL6020_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int fronthaul6020UpgradePrepareActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_FRONTHAUL6020_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int fronthaul6020UpgradeActivateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_FRONTHAUL6020_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int fronthaul6020UpgradeVerifyActivityPollingWaitTime;

    /******************** AXE Node - U P G R A D E - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "AXE_UPGRADE_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES")
    private int axeUpgradeActivityHandleTimeout;

    public void listenForErbsUpgradeInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT") final int erbsInstallActivitytimeoutInterval) {
        this.erbsUpgradeInstallActivitytimeoutInterval = erbsInstallActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, (erbsInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Upgrade Job Install activity is : {} minutes", erbsInstallActivitytimeoutInterval);
    }

    public void listenForMgwUpgradeInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT") final int mgwInstallActivitytimeoutInterval) {
        this.mgwUpgradeInstallActivitytimeoutInterval = mgwInstallActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, (mgwInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Upgrade Job Install activity is : {} minutes", mgwInstallActivitytimeoutInterval);
    }

    public void listenForRncUpgradeInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT") final int rncInstallActivitytimeoutInterval) {
        this.rncUpgradeInstallActivitytimeoutInterval = rncInstallActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, (rncInstallActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RNC Upgrade Job Install activity is : {} minutes", rncInstallActivitytimeoutInterval);
    }

    public void listenForErbsUpgradeVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int erbsVerifyActivitytimeoutInterval) {
        this.erbsUpgradeVerifyActivitytimeoutInterval = erbsVerifyActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, (erbsVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Upgrade Job Verify activity is : {} minutes", erbsVerifyActivitytimeoutInterval);
    }

    public void listenForMgwUpgradeVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int mgwVerifyActivitytimeoutInterval) {
        this.mgwUpgradeVerifyActivitytimeoutInterval = mgwVerifyActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, (mgwVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Upgrade Job Verify activity is : {} minutes", mgwVerifyActivitytimeoutInterval);
    }

    public void listenForErbsUpgradeUpgradeActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT") final int erbsUpgradeActivitytimeoutInterval) {
        this.erbsUpgradeUpgradeActivitytimeoutInterval = erbsUpgradeActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, (erbsUpgradeActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Upgrade Job Upgrade activity is : {} minutes", erbsUpgradeActivitytimeoutInterval);
    }

    public void listenForRncUpgradeUpgradeActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT") final int rncUpgradeActivitytimeoutInterval) {
        this.rncUpgradeUpgradeActivitytimeoutInterval = rncUpgradeActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, (rncUpgradeActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RNC Upgrade Job Upgrade activity is : {} minutes", rncUpgradeActivitytimeoutInterval);
    }

    public void listenForMgwUpgradeUpgradeActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT") final int mgwUpgradeActivitytimeoutInterval) {
        this.mgwUpgradeUpgradeActivitytimeoutInterval = mgwUpgradeActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, (mgwUpgradeActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Upgrade Job Upgrade activity is : {} minutes", mgwUpgradeActivitytimeoutInterval);
    }

    public void listenForErbsUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int erbsConfirmActivitytimeoutInterval) {
        this.erbsUpgradeConfirmActivitytimeoutInterval = erbsConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, (erbsConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for ERBS Upgrade Job Confirm activity is : {} minutes", erbsConfirmActivitytimeoutInterval);
    }

    public void listenForMgwUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int mgwConfirmActivitytimeoutInterval) {
        this.mgwUpgradeConfirmActivitytimeoutInterval = mgwConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, (mgwConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MGW Upgrade Job Confirm activity is : {} minutes", mgwConfirmActivitytimeoutInterval);
    }

    public void listenForRbsUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RBS_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int rbsConfirmActivitytimeoutInterval) {
        this.rbsUpgradeConfirmActivitytimeoutInterval = rbsConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, (rbsConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RBS Upgrade Job Confirm activity is : {} minutes", rbsConfirmActivitytimeoutInterval);
    }

    public void listenForRncUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int rncConfirmActivitytimeoutInterval) {
        this.rncUpgradeConfirmActivitytimeoutInterval = rncConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, (rncConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RNC Upgrade Job Confirm activity is : {} minutes", rncConfirmActivitytimeoutInterval);
    }

    public void listenForSgsnUpgradePrepareActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int sgsnPrepareActivitytimeoutInterval) {
        this.sgsnUpgradePrepareActivitytimeoutInterval = sgsnPrepareActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, (sgsnPrepareActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Upgrade Job Prepare activity is : {} minutes", sgsnPrepareActivitytimeoutInterval);
    }

    public void listenForRadioNodeUpgradePrepareActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int radioNodePrepareActivitytimeoutInterval) {
        this.radioNodeUpgradePrepareActivitytimeoutInterval = radioNodePrepareActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, (radioNodePrepareActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Upgrade Job Prepare activity is : {} minutes", radioNodePrepareActivitytimeoutInterval);
    }

    public void listenForRouter6274UpgradePrepareActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6274_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int router6274PrepareActivitytimeoutInterval) {
        this.router6274UpgradePrepareActivitytimeoutInterval = router6274PrepareActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, router6274PrepareActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6274 Upgrade Job Prepare activity is : {} minutes", router6274PrepareActivitytimeoutInterval);
    }

    public void listenForSgsnUpgradeVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int sgsnVerifyActivitytimeoutInterval) {
        this.sgsnUpgradeVerifyActivitytimeoutInterval = sgsnVerifyActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, (sgsnVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Upgrade Job Verify activity is : {} minutes", sgsnVerifyActivitytimeoutInterval);
    }

    public void listenForRadioNodeUpgradeVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int radioNodeVerifyActivitytimeoutInterval) {
        this.radioNodeUpgradeVerifyActivitytimeoutInterval = radioNodeVerifyActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, (radioNodeVerifyActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Upgrade Job Verify activity is : {} minutes", radioNodeVerifyActivitytimeoutInterval);
    }

    public void listenForSgsnUpgradeActivateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int sgsnActivateActivitytimeoutInterval) {
        this.sgsnUpgradeActivateActivitytimeoutInterval = sgsnActivateActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, (sgsnActivateActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Upgrade Job Activate activity is : {} minutes", sgsnActivateActivitytimeoutInterval);
    }

    public void listenForRadioNodeUpgradeActivateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int radioNodeActivateActivitytimeoutInterval) {
        this.radioNodeUpgradeActivateActivitytimeoutInterval = radioNodeActivateActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, (radioNodeActivateActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Upgrade Job Activate activity is : {} minutes", radioNodeActivateActivitytimeoutInterval);
    }

    public void listenForMSRBSv1UpgradeActivateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MSRBS_V1_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int msrbsv1ActivateActivitytimeoutInterval) {
        this.msrbsv1UpgradeActivateActivitytimeoutInterval = msrbsv1ActivateActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MSRBS_V1.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, (msrbsv1ActivateActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for MSRBS_V1 Upgrade Job Activate activity is : {} minutes", msrbsv1ActivateActivitytimeoutInterval);
    }

    public void listenForSgsnUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int sgsnConfirmActivitytimeoutInterval) {
        this.sgsnUpgradeConfirmActivitytimeoutInterval = sgsnConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, (sgsnConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for SGSN Upgrade Job Confirm activity is : {} minutes", sgsnConfirmActivitytimeoutInterval);
    }

    public void listenForRadioNodeUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int radioNodeConfirmActivitytimeoutInterval) {
        this.radioNodeUpgradeConfirmActivitytimeoutInterval = radioNodeConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, (radioNodeConfirmActivitytimeoutInterval));
        LOGGER.info("Changed timeout value for RadioNode Upgrade Job Confirm activity is : {} minutes", radioNodeConfirmActivitytimeoutInterval);
    }

    public void listenForCppUpgradeInstallActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_INSTALL_ACTIVITY_TIME_OUT") final int installActivitytimeoutInterval) {
        this.cppUpgradeInstallActivitytimeoutInterval = installActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, cppUpgradeInstallActivitytimeoutInterval);
        LOGGER.info("Change in cpp upgrade job install timeout value : {}", cppUpgradeInstallActivitytimeoutInterval);
    }

    public void listenForCppUpgradeVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int verifyActivitytimeoutInterval) {
        this.cppUpgradeVerifyActivitytimeoutInterval = verifyActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, cppUpgradeVerifyActivitytimeoutInterval);
        LOGGER.info("Change in cpp upgrade job verify timeout value : {}", cppUpgradeVerifyActivitytimeoutInterval);
    }

    public void listenForCppUpgradeActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_UPGRADE_ACTIVITY_TIME_OUT") final int upgradeActivitytimeoutInterval) {
        this.cppUpgradeActivitytimeoutInterval = upgradeActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, cppUpgradeActivitytimeoutInterval);
        LOGGER.info("Change in cpp upgrade job upgrade timeout value : {}", cppUpgradeActivitytimeoutInterval);
    }

    public void listenForCppUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int confirmActivitytimeoutInterval) {
        this.cppUpgradeConfirmActivitytimeoutInterval = confirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, cppUpgradeConfirmActivitytimeoutInterval);
        LOGGER.info("Change in cpp upgrade job confirm timeout value : {}", cppUpgradeConfirmActivitytimeoutInterval);
    }

    public void listenForEcimUpgradePrepareActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int ecimUpgradePrepareActivitytimeoutInterval) {
        this.ecimUpgradePrepareActivitytimeoutInterval = ecimUpgradePrepareActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, (ecimUpgradePrepareActivitytimeoutInterval));
        LOGGER.info("Change in ecim upgrade job Prepare timeout value : {} minutes", ecimUpgradePrepareActivitytimeoutInterval);
    }

    public void listenForEcimUpgradeVerifyActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int ecimUpgradeVerifyActivitytimeoutInterval) {
        this.ecimUpgradeVerifyActivitytimeoutInterval = ecimUpgradeVerifyActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, (ecimUpgradeVerifyActivitytimeoutInterval));
        LOGGER.info("Change in ecim upgrade job Verify timeout value : {} minutes", ecimUpgradeVerifyActivitytimeoutInterval);
    }

    public void listenForEcimUpgradeActivateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int ecimUpgradeActivateActivitytimeoutInterval) {
        this.ecimUpgradeActivateActivitytimeoutInterval = ecimUpgradeActivateActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, (ecimUpgradeActivateActivitytimeoutInterval));
        LOGGER.info("Change in ecim upgrade job Activate timeout value : {} minutes", ecimUpgradeActivateActivitytimeoutInterval);
    }

    public void listenForMiniLinkIndoorUpgradeDownloadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_DOWNLOAD_ACTIVITY_TIME_OUT") final int miniLinkIndoorUpgradeDownloadActivitytimeoutInterval) {
        this.miniLinkIndoorUpgradeDownloadActivitytimeoutInterval = miniLinkIndoorUpgradeDownloadActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + DOWNLOAD,
                miniLinkIndoorUpgradeDownloadActivitytimeoutInterval);
        LOGGER.info("Change in MINI-LINK-Indoor upgrade job Download timeout value : {} minutes", miniLinkIndoorUpgradeDownloadActivitytimeoutInterval);
    }

    public void listenForMiniLinkIndoorUpgradeActivateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int miniLinkIndoorUpgradeActivateActivitytimeoutInterval) {
        this.miniLinkIndoorUpgradeActivateActivitytimeoutInterval = miniLinkIndoorUpgradeActivateActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE,
                miniLinkIndoorUpgradeActivateActivitytimeoutInterval);
        LOGGER.info("Change in MINI-LINK-Indoor upgrade job Activate timeout value : {} minutes", miniLinkIndoorUpgradeActivateActivitytimeoutInterval);
    }

    public void listenForMiniLinkIndoorUpgradeConfirmActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int miniLinkIndoorUpgradeConfirmActivitytimeoutInterval) {
        this.miniLinkIndoorUpgradeConfirmActivitytimeoutInterval = miniLinkIndoorUpgradeConfirmActivitytimeoutInterval;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM,
                miniLinkIndoorUpgradeConfirmActivitytimeoutInterval);
        LOGGER.info("Change in MINI-LINK-Indoor upgrade job Confirm timeout value : {} minutes", miniLinkIndoorUpgradeConfirmActivitytimeoutInterval);
    }

    public void listenForVranUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRAN_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int vranUpgradePrepareActivityTimeout) {
        this.vranUpgradePrepareActivityTimeout = vranUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vranUpgradePrepareActivityTimeout);
        LOGGER.info("Change in vRAN upgrade job Prepare activity timeout value : [{}] minutes", this.vranUpgradePrepareActivityTimeout);
    }

    public void listenForVranUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRAN_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int vranUpgradeVerifyActivityTimeout) {
        this.vranUpgradeVerifyActivityTimeout = vranUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vranUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in vRAN upgrade job Verify activity timeout value : [{}] minutes", this.vranUpgradeVerifyActivityTimeout);
    }

    public void listenForVranUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRAN_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int vranUpgradeActivateActivityTimeout) {
        this.vranUpgradeActivateActivityTimeout = vranUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vranUpgradeActivateActivityTimeout);
        LOGGER.info("Change in vRAN upgrade job Activate activity timeout value : [{}] minutes", this.vranUpgradeActivateActivityTimeout);
    }

    public void listenForVranUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRAN_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vranUpgradeConfirmActivityTimeout) {
        this.vranUpgradeConfirmActivityTimeout = vranUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vranUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in vRAN upgrade job Confirm activity timeout value : [{}] minutes", this.vranUpgradeConfirmActivityTimeout);
    }

    public void listenForRnNodeUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int rnNodeUpgradePrepareActivityTimeout) {
        this.rnNodeUpgradePrepareActivityTimeout = rnNodeUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, rnNodeUpgradePrepareActivityTimeout);
        LOGGER.info("Change in RnNode upgrade job Prepare activity timeout value : [{}] minutes", this.rnNodeUpgradePrepareActivityTimeout);
    }

    public void listenForRnNodeUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int rnNodeUpgradeVerifyActivityTimeout) {
        this.rnNodeUpgradeVerifyActivityTimeout = rnNodeUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, rnNodeUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in RnNode upgrade job Verify activity timeout value : [{}] minutes", this.rnNodeUpgradeVerifyActivityTimeout);
    }

    public void listenForRnNodeUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int rnNodeUpgradeActivateActivityTimeout) {
        this.rnNodeUpgradeActivateActivityTimeout = rnNodeUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, rnNodeUpgradeActivateActivityTimeout);
        LOGGER.info("Change in RnNode upgrade job Activate activity timeout value : [{}] minutes", this.rnNodeUpgradeActivateActivityTimeout);
    }

    public void listenForRnNodeUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int rnNodeUpgradeConfirmActivityTimeout) {
        this.rnNodeUpgradeConfirmActivityTimeout = rnNodeUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, rnNodeUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in RnNode upgrade job Confirm activity timeout value : [{}] minutes", this.rnNodeUpgradeConfirmActivityTimeout);
    }

    public void listenFor5GRadioNodeUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int nrNodeUpgradePrepareActivityTimeout) {
        this.nrNodeUpgradePrepareActivityTimeout = nrNodeUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, nrNodeUpgradePrepareActivityTimeout);
        LOGGER.info("Change in 5GRadioNode upgrade job Prepare activity timeout value : [{}] minutes", this.nrNodeUpgradePrepareActivityTimeout);
    }

    public void listenFor5GRadioNodeUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int nrNodeUpgradeVerifyActivityTimeout) {
        this.nrNodeUpgradeVerifyActivityTimeout = nrNodeUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, nrNodeUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in 5GRadioNode upgrade job Verify activity timeout value : [{}] minutes", this.nrNodeUpgradeVerifyActivityTimeout);
    }

    public void listenFor5GRadioNodeUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int nrNodeUpgradeActivateActivityTimeout) {
        this.nrNodeUpgradeActivateActivityTimeout = nrNodeUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, nrNodeUpgradeActivateActivityTimeout);
        LOGGER.info("Change in 5GRadioNode upgrade job Activate activity timeout value : [{}] minutes", this.nrNodeUpgradeActivateActivityTimeout);
    }

    public void listenFor5GRadioNodeUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int nrNodeUpgradeConfirmActivityTimeout) {
        this.nrNodeUpgradeConfirmActivityTimeout = nrNodeUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, nrNodeUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in 5GRadioNode upgrade job Confirm activity timeout value : [{}] minutes", this.nrNodeUpgradeConfirmActivityTimeout);
    }

    public void listenForVRCUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int vrcUpgradePrepareActivityTimeout) {
        this.vrcUpgradePrepareActivityTimeout = vrcUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vrcUpgradePrepareActivityTimeout);
        LOGGER.info("Change in vRC upgrade job Prepare activity timeout value : [{}] minutes", this.vrcUpgradePrepareActivityTimeout);
    }

    public void listenForVRCUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int vrcUpgradeVerifyActivityTimeout) {
        this.vrcUpgradeVerifyActivityTimeout = vrcUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vrcUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in vRC upgrade job Verify activity timeout value : [{}] minutes", this.vrcUpgradeVerifyActivityTimeout);
    }

    public void listenForVRCUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int vrcUpgradeActivateActivityTimeout) {
        this.vrcUpgradeActivateActivityTimeout = vrcUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vrcUpgradeActivateActivityTimeout);
        LOGGER.info("Change in vRC upgrade job Activate activity timeout value : [{}] minutes", this.vrcUpgradeActivateActivityTimeout);
    }

    public void listenForVRCUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vrcUpgradeConfirmActivityTimeout) {
        this.vrcUpgradeConfirmActivityTimeout = vrcUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vrcUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in vRC upgrade job Confirm activity timeout value : [{}] minutes", this.vrcUpgradeConfirmActivityTimeout);
    }

    public void listenForVPPUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int vppUpgradePrepareActivityTimeout) {
        this.vppUpgradePrepareActivityTimeout = vppUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vppUpgradePrepareActivityTimeout);
        LOGGER.info("Change in vPP upgrade job Prepare activity timeout value : [{}] minutes", this.vppUpgradePrepareActivityTimeout);
    }

    public void listenForVPPUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int vppUpgradeVerifyActivityTimeout) {
        this.vppUpgradeVerifyActivityTimeout = vppUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vppUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in vPP upgrade job Verify activity timeout value : [{}] minutes", this.vppUpgradeVerifyActivityTimeout);
    }

    public void listenForVPPUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int vppUpgradeActivateActivityTimeout) {
        this.vppUpgradeActivateActivityTimeout = vppUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vppUpgradeActivateActivityTimeout);
        LOGGER.info("Change in vPP upgrade job Activate activity timeout value : [{}] minutes", this.vppUpgradeActivateActivityTimeout);
    }

    public void listenForVPPUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vppUpgradeConfirmActivityTimeout) {
        this.vppUpgradeConfirmActivityTimeout = vppUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vppUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in vPP upgrade job Confirm activity timeout value : [{}] minutes", this.vppUpgradeConfirmActivityTimeout);
    }

    public void listenForVSDUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int vsdUpgradePrepareActivityTimeout) {
        this.vsdUpgradePrepareActivityTimeout = vsdUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vsdUpgradePrepareActivityTimeout);
        LOGGER.info("Change in vSD upgrade job Prepare activity timeout value : [{}] minutes", this.vsdUpgradePrepareActivityTimeout);
    }

    public void listenForVSDUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int vsdUpgradeVerifyActivityTimeout) {
        this.vsdUpgradeVerifyActivityTimeout = vsdUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vsdUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in vSD upgrade job Verify activity timeout value : [{}] minutes", this.vsdUpgradeVerifyActivityTimeout);
    }

    public void listenForVSDUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int vsdUpgradeActivateActivityTimeout) {
        this.vsdUpgradeActivateActivityTimeout = vsdUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vsdUpgradeActivateActivityTimeout);
        LOGGER.info("Change in vSD upgrade job Activate activity timeout value : [{}] minutes", this.vsdUpgradeActivateActivityTimeout);
    }

    public void listenForVSDUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vsdUpgradeConfirmActivityTimeout) {
        this.vsdUpgradeConfirmActivityTimeout = vsdUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vsdUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in vSD upgrade job Confirm activity timeout value : [{}] minutes", this.vsdUpgradeConfirmActivityTimeout);
    }

    public void listenForVTFRadioNodeUpgradePrepareActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_UPGRADEJOB_PREPARE_ACTIVITY_TIME_OUT") final int vtfRadioNodeUpgradePrepareActivityTimeout) {
        this.vtfRadioNodeUpgradePrepareActivityTimeout = vtfRadioNodeUpgradePrepareActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vtfRadioNodeUpgradePrepareActivityTimeout);
        LOGGER.info("Change in VTFRadioNode upgrade job Prepare activity timeout value : [{}] minutes", this.vtfRadioNodeUpgradePrepareActivityTimeout);
    }

    public void listenForVTFRadioNodeUpgradeVerifyActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_UPGRADEJOB_VERIFY_ACTIVITY_TIME_OUT") final int vtfRadioNodeUpgradeVerifyActivityTimeout) {
        this.vtfRadioNodeUpgradeVerifyActivityTimeout = vtfRadioNodeUpgradeVerifyActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vtfRadioNodeUpgradeVerifyActivityTimeout);
        LOGGER.info("Change in VTFRadioNode upgrade job Verify activity timeout value : [{}] minutes", this.vtfRadioNodeUpgradeVerifyActivityTimeout);
    }

    public void listenForVTFRadioNodeUpgradeActivateActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_UPGRADEJOB_ACTIVATE_ACTIVITY_TIME_OUT") final int vtfRadioNodeUpgradeActivateActivityTimeout) {
        this.vtfRadioNodeUpgradeActivateActivityTimeout = vtfRadioNodeUpgradeActivateActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vtfRadioNodeUpgradeActivateActivityTimeout);
        LOGGER.info("Change in VTFRadioNode upgrade job Activate activity timeout value : [{}] minutes", this.vtfRadioNodeUpgradeActivateActivityTimeout);
    }

    public void listenForVTFRadioNodeUpgradeConfirmActivityTimeoutValue(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_UPGRADEJOB_CONFIRM_ACTIVITY_TIME_OUT") final int vtfRadioNodeUpgradeConfirmActivityTimeout) {
        this.vtfRadioNodeUpgradeConfirmActivityTimeout = vtfRadioNodeUpgradeConfirmActivityTimeout;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vtfRadioNodeUpgradeConfirmActivityTimeout);
        LOGGER.info("Change in VTFRadioNode upgrade job Confirm activity timeout value : [{}] minutes", this.vtfRadioNodeUpgradeConfirmActivityTimeout);
    }

    // For Polling Wait Times 

    public void listenForErbsUpgradeInstallActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_UPGRADEJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsUpgradeInstallActivityPollingWaitTime) {
        this.erbsUpgradeInstallActivityPollingWaitTime = erbsUpgradeInstallActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeInstallActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Upgrade Job Install activity is : {} minutes", erbsUpgradeInstallActivityPollingWaitTime);
    }

    public void listenForErbsUpgradeVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsUpgradeVerifyActivityPollingWaitTime) {
        this.erbsUpgradeVerifyActivityPollingWaitTime = erbsUpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Upgrade Job Verify activity is : {} minutes", erbsUpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForErbsUpgradeUpgradeActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_UPGRADEJOB_UPGRADE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsUpgradeUpgradeActivityPollingWaitTime) {
        this.erbsUpgradeUpgradeActivityPollingWaitTime = erbsUpgradeUpgradeActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeUpgradeActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Upgrade Job Upgrade activity is : {} minutes", erbsUpgradeUpgradeActivityPollingWaitTime);
    }

    public void listenForErbsUpgradeConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsUpgradeConfirmActivityPollingWaitTime) {
        this.erbsUpgradeConfirmActivityPollingWaitTime = erbsUpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for ERBS Upgrade Job Confirm activity is : {} minutes", erbsUpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForSgsnUpgradePrepareActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnUpgradePrepareActivityPollingWaitTime) {
        this.sgsnUpgradePrepareActivityPollingWaitTime = sgsnUpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Upgrade Job Prepare activity is : {} minutes", sgsnUpgradePrepareActivityPollingWaitTime);
    }

    public void listenForRadioNodeUpgradePrepareActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeUpgradePrepareActivityPollingWaitTime) {
        this.radioNodeUpgradePrepareActivityPollingWaitTime = radioNodeUpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Upgrade Job Prepare activity is : {} minutes", radioNodeUpgradePrepareActivityPollingWaitTime);
    }

    public void listenForRadioNodeUpgradeVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeUpgradeVerifyActivityPollingWaitTime) {
        this.radioNodeUpgradeVerifyActivityPollingWaitTime = radioNodeUpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Upgrade Job Verify activity is : {} minutes", radioNodeUpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForSgsnUpgradeVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnUpgradeVerifyActivityPollingWaitTime) {
        this.sgsnUpgradeVerifyActivityPollingWaitTime = sgsnUpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Upgrade Job Verify activity is : {} minutes", sgsnUpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForSgsnUpgradeActivateActivityPollingWaitAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnUpgradeActivateActivityPollingWaitTime) {
        this.sgsnUpgradeActivateActivityPollingWaitTime = sgsnUpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Upgrade Job Activate activity is : {} minutes", sgsnUpgradeActivateActivityPollingWaitTime);
    }

    public void listenForRadioNodeUpgradeActivateActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeUpgradeActivateActivityPollingWaitTime) {
        this.radioNodeUpgradeActivateActivityPollingWaitTime = radioNodeUpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Upgrade Job Activate activity is : {} minutes", radioNodeUpgradeActivateActivityPollingWaitTime);
    }

    public void listenForRadioNodeUpgradeConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeUpgradeConfirmActivityPollingWaitTime) {
        this.radioNodeUpgradeConfirmActivityPollingWaitTime = radioNodeUpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for RadioNode Upgrade Job Confirm activity is : {} minutes", radioNodeUpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForSgsnUpgradeConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnUpgradeConfirmActivityPollingWaitTime) {
        this.sgsnUpgradeConfirmActivityPollingWaitTime = sgsnUpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for SGSN Upgrade Job Confirm activity is : {} minutes", sgsnUpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForCppUpgradeInstallActivityPollinWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_INSTALL_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppUpgradeInstallActivityPollingWaitTime) {
        this.cppUpgradeInstallActivityPollingWaitTime = cppUpgradeInstallActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeInstallActivityPollingWaitTime);
        LOGGER.info("Change in cpp upgrade job install Polling Wait Time value : {}", cppUpgradeInstallActivityPollingWaitTime);
    }

    public void listenForCppUpgradeVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppUpgradeVerifyActivityPollingWaitTime) {
        this.cppUpgradeVerifyActivityPollingWaitTime = cppUpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeVerifyActivityPollingWaitTime);
        LOGGER.info("Change in cpp upgrade job verify Polling Wait Time value : {}", cppUpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForCppUpgradeUpgradeActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_UPGRADE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppUpgradeConfirmActivityPollingWaitTime) {
        this.cppUpgradeConfirmActivityPollingWaitTime = cppUpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeConfirmActivityPollingWaitTime);
        LOGGER.info("Change in cpp upgrade job Upgrade Polling Wait Time value : {}", cppUpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForCppUpgradeConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppUpgradeActivityPollingWaitTime) {
        this.cppUpgradeActivityPollingWaitTime = cppUpgradeActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeActivityPollingWaitTime);
        LOGGER.info("Change in cpp upgrade job confirm Polling Wait Time value : {}", cppUpgradeActivityPollingWaitTime);
    }

    public void listenForEcimUpgradePrepareActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimUpgradePrepareActivityPollingWaitTime) {
        this.ecimUpgradePrepareActivityPollingWaitTime = ecimUpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Change in ecim upgrade job Prepare Polling Wait Time value : {} minutes", ecimUpgradePrepareActivityPollingWaitTime);
    }

    public void listenForEcimUpgradeVerifyActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimUpgradeVerifyActivityPollingWaitTime) {
        this.ecimUpgradeVerifyActivityPollingWaitTime = ecimUpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Change in ecim upgrade job Verify Polling Wait Time value : {} minutes", ecimUpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForEcimUpgradeActivateActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimUpgradeActivateActivityPollingWaitTime) {
        this.ecimUpgradeActivateActivityPollingWaitTime = ecimUpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Change in ecim upgrade job Activate Polling Wait Time value : {} minutes", ecimUpgradeActivateActivityPollingWaitTime);
    }

    public void listenForEcimUpgradeConfirmActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimUpgradeConfirmActivityPollingWaitTime) {
        this.ecimUpgradeConfirmActivityPollingWaitTime = ecimUpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Change in ecim upgrade job Confirm Polling Wait Time value : {} minutes", ecimUpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForRouter6672UpgradeActivateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6672_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6672UpgradeActivateActivityPollingWaitTime) {
        this.router6672UpgradeActivateActivityPollingWaitTime = router6672UpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6672 Upgrade Job Activate activity is : {} minutes",
                router6672UpgradeActivateActivityPollingWaitTime);
    }

    public void listenForRouter6672UpgradeConfirmActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6672_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6672UpgradeConfirmActivityPollingWaitTime) {
        this.router6672UpgradeConfirmActivityPollingWaitTime = router6672UpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6672 Upgrade Job Confirm activity is : {} minutes",
                router6672UpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForRouter6672UpgradePrepareActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6672_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6672UpgradePrepareActivityPollingWaitTime) {
        this.router6672UpgradePrepareActivityPollingWaitTime = router6672UpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6672 Upgrade Job Prepare activity is : {} minutes",
                router6672UpgradePrepareActivityPollingWaitTime);
    }

    public void listenForRouter6672UpgradeVerifyActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6672_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6672UpgradeVerifyActivityPollingWaitTime) {
        this.router6672UpgradeVerifyActivityPollingWaitTime = router6672UpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6672 Upgrade Job Verify activity is : {} minutes",
                router6672UpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForRouter6675UpgradeActivateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6675_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6675UpgradeActivateActivityPollingWaitTime) {
        this.router6675UpgradeActivateActivityPollingWaitTime = router6675UpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6675 Upgrade Job Activate activity is : {} minutes",
                router6675UpgradeActivateActivityPollingWaitTime);
    }

    public void listenForRouter6675UpgradeConfirmActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6675_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6675UpgradeConfirmActivityPollingWaitTime) {
        this.router6675UpgradeConfirmActivityPollingWaitTime = router6675UpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6675 Upgrade Job Confirm activity is : {} minutes",
                router6675UpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForRouter6675UpgradePrepareActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6675_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6675UpgradePrepareActivityPollingWaitTime) {
        this.router6675UpgradePrepareActivityPollingWaitTime = router6675UpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6675 Upgrade Job Prepare activity is : {} minutes",
                router6675UpgradePrepareActivityPollingWaitTime);
    }

    public void listenForRouter6675UpgradeVerifyActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6675_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6675UpgradeVerifyActivityPollingWaitTime) {
        this.router6675UpgradeVerifyActivityPollingWaitTime = router6675UpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6675 Upgrade Job Verify activity is : {} minutes",
                router6675UpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForRouter6x71UpgradeActivateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6x71UpgradeActivateActivityPollingWaitTime) {
        this.router6x71UpgradeActivateActivityPollingWaitTime = router6x71UpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6x71 Upgrade Job Activate activity is : {} minutes",
                router6x71UpgradeActivateActivityPollingWaitTime);
    }

    public void listenForRouter6x71UpgradeConfirmActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6x71UpgradeConfirmActivityPollingWaitTime) {
        this.router6x71UpgradeConfirmActivityPollingWaitTime = router6x71UpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6x71 Upgrade Job Confirm activity is : {} minutes",
                router6x71UpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForRouter6x71UpgradePrepareActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6x71UpgradePrepareActivityPollingWaitTime) {
        this.router6x71UpgradePrepareActivityPollingWaitTime = router6x71UpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6x71 Upgrade Job Prepare activity is : {} minutes",
                router6x71UpgradePrepareActivityPollingWaitTime);
    }

    public void listenForRouter6x71UpgradeVerifyActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6x71_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6x71UpgradeVerifyActivityPollingWaitTime) {
        this.router6x71UpgradeVerifyActivityPollingWaitTime = router6x71UpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6x71 Upgrade Job Verify activity is : {} minutes",
                router6x71UpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForRouter6274UpgradeActivateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6274_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6274UpgradeActivateActivityPollingWaitTime) {
        this.router6274UpgradeActivateActivityPollingWaitTime = router6274UpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradeActivateActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6274 Upgrade Job Activate activity is : {} minutes",
                router6274UpgradeActivateActivityPollingWaitTime);
    }

    public void listenForRouter6274UpgradeConfirmActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6274_UPGRADEJOB_CONFIRM_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6274UpgradeConfirmActivityPollingWaitTime) {
        this.router6274UpgradeConfirmActivityPollingWaitTime = router6274UpgradeConfirmActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradeConfirmActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6274 Upgrade Job Confirm activity is : {} minutes",
                router6274UpgradeConfirmActivityPollingWaitTime);
    }

    public void listenForRouter6274UpgradePrepareActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6274_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6274UpgradePrepareActivityPollingWaitTime) {
        this.router6274UpgradePrepareActivityPollingWaitTime = router6274UpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradePrepareActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6274 Upgrade Job Prepare activity is : {} minutes",
                router6274UpgradePrepareActivityPollingWaitTime);
    }

    public void listenForRouter6274UpgradeVerifyActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6274_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6274UpgradeVerifyActivityPollingWaitTime) {
        this.router6274UpgradeVerifyActivityPollingWaitTime = router6274UpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradeVerifyActivityPollingWaitTime));
        LOGGER.info("Changed Polling Wait Time value for Router6274 Upgrade Job Verify activity is : {} minutes",
                router6274UpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForFronthaul6080UpgradeActivateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_FRONTHAUL6080_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int fronthaul6080UpgradeActivateActivityPollingWaitTime) {
        this.fronthaul6080UpgradeActivateActivityPollingWaitTime = fronthaul6080UpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6080.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, fronthaul6080UpgradeActivateActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Fronthaul6080 Upgrade Job Activate activity is : {} minutes",
                fronthaul6080UpgradeActivateActivityPollingWaitTime);
    }

    public void listenForFronthaul6080UpgradePrepareActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_FRONTHAUL6080_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int fronthaul6080UpgradePrepareActivityPollingWaitTime) {
        this.fronthaul6080UpgradePrepareActivityPollingWaitTime = fronthaul6080UpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6080.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, fronthaul6080UpgradePrepareActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Fronthaul6080 Upgrade Job Prepare activity is : {} minutes",
                fronthaul6080UpgradePrepareActivityPollingWaitTime);
    }

    public void listenForFronthaul6080UpgradeVerifyActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_FRONTHAUL6080_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int fronthaul6080UpgradeVerifyActivityPollingWaitTime) {
        this.fronthaul6080UpgradeVerifyActivityPollingWaitTime = fronthaul6080UpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6080.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, fronthaul6080UpgradeVerifyActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Fronthaul6080 Upgrade Job Verify activity is : {} minutes",
                fronthaul6080UpgradeVerifyActivityPollingWaitTime);
    }

    public void listenForFronthaul6020UpgradeActivateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_FRONTHAUL6020_UPGRADEJOB_ACTIVATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int fronthaul6020UpgradeActivateActivityPollingWaitTime) {
        this.fronthaul6020UpgradeActivateActivityPollingWaitTime = fronthaul6020UpgradeActivateActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6020.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, fronthaul6020UpgradeActivateActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Fronthaul6020 Upgrade Job Activate activity is : {} minutes",
                fronthaul6020UpgradeActivateActivityPollingWaitTime);
    }

    public void listenForFronthaul6020UpgradePrepareActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_FRONTHAUL6020_UPGRADEJOB_PREPARE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int fronthaul6020UpgradePrepareActivityPollingWaitTime) {
        this.fronthaul6020UpgradePrepareActivityPollingWaitTime = fronthaul6020UpgradePrepareActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6020.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, fronthaul6020UpgradePrepareActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Fronthaul6020 Upgrade Job Prepare activity is : {} minutes",
                fronthaul6020UpgradePrepareActivityPollingWaitTime);
    }

    public void listenForFronthaul6020UpgradeVerifyActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_FRONTHAUL6020_UPGRADEJOB_VERIFY_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int fronthaul6020UpgradeVerifyActivityPollingWaitTime) {
        this.fronthaul6020UpgradeVerifyActivityPollingWaitTime = fronthaul6020UpgradeVerifyActivityPollingWaitTime;
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6020.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, fronthaul6020UpgradeVerifyActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Fronthaul6020 Upgrade Job Verify activity is : {} minutes",
                fronthaul6020UpgradeVerifyActivityPollingWaitTime);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        if (UPGRADEJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return UPGRADEJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (UPGRADEJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return UPGRADEJOB_ACTIVITY_TIMEOUTS.get(platformkey);
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
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, erbsUpgradeInstallActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, erbsUpgradeVerifyActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, erbsUpgradeUpgradeActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, erbsUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, mgwUpgradeInstallActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, mgwUpgradeVerifyActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, mgwUpgradeUpgradeActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, mgwUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, rbsUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, rncUpgradeInstallActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, rncUpgradeUpgradeActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, rncUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, sgsnUpgradePrepareActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, radioNodeUpgradePrepareActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, router6274UpgradePrepareActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, sgsnUpgradeVerifyActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, radioNodeUpgradeVerifyActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, sgsnUpgradeActivateActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, radioNodeUpgradeActivateActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.MSRBS_V1.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, msrbsv1UpgradeActivateActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, sgsnUpgradeConfirmActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, radioNodeUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL, cppUpgradeInstallActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, cppUpgradeVerifyActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE, cppUpgradeActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, cppUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, ecimUpgradePrepareActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, ecimUpgradeVerifyActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, ecimUpgradeActivateActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + DOWNLOAD,
                miniLinkIndoorUpgradeDownloadActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE,
                miniLinkIndoorUpgradeActivateActivitytimeoutInterval);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM,
                miniLinkIndoorUpgradeConfirmActivitytimeoutInterval);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vranUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vranUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vranUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vranUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, rnNodeUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, rnNodeUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, rnNodeUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, rnNodeUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, nrNodeUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, nrNodeUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, nrNodeUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, nrNodeUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vrcUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vrcUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vrcUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vrcUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vppUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vppUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vppUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vppUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vsdUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vsdUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vsdUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vsdUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE, vtfRadioNodeUpgradePrepareActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY, vtfRadioNodeUpgradeVerifyActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE, vtfRadioNodeUpgradeActivateActivityTimeout);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM, vtfRadioNodeUpgradeConfirmActivityTimeout);

        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeInstallActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeUpgradeActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (erbsUpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (radioNodeUpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (sgsnUpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + INSTALL + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeInstallActivityPollingWaitTime);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeVerifyActivityPollingWaitTime);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeConfirmActivityPollingWaitTime);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + UPGRADE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppUpgradeActivityPollingWaitTime);
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                (ecimUpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, (axeUpgradeActivityHandleTimeout));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6672UpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6675UpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6x71UpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + CONFIRM
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradeConfirmActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (router6274UpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6020.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (fronthaul6020UpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6020.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (fronthaul6020UpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6020.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (fronthaul6020UpgradeVerifyActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6080.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + ACTIVATE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (fronthaul6080UpgradeActivateActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6080.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + PREPARE
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (fronthaul6080UpgradePrepareActivityPollingWaitTime));
        UPGRADEJOB_ACTIVITY_TIMEOUTS.put(NodeType.FRONTHAUL6080.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE + DELIMETER_UNDERSCORE + VERIFY
                + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, (fronthaul6080UpgradeVerifyActivityPollingWaitTime));
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
        if (UPGRADEJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return UPGRADEJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (UPGRADEJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return UPGRADEJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(platform);
    }
}
