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
package com.ericsson.oss.services.shm.es.api;

import java.io.Serializable;

public class ActivityStepResult implements Serializable {

    private static final long serialVersionUID = -6134341044703022706L;

    private ActivityStepResultEnum activityResultEnum;

    /**
     * @return the activityResultEnum
     */
    public ActivityStepResultEnum getActivityResultEnum() {
        return activityResultEnum;
    }

    /**
     * @param activityResultEnum the activityResultEnum to set
     */
    public void setActivityResultEnum(final ActivityStepResultEnum activityResultEnum) {
        this.activityResultEnum = activityResultEnum;
    }


}
