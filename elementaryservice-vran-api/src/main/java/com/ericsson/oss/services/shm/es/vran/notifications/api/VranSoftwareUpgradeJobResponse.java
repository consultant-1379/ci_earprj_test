/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.vran.notifications.api;

import java.util.Date;

/**
 * This class holds the VRAN Software upgrade Job notification information
 * 
 * @author xindkag
 */

@SuppressWarnings("PMD")
public class VranSoftwareUpgradeJobResponse {

    private static final long serialVersionUID = -8868272325104610399L;
    private String activityName;
    private String networkElementName;

    private String jobCreationTime;
    private String state;
    private int progressLevel;
    private String progressDetail;
    private int jobId;
    private String vnfId;
    private String vnfPackageId;
    private String vnfDescriptorId;
    private long activityJobId;
    private String operation;
    private String flowType;

    private String requestedTime;
    private String finishedTime;
    private Date notificationReceivedTime;
    private int fallbackTimeout;
    private String result;
    private String additionalInfo;
    private boolean isHandleTimeout;

    private String errorMessage;
    private String errorTime;
    private int errorCode;

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName
     * 
     */
    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    /**
     * @return the networkElementName
     */
    public String getNetworkElementName() {
        return networkElementName;
    }

    /**
     * @param networkElementName
     * 
     */
    public void setNetworkElementName(final String networkElementName) {
        this.networkElementName = networkElementName;
    }

    /**
     * @return the jobCreationTime
     */
    public String getJobCreationTime() {
        return jobCreationTime;
    }

    /**
     * @param jobCreationTime
     * 
     */
    public void setJobCreationTime(final String jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state
     * 
     */
    public void setState(final String state) {
        this.state = state;
    }

    /**
     * @return the progressLevel
     */
    public int getProgressLevel() {
        return progressLevel;
    }

    /**
     * @param progressLevel
     * 
     */
    public void setProgressLevel(final int progressLevel) {
        this.progressLevel = progressLevel;
    }

    /**
     * @return the progressDetail
     */
    public String getProgressDetail() {
        return progressDetail;
    }

    /**
     * @param progressDetail
     * 
     */
    public void setProgressDetail(final String progressDetail) {
        this.progressDetail = progressDetail;
    }

    /**
     * @return the jobId
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * @param jobId
     * 
     */
    public void setJobId(final int jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the vnfId
     */
    public String getVnfId() {
        return vnfId;
    }

    /**
     * @param vnfId
     * 
     */
    public void setVnfId(final String vnfId) {
        this.vnfId = vnfId;
    }

    /**
     * @return the vnfPackageId
     */
    public String getVnfPackageId() {
        return vnfPackageId;
    }

    /**
     * @param vnfPackageId
     * 
     */
    public void setVnfPackageId(final String vnfPackageId) {
        this.vnfPackageId = vnfPackageId;
    }

    /**
     * @return the vnfDescriptorId
     */
    public String getVnfDescriptorId() {
        return vnfDescriptorId;
    }

    /**
     * @param vnfDescriptorId
     * 
     */
    public void setVnfDescriptorId(final String vnfDescriptorId) {
        this.vnfDescriptorId = vnfDescriptorId;
    }

    /**
     * @return the activityJobId
     */
    public long getActivityJobId() {
        return activityJobId;
    }

    /**
     * @param activityJobId
     * 
     */
    public void setActivityJobId(final long activityJobId) {
        this.activityJobId = activityJobId;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation
     * 
     */
    public void setOperation(final String operation) {
        this.operation = operation;
    }

    /**
     * @return the flowType
     */
    public String getFlowType() {
        return flowType;
    }

    /**
     * @param flowType
     * 
     */
    public void setFlowType(final String flowType) {
        this.flowType = flowType;
    }

    /**
     * @return the requestedTime
     */
    public String getRequestedTime() {
        return requestedTime;
    }

    /**
     * @param requestedTime
     * 
     */
    public void setRequestedTime(final String requestedTime) {
        this.requestedTime = requestedTime;
    }

    /**
     * @return the finishedTime
     */
    public String getFinishedTime() {
        return finishedTime;
    }

    /**
     * @param finishedTime
     * 
     */
    public void setFinishedTime(final String finishedTime) {
        this.finishedTime = finishedTime;
    }

    /**
     * @return the notificationReceivedTime
     */
    public Date getNotificationReceivedTime() {
        return notificationReceivedTime;
    }

    /**
     * @param notificationReceivedTime
     * 
     */
    public void setNotificationReceivedTime(final Date notificationReceivedTime) {
        this.notificationReceivedTime = notificationReceivedTime;
    }

    /**
     * @return the fallbackTimeout
     */
    public int getFallbackTimeout() {
        return fallbackTimeout;
    }

    /**
     * @param fallbackTimeout
     * 
     */
    public void setFallbackTimeout(final int fallbackTimeout) {
        this.fallbackTimeout = fallbackTimeout;
    }

    /**
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result
     * 
     */
    public void setResult(final String result) {
        this.result = result;
    }

    /**
     * @return the additionalInfo
     */
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * @param additionalInfo
     * 
     */
    public void setAdditionalInfo(final String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    /**
     * @return the isHandleTimeout
     */
    public boolean isHandleTimeout() {
        return isHandleTimeout;
    }

    /**
     * @param isHandleTimeout
     * 
     */
    public void setHandleTimeout(final boolean isHandleTimeout) {
        this.isHandleTimeout = isHandleTimeout;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage
     * 
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the errorTime
     */
    public String getErrorTime() {
        return errorTime;
    }

    /**
     * @param errorTime
     * 
     */
    public void setErrorTime(final String errorTime) {
        this.errorTime = errorTime;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode
     * 
     */
    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "VranSoftwareUpgradeJobResponse [activityName=" + activityName + ", networkElementName=" + networkElementName + ", jobCreationTime=" + jobCreationTime + ", state=" + state
                + ", progressLevel=" + progressLevel + ", progressDetail=" + progressDetail + ", jobId=" + jobId + ", vnfId=" + vnfId + ", vnfPackageId=" + vnfPackageId + ", vnfDescriptorId="
                + vnfDescriptorId + ", activityJobId=" + activityJobId + ", operation=" + operation + ", flowType=" + flowType + ", requestedTime=" + requestedTime + ", finishedTime=" + finishedTime
                + ", notificationReceivedTime=" + notificationReceivedTime + ", fallbackTimeout=" + fallbackTimeout + ", result=" + result + ", additionalInfo=" + additionalInfo + ", isHandleTimeout="
                + isHandleTimeout + ", errorMessage=" + errorMessage + ", errorTime=" + errorTime + ", errorCode=" + errorCode + "]";
    }
}
