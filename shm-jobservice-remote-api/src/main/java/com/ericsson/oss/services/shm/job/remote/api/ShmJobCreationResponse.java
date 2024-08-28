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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;

/**
 * Shm job creation response data to external remote services
 * 
 * @author tcsmaup
 * 
 */
public class ShmJobCreationResponse implements Serializable {

    private static final long serialVersionUID = -795737860200480122L;

    private ShmJobResponseResult shmJobResponseResult;

    private String jobName;

    private JobCreationResponseCode jobCreationResponseCode;

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName
     *            the jobName to set
     */
    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    /**
     * @return the backupJobCreationResponseCode
     */
    public JobCreationResponseCode getJobCreationResponseCode() {
        return jobCreationResponseCode;
    }

    /**
     * @param jobCreationResponseCode the backupJobCreationResponseCode to set
     */
    public void setJobCreationResponseCode(final JobCreationResponseCode jobCreationResponseCode) {
        this.jobCreationResponseCode = jobCreationResponseCode;
    }

    /**
     * @return the shmJobResponseResult
     */
    public ShmJobResponseResult getShmJobResponseResult() {
        return shmJobResponseResult;
    }

    /**
     * @param shmJobResponseResult the shmJobResponseResult to set
     */
    public void setShmJobResponseResult(final ShmJobResponseResult shmJobResponseResult) {
        this.shmJobResponseResult = shmJobResponseResult;
    }

}
