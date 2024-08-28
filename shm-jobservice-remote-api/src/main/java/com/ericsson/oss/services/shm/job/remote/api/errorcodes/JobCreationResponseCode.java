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
package com.ericsson.oss.services.shm.job.remote.api.errorcodes;

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

/**
 * Enum to fetch Error codes and messages for job creation from remote services.
 * 
 * @author xmahnit 
 * 
 * As per CLI-Design guidelines Allowed Error codes and Messages assigned for SHM - 13000 - 13999
 * 
 *         Backup job related error code range - 13200 - 13249 Restart job related error code range - 13250 - 13299 Delete_Upgrade job related error code range - 13300 - 13349
 */
public enum JobCreationResponseCode {
    JOB_NAME_ALREADY_EXIST(13200, "Creation of job failed, as there is another job created with the same name."), 
    BACKUP_NAME_NOT_ALLOWED(13201, "BackupName accepts a maximum length of 40 characters"), 
    START_TIME_BEFORE_NOW(13202, "Start time is older than the current time"), 
    END_TIME_BEFORE_NOW(13203, "End time is older than the current time"), 
    START_TIME_INVALID(13204, "Start time is Invalid"), 
    END_TIME_INVALID(13205, "End time is Invalid"), 
    CRON_EXPRESSION_INVALID(13206, "Cron expression is invalid"), 
    PERIODICITY_LESS_THAN_ONE_DAY(13207,"Cron periodicity is less than one day and it is not supported in shm"), 
    ONLY_END_TIME_NOT_ALLOWED(13208,"Please provide start time or cron expression for scheduled job creation. Only end time is not allowed"), 
    NE_SPEC_NOT_FOUND(13209, "Search criteria did not match any nodes"),
    ME_FDNS_NOT_FOUND(13210, "Me fdns not found with specified Ne specification"), 
    UNEXPECTED_ERROR(13211, "Unexpected error during job creation"), 
    BACKUPCOMMENT_NAME_NOT_ALLOWED(13212, "BackupComment accepts a maximum length of 40 characters"), 
    INVALID_RESATRT_REASON(13250, "Invalid Restart Reason"), 
    INVALID_RESATRT_RANK(13251, "Invalid Restart Rank"), 
    PRODUCTNUMBER_NOT_NULL_EMPTY(13300, "ProductNumber cannot not be Null/Empty"), 
    PRODUCTREVISION_NOT_NULL_EMPTY(13301, "ProductRevision cannot not be Null/Empty"),
    INVALID_LICENSE_KEY_PATH(13302, "Invalid license key file path"),
    INVALID_LICENSE_FINGERPRINT(13303, "Invalid license finger print"),
    INVALID_JOB_NAME(13304,"Invalid job name");

    private final int errorCode;
    private final String errorMessage;

    private JobCreationResponseCode(final int errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
