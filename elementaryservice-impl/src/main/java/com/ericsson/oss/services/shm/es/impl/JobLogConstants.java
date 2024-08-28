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

public class JobLogConstants {

    public static final String ACTIVITY_INITIATED = "\"%s\" activity initiated. ";
    public static final String BACKUP_NAME = "BackupName = \"%s\"";
    public static final String CREATE_BACKUP_WITH_OPTION = "%s with BackupName = \"%s\" is completed succesfully";
    public static final String CREATE_BACKUP_WITH_ROTATE_OPTION = "%s with BackupName = \"%s\" and %s to RELFSW0 is completed succesfully";
    public static final String CREATE_BACKUP_WITH_ENCRYPTION_OPTION = "%s with BackupName = \"%s\" and %s to RELFSW0 is completed succesfully";
    public static final String BACKUP_FILE_NAME = "BackupFileName = \"%s\"";
    public static final String NO_DIRECTORY_FOUND = "Directory for Upload Backup doesn't exist.";
    public static final String PROCESSING_PRECHECK = "Executing precheck before \"%s\"";
    public static final String PRE_CHECK_SUCCESS = "Precheck for \"%s\" is successful.";
    public static final String PRE_CHECK_FAILURE = "Unable to proceed \"%s\" activity because \"%s\".";
    public static final String PRE_CHECK_ACTIVITY_SKIP = "Precheck for \"%s\" is completed and activity will be skipped as it is already completed on node";
    public static final String MO_NOT_EXIST = "Precheck for \"%s\" is failed. Reason: \"%s\" MO not found.";
    public static final String CANCEL_MO_NOT_EXIST = "\"%s\" activity cannot be triggered. Reason: Node is not in sync with OSS.";
    public static final String ACTIVITY_SKIP = "\"%s\" activity is skipped as \"%s\"";
    public static final String ACTIVITY_SKIPPED = "\"%s\" activity is skipped.";

    public static final String MO_CREATION_FAILED = "Unable to start \"%s\" Activity as creation of Upgrade Package MO failed. ";

    public static final String MO_CREATION_FAILED_INSTALL = MO_CREATION_FAILED + "Package: \"%s\" ";

    public static final String INSUFFICIENT_INPUTS = "Unable to proceed \"%s\" Activity because of insufficient inputs.";
    public static final String ACTION_TRIGGERED = "\"%s\" activity is triggered on the node.";
    public static final String ACTION_TRIGGERING = "\"%s\" action is going to trigger on node MO having FDN: ";
    public static final String CANCEL_INVOKED = "Cancel request received for \"%s\" by \"%s\".";
    public static final String CANCEL_INVOKED_ON_NODE = "Cancel of \"%s\" activity triggered on node";
    public static final String ASYNC_ACTION_TRIGGERED = "\"%s\" activity is triggered (timeout = \"%s\" minutes).";
    public static final String ASYNC_ACTION_TRIGGERED_WITH_ID = "\"%s\" activity (action = \"%s\", actionId = \"%s\") is triggered with timeout of \"%s\" minutes.";
    public static final String ADDING_CV = "Configuration Version Name = \"%s\"";
    public static final String ACTION_TRIGGERED_WITH_ACTION_ID = "\"%s\" activity is triggered on the node (actionid = \"%s\")";
    public static final String ASYNC_ACTION_TRIGGERED_WITH_ACTION_ID = "\"%s\" activity is triggered on the node (actionid = \"%s\") with timeout of \"%s\" minutes";
    public static final String ASYNC_ACTION_TRIGGERED_WITH_ACTION_NAME = "\"%s\" activity is triggered on the node (actionName = \"%s\", actionid = \"%s\") with timeout of \"%s\" minutes";
    public static final String ACTION_TRIGGER_FAILED = "Unable to trigger \"%s\" activity on the node. ";
    public static final String ACTION_ABORTED = "\"%s\" activity is aborted due to cancel.";
    public static final String CANCEL_NOT_POSSIBLE = "Node does not support cancellation of \"%s\" activity. Proceeding ahead for completion of the activity.";
    public static final String FAILURE_INVALID_ACTION_ID = "as the returned action id is -1";
    public static final String FAILURE_DUE_TO_EXCEPTION = "as an exception occurred : ";

