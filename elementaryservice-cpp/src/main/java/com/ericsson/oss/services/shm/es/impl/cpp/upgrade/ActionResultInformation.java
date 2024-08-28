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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

public enum ActionResultInformation {

    UNKNOWN(-1,"Unknown Action Result Information"), EXECUTED(0, "The invoked action has been successfully executed without warnings."), UNSPECIFIED(1, "An error or warning that is not specified is detected."), LM_CHECKSUM_VER_FAILED(2,
            "A load module checksum verification has failed."), NOT_ENOUGH_AVAIL_DISK_SPACE(3,
            "There is not enough disk space for the required number of new CVs to be created automatically during an upgrade."), MAX_NO_CV_WILL_BE_EXCEEDED(4,
            "The maximum number of allowed CVs will be exceeded if the required number of new CVs is created automatically during an upgrade."), UPGRADE_FROM_CURRENT_UP_NOT_ALLOWED(5,
            "The Upgrade Window element in the UCF does not specify the current Upgrade Package as a valid upgrade-from version."), NON_SUPPORTED_PIU(6,
            "A plug-in-unit that is not supported is detected, that is, the plug-in-unit in the node is not defined in the UCF."), FAULTY_PIU(7, "A faulty plug-in-unit has been detected."), CREATION_OF_CV_FAILED(
            8, "The auto-creation of an install CV failed."), ACTION_NOT_ALLOWED(9, "The requested action is not allowed as another action is already in progress for another UpgradePackage MO."), INST_MANUALLY_CANCELLED(
            10, "An ongoing installation has been cancelled (aborted) due to request from client."), FTP_SERVER_NOT_ACCESSIBLE(11,
            "The FTP server to be used for downloads of load modules is not accessible."), INSUF_DISK_FOR_LMS(12, "The required disk space for load modules to be installed is insufficient."), FTP_SERVER_IP_ADDR_ERROR(
            13, "Error detected in the IP address of the FTP server."), DELTA_INSTALL_MERGE_ERROR(14,
            "The install of the delta UCF failed, that is, the analysis of the delta UCF with the current active UP's UCF failed."), SELECTIVE_INSTALL_ERROR(15,
            "Error detected during the selection phase of an install."), EXECUTION_FAILED(16, "The execution of invoked action failed."), EXECUTED_WITH_WARNINGS(17,
            "The action has been successfully executed but with warnings."), LM_FILES_NOT_INST(18,
            "All load module files required for a supported plug-in-unit (according to the UCF) are not installed."), NOT_SUPPORTED_SUBRACK_TYPE(19,
            "A subrack that is not supported is detected, that is, the subrack in the node is not defined in the UCF. This will prevent an upgrade from being executed. "), SLOT_OF_PIU_NOT_CONNECTED_TO_SWA(
            20, "A Slot related to a plug-in-unit is not connected to an SwAllocation."), LM_TYPE_NOT_ALLOWED(21,
            "ManagedObject(s) will be connected (via an SwAllocation) to Repertoire(s) referencing load modules of not allowed types."), PIUTYPE_NOT_UNIQUE(22,
            "Several SwAllocations connected to the same Slot define the same PiuType."), PIUTYPE_LOADLIST_INCONSISTENT(23,
            "All Repertoires in a SwAllocation do not support the same set of PiuTypes."), PROGRAM_INSTANCES_INCONSISTENT(24,
            "The result of combining UCF data for program instance values with how repertoires are connected to an upgradeable PIU is inconsistent."), HEAP_SIZE_INCONSISTENT(25,
            "The result of combining UCF data for heap size values with how repertoires are connected to an upgradeable PIU is inconsistent."), POOL_SIZE_INCONSISTENT(26,
            "The result of combining UCF data for pool size values with how repertoires are connected to an upgradeable PIU is inconsistent."), LOADER_DATA_INCONSISTENT(27,
            "The result of combining UCF data for loader type and device loader name with how repertoires are connected to an upgradeable PIU is inconsistent"), DEVICE_LOADER_INFORMATION_INCONSISTENT(
            28, "The result of combining UCF data for device loader information with how repertoires are connected to an upgradeable PIU is inconsistent."), LOADER_TYPE_NOT_ALLOWED_FOR_PARENT_MO(29,
            "The result of combining UCF data for loader type with how repertoires are connected to an upgradeable PIU is inconsistent."), ACTION_NOT_ALLOWED_IF_MORE_THAN_ONE_UP(30,
            "The action is not allowed if more than one UP exists on node."), ACTION_ALLOWED_ONLY_IF_UP_HWSENSITIVE(31,
            "The action is allowed only if the attribute actualTypeOfUP indicates that the UP type is HW_SENSITIVE and if the UP type in all referring CVs are of type HW_SENSITIVE."), ACTION_ALLOWED_ONLY_FOR_CURRENT_UP(
            32, "The action is only allowed for current (active) UP."), REMOVED_LM_FILES(33, "The load module files that have been removed."), REMOVED_LM_FILES_XML_FILE(34,
            "An XML file that contains information of load module files that have been removed."), REFERRING_CV_INFO(35, "Information about the CVs, where this UpgradePackage MO exists."), UCF_VALIDATION_ERROR(
            36, "There is syntax or format problem detected in the UCF."), INIT_UP_ERROR(37,
            "Initialitation of the Upgrade Package Mo failed i.e. the setting of an attribute in the UpgradePackageMo with data specfied in the UCF failed."), AUE_REPORTED_FAILURE(38,
            "An AUE has reported failure or has not answered within an expected time."), AUE_REPORTED_WARNING(39, "An AUE has reported warning."), LACK_OF_DISK_SPACE_ON_C_DISK(40,
            "There is not enough with disk space for the required Load Module files to be installed."), NO_NODE_TYPE_LIST_FOUND_THAT_MATCHES_USED_PIUS(41,
            "Not all used PIUs in the node are supported by a specific node type according to the Upgrade Control File."), DISK_HEALTH_CHECK_FAILURE(42, "Error(s) detected during disk health check."), // QUES: Check the message
    MISMATCHED_LM_PATH(43, "The LM file path in the UCF is not matches the one configured in the node."), REPERTOIRE_REPLACEMENT_ERROR_DETECTED(44,
            "Repertoire replacement definitions in the ExplicitReplacment UCF are not consistent i.e. depending on selected upgrade action."), CERTIFICATE_OR_SIGNATURE_PROBLEM(45,
            "Certificate or signature problem detected for at least one load module."), UCF_DATA_NOT_SUPPORTED(46, "The specified data in the UCF is not supported."), C_AUE_REPERTOIRE_CONNECTED_TO_SLOT(
            47, "A C AUE Repertoire MO is connected to a Slot MO, which is not allowed at execution of an upgrade."), PROGRAM_AND_LOADMODULE_INCONSISTENCY(48,
            "One or more Program MOs refer to a LoadModule MO that has been removed."), UCF_CHECKSUM_VER_FAILED(49, "A UCF checksum verification has failed."), JDBC_CONNECTION_NOT_OK(51,
                    "Not possible to access the database using the JDBC interface."), RESTART_COUNTER_LIMIT_INCONSISTENT(52, "The result from combining UCF data for the values of the restart-counter limit with the way that repertoires are connected to an upgradeable PIU, is inconsistent."),
    RESTART_TIMER_INCONSISTENT(53, "The result from combining UCF data for the values of the restart timer with the way that repertoires are connected to an upgradeable PIU, is inconsistent."), JVM_RESTART_FROM_STATE(54, "JVM restart will be executed in from state."), JAVA_HEAP_SIZE_INCONSISTENT(55, "The specification of the Java heap size in the UCF is not consistent. The heap size must not be specified both in UpgradeSequence and within the Repertoire elements in the UCF."), 
    HARDWARE_MISMATCH_FOR_C_AUE_BOARDS(56, "Hardware mismatch is found on the boards where C-AUEs are configured to run.Auto-configuration has to be done before continuing with upgrade.");

    private int infoId;
    private String infoMessage;

    private ActionResultInformation(final int infoId, final String infoMessage) {
        this.infoId = infoId;
        this.infoMessage = infoMessage;
    }

    public int getInfoId() {
        return infoId;
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public static ActionResultInformation getActionResultInfo(final String actionResultInfo) {

        for (final ActionResultInformation s : ActionResultInformation.values()) {
            if (s.name().equalsIgnoreCase(actionResultInfo)) {
                return s;
            }
        }
        return ActionResultInformation.UNKNOWN;
    }

}
