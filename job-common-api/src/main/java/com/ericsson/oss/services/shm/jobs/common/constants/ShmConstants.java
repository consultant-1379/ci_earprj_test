/*------------------------------------------------------------------------------
 *******************************************************************************
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.common.constants;

public class ShmConstants {
    public static final String NAMESPACE = "shm";
    public static final String JOB = "Job";
    public static final String JOBID = "jobId";
    public static final String JOBTEMPLATE = "JobTemplate";
    public static final String JOBTEMPLATEID = "templateJobId";
    public static final String JOBCONFIGURATION = "JobConfiguration";
    public static final String JOBCONFIGURATIONDETAILS = "jobConfigurationDetails";
    public static final String VERSION = "1.0.0";
    public static final String JOBCONFIGID = "jobConfigId";
    public static final String NAME = "name";
    public static final String PARENT_NAME = "parentName";
    public static final String AXE_NES = "AXE_NES";
    public static final String AXE_UNSUPPORTED_NES = "AXE_UnSupported_NES";
    public static final String UNSUPPORTED_NES = "UnSupportedNes";
    public static final String SUPPORTED_NES = "SupportedNes";
    public static final String IS_COMPONENT_JOB = "isComponentJob";
    public static final String AXE_NODENAME_DELIMITER = "#ParentName_";
    public static final String TYPE = "type";
    public static final String STATE = "state";
    public static final String PROGRESSPERCENTAGE = "progressPercentage";
    public static final String RESULT = "result";
    public static final String STARTTIME = "startTime";
    public static final String ENDTIME = "endTime";
    public static final String COMMENT = "comment";
    public static final String JOBPROPERTIES = "jobProperties";
    public static final String PLATFORMJOBPROPERTIES = "platformTypeJobProperties";
    public static final String NETYPEJOBPROPERTIES = "neTypeJobProperties";
    public static final String JOBNAME = "jobName";
    public static final String FDN = "fdn";
    public static final String MO_ATTRIBUTES = "moAttributes";
    public static final String JOB_CATEGORY = "jobCategory";
    public static final String SHM_JOB_EXEC_USER = "shmJobExecUser";
    public static final String USER_ID_KEY = "X-Tor-UserID";
    public static final String PERIODIC = "periodic";
    public static final String TOTALNODES = "totalNodes";
    public static final String JOB_STATUS = "jobStatus";
    public static final String ACTIVITY_JOB_POS = "activityJobPos";
    public static final String LICENSE_REFRESH = "LICENSE_REFRESH";
    public static final String TECHNOLOGY_DOMAIN_5G = "5GS";
    public static final String TECHNOLOGY_DOMAIN = "technologyDomain";

    public static class JobTemplateConstants {

        public static final String ISDELETABLE = "isDeletable";
        public static final String ISCANCELLED = "isCancelled";
    }

    public static final String PARENTJOBID = "parentJobId";
    public static final String ACTIVITYNAME = "activityName";
    public static final String LOG = "log";
    public static final String LAST_LOG_MESSAGE = "lastLogMessage";
    public static final String LEVEL = "level";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String JOB_TYPE = "jobType";
    public static final String CREATED_BY = "createdBy";
    public static final String OWNER = "owner";
    public static final String CREATION_TIME = "creationTime";
    public static final String DESCRIPTION = "description";
    public static final String SELECTED_NES = "selectedNEs";
    public static final String SELECTED_COMPONENTS = "selectedComponenets";
    public static final String MAIN_SCHEDULE = "mainSchedule";
    public static final String ACTIVITIES = "activities";
    public static final String EXECUTIONINDEX = "executionIndex";
    public static final String SUBMITTED_NES = "submittedNEs";
    public static final String NE_COMPLETED = "neCompleted";
    public static final String COLLECTION_NAMES = "collectionNames";
    public static final String NENAMES = "neNames";
    public static final String NE_WITH_COMPONENT_INFO = "neWithComponentInfo";
    public static final String PACKAGE_NAMES = "packageNames";
    public static final String COLLECTIONS = "collections";
    public static final String JOB_TEMPLATE_ID = "templateJobId";
    public static final String JOBTEMPLATE_ID = "jobTemplateId";
    public static final String COMPONENT_NAME = "componentName";
    public static final String ACTIVITY_NAMES = "activityNames";
    public static final String COMPONENT_ACTIVITIES = "componentActivities";
    public static final String NETYPE_COMPONENT_ACTIVITYDETAILS = "neTypeComponentActivityDetails";

    public static final String MAINSCHEDULE = "mainSchedule";
    public static final String SCHEDULINGPROPERTIES = "scheduleAttributes";
    public static final String NEJOB_PROPERTIES = "neJobProperties";
    public static final String NETYPE_ACTIVITYJOBPROPERTIES = "neTypeActivityJobProperties";
    public static final String ACTIVITYJOB_PROPERTIES = "activityJobProperties";
    public static final String ACTIVITY_PROPERTIES = "activityProperties";
    public static final String STD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'ZUTC'XXX";
    public static final String DATE_WITH_TIME_FORMAT_FOR_FILTER = "yyyy-MM-dd HH:mm:ss";
    public static final String FDN_DELIMITER = ",";
    public static final String EQUALS = "=";
    public static final String SCHEDULE_TIME = "scheduleTime";
    public static final String REQUEST_TYPE = "requestType";

    // Added for retrieval of job logs
    public static final String ACTIVITYJOB_TYPE = "ActivityJob";
    public static final String NEJOB_TYPE = "NEJob";
    public static final String MAINJOBID = "mainJobId";
    public static final String ACTIVITY_JOB_ID = "activityJobId";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ssZ";
    public static final String GMT = "GMT";
    public static final String START_DATE = "START_DATE";
    // Added for deleting Jobs
    public static final String RUNNING = "RUNNING";
    public static final String CANCELLING = "CANCELLING";
    public static final String FAILEDJOB = "failedjob";
    public static final String EXECUTION_MODE = "execMode";
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String CREATION_STARTED = "CREATION_STARTED";
    public static final String CREATION_FAILED = "CREATION_FAILED";
    public static final String CREATION_FAILURE_CAUSE = "CREATION_FAILURE_CAUSE";
    public static final String EXISTED = "EXISTED";
    public static final String CREATION_COMPLETED = "CREATION_COMPLETED";
    public static final String WAIT_FOR_USER_INPUT = "WAIT_FOR_USER_INPUT";
    public static final String SYSTEM_CANCELLING = "SYSTEM_CANCELLING";
    public static final String SYSTEM_CANCELLED = "SYSTEM_CANCELLED";
    public static final String DELETING = "DELETING";
    public static final String NE_NAME = "neName";
    public static final String NE_PROG_PERCENTAGE = "progressPercentage";
    public static final String NE_STATUS = "state";
    public static final String NE_RESULT = "result";
    public static final String NE_START_DATE = "startTime";
    public static final String NE_END_DATE = "endTime";
    public static final String NE_COMMENT = "comment";
    public static final String ACTIVITY_NAME = "name";
    public static final String ACTIVITY_SCHEDULE = "schedule";
    public static final String ACTIVITY_START_DATE = "startTime";
    public static final String ACTIVITY_END_DATE = "endTime";
    public static final String ACTIVITY_RESULT = "result";
    public static final String ACTIVITY_NE_JOB_ID = "neJobId";
    public static final String ACTIVITY_NE_STATUS = "state";
    public static final String JOB_ACTIVITY_NAME = "name";
    public static final String JOB_ACTIVITY_SCHEDULE = "execMode";
    public static final String FALSE = "FALSE";
    public static final String NODENAME = "nodeName";

    public static final String MAIN_JOB_ID = "mainJobId";
    public static final String BUSINESS_KEY = "businessKey";
    public static final String WFS_ID = "wfsId";
    public static final String NE_JOB_ID = "neJobId";
    public static final String NE_JOB = "NEJob";
    public static final String ORDER = "order";
    public static final String ACTIVITY_JOB = "ActivityJob";
    public static final String JOB_ACTIVITIES = "activities";
    public static final String ACTIVITY_ORDER = "order";

    public static final String UP_MO_TYPE = "UpgradePackage";
    public static final String UP_PARENT_MO_TYPE = "SwManagement";
    public static final String UPGRADE_PKG_VERSION = "3.12.0";

    public static final String USER_TASK = "UserTask";
    public static final String USER_INPUT = "UserInput";
    public static final String USER_MESSAGE = "userMessage";

    // Added for CV operations
    public static final String CV_MO_TYPE = "ConfigurationVersion";
    public static final String CV_PARENT_MO_TYPE = "SwManagement";

    // Upgrade activity names
    public static final String INSTALL_ACTIVITY = "install";
    public static final String VERIFY_ACTIVITY = "verify";
    public static final String UPGRADE_ACTIVITY = "upgrade";
    public static final String CONFIRM_ACTIVITY = "confirm";

    // Backup activity names
    public static final String CREATE_CV__ACTIVITY = "createcv";
    public static final String UPLOAD_CV__ACTIVITY = "exportcv";
    public static final String UPLOAD_BACKUP_ACTION = "export";
    public static final String SET_STARTABLE__ACTIVITY = "setcvasstartable";
    public static final String SET_FIRST_IN_THE_ROLLBACKLIST_ACTIVITY = "setcvfirstinrollbacklist";
    public final static String CREATE_BACKUP = "createbackup";
    public final static String UPLOAD_BACKUP = "uploadbackup";
    public static final String RESTORE_BACKUP = "restorebackup";

    // RESTORE activity names
    public static final String RESTORE_DOWNLOAD_ACTIVITY = "download";
    public static final String RESOTRE_VERIFY_ACTIVITY = "verify";
    public static final String RESOTRE_INSTALL_ACTIVITY = "install";
    public static final String RESTORE_ACTIVITY = "restore";
    public static final String RESTORE_CONFIRM_ACTIVITY = "confirm";

    public static final String NODERESTART_ACTIVITY = "manualrestart";
    public static final String DEFAULTVALUE_RESTART_INFO = "manual restart";

    public static final String DELETEUPGRADEPKG_ACTIVITY = "deleteupgradepackage";

    public static final String ERROR_MSG = "Invalid Scheduling Attributes.";
    public static final String REPEAT_TYPE = "REPEAT_TYPE";
    public static final String REPEAT_COUNT = "REPEAT_COUNT";
    public static final String MONTHLY = "Monthly";
    public static final String PO_ID = "PoId";

    public static final String PLATFORM = "platform";
    public static final String PLATFORMTYPE = "platformType";
    public static final String PROPERTIES = "properties";
    public static final String NE_PROPERTIES = "neProperties";
    public static final String TRUE = "TRUE";
    public static final String IS_COMPONENT_JOB_TRUE = "true";

    public static final String ENTRY_TIME = "entryTime";

    public static final String USERNAME = "userName";
    public static final String DATE = "date";

    public static final String DELIMITER_UNDERSCORE = "_";
    public static final String DELIMITER_SLASH = "/";
    public static final String DELIMITER_COLON = ":";
    public static final String DELIMITER_PIPE = "||";
    public static final String BACKUP_LOCATION_DELIMITER = "|";
    public static final String BACKUP_LOCATION_SPLIT_DELIMITER = "\\" + BACKUP_LOCATION_DELIMITER;

    public static final String IS_EXECUTED = "isExecuted"; // job property added for cluster-reboot handling
    public static final String SUCCESS = "success";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String PATH_ON_FTP_SERVER = "pathOnFtpServer";
    public static final String RELATIVE_PATH_FROM_NETWORK_TYPE = "relativePathFromNetworkType";

    public static final String NETYPE = "neType";
    public static final String FTP_SERVER_IP_ADDRESS = "ftpServerIpAddress";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String WFS_ACTIVATE_EXECUTE = "execute";
    public static final String WFS_ACTIVATE_PROCESS_NOTIFICATION = "processNotification";
    public static final String NOTIFIABLE_ATTRIBUTE_VALUE = "notifiableAttributeValue";
    public static final String CV_ADDITIONAL_ACTION_RESULT_DATA = "additionalActionResultData";
    public static final String CV_INFORMATION = "information";

    // added for COLLECTIONS and SAVED SEARCHES
    public static final String NO_OF_NETWORK_ELEMENTS = "numberOfNetworkElements";
    public static final String SAVED_SEARCH_IDS = "savedSearchIds";
    public static final String PO_LIST = "poList";
    public static final String NE_NOT_AVAILABLE = "N/A";
    public static final String TOPOLOGY_EVALUATION_FAILED = "topologyEvaluationFailed";
    public static final String ACTIVITY_CONFIGURATION = "activityConfiguration";
    public static final String ZIPFILE = ".zip";
    public static final String TIMEOUT = "timeout while processing notifications!";

    public static final String CANCELLEDBY = "cancelledBy";
    public static final String NE_JOB_STATE = "neState";

    public static final String FAILED_NES = "FAILED_NES";
    public static final String SKIPPED_NES = "SKIPPED_NES";
    public static final String TOTAL_NES = "TOTAL_NES";
    public static final String EVENT_TYPE = "EventType";
    public static final String PROBABLE_CAUSE = "ProbableCause";
    public static final String SPECIFIC_PROBLEM = "SpecificProblem";
    public static final String PERCEIVED_SEVERITY = "PerceivedSeverity";
    public static final String RECORD_TYPE = "RecordType";
    public static final String MANAGED_OBJECT_INSTANCE = "ManagedObjectInstance";
    public static final String ADDITIONAL_TEXT = "additionalText";
    public static final String INTERNAL_ALARM_ERROR_ID = "SHM.JOB_INTERNAL_ALARM_GENERATION_FAILURE";
    public static final String SOURCE = "SHM";
    public static final String EVENT_TIME = "eventTime";
    public static final String INTERNAL_ALARM_ADDITIONAL_TEXT = "SHM \"%s\" Job \"%s\" created by \"%s\" has failed. \"%s\" was started at \"%s\"."
            + " Total number of nodes in job \"%s\", number of nodes on which job failed \"%s\", number of nodes on which job is skipped \"%s\".";
    public static final String CONTINUE_JOB_RESPONSE_MESSAGE = "Job(s) successfully submitted for continue";

    public static final String DURATION_FOR_NEJOBS_CREATION = "DurationForNeJobsCreation";

    // added for job properties
    public static final String KEY_ACTIVE_RELEASE = "ActiveRelease";
    public static final String KEY_LMUPGRADE_ENTRY_INDEX = "xfSwLmUpgradeEntryIndex";
    // added for retries
    public static final int RETIESCOUNTFORFAILEDTODELETEJOBS = 3;

    public static final String MATCHED_NE_JOB_ATTRIBUTES = "matchedNeJobAttributes";
    public static final String ACTIVITY_RESPONSE_LIST = "activityResponseList";
    public static final String REVISION = "revision";
    public static final String STEP_DURATIONS = "stepDurations";
    public static final String IDENTITY = "identity";

    public static final String ERROR_CODE = "errorCode";
    public static final String BACKUP_JOB = "BackupJob";
    public static final String CV_DEFAULT_TYPE = "OTHER";

    public static final String JOBINPUT = "jobInput";
    public static final String ASENDING = "asc";
    public static final String DESENDING = "desc";
    public static final String STARTDATE = "startDate";
    public static final String ENDDATE = "endDate";
    public static final String PROGRESS = "progress";
    public static final String TOTAL_NO_OF_NES = "totalNoOfNEs";
    public static final String POLLING_ACTIVITY = "PollingActivity";
    public static final String ACTION_RESPONSE = "actionResponse";

    // To define Flow Types
    public static final String ACTION = "action";
    public static final String READ = "read";

    public static final String RETRY_COUNT = "retryCount";
    public static final int ZERO_INT = 0;

    //node health check
    public static final String NEJOB_HEALTH_STATUS = "healthStatus";
    public static final String NODE_HEALTH_CHECK_TEMPLATE = "NODE_HEALTH_CHECK_TEMPLATE";
    public static final String COLLECTIONORSSNAME = "collectionOrSSIDName";
    public static final String MONAME = "moName";
    public static final String MOTYPE = "moType";
    public static final String PLATEFORMTYPE = "platFormType";
    public static final String PLATEFORM_TYPE = "platformType";
    public static final String NODETYPE = "nodeType";
    public static final String COLLECTIONORSSINFO = "collectionIdsOrsaveSearchedIdsInfo";

    public static final String SHM_STAGED_ACTIVITY = "ShmStagedActivity";
    public static final String STAGED_ACTIVITY_STATUS = "stagedActivityStatus";
    public static final String READY_STATE = "READY";
    public static final String LC_COUNTER_KEY = "counterKey";
    public static final String STAGED_ACTIVITY_WAIT_TIME = "waitTime";
    public static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";

    public static final String SUCCESS_NODES = "successNodes";
    public static final String FAILURED_NODES = "failedNodes";

    public static final String INTERNAL_ALARM_ADDITIONAL_TEXT_FOR_FAILED_JOB = "SHM \"%s\" Job \"%s\" created by \"%s\" has failed.";

    public static final String JOB_RETRIEVAL_EVENT_TYPE = "Time Consumption for retrieval of all Job Templates and its corresponding Jobs";
    public static final String MAIN_JOB_RETRIEVAL = "Main Job Retrieval Time Consumption : ";
    public static final String TIME_CONSUMPTION_FOR_MAIN_JOBS_RETRIEVAL = "Total Batches = %d; Time Taken = %s.";
    public static final String SOURCE_FOR_JOBS_RETRIEVAL = "DPS Calls for retrieval of Jobs and Job Templates";
    public static final String RESOURCE_FOR_JOBS_RETRIEVAL = "Main Jobs Page";
    public static final String TIME_CONSUMPTION_FOR_JOBS_RETRIEVAL = "Time Taken to retrieve all Job Template : %s. ";
    public static final String CREATION_FAILED_MSG = "Unable to create job due to exeception : %s ";

    public static final String DELIMITER_DOUBLE_UNDERSCORE = "__";
    public static final String CLUSTER_SUFFIX = "-AXE_CLUSTER";
    public static final String SOFTWARE = "SOFTWARE";
    public static final String BACKUP = "BACKUP";
    public static final String FILE_SIZE = "fileSize";
    public static final String COMPONENT = "component";
    public static final String EMPTY = "";

    //adding for ne jobs count
    public static final String SUCCESS_COUNT = "SUCCESS_COUNT";
    public static final String FAILED_COUNT = "FAILED_COUNT";
    public static final String CANCELLED_COUNT = "CANCELLED_COUNT";
    public static final String SKIPPED_COUNT = "SKIPPED_COUNT";
    public static final String TOTAL_COUNT = "TOTAL_COUNT";
    public static final String PO_ATTRIBUTES = "poAttributes";

    public static final String MAJOR_ALARM_DATA = "majorAlarmPoIds";
    public static final String CRITICAL_ALARM_DATA = "criticalAlarmPoIds";
    public static final String PROGRESS_REPORT = "progressReport";
    public static final String HC_JOB_FDN = "HcJobFdn";

    public static final String NE_PRODUCT_REVISION = "productRevision";
    public static final String APG_COMPONENT_IDENTIFIER_FROM_UI = "__APG";
    public static final String APG_COMPONENT_IDENTIFIER_IN_DB = "_APG";
    public static final double BKPENCRYPTION_SUPPORT_STARTING_VERSION = 3.7;

    public static final String PRODUCT_NUMBER_PLACEHOLDER = "$productnumber";
    public static final String PRODUCT_REVISION_PLACEHOLDER = "$productrevision";
    public static final String TIMESTAMP_PLACEHOLDER = "$timestamp";
    public static final String NODE_NAME_PLACEHOLDER = "$nodename";

    public static final String SECURE_BACKUP = "Secure Backup";
    public static final String DISPLAY_USER_LABEL = "User Label";
}