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
package com.ericsson.oss.services.shm.jobs.common.constants;

public class JobPropertyConstants {
    public static final String KEY = "key";
    public static final String VALUE = "value";
    // UP Properties
    public static final String SWP = "SWP";
    public static final String FORCED_INSTALL = "FORCEDINSTALL";
    public static final String SELECTIVE_INSTALL = "SELECTIVEINSTALL";
    public static final String UCF = "UCF";
    // Backup Properties
    public static final String CREATE_CV = "createcv";
    public static final String SET_CV_AS_STARTABLE = "setcvasstartable";
    public static final String SET_CV_FIRST_IN_ROLLBACK_LIST = "setcvfirstinrollbacklist";
    public static final String EXPORT_CV = "exportcv";
    public static final String CV_NAME = "CV_NAME";
    public static final String CV_TYPE = "CV_TYPE";
    public static final String CV_COMMENT = "CV_COMMENT";

    public static final String BACKUP_NAME = "BACKUP_NAME";
    public static final String STARTABLE_CV_NAME = "STARTABLE_CV_NAME";
    public static final String ROLLBACK_CV_NAME = "ROLLBACK_CV_NAME";
    public static final String UPLOAD_CV_NAME = "UPLOAD_CV_NAME";
    public static final String COMMA = ",";
    public static final String UPLOAD_BACKUP_DETAILS = "UPLOAD_BACKUP_DETAILS";

    // RESTORE Properties
    public static final String MISSING_PKG_SELECTION = "INSTALL_MISSING_UPGRADE_PACKAGES";
    public static final String CORRUPTED_PKG_SELECTION = "REPLACE_CORRUPTED_UPGRADE_PACKAGES";

    // License Properties
    public static final String PROP_LICENSE_KEYFILE_PATH = "LICENSE_FILEPATH";
    public static final String PROP_FINGERPRINT = "FINGER_PRINT";
    public static final String PROP_SEQUENCENUMBER = "SEQUENCE_NUMBER";
    public static final String NE_PROPERTIES = "neProperties";
    public static final String PROPERTIES = "properties";
    public static final String NE_NAMES = "neNames";
    public static final String ACTIVITY_NAME = "activityName";
    public static final String PARENT_NODE = "parentnode";
    public static final String COMPONENTS = "components";

    public static final String CV_IDENTITY = "CV_IDENTITY";

    public static final String DISPLAYNAME_FOR_LICENSE_KEY_FILE = "License Key File";

    // RESTORE activities
    public static final String INSTALL_MISSING_UPGRADE_PACKAGES = "INSTALL_MISSING_UPGRADE_PACKAGES";
    public static final String REPLACE_CORRUPTED_UPGRADE_PACKAGES = "REPLACE_CORRUPTED_UPGRADE_PACKAGES";
    public static final String AUTO_CONFIGURATION = "AUTO_CONFIGURATION";
    public static final String FORCED_RESTORE = "FORCED_RESTORE";

    // UI display names for RESTORE
    public static final String INSTALL_MISSING_UPGRADE_PACKAGES_TRUE = "Install Missing Upgrade Packages : Yes";
    public static final String INSTALL_MISSING_UPGRADE_PACKAGES_FALSE = "Install Missing Upgrade Packages : No";
    public static final String REPLACE_CORRUPTED_UPGRADE_PACKAGES_TRUE = "Replace Corrupted Upgrade Packages : Yes";
    public static final String REPLACE_CORRUPTED_UPGRADE_PACKAGES_FALSE = "Replace Corrupted Upgrade Packages : No";
    public static final String AUTO_CONFIGURATION_WITHCV = "Auto Configuration : According to Downloaded CV";
    public static final String AUTO_CONFIGURATION_ON = "Auto Configuration : ON";
    public static final String AUTO_CONFIGURATION_OFF = "Auto Configuration : OFF";
    public static final String FORCED_RESTORE_TRUE = "Allow Forced Restore : Yes";
    public static final String FORCED_RESTORE_FALSE = "Allow Forced Restore : No";

    // UI display names for upgrade
    public static final String SELECTIVEINSTALL = "Select Type : ";
    public static final String FORCEINSTALL = "Transfer Type : ";
    public static final String REBOOTNODEUPGRADE = "Upgrade Reboot : ";

    // Json Fields
    public static final String ATTRIBUTE_NAME = "attributeName";
    public static final String ATTRIBUTE_VALUE = "attributeValue";

    public static final String REPEAT_TYPE = "REPEAT_TYPE";
    public static final String BACKUP_DOMAIN_TYPE = "BACKUP_DOMAIN_TYPE";
    public static final String BRM_BACKUP_NAME = "BACKUP_NAME";
    public static final String STEP_BY_STEP = "STEP_BY_STEP";
    public static final String SWP_NAME = "SWP_NAME";
    public static final String FORCE_INSTALL = "FORCEINSTALL";
    public static final String REBOOTNODEUPGRADEPARAM = "REBOOTNODEUPGRADE";
    public static final String CRON_EXP = "cronexpression";

    // BackupHousekeeping on NE
    public static final String CLEAR_ALL_BACKUPS = "CLEAR_ALL_BACKUPS";
    public static final String CLEAR_ELIGIBLE_BACKUPS = "CLEAR_ELIGIBLE_BACKUPS";
    public static final String BACKUPS_TO_KEEP_IN_ROLLBACK_LIST = "BACKUPS_TO_KEEP_IN_ROLLBACK_LIST";
    public static final String MAX_BACKUPS_TO_KEEP_ON_NODE = "MAX_BACKUPS_TO_KEEP_ON_NODE";

    // ONLY FOR vRAN nodes
    public static final String POST_ACTIVATE_CHECK_FAILED = "POST_ACTIVATE_CHECK_FAILED";

    // DeleteBackup Properties
    public static final String DELETED_BACKUP_NAME = "backupName";
    public static final String DELETED_BACKUP_FILE = "fileName";
    public static final String LOCATION = "location";

    //NodeRestart 
    public static final String RESTART_REASON = "restartReason";
    public static final String RESTART_RANK = "restartRank";
    public static final String RESTART_INFO = "restartInfo";

    // DeleteUpgradePackage properties
    public static final String PRODUCT_NUMBER = "productNumber";
    public static final String PRODUCT_REVISION = "productRevision";
    public static final String DELETE_REFERRED_UPS = "deleteReferredUPs";
    public static final String DELETE_FROM_ROLLBACK_LIST = "deleteFromRollbackList";
    public static final String DELETE_REFERRED_BACKUPS = "deleteReferredBackups";
    public static final String DELETE_UP_LIST = "deleteUPList";
    public static final String DELETE_NON_ACTIVE_UPS = "deleteNonActiveUps";

    //Auto Generate properties
    public static final String AUTO_GENERATE_BACKUP = "GENERATE_BACKUP_NAME";
    public static final String AUTO_GENERATE_DATE_FORMAT = "ddMMyyyyHHmmss";
    public static final String ROTATE = "Rotate";
    public static final String OVERWRITE = "Overwrite";

    public static final String SECURE_BACKUP_KEY = "Password";
    public static final String WINFIOL_REQUEST_LABEL_HEADER = "Label";
    public static final String USER_LABEL = "Userlabel";
    public static final String DEFAULT_USER_LABEL_FROM_ENM = "Secure backup created from ENM";

}
