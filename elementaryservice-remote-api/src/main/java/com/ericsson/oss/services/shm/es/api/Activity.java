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

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

@EService
@Remote
public interface Activity {

    /*
     * This method has been deprecated since 1.48.6. To introduce new Elementary Service in SHM, please use AsyncActivity Interface.
     */
    @Deprecated
    ActivityStepResult precheck(long activityJobId);

    void execute(long activityJobId);

    /*
     * This method has been deprecated since 1.48.6. To introduce new Elementary Service in SHM, please use AsyncActivity Interface.
     */
    @Deprecated
    ActivityStepResult handleTimeout(long activityJobId);

    ActivityStepResult cancel(long activityJobId);

    ActivityStepResult cancelTimeout(long activityJobId, boolean finalizeResult);

}
