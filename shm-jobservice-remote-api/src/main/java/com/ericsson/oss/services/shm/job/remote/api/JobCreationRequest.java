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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobservice.common.NeJobProperty;
import com.ericsson.oss.services.shm.jobservice.common.NeTypeJobProperty;
import com.ericsson.oss.services.shm.jobservice.common.PlatformProperty;

@SuppressWarnings("PMD.TooManyFields")
public class JobCreationRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private String description;
    private JobTypeEnum jobType;
    private String name;
    private String owner;
    private List<JobRemoteconfigurations> configurations;
    private Map<String, String> packageNames;
    private MainSchedule mainSchedule;
    private ActivitySchedule[] activitySchedule;
    private List<String> collectionNames = new ArrayList<>();
    private List<String> fdns = new ArrayList<>();
    private List<NeNames> neNames = new ArrayList<>();
    private Date creationTime = null;
    private List<Map<String, String>> jobProperties = new ArrayList<>();
    private List<PlatformProperty> platformJobProperties = new ArrayList<>();
    private List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<>();
    private List<NeJobProperty> neJobProperties = new ArrayList<>();
    private List<String> savedSearchIds = new ArrayList<>();

    private JobCategory jobCategory = JobCategory.NHC_UI;

    public void setJobType(final JobTypeEnum jobType) {
        this.jobType = jobType;
    }

    public JobTypeEnum getJobType() {
        return jobType;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setcollectionNames(final List<String> collectionNames) {
        this.collectionNames = collectionNames;
    }

    public List<String> getcollectionNames() {
        return collectionNames;
    }

    public void setFdns(final List<String> fdns) {
        this.fdns = fdns;
    }

    public List<String> getFdns() {
        return fdns;
    }

    public void setNeNames(final List<NeNames> neNames) {
        this.neNames = neNames;
    }

    public List<NeNames> getNeNames() {
        return neNames;
    }

    public void setPackageNames(final Map<String, String> packageNames) {
        this.packageNames = packageNames;
    }

    public Map<String, String> getPackageNames() {
        return packageNames;
    }

    public void setActivitySchedules(final ActivitySchedule[] activitySchedules) {
        this.activitySchedule = activitySchedules;
    }

    public ActivitySchedule[] getActivitySchedules() {
        return activitySchedule;
    }

    public void setMainSchedule(final MainSchedule mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

    public MainSchedule getMainSchedule() {
        return mainSchedule;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setCreationTime(final Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public List<Map<String, String>> getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(final List<Map<String, String>> jobProperties) {
        this.jobProperties = jobProperties;
    }

    public void addJobProperty(final Map<String, String> jobProperty) {
        this.jobProperties.add(jobProperty);
    }

    public List<PlatformProperty> getPlatformJobProperties() {
        return platformJobProperties;
    }

    public void setPlatformJobProperties(final List<PlatformProperty> platformJobProperties) {
        this.platformJobProperties = platformJobProperties;
    }

    public void addPlatformJobProperty(final PlatformProperty platformJobProperty) {
        this.platformJobProperties.add(platformJobProperty);
    }

    public List<NeTypeJobProperty> getNETypeJobProperties() {
        return neTypeJobProperties;
    }

    public void setNETypeJobProperties(final List<NeTypeJobProperty> neTypeJobProperties) {
        this.neTypeJobProperties = neTypeJobProperties;
    }

    public void addNETypeJobProperty(final NeTypeJobProperty neTypejobProperty) {
        this.neTypeJobProperties.add(neTypejobProperty);
    }

    public List<NeJobProperty> getNeJobProperties() {
        return neJobProperties;
    }

    public void setNeJobProperties(final List<NeJobProperty> neJobProperties) {
        this.neJobProperties = neJobProperties;
    }

    public void addNeJobProperties(final NeJobProperty neJobProperties) {
        this.neJobProperties.add(neJobProperties);
    }

    /**
     * @return the configurations
     */
    public List<JobRemoteconfigurations> getConfigurations() {
        return configurations;
    }

    /**
     * @param configurations
     *            the configurations to set
     */
    public void setConfigurations(final List<JobRemoteconfigurations> configurations) {
        this.configurations = configurations;
    }

    /**
     * @return the savedSearchIds
     */
    public List<String> getSavedSearchIds() {
        return savedSearchIds;
    }

    /**
     * @param savedSearchIds
     *            the savedSearchIds to set
     */
    public void setSavedSearchIds(final List<String> savedSearchIds) {
        this.savedSearchIds = savedSearchIds;
    }

    /**
     * @return the jobCategory
     */
    public JobCategory getJobCategory() {
        return jobCategory;
    }

    /**
     * @param jobCategory
     *            the jobCategory to set
     */
    public void setJobCategory(final JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }

    /**
     * Overriding toString() of this object, to print the object data.
     * 
     * @return string
     */
    @Override
    public String toString() {
        return "JobInfo [description=" + this.description + ", jobType=" + this.jobType + ", name=" + this.name + ", owner=" + this.owner + ", configurations=" + this.configurations
                + ", packageNames=" + this.packageNames + ", mainSchedule=" + this.mainSchedule + ", activitySchedules=" + this.activitySchedule + ", collectionNames=" + this.collectionNames
                + ", fdns=" + this.fdns + ", neNames=" + this.neNames + ", creationTime=" + this.creationTime + ", jobProperties=" + this.jobProperties + ", platformJobProperties="
                + this.platformJobProperties + ", neTypeJobProperties=" + this.neTypeJobProperties + ", neJobProperties=" + this.neJobProperties + ", savedSearchIds=" + this.savedSearchIds
                + ", jobCategory=" + this.jobCategory + "]";
    }

}
