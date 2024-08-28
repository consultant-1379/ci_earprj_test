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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;

public class JobConfiguration {

    private List<JobProperty> jobProperties;
    private NEInfo selectedNEs;
    private Schedule mainSchedule;
    private List<Activity> activities;
    private List<NEJobProperty> neJobProperties;
    private List<NeTypeActivityJobProperties> neTypeActivityJobProperties;
    private List<NeTypeJobProperty> neTypeJobProperties;
    private List<PlatformJobProperty> platformJobProperties;

    /**
     * @return the jobProperties
     */
    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

    /**
     * @param jobProperties
     *            the jobProperties to set
     */
    public void setJobProperties(final List<JobProperty> jobProperties) {
        this.jobProperties = jobProperties;
    }

    /**
     * @return the selectedNEs
     */
    public NEInfo getSelectedNEs() {
        return selectedNEs;
    }

    /**
     * @param selectedNEs
     *            the selectedNEs to set
     */
    public void setSelectedNEs(final NEInfo selectedNEs) {
        this.selectedNEs = selectedNEs;
    }

    /**
     * @return the mainSchedule
     */
    public Schedule getMainSchedule() {
        return mainSchedule;
    }

    /**
     * @param mainSchedule
     *            the mainSchedule to set
     */
    public void setMainSchedule(final Schedule mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

    /**
     * @return the activities
     */
    public List<Activity> getActivities() {
        return activities;
    }

    /**
     * @param activities
     *            the activities to set
     */
    public void setActivities(final List<Activity> activities) {
        this.activities = activities;
    }

    /**
     * @return the neTypeActivityProperties
     */
    public List<NeTypeActivityJobProperties> getNeTypeActivityJobProperties() {
        return neTypeActivityJobProperties;
    }

    /**
     * @param list
     *            the neTypeActivityProperties to set
     */
    public void setNeTypeActivityJobProperties(final List<NeTypeActivityJobProperties> neTypeSwpUpgradeActivityProps) {
        this.neTypeActivityJobProperties = neTypeSwpUpgradeActivityProps;
    }

    /**
     * @return the neJobProperties
     */
    public List<NEJobProperty> getNeJobProperties() {
        return neJobProperties;
    }

    /**
     * @param neJobProperties
     *            the neJobProperties to set
     */
    public void setNeJobProperties(final List<NEJobProperty> neJobProperties) {
        this.neJobProperties = neJobProperties;
    }

    /**
     * @return the neTypeJobProperties
     */
    public List<NeTypeJobProperty> getNeTypeJobProperties() {
        return neTypeJobProperties;
    }

    /**
     * @param neTypeJobProperties
     *            the neTypeJobProperties to set
     */
    public void setNeTypeJobProperties(final List<NeTypeJobProperty> neTypeJobProperties) {
        this.neTypeJobProperties = neTypeJobProperties;
    }

    /**
     * @return the platformJobProperties
     */
    public List<PlatformJobProperty> getPlatformJobProperties() {
        return platformJobProperties;
    }

    /**
     * @param platformJobProperties
     *            the platformJobProperties to set
     */
    public void setPlatformJobProperties(final List<PlatformJobProperty> platformJobProperties) {
        this.platformJobProperties = platformJobProperties;
    }
}
