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
import java.util.Set;

public class CollectionDetails implements Serializable {
    private static final long serialVersionUID = 4166317322529237975L;

    public static final String FORBIDDEN_ERROR = "accessDenied";
    private String collectionId;
    private String name;
    private Set<String> managedObjectsIDs;
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
     * @return the collectionId
     */
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * @param collectionId
     *            the collectionId to set
     */
    public void setCollectionId(final String collectionId) {
        this.collectionId = collectionId;
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
     * @return the managedObjectsIDs
     */
    public Set<String> getManagedObjectsIDs() {
        return managedObjectsIDs;
    }

    /**
     * @param managedObjectsIDs
     *            the managedObjectsIDs to set
     */
    public void setManagedObjectsIDs(final Set<String> managedObjectsIDs) {
        this.managedObjectsIDs = managedObjectsIDs;
    }

}
