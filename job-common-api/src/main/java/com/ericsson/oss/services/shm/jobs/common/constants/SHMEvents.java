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

public class SHMEvents {

    public static final String JOB_SKIPPED = "SHM.JOB_SKIPPED";
    public static final String JOB_CANCEL_SKIPPED = "SHM.JOB_CANCEL_SKIPPED";
    public static final String JOB_CONTINUE_SKIPPED = "SHM.JOB_CONTINUE_SKIPPED";
    public static final String JOB_START = "SHM.JOB_START";
    public static final String JOB_END = "SHM.JOB_END";
    public static final String CLI_IMPORT_SOFTWARE_PKG = "SHM.CLI_IMPORT_SOFTWARE_PKG";
    public static final String CLI_IMPORT_LICENSE_KEYFILE = "SHM.CLI_IMPORT_LICENSE_KEYFILE";
    public static final String SHM_EXTERNAL_CREATE_JOB = "SHM.EXTERNAL_CREATE_JOB";

    public static final String DOWNLOAD_EXECUTE = "SHM.DOWNLOAD_EXECUTE ";
    public static final String DOWNLOAD_ACTION_COMPLETED = "SHM.DOWNLOAD_ACTION_COMPLETED";
    public static final String DOWNLOAD_CANCEL = "SHM.DOWNLOAD_CANCEL";
    public static final String INSTALL_PRECHECK = "SHM.INSTALL_PRECHECK ";
    public static final String INSTALL_EXECUTE = "SHM.INSTALL_EXECUTE ";
    public static final String INSTALL_PROCESS_NOTIFICATION = "SHM.INSTALL_PROCESS_NOTIFICATION";
    public static final String INSTALL_ACTION_COMPLETED = "SHM.INSTALL_ACTION_COMPLETED";
    public static final String INSTALL_TIME_OUT = "SHM.INSTALL_TIME_OUT ";
    public static final String INSTALL_CANCEL = "SHM.INSTALL_CANCEL";
    public static final String INSTALL_ON_ACTION_COMPLETE = "SHM.INSTALL_ON_ACTION_COMPLETE";
    public static final String UPGRADE_PRECHECK = "SHM.UPGRADE_PRECHECK ";
    public static final String UPGRADE_EXECUTE = "SHM.UPGRADE_EXECUTE ";
    public static final String UPGRADE_PROCESS_NOTIFICATION = "SHM.UPGRADE_PROCESS_NOTIFICATION";
    public static final String UPGRADE_ON_ACTION_COMPLETE = "SHM.UPGRADE_ON_ACTION_COMPLETE";
    public static final String UPGRADE_TIME_OUT = "SHM.UPGRADE_TIME_OUT ";
    public static final String UPGRADE_CANCEL = "SHM.UPGRADE_CANCEL";
    public static final String VERIFY_PRECHECK = "SHM.VERIFY_PRECHECK ";
    public static final String VERIFY_EXECUTE = "SHM.VERIFY_EXECUTE ";
    public static final String VERIFY_CANCEL = "SHM.VERIFY_CANCEL ";
    public static final String VERIFY_TIME_OUT = "SHM.VERIFY_TIME_OUT ";
    public static final String VERIFY_ON_ACTION_COMPLETE = "SHM.VERIFY_ON_ACTION_COMPLETE";
    public static final String VERIFY_PROCESS_NOTIFICATION = "SHM.VERIFY_PROCESS_NOTIFICATION";
    public static final String CONFIRM_PRECHECK = "SHM.CONFIRM_PRECHECK ";
    public static final String CONFIRM_TIME_OUT = "SHM.CONFIRM_TIME_OUT ";
    public static final String CONFIRM_EXECUTE = "SHM.CONFIRM_EXECUTE ";
    public static final String CONFIRM_PROCESS_NOTIFICATION = "SHM.CONFIRM_PROCESS_NOTIFICATION";
    public static final String CONFIRM_ON_ACTION_COMPLETE = "SHM.CONFIRM_ON_ACTION_COMPLETE";
    public static final String CREATE_BACKUP_SERVICE_REQUEST_ACTION = "SHM.CREATE_BACKUP_SERVICE_REQUEST";
    public static final String UPLOAD_BACKUP_SERVICE_REQUEST_ACTION = "SHM.UPLOAD_BACKUP_SERVICE_REQUEST";
    public static final String SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION = "SHM.SETSTARTABLE_BACKUP_SERVICE_REQUEST";
    public static final String SETFIRSTINROLLBACK_SERVICE_REQUEST_ACTION = "SHM.SETFIRSTINROLLBACK_SERVICE_REQUEST";
    public static final String DELETE_JOBS = "SHM.DELETE_JOBS ";
    public static final String JOB_CREATE = "SHM.JOB_CREATE ";
    public static final String JOB_TEMPLATE_CREATION = "SHM.JOB_TEMPLATE_CREATION ";
    public static final String CONFIRM_RESTORE_PRECHECK = "SHM.CONFIRM_RESTORE_PRECHECK ";
    public static final String CONFIRM_RESTORE_EXECUTE = "SHM.CONFIRM_RESTORE_EXECUTE ";
    public static final String CONFIRM_RESTORE_TIME_OUT = "SHM.CONFIRM_RESTORE_TIME_OUT ";

