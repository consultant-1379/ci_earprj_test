/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors;

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class VerifyActivityExecuteProcessor extends ExecuteTask {

    @Override
    public String getActivityToBeTriggered() {
        return ActivityConstants.VERIFY;
    }

}
