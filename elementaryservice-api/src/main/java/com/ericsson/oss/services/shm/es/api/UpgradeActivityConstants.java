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

package com.ericsson.oss.services.shm.es.api;

public class UpgradeActivityConstants {

    public static final String UP_MO_TYPE = "UpgradePackage";
    public static final String UP_PARENT_MO_TYPE = "SwManagement";

    public static final String UPPKG_NAMESPACE = "ImportSoftwarePackage";
    public static final String UPPKG_TYPE_CPP = "CppSoftwarePackage";

    // Install Tasks
    public static final String ACTION_INSTALL = "nonBlockingInstall";
    public static final String ACTION_FORCED_INSTALL = "nonBlockingForcedInstall";
    public static final String ACTION_SELECTIVE_INSTALL = "nonBlockingSelectiveInstall";
    public static final String ACTION_SELECTIVE_FORCED_INSTALL = "nonBlockingSelectiveForcedInstall";
    public static final String ACTION_UPDATE_FTP_SERVER_DATA = "updateFTPServerData";

    // Verify Tasks
    public static final String ACTION_VERIFY_UPGRADE = "verifyUpgrade";

    // Upgrade/Update Tasks
    public static final String ACTION_REBOOT_NODE_UPGRADE = "rebootNodeUpgrade";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_UPGRADE = "upgrade";

    // Confirm Tasks
    public static final String ACTION_CONFIRM_UPGRADE = "confirmUpgrade";

    // Cancel Tasks
    public static final String ACTION_CANCEL_INSTALL = "cancelInstall";
    public static final String ACTION_CANCEL_UPGRADE = "cancelUpgrade";
    public static final String ACTION_CANCEL_DOWNLOAD = "cancelDownload";

    public static final String SWP_NAME = "SWP_NAME";
    public static final String UCF = "UCF";
    public static final String SELECTIVEINSTALL = "SELECTIVEINSTALL";
    public static final String FORCEINSTALL = "FORCEINSTALL";
    public static final String REBOOTNODEUPGRADE = "REBOOTNODEUPGRADE";

    public static final String UP_FDN = "UP_FDN";
    public static final String UP_STATE = "UP_STATE";

    public static final String PROG_HEADER = "PROG_HEADER";
    public static final String PROG_COUNT = "PROG_COUNT";
    public static final String PROG_TOTAL = "PROG_TOTAL";

    public static final String ACTION_ID = "ACTION_ID";

    public static final String ACTION_RESULT_INFO = "info";
    public static final String UPGRADE_PACKAGE_ID = "UpgradePackageId";
    public static final String ADMINISTRATIVE_DATA = "administrativeData";
    public static final String PRODUCT_NUMBER = "productNumber";
    public static final String PRODUCT_REVISION = "productRevision";
    public static final String DELETE_PREVENTING_CVS = "deletePreventingCVs";
    public static final String DELETE_PREVENTING_UPS = "deletePreventingUPs";
    public static final String DELETE_PREVENTING_UP_STATUS = "deletePreventingUpStatus";
    public static final String ECIM_SWINVENTORY_TYPE = "SwInventory";

    public static final String UP_PO_SWPKG_NAME = "swPkgName";
    public static final String UP_PO_NODE_NAME = "nodeName";
    public static final String UP_PO_FILE_PATH = "filePath";
    public static final String UP_PO_HASH = "hash";
    public static final String UP_PO_UCF = "ucfName";
    public static final String UP_PO_PARENT_FDN = "parentFdn";
    public static final String UP_PO_MODEL_VERSION = "upModelVersion";
    public static final String UP_PO_PROD_NUMBER = "productNumber";
    public static final String UP_PO_PROD_NAME = "productName";
    public static final String UP_PO_PROD_DATE = "productionDate";
    public static final String UP_PO_PROD_INFO = "productInfo";
    public static final String UP_PO_PROD_REVISION = "productRevision";
    public static final String UP_PO_PACKAGE_NAME = "packageName";
    public static final String UP_PO_NODE_PLATFORM = "nodePlatform";
    public static final String UP_PO_IMPORTED_BY = "importedBy";
    public static final String UP_PO_IMPORT_DATE = "importDate";
    public static final String UP_PO_DESCRIPTION = "description";
    public static final String UP_PO_JOBPARAMS = "jobParameters";
    public static final String UP_PO_PARAM_NAME = "paramName";
    public static final String UP_PO_ITEMS = "items";
    public static final String UP_PO_VALUE = "value";