    public static final String DELETE_BACKUP_PRECHECK = "SHM.DELETE_BACKUP_PRECHECK ";
    public static final String DELETE_BACKUP_EXECUTE = "SHM.DELETE_BACKUP_EXECUTE ";

    public static final String CREATE_BACKUP_PRECHECK = "SHM.CREATE_BACKUP_PRECHECK ";
    public static final String VERIFY_RESTORE_PRECHECK = "SHM.VERIFY_RESTORE_PRECHECK ";
    public static final String CREATE_BACKUP_EXECUTE = "SHM.CREATE_BACKUP_EXECUTE ";
    public static final String CREATE_BACKUP_TIME_OUT = "SHM.CREATE_BACKUP_TIME_OUT";
    public static final String UPLOAD_BACKUP_PRECHECK = "SHM.UPLOAD_BACKUP_PRECHECK ";
    public static final String UPLOAD_BACKUP_EXECUTE = "SHM.UPLOAD_BACKUP_EXECUTE ";
    public static final String RESTORE_BACKUP_EXECUTE = "SHM.RESTORE_BACKUP_EXECUTE ";
    public static final String IMPORT_BACKUP_EXECUTE = "SHM.IMPORT_BACKUP_EXECUTE ";
    public static final String UPLOAD_BACKUP_TIME_OUT = "SHM.UPLOAD_BACKUP_TIME_OUT ";
    public static final String UPLOAD_PROCESS_NOTIFICATION = "SHM.UPLOAD_PROCESS_NOTIFICATION";
    public static final String SET_STARTABLE_BACKUP_PRECHECK = "SHM.SET_STARTABLE_BACKUP_PRECHECK ";
    public static final String SET_STARTABLE_BACKUP_EXECUTE = "SHM.SET_STARTABLE_BACKUP_EXECUTE ";
    public static final String SET_STARTABLE_BACKUP_TIME_OUT = "SHM.SET_STARTABLE_BACKUP_TIME_OUT ";
    public static final String SET_BACKUP_FIRST_IN_ROLLBACK_LIST_PRECHECK = "SHM.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_PRECHECK ";
    public static final String SET_BACKUP_FIRST_IN_ROLLBACK_LIST_EXECUTE = "SHM.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_EXECUTE ";
    public static final String SET_BACKUP_FIRST_IN_ROLLBACK_LIST_TIME_OUT = "SHM.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_TIME_OUT ";

