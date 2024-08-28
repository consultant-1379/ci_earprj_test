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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * Class holds response of JobStatus query.
 * 
 * @author xmadupp
 */
public class JobStatusResponse implements Serializable {

    private static final long serialVersionUID = -8922434050556430296L;

    private JobReportData neLevelJobStatus;

    private List<SHMJobData> mainJobStatus;

    public JobStatusResponse(final JobReportData neLevelJobStatus) {
        this.neLevelJobStatus = neLevelJobStatus;
    }

    public JobStatusResponse(final List<SHMJobData> mainJobStatus) {
        this.mainJobStatus = mainJobStatus;
    }

    /**
     * @param neLevelJobStatus
     *            the neLevelJobStatus to set
     */
    public void setNeLevelJobStatus(final JobReportData neLevelJobStatus) {
        this.neLevelJobStatus = neLevelJobStatus;
    }

    /**
     * @return the neLevelJobStatus
     */
    public JobReportData getNeLevelJobStatus() {
        return neLevelJobStatus;
    }

    /**
     * @return the mainJobStatus
     */
    public List<SHMJobData> getMainJobStatus() {
        return mainJobStatus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JobStatusResponse [neLevelJobStatus=" + neLevelJobStatus + ", mainJobStatus=" + mainJobStatus + "]";
    }
}
