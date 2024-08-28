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

package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

public class MiniLinkConstants {

    public static final String LOG_EXCEPTION = "Exception occurred in \"%s\" for the activity \"%s\" on node \"%s\".";

    public static final String XF_SW_GLOBAL_STATE = "xfSwGlobalState";
    public static final String XF_SW_ACTIVE_RELEASE = "xfSwActiveRelease";
    public static final String XF_SW_RELEASE_OPER_STATUS = "xfSwReleaseOperStatus";
    public static final String XF_SW_RELEASE_ADMIN_STATUS = "xfSwReleaseAdminStatus";
    public static final String XF_DCN_FTP = "xfDcnFTP";
    public static final String XF_SW_UPGRADE_ACTIVE_FTP = "xfSwUpgradeActiveFTP";
    public static final String XF_SW_RELEASE_PRODUCT_NUMBER = "xfSwReleaseProductNumber";
    public static final String XF_SW_RELEASE_REVISION = "xfSwReleaseRevision";
    public static final String XF_SW_VERSION_CONTROL = "xfSwVersionControl";
    public static final String XF_SW_LOAD_MODULE_OPER_STATUS = "xfSwLoadModuleOperStatus";
    public static final String XF_SW_LOAD_MODULE_PROGRESS = "xfSwLoadModuleProgress";
    public static final String XF_SW_LOAD_MODULE_FAILURE = "xfSwLoadModuleFailure";
    public static final String XF_SW_BOARD_PROGRESS = "xfSwBoardProgress";
    public static final String XF_SW_OBJECTS = "xfSwObjects";
    public static final String XF_SW_UPGRADE_PREFERENCES = "xfSwUpgradePreferences";
    public static final String XF_SW_RELEASE_TABLE = "xfSwReleaseTable";
    public static final String XF_SW_RELEASE_ENTRY = "xfSwReleaseEntry";
    public static final String XF_SW_LMUPGRADE_TABLE = "xfSwLmUpgradeTable";
    public static final String XF_SW_LMUPGRADE_ENTRY = "xfSwLmUpgradeEntry";
    public static final String XF_SW_LMUPGRADE_REVISION = "xfSwLmUpgradeRevision";
    public static final String XF_SW_LMUPGRADE_PRODUCT_NUMBER = "xfSwLmUpgradeProductNumber";
    public static final String XF_SW_LMUPGRADE_ADMIN_STATUS = "xfSwLmUpgradeAdminStatus";
    public static final String XF_SW_LMUPGRADE_OPER_STATUS = "xfSwLmUpgradeOperStatus";
    public static final String XF_SW_LMUPGRADE_FAILURE = "xfSwLmUpgradeFailure";
    public static final String XF_SW_BOARD_TABLE = "xfSwBoardTable";
    public static final String XF_SW_BOARD_PRODUCT_NUMBER = "xfSwBoardProductNumber";
    public static final String XF_SW_BOARD_REVISION = "xfSwBoardRevision";
    public static final String XF_SW_BOARD_STATUS = "xfSwBoardStatus";

    public static final String XF_SW_BOOT_TIME = "xfSwBootTime";
    public static final String XF_SW_COMMIT_TYPE = "xfSwCommitType";

    public static final String DOWNLOAD_STARTED = "Software package download started.";
    public static final String DOWNLOAD_PACKAGE = "Software package: \"%s\" type: \"%s\".";

    public static final String MINI_LINK_INDOOR = "MINI-LINK-Indoor";
    public static final String XF_CONFIG_FILE_NAME = "xfConfigFileName";
    public static final String XF_CONFIG_LOAD_OBJECTS = "xfConfigLoadObjects";
    public static final String XF_NE_RESTART_COMMANDS = "xfNERestartCommands";
    public static final String XF_NE_RESTART_OBJECTS = "xfNERestartObjects";
    public static final String XF_CONFIG_LOAD_ACTIVE_FTP = "xfConfigLoadActiveFTP";
    public static final String XF_CONFIG_LOAD_COMMAND = "xfConfigLoadCommand";
    public static final String XF_CONFIG_ACCEPT = "xfConfigAccept";
    public static final String XF_CONFIG_STATUS = "xfConfigStatus";
    public static final String INCORRECT_CONFIG_STATE = "Incorrect config state";
    public static final String NODE_IS_NOT_SYNCED = "Node is not synchronized!";
    public static final String BACKUP_FILE_DOES_NOT_EXIST = "Backup file does not exist";
    public static final String NO_BACKUP_FILE_SELECTED = "No backup file selected for deletion";
    public static final String EXCEPTION_OCCURED_FAILURE_REASON = "exception occured";
    public static final String CM_FUNCTION = "CmFunction";
    public static final String SYNC_STATUS = "syncStatus";
    public static final String FTP_BUSY = "Failed, Due to Ongoing File Transfer";
    public static final String SET_FTP_FAILED = "FTP details could not be set, As FTP connection is busy";
    public static final String XFDCNFTPACTIVE_VALUE = "ftp1";
    public static final String CONFIG_DOWN_LOAD_OK_NOT_SET = "configDownLoadOK is not set on node";