    // License
    public static final String LICENSE_INSTALL_PRECHECK = "SHM.LICENSE_INSTALL_PRECHECK";
    public static final String LICENSE_INSTALL_EXECUTE = "SHM.LICENSE_INSTALL_EXECUTE";
    public static final String LICENSE_INSTALL_PROCESS_NOTIFICATION = "SHM.LICENSE_INSTALL_PROCESS_NOTIFICATION";
    public static final String LICENSE_INSTALL_DELETE = "SHM.LICENSE_INSTALL_DELETE";

    //Clean CV
    public static final String CLEAN_CV_PRECHECK = "SHM.CLEAN_CV_PRECHECK ";
    public static final String CLEAN_CV_EXECUTE = "SHM.CLEAN_CV_EXECUTE ";
    public static final String CLEAN_CV_SERVICE = "SHM.CLEAN_CV_SERVICE";
    public static final String CLEAN_CV_TIME_OUT = "SHM.CLEAN_CV_TIME_OUT ";

    public static final String LICENSE_INSTALL_TIME_OUT = "SHM.LICENSE_INSTALL_TIME_OUT";
    // Delete backupCV
    public static final String TIME_OUT_FOR_DELETE_BACKUP = "SHM.DELETE_BACKUP_TIME_OUT";
    public static final String DELETE_BACKUP_SERVICE = "SHM.DELETE_BACKUP_SERVICE";

    public static final String CREATE_BACKUP_SERVICE = "SHM.CREATE_BACKUP_SERVICE";
    public static final String SET_STARTABLE_BACKUP_SERVICE = "SHM.SET_STARTABLE_BACKUP_SERVICE";
    public static final String SET_BACKUP_FIRST_IN_ROLLBACK_LIST_SERVICE = "SHM.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_SERVICE";
    public static final String UPLOAD_BACKUP_SERVICE = "UPLOAD_BACKUP_SERVICE";

    public static final String VERIFY_RESTORE_CV = "VERIFY_RESTORE_CV";

    public static final String INSTALL_SERVICE = "SHM.INSTALL_SERVICE";
    public static final String VERIFY_SERVICE = "SHM.VERIFY_SERVICE";
    public static final String UPGRADE_SERVICE = "SHM.UPGRADE_SERVICE";
    public static final String CONFIRM_SERVICE = "SHM.CONFIRM_SERVICE";
    public static final String DOWNLOAD_SERVICE = "SHM.DOWNLOAD_SERVICE";

    public static final String STN_INSTALL_SERVICE = "SHM.STN_INSTALL_SERVICE";
    public static final String STN_UPGRADE_SERVICE = "SHM.STN_UPGRADE_SERVICE";
    public static final String STN_APPROVE_SERVICE = "SHM.STN_APPROVE_SERVICE";
    public static final String STN_ADJUST_SERVICE = "SHM.STN_ADJUST_SERVICE";

    public static final String LICENSE_INSTALL_SERVICE = "SHM.LICENSE_INSTALL_SERVICE";
    public static final String NEJOBS_SUBMITTED = "SHM.NEJOBS_SUBMITTED";
    public static final String NEJOBS_SUBMISSION_FAILED = "SHM.NEJOBS_SUBMISSION_FAILED";
    public static final String NEJOBS_CREATION_FAILED = "SHM.NEJOBS_CREATION_FAILED";
    public static final String MAIN_JOB_COMPLETED = "SHM.MAIN_JOB_COMPLETED";

    public static final String MO_ACTION_ADDITIONAL_INFO = "ActivityJobId: %d, MainJobId: %d, JobType: %s";
    public static final String EVENT_ADDITIONAL_INFO = "ActivityJobId: %d, NodeName: %s, Message: %s";
    public static final String DELETE_BACKUP_SERVICE_REQUEST_ACTION = "SHM.DELETE_BACKUP_SERVICE_REQUEST";
    public static final String JOB_COMPLETION_RESULT = "Response message is %s. Time taken to complete delete job rest call with %d jobs is %d milliseconds";

