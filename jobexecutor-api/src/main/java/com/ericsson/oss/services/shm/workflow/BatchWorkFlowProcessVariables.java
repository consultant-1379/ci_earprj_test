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
package com.ericsson.oss.services.shm.workflow;

/**
 * Constants for configuration of batch work flow model.Must be in a shared module will be used by SHM Service.
 */
public class BatchWorkFlowProcessVariables {

    public static final String TEMPLATE_JOB_ID = "templateJobId";
    public static final String MAIN_JOB_ID = "mainJobId";
    public static final String BATCH_STARTUP = "batchStartup";
    public static final String BATCH_STARTUP_SCHEDULED = "scheduled";
    public static final String BATCH_STARTUP_SCHEDULED_TIME = "scheduleTime";
    public static final String BATCH_STARTUP_MANUAL = "manual";
    public static final String BATCH_STARTUP_IMMEDIATE = "immediate";
    public static final String TOTAL_NE_COUNT_SUBMITTED = "neSubmitted";
    public static final String NE_COMPLETED_COUNTER = "neCount";
    public static final String JOB_TYPE = "jobType";
    public static final String REPEAT_TYPE = "repeatType";
    public static final String REPEAT_ON = "repeatOn";
    public static final String IS_JOB_RESUMED = "isJobResumed";
    public static final String PERIODIC = "periodic";

}
