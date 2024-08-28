package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;

/**
 * This class holds the ActivityJob PO details for retrieval of job logs.
 * 
 */
public class JobLogDetails {

    private String activityName;
    private List<LogDetails> activityLogs;

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName
     *            the activityName to set
     */
    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    /**
     * @return the activityLogs
     */
    public List<LogDetails> getActivityLogs() {
        return activityLogs;
    }

    /**
     * @param activityLogs
     *            the activityLogs to set
     */
    public void setActivityLogs(final List<LogDetails> activityLogs) {
        this.activityLogs = activityLogs;
    }
}
