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
package com.ericsson.oss.services.shm.es.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class ActivityServiceProviderTest {

    @InjectMocks
    ActivityServiceProvider activityServiceProvider;

    @Mock
    Logger logger;
    @Mock
    ActivityCallback cppUploadCvService;
    @Mock
    ActivityCallback cppDownloadCvService;
    @Mock
    ActivityCallback cppVerifyRestoreService;
    @Mock
    ActivityCallback cppRestoreInstallService;
    @Mock
    ActivityCallback cppRestoreService;
    @Mock
    ActivityCallback cppConfirmRestoreService;
    @Mock
    ActivityCallback cppInstallService;
    @Mock
    ActivityCallback cppVerifyService;
    @Mock
    ActivityCallback cppUpgradeService;
    @Mock
    ActivityCallback cppConfirmService;
    @Mock
    ActivityCallback ecimCreateBackupService;
    @Mock
    ActivityCallback ecimUploadBackupService;
    @Mock
    ActivityCallback ecimCleanBackupService;
    @Mock
    ActivityCallback ecimDeleteBackupService;
    @Mock
    ActivityCallback ecimInstallLicenseKeyFileService;
    @Mock
    ActivityCallback ecimDownloadBackupService;
    @Mock
    ActivityCallback ecimRestoreService;
    @Mock
    ActivityCallback ecimPrepareService;
    @Mock
    ActivityCallback ecimVerifyService;
    @Mock
    ActivityCallback ecimActivateService;
    @Mock
    ActivityCallback minilinkIndoorInstallLicenseKeyFileService;
    @Mock
    ActivityCallback minilinkOutdoorInstallLicenseKeyFileService;
    @Mock
    ActivityCallback minilinkoutdoorDownloadService;
    @Mock
    ActivityCallback minilinkoutdoorActivateService;
    @Mock
    ActivityCallback minilinkoutdoorConfirmService;
    @Mock
    ActivityCallback minilinkDownloadService;
    @Mock
    ActivityCallback minilinkActivateService;
    @Mock
    ActivityCallback minilinkConfirmService;
    @Mock
    ActivityCallback minilinkBackupService;
    @Mock
    ActivityCallback minilinkoutdoorBackupService;
    @Mock
    ActivityCallback minilinkDownloadBackupService;
    @Mock
    ActivityCallback minilinkVerifyBackupService;
    @Mock
    ActivityCallback minilinkRestoreBackupService;
    @Mock
    ActivityCallback minilinkoutdoorRestoreBackupService;
    @Mock
    ActivityCallback minilinkOutdoorDeleteBackupService;
    @Mock
    ActivityCallback minilinkDeleteBackupService;
    @Mock
    ActivityCallback vranPrepareService;
    @Mock
    ActivityCallback vranVerifyService;
    @Mock
    ActivityCallback vranActivateService;
    @Mock
    ActivityCallback vranConfirmService;
    @Mock
    ActivityCallback vranOnboardService;
    @Mock
    ActivityCallback downloadService;
    @Mock
    ActivityCallback activateService;
    @Mock
    ActivityCallback approveService;
    @Mock
    ActivityCallback swAdjustService;
    @Mock
    ActivityCallback deletePackageService;
    @Mock
    ActivityCallback cppDeleteUpgradePackageService;
    @Mock
    ActivityCallback deleteUpgradePackageService;
    @Mock
    ActivityCallback licenseRefreshService;
    @Mock
    ActivityCallback licenseRefreshRequestService;

    @Test
    public void testGetActivityNotificationHandler() {

        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.BACKUP, "exportcv");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "download");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "verify");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "confirm");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, "verify");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, "upgrade");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, "confirm");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, "createbackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, "uploadbackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP_HOUSEKEEPING, "deletebackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.DELETEBACKUP, "deletebackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.LICENSE, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.RESTORE, "downloadbackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.RESTORE, "restorebackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.UPGRADE, "prepare");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.UPGRADE, "verify");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.UPGRADE, "activate");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.UPGRADE, "download");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.UPGRADE, "activate");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.UPGRADE, "confirm");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.BACKUP, "backup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.BACKUP, "backup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.RESTORE, "download");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.RESTORE, "verify");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.RESTORE, "restore");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.RESTORE, "restore");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.DELETEBACKUP, "deletebackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.DELETEBACKUP, "deletebackup");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_INDOOR, JobTypeEnum.LICENSE, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.LICENSE, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.UPGRADE, "download");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.UPGRADE, "activate");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.MINI_LINK_OUTDOOR, JobTypeEnum.UPGRADE, "confirm");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.vRAN, JobTypeEnum.UPGRADE, "prepare");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.vRAN, JobTypeEnum.UPGRADE, "verify");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.vRAN, JobTypeEnum.UPGRADE, "activate");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.vRAN, JobTypeEnum.UPGRADE, "confirm");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.STN, JobTypeEnum.UPGRADE, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.STN, JobTypeEnum.UPGRADE, "upgrade");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.STN, JobTypeEnum.UPGRADE, "approvesw");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.STN, JobTypeEnum.UPGRADE, "swadjust");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.vRAN, JobTypeEnum.ONBOARD, "onboard");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.vRAN, JobTypeEnum.DELETE_SOFTWAREPACKAGE, "delete_softwarepackage");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.DELETE_UPGRADEPACKAGE, "deleteupgradepackage");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.CPP, JobTypeEnum.DELETE_UPGRADEPACKAGE, "deleteupgradepackage");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.LICENSE_REFRESH, "refresh");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.LICENSE_REFRESH, "request");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.LICENSE_REFRESH, "install");
        activityServiceProvider.getActivityNotificationHandler(PlatformTypeEnum.ECIM, JobTypeEnum.LICENSE_REFRESH, "elis");
    }

}
