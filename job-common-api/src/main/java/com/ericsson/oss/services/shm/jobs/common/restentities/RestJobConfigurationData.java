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
package com.ericsson.oss.services.shm.jobs.common.restentities;

import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.modelentities.NEJobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.SelectedNEInfo;

public class RestJobConfigurationData {
    private String jobName;
    private String description;
    private String createdOn;
    private String jobType;
    private String startTime;
    private String mode;
    private List<JobConfigurationDetails> jobParams;
    private List<NEJobProperty> neJobProperties;
    private SelectedNEInfo selectedNEs;
    private List<String> neNames;
    private int skippedNeCount;
    private String owner;
    private ScheduleJobConfiguration scheduleJobConfiguration;

    public RestJobConfigurationData() {

    }

    /**
     * @param name
     * @param description
     * @param createdOn
     * @param jobType
     * @param startTime
     * @param mode
     * @param jobParams
     * @param scheduleJobConfiguration
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public RestJobConfigurationData(final String jobName, final String description, final String createdOn, final String jobType, final String startTime, final String mode,
            final List<JobConfigurationDetails> jobParams, final List<NEJobProperty> neJobProperties, final String owner, final SelectedNEInfo selectedNEs, final List<String> neNames,
            final int skippedNeCount, final ScheduleJobConfiguration scheduleJobConfiguration) {
        super();
        this.jobName = jobName;
        this.description = description;
        this.createdOn = createdOn;
        this.jobType = jobType;
        this.startTime = startTime;
        this.mode = mode;
        this.jobParams = jobParams;
        this.neJobProperties = neJobProperties;
        this.owner = owner;
        this.selectedNEs = selectedNEs;
        this.neNames = neNames;
        this.skippedNeCount = skippedNeCount;
        this.scheduleJobConfiguration = scheduleJobConfiguration;
    }

    /**
     * @return the jobParams
     */
    public List<JobConfigurationDetails> getJobParams() {
        return jobParams;
    }

    /**
     * @return the name
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the createdOn
     */
    public String getCreatedOn() {
        return createdOn;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @return the startTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * @return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the selectedNEs
     */
    public SelectedNEInfo getSelectedNEs() {
        return selectedNEs;
    }

    /**
     * @return the skippedNeCount
     */
    public int getSkippedNeCount() {
        return skippedNeCount;
    }

    /**
     * @return the scheduleJobConfiguration
     */
    public ScheduleJobConfiguration getScheduleJobConfiguration() {
        return scheduleJobConfiguration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JobConfiguration [jobName=" + jobName + ", description=" + description + ", createdOn=" + createdOn + ", jobType=" + jobType + ", startTime=" + startTime + ", mode=" + mode
                + ", jobParams=" + jobParams + ", selectedNEs=" + selectedNEs + ", skippedNeCount=" + skippedNeCount + ", owner=" + owner + ", scheduleJobConfiguration=" + scheduleJobConfiguration
                + "]";
    }

    /**
     * @return the neJobProperties
     */
    public List<NEJobProperty> getNeJobProperties() {
        return neJobProperties;
    }

    /**
     * @return the neNames
     */
    public List<String> getNeNames() {
        return neNames;
    }

}
