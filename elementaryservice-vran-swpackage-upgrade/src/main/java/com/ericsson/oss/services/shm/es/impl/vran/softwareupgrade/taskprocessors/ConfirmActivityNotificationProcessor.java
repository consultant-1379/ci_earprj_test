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

import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.*;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.*;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ConfirmActivityNotificationProcessor extends ProcessNotificationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmActivityNotificationProcessor.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VnfInformationProvider vnfInformationProvider;

    @Inject
    private TaskBase taskBase;

    @Inject
    private JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Inject
    private ProcessNotificationFailureHandler processNotificationFailureHandler;

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    @Override
    public void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {
        final long activityJobId = jobActivityInformation.getActivityJobId();
        final JobEnvironment jobContext = upgradePackageContext.getJobEnvironment();
        LOGGER.debug("ActivityJob ID - [{}] : Job notification received : {}", activityJobId, vranSoftwareUpgradeJobResponse);

        recordNotification(vranSoftwareUpgradeJobResponse, activityJobId, upgradePackageContext, VranJobEvents.CONFIRM_PROCESS_NOTIFICATION);

        LOGGER.debug("ActivityJob ID - [{}] : Confirm Service - Processing notification for activity [{}] with event type : [{}]", activityJobId, jobActivityInformation.getActivityName(),
                vranSoftwareUpgradeJobResponse.getFlowType());
        if (isConfirmInitiationSuccessResponse(vranSoftwareUpgradeJobResponse)) {
            if (notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)) {
                processActivityActionNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobContext, jobActivityInformation, VranUprgradeConstants.CONFIRM_OPERATION);
            } else if (notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)) {
                processActivityProgressNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInformation, VranUprgradeConstants.CONFIRM_OPERATION);
            } else {
                LOGGER.warn("Unsupported FlowType in processing notification");
            }
        } else if (isConfirmInitiationCancelResponse(vranSoftwareUpgradeJobResponse)) {
            LOGGER.debug("ActivityJob ID - [{}] : Processing Cancel action during Confirm activity of software upgrade on node", activityJobId);
            jobCancelActivityNotificationProcessor.processNotification(jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);
        } else {
            LOGGER.debug("ActivityJob ID - [{}] : Processing failure notification for activity [{}] of software upgrade on node", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
            processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, jobActivityInformation.getActivityName(), upgradePackageContext, jobActivityInformation);

        }
    }

    @Override
    public void proceedWithNextSteps(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInformation, final String nodeName, final String operation) {

        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        recordActivitySucess(vranSoftwareUpgradeJobResponse, upgradePackageContext, nodeName, VranJobEvents.CONFIRM_PROCESS_NOTIFICATION);

        taskBase.unSubscribeNotification(jobActivityInformation, upgradePackageContext, activityJobId);

        final String jobLogMessage = notificationTaskUtils.prepareJobLogMessage(vranSoftwareUpgradeJobResponse, VranUprgradeConstants.CONFIRM_OPERATION);
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, vranSoftwareUpgradeJobResponse.getNotificationReceivedTime(), JobLogLevel.INFO.toString()));

        final String previousVnfId = vnfInformationProvider.getVnfId(activityJobId, upgradePackageContext.getVnfId(), VranJobConstants.TO_VNF_ID, upgradePackageContext.getNodeName());
        LOGGER.debug("ActivityJob ID - [{}] : retrieved previousVnfId {} based on fromVnfId : {}", activityJobId, previousVnfId, upgradePackageContext.getVnfId());

        if (previousVnfId != null) {
            jobAttributesPersistenceProvider.persistJobLogs(vranSoftwareUpgradeJobResponse.getActivityJobId(), jobLogs);

            upgradePackageContext.setVnfId(previousVnfId);
            taskBase.performSoftwareUpgrade(activityJobId, upgradePackageContext, VranUprgradeConstants.DELETE_ACTIVITY, jobActivityInformation, ActivityConstants.CONFIRM);
        } else {
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(
                    String.format(VranJobLogMessageTemplate.FOUND_NO_UPGRADE_JOBS_FOR_VNFID, upgradePackageContext.getVnfId(), upgradePackageContext.getVnfmName()),
                    vranJobActivityUtil.incrementTime(null, 1).getTime(), JobLogLevel.INFO.toString()));
            jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(vranSoftwareUpgradeJobResponse.getActivityJobId(), jobProperties, jobLogs);

            activityUtils.sendNotificationToWFS(upgradePackageContext.getJobEnvironment(), activityJobId, vranSoftwareUpgradeJobResponse.getActivityName(), processVariables);
            LOGGER.debug("ActivityJob ID - [{}] : {} activity has been completed successfully. Notifying to workflow service.", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
        }

    }

    private boolean isConfirmInitiationSuccessResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse);
    }

    private boolean isConfirmInitiationCancelResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return vranSoftwareUpgradeJobResponse != null && ShmConstants.CANCELLED.equalsIgnoreCase(vranSoftwareUpgradeJobResponse.getResult());
    }

    @Override
    public String getActivityName() {
        return ActivityConstants.CONFIRM;
    }
}
