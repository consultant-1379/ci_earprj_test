/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.axe.common;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupPollResponse {

    public static final String BACKUP_NAME = "backupName";
    public static final String BACKUP_STATUS = "status";
    public static final String PROGRESS_PERCENTAGE = "percentageDone";
    public static final String STATUS_MESAGE = "statusMsg";

    private final String backupName;
    private final String status;
    private final int percentageDone;
    private final String statusMsg;

    public BackupPollResponse(final Map<String, Object> backupPollResponseMap) {
        this.backupName = backupPollResponseMap.get(BACKUP_NAME) != null ? (String) backupPollResponseMap.get(BACKUP_NAME) : "";
        this.status = backupPollResponseMap.get(BACKUP_STATUS) != null ? (String) backupPollResponseMap.get(BACKUP_STATUS) : "";
        this.percentageDone = backupPollResponseMap.get(PROGRESS_PERCENTAGE) != null ? (int) backupPollResponseMap.get(PROGRESS_PERCENTAGE) : 0;
        this.statusMsg = backupPollResponseMap.get(STATUS_MESAGE) != null ? (String) backupPollResponseMap.get(STATUS_MESAGE) : "";
    }

    public String getBackupName() {
        return backupName;
    }

    public String getStatus() {
        return status;
    }

    public int getPercentageDone() {
        return percentageDone;
    }

    public String getStatusMessage() {
        return statusMsg;
    }

    /*
     * This will be used in Job logs display
     */
    @Override
    public String toString() {
        return "ProgressInfo : BackupName=" + backupName + ", Status=" + status + ", ProgressPercentage=" + percentageDone;
    }
}
