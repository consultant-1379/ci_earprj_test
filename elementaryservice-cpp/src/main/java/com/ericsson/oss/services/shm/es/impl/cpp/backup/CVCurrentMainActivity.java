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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

public enum CVCurrentMainActivity {

    // All messages are according to CPP-MOM
    // Modified "Uploading a backup cv to a OSS." to "Uploading the backup file to OSS File Store."
    UNKNOWN("Unknown Main Activity"), IDLE("The CV function is idle."), CREATING_CV("Creating a CV."), DELETING_CV("Deleting a CV."), REMOVING_CV_FROM_ROLLBACK_LIST(
            "Removing a CV from the rollback list."), SETTING_CV_FIRST_IN_ROLLBACK_LIST("Setting a CV first in the rollback list."), SETTING_CV_AS_STARTABLE("Setting a CV as startable. "), EXPORTING_BACKUP_CV(
            "Uploading the backup file to OSS File Store."), IMPORTING_BACKUP_CV("Downloading backup CV from OSS. "), RESTORING_DOWNLOADED_BACKUP_CV("Restoring a downloaded backup CV."), VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE(
            "Verifying downloaded backup CV before restore."), CANCELLING_RESTORE_OF_DOWNLOADED_BACKUP_CV("Cancelling restore of a downloaded backup CV."), CONFIRMING_RESTORE_OF_DOWNLOADED_BACKUP_CV(
            "Confirming the restore of the downloaded backup CV."), CHANGING_RESTORE_CONFIRMATION_TIMEOUT("Changing the restore confirmation timeout value."), CONFIG_COUNTDOWN_ONGOING(
            "Counting down the time left to confirm a configuration change."), EMERGENCY_RESTORE_OF_DOWNLOADED_BACKUP_CV("Executing emergency restore of a downloaded backup CV.");

    private String activityMessage;

    private CVCurrentMainActivity(final String activityMessage) {
        this.activityMessage = activityMessage;
    }

    public String getActivityMessage() {
        return activityMessage;
    }

    public static CVCurrentMainActivity getMainActivity(final String mainActivity) {
        for (final CVCurrentMainActivity s : CVCurrentMainActivity.values()) {
            if (s.name().equalsIgnoreCase(mainActivity)) {
                return s;
            }
        }
        return null;
    }

}
