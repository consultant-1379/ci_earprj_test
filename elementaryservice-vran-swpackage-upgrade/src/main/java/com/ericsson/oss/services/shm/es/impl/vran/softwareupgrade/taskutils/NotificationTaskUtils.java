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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;

public class NotificationTaskUtils {

    public String assignJobEventNameBasedOnActivity(final String activityName) {
        String jobEvent = null;
        if (activityName.equalsIgnoreCase(ActivityConstants.PREPARE)) {
            jobEvent = VranJobEvents.PREPARE_PROCESS_NOTIFICATION;
        } else if (activityName.equalsIgnoreCase(ActivityConstants.VERIFY)) {
            jobEvent = VranJobEvents.VERIFY_PROCESS_NOTIFICATION;
        } else if (activityName.equalsIgnoreCase(ActivityConstants.ACTIVATE)) {
            jobEvent = VranJobEvents.ACTIVATE_PROCESS_NOTIFICATION;
        } else if (activityName.equalsIgnoreCase(ActivityConstants.CONFIRM)) {
            jobEvent = VranJobEvents.CONFIRM_PROCESS_NOTIFICATION;
        }
        return jobEvent;
    }

    public String assignServiceNameBasedOnActivity(final String activityName) {
        String service = null;
        if (activityName.equalsIgnoreCase(ActivityConstants.PREPARE)) {
            service = VranJobEvents.PREPARE_SERVICE;
        } else if (activityName.equalsIgnoreCase(ActivityConstants.VERIFY)) {
            service = VranJobEvents.VERIFY_SERVICE;
        } else if (activityName.equalsIgnoreCase(ActivityConstants.ACTIVATE)) {
            service = VranJobEvents.ACTIVATE_SERVICE;
        } else if (activityName.equalsIgnoreCase(ActivityConstants.CONFIRM)) {
            service = VranJobEvents.CONFIRM_SERVICE;
        }
        return service;
    }

    public boolean isJobSuccessResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return vranSoftwareUpgradeJobResponse != null && ShmConstants.SUCCESS.equalsIgnoreCase(vranSoftwareUpgradeJobResponse.getResult());
    }

    public boolean isFlowTypeAction(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return VranJobConstants.ACTION.equals(vranSoftwareUpgradeJobResponse.getFlowType());
    }

    public boolean isFlowTypeProgress(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return VranJobConstants.PROGRESS.equals(vranSoftwareUpgradeJobResponse.getFlowType());
    }

    public Map<String, Object> prepareEventAttributes(final UpgradePackageContext upgradePackageContext, final long activityJobId) {
        final Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put(ShmConstants.NE_NAME, upgradePackageContext.getNodeName());
        eventAttributes.put(VranJobConstants.VNF_ID, upgradePackageContext.getVnfId());
        eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        return eventAttributes;
    }

    public String prepareJobLogMessage(final VranSoftwareUpgradeJobResponse notification, final String operation) {
        return String.format(VranJobLogMessageTemplate.PROGRESS_INFORMATION_WITH_RESULT, notification.getOperation() != null ? notification.getOperation() : operation, notification.getProgressLevel(),
                notification.getProgressDetail(), notification.getState(), notification.getResult(), notification.getAdditionalInfo(), notification.getRequestedTime(), notification.getFinishedTime());

    }

    public String prepareErrorLogMessage(final VranSoftwareUpgradeJobResponse notification, final String operation) {
        String logMessage = null;
        if (notification.getAdditionalInfo() != null && !notification.getAdditionalInfo().isEmpty()) {
            logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_WITH_ADDITIONAL_INFO, operation, notification.getAdditionalInfo(), notification.getProgressDetail());
        } else {
            logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_WITH_ERROR, operation, notification.getErrorMessage(), notification.getErrorCode(), notification.getErrorTime());
        }
        return logMessage;
    }

    public boolean isAdditionalInfoExists(final String additionalInfo) {
        return additionalInfo != null && !additionalInfo.isEmpty();
    }
}
