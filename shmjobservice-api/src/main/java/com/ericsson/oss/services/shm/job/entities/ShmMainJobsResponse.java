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
package com.ericsson.oss.services.shm.job.entities;

import java.util.List;

import com.ericsson.oss.services.shm.jobs.entities.SHMMainJob;

public class ShmMainJobsResponse {

    private final int totalCount;
    private final List<SHMMainJob> result;
    private final boolean clearOffset;

    public ShmMainJobsResponse(final int totalCount, final List<SHMMainJob> result, final boolean clearOffset) {
        this.totalCount = totalCount;
        this.result = result;
        this.clearOffset = clearOffset;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Object getResult() {
        return result;
    }

    public boolean isClearOffset() {
        return clearOffset;
    }

}
