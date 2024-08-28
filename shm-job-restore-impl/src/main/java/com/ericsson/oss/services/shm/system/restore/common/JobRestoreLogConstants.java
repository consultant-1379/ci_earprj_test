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
package com.ericsson.oss.services.shm.system.restore.common;

public class JobRestoreLogConstants {
    public static final String RESTORE_NEJOB_CANCELLING = "NE Job with NEName= \"%s\" is System Cancelling successfully";
    public static final String RESTORE_NEJOB_CANCELLED = "NE Job with NEName= \"%s\" is System Cancelled successfully";
    public static final String RESTORE_ACTIVITY_JOB_CANCELLED = "\"%s\" activity is System Cancelled successfully";

    public static final String JOB_LOG = "log";
    public static final String JOB_LOG_ENTRY_TIME = "entryTime";
    public static final String JOB_LOG_MESSAGE = "message";
    public static final String JOB_LOG_TYPE = "type";
    public static final String SYSTEM_JOB_LOG = "SYSTEM";
    public static final String SYSTEM_CANCELLING = "\"%s\" is System Cancelling.";
    public static final String SYSTEM_CANCELLED = "\"%s\" is System Cancelled.";

    public static final String NE_SYSTEM_CANCELLING = "For \"%s\", \"%s\"'s Job is System Cancelling.";
    public static final String NE_SYSTEM_CANCELLED = "For \"%s\", \"%s\"'s Job is System Cancelled.";

}
