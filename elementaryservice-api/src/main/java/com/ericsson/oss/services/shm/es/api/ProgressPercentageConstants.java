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
package com.ericsson.oss.services.shm.es.api;

/**
 *This class is used to hold all the constant variables for SHM Activity job Progress Percentages.
 * 
 * @author tcsnean
 * 
 */
public class ProgressPercentageConstants {
    public static final double PRECHECK_START_PROGRESS_PERCENTAGE = 2.0;
    public static final double PRECHECK_END_PROGRESS_PERCENTAGE = 10.0;
    public static final double DELETE_ROLLBACK_LIST_END_PROGRESS_PERCENTAGE = 20.0;
    public static final double MOACTION_START_PROGRESS_PERCENTAGE = 25.0;
    public static final double UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE = 70.0;
    public static final double MOACTION_END_PROGRESS_PERCENTAGE = 80.0;
    public static final double DELETE_NODE_BACKUP_LIST_END_PROGRESS_PERCENTAGE = 70.0;
    public static final double ACTIVITY_END_PROGRESS_PERCENTAGE = 100.0;
    public static final double EXECUTE_REPEAT_PROGRESS_PERCENTAGE = 90.0;
    public static final double HANDLE_TIMEOUT_PROGRESS_PERCENTAGE = 90.0;
    public static final double CREATE_UPGRADEPKG_END_PROGRESS_PERCENTAGE = 50.0;
}
