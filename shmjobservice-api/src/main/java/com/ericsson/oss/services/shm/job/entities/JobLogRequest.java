package com.ericsson.oss.services.shm.job.entities;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.ericsson.oss.services.shm.common.FilterDetails;

/**
 * This class receives the input from UI. Also it defines the setters and getters for variables sent from front end.
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobLogRequest {

    // Comma separated NE Job IDs
    private String neJobIds;

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

    // To display all the logs for a Main Job
    private long mainJobId;

    //filtering joblogs.
    private List<FilterDetails> filterDetails;

    /**
     * @returns the loglevel
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * @param loglevel
     *            the loglevel to set
     */
    public void setLogLevel(final String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * @return the neJobIds
     */
    public String getNeJobIds() {
        return neJobIds;
    }

    /**
     * @param neJobIds
     *            the neJobIds to set
     */
    public void setNeJobIds(final String neJobIds) {
        this.neJobIds = neJobIds;
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset
     *            the offset to set
     */
    public void setOffset(final int offset) {
        this.offset = offset;
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit
     *            the limit to set
     */
    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * @return the sortBy
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * @param sortBy
     *            the sortBy to set
     */
    public void setSortBy(final String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * @return the orderBy
     */
    public OrderByEnum getOrderBy() {
        return orderBy;
    }

    /**
     * @param orderBy
     *            the orderBy to set
     */
    public void setOrderBy(final OrderByEnum orderBy) {
        this.orderBy = orderBy;
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
