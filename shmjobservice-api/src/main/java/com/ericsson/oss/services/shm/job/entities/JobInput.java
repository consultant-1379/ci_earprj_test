package com.ericsson.oss.services.shm.job.entities;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobInput {

    private List<String> columns;
    private int offset;
    private int limit;
    private String sortBy;
    private String orderBy;
    private List<FilterDetails> filterDetails;
    private List<String> jobCategory = JobCategory.getShmJobCategories();

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(final List<String> columns) {
        this.columns = columns;
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

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(final String orderBy) {
        this.orderBy = orderBy;
    }

    public List<FilterDetails> getFilterDetails() {
        return filterDetails;
    }

    public void setFilterDetails(final List<FilterDetails> filterDetails) {
        this.filterDetails = filterDetails;
    }

    public List<String> getJobCategory() {
        return jobCategory;
    }

    public void setJobCategory(final List<String> jobCategory) {
        this.jobCategory = jobCategory;
    }

    @Override
    public String toString() {
        return "JobInput [columns=" + columns + ", offset=" + offset + ", limit=" + limit + ", sortBy=" + sortBy + ", orderBy=" + orderBy + ", filterDetails=" + filterDetails + ", jobCategory="
                + jobCategory + "]";
    }

}
