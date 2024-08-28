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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;

public class ReportLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private int totalCount;
    private List<JobLogResponse> result;
    private List<Map<String, String>> columns;
    private boolean clearOffset;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    public List<JobLogResponse> getResult() {
        return result;
    }

    public void setResult(final List<JobLogResponse> result) {
        this.result = result;
    }

    public List<Map<String, String>> getColumns() {
        return columns;
    }

    public void setColumns(final List<Map<String, String>> columns) {
        this.columns = columns;
    }

    public boolean isClearOffset() {
        return clearOffset;
    }

    public void setClearOffset(final boolean isClearOffsetRequired) {
        this.clearOffset = isClearOffsetRequired;
    }

}
