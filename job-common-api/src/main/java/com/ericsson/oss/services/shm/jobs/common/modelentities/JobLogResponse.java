package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.io.Serializable;

/**
 * This class holds the output which is sent to UI as a response
 * 
 */
public class JobLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String neName;
    private String activityName;
    private String entryTime;
    private String message;
    private String error;
    private String logLevel;
    private String nodeType;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JobLogResponse [neName=" + neName + ", nodeType=" + nodeType + ", activityName=" + activityName + ", entryTime=" + entryTime + ", message=" + message + ", error=" + error
                + ", logLevel=" + logLevel + ", ]";
    }

    /**
     * @return the neName
     */
    public String getNeName() {
        return neName;
    }

    /**
     * @param neName
     *            the neName to set
     */
    public void setNeName(final String neName) {
        this.neName = neName;
    }

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
     * @return the entryTime
     */
    public String getEntryTime() {
        return entryTime;
    }

    /**
     * @param entryTime
     *            the entryTime to set
     */
    public void setEntryTime(final String entryTime) {
        this.entryTime = entryTime;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     *            the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * @param error
     *            the error to set
     */
    public void setError(final String error) {
        this.error = error;
    }

    /**
     * @return the logLevel
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * @param logLevel
     *            the logLevel to set
     */
    public void setLogLevel(final String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @param nodeType
     *            the nodeType to set
     */
    public void setNodeType(final String nodeType) {
        this.nodeType = nodeType;
    }

}
