/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.axe.common;

public class AxeConstants {

    private AxeConstants() {

    }

    public static final String INPUT_BACKUP_NAMES = "backupName";
    public static final String UPLOAD_BACKUP_DETAILS = "UPLOAD_BACKUP_DETAILS";
    public static final String INTERMEDIATE_FAILURE = "intermediateFailure";
    public static final String UPLOAD_DISPLAY_NAME = "Upload";
    public static final String DELETE_BKP_DISPLAY_NAME = "Delete Backup";
    public static final String TOTAL_BACKUPS = "totalBackups";
    public static final String CURRENT_BACKUP = "currentBackup";
    public static final String PROCESSED_BACKUPS = "processedBackups";
    public static final String FAILED_BACKUPS = "failedBackups";
    public static final String SESSION_ID = "sessionId";
    public static final String CLUSTER_SUFFIX = "-AXE_CLUSTER";
    public static final String CLUSTER_BACKUP = "Cluster";
    public static final String NODE_COMPONENT_SEPERATOR = "__";
    public static final String CREATE_BACKUP_DISPLAY_NAME = "Createbackup";
    public static final String INSTALL_LICENSE_DISPLAY_NAME = "Licence install";
    public static final String CHAR_ENCODING = "UTF-8";
    public static final String REST_HOST_NAME = "haproxy-int";
    public static final String HOST_NAME = "hostname";
    public static final String BACKUP_COMPLETE = "BACKUP_COMPLETE";
    public static final String BACKUP_ONGOING = "Backup ongoing";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String BODY_PARAM_FILE = "file";
    public static final String AP2_CLUSTER_IP_ADDRESS = "ap2clusterIpAddress";
    public static final String DESTINATION_PATH = "destinationPath";
    public static final String ERICSSON = "ericsson";
    public static final String TOR = "tor";
    public static final String HOME = "home";
    public static final String INVALID_RESPONSE = "Invalid Response from WinFiol";
    public static final String APG2_COMPONENT = "APG2";
    public static final String BACKUP_NAME = "BACKUP_NAME";
    public static final String BACKUPNAME_AND_EXTENSION_DELIMITER = "-";
    public static final String AP_COMPONENT = "AP";
    public static final String CP_COMPONENT = "CP";
    public static final String APG_COMPONENT = "APG";
    public static final String ROTATE = "Rotate";
    public static final String OVERWRITE = "Overwrite";
    public static final String CREATE_NEW = "CREATE_NEW";
    public static final String CREATE_NEW_AND_ROTATE = "CREATE_NEW_AND_ROTATE";
    public static final String OVERWRITE_AND_ROTATE = "OVERWRITE_AND_ROTATE";
    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String COOKIE_HEADER = "Cookie";
    public static final String WINFIOL_SERVERID = "WINFIOL_SERVERID";
    public static final String SESSIONIDRESPONSE = "SessionIdResponse";
    public static final String SEMI_COLON = ";";
    public static final String LATEST_BACKUPDATA_AFTER_ROTATE = "RELFSW0";
    public static final String CREATE_NEW_BACKUP = "Create new backup";
    public static final String OVERWRITE_BACKUP = "Overwrite backup";
    public static final String ROTATE_BACKUP = "Rotate backup";
    public static final String CREATE_NEW_AND_ROTATE_BACKUP = CREATE_NEW_BACKUP + " and " + ROTATE_BACKUP;
    public static final String OVERWRITE_AND_ROTATE_BACKUP = OVERWRITE_BACKUP + " and " + ROTATE_BACKUP;
    public static final String CREATE_ENCRYPTED_BACKUP = "CREATE_ENCRYPTED_BACKUP";
    public static final String CREATE_REGULAR_BACKUP = "CREATE_REGULAR_BACKUP";
    public static final String ENCRYPTED_BACKUP_HEADER = "BackupAuth";

}
