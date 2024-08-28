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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

public enum LmActivationState {

    INACTIVE("grace period is inactive"), ACTIVATED("grace period is activated"), ACTIVATED_EXPIRING("grace period is about to expire"), EXPIRED("grace period has expired");

    private String activationState;

    public String getActivityMessage() {
        return activationState;
    }

    private LmActivationState(final String activationState) {
        this.activationState = activationState;
    }

    public static LmActivationState getActivationState(final String emergencyUnlockState) {

        for (final LmActivationState s : LmActivationState.values()) {
            if (s.name().equalsIgnoreCase(emergencyUnlockState)) {
                return s;
            }
        }
        return null;
    }
}
