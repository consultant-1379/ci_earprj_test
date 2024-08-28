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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.ericsson.oss.services.shm.common.enums.NetworkElementNamePattern;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * POJO class to fetch job configuration data from external services and use it for job creation
 * 
 * @author tcsmaup
 * 
 */

public class ShmRemoteJobData implements Serializable {

    private static final long serialVersionUID = -1450709668241626244L;

    private JobType jobType;

    private String jobName;

    private String jobDescription;

    private JobCategory jobCategory;

    private Set<String> neNames;

    private Set<String> fdns;

    private String Collection;

    private String savedSearchId;

    private String loggedInUser;

    private Map<String, NetworkElementNamePattern> networkElementSearchScopes;

    private ScheduleData ScheduleData;

    private String execMode;

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName
     *            the jobName to set
     */
    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    /**
     * @return the jobDescription
     */
    public String getJobDescription() {
        return jobDescription;
    }

    /**
     * @param jobDescription
     *            the jobDescription to set
     */
    public void setJobDescription(final String jobDescription) {
        this.jobDescription = jobDescription;
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return Collection;
    }

    /**
     * @param collection
     *            the collection to set
     */
    public void setCollection(final String collection) {
        Collection = collection;
    }

    /**
     * @return the savedSearchId
     */
    public String getSavedSearchId() {
        return savedSearchId;
    }

    /**
     * @param savedSearchId
     *            the savedSearchId to set
     */
    public void setSavedSearchId(final String savedSearchId) {
        this.savedSearchId = savedSearchId;
    }

    /**
     * @return the loggedInUser
     */
    public String getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * @param loggedInUser
     *            the loggedInUser to set
     */
    public void setLoggedInUser(final String loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    /**
     * @return the jobType
     */
    public JobType getJobType() {
        return jobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final JobType jobType) {
        this.jobType = jobType;
    }

    /**
     * @return the jobCategory
     */
    public JobCategory getJobCategory() {
        return jobCategory;
    }

    /**
     * @return the neNames
     */
    public Set<String> getNeNames() {
        return neNames;
    }

    /**
     * @param neNames
     *            the neNames to set
     */
    public void setNeNames(final Set<String> neNames) {
        this.neNames = neNames;
    }

    /**
     * @return the fdns
     */
    public Set<String> getFdns() {
        return fdns;
    }

    /**
     * @param fdns
     *            the fdns to set
     */
    public void setFdns(final Set<String> fdns) {
        this.fdns = fdns;
    }

    /**
     * @param jobCategory
     *            the jobCategory to set
     */
    public void setJobCategory(final JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }

    /**
     * @return the networkElementSearchScopes
     */
    public Map<String, NetworkElementNamePattern> getNetworkElementSearchScopes() {
        return networkElementSearchScopes;
    }

    /**
     * @param networkElementSearchScopes
     *            the networkElementSearchScopes to set
     */
    public void setNetworkElementSearchScopes(final Map<String, NetworkElementNamePattern> networkElementSearchScopes) {
        this.networkElementSearchScopes = networkElementSearchScopes;
    }

    /**
     * @return the scheduleData
     */
    public ScheduleData getScheduleData() {
        return ScheduleData;
    }

    /**
     * @param scheduleData
     *            the scheduleData to set
     */
    public void setScheduleData(final ScheduleData scheduleData) {
        ScheduleData = scheduleData;
    }

    /**
     * @return the execMode
     */
    public String getExecMode() {
        return execMode;
    }

    /**
     * @param execMode
     *            the execMode to set
     */
    public void setExecMode(final String execMode) {
        this.execMode = execMode;
    }

    @Override
    public String toString() {
        return "ShmRemoteJobData [jobType=" + jobType + ", jobName=" + jobName + ", jobDescription=" + jobDescription + ", jobCategory=" + jobCategory + ", neNames=" + neNames + ", fdns=" + fdns
                + ", Collection=" + Collection + ", savedSearchId=" + savedSearchId + ", loggedInUser=" + loggedInUser + ", networkElementSearchScopes=" + networkElementSearchScopes
                + ", ScheduleData=" + ScheduleData + ", execMode=" + execMode + "]";
    }

}
