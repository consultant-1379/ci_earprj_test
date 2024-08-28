/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadBackupPollResponse {

    private int status;
    private String statusMsg;
    private String file;
    private double percentageDone;
    private String retries;
    private String executionTime;

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(final String statusMsg) {
        this.statusMsg = statusMsg;
    }

    public String getFile() {
        return file;
    }

    public void setFile(final String file) {
        this.file = file;
    }

    public double getPercentageDone() {
        return percentageDone;
    }

    public void setPercentageDone(final double percentageDone) {
        this.percentageDone = percentageDone;
    }

    public String getRetries() {
        return retries;
    }

    public void setRetries(final String retries) {
        this.retries = retries;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final String executionTime) {
        this.executionTime = executionTime;
    }

}
