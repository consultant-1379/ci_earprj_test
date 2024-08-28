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

public enum CVCurrentDetailedActivity {

    UNKNOWN("Unknown Detailed Activity"), IDLE("The CV function is idle."), CREATING_BACKUP("Creating a backup of a CV existing on the node."), TRANSFERING_BACKUP_TO_REMOTE_SERVER(
            "The backup CV is transferred to a remote FTP Server."), RETREIVING_BACKUP_FROM_REMOTE_SERVER("Downloads a remote backup of a CV to the node."), UNPACKING_RETREIVED_BACKUP(
            "The retrieved backup CV is unpacked."), RESTORE_REQUESTED("Variant of restore action initiated (that is, restore/forcedRestore)."), VERIFYING_UPGRADE_PACKAGE_PRESENT(
            "Verifying that there are no missing upgrade packages."), VERIFYING_CHECKSUM_FOR_LOAD_MODULES("Verifying the checksum for all load modules used by the CV."), VERIFYING_CORE_CENTRAL_MP(
            "Verifying that the node core central MP position is the same as in the downloaded backup CV."), VERIFYING_HARDWARE_SOFTWARE_COMPATIBILITY(
            "Verifying the node board/slot configuration with the board/slot configuration in the downloaded backup CV."), VERIFYING_NODE_IDENTITY(
            "Verifying if the downloaded backup CV originates from this node/site or from another node/site."), VERIFYING_APPLICATION_CONFIGURATION_DATA(
            "Verifying node configuration data with application configuration data in the downloaded backup CV."), VERIFYING_NETWORK_CONFIGURATION_FOR_IP_ATM(
            "Verifying node network configuration for IP/ATM with the network configuration for IP/ATM in the downloaded backup CV."), RESTORE_INITIATED(
            "The restore has been initiated, that is the verification phase has been successfully executed."), SAVING_ROLLBACK_CV("Saving a rollback Configuration Version."), NODE_RESTART_REQUEST(
            "The node is to be restarted."), INITIATING_CONFIRM_RESTORE_TIME_SUPERVISION("The confirm restore time supervision is to be started."), AWAITING_RESTORE_CONFIRMATION(
            "Waiting for confirmation of the restore request."), RESTORE_CONFIRMATION_RECEIVED("The restore of the node is made permanent."), TERMINATING_CONFIRM_RESTORE_TIME_SUPERVISION(
            "The confirm restore time supervision is to be stopped."), SAVING_FINAL_CV("Saving a final Configuration Version."), RESTORE_EXECUTED("Restore has been executed successfully."), EXECUTION_FAILED(
            "The execution of an action has been aborted due to not possible to continue."), EXECUTING_BLOCKING_ACTION("A blocking action is executing (no detailed information is available)."), VERIFYING_HARDWARE_COMPATIBILITY(
            "Verifying that the hardware configuration in the downloaded backup CV is compatible with node hardware configuration."), VERIFY_RESTORE_REQUESTED(
            "Verification before restore requested, that is, action verifyRestore has been invoked."), RESTORE_CANCELLED("Restore has been cancelled."), VERIFYING_POSSIBLE_TO_CREATE_REQUIRED_NUMBER_OF_CVS(
            "Verifying that it is possible to create the required number of CVs at restore."), EXPORT_OF_BACKUP_CV_REQUESTED("Export of a node CV to a FTP server has been requested."), IMPORT_OF_BACKUP_CV_REQUESTED(
            "Import of a backup CV from a FTP server to the node has been requested."), CONFIG_COUNTDOWN_ONGOING("Counting down the time left to confirm a configuration change."), EMERGENCY_RESTORE_INITIATED(
            "Emergency restore has been initiated."), EMERGENCY_RESTORE_EXECUTED("Emergency restore has been executed successfully."), ACTIVATE_ROBUST_RECONFIG_REQUESTED(
            "Activation of robust reconfiguration requested."), DEACTIVATE_ROBUST_RECONFIG_REQUESTED("Deactivation of robust reconfiguration requested."), VERIFYING_NETWORK_CONFIGURATION_FOR_IP(
            "Verifying node network configuration for IP with the network configuration for IP in the downloaded backup CV."), VERIFYING_LM_SM(
            "Verifying the certificate and signature load modules defined in the UCF."), VERIFYING_CHECKSUM_FOR_UCF("Verifying the checksum for the UCF associated to the CV.");

    private String activityMessage;

    private CVCurrentDetailedActivity(final String activityMessage) {
        this.activityMessage = activityMessage;
    }

    public String getActivityMessage() {
        return activityMessage;
    }

    public static CVCurrentDetailedActivity getDetailedActivity(final String detailedActivity) {
        for (final CVCurrentDetailedActivity s : CVCurrentDetailedActivity.values()) {
            if (s.name().equalsIgnoreCase(detailedActivity)) {
                return s;
            }
        }
        return null;
    }

}
