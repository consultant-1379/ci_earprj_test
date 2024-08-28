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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.Map;

public class ActivityJobDetail extends ActivityDetails {
    private static final long serialVersionUID = 6336368501884208168L;

    private long neJobId;
    private String state;
    private String lastLogMessage;
    private Map<String, Object> log;

    /**
     * @return the log
     */
    public Map<String, Object> getLog() {
        return log;
    }

    /**
     * @param log
     *            the log to set
     */
    public void setLog(final Map<String, Object> log) {
        this.log = log;
    }

    /**
     * @return the lastLogMessage
     */
    public String getLastLogMessage() {
        return lastLogMessage;
    }

    /**
     * @param lastLogMessage
     *            the lastLogMessage to set
     */
    public void setLastLogMessage(final String lastLogMessage) {
        this.lastLogMessage = lastLogMessage;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(final String state) {
        this.state = state;
    }

    /**
     * @return the neJobId
     */
    public long getNeJobId() {
        return neJobId;
    }

    /**
     * @param neJobId
     *            the neJobId to set
     */
    public void setNeJobId(final long neJobId) {
        this.neJobId = neJobId;
    }

    @Override
    public String toString() {
        return "ActivityJobDetail [neJobId=" + neJobId + ", state=" + state + ", lastLogMessage=" + lastLogMessage + ", log=" + log + "]";
    }

}
