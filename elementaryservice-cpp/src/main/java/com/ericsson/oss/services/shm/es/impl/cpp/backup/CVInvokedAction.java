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

public enum CVInvokedAction {

    CONFIRM_RESTORE(0, "Makes the restore permanent."), FORCED_RESTORE(1,
            "Restores the node to a previously downloaded backup CV irrespectively of warnings are detected or not during the verification phase."), GET_FROM_FTP_SERVER(2,
            "Downloads a remote backup of a CV to the node."), PUT_TO_FTP_SERVER(3, "Creates a backup of a CV existing on the node and uploads it to an FTP server."), RESTORE(4,
            "Restores the node to a previously downloaded backup CV."), VERIFY_RESTORE(5, "Verifies that restore of a downloaded backup CV is possible. "), ACTIVATE_ROBUST_RECONFIGURATION(6,
            "Activates the robust reconfiguration function."), DEACTIVATE_ROBUST_RECONFIGURATION(7, "Deactivates the robust reconfiguration function.");

    private int invokedActionId;
    private String invokedActionMessage;

    private CVInvokedAction(final int invokedActionId, final String invokedActionMessage) {
        this.invokedActionId = invokedActionId;
        this.invokedActionMessage = invokedActionMessage;
    }

    public int getInvokedActionId() {
        return invokedActionId;
    }

    public String getInvokedActionMessage() {
        return invokedActionMessage;
    }

    public static CVInvokedAction getInvokedAction(final String invokedAction) {
        for (final CVInvokedAction s : CVInvokedAction.values()) {
            if (s.name().equalsIgnoreCase(invokedAction)) {
                return s;
            }
        }
        return null;
    }

}
