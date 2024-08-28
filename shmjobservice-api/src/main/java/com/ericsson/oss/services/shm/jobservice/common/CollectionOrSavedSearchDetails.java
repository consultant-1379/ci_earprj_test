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
import java.util.List;
import java.util.Map;

public class CollectionOrSavedSearchDetails implements Serializable {

    private static final long serialVersionUID = -3205978276250148638L;

    private Long id;
    private String name;
    private List<Map<String, Object>> objects;

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final Long id) {
        this.id = id;
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
     * @return the objects
     */
    public List<Map<String, Object>> getObjects() {
        return objects;
    }

    /**
     * @param objects
     *            the objects to set
     */
    public void setObjects(final List<Map<String, Object>> objects) {
        this.objects = objects;
    }

}
