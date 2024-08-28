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
 * This class is used to listen and update the neType specific timeouts of Backup Job
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
@JobTypeAnnotation(jobType = JobType.BACKUP)
@SuppressWarnings({ "PMD.TooManyFields", "PMD.ExcessivePublicCount" })
public class BackupJobActivityTimeouts implements ActivityTimeoutsProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> BACKUPJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<String, Integer>();

    private static final String UPLOAD_CV = "exportcv";
    private static final String CREATE_BACKUP = "createbackup";
    private static final String UPLOAD_BACKUP = "uploadbackup";
    public static final String DELETE_BACKUP = "deletebackup";
    public static final String BACKUP = "backup";
    private static final String DELIMETER_UNDERSCORE = "_";

    @Inject
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Inject
    private SystemRecorder systemRecorder;

    /*********************** ECIM Backup Job Create Backup Activity Timeouts and Polling Configuration **************************/
    @Inject
    @Configured(propertyName = "SGSN_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int sgsnBackupCreateActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int radioNodeBackupCreateActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnBackupCreateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeBackupCreateActivityPollingWaitTime;

    /*********************** CPP Backup Job Upload Activity Timeouts **************************/
    @Inject
    @Configured(propertyName = "RNC_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT")
    private int rncBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ERBS_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT")
    private int erbsBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MGW_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT")
    private int mgwBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "MRS_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT")
    private int mrsBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ERBS_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int erbsBackupUploadActivityPollingWaitTime;

    /*********************** ECIM Backup Job Upload Activity Timeouts **************************/
    @Inject
    @Configured(propertyName = "SGSN_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int sgsnBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "RADIONODE_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int radioNodeBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_SGSN_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int sgsnBackupUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_RADIONODE_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int radioNodeBackupUploadActivityPollingWaitTime;

    /******************** C P P P L A T F O R M - B A C K U P J O B - T I M E O U T S *****************************/

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT")
    private int cppBackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppBackupUploadActivityPollingWaitTime;

    /******************** E C I M P L A T F O R M - B A C K U P J O B - T I M E O U T S *****************************/
    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int ecimCreateBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int ecimUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimCreateBackupActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int ecimUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "ROUTER6672_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int router6672BackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6675_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int router6675BackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6X71_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int router6x71BackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6274_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int router6274BackupUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6672_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int router6672CreateBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6675_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int router6675CreateBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6X71_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int router6x71CreateBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "ROUTER6274_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int router6274CreateBackupActivitytimeoutInterval;

    /******************** MINI-LINK BACKUP TIMEOUTS **************************************************/
    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_BACKUPJOB_BACKUP_ACTIVITY_TIME_OUT")
    private int miniLinkIndoorBackupJobBackupActivitytimeoutInterval;

    /** RNNODE - BACKUP TIMEOUTS **/
    @Inject
    @Configured(propertyName = "RNNODE_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int rnNodeBackupCreateActivityTimeout;

    @Inject
    @Configured(propertyName = "RNNODE_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int rnNodeBackupUploadActivityTimeout;

    /** 5GRadioNode (NR Node) - BACKUP TIMEOUTS **/
    @Inject
    @Configured(propertyName = "NSANR_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int nrNodeBackupCreateActivityTimeout;

    @Inject
    @Configured(propertyName = "NSANR_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int nrNodeBackupUploadActivityTimeout;

    /** VRC - BACKUP TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VRC_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int vrcBackupCreateActivityTimeout;

    @Inject
    @Configured(propertyName = "VRC_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vrcBackupUploadActivityTimeout;

    /** VPP - BACKUP TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VPP_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int vppBackupCreateActivityTimeout;

    @Inject
    @Configured(propertyName = "VPP_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vppBackupUploadActivityTimeout;

    /** VSD - BACKUP TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VSD_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int vsdBackupCreateActivityTimeout;

    @Inject
    @Configured(propertyName = "VSD_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vsdBackupUploadActivityTimeout;

    /** VTFRadioNode - BACKUP TIMEOUTS **/
    @Inject
    @Configured(propertyName = "VTFRADIONODE_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeBackupCreateActivityTimeout;

    @Inject
    @Configured(propertyName = "VTFRADIONODE_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT")
    private int vtfRadioNodeBackupUploadActivityTimeout;

    @Inject
    @Configured(propertyName = "AXE_CREATEBACKUP_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES")
    private int axeCreateBackupActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "AXE_UPLOADBACKUP_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES")
    private int axeUploadActivitytimeoutInterval;

    @Inject
    @Configured(propertyName = "SHM_AXE_BACKUPJOB_CREATEBACKUP_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int axeCreateBackupActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_AXE_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int axeUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6672_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6672BackupUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6672_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6672BackupCreateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6675_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6675BackupUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6675_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6675BackupCreateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6x71_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6x71BackupUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6x71_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6x71BackupCreateActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6274_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6274BackupUploadActivityPollingWaitTime;

    @Inject
    @Configured(propertyName = "SHM_ROUTER6274_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int router6274BackupCreateActivityPollingWaitTime;

    public void listenForSgsnBackupCreateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int sgsnCreateActivitytimeoutInterval) {
        sgsnBackupCreateActivitytimeoutInterval = sgsnCreateActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, sgsnCreateActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for SGSN Backup Job Create Backup activity is : {} minutes", sgsnCreateActivitytimeoutInterval);
    }

    public void listenForRadioNodeBackupCreateActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int radioNodeCreateActivitytimeoutInterval) {
        radioNodeBackupCreateActivitytimeoutInterval = radioNodeCreateActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, radioNodeCreateActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for RadioNode Backup Job Create Backup activity is : {} minutes", radioNodeCreateActivitytimeoutInterval);
    }

    public void listenForRncBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RNC_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT") final int rncUploadActivitytimeoutInterval) {
        rncBackupUploadActivitytimeoutInterval = rncUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, rncUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for RNC Backup Job Upload Backup activity is : {} minutes", rncUploadActivitytimeoutInterval);
    }

    public void listenForErbsBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "ERBS_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT") final int erbsUploadActivitytimeoutInterval) {
        erbsBackupUploadActivitytimeoutInterval = erbsUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, erbsUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for ERBS Backup Job Upload Backup activity is : {} minutes", erbsUploadActivitytimeoutInterval);
    }

    public void listenForMgwBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MGW_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT") final int mgwUploadActivitytimeoutInterval) {
        mgwBackupUploadActivitytimeoutInterval = mgwUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, mgwUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for MGW Backup Job Upload Backup activity is : {} minutes", mgwUploadActivitytimeoutInterval);
    }

    public void listenForMrsBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "MRS_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT") final int mrsUploadActivitytimeoutInterval) {
        mrsBackupUploadActivitytimeoutInterval = mrsUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, mrsUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for MRS Backup Job Upload Backup activity is : {} minutes", mrsUploadActivitytimeoutInterval);
    }

    public void listenForSgsnBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SGSN_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int sgsnUploadActivitytimeoutInterval) {
        sgsnBackupUploadActivitytimeoutInterval = sgsnUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, sgsnUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for SGSN Backup Job Create Upload activity is : {} minutes", sgsnUploadActivitytimeoutInterval);
    }

    public void listenForRadioNodeBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RADIONODE_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int radioNodeUploadActivitytimeoutInterval) {
        radioNodeBackupUploadActivitytimeoutInterval = radioNodeUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, radioNodeUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for RadioNode Backup Job Upload Backup activity is : {} minutes", radioNodeUploadActivitytimeoutInterval);
    }

    public void listenForCppBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPJOB_UPLOAD_ACTIVITY_TIME_OUT") final int uploadActivitytimeoutInterval) {
        cppBackupUploadActivitytimeoutInterval = uploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, cppBackupUploadActivitytimeoutInterval);
        LOGGER.info("Changed action for cpp upload timeout value : {} minutes", cppBackupUploadActivitytimeoutInterval);
    }

    public void listenForEcimCreateBackupActivitytimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int ecimCreateBackupActivitytimeoutInterval) {
        this.ecimCreateBackupActivitytimeoutInterval = ecimCreateBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, ecimCreateBackupActivitytimeoutInterval);
        LOGGER.info("Changed action for ecim upload timeout value : {} minutes", ecimCreateBackupActivitytimeoutInterval);
    }

    public void listenForEcimBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int ecimUploadActivitytimeoutInterval) {
        this.ecimUploadActivitytimeoutInterval = ecimUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, ecimUploadActivitytimeoutInterval);
        LOGGER.info("Changed action for ecim upload timeout value : {} minutes", ecimUploadActivitytimeoutInterval);
    }

    public void listenForMiniLinkIndoorBackupActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_BACKUPJOB_BACKUP_ACTIVITY_TIME_OUT") final int miniLinkIndoorBackupActivitytimeoutInterval) {
        miniLinkIndoorBackupJobBackupActivitytimeoutInterval = miniLinkIndoorBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + BACKUP, miniLinkIndoorBackupActivitytimeoutInterval);
        LOGGER.info("Change in MINI-LINK-Indoor backup job backup timeout value : {} minutes", miniLinkIndoorBackupActivitytimeoutInterval);
    }

    public void listenForChangeInRnNodeBackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int rnNodeBackupCreateActivityTimeout) {
        this.rnNodeBackupCreateActivityTimeout = rnNodeBackupCreateActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, rnNodeBackupCreateActivityTimeout);
        LOGGER.info("Changed timeout value for RnNode Backup Job Create Backup activity is : {} minutes", rnNodeBackupCreateActivityTimeout);
    }

    public void listenForChangeInRouter6672BackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6672_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int router6672CreateBackupActivitytimeoutInterval) {
        this.router6672CreateBackupActivitytimeoutInterval = router6672CreateBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6672CreateBackupActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6672 Backup Job Create Backup activity is : {} minutes", router6672CreateBackupActivitytimeoutInterval);
    }

    public void listenForChangeInRouter6675BackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6675_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int router6675CreateBackupActivitytimeoutInterval) {
        this.router6675CreateBackupActivitytimeoutInterval = router6675CreateBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6675CreateBackupActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6675 Backup Job Create Backup activity is : {} minutes", router6675CreateBackupActivitytimeoutInterval);
    }

    public void listenForChangeInRouter6x71BackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6X71_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int router6x71CreateBackupActivitytimeoutInterval) {
        this.router6x71CreateBackupActivitytimeoutInterval = router6x71CreateBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6x71CreateBackupActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6x71 Backup Job Create Backup activity is : {} minutes", router6x71CreateBackupActivitytimeoutInterval);
    }

    public void listenForChangeInRouter6274BackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6274_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int router6274CreateBackupActivitytimeoutInterval) {
        this.router6274CreateBackupActivitytimeoutInterval = router6274CreateBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6274CreateBackupActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6274 Backup Job Create Backup activity is : {} minutes", router6274CreateBackupActivitytimeoutInterval);
    }

    public void listenForChangeInRnNodeBackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "RNNODE_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int rnNodeBackupUploadActivityTimeout) {
        this.rnNodeBackupUploadActivityTimeout = rnNodeBackupUploadActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, rnNodeBackupUploadActivityTimeout);
        LOGGER.info("Changed timeout value for RnNode Backup Job Upload Backup activity is : {} minutes", rnNodeBackupUploadActivityTimeout);
    }

    public void listenForChangeIn5GRadioNodeBackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int nrNodeBackupCreateActivityTimeout) {
        this.nrNodeBackupCreateActivityTimeout = nrNodeBackupCreateActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, nrNodeBackupCreateActivityTimeout);
        LOGGER.info("Changed timeout value for 5GRadioNode Backup Job Create Backup activity is : {} minutes", nrNodeBackupCreateActivityTimeout);
    }

    public void listenForChangeIn5GRadioNodeBackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "NSANR_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int nrNodeBackupUploadActivityTimeout) {
        this.nrNodeBackupUploadActivityTimeout = nrNodeBackupUploadActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, nrNodeBackupUploadActivityTimeout);
        LOGGER.info("Changed timeout value for 5GRadioNode Backup Job Upload Backup activity is : {} minutes", nrNodeBackupUploadActivityTimeout);
    }

    public void listenForChangeInVrcBackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int vrcBackupCreateActivityTimeout) {
        this.vrcBackupCreateActivityTimeout = vrcBackupCreateActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vrcBackupCreateActivityTimeout);
        LOGGER.info("Changed timeout value for vRC Backup Job Create Backup activity is : {} minutes", vrcBackupCreateActivityTimeout);
    }

    public void listenForChangeInRouter6672BackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6672_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int router6672BackupUploadActivitytimeoutInterval) {
        this.router6672BackupUploadActivitytimeoutInterval = router6672BackupUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6672BackupUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6672 Backup Job Upload Backup activity is : {} minutes", router6672BackupUploadActivitytimeoutInterval);
    }

    public void listenForChangeInRouter6675BackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6675_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int router6675BackupUploadActivitytimeoutInterval) {
        this.router6675BackupUploadActivitytimeoutInterval = router6675BackupUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6675BackupUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6675 Backup Job Upload Backup activity is : {} minutes", router6675BackupUploadActivitytimeoutInterval);
    }

    public void listenForChangeInRouter6x71BackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6X71_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int router6x71BackupUploadActivitytimeoutInterval) {
        this.router6x71BackupUploadActivitytimeoutInterval = router6x71BackupUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6x71BackupUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6x71 Backup Job Upload Backup activity is : {} minutes", router6x71BackupUploadActivitytimeoutInterval);
    }

    public void listenForChangeInRouter6274BackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "ROUTER6274_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int router6274BackupUploadActivitytimeoutInterval) {
        this.router6274BackupUploadActivitytimeoutInterval = router6274BackupUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6274BackupUploadActivitytimeoutInterval);
        LOGGER.info("Changed timeout value for Router6274 Backup Job Upload Backup activity is : {} minutes", router6274BackupUploadActivitytimeoutInterval);
    }

    public void listenForChangeInVrcBackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VRC_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int vrcBackupUploadActivityTimeout) {
        this.vrcBackupUploadActivityTimeout = vrcBackupUploadActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vrcBackupUploadActivityTimeout);
        LOGGER.info("Changed timeout value for vRC Backup Job Upload Backup activity is : {} minutes", vrcBackupUploadActivityTimeout);
    }

    public void listenForChangeInVppBackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int vppBackupCreateActivityTimeout) {
        this.vppBackupCreateActivityTimeout = vppBackupCreateActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vppBackupCreateActivityTimeout);
        LOGGER.info("Changed timeout value for vPP Backup Job Create Backup activity is : {} minutes", vppBackupCreateActivityTimeout);
    }

    public void listenForChangeInVppBackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VPP_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int vppBackupUploadActivityTimeout) {
        this.vppBackupUploadActivityTimeout = vppBackupUploadActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vppBackupUploadActivityTimeout);
        LOGGER.info("Changed timeout value for vPP Backup Job Upload Backup activity is : {} minutes", vppBackupUploadActivityTimeout);
    }

    public void listenForChangeInVsdBackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int vsdBackupCreateActivityTimeout) {
        this.vsdBackupCreateActivityTimeout = vsdBackupCreateActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vsdBackupCreateActivityTimeout);
        LOGGER.info("Changed timeout value for vSD Backup Job Create Backup activity is : {} minutes", vsdBackupCreateActivityTimeout);
    }

    public void listenForChangeInVsdBackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VSD_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int vsdBackupUploadActivityTimeout) {
        this.vsdBackupUploadActivityTimeout = vsdBackupUploadActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vsdBackupUploadActivityTimeout);
        LOGGER.info("Changed timeout value for vSD Backup Job Upload Backup activity is : {} minutes", vsdBackupUploadActivityTimeout);
    }

    public void listenForChangeInVTFRadioNodeBackupCreateActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_BACKUPJOB_CREATEBACKUP_ACTIVITY_TIME_OUT") final int vtfRadioNodeBackupCreateActivityTimeout) {
        this.vtfRadioNodeBackupCreateActivityTimeout = vtfRadioNodeBackupCreateActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vtfRadioNodeBackupCreateActivityTimeout);
        LOGGER.info("Changed timeout value for VTFRadioNode Backup Job Create Backup activity is : {} minutes", vtfRadioNodeBackupCreateActivityTimeout);
    }

    public void listenForChangeInVTFRadioNodeBackupUploadActivityTimeout(
            @Observes @ConfigurationChangeNotification(propertyName = "VTFRADIONODE_BACKUPJOB_UPLOADBACKUP_ACTIVITY_TIME_OUT") final int vtfRadioNodeBackupUploadActivityTimeout) {
        this.vtfRadioNodeBackupUploadActivityTimeout = vtfRadioNodeBackupUploadActivityTimeout;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vtfRadioNodeBackupUploadActivityTimeout);
        LOGGER.info("Changed timeout value for VTFRadioNode Backup Job Upload Backup activity is : {} minutes", vtfRadioNodeBackupUploadActivityTimeout);
    }

    //Listeners For polling Best Times, implemented for TORF - 237622
    public void listenForSgsnBackupCreateActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnCreateActivityPollinWaitTime) {
        sgsnBackupCreateActivityPollingWaitTime = sgsnCreateActivityPollinWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                sgsnCreateActivityPollinWaitTime);
        LOGGER.info("Changed Polling Wait Time value for SGSN Backup Job Create Backup activity is : {} minutes", sgsnCreateActivityPollinWaitTime);
    }

    public void listenForRadioNodeBackupCreateActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeCreateActivityPollinWaitTime) {
        radioNodeBackupCreateActivityPollingWaitTime = radioNodeCreateActivityPollinWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radioNodeCreateActivityPollinWaitTime);
        LOGGER.info("Changed Polling Wait Time value for RADIO Node Backup Job Create Backup activity is : {} minutes", radioNodeCreateActivityPollinWaitTime);
    }

    public void listenForErbsBackupUploadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ERBS_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int erbsUploadActivityPollingWaitTime) {
        erbsBackupUploadActivityPollingWaitTime = erbsUploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                erbsUploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for ERBS Backup Job Upload Backup activity is : {} minutes", erbsUploadActivityPollingWaitTime);
    }

    public void listenForSgsnBackupUploadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_SGSN_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int sgsnUploadActivityPollingWaitTime) {
        sgsnBackupUploadActivityPollingWaitTime = sgsnUploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                sgsnUploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for SGSN Backup Job Create Upload activity is : {} minutes", sgsnUploadActivityPollingWaitTime);
    }

    public void listenForRadioNodeBackupUploadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_RADIONODE_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int radioNodeUploadActivityPollingWaitTime) {
        radioNodeBackupUploadActivityPollingWaitTime = radioNodeUploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radioNodeUploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Radio Node Backup Job Create Upload activity is : {} minutes", radioNodeUploadActivityPollingWaitTime);
    }

    public void listenForCppBackupUploadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int uploadActivityPollingWaitTime) {
        cppBackupUploadActivityPollingWaitTime = uploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppBackupUploadActivityPollingWaitTime);
        LOGGER.info("Changed action for cpp upload Polling Wait time value : {} minutes", cppBackupUploadActivityPollingWaitTime);
    }

    public void listenForEcimCreateBackupActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimCreateBackupActivityPollingWaitTime) {
        this.ecimCreateBackupActivityPollingWaitTime = ecimCreateBackupActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimCreateBackupActivityPollingWaitTime);
        LOGGER.info("Changed action for ecim Backup Job Create activity Polling Wait Time value : {} minutes", ecimCreateBackupActivityPollingWaitTime);
    }

    public void listenForEcimBackupUploadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int ecimUploadActivityPollingWaitTime) {
        this.ecimUploadActivityPollingWaitTime = ecimUploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimUploadActivityPollingWaitTime);
        LOGGER.info("Changed action for ecim upload Polling Wait Time value : {} minutes", ecimUploadActivityPollingWaitTime);
    }

    public void listenForAxeCreateBackupActivitytimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "AXE_CREATEBACKUP_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES") final int axeCreateBackupActivitytimeoutInterval) {
        this.axeCreateBackupActivitytimeoutInterval = axeCreateBackupActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, axeCreateBackupActivitytimeoutInterval);
        LOGGER.info("Changed action for axe upload timeout value : {} minutes", axeCreateBackupActivitytimeoutInterval);
    }

    public void listenForAxeBackupUploadActivityTimeoutAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "AXE_UPLOADBACKUP_ACTIVITY_HANDLE_TIMEOUT_IN_MINUTES") final int axeUploadActivitytimeoutInterval) {
        this.ecimUploadActivitytimeoutInterval = axeUploadActivitytimeoutInterval;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, axeUploadActivitytimeoutInterval);
        LOGGER.info("Changed action for axe upload timeout value : {} minutes", axeUploadActivitytimeoutInterval);
    }

    public void listenForAxeCreateBackupActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_BACKUPJOB_CREATEBACKUP_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int axeCreateBackupActivityPollingWaitTime) {
        this.axeCreateBackupActivityPollingWaitTime = axeCreateBackupActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                axeCreateBackupActivityPollingWaitTime);
        LOGGER.info("Changed action for ecim Backup Job Create activity Polling Wait Time value : {} minutes", axeCreateBackupActivityPollingWaitTime);
    }

    public void listenForAxeBackupUploadActivityPollingWaitTimeAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int axeUploadActivityPollingWaitTime) {
        this.axeUploadActivityPollingWaitTime = axeUploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                axeUploadActivityPollingWaitTime);
        LOGGER.info("Changed action for ecim upload Polling Wait Time value : {} minutes", axeUploadActivityPollingWaitTime);
    }

    //Listeners For polling Best Times for Router nodes
    public void listenForRouter6672BackupCreateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6672_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6672CreateActivityPollingWaitTime) {
        router6672BackupCreateActivityPollingWaitTime = router6672CreateActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6672CreateActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6672 Backup Job Create Backup activity is : {} minutes",
                router6672CreateActivityPollingWaitTime);
    }

    public void listenForRouter6672BackupUploaadActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6672_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6672UploadActivityPollingWaitTime) {
        router6672BackupUploadActivityPollingWaitTime = router6672UploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6672UploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6672 Backup Job Upload Backup activity is : {} minutes",
                router6672UploadActivityPollingWaitTime);
    }

    public void listenForRouter6675BackupCreateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6675_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6675CreateActivityPollingWaitTime) {
        router6675BackupCreateActivityPollingWaitTime = router6675CreateActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6675CreateActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6675 Backup Job Create Backup activity is : {} minutes",
                router6675CreateActivityPollingWaitTime);
    }

    public void listenForRouter6675BackupUploaadActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6675_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6675UploadActivityPollingWaitTime) {
        router6675BackupUploadActivityPollingWaitTime = router6675UploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6675UploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6675 Backup Job Upload Backup activity is : {} minutes",
                router6675UploadActivityPollingWaitTime);
    }

    public void listenForRouter6x71BackupCreateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6x71_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6x71CreateActivityPollingWaitTime) {
        router6x71BackupCreateActivityPollingWaitTime = router6x71CreateActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6x71CreateActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6x71 Backup Job Create Backup activity is : {} minutes",
                router6x71CreateActivityPollingWaitTime);
    }

    public void listenForRouter6x71BackupUploaadActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6x71_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6x71UploadActivityPollingWaitTime) {
        router6x71BackupUploadActivityPollingWaitTime = router6x71UploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6x71UploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6x71 Backup Job Upload Backup activity is : {} minutes",
                router6x71UploadActivityPollingWaitTime);
    }

    public void listenForRouter6274BackupCreateActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6274_BACKUPJOB_CREATE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6274CreateActivityPollingWaitTime) {
        router6274BackupCreateActivityPollingWaitTime = router6274CreateActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6274CreateActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6274 Backup Job Create Backup activity is : {} minutes",
                router6274CreateActivityPollingWaitTime);
    }

    public void listenForRouter6274BackupUploaadActivityPollingWaitTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_ROUTER6274_BACKUPJOB_UPLOAD_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int router6274UploadActivityPollingWaitTime) {
        router6274BackupUploadActivityPollingWaitTime = router6274UploadActivityPollingWaitTime;
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6274UploadActivityPollingWaitTime);
        LOGGER.info("Changed Polling Wait Time value for Router6274 Backup Job Upload Backup activity is : {} minutes",
                router6274UploadActivityPollingWaitTime);
    }

    @Override
    public Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        if (BACKUPJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return BACKUPJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (BACKUPJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return BACKUPJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityTimeoutBasedOnPlatform(platform);
    }

    @Override
    public Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName) {
        final String key = neType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase() + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING;
        final String platformkey = platform.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase() + DELIMETER_UNDERSCORE
                + ActivityTimeoutConstants.POLLING;
        if (BACKUPJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return BACKUPJOB_ACTIVITY_TIMEOUTS.get(key);
        } else if (BACKUPJOB_ACTIVITY_TIMEOUTS.containsKey(platformkey)) {
            return BACKUPJOB_ACTIVITY_TIMEOUTS.get(platformkey);
        }
        return shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(platform);
    }

    @Override
    public String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName) {
        final Integer activityTimeout = getActivityPollWaitTimeAsInteger(neType, platformTypeEnum.toString(), jobTypeEnum.toString(), activityName);
        return convertToIsoFormat(activityTimeout);
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
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, sgsnBackupCreateActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, radioNodeBackupCreateActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, erbsBackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNC + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, rncBackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, sgsnBackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, radioNodeBackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, mgwBackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, mrsBackupUploadActivitytimeoutInterval);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV, cppBackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, ecimCreateBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, ecimUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + BACKUP,
                miniLinkIndoorBackupJobBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vrcBackupCreateActivityTimeout);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VRC.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vrcBackupUploadActivityTimeout);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vppBackupCreateActivityTimeout);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VPP.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vppBackupUploadActivityTimeout);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vsdBackupCreateActivityTimeout);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VSD.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vsdBackupUploadActivityTimeout);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, rnNodeBackupCreateActivityTimeout);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.RNNODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, rnNodeBackupUploadActivityTimeout);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, nrNodeBackupCreateActivityTimeout);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.NSANR.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, nrNodeBackupUploadActivityTimeout);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, vtfRadioNodeBackupCreateActivityTimeout);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.VTFRADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, vtfRadioNodeBackupUploadActivityTimeout);

        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6672BackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6675BackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6x71BackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP,
                router6274BackupUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6672CreateBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6675CreateBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6x71CreateBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP,
                router6274CreateBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                sgsnBackupCreateActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radioNodeBackupCreateActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                erbsBackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.SGSN_MME.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                sgsnBackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                NodeType.RADIONODE.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                radioNodeBackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_CV + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                cppBackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimCreateBackupActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                ecimUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP, axeCreateBackupActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP, axeUploadActivitytimeoutInterval);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                axeCreateBackupActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(
                PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING,
                axeUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6672BackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6672.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6672BackupCreateActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6675BackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6675.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6675BackupCreateActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6x71BackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6X71.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6x71BackupCreateActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + UPLOAD_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6274BackupUploadActivityPollingWaitTime);
        BACKUPJOB_ACTIVITY_TIMEOUTS.put(NodeType.ROUTER6274.getName() + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP + DELIMETER_UNDERSCORE
                + CREATE_BACKUP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.POLLING, router6274BackupCreateActivityPollingWaitTime);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

}
