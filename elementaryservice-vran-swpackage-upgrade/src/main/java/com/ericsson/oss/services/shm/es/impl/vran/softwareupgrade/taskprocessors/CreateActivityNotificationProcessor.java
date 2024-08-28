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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationFailureHandler;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

public class CreateActivityNotificationProcessor extends ProcessNotificationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateActivityNotificationProcessor.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private TaskBase taskBase;

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private ProcessNotificationFailureHandler processNotificationFailureHandler;

    @Inject
    private JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Inject
    private JobLogsPersistenceProvider jogLogsPersistenceProvider;

    @Override
    public void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {
        final JobEnvironment jobContext = upgradePackageContext.getJobEnvironment();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();

        LOGGER.debug("ActivityJob ID - [{}] : Create Service - Processing notification for activity [{}] with event type : [{}]", activityJobId, jobActivityInformation.getActivityName(),
                vranSoftwareUpgradeJobResponse.getFlowType());
        if (isCreateInitiationSuccessResponse(vranSoftwareUpgradeJobResponse)) {
            if (notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)) {
                processActivityActionNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobContext, jobActivityInformation, VranUprgradeConstants.CREATE_OPERATION);
            } else if (notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)) {
                processActivityProgressNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInformation, VranUprgradeConstants.CREATE_OPERATION);
            } else {
                LOGGER.warn("Unsupported FlowType in processing notification");
            }
        } else if (isCreateInitiationCancelResponse(vranSoftwareUpgradeJobResponse)) {
            LOGGER.debug("ActivityJob ID - [{}] : Processing Cancel action during Create activity of software upgrade on node", activityJobId);
            jobCancelActivityNotificationProcessor.processNotification(jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);
        } else {
            LOGGER.debug("ActivityJob ID - [{}] : Processing failure notification for activity [{}] of software upgrade on node", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
            processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, jobActivityInformation.getActivityName(), upgradePackageContext, jobActivityInformation);
        }
    }

    @Override
    public void proceedWithNextSteps(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext vranUpgradeInformation,
            final JobActivityInfo jobActivityInformation, final String nodeName, final String operation) {
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        final JobEnvironment jobContext = vranUpgradeInformation.getJobEnvironment();

        jogLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, operation, jobProperties);
        recordActivitySucess(vranSoftwareUpgradeJobResponse, vranUpgradeInformation, nodeName, operation);

        taskBase.unSubscribeNotification(jobActivityInformation, vranUpgradeInformation, activityJobId);
        notifyWfs(jobActivityInformation, jobContext);

        LOGGER.debug("ActivityJob ID - [{}] : {} activity has been completed successfully. Notifying to workflow service.", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
    }

    private void notifyWfs(final JobActivityInfo jobActivityInformation, final JobEnvironment jobContext) {
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        activityUtils.sendNotificationToWFS(jobContext, jobActivityInformation.getActivityJobId(), ActivityConstants.CREATE, processVariables);
        LOGGER.debug("ActivityJob ID - [{}] : Create Software Upgrade Job has been completed successfully. Notifying to workflow service.", jobActivityInformation.getActivityJobId());
    }

    private boolean isCreateInitiationSuccessResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse);
    }

    private boolean isCreateInitiationCancelResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return vranSoftwareUpgradeJobResponse != null && ShmConstants.CANCELLED.equalsIgnoreCase(vranSoftwareUpgradeJobResponse.getResult());
    }

    @Override
    public String getActivityName() {
        return ActivityConstants.CREATE;
    }

}
