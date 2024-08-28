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
package com.ericsson.oss.services.shm.jobservice.common;

import java.util.Map;
import java.util.Set;

public class NeTypesPlatformData {

    private Map<String, Set<String>> supportedNeTypesByPlatforms;
    private Set<String> unsupportedNeTypes;

    /**
     * @return the SupportedNeTypesByPlatforms
     */
    public Map<String, Set<String>> getSupportedNeTypesByPlatforms() {
        return supportedNeTypesByPlatforms;
    }

    /**
     * @param SupportedNeTypesByPlatforms
     *            the SupportedNeTypesByPlatforms to set
     */
    public void setSupportedNeTypesByPlatforms(final Map<String, Set<String>> supportedNeTypesByPlatforms) {
        this.supportedNeTypesByPlatforms = supportedNeTypesByPlatforms;
    }

    /**
     * @return the unsupportedNeTypes
     */
    public Set<String> getUnsupportedNeTypes() {
        return unsupportedNeTypes;
    }

    /**
     * @param unsupportedNeTypes
     *            the unsupportedNeTypes to set
     */
    public void setUnsupportedNeTypes(final Set<String> unsupportedNeTypes) {
        this.unsupportedNeTypes = unsupportedNeTypes;
    }
}
