/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
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
 * This enum is used to hold the different states of polling activity request.
 * 
 * @author xsrabop
 */
public enum PollCycleStatus {

    READY("READY"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED");

    private String pollingCycleStatus;

    private PollCycleStatus(final String pollingCycleStatus) {
        this.pollingCycleStatus = pollingCycleStatus;
    }

    public String getPollCycleStatus() {
        return pollingCycleStatus;
    }

}
