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
package com.ericsson.oss.services.shm.systemrestore.impl;

public class RestoreInitiationServiceRecordingConstants {

    // SHM as Event source 
    public static final String SHM_SOURCE = "SHM";

    // ENM System Restore
    public static final String SYSTEM_RESTORE_RESPONSE_ALLOWED = "SHM.SYSTEM_RESTORE_RESPONSE_ALLOWED";
    public static final String SYSTEM_RESTORE_RESPONSE_NOT_ALLOWED = "SHM.SYSTEM_RESTORE_RESPONSE_NOT_ALLOWED";
    public static final String SYSTEM_RESTORE_RESPONSE_COMPLETED = "SHM.SYSTEM_RESTORE_RESPONSE_COMPLETED";
    public static final String SYSTEM_RESTORE_INITIATED = "SHM.SYSTEM_RESTORE_INITIATED";
    public static final String SYSTEM_RESTORE_FINISHED = "SHM.SYSTEM_RESTORE_FINISHED";

    // Additional Information
    public static final String ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_ALLOWED = "SHM system restore can be initiated";
    public static final String ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_NOT_ALLOWED = "SHM system restore can't be executed";
    public static final String ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_COMPLETED = "SHM system restore can't be executed as Restore has already been performed";
    public static final String ADDITIONAL_INFO_WHEN_RESTORE_IS_INITIATED = "SHM System restore initiated";
    public static final String ADDITIONAL_INFO_WHEN_RESTORE_IS_FINISHED = "SHM System restore finished successfully";

}