    public static final String XF_LICENSE_INSTALL_OBJECTS = "xfLicenseInstallObjects";
    public static final String XF_LICENSE_INSTALL_FILE_NAME = "xfLicenseInstallFileName";
    public static final String XF_LICENSE_INSTALL_ADMIN_STATUS = "xfLicenseInstallAdminStatus";
    public static final String XF_LICENSE_INSTALL_OPER_STATUS = "xfLicenseInstallOperStatus";
    public static final String XF_LICENSE_INSTALL_ACTIVE_FTP = "xfLicenseInstallActiveFTP";

    public static final String CONFIG_FILE_EXTENSION = "cfg";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";

    public static final String BACKUP_NAME = "BACKUP_NAME";
    public static final String BACKUP_FILE_NAME = "BACKUP_FILE_NAME";
    public static final String GENERATE_BACKUP_NAME = "GENERATE_BACKUP_NAME";

    public static final String PROCESSED_BACKUPS = "PROCESSED_BACKUPS";
    public static final String INTERMEDIATE_FAILURE = "INTERMEDIATE_FAILURE";
    public static final String CURRENT_BACKUP = "currentBackup";
    public static final String TOTAL_BACKUPS = "totalBackups";

    public static final String LIVE_BUCKET = "Live";
    public static final String OSS_NE_CM_DEF = "OSS_NE_CM_DEF";

    public static final String BACKUP_JOB = "Backup job";
    public static final String RESTORE_JOB = "Restore job";
    public static final String UPGRADE_JOB = "Upgrade job";
    public static final String LICENSE_JOB = "License job";
    public static final String LICENSE_FILEPATH = "LICENSE_FILEPATH";
	public static final String SMRS_PATH = "/home/smrs/MINI-LINK/MINI-LINK-Indoor/tn_licenses/";

    public static final String XF_DCN_FTP_ACTIVE = "xfDcnFTPActive";
    public static final String XF_NE_PM = "xfNEPm";
    public static final String SMRS_NODE_TYPE = "MINI-LINK-Indoor";
    public static final String XF_PM_FILE_STATUS = "xfPMFileStatus";
    public static final String XF_DCN_FTP_ADDRESS_1 = "xfDcnFTPAddress1";
    public static final String XF_DCN_FTP_USERNAME_1 = "xfDcnFTPUserName1";
    public static final String XF_DCN_FTP_CONFIG_PWD = "xfDcnFTPPassword1";
    public static final String NETWORKELEMENT = "NetworkElement=";
    public static final String OSSMODELIDENTITY = "ossModelIdentity";
    public static final String CONFIG_UPLOADING = "configUpLoading";
    public static final String CONFIG_DOWNLOADING = "configDownLoading";
    public static final String TRANSFER_ACTIVE = "transferActive";
    public static final String MODEL_IDENTITY = "M11B-TN-4.4FP.7";
    public static final String MINILINK_INDOOR_MODEL_IDENTITY ="M11B-TN-4.4FP.7";
    public static final String CN210_MODEL_IDENTITY ="M12A-CN210-1.2";
    public static final String CN510R1_MODEL_IDENTITY ="M12A-CN510R1-1.2";
    public static final int FTP_RETRY_ATTEMPTS = 3;
    public static final int FTP_RETRY_INTERVAL = 60;
    public static final String DEFAULT_USERNAME = "anonymous";
    public static final String DEFAULT_PWD = "anonymous";
    public static final String DEFAULT_ADDRESS = "10.0.0.2";

    public static final String DATE_FORMAT_TOBE_APPENDED_TO_BACKUPNAME = "ddMMyyyyHHmmss";
    public static final int BACKUP_FILE_NAME_LENGTH_LIMIT = 80;
    public static final int TIMESTAMP_WITH_EXT_LENGTH =19;
    public static final int EXTENSION_LENGTH =4;

