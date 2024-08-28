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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade;

public enum OPSScriptExecStatus {
    RUNNING("RUNNING"), FINISHED("FINISHED"), STOPPED("STOPPED"), FAILED("FAILED"), NOT_STARTED("NOT_STARTED"), WAITING_FOR_INPUT("WAITING_FOR_INPUT"), INTERRUPTED("INTERRUPTED"), INVALID_STATUS(
            "INVALID_STATUS");

    private String status;

    private OPSScriptExecStatus(final String status) {
        this.status = status;
    }

    public static OPSScriptExecStatus getStatusName(final String status) {
        for (OPSScriptExecStatus state : OPSScriptExecStatus.values()) {
            if (state.status.equals(status)) {
                return state;
            }
        }

        return OPSScriptExecStatus.INVALID_STATUS;
    }

}