    public static final String ACTIVITY_COMPLETED_SUCCESSFULLY = "\"%s\" activity is completed successfully.";
    public static final String ACTIVITY_CANCELLED_SUCCESSFULLY = "\"%s\" activity is cancelled successfully.";
    public static final String NODE_IS_REACHABLE = "Node is reachable.";
    public static final String VERIFY_RESTORE_STATUS = "Verifying restore activity status.";
    public static final String NODE_NOT_REACHABLE = "Failing restore activity as node is not reachable till activity timeout.";
    public static final String RESTORE_IN_PROGRESS = "Restore activity not completed yet. Waiting until timeout.";
    public static final String ACTIVITY_FAILED = "\"%s\" activity has failed.";
    public static final String UNABLE_TO_TRIGGER_ACTIVITY_FAILED_REASON = "Unable to trigger \"%s\" activity on the node. Failure reason: \"%s\"";
    public static final String ADDITIONAL_FAILURE_RESULT = "\"%s\" activity failed with reason \"%s\" : \"%s\".";
    public static final String ACTIVITY_ROLLBACK = "\"%s\" activity is rolled back.";
    public static final String ACTIVITY_FAILED_FOR_BACKUP = "\"%s\" activity has failed for backup \"%s\"";
    public static final String TIMEOUT = "Notifications not received for the \"%s\" activity. Retrieving status from node.";
    public static final String PRECHECK_TIMEOUT = "Precheck not completed for the \"%s\" activity with in \"%s\" minutes. Failing the Activity.";
    public static final String TIMEOUT_FOR_HANDLE_TIMEOUT = "Failed to get the \"%s\" activity status from the node within \"%s\" minutes. Failing the Activity.";
    public static final String CANCEL_TIMEOUT = "Notifications not received for the \"%s\" activity after cancel is triggered. Retrieving status from node.";
    public static final String SERVICE_BUSY_STATUS = "\"%s\" is still running or its result is not yet available, waiting untill activity is timedout.";
    public static final String ACTION_CANCELLED = "Unable to proceed \"%s\" Activity due to cancel.";

    public static final String OPERATION_TIMED_OUT = "Activity \"%s\" timed out. Retrieving status from node.";

    public static final String EXECUTING = "Executing \"%s\".";
    public static final String NEJOB_CANCELLED = "NE Job with NEName= \"%s\" is cancelled.";
    public static final String ACTIVITY_JOB_CANCELLED = "\"%s\" activity is cancelled successfully.";
    public static final String JOB_CANCEL_SKIPPED = "Cancel of \"%s\" job is skipped ";
    public static final String SKIP_NE_JOB_CANCEL = "Skipping cancellation of NE Job, as job is already completed.";

    public static final String CV_CREATED_SUCCESSFULLY = "Configuration Version \"%s\" is created successfully.";
    public static final String UNABLE_TO_CREATE_CV = "Unable to Create Configuration Version \"%s\".";

    public static final String MO_IS_BUSY = "\"%s\". Unable to proceed \"%s\" activity.";

    public static final String STILL_EXECUTING = "Failing \"%s\" Activity as it is taking more than expected time.";

    public static final String STILL_RUNNING = "Failing \"%s\" activity as it is still executing on the node.";

    public static final String WORKFLOW_SERVICE_INVOCATION_FAILED = "\"%s\" completed, notifying wfs has failed. Waiting until timeout.";

    public static final String EXECUTION_TRIGGERED_EVALUATE_RESULT = "Resuming Execution for Activity \"%s\". Activity executed already proceeding to evaluate result.";
    public static final String ACTION_UPDATE_FTP_FAILED = "Unable to trigger \"%s\" action on the Fdn: \"%s\"";

    public static final String UP_MO_CREATED = "An UpgradePackage MO created on the node with Fdn: \"%s\"";
    public static final String FTPSERVER_DATA_UPDATED = "FtpServerData updated on existing UpgradePackage MO(%s), with actionId: \"%s\"";

    public static final String UP_MO_FDN_IS_EMPTY = "Unable to retrieve UpgradePackage MO Fdn.";

    public static final String UP_MO_STATE = "UpgradePackage State : \"%s\"";

    public static final String UP_MO_PROGRESS_HEADER = "UpgradePackage header : \"%s\"";

    public static final String NE_PLATFORMQUERY_FAILED = "Failed to query the NetworkElements.";

    public static final String MAIN_ACTION_RESULT = "Main action result: \"%s\"";

    public static final String EXTRA_ACTION_RESULT = "Extra action result: \"%s\"";

    public static final String FETCH_ACTION_RESULT = "Fetching action result.";

    public static final String FETCH_ACTION_RESULT_FAILED = "Failed to fetch action result. Assuming %s activity failed.";

    public static final String MAIN_ACTION_RESULT_NOT_FOUND = "Main action result not found.";

