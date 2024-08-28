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

package com.ericsson.oss.services.shm.jobservice.common;

import java.io.Serializable;

public class SavedSearchDetails implements Serializable {
    private static final long serialVersionUID = 9221006886078958609L;

    public static final String FORBIDDEN_ERROR = "accessDenied";
    private String savedSearchId;
    private String name;
    private String query;
    private String category;
    private String errorMessage;

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage
     *            the errorMessage to set
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category
     *            the category to set
     */
    public void setCategory(final String category) {
        this.category = category;
    }

    /**
     * @return the savedSearchId
     */
    public String getSavedSearchId() {
        return savedSearchId;
    }

    /**
     * @param savedSearchId
     *            the savedSearchId to set
     */
    public void setSavedSearchId(final String savedSearchId) {
        this.savedSearchId = savedSearchId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query
     *            the query to set
     */
    public void setQuery(final String query) {
        this.query = query;
    }
}
