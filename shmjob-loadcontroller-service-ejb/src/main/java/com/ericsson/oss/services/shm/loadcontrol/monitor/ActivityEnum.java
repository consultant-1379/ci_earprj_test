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
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

public enum ActivityEnum {
    cpp_sw_install("install", "CPP", "UPGRADE"),
    cpp_sw_verify("verify", "CPP", "UPGRADE"),
    cpp_sw_upgrade("upgrade", "CPP", "UPGRADE"),
    cpp_sw_confirm("confirm", "CPP", "UPGRADE"),
    cpp_bkp_create_cv("createcv", "CPP", "BACKUP"),
    cpp_bkp_export_cv("exportcv", "CPP", "BACKUP"),
    cpp_bkp_setcvasstartable("setcvasstartable", "CPP", "BACKUP"),
    cpp_bkp_setcvfirstinrollbacklist("setcvfirstinrollbacklist", "CPP", "BACKUP"),
    cpp_bkp_delete_cv("deletecv", "CPP", "DELETEBACKUP"),
    cpp_lic_create_cv("install", "CPP", "LICENSE"),
    cpp_restore_download("download", "CPP", "RESTORE"),
    cpp_restore_verify("verify", "CPP", "RESTORE"),
    cpp_restore_install("install", "CPP", "RESTORE"),
    cpp_restore_restore("restore", "CPP", "RESTORE"),
    cpp_restore_confirm("confirm", "CPP", "RESTORE"),
    cpp_backup_housekeeping("cleancv", "CPP", "BACKUP_HOUSEKEEPING"),
    cpp_noderestart_manualrestart("manualrestart", "CPP", "NODERESTART"),
    ecim_upgrade_prepare("prepare", "ECIM", "UPGRADE"),
    ecim_upgrade_verify("verify", "ECIM", "UPGRADE"),
    ecim_upgrade_activate("activate", "ECIM", "UPGRADE"),
    ecim_upgrade_confirm("confirm", "ECIM", "UPGRADE"),
    ecim_backup_createbackup("createbackup", "ECIM", "BACKUP"),
    ecim_backup_uploadbackup("uploadbackup", "ECIM", "BACKUP"),
    ecim_restore_downloadbackup("downloadbackup", "ECIM", "RESTORE"),
    ecim_restore_restorebackup("restorebackup", "ECIM", "RESTORE"),
    ecim_restore_confirmbackup("confirmbackup", "ECIM", "RESTORE"),
    ecim_backup_deletebackup("deletebackup", "ECIM", "DELETEBACKUP"),
    ecim_license_install("install", "ECIM", "LICENSE"),
    ecim_backup_housekeeping("deletebackup", "ECIM", "BACKUP_HOUSEKEEPING"),
    ECIM_LICENSE_REFRESH_REFRESH("refresh","ECIM","LICENSE_REFRESH"),
    ECIM_LICENSE_REFRESH_REQUEST("request","ECIM","LICENSE_REFRESH"),
    ECIM_LICENSE_REFRESH_INSTALL("install","ECIM","LICENSE_REFRESH"),
    mini_link_indoor_sw_download("download", "MINI_LINK_INDOOR", "UPGRADE"),
    mini_link_indoor_sw_activate("activate", "MINI_LINK_INDOOR", "UPGRADE"),
    mini_link_indoor_sw_confirm("confirm", "MINI_LINK_INDOOR", "UPGRADE"),
    mini_link_indoor_restore_downloadBackup("download", "MINI_LINK_INDOOR", "RESTORE"),
    mini_link_indoor_restore_verify("verify", "MINI_LINK_INDOOR", "RESTORE"),
    mini_link_indoor_restore_restore("restore", "MINI_LINK_INDOOR", "RESTORE"),
    mini_link_indoor_backup("backup", "MINI_LINK_INDOOR", "BACKUP"),
    mini_link_indoor_deletebackup("deletebackup", "MINI_LINK_INDOOR", "DELETEBACKUP"),
    MINI_LINK_INDOOR_LICENSE_INSTALL("install", "MINI_LINK_INDOOR", JobTypeEnum.LICENSE.getAttribute()),
    cpp_delete_upgradepackage_deleteupgradepackage("deleteupgradepackage", "CPP", "DELETE_UPGRADEPACKAGE"), 
    ecim_delete_upgradepackage_deleteupgradepackage("deleteupgradepackage", "ECIM", "DELETE_UPGRADEPACKAGE"),
    ECIM_NODE_HEALTH_CHECK_NODEHEALTHCHECK("nodehealthcheck", "ECIM", "NODE_HEALTH_CHECK"),
    AXE_BACKUP_CREATEBACKUP("createbackup", "AXE", "BACKUP"),
    AXE_BACKUP_UPLOADBACKUP("uploadbackup", "AXE", "BACKUP"),
    AXE_UPGRADE_ACTIVITY("axeActivity", "AXE", "UPGRADE");
    
    private ActivityEnum(final String name, final String platform, final String jobType) {
        this.name = name;
        this.platform = platform;
        this.jobType = jobType;

    }

    private final String name;
    private final String platform;
    private final String jobType;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the platform
     */
    public String getPlatform() {
        return platform;
    }


    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

}
