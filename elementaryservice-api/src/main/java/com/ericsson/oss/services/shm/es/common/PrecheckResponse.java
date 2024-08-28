/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.common;

import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;

/**
 * This class holds the status of Pre-validation.
 * 
 * @author tcsgusw, xprapav, xarirud
 * 
 */
public class PrecheckResponse {

    private ActivityStepResultEnum activityStepResultEnum;

    /**
     * @param activityStepResultEnum
     */
    public PrecheckResponse(final ActivityStepResultEnum activityStepResultEnum) {
        this.activityStepResultEnum = activityStepResultEnum;
    }

    /**
     * @return the activityStepResultEnum
     */
    public ActivityStepResultEnum getActivityStepResultEnum() {
        return activityStepResultEnum;
    }

    /**
     * @param activityStepResultEnum
     *            the activityStepResultEnum to set
     */
    public void setActivityStepResultEnum(final ActivityStepResultEnum activityStepResultEnum) {
        this.activityStepResultEnum = activityStepResultEnum;
    }

}
