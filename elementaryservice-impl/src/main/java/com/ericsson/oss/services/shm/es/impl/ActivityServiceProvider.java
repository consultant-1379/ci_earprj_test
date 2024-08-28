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
package com.ericsson.oss.services.shm.es.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * There is a memory leakage issue in using InstanceProvider. So, we changed the implementation to Factory Design as a quick fix. However, using Factory Design leads to poor code maintainability, so
 * we need to analyze and find out a better solution which improves code maintainability and doesn't leak any memory. JIRA : https://jira-nam.lmera.ericsson.se/browse/TORF-180324
 */
@ApplicationScoped
@SuppressWarnings({ "PMD.TooManyFields", "PMD.ExcessiveMethodLength" })
public class ActivityServiceProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    //CPP Backup Job
    @Inject
    @ActivityInfo(activityName = "exportcv", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppUploadCvService;

    //CPP Restore Job
    @Inject
    @ActivityInfo(activityName = "download", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppDownloadCvService;

    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppVerifyRestoreService;

    @Inject
    @ActivityInfo(activityName = "install", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppRestoreInstallService;

    @Inject
    @ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppRestoreService;

    @Inject
    @ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppConfirmRestoreService;

    //CPP Upgrade Job
    @Inject
    @ActivityInfo(activityName = "install", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppInstallService;

    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppVerifyService;

    @Inject
    @ActivityInfo(activityName = "upgrade", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppUpgradeService;

    @Inject
    @ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppConfirmService;

    //ECIM Backup Job
    @Inject
    @ActivityInfo(activityName = "createbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimCreateBackupService;

    @Inject
    @ActivityInfo(activityName = "uploadbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimUploadBackupService;

    //ECIM BackupHousekeeping Job
    @Inject
    @ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.BACKUP_HOUSEKEEPING, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimCleanBackupService;

    //ECIM DeleteBackup Job
    @Inject
    @ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimDeleteBackupService;

    //ECIM License Job
    @Inject
    @ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimInstallLicenseKeyFileService;

    //ECIM Restore Job
    @Inject
    @ActivityInfo(activityName = "downloadbackup", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimDownloadBackupService;

    @Inject
    @ActivityInfo(activityName = "restorebackup", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimRestoreService;

    //ECIM Upgrade Job
    @Inject
    @ActivityInfo(activityName = "prepare", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimPrepareService;

    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimVerifyService;

    @Inject
    @ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback ecimActivateService;

    // MINI-LINK License install Job
    @Inject
    @ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkIndoorInstallLicenseKeyFileService;

    // MINI-LINK-Outdoor License install Job
    @Inject
    @ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkOutdoorInstallLicenseKeyFileService;

    // MINI-LINK-Outdoor Upgrade Job
    @Inject
    @ActivityInfo(activityName = ActivityConstants.DOWNLOAD, jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkoutdoorDownloadService;

    @Inject
    @ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkoutdoorActivateService;

    @Inject
    @ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkoutdoorConfirmService;

    // MINI-LINK Upgrade Job
    @Inject
    @ActivityInfo(activityName = ActivityConstants.DOWNLOAD, jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkDownloadService;

    @Inject
    @ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkActivateService;

    @Inject
    @ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkConfirmService;

    // MINI-LINK Backup Job
    @Inject
    @ActivityInfo(activityName = "backup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkBackupService;

    //MINI-LINK-OUTDOOR Backup Job
    @Inject
    @ActivityInfo(activityName = "backup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkoutdoorBackupService;

    // MINI-LINK Restore Job
    @Inject
    @ActivityInfo(activityName = "download", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkDownloadBackupService;

    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkVerifyBackupService;

    @Inject
    @ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkRestoreBackupService;

    // MINI-LINK-OUTDOOR Restore Job
    @Inject
    @ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkoutdoorRestoreBackupService;

    // MINI-LINK-OUTDOOR Delete Backup Job
    @Inject
    @ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
    ActivityCallback minilinkOutdoorDeleteBackupService;

    // MINI-LINK Delete Backup Job
    @Inject
    @ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
    ActivityCallback minilinkDeleteBackupService;

    //VRAN Upgrade Job
    @Inject
    @ActivityInfo(activityName = "prepare", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
    ActivityCallback vranPrepareService;

    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
    ActivityCallback vranVerifyService;

    @Inject
    @ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
    ActivityCallback vranActivateService;

    @Inject
    @ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
    ActivityCallback vranConfirmService;

    @Inject
    @ActivityInfo(activityName = "onboard", jobType = JobTypeEnum.ONBOARD, platform = PlatformTypeEnum.vRAN)
    ActivityCallback vranOnboardService;
    //Activities for STN nodes

    @Inject
    @ActivityInfo(activityName = "install", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.STN)
    ActivityCallback downloadService;

    @Inject
    @ActivityInfo(activityName = "upgrade", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.STN)
    ActivityCallback activateService;

    @Inject
    @ActivityInfo(activityName = "approvesw", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.STN)
    ActivityCallback approveService;

    @Inject
    @ActivityInfo(activityName = "swadjust", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.STN)
    ActivityCallback swAdjustService;

    //Delete Job of vRAN SoftwarePackage
    @Inject
    @ActivityInfo(activityName = "delete_softwarepackage", jobType = JobTypeEnum.DELETE_SOFTWAREPACKAGE, platform = PlatformTypeEnum.vRAN)
    ActivityCallback deletePackageService;

    @Inject
    @ActivityInfo(activityName = "deleteupgradepackage", jobType = JobTypeEnum.DELETE_UPGRADEPACKAGE, platform = PlatformTypeEnum.CPP)
    ActivityCallback cppDeleteUpgradePackageService;

    @Inject
    @ActivityInfo(activityName = "deleteupgradepackage", jobType = JobTypeEnum.DELETE_UPGRADEPACKAGE, platform = PlatformTypeEnum.ECIM)
    ActivityCallback deleteUpgradePackageService;

    @Inject
    @ActivityInfo(activityName = "refresh", jobType = JobTypeEnum.LICENSE_REFRESH, platform = PlatformTypeEnum.ECIM)
    ActivityCallback licenseRefreshService;

    @Inject
    @ActivityInfo(activityName = "request", jobType = JobTypeEnum.LICENSE_REFRESH, platform = PlatformTypeEnum.ECIM)
    ActivityCallback licenseRefreshRequestService;

    @SuppressWarnings("PMD.NcssMethodCount")
    public ActivityCallback getActivityNotificationHandler(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activityName) {
        ActivityCallback activityCallBackInstance = null;
        final String qualifier = platform + JobVariables.VAR_NAME_DELIMITER + jobType + JobVariables.VAR_NAME_DELIMITER + activityName;
        switch (qualifier) {

        //CPP Backup Job
        case "CPP.BACKUP.exportcv":
            activityCallBackInstance = cppUploadCvService;
            break;

        //CPP Restore Job
        case "CPP.RESTORE.download":
            activityCallBackInstance = cppDownloadCvService;
            break;
        case "CPP.RESTORE.verify":
            activityCallBackInstance = cppVerifyRestoreService;
            break;
        case "CPP.RESTORE.install":
            activityCallBackInstance = cppRestoreInstallService;
            break;
        case "CPP.RESTORE.restore":
            activityCallBackInstance = cppRestoreService;
            break;
        case "CPP.RESTORE.confirm":
            activityCallBackInstance = cppConfirmRestoreService;
            break;

        //CPP Upgrade Job
        case "CPP.UPGRADE.install":
            activityCallBackInstance = cppInstallService;
            break;
        case "CPP.UPGRADE.verify":
            activityCallBackInstance = cppVerifyService;
            break;
        case "CPP.UPGRADE.upgrade":
            activityCallBackInstance = cppUpgradeService;
            break;
        case "CPP.UPGRADE.confirm":
            activityCallBackInstance = cppConfirmService;
            break;

        //ECIM Backup Job
        case "ECIM.BACKUP.createbackup":
            activityCallBackInstance = ecimCreateBackupService;
            break;
        case "ECIM.BACKUP.uploadbackup":
            activityCallBackInstance = ecimUploadBackupService;
            break;

        //ECIM BackupHousekeeping Job
        case "ECIM.BACKUP_HOUSEKEEPING.deletebackup":
            activityCallBackInstance = ecimCleanBackupService;
            break;

        //ECIM DeleteBackup Job
        case "ECIM.DELETEBACKUP.deletebackup":
            activityCallBackInstance = ecimDeleteBackupService;
            break;

        //ECIM License Job
        case "ECIM.LICENSE.install":
            activityCallBackInstance = ecimInstallLicenseKeyFileService;
            break;

        //ECIM Restore Job
        case "ECIM.RESTORE.downloadbackup":
            activityCallBackInstance = ecimDownloadBackupService;
            break;
        case "ECIM.RESTORE.restorebackup":
            activityCallBackInstance = ecimRestoreService;
            break;

        //ECIM Upgrade Job
        case "ECIM.UPGRADE.prepare":
            activityCallBackInstance = ecimPrepareService;
            break;
        case "ECIM.UPGRADE.verify":
            activityCallBackInstance = ecimVerifyService;
            break;
        case "ECIM.UPGRADE.activate":
            activityCallBackInstance = ecimActivateService;
            break;

        // MINI-LINK Upgrade Job
        case "MINI_LINK_INDOOR.UPGRADE.download":
            activityCallBackInstance = minilinkDownloadService;
            break;
        case "MINI_LINK_INDOOR.UPGRADE.activate":
            activityCallBackInstance = minilinkActivateService;
            break;
        case "MINI_LINK_INDOOR.UPGRADE.confirm":
            activityCallBackInstance = minilinkConfirmService;
            break;

        //Minilink Backup Job
        case "MINI_LINK_INDOOR.BACKUP.backup":
            activityCallBackInstance = minilinkBackupService;
            break;

        //Minilink outdoor Backup Job
        case "MINI_LINK_OUTDOOR.BACKUP.backup":
            activityCallBackInstance = minilinkoutdoorBackupService;
            break;

        // MINI-LINK Restore Job
        case "MINI_LINK_INDOOR.RESTORE.download":
            activityCallBackInstance = minilinkDownloadBackupService;
            break;

        case "MINI_LINK_INDOOR.RESTORE.verify":
            activityCallBackInstance = minilinkVerifyBackupService;
            break;

        case "MINI_LINK_INDOOR.RESTORE.restore":
            activityCallBackInstance = minilinkRestoreBackupService;
            break;

        // MINI-LINK outdoor Restore Job
        case "MINI_LINK_OUTDOOR.RESTORE.restore":
            activityCallBackInstance = minilinkoutdoorRestoreBackupService;
            break;

        // MINI-LINK Delete Backup Job
        case "MINI_LINK_INDOOR.DELETEBACKUP.deletebackup":
            activityCallBackInstance = minilinkDeleteBackupService;
            break;

        // MINI-LINK Outdoor Delete Backup Job
        case "MINI_LINK_OUTDOOR.DELETEBACKUP.deletebackup":
            activityCallBackInstance = minilinkOutdoorDeleteBackupService;
            break;

        //MINI-LINK License Job
        case "MINI_LINK_INDOOR.LICENSE.install":
            activityCallBackInstance = minilinkIndoorInstallLicenseKeyFileService;
            break;

        //MINI-LINK-Outdoor License Job
        case "MINI_LINK_OUTDOOR.LICENSE.install":
            activityCallBackInstance = minilinkOutdoorInstallLicenseKeyFileService;
            break;

        // MINI-LINK Outdoor Upgrade Job
        case "MINI_LINK_OUTDOOR.UPGRADE.download":
            activityCallBackInstance = minilinkoutdoorDownloadService;
            break;
        case "MINI_LINK_OUTDOOR.UPGRADE.activate":
            activityCallBackInstance = minilinkoutdoorActivateService;
            break;
        case "MINI_LINK_OUTDOOR.UPGRADE.confirm":
            activityCallBackInstance = minilinkoutdoorConfirmService;
            break;

        //VRAN Upgrade Job
        case "vRAN.UPGRADE.prepare":
            activityCallBackInstance = vranPrepareService;
            break;
        case "vRAN.UPGRADE.verify":
            activityCallBackInstance = vranVerifyService;
            break;
        case "vRAN.UPGRADE.activate":
            activityCallBackInstance = vranActivateService;
            break;
        case "vRAN.UPGRADE.confirm":
            activityCallBackInstance = vranConfirmService;
            break;
        //STN UPGRADE JOB
        case "STN.UPGRADE.install":
            activityCallBackInstance = downloadService;
            break;
        case "STN.UPGRADE.upgrade":
            activityCallBackInstance = activateService;
            break;
        case "STN.UPGRADE.approvesw":
            activityCallBackInstance = approveService;
            break;
        case "STN.UPGRADE.swadjust":
            activityCallBackInstance = swAdjustService;
            break;
        //VRAN ONBOARD Job
        case "vRAN.ONBOARD.onboard":
            activityCallBackInstance = vranOnboardService;
            break;

        //Delete Job of vRAN SoftwarePackage
        case "vRAN.DELETE_SOFTWAREPACKAGE.delete_softwarepackage":
            activityCallBackInstance = deletePackageService;
            break;

        //Delete_upgradepackage job
        case "ECIM.DELETE_UPGRADEPACKAGE.deleteupgradepackage":
            activityCallBackInstance = deleteUpgradePackageService;
            break;

        case "CPP.DELETE_UPGRADEPACKAGE.deleteupgradepackage":
            activityCallBackInstance = cppDeleteUpgradePackageService;
            break;

        case "ECIM.LICENSE_REFRESH.refresh":
            activityCallBackInstance = licenseRefreshService;
            break;

        case "ECIM.LICENSE_REFRESH.request":
            activityCallBackInstance = licenseRefreshRequestService;
            break;

        default:
            logger.warn("Unspecified Qualifier : {}", qualifier);
        }
        return activityCallBackInstance;

    }
}
