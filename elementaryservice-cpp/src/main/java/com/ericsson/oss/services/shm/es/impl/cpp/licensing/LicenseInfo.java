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
package com.ericsson.oss.services.shm.es.impl.cpp.licensing;

public class LicenseInfo {

    private final String fdn;
    private final boolean isPrecheckSuccess;
    private final boolean isPrecheckSkipped;

    /**
     * @return the isPrecheckSkipped
     */
    public boolean isPrecheckSkipped() {
        return isPrecheckSkipped;
    }

    LicenseInfo(final String fdn, final boolean isPrecheckSuccess, final boolean isPrecheckSkipped) {
        this.fdn = fdn;
        this.isPrecheckSuccess = isPrecheckSuccess;
        this.isPrecheckSkipped = isPrecheckSkipped;
    }

    /**
     * @return the fdn
     */
    public String getFdn() {
        return fdn;
    }

    /**
     * @return the isPrecheckSuccess
     */
    public boolean isPrecheckSuccess() {
        return isPrecheckSuccess;
    }
}