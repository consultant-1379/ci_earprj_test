/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.license.refresh.api;

import java.util.Date;
import java.util.Map;

public class LkfImportResponse {

    private String fingerprint;
    private String neJobId;
    private String status;
    private String state;
    private String additionalInfo;
    private Map<String, Object> eventAttributes;
    private Date notificationReceivedTime;
    private String activityName;
    private long activityJobId;

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(final String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getNeJobId() {
        return neJobId;
    }

    public void setNeJobId(final String neJobId) {
        this.neJobId = neJobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(final String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Map<String, Object> getEventAttributes() {
        return eventAttributes;
    }

    public void setEventAttributes(final Map<String, Object> eventAttributes) {
        this.eventAttributes = eventAttributes;
    }

    public Date getNotificationReceivedTime() {
        return notificationReceivedTime;
    }

    public void setNotificationReceivedTime(final Date notificationReceivedTime) {
        this.notificationReceivedTime = notificationReceivedTime;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    public long getActivityJobId() {
        return activityJobId;
    }

    public void setActivityJobId(long activityJobId) {
        this.activityJobId = activityJobId;
    }

    @Override
    public String toString() {
        return "LkfImportResponse [fingerprint=" + fingerprint + ", neJobId=" + neJobId + ", status=" + status + ", state=" + state + ", additionalInfo=" + additionalInfo + ", eventAttributes="
                + eventAttributes + ", notificationReceivedTime=" + notificationReceivedTime + ", activityName=" + activityName + ", activityJobId=" + activityJobId + "]";
    }

}
