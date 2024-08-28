package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.ericsson.oss.services.shm.common.FilterDetails;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeJobInput {
    private int offset;
    private int limit;
    private String sortBy;
    private String orderBy;
    private List<Long> jobIdsList;
    private List<FilterDetails> filterDetails;
    private boolean selectAll;

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

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(final String orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * @return the jobIdsList
     */
    public List<Long> getJobIdsList() {
        return jobIdsList;
    }

    /**
     * @param jobIdsList
     *            the jobIdsList to set
     */
    public void setJobIdsList(final List<Long> jobIdsList) {
        this.jobIdsList = jobIdsList;
    }

    /**
     * @return the filterDetails
     */
    public List<FilterDetails> getFilterDetails() {
        return filterDetails;
    }

    /**
     * @param filterDetails
     *            the filterDetails to set
     */
    public void setFilterDetails(final List<FilterDetails> filterDetails) {
        this.filterDetails = filterDetails;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(final boolean selectAll) {
        this.selectAll = selectAll;
    }
}
