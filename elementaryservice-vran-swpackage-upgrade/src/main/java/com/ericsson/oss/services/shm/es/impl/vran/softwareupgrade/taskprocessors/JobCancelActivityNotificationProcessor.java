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
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

public class JobCancelActivityNotificationProcessor extends ProcessNotificationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobCancelActivityNotificationProcessor.class);

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobLogsPersistenceProvider jobLogsPersistenceProvider;

    @Inject
    private TaskBase taskBase;

    public void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {
        LOGGER.trace("ActivityJob ID - [{}] : Notification received for cancelling the upgrade job and notification parameteres are: {}", vranSoftwareUpgradeJobResponse.getActivityJobId(),
                vranSoftwareUpgradeJobResponse);
        final String nodeName = upgradePackageContext.getJobEnvironment().getNodeName();
        if (isActivtityCompleted(vranSoftwareUpgradeJobResponse)) {
            proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInformation, nodeName, VranJobConstants.JOB_CANCEL);
        } else {
            jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, VranJobConstants.JOB_CANCEL);
            trackActivityStatusOnVnfm(vranSoftwareUpgradeJobResponse, upgradePackageContext);
        }
    }

    @Override
    public void proceedWithNextSteps(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInformation, final String nodeName, final String operation) {
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        LOGGER.debug("ActivityJob ID - [{}] : Progress Notification proceed with next steps for activity {} for operation {}  ", activityJobId, jobActivityInformation.getActivityName(), operation);

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.toString());
        jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, operation, jobProperties);

        recordActivitySucess(vranSoftwareUpgradeJobResponse, upgradePackageContext, nodeName, operation);

        taskBase.unSubscribeNotification(jobActivityInformation, upgradePackageContext, activityJobId);
        activityUtils.sendNotificationToWFS(upgradePackageContext.getJobEnvironment(), activityJobId, VranJobConstants.JOB_CANCEL, processVariables);

        LOGGER.debug("ActivityJob ID - [{}] : {} activity has been completed successfully. Notifying to workflow service.", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
    }

    @Override
    public void trackActivityStatusOnVnfm(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext) {
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        final Map<String, Object> eventAttributes = notificationTaskUtils.prepareEventAttributes(upgradePackageContext, activityJobId);
        vranSoftwareUpgradeEventSender.sendUpgradeJobStatusRequest(VranJobConstants.JOB_CANCEL, vranSoftwareUpgradeJobResponse.getJobId(), upgradePackageContext.getVnfmFdn(), eventAttributes);
    }

    private boolean isActivtityCompleted(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return ShmConstants.CANCELLED.equalsIgnoreCase(vranSoftwareUpgradeJobResponse.getResult()) || VranUprgradeConstants.PREPARE_COMPLETED.equals(vranSoftwareUpgradeJobResponse.getState())
                || VranUprgradeConstants.INITIALIZED.equalsIgnoreCase(vranSoftwareUpgradeJobResponse.getState());
    }

    @Override
    public String getActivityName() {
        return VranJobConstants.JOB_CANCEL;
    }

}