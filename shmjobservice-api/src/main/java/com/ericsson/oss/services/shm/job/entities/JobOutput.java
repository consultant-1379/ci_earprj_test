package com.ericsson.oss.services.shm.job.entities;

import java.util.List;
import java.util.Map;

public class JobOutput {
    private int totalCount;
    private Object result;
    private List<Map<String, String>> columns;
    private boolean clearOffset;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(final Object result) {
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
