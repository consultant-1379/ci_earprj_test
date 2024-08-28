package com.ericsson.oss.services.shm.jobexecutor;

public class JobExecutorConstants {

    public static final String WORKFLOW_SUBMISSION_FAILED = "Workflow submission failed for \"%s\"";
    public static final String JOB_CANCEL_SKIPPED = "Cancel of \"%s\" job is skipped ";
    public static final String SKIP_NE_JOB_CANCEL = "Skipping cancellation of NE Job, as job is already completed.";

    public static final String WORKFLOW_NOT_IN_WAIT_FOR_USER_INPUT_STATE = "workflow not in waiting state";
    public static final String CORRELATION_FAILED = "Unable to complete user task because \"%s\"";
    public static final String FETCH_WAITING_MAINJOB_WORKFLOW_ID_FAILED = "Failed to continue Main Job workflow for the job \"%s\" triggered by \"%s\". Failed reason : %s .";
    public static final String FETCH_WAITING_ACTIVITYJOB_WORKFLOW_ID_FAILED = "Failed to continue Activity Job workflow for the activity \"%s\" triggered by \"%s\". Failed reason :  %s .";
    public static final String COLLECTION_EXCEPTION = "Unable to fetch Network Elements for collection \"%s\"";
    public static final String SAVEDSEARCH_EXCEPTION = "Unable to fetch Network Elements for SavedSearch \"%s\"";
    public static final String NE_PLATFORMQUERY_FAILED = "Failed to query the NetworkElements.";
    public static final String UNSUPPORTED_NODES = "Nodes [\"%s\"] will not be processed. ";
    public static final String CANCEL_INVOKED = "Cancel request received for \"%s\" by \"%s\".";
    public static final String TBAC_FAILURE_REASON = "Failed to authorize nodes. Reason:";
    public static final String TBAC_ACCESS_DENIED_AT_NE_LEVEL = "Access denied. User '%s' does not exist or unauthorized to perform operations on this node.";
    public static final String TBAC_ACCESS_DENIED_AT_JOB_LEVEL = "Access denied. User '%s' does not exist or unauthorized to perform operations on all/some of the nodes.";
    public static final String CONTINUE_INVOKED_AT_MAINJOB = "User '%s' has coninued the job.";
    public static final String CONTINUE_INVOKED_AT_NEJOB = "User '%s' has continued the activity '%s'.";
}
