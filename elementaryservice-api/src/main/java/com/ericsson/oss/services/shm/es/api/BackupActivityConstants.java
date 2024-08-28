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

public class BackupActivityConstants {

    private BackupActivityConstants() {
    }

    public static final String CV_MO_TYPE = "ConfigurationVersion";

    public static final String ACTION_CREATE_CV = "create";
    public static final String ACTION_DELETE_CV = "delete";
    public static final String ACTION_UPLOAD_CV = "putToFtpServer";
    public static final String ACTION_SET_STARTABLE_CV = "setStartable";
    public static final String ACTION_SET_FIRST_IN_ROLLBACK_CV = "setFirstRollbackList";
    public static final String ACTION_CONFIRM_RESTORE_CV = "confirmRestore";
    public static final String ACTION_DOWNLOAD_CV = "getFromFtpServer";
    public static final String ACTION_VERIFY_RESTORE_CV = "verifyRestore";
    public static final String ACTION_FORCED_RESTORE_CV = "forcedRestore";
    public static final String ACTION_RESTORE_CV = "restore";
    public static final String EMPTY_STRING = "";
    public static final String ACTION_VERIFY = "verify";
    public static final String ACTION_CONFIRM = "confirm";

    public static final String CV_IDENTITY = "CV_IDENTITY";
    public static final String CV_COMMENT = "CV_COMMENT";
    public static final String CURRENT_MAIN_ACTIVITY = "CURRENT_MAIN_ACTIVITY";
    public static final String CURRENT_DETAILED_ACTIVITY = "CURRENT_DETAILED_ACTIVITY";
    public static final String PREVIOUS_CURRENT_MAIN_ACTIVITY = "PREVIOUS_CURRENT_MAIN_ACTIVITY";
    public static final String PREVIOUS_CURRENT_DETAILED_ACTIVITY = "PREVIOUS_CURRENT_DETAILED_ACTIVITY";

    public static final String CV_NAME_PREFIX = "OP_";

    public static final String VERIFY_RESTORE_CV_COMPLETED = "VERIFY_RESTORE_CV_COMPLETED";
    public static final String CONFIRM_RESTORE_CV_COMPLETED = "CONFIRM_RESTORE_CV_COMPLETED";

    public static final String ACTION_FORCED_RESTORE = "forcedRestore";
    public static final String ACTION_RESTORE = "restore";
    public static final String ACTION_CANCEL_RESTORE = "cancelRestore";

    public static final String DATE_FORMAT = "yyyyMMdd_HHmm";

    public static final int PROD_ID_LIMIT = 23;
    public static final int CV_NAME_MIN_CHAR_LIMIT = 26;
    public static final int CV_NAME_MAX_CHAR_LIMIT = 40;

    public static final String DEFAULT_PROD_ID = "InitialUP";

    public static final String CV_STATUS = "cvStatus";

    public static final String CV_NAME = "CV_NAME";
    public static final String CV_TYPE = "CV_TYPE";
    public static final String CV_LOCATION = "CV_LOCATION";
    public static final String STARTABLE_CV_NAME = "STARTABLE_CV_NAME";
    public static final String ROLLBACK_CV_NAME = "ROLLBACK_CV_NAME";
    public static final String UPLOAD_CV_NAME = "UPLOAD_CV_NAME";

    public static final String REMOVE_ROLLBACK_LIST = "removeFromRollbackList";

    public static final String BACKUP_FILE_EXTENSION = ".zip";
    public static final String FORCED_RESTORE = "FORCED_RESTORE";

    public static final String PROCESSED_BACKUPS = "PROCESSED_BACKUPS";
    public static final String TOTAL_BACKUPS = "TOTAL_BACKUPS";
    public static final String INTERMEDIATE_FAILURE = "INTERMEDIATE_FAILURE";

    public static final String BACKUP_FILE_EXTENSION_GZ = ".tar.gz";
    public static final String DOWNLOAD_CV = "download";
    public static final String EXPORT_CV = "exportcv";
    public static final String CURRENT_BACKUP = "currentBackup";
    public static final String MO_ACTIVITY_END_PROGRESS = "moActivityEndProgress";
    public static final String INTERMEDIATE_SUCCESS = "INTERMEDIATE_SUCCESS";
}
