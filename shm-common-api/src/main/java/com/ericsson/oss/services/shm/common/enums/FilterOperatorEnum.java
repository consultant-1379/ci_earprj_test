/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.common.enums;

public enum FilterOperatorEnum {

    GREATERTHAN(">"), LESSTHAN("<"), EQUALS("="), NOT_EQUALS("!="), CONTAINS("*"), STARTS_WITH("ab*"), ENDS_WITH("*ab");

    private final String attribute;

    private FilterOperatorEnum(final String attribute) {

        this.attribute = attribute;
    }

    public String getAttribute() {
        return attribute;
    }
}
