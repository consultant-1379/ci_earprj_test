/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

/**
 * This enum is used as identifiers for each activity steps.
 * 
 * @author xarirud
 * 
 */
public enum ActivityStepsEnum {
    PRECHECK("PRECHECK"), EXECUTE("EXECUTE"), PROCESS_NOTIFICATION("PROCESS_NOTIFICATION"), HANDLE_TIMEOUT("HANDLE_TIMEOUT");

    private String step;

    ActivityStepsEnum(final String step) {
        this.step = step;
    }

    public String getStep() {
        return this.step;
    }

}
