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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

/**
 * POJO class to fetch schedule configuration data from external services and use it for schedule job creation
 * 
 * @author tcsmano
 * 
 */
public class ScheduleData implements Serializable{

    private static final long serialVersionUID = -134085859041198899L;

    private String startTime;
    
    private String cronExpression;
    
    private String endTime;

    /**
     * @return the startTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the cronExpression
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * @param cronExpression the cronExpression to set
     */
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * @return the endTime
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }
}
