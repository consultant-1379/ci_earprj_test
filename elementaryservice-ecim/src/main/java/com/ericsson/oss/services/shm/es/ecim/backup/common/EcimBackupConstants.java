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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

public class EcimBackupConstants {

    public final static String ECIM_NODE_MODEL_NAMESPACE = "SgsnMmeBRM";
    public static final String BACKUP_MANAGER_MO_TYPE = "BrmBackupManager";
    public static final String BACKUP_MO_TYPE = "BrmBackup";
    public static final String ACTION_EXPORT_BACKUP = "export";

    public static final String ACTION_ID = "actionId";
    public static final String ACTION_NAME = "actionName";
    public static final String PROGRESS_INFO = "progressInfo";
    public static final String PROGRESS_PERCENTAGE = "progressPercentage";
    public static final String RESULT = "result";
    public static final String RESULT_INFO = "resultInfo";
    public static final String STATE = "state";
    public static final String TIME_ACTION_COMPLETED = "timeActionCompleted";
    public static final String TIME_ACTION_STARTED = "timeActionStarted";
    public static final String TIME_OF_LAST_STATUS_UPDATE = "timeOfLastStatusUpdate";

    public static final String BRM_BACKUP_MANAGER_ID = "BACKUP_DOMAIN_TYPE";
    public static final String BACKUP_ID = "brmBackupId";
    public static final String BACKUP_URI = "uri";

    public static final String ACTION_ARG_PATH_ON_FTP_SERVER = "pathOnFtpServer";
    public static final String DOMAIN_NAME = "backupDomain";
    public static final String BACKUP_TYPE = "backupType";
    public static final String BACKUP_FILE_NAME = "backupFileName";
    public static final String FAILED_BACKUPS = "failedBackups";

    public static final String BACKUP_STATUS = "BrmBackupStatus";
    public static final String BRM_BACKUP_NAME = "BACKUP_NAME";
    public static final String BRM_BACKUP_DOMAIN = "BACKUP_DOMAIN";
    public static final String BRM_BACKUP_TYPE = "BACKUP_TYPE";
    public static final String ABSOLUTE_BACKUP_FILE_NAME = "BACKUP_FILE_NAME";

    public static final String scheme = "scheme";
    public static final String userInfo = "userInfo";
    public static final String host = "host";
    public static final String port = "port";
    public static final String path = "path";
    public static final String query = "query";
    public static final String fragment = "fragment";
    public static final String URI = "uri";

    public static final String BACKUP_FILE_EXTENSION = ".zip";

    public static final String SCHEMA_MO = "Schema";
    public static final String ECIM_TOP_NAMESPACE = "ECIM_TOP";

    public static final String ACTION_ARG_BACKUP_NAME = "name";
    public static final String ACTION_DELETE_BACKUP = "deleteBackup";

    public static final String FINISHED = "FINISHED";
    public static final String RUNNING = "RUNNING";
    public static final String CANCELLED = "CANCELLED";
    public static final String CANCELLING = "CANCELLING";

    public static final String BACKUP_NAME = "backupName";

    public static final String NOTIFICATION_ATTRIBUTE_VALUE = "notifiableAttributeValue";
    public static final String PREVIOUS_NOTIFICATION_ATTRIBUTE_VALUE = "previousNotifiableAttributeValue";
    public static final String BACKUPID = "BACKUP_ID";
    public static final String CREATION_TIME = "creationTime";

    public static final String BACKUP_DOMAIN = "backupDomain";
    public static final String BACKUP_FILE_LOCATION = "BACKUP_LOCATION";

    public static final String CREATION_TYPE = "creationType";

    public static final String BRM_BACKUP_STATUS = "status";
    public static final String PRODUCT_DATA = "swVersion";

    public static final String DELETE_BACKUP = "deletebackup";

    public final static String BACKUP_CANCEL_ACTION = "cancelCurrentAction";

    public final static String CREATE_BACKUP = "Createbackup";
    public final static String UPLOAD_BACKUP = "Uploadbackup";
    public final static String RESTORE_BACKUP = "RestoreBackup";
    public static final String CONFIRM_RESTORE = "confirmbackup";
    public static final String CURRENT_MAIN_ACTIVITY = "CURRENT_MAIN_ACTIVITY";
    public static final String EXPORTING_BACKUP_BM = "Uploading the backup file to OSS File Store.";
    public static final String IDLE = "The BM function is idle.";

    public static final String UPLOAD_BACKUP_DETAILS = "UPLOAD_BACKUP_DETAILS";

    public final static String DOWNLOAD_BACKUP = "Downloadbackup";

    public static final String IS_BACKUP_DOWNLOAD_SUCCESSFUL = "isBackupDownloadSuccessful";
    public static final String IS_BRM_BACKUP_MO_CREATED = "isBrmBackupMOCreated";

    public static final String IMPORT_BACKUP_ACTION_NAME = "IMPORT";
    public static final String LOCATION_ENM = "ENM";

    public final static String IMPORT_BACKUP = "ImportBackup";
    public final static String CREATE_BACKUP_CANCEL = "Cancel Create Backup";
    public static final String BACKUP_CREATE_ACTION = "CREATE";
    public static final String BACKUP_CREATE_ACTION_BSP = "createBackup";

    public static final String BACKUP_EXPORT_ACTION = "EXPORT";
    public static final String BACKUP_EXPORT_ACTION_BSP = "export";

    public static final String BACKUP_DELETE_ACTION = "DELETE";
    public static final String BACKUP_DELETE_ACTION_BSP = "deleteBackup";
    public static final String RESTORE_BACKUP_ACTION = "restore";
    public static final String IMPORT_BACKUP_ACTION = "importBackup";

    public static final String DATE_FORMAT_TOBE_APPENDED_TO_BACKUPNAME = "yyyyMMdd_HHmm";
    public static final String LOCATION_NODE = "NODE";
    public static final String CURRENT_BACKUP = "currentBackup";
    public static final String TOTAL_BACKUPS = "totalBackups";

    //ECIM Specific MO attributes
    public static final String BRM_BKP_MNGR_BACKUP_DOMAIN = "backupDomain";
    public static final String BRM_BKP_MNGR_BACKUP_TYPE = "backupType";
    public static final String ECIM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static final String DEFAULT_BACKUP_NAME = "Backup";

    public static final String BACKUP_NAME_IN_ADDITIONAL_INFO = "Name: ";
    public static final String CREATE_SECURE_BACKUP_MOACTION = "CreateSecuredBackupWithPasswd";

}