    // Restore
    public static final String RESTORE_PRECHECK = "SHM.RESTORE_PRECHECK";
    public static final String RESTORE_EXECUTE = "SHM.RESTORE_EXECUTE";
    public static final String RESTORE_SERVICE = "RESTORE_SERVICE";
    public static final String RESTORE_ACTIVITY_TIME_OUT = "SHM.RESTORE_ACTIVITY_TIME_OUT ";
    public static final String RESTORE_PROCESS_NOTIFICATION = "SHM.RESTORE_PROCESS_NOTIFICATION";
    public static final String RESTORE_CANCEL = "SHM.RESTORE_CANCEL";

    public static final String DOWNLOAD_RESTORE_PRECHECK = "SHM.DOWNLOAD_RESTORE_PRECHECK ";
    public static final String DOWNLOAD_RESTORE_EXECUTE = "SHM.DOWNLOAD_RESTORE_EXECUTE ";
    public static final String DOWNLOAD_RESTORE_SERVICE = "SHM.DOWNLAOD_RESTORE_SERVICE";
    public static final String DOWNLOAD_RESTORE_TIME_OUT = "SHM.DOWNLOAD_RESTORE_TIME_OUT";

    public static final String SKIP_JOB = "SHM.SKIP_JOB";
    public static final String SYSTEM_CANCELLING = "SHM.SYSTEM_CANCELLING";
    public static final String SYSTEM_CANCELLED = "SHM.SYSTEM_CANCELLED";

    public static final String RESUBMIT_FAILED = "SHM.RESUBMIT_FAILED";

    public static final String DELETING_JOB = "SHM.DELETING_JOB";
    public static final String JOB_DELETED_SUCCESSFULLY = "SHM.DELETE_JOB_SUCCESS";

    //ECIM MO ACTION EVENTS
    public static final String ECIM_UPLOAD_BACKUP_PRECHECK = "SHM.ECIM_UPLOAD_BACKUP_PRECHECK ";
    public static final String ECIM_RESTORE_BACKUP_PRECHECK = "SHM.ECIM_RESTORE_BACKUP_PRECHECK ";
    public static final String ECIM_UPLOAD_BACKUP_EXECUTE = "SHM.ECIM_UPLOAD_BACKUP_EXECUTE ";
    public static final String ECIM_UPLOAD_BACKUP_PROCESS_NOTIFICATION = "SHM.ECIM_UPLOAD_BACKUP_PROCESS_NOTIFICATION";
    public static final String ECIM_RESTORE_BACKUP_PROCESS_NOTIFICATION = "SHM.ECIM_RESTORE_BACKUP_PROCESS_NOTIFICATION";
    public static final String ECIM_UPLOAD_BACKUP_TIME_OUT = "SHM.ECIM_UPLOAD_BACKUP_TIME_OUT ";
    public static final String ECIM_IMPORT_BACKUP_PROCESS_NOTIFICATION = "SHM.ECIM_IMPORT_BACKUP_PROCESS_NOTIFICATION";

    public static final String PREPARE_PRECHECK = "SHM.PREPARE_PRECHECK";
    public static final String PREPARE_EXECUTE = "SHM.PREPARE_EXECUTE";
    public static final String CREATE_EXECUTE = "SHM.CREATE_EXECUTE";

    public static final String PREPARE_SERVICE = "SHM.PREPARE_SERVICE";
    public static final String CREATE_SERVICE = "SHM.CREATE_SERVICE";
    //ECIM UPGRADE EVENTS
    public static final String ECIM_ACTIVATE_PRECHECK = "SHM.ECIM_ACTIVATE_PRECHECK ";
    public static final String ECIM_ACTIVATE_EXECUTE = "SHM.ECIM_ACTIVATE_EXECUTE ";
    public static final String ECIM_ACTIVATE_PROCESS_NOTIFICATION = "SHM.ECIM_ACTIVATE_PROCESS_NOTIFICATION";
    public static final String ECIM_ACTIVATE_TIME_OUT = "SHM.ECIM_ACTIVATE_TIME_OUT ";
    public static final String ACTIVATE_SERVICE = "SHM.ACTIVATE_SERVICE";
    public static final String ECIM_ACTIVATE_CANCEL = "SHM.ECIM_ACTIVATE_CANCEL ";

