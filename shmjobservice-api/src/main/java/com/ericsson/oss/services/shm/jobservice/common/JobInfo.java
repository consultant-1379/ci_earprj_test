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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeComponentActivityDetails;

@SuppressWarnings("PMD")
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobInfo {
    private String description;
    private JobTypeEnum jobType;
    private String name;
    private String owner;
    private List<Map<String, Object>> configurations;
    private Map<String, String> packageNames;
    private Map<String, Object> mainSchedule;
    private List<Map<String, Object>> activitySchedules;
    private List<String> collectionNames = new ArrayList<String>();
    private List<String> fdns = new ArrayList<String>();
    private List<Map<String, Object>> neNames = new ArrayList<Map<String, Object>>();
    private Date creationTime = null;
    private List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
    private List<PlatformProperty> platformJobProperties = new ArrayList<PlatformProperty>();
    private List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<NeTypeJobProperty>();
    private List<NeJobProperty> neJobProperties = new ArrayList<NeJobProperty>();
    private List<NeNamesWithSelectedComponents> parentNeWithComponents = new ArrayList<>();
    private List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails = new ArrayList<>();
    private List<NeTypeActivityJobProperties> neTypeActivityJobProperties = new ArrayList<>();
    private List<String> savedSearchIds = new ArrayList<String>();
    private JobCategory jobCategory = JobCategory.UI;

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

    public void setNeNames(final List<Map<String, Object>> neNames) {
        this.neNames = neNames;
    }

    public List<Map<String, Object>> getNeNames() {
        return neNames;
    }

    public void setPackageNames(final Map<String, String> packageNames) {
        this.packageNames = packageNames;
    }

    public Map<String, String> getPackageNames() {
        return packageNames;
    }

    public void setActivitySchedules(final List<Map<String, Object>> activitySchedules) {
        this.activitySchedules = activitySchedules;
    }

    public List<Map<String, Object>> getActivitySchedules() {
        return activitySchedules;
    }

    public void setMainSchedule(final Map<String, Object> mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

    public Map<String, Object> getMainSchedule() {
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

    public void addNeTypeActivityJobProperties(final NeTypeActivityJobProperties activityProperties) {
        this.neTypeActivityJobProperties.add(activityProperties);
    }

    /**
     * @return the configurations
     */
    public List<Map<String, Object>> getConfigurations() {
        return configurations;
    }

    /**
     * @param configurations
     *            the configurations to set
     */
    public void setConfigurations(final List<Map<String, Object>> configurations) {
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
    public void setJobCategory(JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }

    /**
     * @return the activityProperties
     */
    public List<NeTypeActivityJobProperties> getNeTypeActivityJobProperties() {
        return neTypeActivityJobProperties;
    }

    /**
     * @param activityProperties
     *            the activityProperties to set
     */
    public void setNeTypeActivityJobProperties(List<NeTypeActivityJobProperties> activityProperties) {
        this.neTypeActivityJobProperties = activityProperties;
    }

    public List<NeNamesWithSelectedComponents> getParentNeWithComponents() {
        return parentNeWithComponents;
    }

    /**
     * @param neNamesWithComponents
     *            the neNamesWithComponents to set
     * @param neNamesWithCompo
     */
    public void setParentNeWithComponents(final List<NeNamesWithSelectedComponents> neNamesWithSelectedComponents) {
        this.parentNeWithComponents = neNamesWithSelectedComponents;
    }

    public List<NeTypeComponentActivityDetails> getNeTypeComponentActivityDetails() {
        return neTypeComponentActivityDetails;
    }

    public void setNeTypeComponentActivityDetails(final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails) {
        this.neTypeComponentActivityDetails = neTypeComponentActivityDetails;
    }

    /**
     * Overriding toString() of this object, to print the object data.
     * 
     * @return string
     */
    @Override
    public String toString() {
        return "JobInfo [description=" + this.description + ", jobType=" + this.jobType + ", name=" + this.name + ", owner=" + this.owner + ", configurations=" + this.configurations
                + ", packageNames=" + this.packageNames + ", mainSchedule=" + this.mainSchedule + ", activitySchedules=" + this.activitySchedules + ", collectionNames=" + this.collectionNames
                + ", fdns=" + this.fdns + ", neNames=" + this.neNames + ", creationTime=" + this.creationTime + ", jobProperties=" + this.jobProperties + ", platformJobProperties="
                + this.platformJobProperties + ", neTypeJobProperties=" + this.neTypeJobProperties + ", neJobProperties=" + this.neJobProperties + ", savedSearchIds=" + this.savedSearchIds
                + ", jobCategory=" + this.jobCategory + " parentNeWithComponents " + this.parentNeWithComponents + " ]";
    }

}