    public static final String ADDTIONAL_INFO = " Additional Info: \"%s\"";

    public static final String FAILURE_REASON = "Failure reason: \"%s\"";
    public static final String UP_MO_READ_FAILURE_MESSAGE = "Unable to read the Upgrade package MO FDN from Database";
    public static final String ACTIVITY_EXECUTE_FAILED_WITH_REASON = ACTIVITY_FAILED + " " + FAILURE_REASON;
    public static final String UPGRADE_PACKAGE_CREATION_FAILURE = "Failed to create UpgradePackage on the Node for \"%s\"";

    public static final String RESULT_EVALUATION_FAILED = "Failed to evaluate result for %s activity. Reason: \"%s\"";
    public static final String STATUS_EVALUATION_FAILED = "Failed to get the %s activity status from the node." + FAILURE_REASON;

    public static final String URI_BUILD_FAILED = "Failed to build URI for uploading backup";
    public static final String BACKUP_CORRUPTED = "\"%s\". Unable to proceed \"%s\" activity.";

    public static final String PRECHECK_SUCCESS = "Precheck for \"%s\" activity is successful.";
    public static final String MO_DOES_NOT_EXIST = "\"%s\"";
    public static final String UNSUPPORTED_NODES = "Nodes [\"%s\"] will not be processed. ";
    public static final String WORKFLOW_SUBMISSION_FAILED = "Workflow submission failed for \"%s\"";
    public static final String INFO = "info";
    public static final String BRMBACKUPMANAGER_NOT_FOUND = "BrmBackupManager MO not found for the node \"%s\", Unable to proceed for action.";
    public static final String FRAGMENT_NOT_SUPPORTED = "UnSupported Fragment for the node \"%s\", Unable to proceed for action.";
    public static final String NODE_READ_EXCEPTION = "Unable to contact to the node. Reason: \"%s\"";
    public static final String WARN = "warn";
    public static final String DEBUG = "debug";
    public static final String ERROR = "error";
    //Added for Remoting
    public static final String COLLECTION_EXCEPTION = "Unable to fetch Network Elements for collection \"%s\"";
    public static final String SAVEDSEARCH_EXCEPTION = "Unable to fetch Network Elements for SavedSearch \"%s\"";

    public static final String CORRUPTED = "Corrupted";
    public static final String MISSING = "Missing";
    public static final String UP_AS_FOLLOWS = "Number of \"%s\" Upgrade Packages are \"%s\".";
    public static final String UP_DETAILS = "\"%s\" Upgrade Packages %s : \"%s\"";

    public static final String JOB_CONFIGURATION_ERROR = "Job Configuration Error : \"%s\"";
    public static final String SW_PACKAGE_NOT_FOUND = "Couldn't find software package. Unable to proceed action for node \"%s\"";

    public static final String ACTIVITY_STEP_COMPLETED_SUCCESSFULLY = "\"%s\" activity step is completed successfully. Proceeding to start next step";
    public static final String EXCEPTION_OCCURED = "Following exception has occurred in the middle of operation \"%s\": ";

