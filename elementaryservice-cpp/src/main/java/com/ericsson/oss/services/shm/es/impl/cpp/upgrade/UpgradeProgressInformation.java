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

public enum UpgradeProgressInformation {

    UNKNOWN(-1,"Unknown Upgrade Progress Header Information"), IDLE(0, "no action is in progress"), DOWNLOADING_FILES(1, "downloading of files (load modules) in progress."), SAVING_CV(2, "saving a Configuration Version. "), RECONFIGURE_MOS(3,
            "reconfiguring MOs according to the UCF."), INITIATE_LOADER_INFO(4, "executing trigger Initiate."), PRELOADING(5, "executing trigger Preload."), CONV_OF_PERSISTENT_DATA(6,
            "executing trigger Convert."), APPL_SPECIFIC_ACTION(7, "executing an application unique trigger."), NODE_RESTART_REQ(8, "the node is to be restarted"), TAKE_NEW_SW_INTO_SERVICE(9,
            "executing trigger Execute."), FINISH_AND_CLEAN_UP(10, "executing trigger Finish."), ENTER_NORMAL_MODE(11, "the execution mode is switched back to normal."), CANCEL_OF_INST_IS_EXECUTING(
            12, "cancellation of an ongoing installation is in progress."), CANCEL_OF_INST_FAILED(13, "execution of the cancelInstall action failed."), VERIFICATION_INITIATED(14,
            "the verification phase has been initiated."), VERIFY_UPGR_FROM_VER(15, "verifying that the upgrade Window defined in the UCF allows an upgrade."), VERIFY_CREATE_REQ_CVS(16,
            "verifying that it is possible to create the required number of CVs during the upgrade phase."), VERIFY_PIUS_SUPPORTED(17,
            "verifying that the used plug-in-units in the node are supported according to the UCF."), VERIFY_CHECKSUM_FOR_LM(18,
            "verifying the checksum for all load modules that has a checksum value defined in the UCF."), VERIFY_PIUS_NOT_FAULTY(19,
            "verifying that plug-in-units on the node are not faulty before the upgrade is initiated."), EXECUTION_FAILED(20, "Execution of an install or an upgrade action has failed."), VERIFICATION_FINISHED(
            21, "the verification phase is finished."), VERIFICATION_FAILED(22, "The verification phase has failed."), JVM_RESTART_REQ(23, "the JVM is to be restarted."), ENTER_UPGRADE_MODE(24,
            "the execution mode is switched to upgrade."), PIU_UPGRADE_REQ(25, "one or several plug-in-units are to be upgraded."), SOFT_PIU_UPGRADE_REQ(26,
            "a number of programs are started, stopped or upgraded."), PROGRAM_ADD_REMOVE(27, "one or several programs are added and/or removed."), NODE_UPGRADE_REQ(28, "node upgrade requested."), CONF_NORMAL_WORKING_STATE(
            29, "executing trigger Confirmed."), HANDLING_FINAL_CV_FAILED(30, "handling of final configuration version failed"), WAIT_FOR_CONF_UPGRADE(31, "waiting for confirmation of the upgrade."), UPGRADE_EXECUTED(
            32, "upgrade has been executed successfully."), UPGRADE_REQUESTED(33, "variant of upgrade action initiated "), AUE_PROGRESS(34,
            "progress indication from an AUE that is part of the upgrade."), AUE_FAILURE(36, "An AUE has reported failure or has not answered within an expected time."), AUE_CONF_WITH_WARNING(37,
            "an AUE has confirmed with warning."), VERIFY_INIT_CONF_PIUS(38,
            "verifying that initial configured PIUs (with programs) are supported by the Upgrade Package (in UCF) and that all load modules referenced in programs are valid."), VERIFY_JVM_PIUS(39,
            "verifying that the JVM MP(s) is operational."), VERIFY_FTC(40, "verifying the Fault Tolerant Core (FTC) states."), VERIFY_LM_FILES(41,
            "verifying that all required load module files are installed for the supported plug-in-units according to the UCF."), VERIFY_SUBRACK_TYPE(42,
            "verifying that the subracks in the node are supported according to the UCF."), VERIFY_PIUTYPE_UNIQUE(43,
            "verifying that SwAllocations connected to the same slot do not define the same PiuType."), VERIFYING_SLOTS_OF_PIUS_CONNECTED_TO_SWA(44,
            "verifying that Slots related to plug-in-units are connected to an SWA."), VERIFYING_LM_TYPE_ALLOWED(45,
            "verifying that only allowed types of load modules are referenced in Repertoires to be connected to ManagedObject(s) via an SWA."), VERIFY_PIUTYPE_LOADLIST(46,
            "verifying that all Repertoires in a SwAllocation support the same set of PiuTypes."), VERIFY_PROGRAM_CONSISTENCY(
            47,
            "verifying that programs that will exist in TO state are specified in a consistent manner, that is the result of combining UCF data with how repertoires are connected to an upgradeable PIU must be consistent. "), SHRINK_SUCCESSFULLY_EXECUTED(
            48, "shrink has been executed successfully."), SHRINK_INITIATED(49, "execution of shrink has been initiated."), IDENTIFYING_NOT_REQUIRED_LM_FILES(50,
            "identifying not required load module files."), REMOVING_NOT_REQUIRED_LM_FILES(51, "removing not required load module files."), READREFERRINGCVINFORMATION_INITIATED(52,
            "read of referring CV information initiated"), VALIDATION_OF_UCF(53, "the contents of the UCF is validated."), INITIATION_OF_UP(54,
            "attributes in the UP MO are set with values from the UCF."), CALCULATION_OF_LM_FILES(55, "the number of load modules to be installed are identified."), PRE_UPGRADE_SU(56,
            "the SU function is pre-upgraded."), DOWNGRADE_SU(57, "the SU function is restored (downgraded)."), AUE_LOAD_UNLOAD(58, "dynamic load or unload of Java AUEs."), VERIFY_INSTALL(59,
            "a verify installation trigger has been sent to the participating AUEs before the installation of load module files are started."), VERIFY_UPGRADE(60,
            "a verify upgrade trigger has been sent to the participating AUEs so that they can do their task in sequence with built in pre-checks performed by the SU logic."), VERIFY_HARD_UPGRADE(61,
            "a hard upgrade trigger has been sent to verify a node upgrade sequence."), VERIFY_SOFT_UPGRADE(62, "a soft upgrade trigger has been sent to verify a board upgrade sequence."), VERIFY_UPDATE(
            63, "a verify update trigger has been sent to verify update sequence."), VERIFY_AVAILABLE_DISK_SPACE(64,
            "verifying if there is available disk space on C- and D- disk partition(s) for the load module files to be installed."), VERIFY_NODE_TYPE_LIST(65,
            "verifying that any of the specified node type specific piu type lists in the UCF matches current node configuration with respect to configured Upgradeable Plug In Units."), VERIFY_DISK_HEALTH(
            66, "verifying that the file system, configuration version and/or upgrade packages are NOT corrupt."), VERIFY_MATCHED_LM_PATH(67,
            "verifying that the LM file path in the UCF is matches the one configured in the node."), VERIFY_REPERTOIRE_REPLACEMENT(68,
            "verifying that repertoire replacement definitions in UCF are consistent."), VERIFYING_LM_SM(69, "verifying the certificate and signature load modules defined in the UCF."), VERIFY_UCF_SUPP_BY_NODE(
            70, "verifying whether the UCF contents are supported by the node."), VERIFYING_C_AUE_REPERTOIRES(71, "verifying that no C AUE Repertoire MO is connected to any Slot MO."), VERIFYING_PROGRAM_AND_LOADMODULE_CONSISTENCY(
            72, "verifying that references from Program MOs to LoadModule MOs are consistent."), WAIT_FOR_RESUME_UPGRADE(73, "waiting for resume of the SW upgrade."), UPGRADE_RESUMED(74,
            "the SW Upgrade has been resumed."), VERIFY_CHECKSUM_FOR_UCF(75, "checksum for the UCF is in progress."),
            VERIFYING_C_AUE_CONSISTENCY(76, "Verifying that CAUEs that will be executed during upgrade are specified in a consistent way. That is, the result of combining UCF data with how the LoadModule MOs are configured, is consistent."), 
            VERIFY_JDBC_CONNECTION(77, "Ensure that it is possible to access the database via the JDBC Interface."),
            VERIFYING_NO_JVM_RESTART_FROM_STATE(78, "Verifying that no JVM restart will be executed in from state."), 
            VERIFY_JAVA_HEAP_SIZE_CONSISTENCY(79, "Verifying that the UCF attribute javaHeapSize is specified consistently."),
            VERIFY_HARDWARE_MISMATCH_FOR_C_AUE_BOARDS(80, "Verifying whether there is a hardware mismatch on boards where C-AUEs are configured to run.");

    private int progressId;
    private String progressMessage;

    private UpgradeProgressInformation(final int progressId, final String progressMessage) {
        this.progressId = progressId;
        this.progressMessage = progressMessage;
    }

    public int getProgressId() {
        return progressId;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public static UpgradeProgressInformation getHeader(final String header) {

        for (final UpgradeProgressInformation s : UpgradeProgressInformation.values()) {
            if (s.name().equalsIgnoreCase(header)) {
                return s;
            }
        }
        return UpgradeProgressInformation.UNKNOWN;
    }

}
