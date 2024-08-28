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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.io.Serializable;

public class JobReportData implements Serializable {

    private static final long serialVersionUID = -1731174020445337302L;
    private JobReportDetails jobDetails;
    private NeDetails neDetails;

    public JobReportData() {
    }

    public JobReportData(final JobReportDetails jobDetails, final NeDetails neDetails) {

        this.jobDetails = jobDetails;
        this.neDetails = neDetails;

    }

    public JobReportDetails getJobDetails() {
        return jobDetails;
    }

    public void setJobReportDetails(final JobReportDetails jobDetails) {
        this.jobDetails = jobDetails;
    }

    public NeDetails getNeDetails() {
        return neDetails;
    }

    public void setNeDetails(final NeDetails neDetails) {
        this.neDetails = neDetails;
    }

}
