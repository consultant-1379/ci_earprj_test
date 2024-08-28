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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationFailureHandler;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

public class PrepareActivityNotificationProcessor extends ProcessNotificationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareActivityNotificationProcessor.class);

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private ProcessNotificationFailureHandler processNotificationFailureHandler;

    public void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {

        final long activityJobId = jobActivityInformation.getActivityJobId();
        final JobEnvironment jobContext = upgradePackageContext.getJobEnvironment();
        LOGGER.debug("ActivityJob ID - [{}] : Job notification received : {}", activityJobId, vranSoftwareUpgradeJobResponse);

        recordNotification(vranSoftwareUpgradeJobResponse, activityJobId, upgradePackageContext, VranJobEvents.PREPARE_PROCESS_NOTIFICATION);

        LOGGER.debug("ActivityJob ID - [{}] : Prepare Service - Processing notification for activity [{}] with event type : [{}]", activityJobId, jobActivityInformation.getActivityName(),
                vranSoftwareUpgradeJobResponse.getFlowType());

        if (isPrepareInitiationSuccessResponse(vranSoftwareUpgradeJobResponse)) {
            if (notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)) {
                processActivityActionNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobContext, jobActivityInformation, VranUprgradeConstants.PREPARE_OPERATION);
            } else if (notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)) {
                processActivityProgressNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInformation, VranUprgradeConstants.PREPARE_OPERATION);
            } else {
                LOGGER.warn("Unsupported FlowType in processing notification");
            }
        } else {
            LOGGER.debug("ActivityJob ID - [{}] : Processing failure notification for activity [{}] of software upgrade on node", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
            processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, jobActivityInformation.getActivityName(), upgradePackageContext, jobActivityInformation);
        }
    }

    private boolean isPrepareInitiationSuccessResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse);
    }

    @Override
    public String getActivityName() {
        return ActivityConstants.PREPARE;
    }

}
