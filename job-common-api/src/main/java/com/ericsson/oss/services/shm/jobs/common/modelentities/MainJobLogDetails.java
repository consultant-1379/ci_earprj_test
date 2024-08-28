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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;

public class MainJobLogDetails {
    private List<JobLogDetails> jobLogDetails;

    public List<JobLogDetails> getJobLogDetails() {
        return jobLogDetails;
    }

    public void setJobLogDetails(final List<JobLogDetails> jobLogDetails) {
        this.jobLogDetails = jobLogDetails;
    }

}
