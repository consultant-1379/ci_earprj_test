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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.List;

/**
 * This class holds response of job.
 * 
 * @author xgudpra
 */
public class JobStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobName;

    private String startTime;

    private String endTime;

    private Integer noOfNetworkElements;

    private String state;

    private double progressPercentage;

    private List<NeJobData> neJobResult;

    private String jobResult;

    /**
     * @return the progressPercentage
     */
    public double getProgressPercentage() {
        return progressPercentage;
    }

    /**
     * @param progressPercentage
     *            the progressPercentage to set
     */
    public void setProgressPercentage(final double progressPercentage) {
        this.progressPercentage = progressPercentage;
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
     * @return the endTime of main job
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * @param endTime
     *            the endTime to set
     */
    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }

    /**
     * @return the startTime of main job
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * @param startTime
     *            the startTime to set
     */
    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the noOfNetworkElements
     */
    public Integer getNoOfNetworkElements() {
        return noOfNetworkElements;
    }

    /**
     * @param noOfNetworkElements
     *            the noOfNetworkElements to set
     */
    public void setNoOfNetworkElements(final Integer noOfNetworkElements) {
        this.noOfNetworkElements = noOfNetworkElements;
    }

    /**
     * @return the jobResult of main job
     */
    public String getJobResult() {
        return jobResult;
    }

    /**
     * @param jobResult
     *            the jobResult to set
     */
    public void setJobResult(final String jobResult) {
        this.jobResult = jobResult;
    }

    /**
     * @return the state of main job
     */
    public String getState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(final String state) {
        this.state = state;
    }

    /**
     * @return the neJob details of main job
     */
    public List<NeJobData> getNeJobResult() {
        return neJobResult;
    }

    /**
     * @param neJobResult
     *            the neJobResult to set
     */
    public void setNeJobResult(final List<NeJobData> neJobResult) {
        this.neJobResult = neJobResult;
    }

    @Override
    public String toString() {
        return "JobStatus [jobName=" + jobName + ", startTime=" + startTime + ", endTime=" + endTime + ", noOfNetworkElements=" + noOfNetworkElements + ", state=" + state + ", progressPercentage="
                + progressPercentage + ", neJobResult=" + neJobResult + ", jobResult=" + jobResult + "]";
    }

}