    public static final String CANCEL_IN_PROGRESS = "Cancellation is in progress with Percentage = \"%s\" and Information = \"%s\" ";
    public static final String PROGRESS_INFORMATION = "Progress Info : Action Name= \"%s\" ProgressPercentage=\"%s\" State= \"%s\"  ";
    public static final String PROGRESS_INFORMATION_WITH_RESULT = "Progress Info : Action Name= \"%s\" ProgressPercentage=\"%s\" State= \"%s\" Result= \"%s\" ";
    public static final String EXECUTION_STARTED = "Executing \"%s\" activity on backup file = \"%s\".";
    public static final String MISMATCHED_PARAMETERS = "Mismatched parameters: \"%s\"  = \"%s\".";
    public static final String MATCHED_PARAMETERS = "Matched parameters: \"%s\"  = \"%s\".";
    public static final String ONEGO_ACTIVATION = "One Go activation is selected";
    public static final String STEPBYSTEP_ACTIVATION = "Step by Step activation is selected. Total Number of Steps : \"%d\"";
    public static final String STEPBYSTEP_TRIGGERED = "Step \"%d\" : \"%s\" activity is triggered (timeout = %s minutes).";
    public static final String NETWORKELEMENT_NOT_FOUND = "NetworkElement MO is not found for the node : \"%s\".";
    public static final String BRMROLLBACKATRESTORE_NOT_FOUND = "BrmRollbackAtRestore MO not found on the node";
    public static final String LASTRESTOREDBACKUP_NOT_SET = "Last restored backup value is not set in BrmBackupLabelStore MO for the node \"%s\"";
    public static final String PRE_CHECK_SUCCESS_SKIP_EXECUTION = "Precheck for \"%s\" is successful. But execution skipped because \"%s\". ";
    public static final String RESTORE_CONFIRM_REQUIRED = "Confirm Restore action is required for successful restore. ";
    public static final String RESTORE_CONFIRM_NOT_REQUIRED = "Confirm Restore action is not required. Restore is successful.";
    public static final String ECIM_FAILURE_REASON = "\"%s\" failed due to Reason : \"%s\"";
    public static final String FAILURE_REASON_NOT_AVAILABLE = "Failure reason is not available on the node";
    public static final String EXCEPTION_OCCURED_WHILE_ACTION = "Exception has occurred in the middle of action \"%s\": ";
    public static final String WAIT_FOR_USER_INPUT = "Waiting for user to proceed to next Step";
    public static final String PRE_CHECK_PARTIAL_SUCCESS = "Precheck for \"%s\" is partial successful.";
    public static final String NO_ACTION_TRIGGERED = "None of the required actions are triggered.";
    public static final String BRMFAILSAFEBACKUP_NOT_FOUND = "BrmFailsafeBackup MO not found for the node \"%s\". Unable to proceed for action.";
    public static final String STEP_COMPLETED_SUCCESSFULLY = "Failsafe \"%s\" step is completed successfully.";
    public static final String NODE_IS_IN_TREAT_AS_SUPPORT = "Node is in treat as support with ENM supported : { MIM version = \"%s\" , Release version = \"%s\" , Product data =  \"%s\" }. Node product data is : \"%s\".";
    public static final String CV_ALREADY_EXISTS_EXCEPTION = "cvalreadyexist";
    public static final String CONFIGURATION_VERSION_FILE_SYSTEM_ERROR = "configurationversionfilesystemerror";
    public static final String CONFIGURATION_VERSION_FORMAT_ERROR = "cvformaterrorexception";
    public static final String CV_MAX_NUMBER_OF_INSTANCES_EXISTS = "cv_maxnumberofinstanc";
    public static final String UNABLE_TO_CREATE_DUPLICATE_CV = "Configuration Version already exists on node.";
    public static final String UNABLE_TO_CREATE_CV_MAX_NUMBER_OF_INSTANCES_EXISTS = "Maximum no of cvs created.";
    public static final String UNABLE_TO_CREATE_CONFIGURATION_VERSION_FILE_SYSTEM_ERROR = "Configuration Version file system error, check the disk on the node to see if it is nearly full or corrupted";
    public static final String UNABLE_TO_CREATE_CONFIGURATION_VERSION_FORMAT_ERROR = "Configuration Version name is not valid. Only the characters \"A - Z\", \"a -z\", \"0 - 9\", \"-\", \"_\", \".\" , \"%\" and \":\" are allowed.";
    public static final String FTPSERVER_DATA_NOT_UPDATED = "Proceeding with existing ftp server details in UpgradePackage MO";
    public static final String BACKUP_NOT_FOUND = "selected backup could not be found";

    public static final String NODE_READ_FAIL = "Unable to retrieve data for node \"%s\".";

    public static final String MO_NOT_FOUND_UNSUPPORTED_NODE_MODEL = "\"%s\" MO is not found due to unsupported node model.";
    public static final String UNSUPPORTED_NODE_MODEL = "Unsupported node model for NE type \"%s\" and OSS model Identity \"%s\".";
    public static final String ACTIVITY_SCHEDULE_TIME_INFO = "\"%s\" activity is scheduled to run at \"%s\".";
    public static final String ACTIVITY_SCHEDULE_TIME_WAS = "\"%s\" activity was originally scheduled at \"%s\". As the scheduled time is prior to current time, this activity is triggered now.";
    public static final String ACTION_FAILED = "Unable to proceed for action. ";
    public static final String BACKUPS_AVAILABLE_LESS_THAN_OR_EQUAL_TO_BACKUPS_TO_KEEP_ON_NODE = "Backups available on the node are less than or equal to backups to keep on the node. Total backups on node:%s, backups to keep on node:%s";
    public static final String CV_AVAILABLE_LESS_THAN_OR_EQUAL_TO_CV_TO_KEEP_ON_NODE = "CV's available on the node are less than or equal to CV's to keep on the node. Number of CV's on node:%s, Number of CV's to keep on node:%s";
    public static final String CV_IN_ROLLBACK_LIST_IS_LESS_THAN_OR_EQUAL_TO_CV_TO_KEEP_IN_ROLLBACK_LIST = "CV's available in the rollback list is less than or equal to CV's to keep in rollback list, Number of CV's in rollbacklist:%s, Number of CV's to keep in rollbacklist:%s";

