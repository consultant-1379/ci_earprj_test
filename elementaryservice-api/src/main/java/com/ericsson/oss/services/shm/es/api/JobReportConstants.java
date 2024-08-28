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
package com.ericsson.oss.services.shm.es.api;

/**
 * This class is used for Job reporting feature related constants.
 * 
 * @author tcschat, xarirud
 * 
 */
public class JobReportConstants {

    public static final String MIN = "MIN";
    public static final String AVG = "AVG";
    public static final String MAX = "MAX";

    // delimeter constants
    public static final String AT_THE_RATE = "@";
    public static final String FILE_EXTENSION_TXT = ".txt";
    public static final String NEW_LINE = "\n";
    public static final String CARRIAGE_RETURN = "\r";

    // activity level header constants
    public static final String NODE_NAME = "NodeName";
    public static final String ACTIVITY_RESULT = "ActivityResult";
    public static final String ACTIVITY_START_TIME = "ActivityStartTime";
    public static final String ACTIVITY_END_TIME = "ActivityEndTime";
    public static final String PRECHECK_DURATION = "PrecheckDuration";
    public static final String EXECUTE_DURATION = "ExecuteDuration";
    public static final String PROCESS_NOTIFICATION_DURATION = "ProcessNotificationDuration";
    public static final String HANDLE_NOTIFICATION_DURATION = "HandleTimeoutDuration";

    public static final String PLATFORM_TYPE = "Platform Type : ";
    public static final String ACTIVITY_NAME = "Activity Name : ";

    public static final String HEADER_NOTE_DURATIONS_IN_SECONDS = "#Note: All durations in the report are in seconds.";
    public static final String HEADER_NOTE_NEGETIVE_DURATION = "#Note: A step duration will be negative" + " if it is persisted before the previous step duration got persisted.";
    public static final String JOB_ID = "Job Id : ";
    public static final String JOB_NAME = "Job Name : ";
    public static final String JOB_START_TIME = "Job Start Time : ";
    public static final String JOB_END_TIME = "Job End Time : ";
    public static final String JOB_DURATION = "Job Duration : ";

    public static final String JOB_NOT_COMPLETED_MESSAGE = "Job has not Completed";
    public static final String JOB_REPORT_TXT = "jobReportCsv";

}
