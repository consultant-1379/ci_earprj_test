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
package com.ericsson.oss.services.shm.loadcontrol.constants;

public enum LoadControlCheckState {

    PERMITTED("PERMITTED"), NOTPERMITTED("NOT_PERMITTED"), BYPASSED("BYPASSED");

    private String loadControlCheckState;

    LoadControlCheckState(final String loadControlCheckState) {
        this.loadControlCheckState = loadControlCheckState;
    }

    @Override
    public String toString() {
        return loadControlCheckState;
    }

}
