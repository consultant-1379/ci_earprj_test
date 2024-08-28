package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.*;

public class Job {
    private Long jobConfigId;
    private String state;
    private double progressPercentage;
    private String result;
    private Date startTime;
    private Date endTime;
    private List<Map<String, Object>> jobComments;
    private List<JobProperty> jobProperties;
    private Long parentJobId;
    private String activityName;
    private List<JobLog> log;
    private int level;
    private long id;
    private int numberOfNetworkElements;

    /*
     * This method should be used by the services in cppinventorysynchservice
     */
    public Long getJobConfigIdAsLong() {
        return jobConfigId;
    }

    public void setJobConfigId(final Long jobConfigId) {
        this.jobConfigId = jobConfigId;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(final double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
        this.result = result;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(final Date endTime) {
        this.endTime = endTime;
    }

    public List<Map<String, Object>> getComment() {
        return jobComments;
    }

    public void setComment(final List<Map<String, Object>> jobComments) {
        this.jobComments = jobComments;
    }

    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(final List<JobProperty> jobProperties) {
        this.jobProperties = jobProperties;
    }

    public Long getParentJobId() {
        return parentJobId;
    }

    public void setParentJobId(final Long parentJobId) {
        this.parentJobId = parentJobId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    public List<JobLog> getLog() {
        return log;
    }

    public void setLog(final List<JobLog> log) {
        this.log = log;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int level) {
        this.level = level;
    }

    /*
     * This method should be used by the services in cppinventorysynchservice
     */
    public long getIdAsLong() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    /*
     * Because javascript cannot handle long numbers we are converting the long value to string. The Json serializer will pick this method instead and send the long value as a string
     * 
     * Below is the url to the javascript limitation explanation
     * 
     * http://stackoverflow.com/questions/15689790/parse-json-in-javascript-long-numbers-get-rounded
     */
    public String getJobConfigId() {
        return String.valueOf(jobConfigId);
    }

    /*
     * Because javascript cannot handle long numbers we are converting the long value to string. The Json serializer will pick this method instead and send the long value as a string
     * 
     * Below is the url to the javascript limitation explanation
     * 
     * http://stackoverflow.com/questions/15689790/parse-json-in-javascript-long-numbers-get-rounded
     */
    public String getId() {
        return String.valueOf(id);
    }

    /**
     * @return the numberOfNetworkElements
     */
    public int getNumberOfNetworkElements() {
        return numberOfNetworkElements;
    }

    /**
     * @param numberOfNetworkElements
     *            the numberOfNetworkElements to set
     */
    public void setNumberOfNetworkElements(final int numberOfNetworkElements) {
        this.numberOfNetworkElements = numberOfNetworkElements;
    }
}
