package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.Date;

public class JobLog {
    final private Date entryTime;
    final private String message;
    final private String neName;
    final private String activityName;
    final private String logLevel;
    final private String nodeType;

    public JobLog(final Date entryTime, final String message, final String neName, final String nodeType, final String activityName, final String logLevel) {
        this.entryTime = entryTime;
        this.message = message;
        this.neName = neName;
        this.nodeType = nodeType;
        this.activityName = activityName;
        this.logLevel = logLevel;

    }

    public Date getEntryTime() {
        return entryTime;
    }

    public String getMessage() {
        return message;
    }

    public String getNeName() {
        return neName;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getNodeType() {
        return nodeType;
    }

}
