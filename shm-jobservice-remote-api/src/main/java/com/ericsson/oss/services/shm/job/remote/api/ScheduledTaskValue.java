/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
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

public class ScheduledTaskValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String[] scheduleAttributes;

    private String execMode;

    private Integer order;

    private String activityName;

    public String[] getScheduleAttributes() {
        return scheduleAttributes;
    }

    public void setScheduleAttributes(final String[] scheduleAttributes) {
        this.scheduleAttributes = scheduleAttributes;
    }

    public String getExecMode() {
        return execMode;
    }

    public void setExecMode(final String execMode) {
        this.execMode = execMode;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(final Integer order) {
        this.order = order;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    @Override
    public String toString() {
        return "ClassPojo [scheduleAttributes = " + scheduleAttributes + ", execMode = " + execMode + ", order = " + order + ", activityName = " + activityName + "]";
    }
}