    public static final String CREATE_UPGRADE_PACKAGE_PROCESS_NOTIFICATION = "SHM.CREATE_UPGRADE_PACKAGE_PROCESS_NOTIFICATION";
    public static final String PREPARE_PROCESS_NOTIFICATION = "SHM.PREPARE_PROCESS_NOTIFICATION";
    public static final String CANCEL_NOTIFICATION = "SHM.CANCEL_PROCESS_NOTIFICATION";

    public static final String CREATE_UPGRADE_PACKAGE_TIME_OUT = "SHM.CREATE_UPGRADE_PACKAGE_TIME_OUT";
    public static final String PREPARE_TIME_OUT = "SHM.PREPARE_TIME_OUT";
    public static final String CANCEL_TIME_OUT = "SHM.CANCEL_TIME_OUT";
    public static final String NE_JOB_VALIDATION = "SHM.NE_JOB_VALIDATION";

    public static final String VERIFY_CPP_NODE_RESTART_PRECHECK = "VERIFY_CPP_NODE_RESTART_PRECHECK";
    public static final String CPP_NODE_RESTART_PRECHECK = "SHM.CPP_NODE_RESTART_PRECHECK ";
    public static final String CPP_NODE_RESTART_EXECUTE = "SHM.CPP_NODE_RESTART_EXECUTE";
    public static final String CPP_NODE_RESTART_TIMEOUT = "SHM.CPP_NODE_RESTART_TIME_OUT";
    public static final String CPP_NODE_RESTART_CANCEL = "SHM.CPP_NODE_RESTART_CANCEL";

    //MINI-LINK Upgrade
    public static final String MINI_LINK_UPGRADE_ACTIVATE = "SHM.MINI_LINK_UPGRADE_ACTIVATE";
    public static final String MINI_LINK_UPGRADE_CONFIRM = "SHM.MINI_LINK_UPGRADE_CONFIRM";

    public static final String WORKFLOW_SERVICE_CORRELATION = "SHM.WORKFLOW_SERVICE_CORRELATION";

    public static final String CPP_NODE_RESTART_ACTION_COMPLETED = "SHM.NODE_RESTART_ACTION_COMPLETED";
    public static final String CPP_CV_RESTORE_ACTION_COMPLETED = "SHM.CV_RESTORE_ACTION_COMPLETED";
    public static final String CPP_CV_RESTORE_ACTION_FAILED = "SHM.CV_RESTORE_ACTION_FAILED";
    public static final String CPP_NODE_RESTART_ACTION_FAILED = "SHM.NODE_RESTART_ACTION_FAILED";
    public static final String CPP_CV_RESTORE_ACTION_IN_PROGRESS = "SHM.CV_RESTORE_ACTION_IN_PROGRESS";

    public static final String UPGRADE_COMPLETED_IN_POLLING = "SHM.UPGRADE_COMPLETED_IN_POLLING";
    public static final String RESTORE_COMPLETED_IN_POLLING = "SHM.RESTORE_COMPLETED_IN_POLLING";
    public static final String HOUSEKEEPING_COMPLETED_SUCCESSFULLY = "SHM.HOUSEKEEPING_COMPLETED";

    public static final String ACTIVITY_COMPLETION = "SHM.%s.%s.%s_%s";

    public static final String CPP_DELETEUPGRADEPACKAGE_EXECUTE = "SHM.CPP_DELETEUPGRADEPACKAGE_EXECUTE";
    public static final String CPP_DELETEUPGRADEPACKAGE_TIMEOUT = "SHM.CPP_DELETEUPGRADEPACKAGE_TIMEOUT";

