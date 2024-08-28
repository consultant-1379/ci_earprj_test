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

public class ActivitySchedule implements Serializable {

    private static final long serialVersionUID = 1L;
    private String platformType;

    private ActivitySchedulesValue[] value;

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(final String platformType) {
        this.platformType = platformType;
    }

    public ActivitySchedulesValue[] getValue() {
        return value;
    }

    public void setValue(final ActivitySchedulesValue[] value) {
        this.value = value;
    }

}
