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
package com.ericsson.oss.services.shm.loadcontrol.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.loadcontrol.instrumentation.LoadControlInstrumentationBean;
import com.ericsson.oss.services.shm.loadcontrol.local.api.PrepareLoadControllerLocalCounterService;

@SuppressWarnings({ "PMD.TooManyFields", "PMD.TooManyMethods" })
@ApplicationScoped
public class ConfigurationParamProvider implements PrepareLoadControllerLocalCounterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParamProvider.class);

    private static final String CPP_NODE_RESTART = "CPPNODERESTARTmanualrestart";
    private static final String CPP_DELETE_UPGRADEPACKAGE = "CPPDELETE_UPGRADEPACKAGEdeleteupgradepackage";
    private static final String ECIM_DELETE_UPGRADEPACKAGE = "ECIMDELETE_UPGRADEPACKAGEdeleteupgradepackage";
    private static final String VRAN_UPGRADE_PREPARE = "vRANUPGRADEprepare";
    private static final String VRAN_UPGRADE_VERIFY = "vRANUPGRADEverify";
    private static final String VRAN_UPGRADE_ACTIVATE = "vRANUPGRADEactivate";
    private static final String VRAN_UPGRADE_CONFIRM = "vRANUPGRADEconfirm";
    private static final String VRAN_ONBOARD_ONBOARD = "vRANONBOARDonboard";
    private static final String VRAN_DELETE_SOFTWAREPACKAGE_DELETE_SOFTWAREPACKAGE = "vRANDELETE_SOFTWAREPACKAGEdelete_softwarepackage";
    private static final String STN_UPGRADE_INSTALL = "STNUPGRADEinstall";
    private static final String STN_UPGRADE_UPGRADE = "STNUPGRADEupgrade";
    private static final String STN_UPGRADE_APPROVESW = "STNUPGRADEapprovesw";
    private static final String STN_UPGRADE_SWADJUST = "STNUPGRADEswadjust";
    private static final String STAGED_ACTIVITIES_BATCH_SIZE = "staged_activities_batch_size";
    private static final String AXE_BACKUP_CREATEBACKUP = "AXEBACKUPcreatebackup";
    private static final String AXE_BACKUP_UPLOADBACKUP = "AXEBACKUPuploadbackup";
    private static final String AXE_DELETE_BACKUP_DELETEBACKUP = "AXEDELETEBACKUPdeletebackup";
    private static final String AXE_INSTALL_LICENSE = "AXELICENSEinstall";
    private static final String AXE_UPGRADE_ACTIVITY = "AXEUPGRADEaxeActivity";
    private static final String ECIM_LICENSE_REFRESH_REFRESH = "ECIMLICENSE_REFRESHrefresh";
    private static final String ECIM_LICENSE_REFRESH_REQUEST = "ECIMLICENSE_REFRESHrequest";
    private static final String ECIM_LICENSE_REFRESH_INSTALL = "ECIMLICENSE_REFRESHinstall";

    // Configured parameters for Max count of upgarde, backup and license Job activities
    // These parameter are configured through LITP CLI at runtime

    @Inject
    private LoadControlCounterManager loadControlCounterManager;

    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long installActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_VERIFY_ACTIVITY_MAX_COUNT")
    private long vefifyActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_UPGRADE_ACTIVITY_MAX_COUNT")
    private long upgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT")
    private long confirmActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPJOB_CREATE_CV_ACTIVITY_MAX_COUNT")
    private long creatCVActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPJOB_UPLOAD_CV_ACTIVITY_MAX_COUNT")
    private long exportCVActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPJOB_SET_AS_STARTABLE_ACTIVITY_MAX_COUNT")
    private long setStartableActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUPJOB_SET_AS_FIRST_IN_ROLLBACKLIST_ACTIVITY_MAX_COUNT")
    private long setAsFirstActvityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long licenseInstallActvityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_DELETEBACKUPJOB_DELETE_ACTIVITY_MAX_COUNT")
    private long deleteBackupActvityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_DOWNLOAD_ACTIVITY_MAX_COUNT")
    private long downloadRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_VERIFY_ACTIVITY_MAX_COUNT")
    private long vefifyRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long installRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_RESTORE_ACTIVITY_MAX_COUNT")
    private long restoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_RESTOREJOB_CONFIRM_ACTIVITY_MAX_COUNT")
    private long confirmRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_BACKUP_HOUSEKEEPINGJOB_CLEANCV_ACTIVITY_MAX_COUNT")
    private long cppBackupHousekeepingCleancvActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_NODERESTARTJOB_MANUALRESTART_ACTIVITY_MAX_COUNT")
    private long manualrestartNoderestartActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUPJOB_CREATE_BACKUP_ACTIVITY_MAX_COUNT")
    private long createBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUPJOB_UPLOAD_BACKUP_ACTIVITY_MAX_COUNT")
    private long uploadBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_DELETEBACKUPJOB_DELETE_ACTIVITY_MAX_COUNT")
    private long ecimDeleteBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_PREPARE_ACTIVITY_MAX_COUNT")
    private long prepareUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_VERIFY_ACTIVITY_MAX_COUNT")
    private long verifyUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_ACTIVATE_ACTIVITY_MAX_COUNT")
    private long activateUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT")
    private long confirmUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long installLicenseActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_DOWNLOAD_BACKUP_ACTIVITY_MAX_COUNT")
    private long downloadBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_RESTORE_BACKUP_ACTIVITY_MAX_COUNT")
    private long restoreBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_RESTOREJOB_CONFIRM_BACKUP_ACTIVITY_MAX_COUNT")
    private long confirmBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_BACKUP_HOUSEKEEPINGJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT")
    private long ecimBackupHousekeepingDeletebackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REFRESH_ACTIVITY_MAX_COUNT")
    private long ecimLicenseRefreshJobRefreshActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REQUEST_ACTIVITY_MAX_COUNT")
    private long ecimLicenseRefreshJobRequestActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long ecimLicenseRefreshJobInstallActivityMaxCount;

    @Inject
    @Configured(propertyName = "ecimNodeHealthCheckMaxActivityCount")
    private long ecimNodeHealthCheckMaxActivityCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_DOWNLOAD_ACTIVITY_MAX_COUNT")
    private long downloadMiniLinkIndoorUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_ACTIVATE_ACTIVITY_MAX_COUNT")
    private long activateMiniLinkIndoorUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT")
    private long confirmMiniLinkIndoorUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_BACKUPJOB_BACKUP_ACTIVITY_MAX_COUNT")
    private long backupMiniLinkIndoorBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_DOWNLOAD_ACTIVITY_MAX_COUNT")
    private long downloadMiniLinkIndoorRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_VERIFY_ACTIVITY_MAX_COUNT")
    private long verifyMiniLinkIndoorRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_RESTOREJOB_RESTORE_ACTIVITY_MAX_COUNT")
    private long restoreMiniLinkIndoorRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT")
    private long deletebackupMiniLinkIndoorDeleteBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_INDOOR_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long installLicenseKeyFileActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_BACKUPJOB_BACKUP_ACTIVITY_MAX_COUNT")
    private long backupMiniLinkOutdoorBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long minilinkoutdoorinstallLicenseKeyFileActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_RESTOREJOB_RESTORE_ACTIVITY_MAX_COUNT")
    private long restoreMiniLinkOutdoorRestoreActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_UPGRADEJOB_DOWNLOAD_ACTIVITY_MAX_COUNT")
    private long downloadMiniLinkOutdoorUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_UPGRADEJOB_ACTIVATE_ACTIVITY_MAX_COUNT")
    private long activateMiniLinkOutdoorUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT")
    private long confirmMiniLinkOutdoorUpgradeActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_MINI_LINK_OUTDOOR_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT")
    private long deletebackupMiniLinkOutdoorDeleteBackupActivityMaxCount;

    private final Map<String, Long> thresholdValueOfCounterMap = new HashMap<String, Long>();

    @Inject
    @Configured(propertyName = "loadControlQueueConsumerTimeout_ms")
    private long loadControlQueueConsumerTimeout_ms;

    @Inject
    @Configured(propertyName = "SHM_VRAN_UPGRADEJOB_PREPARE_ACTIVITY_MAX_COUNT")
    private long vranUpgradePrepareActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_VRAN_UPGRADEJOB_VERIFY_ACTIVITY_MAX_COUNT")
    private long vranUpgradeVerifyActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_VRAN_UPGRADEJOB_ACTIVATE_ACTIVITY_MAX_COUNT")
    private long vranUpgradeActivateActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_VRAN_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT")
    private long vranUpgradeConfirmActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_VRAN_ONBOARDJOB_ONBOARD_ACTIVITY_MAX_COUNT")
    private long vranOnboardJobOnboardActivityMaxCount;
    //STN Nodes

    @Inject
    @Configured(propertyName = "SHM_STN_UPGRADEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    Long stnUpgradeDownloadActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_STN_UPGRADEJOB_UPGRADE_ACTIVITY_MAX_COUNT")
    Long stnUpgradeActivateActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_STN_UPGRADEJOB_APPROVESW_ACTIVITY_MAX_COUNT")
    Long stnUpgradeApproveActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_STN_UPGRADEJOB_SWADJUST_ACTIVITY_MAX_COUNT")
    Long stnUpgradeSwAdjustActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_CPP_DELETE_UPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_MAX_COUNT")
    private long deleteupgradepackageCppDeleteupgradepackageActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_ECIM_DELETE_UPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_MAX_COUNT")
    private long deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount;

    @Inject
    private LoadControlInstrumentationBean loadControlInstrumentationBean;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    @Configured(propertyName = "SHM_VRAN_DELETE_SOFTWAREPACKAGE_DELETE_SOFTWAREPACKAGE_ACTIVITY_MAX_COUNT")
    private long vranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount;

    @Inject
    @Configured(propertyName = STAGED_ACTIVITIES_BATCH_SIZE)
    private int stagedactivitiesBatchSize;

    @Inject
    @Configured(propertyName = "RESET_SHM_LC_CURRENT_VALUE")
    private boolean resetShmLcCurrentValue;

    @Inject
    @Configured(propertyName = "SHM_AXE_BACKUPJOB_CREATE_BACKUP_ACTIVITY_MAX_COUNT")
    private long shmAxeBackupjobCreateBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_AXE_BACKUPJOB_UPLOAD_BACKUP_ACTIVITY_MAX_COUNT")
    private long shmAxeBackupjobUploadBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_AXE_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT")
    private long shmAxeDeleteBackupJobdeleteBackupActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_AXE_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT")
    private long shmAxeLicenseInstalljobActivityMaxCount;

    @Inject
    @Configured(propertyName = "SHM_AXE_UPGRADEJOB_ACTIVITY_MAX_COUNT")
    private long shmAxeUpgradejobActivityMaxCount;

    @Override
    public void prepareMaxCountMap(final int membersCount) {
        updateThresholdCounters("CPPUPGRADEinstall", getLoadPerMember(installActivityMaxCount, membersCount));
        updateThresholdCounters("CPPUPGRADEverify", getLoadPerMember(vefifyActivityMaxCount, membersCount));
        updateThresholdCounters("CPPUPGRADEupgrade", getLoadPerMember(upgradeActivityMaxCount, membersCount));
        updateThresholdCounters("CPPUPGRADEconfirm", getLoadPerMember(confirmActivityMaxCount, membersCount));

        updateThresholdCounters("CPPBACKUPcreatecv", getLoadPerMember(creatCVActivityMaxCount, membersCount));
        updateThresholdCounters("CPPBACKUPexportcv", getLoadPerMember(exportCVActivityMaxCount, membersCount));
        updateThresholdCounters("CPPBACKUPsetcvasstartable", getLoadPerMember(setStartableActivityMaxCount, membersCount));
        updateThresholdCounters("CPPBACKUPsetcvfirstinrollbacklist", getLoadPerMember(setAsFirstActvityMaxCount, membersCount));

        updateThresholdCounters("CPPDELETEBACKUPdeletecv", getLoadPerMember(deleteBackupActvityMaxCount, membersCount));

        updateThresholdCounters("CPPLICENSEinstall", getLoadPerMember(licenseInstallActvityMaxCount, membersCount));

        updateThresholdCounters("CPPRESTOREdownload", getLoadPerMember(downloadRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("CPPRESTOREverify", getLoadPerMember(vefifyRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("CPPRESTOREinstall", getLoadPerMember(installRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("CPPRESTORErestore", getLoadPerMember(restoreActivityMaxCount, membersCount));
        updateThresholdCounters("CPPRESTOREconfirm", getLoadPerMember(confirmRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("CPPBACKUP_HOUSEKEEPINGcleancv", getLoadPerMember(cppBackupHousekeepingCleancvActivityMaxCount, membersCount));
        updateThresholdCounters("CPPNODE_HEALTH_CHECKenmhealthcheck", getLoadPerMember(ecimNodeHealthCheckMaxActivityCount, membersCount));
        updateThresholdCounters("CPPNODE_HEALTH_CHECKnodehealthcheck", getLoadPerMember(ecimNodeHealthCheckMaxActivityCount, membersCount));
        updateThresholdCounters(CPP_NODE_RESTART, getLoadPerMember(manualrestartNoderestartActivityMaxCount, membersCount));

        updateThresholdCounters(CPP_DELETE_UPGRADEPACKAGE, getLoadPerMember(deleteupgradepackageCppDeleteupgradepackageActivityMaxCount, membersCount));
        updateThresholdCounters(ECIM_DELETE_UPGRADEPACKAGE, getLoadPerMember(deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount, membersCount));

        updateThresholdCounters("ECIMBACKUPcreatebackup", getLoadPerMember(createBackupActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMBACKUPuploadbackup", getLoadPerMember(uploadBackupActivityMaxCount, membersCount));

        updateThresholdCounters("ECIMUPGRADEprepare", getLoadPerMember(prepareUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMUPGRADEverify", getLoadPerMember(verifyUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMUPGRADEactivate", getLoadPerMember(activateUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMUPGRADEconfirm", getLoadPerMember(confirmUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMDELETEBACKUPdeletebackup", getLoadPerMember(ecimDeleteBackupActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMLICENSEinstall", getLoadPerMember(installLicenseActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMRESTOREdownloadbackup", getLoadPerMember(downloadBackupActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMRESTORErestorebackup", getLoadPerMember(restoreBackupActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMRESTOREconfirmbackup", getLoadPerMember(confirmBackupActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMBACKUP_HOUSEKEEPINGdeletebackup", getLoadPerMember(ecimBackupHousekeepingDeletebackupActivityMaxCount, membersCount));
        updateThresholdCounters("ECIMNODE_HEALTH_CHECKnodehealthcheck", getLoadPerMember(ecimNodeHealthCheckMaxActivityCount, membersCount));
        updateThresholdCounters("ECIMNODE_HEALTH_CHECKenmhealthcheck", getLoadPerMember(ecimNodeHealthCheckMaxActivityCount, membersCount));
        updateThresholdCounters(ECIM_LICENSE_REFRESH_REFRESH, ecimLicenseRefreshJobRefreshActivityMaxCount);
        updateThresholdCounters(ECIM_LICENSE_REFRESH_REQUEST, ecimLicenseRefreshJobRequestActivityMaxCount);
        updateThresholdCounters(ECIM_LICENSE_REFRESH_INSTALL, ecimLicenseRefreshJobInstallActivityMaxCount);


        updateThresholdCounters("MINI_LINK_INDOORUPGRADEdownload", getLoadPerMember(downloadMiniLinkIndoorUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORUPGRADEactivate", getLoadPerMember(activateMiniLinkIndoorUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORUPGRADEconfirm", getLoadPerMember(confirmMiniLinkIndoorUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORBACKUPbackup", getLoadPerMember(backupMiniLinkIndoorBackupActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORRESTOREdownload", getLoadPerMember(downloadMiniLinkIndoorRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORRESTOREverify", getLoadPerMember(verifyMiniLinkIndoorRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORRESTORErestore", getLoadPerMember(restoreMiniLinkIndoorRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORDELETEBACKUPdeletebackup", getLoadPerMember(deletebackupMiniLinkIndoorDeleteBackupActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_INDOORLICENSEinstall", getLoadPerMember(installLicenseKeyFileActivityMaxCount, membersCount));
        updateThresholdCounters(VRAN_UPGRADE_PREPARE, getLoadPerMember(vranUpgradePrepareActivityMaxCount, membersCount));
        updateThresholdCounters(VRAN_UPGRADE_VERIFY, getLoadPerMember(vranUpgradeVerifyActivityMaxCount, membersCount));
        updateThresholdCounters(VRAN_UPGRADE_ACTIVATE, getLoadPerMember(vranUpgradeActivateActivityMaxCount, membersCount));
        updateThresholdCounters(VRAN_UPGRADE_CONFIRM, getLoadPerMember(vranUpgradeConfirmActivityMaxCount, membersCount));
        updateThresholdCounters(VRAN_ONBOARD_ONBOARD, getLoadPerMember(vranOnboardJobOnboardActivityMaxCount, membersCount));
        updateThresholdCounters(VRAN_DELETE_SOFTWAREPACKAGE_DELETE_SOFTWAREPACKAGE, getLoadPerMember(vranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount, membersCount));
        updateThresholdCounters(STN_UPGRADE_INSTALL, stnUpgradeDownloadActivityMaxCount);
        updateThresholdCounters(STN_UPGRADE_UPGRADE, stnUpgradeActivateActivityMaxCount);
        updateThresholdCounters(STN_UPGRADE_APPROVESW, stnUpgradeApproveActivityMaxCount);
        updateThresholdCounters(STN_UPGRADE_SWADJUST, stnUpgradeSwAdjustActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORBACKUPbackup", getLoadPerMember(backupMiniLinkOutdoorBackupActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_OUTDOORLICENSEinstall", getLoadPerMember(minilinkoutdoorinstallLicenseKeyFileActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_OUTDOORRESTORErestore", getLoadPerMember(restoreMiniLinkOutdoorRestoreActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_OUTDOORUPGRADEdownload", getLoadPerMember(downloadMiniLinkOutdoorUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_OUTDOORUPGRADEactivate", getLoadPerMember(activateMiniLinkOutdoorUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_OUTDOORUPGRADEconfirm", getLoadPerMember(confirmMiniLinkOutdoorUpgradeActivityMaxCount, membersCount));
        updateThresholdCounters("MINI_LINK_OUTDOORDELETEBACKUPdeletebackup", getLoadPerMember(deletebackupMiniLinkOutdoorDeleteBackupActivityMaxCount, membersCount));
        updateThresholdCounters(AXE_BACKUP_CREATEBACKUP, getLoadPerMember(shmAxeBackupjobCreateBackupActivityMaxCount, membersCount));
        updateThresholdCounters(AXE_BACKUP_UPLOADBACKUP, getLoadPerMember(shmAxeBackupjobUploadBackupActivityMaxCount, membersCount));
        updateThresholdCounters(AXE_DELETE_BACKUP_DELETEBACKUP, getLoadPerMember(shmAxeDeleteBackupJobdeleteBackupActivityMaxCount, membersCount));
        updateThresholdCounters(AXE_INSTALL_LICENSE, getLoadPerMember(shmAxeLicenseInstalljobActivityMaxCount, membersCount));
        updateThresholdCounters(AXE_UPGRADE_ACTIVITY, getLoadPerMember(shmAxeUpgradejobActivityMaxCount, membersCount));
    }

    public Long getMaximumCount(final String platform, final String jobType, final String name) {
        return thresholdValueOfCounterMap.get(platform + jobType + name);
    }

    public long getLoadControlQueueConsumerTimeout() {
        return loadControlQueueConsumerTimeout_ms;
    }

    /**
     * This method will return maximum Load counter by counterkey. Ex:- counterKey="CPPBACKUPExportcv"
     * 
     * @param counterKey
     * @return maximum activities allowed LC value
     */
    public Long getMaximumCountByCounterKey(final String counterKey) {
        return thresholdValueOfCounterMap.get(counterKey);
    }

    /*----------- Listeners for the configured parameters -----------*/

    /**
     * Listener for Cpp DeleteUpgradepackage Job deleteupgradepackage Activity max Count
     * 
     * @param activateNoderestartActivityMaxCount
     */

    void listenForCppDeleteUpgradepackageJobDeleteUpgradepackageActivtyMaxCount(

            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_DELETE_UPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_MAX_COUNT") final long deleteupgradepackageCppDeleteupgradepackageActivityMaxCount) {
        this.deleteupgradepackageCppDeleteupgradepackageActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(deleteupgradepackageCppDeleteupgradepackageActivityMaxCount);
        updateThresholdCounters(CPP_DELETE_UPGRADEPACKAGE, this.deleteupgradepackageCppDeleteupgradepackageActivityMaxCount);
    }

    /**
     * Listener for Ecim DeleteUpgradepackage Job deleteupgradepackage Activity max Count
     * 
     * @param deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount
     */

    void listenECIMDeleteUpgradepackageJobDeleteUpgradepackageActivtyMaxCount(

            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_DELETE_UPGRADEPACKAGEJOB_DELETEUPGRADEPACKAGE_ACTIVITY_MAX_COUNT") final long deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount) {
        this.deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount);
        updateThresholdCounters(ECIM_DELETE_UPGRADEPACKAGE, this.deleteupgradepackageEcimDeleteupgradepackageActivityMaxCount);
    }

    /**
     * Listener for Install Activity max Count
     * 
     * @param installActivityMaxCount
     */
    void listenForCppUpgradeJobInstallActivtyMaxCount(@Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_INSTALL_ACTIVITY_MAX_COUNT") final long installActivityMaxCount) {
        this.installActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(installActivityMaxCount);
        updateThresholdCounters("CPPUPGRADEinstall", this.installActivityMaxCount);
    }

    /**
     * Listener for Upgrade Job verify Actvity max Count
     * 
     * @param vefifyActivityMaxCount
     */
    void listenForCppUpgradeJobVerifyActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_VERIFY_ACTIVITY_MAX_COUNT") final long vefifyActivityMaxCount) {
        this.vefifyActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vefifyActivityMaxCount);
        updateThresholdCounters("CPPUPGRADEverify", this.vefifyActivityMaxCount);
    }

    /**
     * Listener for Upgrade Job upgrade Actvity max Count
     * 
     * @param upgradeActivityMaxCount
     */
    void listenForCppUpgradeJobUpgradeActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_UPGRADE_ACTIVITY_MAX_COUNT") final long upgradeActivityMaxCount) {
        this.upgradeActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(upgradeActivityMaxCount);
        updateThresholdCounters("CPPUPGRADEupgrade", this.upgradeActivityMaxCount);
    }

    /**
     * Listener for Upgrade Job Confirm Actvity max Count
     * 
     * @param confirmActivityMaxCount
     */
    void listenForCppUpgradeJobConfirmActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT") final long confirmActivityMaxCount) {
        this.confirmActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(confirmActivityMaxCount);
        updateThresholdCounters("CPPUPGRADEconfirm", this.confirmActivityMaxCount);
    }

    /**
     * Listener for Backup Job Create CV Actvity max Count
     * 
     * @param creatCVActivityMaxCount
     */
    void listenForCppBackupJobCreateCVActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPJOB_CREATE_CV_ACTIVITY_MAX_COUNT") final long creatCVActivityMaxCount) {
        this.creatCVActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(creatCVActivityMaxCount);
        updateThresholdCounters("CPPBACKUPcreatecv", this.creatCVActivityMaxCount);
    }

    /**
     * Listener for Backup Job export Actvity max Count
     * 
     * @param exportCVActivityMaxCount
     */
    void listenForCppBackupJobExportCVActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPJOB_UPLOAD_CV_ACTIVITY_MAX_COUNT") final long exportCVActivityMaxCount) {
        this.exportCVActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(exportCVActivityMaxCount);
        updateThresholdCounters("CPPBACKUPexportcv", this.exportCVActivityMaxCount);
    }

    /**
     * Listener for Backup Job SetAsStartable Actvity max Count
     * 
     * @param setStartableActivityMaxCount
     */
    void listenForCppBackupJobSetAsStartableActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPJOB_SET_AS_STARTABLE_ACTIVITY_MAX_COUNT") final long setStartableActivityMaxCount) {
        this.setStartableActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(setStartableActivityMaxCount);
        updateThresholdCounters("CPPBACKUPsetcvasstartable", this.setStartableActivityMaxCount);
    }

    /**
     * Listener for Install Actvity max Count
     * 
     * @param setAsFirstActvityMaxCount
     */
    void listenForCppBackupJobSetAsFirstInRollbackListActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUPJOB_SET_AS_FIRST_IN_ROLLBACKLIST_ACTIVITY_MAX_COUNT") final long setAsFirstActvityMaxCount) {
        this.setAsFirstActvityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(setAsFirstActvityMaxCount);
        updateThresholdCounters("CPPBACKUPsetcvfirstinrollbacklist", this.setAsFirstActvityMaxCount);
    }

    /**
     * Listener for License Install Actvity max Count
     * 
     * @param licenseInstallActvityMaxCount
     */
    void listenForCppLicenseJobCreateCVActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT") final long licenseInstallActvityMaxCount) {
        this.licenseInstallActvityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(licenseInstallActvityMaxCount);
        updateThresholdCounters("CPPLICENSEinstall", this.licenseInstallActvityMaxCount);
    }

    /**
     * Listener for Delete backup(CV) Actvity max Count
     * 
     * @param deleteBackupActvityMaxCount
     */
    void listenForCppDeleteBackupJobDeleteCVActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_DELETEBACKUPJOB_DELETE_ACTIVITY_MAX_COUNT") final long deleteBackupActvityMaxCount) {
        this.deleteBackupActvityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(deleteBackupActvityMaxCount);
        updateThresholdCounters("CPPDELETEBACKUPdeletecv", this.deleteBackupActvityMaxCount);
    }

    /**
     * Listener for Download Restore Activity max Count
     * 
     * @param downloadRestoreActivityMaxCount
     */
    void listenForCppRestoreJobDownlaodActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_DOWNLOAD_ACTIVITY_MAX_COUNT") final long downloadRestoreActivityMaxCount) {
        this.downloadRestoreActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(downloadRestoreActivityMaxCount);
        updateThresholdCounters("CPPRESTOREdownload", this.downloadRestoreActivityMaxCount);
    }

    /**
     * Listener for Verify Restore Job verify Actvity max Count
     * 
     * @param vefifyRestoreActivityMaxCount
     */
    void listenForCppRestoreJobVerifyActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_VERIFY_ACTIVITY_MAX_COUNT") final long vefifyRestoreActivityMaxCount) {
        this.vefifyRestoreActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vefifyRestoreActivityMaxCount);
        updateThresholdCounters("CPPRESTOREverify", this.vefifyRestoreActivityMaxCount);
    }

    /**
     * Listener for Install Retsore Job upgrade Actvity max Count
     * 
     * @param installRestoreActivityMaxCount
     */
    void listenForCppRestoreJobInstallActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_INSTALL_ACTIVITY_MAX_COUNT") final long installRestoreActivityMaxCount) {
        this.installRestoreActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(installRestoreActivityMaxCount);
        updateThresholdCounters("CPPRESTOREinstall", this.installRestoreActivityMaxCount);
    }

    /**
     * Listener for Restore Job Actvity max Count
     * 
     * @param restoreActivityMaxCount
     */
    void listenForCppRestoreJobRestoreActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_RESTORE_ACTIVITY_MAX_COUNT") final long restoreActivityMaxCount) {
        this.restoreActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(restoreActivityMaxCount);
        updateThresholdCounters("CPPRESTORErestore", this.restoreActivityMaxCount);
    }

    /**
     * Listener for Restore Confirm Job Actvity max Count
     * 
     * @param confirmRestoreActivityMaxCount
     */
    void listenForCppRestoreJobCreateCVActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_RESTOREJOB_CONFIRM_ACTIVITY_MAX_COUNT") final long confirmRestoreActivityMaxCount) {
        this.confirmRestoreActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(confirmRestoreActivityMaxCount);
        updateThresholdCounters("CPPRESTOREconfirm", this.confirmRestoreActivityMaxCount);
    }

    /**
     * Listener for Backup housekeeping Job housekeeping Activity max Count (for CPP)
     * 
     * @param cppBackupHousekeepingCleancvActivityMaxCount
     */
    void listenForCppBackupHousekeepingJobHousekeepingActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_BACKUP_HOUSEKEEPINGJOB_CLEANCV_ACTIVITY_MAX_COUNT") final long cppBackupHousekeepingCleancvActivityMaxCount) {
        this.cppBackupHousekeepingCleancvActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(cppBackupHousekeepingCleancvActivityMaxCount);
        updateThresholdCounters("CPPBACKUP_HOUSEKEEPINGcleancv", this.cppBackupHousekeepingCleancvActivityMaxCount);
    }

    /**
     * Listener for Noderestart Job manual restart Activity max Count
     * 
     * @param manualrestartNoderestartActivityMaxCount
     */

    void listenForCppNoderestartJobRestartActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_CPP_NODERESTARTJOB_MANUALRESTART_ACTIVITY_MAX_COUNT") final long manualrestartNoderestartActivityMaxCount) {
        this.manualrestartNoderestartActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(manualrestartNoderestartActivityMaxCount);
        updateThresholdCounters(CPP_NODE_RESTART, this.manualrestartNoderestartActivityMaxCount);
    }

    /**
     * Listener for Restore Confirm Job Actvity max Count
     * 
     * @param createBackupActivityMaxCount
     */
    void listenForEcimBackupJobCreateBackupActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUPJOB_CREATE_BACKUP_ACTIVITY_MAX_COUNT") final long createBackupActivityMaxCount) {
        this.createBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(createBackupActivityMaxCount);
        updateThresholdCounters("ECIMBACKUPcreatebackup", this.createBackupActivityMaxCount);
    }

    /**
     * Listener for Restore Confirm Job Actvity max Count
     * 
     * @param uploadBackupActivityMaxCount
     */
    void listenForEcimBackupJobuploadBackupActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUPJOB_UPLOAD_BACKUP_ACTIVITY_MAX_COUNT") final long uploadBackupActivityMaxCount) {
        this.uploadBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(uploadBackupActivityMaxCount);
        updateThresholdCounters("ECIMBACKUPuploadbackup", this.uploadBackupActivityMaxCount);
    }

    /**
     * Listener for License install Job Actvity max Count
     * 
     * @param installLicenseActivityMaxCount
     */
    void listenForEcimLicenseJobInstallActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT") final long installLicenseActivityMaxCount) {
        this.installLicenseActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(installLicenseActivityMaxCount);
        updateThresholdCounters("ECIMLICENSEinstall", this.installLicenseActivityMaxCount);
    }

    /**
     * Listener for License Refresh Job Refresh Activity max Count
     * 
     * @param ecimLicenseRefreshJobRefreshActivityMaxCount
     */
    void listenForEcimLicenseRefreshJobRefreshActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REFRESH_ACTIVITY_MAX_COUNT") final long ecimLicenseRefreshJobRefreshActivityMaxCount) {
        this.ecimLicenseRefreshJobRefreshActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(ecimLicenseRefreshJobRefreshActivityMaxCount);
        updateThresholdCounters(ECIM_LICENSE_REFRESH_REFRESH, this.ecimLicenseRefreshJobRefreshActivityMaxCount);
    }

    /**
     * Listener for License Refresh Job Request Activity max Count
     * 
     * @param ecimLicenseRefreshJobRequestActivityMaxCount
     */
    void listenForEcimLicenseRefreshJobRequestActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_REQUEST_ACTIVITY_MAX_COUNT") final long ecimLicenseRefreshJobRequestActivityMaxCount) {
        this.ecimLicenseRefreshJobRequestActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(ecimLicenseRefreshJobRequestActivityMaxCount);
        updateThresholdCounters(ECIM_LICENSE_REFRESH_REQUEST, this.ecimLicenseRefreshJobRequestActivityMaxCount);
    }

    /**
     * Listener for License Refresh Job Install Activity max Count
     * 
     * @param ecimLicenseRefreshJobInstallActivityMaxCount
     */
    void listenForEcimLicenseRefreshJobInstallActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_LICENSE_REFRESH_JOB_INSTALL_ACTIVITY_MAX_COUNT") final long ecimLicenseRefreshJobInstallActivityMaxCount) {
        this.ecimLicenseRefreshJobInstallActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(ecimLicenseRefreshJobInstallActivityMaxCount);
        updateThresholdCounters(ECIM_LICENSE_REFRESH_INSTALL, this.ecimLicenseRefreshJobInstallActivityMaxCount);
    }

    void listenForMiniLinkIndoorLicenseJobInstallActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_INDOOR_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT") final long installLicenseKeyFileActivityMaxCount) {
        this.installLicenseActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(installLicenseKeyFileActivityMaxCount);
        updateThresholdCounters("MINI_LINK_INDOORLICENSEinstall", this.installLicenseKeyFileActivityMaxCount);
    }

    void listenForMiniLinkOutdoorBackupJobBackupActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_BACKUPJOB_BACKUP_ACTIVITY_MAX_COUNT") final long backupMiniLinkOutdoorBackupActivityMaxCount) {
        this.backupMiniLinkOutdoorBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(backupMiniLinkOutdoorBackupActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORBACKUPbackup", this.backupMiniLinkOutdoorBackupActivityMaxCount);
    }

    void listenForMiniLinkOutdoorLicenseJobInstallActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT") final long minilinkoutdoorinstallLicenseKeyFileActivityMaxCount) {
        this.minilinkoutdoorinstallLicenseKeyFileActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(minilinkoutdoorinstallLicenseKeyFileActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORLICENSEinstall", this.minilinkoutdoorinstallLicenseKeyFileActivityMaxCount);
    }

    void listenForMiniLinkOutdoorRestoreJobRestoreActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_RESTOREJOB_RESTORE_ACTIVITY_MAX_COUNT") final long restoreMiniLinkOutdoorRestoreActivityMaxCount) {
        this.restoreMiniLinkOutdoorRestoreActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(restoreMiniLinkOutdoorRestoreActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORRESTORErestore", this.restoreMiniLinkOutdoorRestoreActivityMaxCount);
    }

    void listenForMiniLinkOutdoorUpgradeJobDownloadActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_UPGRADEJOB_DOWNLOAD_ACTIVITY_MAX_COUNT") final long downloadMiniLinkOutdoorUpgradeActivityMaxCount) {
        this.downloadMiniLinkOutdoorUpgradeActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(downloadMiniLinkOutdoorUpgradeActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORUPGRADEdownload", this.downloadMiniLinkOutdoorUpgradeActivityMaxCount);
    }

    void listenForMiniLinkOutdoorUpgradeJobActivateActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_UPGRADEJOB_ACTIVATE_ACTIVITY_MAX_COUNT") final long activateMiniLinkOutdoorUpgradeActivityMaxCount) {
        this.activateMiniLinkOutdoorUpgradeActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(activateMiniLinkOutdoorUpgradeActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORUPGRADEactivate", this.activateMiniLinkOutdoorUpgradeActivityMaxCount);
    }

    void listenForMiniLinkOutdoorUpgradeJobConfirmActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT") final long confirmMiniLinkOutdoorUpgradeActivityMaxCount) {
        this.confirmMiniLinkOutdoorUpgradeActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(confirmMiniLinkOutdoorUpgradeActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORUPGRADEconfirm", this.confirmMiniLinkOutdoorUpgradeActivityMaxCount);
    }

    void listenForMiniLinkOutdoorDeleteBackupJobDeleteBackupActivtyMaxCount(

            @Observes @ConfigurationChangeNotification(propertyName = "SHM_MINI_LINK_OUTDOOR_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT") final long deletebackupMiniLinkOutdoorDeleteBackupActivityMaxCount) {
        this.deletebackupMiniLinkOutdoorDeleteBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(deletebackupMiniLinkOutdoorDeleteBackupActivityMaxCount);
        updateThresholdCounters("MINI_LINK_OUTDOORDELETEBACKUPdeletebackup", this.deletebackupMiniLinkOutdoorDeleteBackupActivityMaxCount);
    }

    void listenForEcimRestoreJobImportBackupActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_DOWNLOAD_BACKUP_ACTIVITY_MAX_COUNT") final long downloadBackupActivityMaxCount) {
        this.downloadBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(downloadBackupActivityMaxCount);
        updateThresholdCounters("ECIMRESTOREdownloadbackup", this.downloadBackupActivityMaxCount);
    }

    void listenForEcimRestoreJobRestoreBackupActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_RESTORE_BACKUP_ACTIVITY_MAX_COUNT") final long restoreBackupActivityMaxCount) {
        this.restoreBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(restoreBackupActivityMaxCount);
        updateThresholdCounters("ECIMRESTORErestorebackup", restoreBackupActivityMaxCount);
    }

    void listenForEcimRestoreJobConfirmBackupActivtyMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_RESTOREJOB_CONFIRM_BACKUP_ACTIVITY_MAX_COUNT") final long confirmBackupActivityMaxCount) {
        this.confirmBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(confirmBackupActivityMaxCount);
        updateThresholdCounters("ECIMRESTOREconfirmbackup", confirmBackupActivityMaxCount);
    }

    void listnerForVranUpgradeJobPrepareActivityMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_VRAN_UPGRADEJOB_PREPARE_ACTIVITY_MAX_COUNT") final long vranUpgradePrepareActivityMaxCount) {
        this.vranUpgradePrepareActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vranUpgradePrepareActivityMaxCount);
        updateThresholdCounters(VRAN_UPGRADE_PREPARE, this.vranUpgradePrepareActivityMaxCount);
    }

    void listnerForVranUpgradeJobVerifyActivityMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_VRAN_UPGRADEJOB_VERIFY_ACTIVITY_MAX_COUNT") final long vranUpgradeVerifyActivityMaxCount) {
        this.vranUpgradeVerifyActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vranUpgradeVerifyActivityMaxCount);
        updateThresholdCounters(VRAN_UPGRADE_VERIFY, this.vranUpgradeVerifyActivityMaxCount);
    }

    void listnerForVranUpgradeJobActivateActivityMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_VRAN_UPGRADEJOB_ACTIVATE_ACTIVITY_MAX_COUNT") final long vranUpgradeActivateActivityMaxCount) {
        this.vranUpgradeActivateActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vranUpgradeActivateActivityMaxCount);
        updateThresholdCounters(VRAN_UPGRADE_ACTIVATE, this.vranUpgradeActivateActivityMaxCount);
    }

    void listnerForVranUpgradeJobConfirmActivityMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_VRAN_UPGRADEJOB_CONFIRM_ACTIVITY_MAX_COUNT") final long vranUpgradeConfirmActivityMaxCount) {
        this.vranUpgradeConfirmActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vranUpgradeConfirmActivityMaxCount);
        updateThresholdCounters(VRAN_UPGRADE_CONFIRM, this.vranUpgradeConfirmActivityMaxCount);
    }

    void listnerForVranOnboardJobOnboardActivityMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_VRAN_ONBOARDJOB_ONBOARD_ACTIVITY_MAX_COUNT") final long vranOnboardJobOnboardActivityMaxCount) {
        this.vranOnboardJobOnboardActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vranOnboardJobOnboardActivityMaxCount);
        updateThresholdCounters(VRAN_ONBOARD_ONBOARD, this.vranOnboardJobOnboardActivityMaxCount);
    }

    void listnerForVranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount(
            @Observes @ConfigurationChangeNotification(propertyName = "SHM_VRAN_DELETE_SOFTWAREPACKAGE_DELETE_SOFTWAREPACKAGE_ACTIVITY_MAX_COUNT") final long vranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount) {
        this.vranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(vranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount);
        updateThresholdCounters(VRAN_DELETE_SOFTWAREPACKAGE_DELETE_SOFTWAREPACKAGE, this.vranDeleteSoftwarePackageDeleteSoftwarePackageActivityMaxCount);
    }

    void listenForLoadControlQueueConsumerTimeout(@Observes @ConfigurationChangeNotification(propertyName = "loadControlQueueConsumerTimeout_ms") final long loadControlQueueConsumerTimeout) {
        this.loadControlQueueConsumerTimeout_ms = loadControlQueueConsumerTimeout;
    }

    private void updateThresholdCounters(final String paramName, final Long configuredMaxCount) {
        thresholdValueOfCounterMap.put(paramName, configuredMaxCount);
        loadControlInstrumentationBean.setCurrentThresholdCounts(getCurrentThresholdCounts());
    }

    /**
     * Listener for Backup housekeeping Job housekeeping Activity max Count (for ECIM)
     * 
     * @param ecimBackupHousekeepingDeletebackupActivityMaxCount
     */
    void listenForEcimBackupHousekeepingJobHousekeepingActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_ECIM_BACKUP_HOUSEKEEPINGJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT") final long ecimBackupHousekeepingDeletebackupActivityMaxCount) {
        this.ecimBackupHousekeepingDeletebackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(ecimBackupHousekeepingDeletebackupActivityMaxCount);
        updateThresholdCounters("ECIMBACKUP_HOUSEKEEPINGdeletebackup", this.ecimBackupHousekeepingDeletebackupActivityMaxCount);
    }

    void listenForEcimNodeHealthCheckMaxActivityCount(@Observes @ConfigurationChangeNotification(propertyName = "ecimNodeHealthCheckMaxActivityCount") final long ecimNodeHealthCheckMaxActivityCount) {
        this.ecimNodeHealthCheckMaxActivityCount = getLoadControllerCountBasedOnCurrentMembersCount(ecimNodeHealthCheckMaxActivityCount);
        updateThresholdCounters("ECIMNODE_HEALTH_CHECKnodehealthcheck", this.ecimNodeHealthCheckMaxActivityCount);
    }

    /**
     * Supplied maximum threshold value of counter for instrumentation
     * 
     * @return
     */
    private String getCurrentThresholdCounts() {
        return thresholdValueOfCounterMap.toString();
    }

    @Inject
    @Configured(propertyName = "MAX_NUMBER_OF_LOAD_CONTROL_CORRELATION_FAILURE_RETRIES")
    private int maxNoOfLoadControllerCorrelationFailureRetries;

    void listnerForLoadcontrollerRetryCount(
            @Observes @ConfigurationChangeNotification(propertyName = "MAX_NUMBER_OF_LOAD_CONTROL_CORRELATION_FAILURE_RETRIES") final int maxNoOfLoadControllerCorrelationFailureRetries) {
        this.maxNoOfLoadControllerCorrelationFailureRetries = maxNoOfLoadControllerCorrelationFailureRetries;
    }

    public int getLoadControllerCorrelationFailureRetryCount() {
        return maxNoOfLoadControllerCorrelationFailureRetries;
    }

    private long getLoadControllerCountBasedOnCurrentMembersCount(final long count) {
        final double countAsDouble = count;
        return (long) Math.ceil(countAsDouble / membershipListenerInterface.getCurrentMembersCount());
    }

    private long getLoadPerMember(final long count, final int curentMemberCount) {
        final double countAsDouble = count;
        return (long) Math.ceil(countAsDouble / curentMemberCount);
    }

    /**
     * Listener for runningMainJobsBatchSize attribute value
     * 
     * @param runningMainJobsBatchSize
     */
    void listenForstagedactivitiesBatchSize(@Observes @ConfigurationChangeNotification(propertyName = STAGED_ACTIVITIES_BATCH_SIZE) final int stagedactivitiesBatchSize) {
        this.stagedactivitiesBatchSize = stagedactivitiesBatchSize;
    }

    public int getstagedactivitiesBatchSize() {
        return stagedactivitiesBatchSize;
    }

    void listnerForResetCurrentLoadControlValue(@Observes @ConfigurationChangeNotification(propertyName = "RESET_SHM_LC_CURRENT_VALUE") final boolean resetCurrentLoadControlValue) {
        this.resetShmLcCurrentValue = resetCurrentLoadControlValue;
        if (this.resetShmLcCurrentValue) {
            final Set<String> loadControlParams = thresholdValueOfCounterMap.keySet();
            for (final String eachLoadControlParam : loadControlParams) {
                loadControlCounterManager.resetCounter(eachLoadControlParam);
                loadControlCounterManager.resetGlobalCounter(eachLoadControlParam);
            }
        } else {
            LOGGER.info("Current Load Control set to false. Nothing to reset.");
        }
    }

    void listenForAxeBackupJobCreateBackupActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_BACKUPJOB_CREATE_BACKUP_ACTIVITY_MAX_COUNT") final long shmAxeBackupjobCreateBackupActivityMaxCount) {
        this.shmAxeBackupjobCreateBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(shmAxeBackupjobCreateBackupActivityMaxCount);
        updateThresholdCounters(AXE_BACKUP_CREATEBACKUP, this.shmAxeBackupjobCreateBackupActivityMaxCount);
    }

    void listenForAxeBackupJobuploadBackupActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_BACKUPJOB_UPLOAD_BACKUP_ACTIVITY_MAX_COUNT") final long shmAxeBackupjobUploadBackupActivityMaxCount) {
        this.shmAxeBackupjobUploadBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(shmAxeBackupjobUploadBackupActivityMaxCount);
        updateThresholdCounters(AXE_BACKUP_UPLOADBACKUP, this.shmAxeBackupjobUploadBackupActivityMaxCount);
    }

    void listenForAxeLicenseJobInstallActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_LICENSEJOB_INSTALL_ACTIVITY_MAX_COUNT") final long shmAxeLicenseInstalljobActivityMaxCount) {
        this.shmAxeLicenseInstalljobActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(shmAxeLicenseInstalljobActivityMaxCount);
        updateThresholdCounters(AXE_INSTALL_LICENSE, this.shmAxeLicenseInstalljobActivityMaxCount);
    }

    void listenForAxeDeleteBackupJobuploadDeleteBackupActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_DELETEBACKUPJOB_DELETEBACKUP_ACTIVITY_MAX_COUNT") final long shmAxeDeleteBackupJobdeleteBackupActivityMaxCount) {
        this.shmAxeDeleteBackupJobdeleteBackupActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(shmAxeDeleteBackupJobdeleteBackupActivityMaxCount);
        updateThresholdCounters(AXE_BACKUP_UPLOADBACKUP, this.shmAxeDeleteBackupJobdeleteBackupActivityMaxCount);
    }

    void listenForAxeUpgradeJobActivtyMaxCount(

    @Observes @ConfigurationChangeNotification(propertyName = "SHM_AXE_UPGRADEJOB_ACTIVITY_MAX_COUNT") final long shmAxeUpgradejobActivityMaxCount) {
        this.shmAxeUpgradejobActivityMaxCount = getLoadControllerCountBasedOnCurrentMembersCount(shmAxeUpgradejobActivityMaxCount);
        updateThresholdCounters(AXE_UPGRADE_ACTIVITY, this.shmAxeUpgradejobActivityMaxCount);
    }
}
