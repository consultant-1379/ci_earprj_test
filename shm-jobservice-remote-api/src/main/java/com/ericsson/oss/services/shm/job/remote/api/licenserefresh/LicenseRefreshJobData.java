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
package com.ericsson.oss.services.shm.job.remote.api.licenserefresh;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;

@SuppressWarnings({ "squid:S1948" })
public class LicenseRefreshJobData extends ShmRemoteJobData implements Serializable {

    private static final long serialVersionUID = -1228698857292342632L;

    private List<Map<String, Object>> configurations;

    private List<Map<String, Object>> activitySchedules;

    private Map<String, Object> mainSchedule;

    private List<Map<String, String>> jobProperties;

    public List<Map<String, Object>> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(final List<Map<String, Object>> configurations) {
        this.configurations = configurations;
    }

    public List<Map<String, Object>> getActivitySchedules() {
        return activitySchedules;
    }

    public void setActivitySchedules(final List<Map<String, Object>> activitySchedules) {
        this.activitySchedules = activitySchedules;
    }

    public Map<String, Object> getMainSchedule() {
        return mainSchedule;
    }

    public void setMainSchedule(final Map<String, Object> mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

    public List<Map<String, String>> getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(List<Map<String, String>> jobProperties) {
        this.jobProperties = jobProperties;
    }

    @Override
    public String toString() {
        return "LicenseRefreshJobData [configurations=" + configurations + ", activitySchedules=" + activitySchedules + ", mainSchedule=" + mainSchedule + ", jobProperties=" + jobProperties + "]";
    }
}