    public static final String FETCH_WAITING_MAINJOB_WORKFLOW_ID_FAILED = "Failed to continue Main Job workflow for the job \"%s\" triggered by \"%s\". Failed reason : %s .";
    public static final String FETCH_WAITING_ACTIVITYJOB_WORKFLOW_ID_FAILED = "Failed to continue Activity Job workflow for the activity \"%s\" triggered by \"%s\". Failed reason :  %s .";
    public static final String WORKFLOW_NOT_IN_WAIT_FOR_USER_INPUT_STATE = "workflow not in waiting state";
    public static final String CORRELATION_FAILED = "Unable to complete user task because \"%s\"";

    public static final String SKIP_RESTORE_JOB_CV_EXISTS_ON_NODE = "Configuration Version \"%s\" is present on node with type \"%s\". Skipping \"%s\" activity as CV is not in DOWNLOADED state.";
    public static final String SET_STARTABLE_EXECUTE_ACTION = "Triggering set as startable action with Configuration Version \"%s\"";
    public static final String RESTORE_ACTIVITY_FAILED_CV_DOES_NOT_EXISTS = "Restore activity has failed. Reason: CV \"%s\" does not exist on the node.";
    public static final String RESTORE_ACTIVITY_FAILED = "Restore activity has failed for the CV \"%s\". Reason: %s.";
    public static final String SET_STARTABLE_ACTION_FAILED = "Set as startable action has failed for configuration version \"%s\".";

    public static final String SET_STARTABLE_ACTION_SUCCESS = "Configuration Version \"%s\" has been set as a Startable CV Successfully.";
    public static final String ACTIVITY_FAILED_IN_TIMEOUT = "\"%s\" action has failed in handle timeout. Reason: \"%s\".";
    public static final String SET_STARTABLE_ACTION_FAILED_WITH_EXCEPTION = "Set as startable action has failed for configuration version  \"%s\". Reason: \"%s\"";
    public static final String CONFIGURATION_VERSION_NOT_LOADED = "Restore of Configuration version \"%s\" has failed";
    public static final String RESTORE_BY_SETSTARTABLE_NODERESTART = "Triggering set as startable and node restart actions on the node as Configuration Version \"%s\" is not in DOWNLOADED state";
    public static final String NODE_RESTORE_SUCCESSFUL = "Node Restored successfully with Configuration Version \"%s\".";
    public static final String UNABLE_TO_PROCEED_ACTION = "Unable to proceed \"%s\" activity because \"%s\".";

    public static final String ACTION_NAME_MISMATCH_FAILURE_MSG = "\"%s\" activity failed. Failure reason: Expected action name : \"%s\" ; Received action name from node : \"%s\"";
    public static final String CV_ALREADY_EXISTS_ON_NODE = "Configuration Version \"%s\" is already present on node with type \"%s\". Skipping the \"%s\" activity.";

    public static final String KEYFILE_MISMATCH_WITH_NETYPE = "Unable to proceed \"%s\" activity because \"License key file with product type as %s cannot be installed on the node : %s\"";

    public static final String ACTIVITY_COMPLETED_THROUGH_POLLING = "\"%s\" activity is completed through Polling.";
    public static final String ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS = "\"%s\" activity is completed through Notifications.";

    public static final String CANCELLATION_FAILED = "Cancellation of \"%s\" activity Failed.";

    public static final String SYNC_STATUS_CHANGE_MESSAGE = "syncStatus changed to %s";
    public static final String DATABASE_SERVICE_NOT_ACCESSIBLE = "Database service is not accessible.";

