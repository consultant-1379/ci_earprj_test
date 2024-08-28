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
package com.ericsson.oss.services.shm.vran.constants;

//TODO : Why SHM commons are not used here?
//NOTE
public class VranUprgradeConstants {

    // Upgrade constants
    public static final String UPGRADE = "UPGRADE";
    public static final String CREATE_UPGRADE_PACKAGE = "createUpgradePackage";

    public static final String DELETE_ACTIVITY = "Delete";

    public static final String INITIALIZED = "INITIALIZED";
    public static final String PREPARE_IN_PROGRESS = "PREPARE_IN_PROGRESS";
    public static final String VERIFY_IN_PROGRESS = "VERIFY_IN_PROGRESS";
    public static final String ACTIVATION_IN_PROGRESS = "ACTIVATION_IN_PROGRESS";
    public static final String CONFIRM_IN_PROGRESS = "CONFIRM_IN_PROGRESS";
    public static final String PREPARE_COMPLETED = "PREPARE_COMPLETED";
    public static final String WAITING_FOR_CONFIRM = "WAITING_FOR_CONFIRM";
    public static final String CONFIRM_COMPLETED = "CONFIRM_COMPLETED";
    public static final String CANCEL_ONGOING_REQUESTED = "CANCEL_ONGOING_OPERATION_REQUESTED";
    public static final String VRAN_UPGRADE_JOB_PROGRESS_NOTIFICATION_FILTER = "(platformType = '" + VranJobConstants.VRAN + "' AND jobType = '" + UPGRADE + "')";

    public static final String CREATE_OPERATION = "CREATE";
    public static final String PREPARE_OPERATION = "PREPARE";
    public static final String VERIFY_OPERATION = "VERIFY";
    public static final String ACTIVATE_OPERATION = "ACTIVATE";
    public static final String CONFIRM_OPERATION = "CONFIRM";

}
