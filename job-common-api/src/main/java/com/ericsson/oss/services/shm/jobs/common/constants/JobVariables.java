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

/**
 * common constants used across all the platform type process variable builders
 * 
 * @author xrajeke
 * 
 */
public class JobVariables {

    public static final String ACTIVITY_NAME = "callActivity";
    public static final String JOB_TYPE = "jobType";

    public static final String VAR_NAME_DELIMITER = ".";
    public static final String TIMEOUT = "timeout";
    public static final String SYNCRONIZATION_TIMEOUT = "syncTimeout";
    public static final String PRECHECK_TIMEOUT = "precheckTimeout";
    public static final String REPEAT_PRECHECK_WAIT_TIME = "repeatPrecheckWaitTime";
    public static final String TIMEOUT_FOR_HANDLE_TIMEOUT = "timeoutForHandleTimeout";
    public static final String UPGRADE = "UPGRADE";
    public static final String BACKUP = "BACKUP";
    public static final String LICENSE = "LICENSE";
    public static final String DELETE_BACKUP = "DELETEBACKUP";
    public static final String RESTORE = "RESTORE";

    public static final String JOB_ID = "jobId";
    public static final String NE_JOB_ID = "neJobId";
    public static final String ACTIVITY_JOBID = "activityJobId";

    public static final String ACTIVITY_STARTUP = "startup";
    public static final String ACTIVITY_REQUIRED = "required";
    public static final String ACTIVITY_STARTUP_MANUAL = "manual";
    public static final String ACTIVITY_STARTUP_IMMEDIATE = "immediate";
    public static final String ACTIVITY_STARTUP_SCHEDULED = "scheduled";
    public static final String ACTIVITY_SKIP_EXECUTION = "skipStepExecution";
    public static final String ACTIVITY_PRECHECK_COMPLETED = "precheckCompleted";
    public static final String TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED = "timeoutForHandleTimeoutCompleted";
    public static final String ACTIVITY_RESTART = "restart";
    public static final String ACTIVITY_REPEAT_EXECUTE = "repeatExecute";
    public static final String ACTIVITY_EXECUTE_MANUALLY = "executeManually";
    public static final String ACTIVITY_REPEAT_PRECHECK = "repeatPrecheck";

    // Added for cancel job
    public static final String PROPAGATE_CANCEL_TO_MAIN_JOB = "propagateCancelToMainJob";
    //For LoadController Service
    public static final String ALLOW_CURRENT_ACTIVITY = "allowCurrentActivity";
    public static final String PLATFORM_TYPE = "platformType";
    public static final String ACTIVITY_REQUEST = "activityRequest";

    public static final String NAME = "name";
    public static final String POID = "poId";

    //activity result constant
    public static final String ACTIVITY_RESULT = "activityResult";

    // schedule property attributes
    public static final String SCHEDULE_TIME = "scheduleTime";

    public static final String NE_TYPE = "neType";
    public static final String IS_ACTIVITY_STARTED = "isActivityStarted";

    // polling attributes
    public static final String BEST_TIMEOUT = "bestTimeout";
    public static final String IS_POLLING_ENABLED = "isPollingEnabled";
    public static final String WAIT_FOR_EXECUTE = "waitForExecute";

    //neTypes
    public static final String NETYPE_ROUTER = "router";

    public static final String IS_NEXT_ACTIVITY_SYNCHRONOUS = "isNextActivitySynchronous";
    public static final String ACTIVITIES_COUNT = "activitiesCount";
    public static final String AXE_HANDLE_TIMEOUT_RETRY_COUNT = "axeHandleTimeoutRetryCount";
    public static final String REPEAT_HANDLE_TIMEOUT = "repeatHandleTimeout";
    public static final String RESET_HANDLE_TIMEOUT = "resetHandleTimeout";
    public static final String ACTIVITY_ORDER_LIST = "activityOrderList";
    public static final String CANCEL_IN_PROGRESS = "cancelInProgress";
    public static final String CANCEL_ACTION_DONE = "cancelActionDone";
    public static final String ACTIVITY_EXECUTION_COMPLETED = "COMPLETED";
    public static final String NODE_SYNC_CHECK_WAIT_INTERVAL = "nodeSyncCheckWaitIntervalInMin";
    public static final String NODE_SYNC_CHECK_TIMEOUT = "nodeSyncCheckTimeout";

}