package com.ericsson.oss.shm.job.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SHMJobData implements Serializable {
    private static final long serialVersionUID = 997519724156290735L;
    private long jobId;
    private long jobTemplateId;
    private String jobName;
    private String jobType;
    private String createdBy;
    private int noOfMEs;
    private double progress;
    private String status;
    private String result;
    private String startDate;
    private String endDate;
    private String creationTime;
    private List<Map<String, Object>> jobComments;
    private boolean periodic;
    private String totalNoOfNEs;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(final String jobType) {
        this.jobType = jobType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public int getNoOfMEs() {
        return noOfMEs;
    }

    public void setNoOfMEs(final int noOfMEs) {
        this.noOfMEs = noOfMEs;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(final double progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
        this.result = result;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(final String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(final String endDate) {
        this.endDate = endDate;
    }

    /*
     * This method should be used by the services in cppinventorysynchservice
     */
    public long getJobTemplateIdAsLong() {
        return jobTemplateId;
    }

    public void setJobTemplateId(final long jobTemplateId) {
        this.jobTemplateId = jobTemplateId;
    }

    /*
     * This method should be used by the services in cppinventorysynchservice
     */
    public long getJobIdAsLong() {
        return jobId;
    }

    public void setJobId(final long jobId) {
        this.jobId = jobId;
    }

    /*
     * Because javascript cannot handle long numbers we are converting the long value to string. The Json serializer will pick this method instead and send the long value as a string
     * 
     * Below is the url to the javascript limitation explanation
     * 
     * http://stackoverflow.com/questions/15689790/parse-json-in-javascript-long- numbers-get-rounded
     */
    public String getJobId() {
        return String.valueOf(jobId);
    }

    /*
     * Because javascript cannot handle long numbers we are converting the long value to string. The Json serializer will pick this method instead and send the long value as a string
     * 
     * Below is the url to the javascript limitation explanation
     * 
     * http://stackoverflow.com/questions/15689790/parse-json-in-javascript-long- numbers-get-rounded
     */
    public String getJobTemplateId() {
        return String.valueOf(jobTemplateId);
    }

    /**
     * @return the creationTime
     */
    public String getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime
     *            the creationTime to set
     */
    public void setCreationTime(final String creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @return the comment
     */
    public List<Map<String, Object>> getComment() {
        return jobComments;
    }

    /**
     * @param jobComment
     *            the comment to set
     */
    public void setComment(final List<Map<String, Object>> jobComments) {
        this.jobComments = jobComments;
    }

    /**
     * @return the periodic
     */
    public boolean isPeriodic() {
        return periodic;
    }

    /**
     * @param periodic
     *            the periodic to set
     */
    public void setPeriodic(final boolean periodic) {
        this.periodic = periodic;
    }

    /**
     * @return the totalNoOfNEs
     */
    public String getTotalNoOfNEs() {
        return totalNoOfNEs;
    }

    /**
     * @param totalNoOfNEs
     *            the totalNoOfNEs to set
     */
    public void setTotalNoOfNEs(final String totalNoOfNEs) {
        this.totalNoOfNEs = totalNoOfNEs;
    }

}
