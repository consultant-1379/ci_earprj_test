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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.task.common;

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

public class ActivityStatusManager {

    public boolean isActivityCompleted(final String operation, final String state) {
        return assignCompletedStateBasedOnOperation(operation).equalsIgnoreCase(state);
    }

    public boolean isActivityInProgress(final String activityName, final String state) {
        return assignInProgressStateBasedOnOperation(activityName).equalsIgnoreCase(state);
    }

    public String assignCompletedStateBasedOnOperation(final String operation) {
        String completedState = "";
        if (operation.equalsIgnoreCase(ActivityConstants.CREATE)) {
            completedState = VranUprgradeConstants.INITIALIZED;
        } else if (operation.equalsIgnoreCase(ActivityConstants.PREPARE)) {
            completedState = VranUprgradeConstants.PREPARE_COMPLETED;
        } else if (operation.equalsIgnoreCase(ActivityConstants.VERIFY)) {
            completedState = VranUprgradeConstants.PREPARE_COMPLETED;
        } else if (operation.equalsIgnoreCase(ActivityConstants.ACTIVATE)) {
            completedState = VranUprgradeConstants.WAITING_FOR_CONFIRM;
        } else if (operation.equalsIgnoreCase(ActivityConstants.CONFIRM)) {
            completedState = VranUprgradeConstants.CONFIRM_COMPLETED;
        }
        return completedState;
    }

    public String assignInProgressStateBasedOnOperation(final String operation) {
        String completedState = "";
        if (operation.equalsIgnoreCase(ActivityConstants.PREPARE)) {
            completedState = VranUprgradeConstants.PREPARE_IN_PROGRESS;
        } else if (operation.equalsIgnoreCase(ActivityConstants.VERIFY)) {
            completedState = VranUprgradeConstants.VERIFY_IN_PROGRESS;
        } else if (operation.equalsIgnoreCase(ActivityConstants.ACTIVATE)) {
            completedState = VranUprgradeConstants.ACTIVATION_IN_PROGRESS;
        } else if (operation.equalsIgnoreCase(ActivityConstants.CONFIRM)) {
            completedState = VranUprgradeConstants.CONFIRM_IN_PROGRESS;
        }
        return completedState;
    }

}
