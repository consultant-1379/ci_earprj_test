package com.ericsson.oss.services.shm.shared.util;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

public class JobLogUtil {

    /**
     * This method prepares the job log list to be persisted in Database.
     * 
     * @param jobLogList
     * @param activityLogMessage
     * @param entryTime
     * @param logType
     * @return void
     */
    public void prepareJobLogAtrributesList(final List<Map<String, Object>> jobLogList, final String activityLogMessage, final Date entryTime, final String logType, final String logLevel) {
        if (activityLogMessage != null && !activityLogMessage.isEmpty()) {
            final Map<String, Object> activityAttributes = new HashMap<String, Object>();
            activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, activityLogMessage);
            activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, entryTime);
            activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, logType);
            activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
            jobLogList.add(activityAttributes);
        }
    }

    /**
     * Creates a New Log entry of SYSTEM type, with given entry date.
     * 
     * @param logMessage
     * @param notificationTime
     * @return
     */
    public Map<String, Object> createNewLogEntry(final String logMessage, final Date notificationTime, final String logLevel) {
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, notificationTime);
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        return logEntry;
    }

}
