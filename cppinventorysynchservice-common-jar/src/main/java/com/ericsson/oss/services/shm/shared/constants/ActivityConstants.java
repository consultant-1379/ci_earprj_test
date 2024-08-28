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
package com.ericsson.oss.services.shm.shared.constants;

/**
 * This class is used to hold all the constant variables for SHM job Activities.
 */
public class ActivityConstants {
    public static final String INSTALL_STN = "install";
    public static final String SW_ADJUST = "sw_adjust";
    public static final String PRECHECK_STATUS = "preCheckStatus";
    public static final String ACTIVITY_RESULT = "result";
    public static final String ACTIVITY_JOB_ID = "activityJobId";
    public static final String ACTIVITY_STATUS = "activityStatus";
    public static final String JOB_RESULT = "jobResult";
    public static final String ACTION_ID = "actionId";
    public static final String CANCEL_ACTION_ID = "cancelActionId";
    public static final String ACTIVITY_NAME = "name";
    public static final String TOTAL_ACTIVITIES = "totalActivities";
    public static final String TOTAL_ACTIVITIES_PROGRESS_PERCENTAGE = "totalActivitiesPercentage";
    public static final String COMPLETED_ACTIVITIES = "completedActivities";
    public static final String APPROVE_SW = "approvesw";
    public static final String PACKAGE_TYPE = "packagetype";
    public static final String RELATIVE_PATH = "relativepath";
    public static final String CHECK_TRUE = "true";
    public static final String CHECK_FALSE = "false";
    public static final boolean TRUE = true;
    public static final boolean FALSE = false;
    public static final String JOB_CONFIGURATION_DETAILS = "jobConfigurationDetails";
    public static final String JOB_PROPERTIES = "jobProperties";
    public static final String NE_JOB_PROPERTIES = "neJobProperties";
    public static final String NETYPEJOBPROPERTIES = "neTypeJobProperties";
    public static final String JOB_PROP_KEY = "key";
    public static final String JOB_PROP_VALUE = "value";
    public static final String JOB_LOG = "log";
    public static final String JOB_LOG_ENTRY_TIME = "entryTime";
    public static final String JOB_LOG_MESSAGE = "message";
    public static final String JOB_LOG_TYPE = "type";
    public static final String JOB_LOG_LEVEL = "logLevel";
    public static final String JOB_ENDTIME = "endTime";
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String UNDERSCORE = "_";
    public static final String SLASH = "/";
    public static final String PERCENTAGE = "%";
    public static final String EQUAL = "=";
    public static final String EMPTY = "";
    public static final String DOT = ".";
    public static final int SPLIT_LIMIT = 2;
    public static final String ROLL_BACK = "ROLL_BACK";
    public static final String CV_NAME = "CV_NAME";
    public static final String CV_LIST = "CVNameList";
    public static final String CV_LIST_SMRS = "CVNameListSmrs";
    public static final String CV_NAME_LOCATION = "CV_NAME_LOCATION";
    public static final String ROLL_BACK_FLAG = "rollBackFlag";
    public static final String INSTALL = "Install";
    public static final String DOWNLOAD = "download";
    public static final String UPLOAD = "Upload";
    public final static String VERIFY = "Verify";
    public final static String UPGRADE = "Upgrade";
    public static final String CANCEL_UPGRADE = "CANCEL_UPGRADE";
    public final static String CONFIRM = "Confirm";
    public final static String CREATE_CV = "Create Configuration Version";
    public final static String VERIFY_RESTORE_CV = "Verify Restore CV";
    public final static String SET_AS_STARTABLE_CV = "Set as Startable CV";
    public final static String SET_FIRST_IN_ROLLBACK_LIST = "Set first in rollback list";
    public final static String UPLOAD_CV = "Upload Configuration Version";
    public final static String DELETE_CV = "Delete Configuration Version";
    public final static String CONFIRM_RESTORE_CV = "Confirm Restore Configuration Version";
    public final static String RESTORE_INSTALL_CV = "Restore Install Configuration Version";
    public final static String DOWNLOAD_CV = "Download Configuration Version";
    public final static String CLEAN_CV = "Clean CV";
    public final static String INSTALL_LICENSE = "Install License Key File";
    public final static String RESTORE = "Restore Configuration Version";
    public final static String FORCED_RESTORE = "FORCED_RESTORE";
    public final static String AUTO_CONFIGURATION = "AUTO_CONFIGURATION";
    public final static String RESTORE_ACTIVITY = "RESTORE_ACTIVITY";
    public final static String CANCEL_RESTORE = "Cancel Restore";
    public static final String HAVE_SEEN_RESTORING = "haveBeenRestoring";
    public static final String PROGRESS_PERCENTAGE = "progressPercentage";
    public final static String PROCESS_NOTIFICATIONS = "processNotification";
    public final static String EXECUTE = "execute";
    public final static String ANY_INTERMEDIATE_FAILURE = "ANY_INTERMEDIATE_FAILURE";
    public final static String PREPARE = "Prepare";
    public final static String CREATE = "Create";
    public final static String ACTIVATE = "Activate";
    public static final String BACKUP = "backup";
    public static final String CONFIRM_BACKUP = "confirmbackup";
    public static final String DELETE_BACKUP = "deletebackup";
    public static final String CANCEL_DELETE_BACKUP = "Cancel Delete Backup";
    public static final String CREATE_BACKUP = "createbackup";
    public static final String SMRS_ACTIVITY_RESULT = "smrsResult";
    public static final String IS_ACTIVITY_TRIGGERED = "isActivityTriggered";
    public static final String IS_CANCEL_TRIGGERED = "isCancelTriggered";
    public static final String ACTION_TRIGGERED = "actionTriggered";
    public static final String COMPLETED_THROUGH_NOTIFICATIONS = "COMPLETED_THROUGH_NOTIFICATIONS";
    public static final String COMPLETED_THROUGH_POLLING = "COMPLETED_THROUGH_POLLING";
    public static final String COMPLETED_THROUGH_TIMEOUT = "COMPLETED_THROUGH_TIMEOUT";
    public static final String COMPLETION_FLOW = "; Flow : %s";
    public static final String ACTION_TRIGGERED_TIME = "actionTriggeredTime";
    public static final String DELETE_UP_DISPLAY_NAME = "Delete Upgrade Package";
    public static final String IS_PRECHECK_DONE = "isPrecheckDone";
    public static final String ATTEMPTS_FOR_REPEAT_PRECHECK = "attemptsForRepeatPrecheck";
    public static final String ATTEMPTS = "attempts";
    public static final String CREATE_CV_ACTIVITY_NAME = "createcv";
    public static final String EXPORT_CV_ACTIVITY_NAME = "exportcv";
    public static final String SET_FIRST_IN_ROLLBACK_ACTIVITY_NAME = "setcvfirstinrollbacklist";
    public static final String SET_AS_STARTABLE_ACTIVITY_NAME = "setcvasstartable";
    public static final String CLEANCV_ACTIVITY_NAME = "cleancv";
    public static final String DELETE_CV_ACTIVITY_NAME = "deletecv";
    public static final String INSTALL_LICENSE_ACTIVITY = "install";
    public static final String NODE_RESTART_ACTIVITY_NAME = "manualrestart";
    public static final String OPS_SESSION_ID = "sessionId";
    public static final String OPS_CLUSTER_ID = "clusterId";
    public static final String OPS_SESSIONID_CLUSTERID_NOT_FOUND = "Ops cluster Id or session Id not found";
    public static final String ACTIVITY_EXECUTION_STARTED = "STARTED";
    public static final String ACTIVITY_EXECUTION_COMPLETED = "COMPLETED";
    public static final String FALLBACK_TIMEOUT = "fallbackTimeout";
    public static final String LICENSE_REFRESH_REFRESH = "refresh";
    public static final String LICENSE_REFRESH_REQUEST = "request";
    public static final String LICENSE_REFRESH_INSTALL = "install";
}