    public static enum XfSwReleaseOperStatus {
        passive(1),
        upgradeStarted(2),
        upgradeFinished(3),
        testing(4),
        upgradeFailed(5),
        upgradeAborted(6),
        running(7),
        testingFromManual(8),
        errorInternal(50),
        errorFileStorage(51),
        ftpPingFailed(52),
        ftpNoAccess(53),
        ftpConnectionDetailsMissing(54),
        ftpConnectionDetailsInvalid(55),
        ftpConnectionTimeout(56),
        ftpNoSuchRemoteFile(57),
        ftpNoSuchRemoteDir(58),
        ftpServiceNotAvailable(421),
        ftpUnableToOpenDataConnection(425),
        ftpConnectionClosed(426),
        ftpFileBusy(450),
        ftpLocalError(451),
        ftpInsufficientStorageSpace(452),
        ftpSyntaxError(501),
        ftpCommandNotImplemented(502),
        ftpBadSequenceCommands(503),
        ftpParameterNotImplemented(504),
        ftpNoLoggedIn(530),
        ftpNeedAccount(532),
        ftpFileUnavailable(550),
        ftpExceededStorageAllocation(552),
        ftpFileNameNotAllowed(553);

        private final int statusValue;

        private XfSwReleaseOperStatus(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }

    }

    public enum XfSwReleaseAdminStatus {
        upgradeStarted(1),
        upgradeAborted(2),
        activeAndRunning(5),
        upgradeTest(6);

        private final int statusValue;

        private XfSwReleaseAdminStatus(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfSwGlobalState {
        noUpgrade(0),
        sblStarted(1),
        sblWaitForActivate(2),
        sblWaitForCommit(3),
        manualStarted(4),
        manualWaitForActivate(5),
        manualWaitForCommit(6),
        unitUpgrade(7),
        cachingLoadModules(8),
        preparingForTest(9);

        private final int statusValue;

        private xfSwGlobalState(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfLicenseInstallOperStatus {
        LKF_INSTALL_FINISHED(0, "lkfInstallFinished"),
        LKF_DOWNLOAD_STARTED(1, "lkfDownloadStarted"),
        LKF_VALIDATION_STARTED(2, "lkfValidationStarted"),
        LKF_INSTALLING_ON_RMM(3, "lkfInstallingOnRMM"),
        LKF_ENABLING(4, "lkfEnabling"),
        UNKNOWN_VERSION(5, "unKnownVersion"),
        UNKNOWN_SIGNATURETYPE(6,"unKnownSignatureType"),
        UNKNOWN_FINGERPRINT_METHOD(7, "unknownFingerprintMethod"),
        UNKNOWN_FINGERPRINT(8, "unknownFingerprint"),
        ERROR_CORRUPT_SIGNATURE(9, "errorCorruptSignature"),
        ERROR_NO_SPACE_ON_RMM(10, "errorNoSpaceOnRMM"),
        ERROR_RMM_UNAVAILABLE(11, "errorRMMUnavailable");

        private final int statusValue;
        private final String status;

        private xfLicenseInstallOperStatus(final int statusValue, final String status) {
            this.statusValue = statusValue;
            this.status = status;
        }

        public int getStatusValue() {
            return statusValue;
        }

        public String getStatus() {
            return status;
        }
    }

    public enum xfSwVersionControl {
        enable(1),
        disable(2);

        private final int statusValue;

        private xfSwVersionControl(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfswCommitType {
        operatorCommit(1),
        nodeCommit(2);

        private final int statusValue;

        private xfswCommitType(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfSwLmUpgradeAdminStatus {
        upgradeStarted(1),
        upgradeAborted(2),
        activeAndRunning(3),
        upgradeTest(4);

        private final int statusValue;

        private xfSwLmUpgradeAdminStatus(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfSwBoardStatus {
        unknown(1),
        active(2),
        upgrading(3),
        wrongSoftware(4),
        minSoftwareRevision(5);

        private final int statusValue;

        private xfSwBoardStatus(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfSwLmUpgradeFailure {
        downloadFailure(1),
        programFailure(2),
        noFailure(3),
        swIntegrityFailure(11);

        private final int statusValue;

        private xfSwLmUpgradeFailure(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

    public enum xfConfigAccept {
        acceptFactorySettings(1),
        acceptFTP(2),
        acceptFlash(3),
        acceptRMM(4);

        private final int statusValue;

        private xfConfigAccept(final int statusValue) {
            this.statusValue = statusValue;
        }

        public int getStatusValue() {
            return statusValue;
        }
    }

}
