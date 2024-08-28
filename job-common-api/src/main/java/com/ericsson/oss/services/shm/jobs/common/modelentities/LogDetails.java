package com.ericsson.oss.services.shm.jobs.common.modelentities;

/**
 * This class holds the log details for ActivityJob PO
 * 
 */
public class LogDetails {

    private String entryTime;
    private String message;
    private String logLevel;

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

}
