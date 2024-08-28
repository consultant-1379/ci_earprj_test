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
package com.ericsson.oss.services.shm.es.vran.onboard.api.notifications;

import java.io.Serializable;
import java.util.Date;

/**
 * This interface will provide abstract methods for nfvo software package job notification
 */
public interface NfvoSoftwarePackageJobResponse extends Serializable {

    /**
     * @return the nodeAddress
     */
    String getNodeAddress();

    /**
     * @param nodeAddress
     *            the nodeAddress to set
     */
    void setNodeAddress(final String nodeAddress);

    /**
     * @return the vnfPackageId
     */
    String getVnfPackageId();

    /**
     * @param vnfPackageId
     *            the vnfPackageId to set
     */
    void setVnfPackageId(final String vnfPackageId);

    /**
     * @return the jobId
     */
    String getJobId();

    /**
     * @param jobId
     *            the jobId to set
     */
    void setJobId(final String jobId);

    /**
     * @return the responseType
     */
    String getResponseType();

    /**
     * @param responseType
     *            the responseType to set
     */
    void setResponseType(final String responseType);

    /**
     * @return the status
     */
    String getStatus();

    /**
     * @param status
     *            the status to set
     */
    void setStatus(final String status);

    /**
     * @return the description
     */
    String getDescription();

    /**
     * @param description
     *            the description to set
     */
    void setDescription(final String description);

    /**
     * @return the activityJobId
     */
    long getActivityJobId();

    /**
     * @param activityJobId
     *            the activityJobId to set
     */
    void setActivityJobId(final long activityJobId);

    /**
     * @return the activityName
     */
    String getActivityName();

    /**
     * @param activityName
     *            the activityName to set
     */
    void setActivityName(final String activityName);

    /**
     * @return the notificationTimeStamp
     */
    Date getNotificationTimeStamp();

    /**
     * @param notificationTimeStamp
     *            the notificationTimeStamp to set
     */
    void setNotificationTimeStamp(final Date notificationTimeStamp);

    /**
     * @return the result
     */
    String getResult();

    /**
     * @param result
     *            the result to set
     */
    void setResult(final String result);

    /**
     * @return the errorMessage
     */
    String getErrorMessage();

    /**
     * @param errorMessage
     *            the errorMessage to set
     */
    void setErrorMessage(final String errorMessage);

    /**
     * @return the errorCode
     */
    int getErrorCode();

    /**
     * @param errorCode
     *            the errorCode to set
     */
    void setErrorCode(final int errorCode);

    /**
     * @return the fullFilePath
     */
    String getFullFilePath();

    /**
     * @param fullFilePath
     *            the fullFilePath to set
     */
    void setFullFilePath(final String fullFilePath);
}
