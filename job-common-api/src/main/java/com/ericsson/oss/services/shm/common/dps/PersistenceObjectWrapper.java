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
package com.ericsson.oss.services.shm.common.dps;

import java.util.Map;

public class PersistenceObjectWrapper {

    private String name;
    private String namespace;
    private String type;
    private String version;
    private String fdn;
    private Map<String, Object> attributesMap;
    private long poId;

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
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace
     *            the namespace to set
     */
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return the version -
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * @return the fdn
     */
    public String getFdn() {
        return fdn;
    }

    /**
     * @param fdn
     *            the fdn to set
     */
    public void setFdn(final String fdn) {
        this.fdn = fdn;
    }

    /**
     * @return the attributesMap
     */
    public Map<String, Object> getAttributesMap() {
        return attributesMap;
    }

    /**
     * @param attributesMap
     *            the attributesMap to set
     */
    public void setAttributesMap(final Map<String, Object> attributesMap) {
        this.attributesMap = attributesMap;
    }

    public long getPoId() {
        return poId;
    }

    public void setPoId(final long poId) {
        this.poId = poId;
    }

}