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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

/**
 * This class holds NE job status
 * 
 * @author xgudpra
 * 
 */
public class NeJobData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String networkElement;

    private String neResult;

    private String lastLogMessage;

    /**
     * @return the lastLogMessage on selected network element
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
     * @return the networkElementName
     */
    public String getNetworkElement() {
        return networkElement;
    }

    /**
     * @param networkElement
     *            the networkElement to set
     */
    public void setNetworkElement(final String networkElement) {
        this.networkElement = networkElement;
    }

    /**
     * @return the neResult
     */
    public String getNeResult() {
        return neResult;
    }

    /**
     * @param neResult
     *            the neResult to set
     */
    public void setNeResult(final String neResult) {
        this.neResult = neResult;
    }

    @Override
    public String toString() {
        return "NeJobDetails [networkElement=" + networkElement + ", neResult=" + neResult + ", lastLogMessage=" + lastLogMessage + "]";
    }

}
