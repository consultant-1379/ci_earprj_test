/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common;

public class Constants {

    private Constants() {

    }

    public static final String BACKUP_ACCOUNT = "BACKUP";
    public static final String LOG_EXCEPTION = "Exception occurred in \"%s\" for the activity \"%s\" on node \"%s\".";
    public static final String BACKUP_JOB = "Backup job";
    public static final String RESTORE_JOB = "Restore job";
    public static final String BACKUP_NAME = "BACKUP_NAME";
    public static final String BACKUP = "BACKUP";
    public static final String BACKUP_FILE_DOES_NOT_EXIST = "Backup file does not exist";
    public static final String RESTORE = "RESTORE";
    public static final String NETWORKELEMENT = "NetworkElement=";
    public static final String NETYPE = "netype";
    public static final String ACTIVITY_JOB_ID = "ActivityJobId";
    public static final String ACTIVITY_NAME = "ActivityName";
    public static final String EXCEPTION_OCCURED_FAILURE_REASON = "exception occured";
    public static final String DELIMETER = "@";
    public static final String IDLE = "IDLE";
    public static final String COMPLETE = "COMPLETE";
    public static final String FAILED = "FAILED";
    public static final String UPLOADING = "UPLOADING";
    public static final String SUCCESS = "Success";
    public static final String UNAUTHORIZED_USER = "User authorization Failed.";
    public static final String BACKUPFILE_UNAVAILABLE = "Backup file does not exists in ENM";
    public static final String INVENTORY_SUPERVISION_DISABLED = "Inventory supervision is Disabled";
    public static final String LICENSE_JOB = "License job";
    public static final String LICENCE = "LICENCE";
    public static final String LICENSE_FILEPATH = "LICENSE_FILEPATH";
    public static final String NO_BACKUP_FILE_SELECTED = "No backup file selected for deletion";
    public static final String PROCESSED_BACKUPS = "PROCESSED_BACKUPS";
    public static final String CURRENT_BACKUP = "currentBackup";
    public static final String TOTAL_BACKUPS = "totalBackups";
    public static final String INTERMEDIATE_FAILURE = "INTERMEDIATE_FAILURE";
    public static final String INVENTORY_SUPERVISION = "InventorySupervision";
    public static final String COMMA = ",";
    public static final String EQUAL = "=";
    public static final String ONE = "1";

    //License job operation status
    public static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";
    public static final String UNKNOWN_SIGNATURE_TYPE = "UNKNOWN_SIGNATURE_TYPE";
    public static final String UNKNOWN_FINGERPRINT_METHOD = "UNKNOWN_FINGERPRINT_METHOD";
    public static final String UNKNOWN_FINGERPRINT = "UNKNOWN_FINGERPRINT";
    public static final String ERROR_CORRUPT_SIGNATURE = "ERROR_CORRUPT_SIGNATURE";
    public static final String ERROR_NO_SPACE = "ERROR_NO_SPACE";
    public static final String ERROR_NO_STORAGE = "ERROR_NO_STORAGE";
    public static final String ERROR_TRANSFER_FAILED = "ERROR_TRANSFER_FAILED";
    public static final String ERROR_SEQUENCE_NUMBER = "ERROR_SEQUENCE_NUMBER";
    public static final String ERROR_XML_SYNTAX = "ERROR_XML_SYNTAX";
    public static final String ERROR_SYSTEM_ERROR = "ERROR_SYSTEM_ERROR";
    public static final String NOOP = "NOOP";
    public static final String LKF_CONFIGURING = "LKF_CONFIGURING";
    public static final String LKF_DOWNLOADING = "LKF_DOWNLOADING";
    public static final String LKF_VALIDATING = "LKF_VALIDATING";
    public static final String LKF_INSTALLING = "LKF_INSTALLING";
    public static final String LKF_ENABLING = "LKF_ENABLING";

    public static final String DOWNLOAD = "download";
    public static final String ACTIVATE = "activate";
    public static final String CONFIRM = "confirm";


    /**
    *
    * Progress States
    *
    */
   public enum ActivityProgressStateEnm {
       AWAITING_DOWNLOAD, INITIATING_DOWNLOAD, FLASHING, ACTIVATING, DOWNLOADING;
       /**
        *
        * @param operstatus
        *            operstatus
        * @return boolean
        *              boolean
        */
       public static boolean isContains(final String operstatus) {

           for (final ActivityProgressStateEnm s : ActivityProgressStateEnm.values()) {
               if (s.name().equals(operstatus)) {
                   return true;
               }
           }
           return false;
       }

   }

   /**
   *
   * Success States
   *
   */
  public enum ActivitySuccessStateEnm {
       AWAITING_ACTIVATION, AWAITING_NR_COMMIT, IDLE;
      /**
       *
       * @param operstatus
       *            operstatus
       * @return boolean
       *              boolean
       */
      public static boolean isContains(final String operstatus) {

          for (final ActivitySuccessStateEnm s : ActivitySuccessStateEnm.values()) {
              if (s.name().equals(operstatus)) {
                  return true;
              }
          }
          return false;
      }

  }
}
