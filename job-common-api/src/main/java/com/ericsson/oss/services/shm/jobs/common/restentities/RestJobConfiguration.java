package com.ericsson.oss.services.shm.jobs.common.restentities;

import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.modelentities.SelectedNEInfo;

public class RestJobConfiguration {
    private String jobName;
    private String description;
    private String createdOn;
    private String jobType;
    private String startTime;
    private String mode;
    private List<JobParam> jobParams;
    private SelectedNEInfo selectedNEs;
    private String owner;
    private ScheduleJobConfiguration scheduleJobConfiguration;

    public RestJobConfiguration() {

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
    public RestJobConfiguration(final String jobName, final String description, final String createdOn, final String jobType, final String startTime, final String mode,
            final List<JobParam> jobParams, final String owner, final SelectedNEInfo selectedNEs, final ScheduleJobConfiguration scheduleJobConfiguration) {
        super();
        this.jobName = jobName;
        this.description = description;
        this.createdOn = createdOn;
        this.jobType = jobType;
        this.startTime = startTime;
        this.mode = mode;
        this.jobParams = jobParams;
        this.owner = owner;
        this.selectedNEs = selectedNEs;
        this.scheduleJobConfiguration = scheduleJobConfiguration;
    }

    /**
     * @return the jobParams
     */
    public List<JobParam> getJobParams() {
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
                + ", jobParams=" + jobParams + ", selectedNEs=" + selectedNEs + ", owner=" + owner + ", scheduleJobConfiguration=" + scheduleJobConfiguration + "]";
    }

}
