/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.constants;

import com.ericsson.oss.services.shm.common.recording.RecordingConstants;

/***
 * This class provides constants used for recording VRAN job Events
 * 
 * @author xindkag
 */
public class VranJobEvents {

    // Software upgrade job events 
    public static final String PREPARE_SERVICE = "SHM.PREPARE_SERVICE";
    public static final String VERIFY_SERVICE = "SHM.VERIFY_SERVICE";
    public static final String ACTIVATE_SERVICE = "SHM.ACTIVATE_SERVICE";
    public static final String CONFIRM_SERVICE = "SHM.CONFIRM_SERVICE";
    public static final String PREPARE_PROCESS_NOTIFICATION = "SHM.PREPARE_PROCESS_NOTIFICATION";
    public static final String VERIFY_PROCESS_NOTIFICATION = "SHM.VERIFY_PROCESS_NOTIFICATION";
    public static final String ACTIVATE_PROCESS_NOTIFICATION = "SHM.ACTIVATE_PROCESS_NOTIFICATION";
    public static final String CONFIRM_PROCESS_NOTIFICATION = "SHM.CONFIRM_PROCESS_NOTIFICATION";
    public static final String CANCEL_NOTIFICATION = "SHM.CANCEL_PROCESS_NOTIFICATION";
    public static final String VERIFY_EXECUTE = "SHM.VERIFY_EXECUTE ";
    public static final String JOB_PROGRESS = "SHM.JOB_PROGRESS ";

    // Onboard Software Package job events
    public static final String ONBOARD_SOFTWARE_PACKAGE_NOTIFICATION = "SHM.ONBOARD_PROCESS_NOTIFICATION";

    // Delete Software Package job events
    public static final String DELETE_SOFTWARE_PACKAGE_NOTIFICATION = "SHM.DELETE_PROCESS_NOTIFICATION";

    private static final String SOFTWARE_PACKAGE = "SOFTWARE_PACKAGE";
    public static final String DELIMITER = ".";
    public static final String PACKAGE_NOT_FOUND_EXCEPTION = RecordingConstants.EVENT_NAME_PREFIX + "PACKAGE_NOT_UNDER_PROVIDERS";
    public static final String FILEPATH_EMPTY_EXCEPTION = RecordingConstants.EVENT_NAME_PREFIX + "FILEPATH_EMPTY_EXCEPTION";
    public static final String DELETION_FAILURE_EXCEPTION = RecordingConstants.EVENT_NAME_PREFIX + SOFTWARE_PACKAGE + DELIMITER + "DATABASE_DELETION_FAILURE_EXCEPTION";
    public static final String DATABASE_EXCEPTION = RecordingConstants.EVENT_NAME_PREFIX + SOFTWARE_PACKAGE + DELIMITER + "DATABASE_EXCEPTION";
    public static final String RESOURCE_EXCEPTION = RecordingConstants.EVENT_NAME_PREFIX + SOFTWARE_PACKAGE + DELIMITER + "RESOURCE_EXCEPTION";
    public static final String IO_EXCEPTION = RecordingConstants.EVENT_NAME_PREFIX + SOFTWARE_PACKAGE + DELIMITER + "IO_EXCEPTION";
}
