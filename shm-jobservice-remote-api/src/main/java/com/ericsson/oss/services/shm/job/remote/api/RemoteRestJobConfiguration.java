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
import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.modelentities.NEJobProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;
import com.ericsson.oss.services.shm.jobs.common.restentities.ScheduleJobConfiguration;

/**
 * As we cannot pass local parameters like NetworkElement and ProductData (directly or indirectly) to a remote business method, defined these attributes and SelectedNEInfoConverter attribute here,
 * which is duplicate of RestJobConfigurationData POJO
 * 
 * @author xkalkil
 *
 */
public class RemoteRestJobConfiguration implements Serializable {
    private static final long serialVersionUID = 1234567L;
    private final String jobName;
    private final String description;
    private final String createdOn;
    private final String jobType;
    private final String startTime;
    private final String mode;
    private final List<JobConfigurationDetails> jobParams;
    private final List<NEJobProperty> neJobProperties;
    private final SelectedNEInfoConverter selectedNEs;
    private final List<String> neNames;
    private final int skippedNeCount;
    private final String owner;
    private final ScheduleJobConfiguration scheduleJobConfiguration;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public RemoteRestJobConfiguration(final String jobName, final String description, final String createdOn, final String jobType, final String startTime, final String mode,
            final List<JobConfigurationDetails> jobParams, final List<NEJobProperty> neJobProperties, final SelectedNEInfoConverter selectedNEs, final List<String> neNames, final int skippedNeCount,
            final String owner, final ScheduleJobConfiguration scheduleJobConfiguration) {
        this.jobName = jobName;
        this.description = description;
        this.createdOn = createdOn;
        this.jobType = jobType;
        this.startTime = startTime;
        this.mode = mode;
        this.jobParams = jobParams;
        this.neJobProperties = neJobProperties;
        this.selectedNEs = selectedNEs;
        this.neNames = neNames;
        this.skippedNeCount = skippedNeCount;
        this.owner = owner;
        this.scheduleJobConfiguration = scheduleJobConfiguration;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getJobName() {
        return jobName;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public String getJobType() {
        return jobType;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getMode() {
        return mode;
    }

    public List<JobConfigurationDetails> getJobParams() {
        return jobParams;
    }

    public List<NEJobProperty> getNeJobProperties() {
        return neJobProperties;
    }

    public SelectedNEInfoConverter getSelectedNEs() {
        return selectedNEs;
    }

    public List<String> getNeNames() {
        return neNames;
    }

    public int getSkippedNeCount() {
        return skippedNeCount;
    }

    public String getOwner() {
        return owner;
    }

    public ScheduleJobConfiguration getScheduleJobConfiguration() {
        return scheduleJobConfiguration;
    }

    @Override
    public String toString() {
        return "RemoteRestJobConfiguration [jobName=" + jobName + ", description=" + description + ", createdOn=" + createdOn + ", jobType=" + jobType + ", startTime=" + startTime + ", mode=" + mode
                + ", jobParams=" + jobParams + ", neJobProperties=" + neJobProperties + ", selectedNEs=" + selectedNEs + ", neNames=" + neNames + ", skippedNeCount=" + skippedNeCount + ", owner="
                + owner + ", scheduleJobConfiguration=" + scheduleJobConfiguration + "]";
    }

}
