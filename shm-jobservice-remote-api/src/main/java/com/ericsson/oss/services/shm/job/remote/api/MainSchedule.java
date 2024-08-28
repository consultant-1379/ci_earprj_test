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

public class MainSchedule implements Serializable {

    private static final long serialVersionUID = 1L;
    private ScheduleAttributes[] scheduleAttributes;

    private String execMode;

    public ScheduleAttributes[] getScheduleAttributes ()
    {
        return scheduleAttributes;
    }

    public void setScheduleAttributes (final ScheduleAttributes[] scheduleAttributes)
    {
        this.scheduleAttributes = scheduleAttributes;
    }

    public String getExecMode ()
    {
        return execMode;
    }

    public void setExecMode (final String execMode)
    {
        this.execMode = execMode;
    }

}