    public static final String ECIM_DELETE_UPGRADEPACKAGE_PRECHECK = "SHM.ECIM_DELETE_UPGRADEPACKAGE_PRECHECK";
    public static final String ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE = "SHM.ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE";
    public static final String ECIM_DELETE_UPGRADEPACKAGE_EXECUTE = "SHM.ECIM_DELETE_UPGRADEPACKAGE_EXECUTE";
    public static final String ECIM_DELETE_UPGRADEPACKAGE_PROCESS_NOTIFICATION = "SHM.ECIM_DELETE_UPGRADEPACKAGE_PROCESS_NOTIFICATION";

    public static final String ECIM_DELETE_UPGRADEPACKAGE_CANCEL = "SHM.ECIM_DELETE_UPGRADEPACKAGE_CANCEL";
    public static final String ECIM_DELETE_UPGRADEPACKAGE_CANCEL_TIMEOUT = "SHM.ECIM_DELETE_UPGRADEPACKAGE_CANCEL_TIMEOUT";
    public static final String POLLING_SUBSCRIPTION_SUCCESS = "SHM.POLLING_SUBSCRIPTION_SUCCESS";
    public static final String POLLING_UNSUBSCRIPTION_SUCCESS = "SHM.POLLING_UNSUBSCRIPTION_SUCCESS";
    public static final String POLLING_RE_SUBSCRIPTION_SUCCESS = "SHM.POLLING_RE_SUBSCRIPTION_SUCCESS";
    public static final String POLLING_CYCLE_STATUS_IN_PROGRESS = "SHM.POLLING_STILL_IN_PROGRESS";
    public static final String POLLING_WAIT_PERIOD_TIMEDOUT = "SHM.POLLING_WAIT_PERIOD_TIMEDOUT";

    //DPS Events
    public static final String DPS_CONNECTIVITY_LOST = "SHM.DPS_CONNECTIVITY_LOST";
    public static final String DPS_RE_CONNECTION_FAILED = "SHM.DPS_RE_CONNECTION_FAILED";
    public static final String DPS_RE_CONNECTION_SUCCESFUL = "SHM.DPS_RE_CONNECTION_SUCCESFUL";

    public static final String AXE_ACTIVITY = "SHM.AXE_ACTIVITY";
    public static final String LOAD_CONTROL_STATE = "SHM.LOAD_CONTROL_STATE";

    public static final String SMRS_BACKUP_FILE_SIZE = "SHM.SMRS_BACKUP_FILE_SIZE";
    public static final String POST_CONSTRUCT = "SHM.POST_CONSTRUCT";

    // License Refresh
    public static final String LICENSEREFRESH_REFRESH_PRECHECK = "SHM.LICENSEREFRESH_REFRESH_PRECHECK";
    public static final String LICENSEREFRESH_REFRESH_EXECUTE = "SHM.LICENSEREFRESH_REFRESH_EXECUTE";
    public static final String LICENSEREFRESH_REFRESH_NOTIFICATION = "SHM.LICENSEREFRESH_REFRESH_NOTIFICATION";
    public static final String LICENSEREFRESH_REQUEST_PRECHECK = "SHM.LICENSEREFRESH_REQUEST_PRECHECK";
    public static final String LICENSEREFRESH_REQUEST_EXECUTE = "SHM.LICENSEREFRESH_REQUEST_EXECUTE";
    public static final String LICENSEREFRESH_REQUEST_NOTIFICATION = "SHM.LICENSEREFRESH_REQUEST_NOTIFICATION";

    private SHMEvents() {
        // Default constructor.
    }

    public static final class ShmPostConstructConstants {
        public static final String CLASS_NAME = "ClassName";
        public static final String TIME_TAKEN = "TimeTakenInMillis";
        public static final String MESSAGE = "Message";

        private ShmPostConstructConstants() {

        }
    }

}
