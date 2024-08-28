/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.entities;

import java.util.Date;

public class SHMMainJob {

    private final long jobId;
    private final long jobTemplateId;
    private final String jobName;
    private final String createdBy;
    private final String totalNoOfNEs;
    private final Boolean periodic;
    private final String jobType;
    private final String status;
    private final Double progress;
    private final String result;
    private final Date startTime;
    private final Date endTime;

    public SHMMainJob(final SHMMainJobDto mainJobDto) {
        this.jobId = mainJobDto.getJobId();
        this.jobTemplateId = mainJobDto.getJobTemplateId();
        this.jobName = mainJobDto.getJobName();
        this.createdBy = mainJobDto.getCreatedBy();
        this.totalNoOfNEs = mainJobDto.getTotalNoOfNEs();
        this.periodic = mainJobDto.getPeriodic();
        this.jobType = mainJobDto.getJobType();
        this.status = mainJobDto.getStatus();
        this.progress = mainJobDto.getProgress();
        this.result = mainJobDto.getResult();
        this.startTime = mainJobDto.getStartTime();
        this.endTime = mainJobDto.getEndTime();
    }

    public long getJobId() {
        return jobId;
    }

    public long getJobTemplateId() {
        return jobTemplateId;
    }

    public String getJobIdAsLong() {
        return String.valueOf(jobId);
    }

    public String getJobTemplateIdAsLong() {
        return String.valueOf(jobTemplateId);
    }

    public String getJobName() {
        return jobName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getTotalNoOfNEs() {
        return totalNoOfNEs;
    }

    public Boolean getPeriodic() {
        return periodic;
    }

    public String getJobType() {
        return jobType;
    }

    public String getStatus() {
        return status;
    }

    public Double getProgress() {
        return progress;
    }

    public String getResult() {
        return result;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

}
