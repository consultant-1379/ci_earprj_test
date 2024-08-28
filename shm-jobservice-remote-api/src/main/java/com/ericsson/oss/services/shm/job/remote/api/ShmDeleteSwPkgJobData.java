/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
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
import java.util.List;
import java.util.Map;

/**
 * POJO class to fetch delete software package job configuration data from external services and use it for Delete Software Package job creation
 * 
 * @author xgudpra
 * 
 */
public class ShmDeleteSwPkgJobData extends ShmRemoteJobData implements Serializable {

    private static final long serialVersionUID = 2186316806841577519L;

    private String activity;

    private List<Map<String, Object>> configurations;

    /**
     * @return the configurations
     */
    public List<Map<String, Object>> getConfigurations() {
        return configurations;
    }

    /**
     * @return the activity
     */
    public String getActivity() {
        return activity;
    }

    /**
     * @param activity
     * the activity to set
     */
    public void setActivity(final String activity) {
        this.activity = activity;
    }

    /**
     * @param pkgList
     * the configurations to set
     */
    public void setConfigurations(final List<Map<String, Object>> pkgList) {
        this.configurations = pkgList;
    }

    @Override
    public String toString() {
        return "ShmDeleteSwPkgJobData [activity=" + activity + ", configurations=" + configurations + "]";
    }

}
