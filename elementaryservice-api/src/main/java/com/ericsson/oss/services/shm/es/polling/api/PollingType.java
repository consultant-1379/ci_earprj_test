/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling.api;

/**
 * This enum is used to hold the different types of polling activity request.
 * 
 * @author xsrabop
 */
public enum PollingType {

    NODE("NODE"), ENM("ENM");

    private String type;

    private PollingType(final String type) {
        this.type = type;
    }

    public String getPollingType() {
        return type;
    }

}
