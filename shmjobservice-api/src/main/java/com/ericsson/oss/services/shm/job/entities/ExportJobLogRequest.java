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
package com.ericsson.oss.services.shm.job.entities;

import java.io.Serializable;

/**
 * This class receives the input from UI. Also it defines the setters and getters for variables sent from front end.
 * 
 */
/**
 * @author tcsande
 * 
 */
public class ExportJobLogRequest implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    //List of NE Job Ids
    private Long neJobIds;
    private Long mainJobId;
    private String jobName;
    private String jobType;

    /**
     * @return the neJobIds
     */
    public Long getNeJobIds() {
        return neJobIds;
    }

    /**
     * @param neJobIds
     *            the neJobIds to set
     */
    public void setNeJobIds(final Long neJobIds) {
        this.neJobIds = neJobIds;
    }

    /**
     * @return the mainJobId
     */
    public Long getMainJobId() {
        return mainJobId;
    }

    /**
     * @param mainJobId
     *            the mainJobId to set
     */
    public void setMainJobId(final Long mainJobId) {
        this.mainJobId = mainJobId;
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName
     *            the jobName to set
     */
    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final String jobType) {
        this.jobType = jobType;
    }

}
