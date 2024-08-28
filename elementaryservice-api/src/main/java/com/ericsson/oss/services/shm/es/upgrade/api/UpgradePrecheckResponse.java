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
package com.ericsson.oss.services.shm.es.upgrade.api;

import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.common.PrecheckResponse;

/**
 * This class holds the minimal data required in both during pre validation and to execute an action.
 * 
 * @author tcsgusw
 * 
 */
public class UpgradePrecheckResponse extends PrecheckResponse {

    private final String upMoFdn;

    public UpgradePrecheckResponse(final String upMoFdn, final ActivityStepResultEnum activityStepResultEnum) {
        super(activityStepResultEnum);
        this.upMoFdn = upMoFdn;
    }

    public String getUpMoFdn() {
        return upMoFdn;
    }
}
