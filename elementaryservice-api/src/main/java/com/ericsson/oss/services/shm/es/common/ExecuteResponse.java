/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.common;

/**
 * This class holds the status of an action triggered on the node and MO details on which action initiated.
 * 
 * @author tcsgusw
 * 
 */
public class ExecuteResponse {

    private final boolean isActionTriggered;
    private final String fdn;
    private final int actionId;
    private boolean isActionRunning;

    public ExecuteResponse(final boolean isActionTriggered, final String fdn, final int actionId) {
        this.isActionTriggered = isActionTriggered;
        this.fdn = fdn;
        this.actionId = actionId;
    }

    public boolean isActionTriggered() {
        return isActionTriggered;
    }

    public String getFdn() {
        return fdn;
    }

    public int getActionId() {
        return actionId;
    }

    public boolean isActionRunning() {
        return isActionRunning;
    }

    public void setActionRunning(final boolean isActionRunning) {
        this.isActionRunning = isActionRunning;
    }

}
