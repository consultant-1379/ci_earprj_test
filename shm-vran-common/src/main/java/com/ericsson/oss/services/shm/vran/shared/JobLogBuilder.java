/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.shared;

import java.util.Date;

/**
 * Service to build job log
 * 
 * @author xsripod
 * 
 */

public class JobLogBuilder {

    private final JobLog jobLog = new JobLog();

    public JobLogBuilder setMessage(final String message) {
        jobLog.setMessage(message);
        return this;
    }

    public JobLogBuilder setTime(final Date entryTime) {
        jobLog.setTime(entryTime);
        return this;
    }

    public JobLogBuilder setSource(final String source) {
        jobLog.setSource(source);
        return this;
    }

    public JobLogBuilder setServerity(final String severity) {
        jobLog.setServerity(severity);
        return this;
    }

    public JobLog buildJobLog() {
        return jobLog;
    }
}
