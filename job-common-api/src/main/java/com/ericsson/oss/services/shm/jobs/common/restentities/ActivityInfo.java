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
package com.ericsson.oss.services.shm.jobs.common.restentities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;

public class ActivityInfo implements Serializable {

    private static final long serialVersionUID = 1234567L;
    private String activityName;

    private String scheduleType;

    private int order;

    private String scheduledTime;

    private List<JobProperty> jobProperties = new ArrayList<JobProperty>();

    /**
     * This Constructor is being used only for converting Json object to POJO
     */
    public ActivityInfo() {
    }

    /**
     * @param activityName
     * @param scheduleType
     */
    public ActivityInfo(final String name, final String scheduleType, final int order) {
        super();
        this.activityName = name;
        this.scheduleType = scheduleType;
        this.order = order;
    }

    /**
     * 
     * @param name
     * @param scheduleType
     * @param order
     * @param jobProperty
     */
    public ActivityInfo(final String name, final String scheduleType, final int order, final String scheduledTime, final List<JobProperty> jobProperties) {
        super();
        this.activityName = name;
        this.scheduleType = scheduleType;
        this.order = order;
        this.scheduledTime = scheduledTime;
        this.jobProperties = jobProperties;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @return the scheduleType
     */
    public String getScheduleType() {
        return scheduleType;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the scheduledTime
     */
    public String getScheduledTime() {
        return scheduledTime;
    }

    /**
     * @return the jobProperties
     */
    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

}
