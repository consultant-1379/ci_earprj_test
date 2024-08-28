/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.Date;

public class JobLogExport extends JobLog {

    /**
     * @param entryTime
     * @param message
     * @param neName
     * @param activityName
     */
    public JobLogExport(final Date entryTime, final String message, final String neName, final String nodeType, final String activityName, final String logLevel) {
        super(entryTime, message, neName, nodeType, activityName, logLevel);

    }

    private String JobName;

    private String JobType;

    /**
     * @return the jobName
     */
    public String getJobName() {
        return JobName;
    }

    /**
     * @param jobName
     *            the jobName to set
     */
    public void setJobName(final String jobName) {
        JobName = jobName;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return JobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final String jobType) {
        JobType = jobType;
    }

}
