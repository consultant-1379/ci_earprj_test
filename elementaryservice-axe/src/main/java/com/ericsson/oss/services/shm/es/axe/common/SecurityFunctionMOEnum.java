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

public enum SecurityFunctionMOEnum {

    SECURE_USER_NAME("secureUserName"), SECURE_USER_PASSWORD("secureUserPassword"), USER_NAME("username"), PASSWORD("password"), NE_SECURITY_SUFFIX(
            ",SecurityFunction=1,NetworkElementSecurity=1"), NE_SEQURITY_MO("NetworkElementSecurity"), NE_SEQURITY_MO_NAME_SPACE("OSS_NE_SEC_DEF");

    private String name;

    public String getName() {
        return name;
    }

    SecurityFunctionMOEnum(final String name) {
        this.name = name;
    }
}
