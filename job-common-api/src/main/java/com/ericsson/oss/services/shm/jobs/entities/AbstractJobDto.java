/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

public abstract class AbstractJobDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String status;
    private final Double progress;
    private final String result;
    private final Date startTime;
    private final Date endTime;

    public AbstractJobDto(final Map<String, Object> abstractJobAttributes) {
        this.status = getAsString(abstractJobAttributes.get(ShmConstants.STATE));
        this.result = getAsString(abstractJobAttributes.get(ShmConstants.RESULT));
        this.progress = getAsDouble(abstractJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE));
        this.startTime = getAsDate(abstractJobAttributes.get(ShmConstants.STARTTIME));
        this.endTime = getAsDate(abstractJobAttributes.get(ShmConstants.ENDTIME));
    }

    public final String getStatus() {
        return status;
    }

    public final Double getProgress() {
        if (status.equalsIgnoreCase(JobState.SCHEDULED.getJobStateName())) {
            return -2.0;
        } else if (status.equalsIgnoreCase(JobState.WAIT_FOR_USER_INPUT.getJobStateName())) {
            return -1.0;
        }
        return progress;
    }

    public final String getResult() {
        return result;
    }

    public final Date getStartTime() {
        return startTime;
    }

    public final Date getEndTime() {
        return endTime;
    }

    protected static final String getAsString(final Object object) {
        return object != null ? (String) object : "";
    }

    protected static final Double getAsDouble(final Object object) {
        return object != null ? (Double) object : 0L;
    }

    protected static final int getAsInt(final Object object) {
        return object != null ? (int) object : 0;
    }

    protected static final Date getAsDate(final Object object) {
        return object != null ? (Date) object : new Date(0);
    }

}
