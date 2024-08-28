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

public enum UpgradePackageState {

    NOT_INSTALLED(0, "The upgrade package is not installed."), INSTALL_COMPLETED(1, "The upgrade package has been completely installed."), UPGRADE_EXECUTING(2, "Upgrade is executing."), AWAITING_CONFIRMATION(
            3, "The execution of upgrade needs confirmation before it proceeds."), ONLY_DELETEABLE(4, "Only delete is allowed at this stage."), INSTALL_EXECUTING(5,
            "Installation of the UpgradePackage or PiuType MO in progress."), INSTALL_NOT_COMPLETED(6,
            "The upgrade package is not completely installed, that is, only parts of it has been installed. "), UPGRADE_COMPLETED(7, "The upgrade has been successfully executed. "), RESTORE_SU_EXECUTING(
            8, "Restore of the SU function is executing."), VERIFICATION_EXECUTING(9, "Verification of the upgrade package is executing."), WAITING_FOR_RESUME(10,
            "The SW upgrade activity been stopped (paused).");

    private int stateId;
    private String stateMessage;

    private UpgradePackageState(final int stateId, final String stateMessage) {
        this.stateId = stateId;
        this.stateMessage = stateMessage;
    }

    public int getStateId() {
        return stateId;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public static UpgradePackageState getState(final String state) {

        for (final UpgradePackageState s : UpgradePackageState.values()) {
            if (s.name().equalsIgnoreCase(state)) {
                return s;
            }
        }
        return null;
    }

}
