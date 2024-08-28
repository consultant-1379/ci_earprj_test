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
package com.ericsson.oss.services.shm.onboard.notification;

import java.util.Date;

/**
 * Class that implements OnBoardNotification interface And will provides the notification information.
 */
public class NfvoSoftwarePackageJobResponseImpl implements com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String nodeAddress;
    private String vnfPackageId;
    private String jobId;
    private String responseType;
    private String status;
    private String description;
    private long activityJobId;
    private String activityName;
    private String fullFilePath;
    private Date notificationTimeStamp;
    private String result;
    private String errorMessage;
    private int errorCode;

    /**
     * @return the nodeAddress
     */
    public String getNodeAddress() {
        return nodeAddress;
    }

    /**
     * @param nodeAddress
     *            the nodeAddress to set
     */
    public void setNodeAddress(final String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    /**
     * @return the vnfPackageId
     */
    public String getVnfPackageId() {
        return vnfPackageId;
    }

    /**
     * @param vnfPackageId
     *            the vnfPackageId to set
     */
    public void setVnfPackageId(final String vnfPackageId) {
        this.vnfPackageId = vnfPackageId;
    }

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @param jobId
     *            the jobId to set
     */
    public void setJobId(final String jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the responseType
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * @param responseType
     *            the responseType to set
     */
    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return the activityJobId
     */
    public long getActivityJobId() {
        return activityJobId;
    }

    /**
     * @param activityJobId
     *            the activityJobId to set
     */
    public void setActivityJobId(final long activityJobId) {
        this.activityJobId = activityJobId;
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
     * @return the notificationTimeStamp
     */
    public Date getNotificationTimeStamp() {
        return notificationTimeStamp;
    }

    /**
     * @param notificationTimeStamp
     *            the notificationTimeStamp to set
     */
    public void setNotificationTimeStamp(final Date notificationTimeStamp) {
        this.notificationTimeStamp = notificationTimeStamp;
    }

    /**
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result
     *            the result to set
     */
    public void setResult(final String result) {
        this.result = result;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage
     *            the errorMessage to set
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode
     *            the errorCode to set
     */
    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the fullFilePath
     */
    public String getFullFilePath() {
        return fullFilePath;
    }

    /**
     * @param fullFilePath
     *            the fullFilePath to set
     */
    public void setFullFilePath(final String fullFilePath) {
        this.fullFilePath = fullFilePath;
    }

    @Override
    public String toString() {
        return "NfvoSoftwarePackageJobResponse [nodeAddress=" + nodeAddress + ", vnfPackageId=" + vnfPackageId + ", jobId=" + jobId + ", responseType=" + responseType + ", status=" + status
                + ", description=" + description + ", activityJobId=" + activityJobId + ", activityName=" + activityName + ", fullFilePath=" + fullFilePath + ", notificationTimeStamp="
                + notificationTimeStamp + ", result=" + result + ", errorMessage=" + errorMessage + ", errorCode=" + errorCode + "]";
    }

}
