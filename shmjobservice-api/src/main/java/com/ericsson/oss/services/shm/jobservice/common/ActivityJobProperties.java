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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class holds activity job properties which has activity name along with their parameters to be persisted in Database
 * 
 * @author xaniama
 * 
 */
public class ActivityJobProperties implements Serializable {
    private static final long serialVersionUID = 1234568L;

    private String activityName;
    private List<Map<String, String>> activityProperties;

    /**
     * @return the activityProperties
     */
    public List<Map<String, String>> getActivityProperties() {
        return activityProperties;
    }

    /**
     * @param propList
     *            the activityProperties to set
     */
    public void setActivityProperties(final List<Map<String, String>> propList) {
        this.activityProperties = propList;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName
     *            the activityName to set
     */
    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

}
