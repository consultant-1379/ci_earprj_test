/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

import com.ericsson.oss.services.shm.jobservice.common.Property;

public class JobRemoteconfigurations implements Serializable {

    private static final long serialVersionUID = 1L;

    private String neType;

    private Property[] properties;
    
    private Property[] neProperties;

    public String getNeType() {
        return neType;
    }

    public void setNeType(final String neType) {
        this.neType = neType;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(final Property[] properties) {
        this.properties = properties;
    }
    
    public void setNeProperties(final Property[] neProperties) {
        this.neProperties = neProperties;
    }
    
    public Property[] getNeProperties() {
        return neProperties;
    }

}