    public static final String ACTIVITY_FAILED_WITH_REASON = "Precheck for \"%s\" is failed. Reason: \"%s\".";
    //Delete Upgradepackage constants
    public static final String UP_WITH_PNUM_PREV = "UpgradePackage with ProductNumber: \"%s\" and ProductRevision: \"%s\"";
    public static final String UP_DELETED_SUCCESSFULLY = "Successfully deleted the " + UP_WITH_PNUM_PREV + " From the Node.";
    public static final String UP_DELETION_FAILED = "Failed to delete the " + UP_WITH_PNUM_PREV + " From Node.";
    public static final String UP_ACTION_TRIGGER_FAILED = "Unable to trigger \"%s\" activity on the Node having UpMoFdn \"%s\"";
    public static final String ACTION_TRIGGER_FAILED_WITH_REASON = ACTION_TRIGGER_FAILED + FAILURE_REASON;
    public static final String DELETING_CV_FOR_DELTAUP = "CV: \"%s\" will be deleted for Delta " + UP_WITH_PNUM_PREV;
    public static final String CV_REMOVED_FROM_ROLLBACK = "CV: \"%s\" removed from Rollback List.";
    public static final String INSTALL_TRIGGER_FAILED = "Unable to trigger \"%s\" activity on the node for Package: \"%s\"";
    public static final String INSTALL_TRIGGER_FAILED_WITH_REASON = INSTALL_TRIGGER_FAILED + FAILURE_REASON;
    public static final String CV_REMOVAL_FROM_ROLLBACK_FAILED = "Unable to remove CV: \"%s\". " + FAILURE_REASON;
    public static final String UP_DELETE_PREVENTING_ITEMS_DOES_NOT_EXIST = UP_WITH_PNUM_PREV + " is not having any deletePreventingCVs and deletePreventingUPs";
    public static final String PROCCESSING_ITEMS_LIST = UP_WITH_PNUM_PREV + " is having deletePreventingCVs: %s and deletePreventingUPs: %s";
    public static final String CPP_UP_AND_BACKUPDATA_FOR_DELETION = "Deletable " + UP_WITH_PNUM_PREV + " and following is/are deletePreventingCV(s) \"%s\"";
    public static final String UP_MO_DATA = UP_WITH_PNUM_PREV + " cannot be deleted, since it %s on the node";
    public static final String FILTERED_UP_DATA_FOR_DELETION = UP_WITH_PNUM_PREV + " can be deleted";
    public static final String UP_AND_BACKUPDATA_FOR_DELETION = "Deletable " + UP_WITH_PNUM_PREV + " and following is/are referred backup(s) \"%s\"";
    public static final String UP_MO_FDN_NO_BACKUPS = "Proceeding with deletion of " + UP_WITH_PNUM_PREV + " as there are no referred backup(s)";
    public static final String DELETING_DELTAUP_NO_BACKUPS = "Proceeding with deletion of Delta " + UP_WITH_PNUM_PREV;
    public static final String DELETION_SKIP_IF_REFERREDBKPS = UP_WITH_PNUM_PREV + " cannot be deleted as following is/are referred backup(s) \"%s\"";
    public static final String DELETEUP_TIMEOUT = "Notifications not received for the \"%s\" activity for " + UP_WITH_PNUM_PREV + ". Retrieving status from node.";

    public static final String DELETEUP_ASYNC_ACTION_TRIGGERED = "\"%s\" activity is triggered (timeout = \"%s\" minutes) on " + UP_WITH_PNUM_PREV;
    public static final String DELETEUP_ACTION_TRIGGER_FAILED = "Unable to trigger \"%s\" activity on " + UP_WITH_PNUM_PREV;
    public static final String DELETEUP_COMPLETED_SUCCESSFULLY = "\"%s\" activity is completed successfully for " + UP_WITH_PNUM_PREV;
    public static final String DELETEUP_ACTIVITY_FAILED = "\"%s\" activity has failed for " + UP_WITH_PNUM_PREV;
    public static final String DELETEUP_ACTION_TRIGGERING = "\"%s\" action is going to trigger on " + UP_WITH_PNUM_PREV;

    public static final String REFERREDBKP_ACTION_TRIGGERING = "\"%s\" action is going to trigger on backup \"%s\" ";
    public static final String MOCI_CONNECTION_MESSAGE = "Unable to establish connection with the node. Error response:";

    public static final String BRM_BACKUP_MO_DOES_NOT_EXIST_IN_ENM_DB = "BrmBackup MO doesn't exist in ENM DB and \"%s\"";
    public static final String CM_SYNC_STATUS = "CM sync status for the node is \"%s\". ";
    public static final String WAIT_TIME_BEFORE_REPEAT_PRECHECK = "Waiting for \"%s\" minutes.";
    public static final String ATTEMPT = "Attempt: \"%s\". \"%s\"";
    public static final String FINAL_ATTEMPT = "Final Attempt. \"%s\"";

