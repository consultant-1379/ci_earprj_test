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
package com.ericsson.oss.services.shm.notifications.impl;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;

import java.util.Date;

/**
 * Holds the information about the listener.
 */
public class FdnNotificationSubject implements NotificationSubject {

    private static final long serialVersionUID = -7525643961500989072L;
    private final String fdn; // the fdn for which it needs notifications
    private final Object observerHandle;
    private Date timeStamp; // the last time stamp this listener was notified.
    private final NotificationType notificationType;
    private String activityName;
    private String jobType;
    private String platform;
    private NotificationCallbackResult notificationCallBackResult;

    /**
     * @param fdn
     * @param jobId
     * @param activityInfo
     */
    public FdnNotificationSubject(final String fdn, final long jobId, final JobActivityInfo activityInfo) {
        this.fdn = fdn;
        this.observerHandle = jobId;
        notificationType = NotificationType.JOB;
        this.activityName = activityInfo.getActivityName();
        this.jobType = activityInfo.getJobType().name();
        this.platform = activityInfo.getPlatform().name();
    }

    /**
     * @param fdn
     * @param activityInfo
     * @param notificationCallBackResult
     *            TODO
     */
    public FdnNotificationSubject(final String fdn, final JobActivityInfo activityInfo, final NotificationCallbackResult notificationCallBackResult) {
        this.fdn = fdn;
        this.notificationCallBackResult = notificationCallBackResult;
        this.activityName = activityInfo.getActivityName();
        this.jobType = activityInfo.getJobType().name();
        this.platform = activityInfo.getPlatform().name();
        notificationType = NotificationType.SYNCHRONOUS_REQUEST;
        this.observerHandle = "";

    }

    /**
     * @param fdn
     * @param cvNotificationKey
     */
    public FdnNotificationSubject(final String fdn, final String cvNotificationKey) {
        this.fdn = fdn;
        this.observerHandle = cvNotificationKey;
        notificationType = NotificationType.SYNCHRONOUS_REQUEST;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = observerHandle.hashCode() * prime + fdn.hashCode();
        result = result * notificationType.hashCode() + result;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final FdnNotificationSubject other = (FdnNotificationSubject) obj;

        if (!this.observerHandle.equals(other.observerHandle)) {
            return false;
        }

        if (!this.fdn.equals(other.getFdn())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "NotificationSubject [ fdn=" + fdn + " ,  activity job id=" + observerHandle + "]";
    }

    /**
     * @return the fdn
     */
    public String getFdn() {
        return fdn;
    }

    /**
     * @return the observerHandle
     */
    public Object getObserverHandle() {
        return observerHandle;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.upgrade.NotificationSubject #getKey()
     */
    @Override
    public String getKey() {
        return fdn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.upgrade.NotificationSubject #getTimeStamp()
     */
    @Override
    public Date getTimeStamp() {
        return timeStamp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.upgrade.NotificationSubject #setTimeStamp()
     */
    @Override
    public void setTimeStamp(final Date timeStamp) {
        this.timeStamp = timeStamp;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.upgrade.NotificationSubject #getNotificationType()
     */
    @Override
    public NotificationType getNotificationType() {
        return notificationType;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.model.NotificationSubject#getPlatform()
     */
    @Override
    public String getPlatform() {
        return platform;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.model.NotificationSubject#getJobType()
     */
    @Override
    public String getJobType() {
        return jobType;
    }

    /**
     * @return the notificationCallBackResult
     */
    public NotificationCallbackResult getNotificationCallBackResult() {
        return notificationCallBackResult;
    }

}
