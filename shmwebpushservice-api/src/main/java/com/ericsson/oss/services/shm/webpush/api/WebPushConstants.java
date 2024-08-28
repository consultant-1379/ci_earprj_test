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
package com.ericsson.oss.services.shm.webpush.api;

public class WebPushConstants {

    public static final String JOB_KIND = "Job";
    public static final String NE_JOB_KIND = "NEJob";
    public static final String ACTIVITY_JOB_KIND = "ActivityJob";
    public static final String JOB_EVENT = "jobEvent";
    public static final String CREATE_JOB = "create";
    public static final String DELETE_JOB = "delete";

    //Resource and Channel Names for shm page
    public static final String SHM_RESOURCE = "shm";
    public static final String SHM_CHANNEL = "shmChannel";

    //Resource and Channel Names for shmJobDetails page
    public static final String SHM_JOB_DETAILS_RESOURCE = "shmJobDetails";
    public static final String SHM_JOB_DETAILS_CHANNEL = "shmJobDetailsChannel";

    //Channel Name for shmJobCreate and Delete
    public static final String SHM_JOB_RESOURCE = "shmJobs";
    public static final String SHM_JOB_CREATE_DELETE_CHANNEL = "shmJobCreateDeleteChannel";

    //Resource and Channel Names for shmJobLogs page
    public static final String SHM_JOB_LOGS_RESOURCE = "shmJobLogs";
    public static final String SHM_JOB_LOGS_CHANNEL = "shmJobLogsChannel";

    public static final String JOB_ID = "jobId";

    //web push for SHM UI Applications 
    public static final String JOB_DETAILS_APPLICATION = "shmJobDetailsApp";
    public static final String JOB_LOGS_APPLICATION = "shmJobLogsApp";
    public static final String MAIN_JOBS_APPLICATION = "shmApp";
    public static final String SHM_JOBS_APPLICATION = "shmJobsApp";

    //web push for NHC UI Applications 
    public static final String NHC_JOBS_APPLICATION = "nhcJobsApp";
    public static final String NHC_JOB_DETAILS_APPLICATION = "nhcJobDetailsApp";
    public static final String NHC_JOB_LOGS_APPLICATION = "nhcJobLogsApp";
    public static final String NHC_MAIN_JOBS_APPLICATION = "nhcApp";

    //Resource and Channel Names for NHC page
    public static final String NHC_RESOURCE = "nhc";
    public static final String NHC_CHANNEL = "nhcChannel";

    //Resource and Channel Names for nhcJobDetails page
    public static final String NHC_JOB_DETAILS_RESOURCE = "nhcJobDetails";
    public static final String NHC_JOB_DETAILS_CHANNEL = "nhcJobDetailsChannel";

    //Channel Name for nhcJobCreate and Delete
    public static final String NHC_JOB_RESOURCE = "nhcJobs";
    public static final String NHC_JOB_CREATE_DELETE_CHANNEL = "nhcJobCreateDeleteChannel";

    //Resource and Channel Names for nhcJobLogs page
    public static final String NHC_JOB_LOGS_RESOURCE = "nhcJobLogs";
    public static final String NHC_JOB_LOGS_CHANNEL = "nhcJobLogsChannel";

}
