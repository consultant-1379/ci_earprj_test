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
package com.ericsson.oss.services.shm.job.rbac.impl;

public class AccessControlConstants {

    public static final String SHM_SUPERVISION_CONTROLLER_SERVICE = "cppinventorysynch_service";
    public static final String EXECUTE_OPERATION = "execute";
    public static final String CREATE_OPERATION = "create";
    public static final String UPDATE_OPERATION = "update";
    public static final String DELETE_OPERATION = "delete";
    public static final String CANCEL_OPERATION = "cancel";

    // Specific actions for JobService
    public static final String JOB_CONTINUE_ACTION = "job_continue";

    // Specific to GSM
    public static final String OPS_ENM = "ops_enm";

}