    public static final String DELETEUP_PRECHECK_SKIP = "\"%s\" activity is skipped.";
    public static final String DELETEUP_CANCEL_NOT_POSSIBLE = "Node does not support cancellation of \"%s\" activity";
    public static final String REFERRED_BKP_CANCEL_INVOKED_ON_NODE = "Cancel request received while deleting referred backup of \"%s\"";
    public static final String DELETEBACKUP_ASYNC_ACTION_TRIGGERED = "\"%s\" activity is triggered (timeout = \"%s\" minutes) on \"%s\"";

    public static final String DELETE_BACKUP_ENM_MSG = "Proceeding with deletion of Backup(s) on ENM";
    public static final String FINGER_PRINT_MISMATCH = "Fingerprint of License Key file doesn't match with Node Fingerprint";
    public static final String LICENSE_INSTALLATION_SUCCESS = "The \"Installation of the License Key File\" is Initiated!";
    public static final String FINGER_PRINT_MATCHED = "Proceeding License Install Activity as FingerPrints match.";
    public static final String LICENSE_KEY_FILE_NOT_FOUND = "License Key file not found.";

    public static final String TBAC_ACCESS_DENIED_AT_ACTIVITY_LEVEL = "Access denied. User '%s' does not exist or unauthorized to perform operations on this node.";

    public static final String ACTION_TRIGGERED_FAILED = "Failing \"%s\" activity as action is not triggered on node.";

    public static final String DELETEBACKUP_TIMEOUT = BACKUP_NAME + TIMEOUT;
    public static final String DELETEBACKUP_FAILURE = BACKUP_NAME + ACTION_TRIGGER_FAILED_WITH_REASON;

    public static final String BACKUP_NAME_DOES_NOT_EXIST = "Backup name or AutoGenerateBackupName options are not provided.";
    public static final String ACTIVE_SOFTWARE_DETAILS_NOT_FOUND = "Active Software Details not found for generating backup name.";

    public static final String FAILUREREASON = "Failure reason: ";

    public static final String ACTIVITY_FOR_CV_COMPLETED_SUCCESSFULLY = "\"%s\" activity for the CV \"%s\" is completed successfully.";
    public static final String ACTIVITY_FOR_BACKUP_COMPLETED_SUCCESSFULLY = "\"%s\" activity for the backup \"%s\" is completed successfully.";

    public static final String AUTOGENERATE_DEFAULT_BACKUP = "Generating backup with default backup name as \"%s\" because active software details are not retrived properly or they are not in expected format.";
    public static final String NO_INACTIVEUP_ACTIVITY_FAILED = DELETEUP_PRECHECK_SKIP + " Since no inactive upgrade packages.";
    public static final String ONLY_ONE_INACTIVEUP_ACTIVITY_SKIP = DELETEUP_PRECHECK_SKIP + " Since only one inactive Upgrade Package is available.";
    public static final String NO_BACKUP_DELETED_ON_AP_MO_CHANGE = ACTIVITY_SKIPPED + " Since no backup found deleted through AP MO's rbsConfigLevel change.";
    public static final String RESET_AP_MO_RBS_CONFIG_VALUE = "Found AutoProvisioning MO's rbsConfigLevel value as SITE_CONFIG_COMPLETE. Setting it to READY_FOR_SERVICE.";
    public static final String RESET_AP_MO_RBS_CONFIG_VALUE_DELETE_BACKUP = "A System Created backup \"%s\" found deleted with correction in AutoProvisioning MO's rbsConfigLevel value.";

    public static final String INACTIVEUP_ACTIVITY_TRIGGERED = "\"%s\" activity is going to delete inactive upgrade packages.";

    public static final String AXE_ACTIVITY_SCRIPT_MESSAGE = "Activity \"%s\" is in state \"%s\" with process percentage \"%f\"";
    public static final String AXE_ACTIVITY_TIMEOUT_SCRIPT_MESSAGE = "Received \"%s\" Status : \"%s\" and  process percentage : \"%f\" for upgrade_status request";
    public static final String AXE_ACTIVITY_CLUSTER_SESSION_ID_MESSAGE = "Received Cluster Id \"%s\"  and session Id \"%s\" ";
    public static final String REQUEST_FOR_SCRIPT_EXECUTION_SUBMITTED = "Request for Activity \"%s\" with script \"%s\" execution is submitted by the user \"%s\"";
    public static final String AXE_HANDLE_TIMEOUT = "Notification not received with in \"%s\" minutes for the activity \"%s\". Sending the status request with high priority";
    public static final String AXE_MAX_HANDLE_TIMEOUT = "Notification not received with in \"%s\" minutes for the activity \"%s\". Hence failing the activity";
    public static final String AXE_ACTIVITY_EXECUTION_REQUEST_FAILED = "Failed to submit script execution request to OPS for activity : '%s'";
    public static final String AXE_SCRIPT_EXECUTION_COMPLETED_SUCCESSFULLY = "Script execution is completed successfully for activity : '%s'";
    public static final String AXE_SCRIPT_EXECUTION_FAILED = "Script execution is failed for activity : '%s'";
    public static final String AXE_SCRIPT_EXECUTION_FAILED_REASON = "Script execution is failed for activity : '%s' due to '%s'";

