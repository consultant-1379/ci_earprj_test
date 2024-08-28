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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;
import java.util.Map;

/**
 * This class holds activity job properties per netype which has activity name along with their parameters
 * 
 * @author xaniama
 * 
 */

public class NeTypeActivityJobProperties {

    private String neType;
    private List<Map<String, Object>> activityJobProperties;

    /**
     * @return the activityJobProperty
     */
    public List<Map<String, Object>> getActivityJobProperties() {
        return activityJobProperties;
    }

    /**
     * @param allActivitiesPerNetype
     *            the activityJobProperty to set
     */
    public void setActivityJobProperties(final List<Map<String, Object>> allActivitiesPerNetype) {
        this.activityJobProperties = allActivitiesPerNetype;
    }

    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }

    /**
     * @param neType
     *            the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }

}
