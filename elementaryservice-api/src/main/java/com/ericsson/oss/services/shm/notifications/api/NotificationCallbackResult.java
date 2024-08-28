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
package com.ericsson.oss.services.shm.notifications.api;

import java.io.Serializable;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class NotificationCallbackResult implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3704936223738834918L;

    private String message = "";

    private boolean success = false;

    private boolean completed = false;

    private int actionId;

    /**
     * @return the actionId
     */
    public int getActionId() {
        return actionId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the completed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * @param completed
     *            the completed to set
     */
    public void setCompleted(final boolean completed) {
        this.completed = completed;
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param message
     *            the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * @param success
     *            the success to set
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * @param invokedActionId
     */
    public void setActionId(final int invokedActionId) {
        this.actionId = invokedActionId;
    }

    /**
     * @return
     */
    public boolean isActionTimedOut() {
        return this.message.equalsIgnoreCase(ShmConstants.TIMEOUT) ? true : false;
    }

}