    public static final String AXE_SCRIPT_EXECUTION_INTERRUPTED_REASON = "Script execution is interrupted for activity : '%s' due to '%s'";
    public static final String AXE_SCRIPT_EXECUTION_INTERRUPTED = "Script execution is interrupted for activity : '%s'";
    public static final String AXE_SCRIPT_EXECUTION_RESUMED = "Script execution is resumed for activity : '%s'";
    public static final String AXE_SCRIPT_EXECUTION_STOPPED = "Script execution is stopped for activity : '%s' with progress percentage '%s'";

    public static final String AXE_ACTIVITY_INVALID_STATUS_MESSAGE = "Received unknown or invalid Status \"%s\" ";
    public static final String AXE_ACTIVITY_STATUS_NOT_RECEIVED_MESSAGE = "Not received any status from OPS";
    public static final String AXE_ACTIVITY_IMPROPER_PROGRESS_RECEIVED_MESSAGE = "Improper progress percentage received for STOPPED status from OPS on cancel Triggred";

    public static final String AXE_SYNCHRONOUS_NE_FAIL_EXPLICITLY = "Cannot proceed to next synchronous activity.Reason : Current Activity is not succeeded on other node";
    public static final String AXE_TOPOLOGY_APG_INFO_NOT_FOUND = "Unable to fetch node Topology data or APG Data";
    public static final double PROGRESS_PERCENTAGE_AS_MINUS_ONE = -1.0;
    public static final String CANCELLATION_NOT_SUPPORTED = "Cancellation of \"%s\" activity on the node is not Supported.";
    public static final String CANCELLATION_TIMEOUT = "Cancellation of \"%s\" activity timedout. Retrieving status from node.";
    public static final String AXE_ASYNC_ACTION_TRIGGERED_WITH_PARAM = "\"%s\" activity is triggered with option \"%s\" (timeout = \"%s\" minutes).";
    public static final String AXE_CREATEBKP_TRIGGERED_WITH_ENCRYPTION_SUPPORT = "\"%s\" activity is triggered to create secure backup (timeout = \"%s\" minutes).";
    public static final String AXE_CREATE_REGULAR_BKP_TRIGGERED = "\"%s\" activity is triggered to create regular backup (timeout = \"%s\" minutes).";
    public static final String CREATED_APG_BACKUP_SUCCESSFULLY = "\"%s\" activity for the backup \"%s\" is completed successfully";

    public static final String CV_CANNOT_BE_DELETED_ON_NODE = "Job has skipped as CV: \"%s\" cannot be deleted On Node because cv does not exists or cv is already deleted";
    public static final String CV_CANNOT_BE_DELETED_ON_SMRS = "Job has skipped as CV: \"%s\" cannot be deleted from SMRS because cv does not exists or cv is already deleted";
    public static final String BACKUP_CANNOT_BE_DELETED_ON_NODE = "Job has skipped as Backup: \"%s\" cannot be deleted On Node because backup does not exists or backup is already deleted";
    public static final String BACKUP_CANNOT_BE_DELETED_ON_SMRS = "Job has skipped as Backup: \"%s\" cannot be deleted from SMRS because backup does not exists or backup is already deleted";
    public static final String CV_DOES_NOT_EXISTS = "CVDoesNotExistsException";
    public static final String BACKUP_DOES_NOT_EXISTS = "mo_not_defined";

    public static final String EXECUTE_SUCCESS = "Execute for \"%s\" activity is successful.";
    public static final String FAILED_TO_GET_APGVERSION = "Failed to read APG Version from inventory, make sure inventory data is in sync.";
    public static final String GOT_APGVERSION_IN_INVALID_FORMAT = "Unexpected APG version received: \"%s\"";
    public static final String SUPPORT_SECURE_BKP_FOR_APG = "SHM supports secure backup from APG43L version 3.7.0 and later";
    public static final String FAILED_TO_READ_ACTIVE_SOFTWARE = "Failed to get active software from the node \"%s\"";

}