    public static final String UP_PO_SWP_PRODUCT_DETAILS = "swpProductDetails";
    public static final String UP_PO_ACTIVITIES = "activities";
    public static final String UP_PO_ACTIVITY_NAME = "name";

    public static final String USERLABEL_PREFIX = "SHM_";

    // for deciding the inventory transfer type
    public static final String TRANSFER_TYPE_DELTA = "delta";
    public static final String TRANSFER_TYPE_FULL = "full";

    // for selecting type
    public static final String TYPE_SELECTIVE = "selective";
    public static final String TYPE_NOT_SELECTIVE = "not selective";
    public static final String ACTION_RESULT_UPGRADE_NOT_POSSIBLE = "not possible to upgrade";
    public static final String FAILURE_IN_UP_HEADER = "failureInUpHeader";

    public static final String IS_VERIFICATION_FINISHED = "isVerificationFinished";

    public static final String UPGRADE_EXECUTING_SEEN = "upgradeExecutingSeen";

    public static final String FURTHER_ANALYSIS_NEEDED = "furtherAnalysisNeeeded";

    public static final String VERIFICATION_FAILED_MSG = "Saw verification failed header. Assuming upgrade failed.";

    public static final String EXECUTION_FAILED_MSG = "Saw execution failed header. Assuming upgrade failed.";

    public static final String AUE_FAILURE = "Application Upgrade Engine has reported failure. Assuming upgrade failed.";

    public static final String UPGRADE_CANCELLED = "Unexpected stop in operation. Upgrade probably canceled.";

    public static final String UPGRADE_EXECUTING_NOT_SEEN = "Never saw the upgrade executing state. Assuming upgrade failed.";

    public static final String UPGRADE_INVOCATION_FAILED = "Invocation of upgrade action failed.";

    public static final String UPGRADE_UNEXPECTED_UP_STATE = "Upgrade stopped in unexpected state: %s";

    public static final String IS_UP_ACTIVE_ON_NODE = "isUpActiveOnNode";

    public static final String INTERMEDIATE_FAILURE = "INTERMEDIATE_FAILURE";

    // ECIM related constants
    public static final String UPPKG_TYPE_ECIM = "EcimSoftwarePackage";
    public static final String URI = "uri";
    public static final String ACTION_ARG_IP_ADDRESS = "ftpServerIpAddress";
    public static final String ACTION_ARG_USER_ID = "user";
    public static final String ACTION_ARG_PASSWORD = "password";
    public static final String ACTION_ARG_UPFILE_PATH_ON_FTPSERVER = "upFilePathOnFtpServer";
    public static final String UPGRADE_TYPE = "UPGRADETYPE";

    //CPP DeleteUP constants
    public static final String IS_PREVENT_UP_DELETABALE = "deleteReferredUPs";
    public static final String IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST = "deleteFromRollbackList";
    public static final String PREVENTING_UP_CV_INFO = "PreventingUpCvInfo";

    public static final String PROCESSED_UPS = "processedUPs";
    public static final String TOTAL_UPS = "totalUPs";
    public static final String CURRENT_USER_PROVIDED_UP = "currentUserProvidedUP";
    public static final String DELETE_UP_LIST = "deleteUPList";
    public static final String DELETABLE_UP_LIST = "deletableUPList";

    public static final String UPGRADEPACKAGES_REQUEST_DELIMTER = "**|**";
    public static final String UPGRADEPACKAGES_PERSISTENCE_DELIMTER = "\\*\\*\\|\\*\\*";
    public static final String PROCESSING_MO_TYPE = "processingItemType";
    public static final String CURRENT_PROCESSING_UP = "currentProcessingUP";

}
