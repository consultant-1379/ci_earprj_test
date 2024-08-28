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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class NeJobDetails implements Serializable {

    private static final long serialVersionUID = -5212495583759926359L;
    private Long neJobId;
    private String neNodeName;
    private double neProgress;
    private String neStatus;
    private String neResult;
    private String neStartDate;
    private String neEndDate;
    private String neActivity;
    private Map<String, Map<String, String>> neJobConfiguration;
    private List<ActivityDetails> activityDetailsList;
    private String lastLogMessage;
    private String nodeType;

    /**
     * @return the neNodeName
     */
    public String getNeNodeName() {
        return neNodeName;
    }

    /**
     * @param neNodeName
     *            the neNodeName to set
     */
    public void setNeNodeName(final String neNodeName) {
        this.neNodeName = neNodeName;
    }

    /**
     * @return the neJobId
     */
    public Long getNeJobIdAsLong() {
        return neJobId;
    }

    /**
     * @param neJobId
     *            the neJobId to set
     */
    public void setNeJobId(final Long neJobId) {
        this.neJobId = neJobId;
    }

    /**
     * @param neJobId
     *            the neJobId to set
     */
    public void setNeJobIdAsLong(final String neJobId) {
        this.neJobId = Long.valueOf(neJobId);
    }

    /**
     * @return the neProgress
     */
    public double getNeProgress() {
        return neProgress;
    }

    /**
     * @param neProgress
     *            the neProgress to set
     */
    public void setNeProgress(final double neProgress) {
        this.neProgress = neProgress;
    }

    /**
     * @return the neStatus
     */
    public String getNeStatus() {
        return neStatus;
    }

    /**
     * @param neStatus
     *            the neStatus to set
     */
    public void setNeStatus(final String neStatus) {
        this.neStatus = neStatus;
    }

    /**
     * @return the neResult
     */
    public String getNeResult() {
        return neResult;
    }

    /**
     * @param neResult
     *            the neResult to set
     */
    public void setNeResult(final String neResult) {
        this.neResult = neResult;
    }

    /**
     * @return the neStartDate
     */
    public String getNeStartDate() {
        return neStartDate;
    }

    /**
     * @param neStartDate
     *            the neStartDate to set
     */
    public void setNeStartDate(final String neStartDate) {
        this.neStartDate = neStartDate;
    }

    /**
     * @return the neEndDate
     */
    public String getNeEndDate() {
        return neEndDate;
    }

    /**
     * @param neEndDate
     *            the neEndDate to set
     */
    public void setNeEndDate(final String neEndDate) {
        this.neEndDate = neEndDate;
    }

    /**
     * @return the neActivity
     */
    public String getNeActivity() {
        return neActivity;
    }

    /**
     * @param neActivity
     *            the neActivity to set
     */
    public void setNeActivity(final String neActivity) {
        this.neActivity = neActivity;
    }

    /**
     * @return the activityDetailsList
     */
    public List<ActivityDetails> getActivityDetailsList() {
        return activityDetailsList;
    }

    /**
     * @param activityDetailsList
     *            the activityDetailsList to set
     */
    public void setActivityDetailsList(final List<ActivityDetails> activityDetailsList) {
        this.activityDetailsList = activityDetailsList;
    }

    /**
     * @return the neJobId
     */
    public String getNeJobId() {
        return String.valueOf(neJobId);
    }

    /**
     * @return the neJobConfiguration
     */
    public Map<String, Map<String, String>> getNeJobConfiguration() {
        return neJobConfiguration;
    }

    /**
     * @param neJobConfiguration
     *            the neJobConfiguration to set
     */
    public void setNeJobConfiguration(final Map<String, Map<String, String>> neJobConfiguration) {
        this.neJobConfiguration = neJobConfiguration;
    }

    /**
     * @return the lastLogMessage
     */
    public String getLastLogMessage() {
        return lastLogMessage;
    }

    /**
     * @param lastLogMessage
     *            the lastLogMessage to set
     */
    public void setLastLogMessage(final String lastLogMessage) {
        this.lastLogMessage = lastLogMessage;
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
