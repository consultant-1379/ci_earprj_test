/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PollingData implements Serializable {

    private static final long serialVersionUID = 4314389157300348898L;

    private String fragmentType;
    private String moFdn;
    private List<String> moAttributes;
    private Map<String, String> additionalInformation;

    public PollingData(final String fragmentType, final String moFdn, final List<String> moAttributes, final Map<String, String> additionalInformation) {
        this.fragmentType = fragmentType;
        this.moFdn = moFdn;
        this.additionalInformation = additionalInformation;
        this.moAttributes = moAttributes;
    }

    public String getFragmentType() {
        return fragmentType;
    }

    public void setFragmentType(final String fragmentType) {
        this.fragmentType = fragmentType;
    }

    public String getMoFdn() {
        return moFdn;
    }

    public void setMoFdn(final String moFdn) {
        this.moFdn = moFdn;
    }

    public List<String> getMoAttributes() {
        return moAttributes;
    }

    public void setMoAttributes(final List<String> moAttributes) {
        this.moAttributes = moAttributes;
    }

    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(final Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

}
