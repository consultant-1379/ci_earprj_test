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
package com.ericsson.oss.services.shm.job.remote.api.license;

import java.io.Serializable;
import java.util.Map;

import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;

public class InstallLicenseJobData extends ShmRemoteJobData implements Serializable {

    private static final long serialVersionUID = 4156771430262537655L;

    private String activity;
    private Map<String, LicenseJobData> licenseJobData;

    /**
     * @return the licenseJobData
     */
    public Map<String, LicenseJobData> getlicenseJobData() {
        return licenseJobData;
    }

    /**
     * @param licenseJobData
     *            the licenseJobData to set
     */
    public void setlicenseJobData(final Map<String, LicenseJobData> licenseJobData) {
        this.licenseJobData = licenseJobData;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return the activity
     */
    public String getActivity() {
        return activity;
    }

    /**
     * @param activity
     *            the activity to set
     */
    public void setActivity(final String activity) {
        this.activity = activity;
    }

    @Override
    public String toString() {
        return "InstallLicenseJobData [ activity=" + activity + ", getJobName()=" + getJobName() + ", getJobDescription()=" + getJobDescription() + ", getCollection()=" + getCollection()
                + ", getSavedSearchId()=" + getSavedSearchId() + ", getLoggedInUser()=" + getLoggedInUser() + ", getJobType()=" + getJobType() + ", getJobCategory()=" + getJobCategory()
                + ", getNeNames()=" + getNeNames() + ", getFdns()=" + getFdns() + ", getNetworkElementSearchScopes()=" + getNetworkElementSearchScopes() + ", getScheduleData()=" + getScheduleData()
                + ", getExecMode()=" + getExecMode() + ", toString()=" + super.toString() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode() + "]";
    }

}
