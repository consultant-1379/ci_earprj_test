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
import java.util.Map;

public class ActivityDetails implements Serializable {

    private static final long serialVersionUID = 550891584153352633L;

    private String activityName;
    private String activitySchedule;
    private String activityScheduleTime;
    private String activityStartTime;
    private String activityEndTime;
    private Double activityProgress;
    private String activityResult;
    private int activityOrder;
    private Map<String, Map<String, String>> activityConfiguration;
    private String activityJobIdAsString;

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    public String getActivitySchedule() {
        return activitySchedule;
    }

    public void setActivitySchedule(final String activitySchedule) {
        this.activitySchedule = activitySchedule;
    }

    public String getActivityStartTime() {
        return activityStartTime;
    }

    public void setActivityStartTime(final String activityStartTime) {
        this.activityStartTime = activityStartTime;
    }

    public String getActivityEndTime() {
        return activityEndTime;
    }

    public void setActivityEndTime(final String activityEndTime) {
        this.activityEndTime = activityEndTime;
    }

    public String getActivityResult() {
        return activityResult;
    }

    public void setActivityResult(final String activityResult) {
        this.activityResult = activityResult;
    }

    /**
     * @return the activityOrder
     */
    public int getActivityOrder() {
        return activityOrder;
    }

    /**
     * @param activityOrder
     *            the activityOrder to set
     */
    public void setActivityOrder(final int activityOrder) {
        this.activityOrder = activityOrder;
    }

    /**
     * @return the activityConfiguration
     */
    public Map<String, Map<String, String>> getActivityConfiguration() {
        return activityConfiguration;
    }

    /**
     * @param activityConfiguration
     *            the activityConfiguration to set
     */
    public void setActivityConfiguration(final Map<String, Map<String, String>> activityConfiguration) {
        this.activityConfiguration = activityConfiguration;
    }

    /**
     * @return the activityJobIdAsString
     */
    public String getActivityJobIdAsString() {
        return activityJobIdAsString;
    }

    /**
     * @param activityJobIdAsString
     *            the activityJobIdAsString to set
     */
    public void setActivityJobIdAsString(final String activityJobIdAsString) {
        this.activityJobIdAsString = activityJobIdAsString;
    }

    public String getActivityScheduleTime() {
        return activityScheduleTime;
    }

    public void setActivityScheduleTime(final String activityScheduleTime) {
        this.activityScheduleTime = activityScheduleTime;
    }

    /**
     * @return the activityProgress
     */
    public Double getActivityProgress() {
        return activityProgress;
    }

    /**
     * @param activityProgress
     *            the activityProgress to set
     */
    public void setActivityProgress(final Double activityProgress) {
        this.activityProgress = activityProgress;
    }

}
