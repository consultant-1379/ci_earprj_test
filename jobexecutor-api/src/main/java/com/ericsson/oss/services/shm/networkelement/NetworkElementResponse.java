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
package com.ericsson.oss.services.shm.networkelement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

/**
 * This class is used to hold the supported and un-supported NetworkElements information
 * 
 */
public class NetworkElementResponse {

    private List<NetworkElement> supportedNes = new ArrayList<>();
    private Map<NetworkElement, String> unsupportedNes = new HashMap<>();
    private Map<NetworkElement, String> invalidNes = new HashMap<>();
    private Map<String, List<NetworkElement>> nesWithComponents = new HashMap<>();

    /**
     * @return the invalidNes
     */
    public Map<NetworkElement, String> getInvalidNes() {
        return invalidNes;
    }

    /**
     * @param invalidNes
     *            the invalidNes to set
     */
    public void setInvalidNes(final Map<NetworkElement, String> invalidNes) {
        this.invalidNes = invalidNes;
    }

    public List<NetworkElement> getSupportedNes() {
        return supportedNes;
    }

    public void setSupportedNes(final List<NetworkElement> supportedNes) {
        this.supportedNes = supportedNes;
    }

    public Map<NetworkElement, String> getUnsupportedNes() {
        return unsupportedNes;
    }

    public void setUnsupportedNes(final Map<NetworkElement, String> unsupportedNes) {
        this.unsupportedNes = unsupportedNes;
    }

    /**
     * @return the nesWithComponents
     */
    public Map<String, List<NetworkElement>> getNesWithComponents() {
        return nesWithComponents;
    }

    /**
     * @param nesWithComponents
     *            the nesWithComponents to set
     */
    public void setNesWithComponents(final Map<String, List<NetworkElement>> nesWithComponents) {
        this.nesWithComponents = nesWithComponents;
    }

}
