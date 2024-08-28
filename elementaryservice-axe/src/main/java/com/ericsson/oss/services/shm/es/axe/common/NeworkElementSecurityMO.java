/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.axe.common;

public class NeworkElementSecurityMO {

    private final String userName;

    private final String paswrd;

    public NeworkElementSecurityMO(final String userName, final String paswrd) {
        this.userName = userName;
        this.paswrd = paswrd;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return paswrd;
    }

}
