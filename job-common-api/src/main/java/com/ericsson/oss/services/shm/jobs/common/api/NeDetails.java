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
import java.util.List;
import java.util.Map;

public class NeDetails implements Serializable {

    private static final long serialVersionUID = -5212495583759926359L;
    private int totalCount;
    private List<NeJobDetails> result;
    private boolean clearOffset;
    private List<Map<String, Object>> neDetailsWithCustomColumns;

    //private List<NeJobDetails> neJobDetailsList;

    /**
     * @return the totalCount
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * @param totalCount the totalCount to set
     */
    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * @return the result
     */
    public List<NeJobDetails> getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(final List<NeJobDetails> result) {
        this.result = result;
    }

    /**
     * @return the clearOffset
     */
    public boolean isClearOffset() {
        return clearOffset;
    }

    /**
     * @param clearOffset the clearOffset to set
     */
    public void setClearOffset(final boolean clearOffset) {
        this.clearOffset = clearOffset;
    }

    public List<Map<String, Object>> getNeDetailsWithCustomColumns() {
        return neDetailsWithCustomColumns;
    }

    public void setNeDetailsWithCustomColumns(final List<Map<String, Object>> neDetailsWithCustomColumns) {
        this.neDetailsWithCustomColumns = neDetailsWithCustomColumns;
    }
}
