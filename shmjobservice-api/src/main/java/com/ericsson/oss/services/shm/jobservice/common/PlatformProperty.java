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

public class PlatformProperty implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String platform;
    private List<Map<String, String>> jobProperties;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(final String platform) {
        this.platform = platform;
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
        final List<Map<String, String>> platformJobProperties = this.getJobProperties();
        if (platformJobProperties != null) {
            return "PlatformJobProperty : Platform : " + this.getPlatform() + "; jobType : " + platformJobProperties.toString();
        }
        return "PlatformJobProperty : Platform : " + this.getPlatform() + "; jobType : " + platformJobProperties;
    }
}
