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
package com.ericsson.oss.services.shm.job.entities;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.ericsson.oss.services.shm.common.FilterDetails;

/**
 * This class receives the input from UI. Also it defines the setters and getters for variables sent from front end.
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportLogRequest implements Serializable {

    private static final long serialVersionUID = 35626684348568055L;

    // List of NE Job IDs
    private List<Long> neJobIds;

    // Offset is required for pagination
    private int offset;

    // Maximum number of records to be sent in response
    private int limit;

    // Column name for sorting
    private String sortBy;

    // Can be Ascending or Descending
    private OrderByEnum orderBy;

    // To display logs according to loglevel
    private String logLevel;

    // To display all the logs for a Main Report
    private long mainJobId;

    //filtering Report logs.
    private List<FilterDetails> filterDetails;

    public List<Long> getNeJobIds() {
        return neJobIds;
    }

    public void setNeJobIds(final List<Long> neJobIds) {
        this.neJobIds = neJobIds;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(final String sortBy) {
        this.sortBy = sortBy;
    }

    public OrderByEnum getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(final OrderByEnum orderBy) {
        this.orderBy = orderBy;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(final String logLevel) {
        this.logLevel = logLevel;
    }

    public long getMainJobId() {
        return mainJobId;
    }

    public void setMainJobId(final long mainJobId) {
        this.mainJobId = mainJobId;
    }

    public List<FilterDetails> getFilterDetails() {
        return filterDetails;
    }

    public void setFilterDetails(final List<FilterDetails> filterDetails) {
        this.filterDetails = filterDetails;
    }

}
