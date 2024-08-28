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
import java.util.Arrays;
import java.util.List;

/**
 * POJO which configures values for periodic jobs
 * 
 * @author tcsdeku
 * 
 */

public class ScheduleJobConfiguration implements Serializable {
    private static final long serialVersionUID = 1234567L;
    private String repeatType;
    private String repeatCount;
    private List<String> repeatOn;
    private String occurences;
    private String endDate;
    private String startDate;
    private String cronExpression;

    /**
     * @return the repeatType
     */
    public String getRepeatType() {
        return repeatType;
    }

    /**
     * @param repeatType
     *            the repeatType to set
     */
    public void setRepeatType(final String repeatType) {
        this.repeatType = repeatType;
    }

    /**
     * @return the repeatCount
     */
    public String getRepeatCount() {
        return repeatCount;
    }

    /**
     * @param repeatCount
     *            the repeatCount to set
     */
    public void setRepeatCount(final String repeatCount) {
        this.repeatCount = repeatCount;
    }

    /**
     * @return the repeatOn
     */
    public List<String> getRepeatOn() {
        return repeatOn;
    }

    /**
     * @param repeatOn
     *            the repeatOn to set
     */
    public void setRepeatOn(final String repeatOn) {
        List<String> repeatList = null;
        if (repeatOn != null) {
            repeatList = Arrays.asList(repeatOn.split(","));
        }
        this.repeatOn = repeatList;
    }

    /**
     * @return the cronExpression
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * @param cronExpression
     *            the cronExpression to set
     */
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * @return the occurences
     */
    public String getOccurences() {
        return occurences;
    }

    /**
     * @param occurences
     *            the occurences to set
     */
    public void setOccurences(final String occurences) {
        this.occurences = occurences;
    }

    /**
     * @return the endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @param endDate
     *            the endDate to set
     */
    public void setEndDate(final String endDate) {
        this.endDate = endDate;
    }

    /**
     * @return the startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @param startDate
     *            the startDate to set
     */
    public void setStartDate(final String startDate) {
        this.startDate = startDate;
    }

    public String toString() {

        return "ScheduleJobConfiguration [repeatType=" + repeatType + ",repeatCount=" + repeatCount + ",repeatOn=" + repeatOn + ",occurences=" + occurences + ",startDate=" + startDate + ",endDate="
                + endDate + ",cronExpression=" + cronExpression + "]";
    }

}
