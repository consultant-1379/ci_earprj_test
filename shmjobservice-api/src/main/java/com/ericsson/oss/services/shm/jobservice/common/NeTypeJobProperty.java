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

public class NeTypeJobProperty implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String neType;
    private List<Map<String, String>> jobProperties;

    public String getNeType() {
        return neType;
    }

    public void setNeType(final String neType) {
        this.neType = neType;
    }

    public List<Map<String, String>> getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(final List<Map<String, String>> jobProperties) {
        this.jobProperties = jobProperties;
    }

    /**
     * Overriding toString() of this object, to print the object data.
     * 
     * @return string
     */
    @Override
    public String toString() {
        final List<Map<String, String>> neTypeJobProperties = this.getJobProperties();
        if (neTypeJobProperties != null) {
            return "NeTypeJobProperty : neType : " + this.getNeType() + "; getJobProperties : " + neTypeJobProperties.toString();
        }
        return "NeTypeJobProperty : neType : " + this.getNeType() + "; getJobProperties : " + neTypeJobProperties;
    }
}
