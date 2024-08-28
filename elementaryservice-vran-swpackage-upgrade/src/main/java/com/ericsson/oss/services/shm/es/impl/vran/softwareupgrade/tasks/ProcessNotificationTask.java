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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks;

import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.NeJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.task.common.ActivityStatusManager;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public abstract class ProcessNotificationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNotificationTask.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private TaskBase taskBase;

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ProcessNotificationFailureHandler processNotificationFailureHandler;

    @Inject
    private NeJobPropertiesPersistenceProvider neJobPropertiesPersistenceProvider;

    @Inject
    private JobLogsPersistenceProvider jobLogsPersistenceProvider;

    @Inject
    private ActivityStatusManager activityStatusManager;

    private ProcessNotificationTask nextProcessor;

    protected String activityName;

    public void perform(final String actionTriggered, final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {
        LOGGER.debug("ActivityJob ID - [{}] : Resolving next processor, current ativity is {}, last action triggered: {} and next processor is {} ", jobActivityInformation.getActivityJobId(),
                getActivityName(), actionTriggered, getNextProcessor() != null ? getNextProcessor().getActivityName() : getActivityName());

        if (getActivityName().equalsIgnoreCase(actionTriggered)) {
            LOGGER.debug("ActivityJob ID - [{}] : Action Triggered {} ", jobActivityInformation.getActivityJobId(), actionTriggered);

            processNotification(jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);
        } else if (getNextProcessor() != null) {
            LOGGER.debug("ActivityJob ID - [{}] : Next Processor Action Triggered {} ", jobActivityInformation.getActivityJobId(), getNextProcessor().getActivityName(), actionTriggered);
            getNextProcessor().perform(actionTriggered, jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);
        } else {
            LOGGER.warn("ActivityJob ID - [{}] : Unsupported Activity [{}] in handleAction() of Action Activity", jobActivityInformation.getActivityJobId(), jobActivityInformation.getActivityName(),
                    actionTriggered);
        }
    }

    public abstract void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext);

    public void processActivityActionNotification(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext,
            final JobEnvironment jobContext, final JobActivityInfo jobActivityInformation, final String operation) {
        try {
            neJobPropertiesPersistenceProvider.persistVnfJobId(upgradePackageContext, vranSoftwareUpgradeJobResponse, jobContext);
            jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, operation);

            trackActivityStatusOnVnfm(vranSoftwareUpgradeJobResponse, upgradePackageContext);
        } catch (final Exception e) {
            LOGGER.error("ActivityJob ID - [{}] : Failed to process the notification for {} activity. Reason : {}", vranSoftwareUpgradeJobResponse.getActivityJobId(),
                    vranSoftwareUpgradeJobResponse.getActivityName(), e.getMessage(), e);
            processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, vranSoftwareUpgradeJobResponse.getActivityName(), upgradePackageContext, jobActivityInformation);
        }
    }

    public void processActivityProgressNotification(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInformation, final String operation) {
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        final String state = vranSoftwareUpgradeJobResponse.getState();
        final JobEnvironment jobContext = upgradePackageContext.getJobEnvironment();
        final String nodeName = jobContext.getNodeName();

        LOGGER.debug("ActivityJob ID - [{}] : Progress Notification for activity {} for state {}  ", activityJobId, jobActivityInformation.getActivityName(), state);
        try {
            if (activityStatusManager.isActivityCompleted(operation, state)) {
                proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInformation, nodeName, operation);
            } else if (activityStatusManager.isActivityInProgress(jobActivityInformation.getActivityName(), state)) {
                jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, operation);
                trackActivityStatusOnVnfm(vranSoftwareUpgradeJobResponse, upgradePackageContext);
            } else {
                processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, vranSoftwareUpgradeJobResponse.getActivityName(), upgradePackageContext, jobActivityInformation);
            }
        } catch (final Exception e) {
            LOGGER.error("ActivityJob ID - [{}] : Exception occurred while processing Notification. Cause : {}", activityJobId, e.getMessage(), e);
            processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, activityName, upgradePackageContext, jobActivityInformation);
        }
    }

    public void proceedWithNextSteps(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInformation, final String nodeName, final String operation) {
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        LOGGER.debug("ActivityJob ID - [{}] : Progress Notification proceed with next steps for activity {} for operation {}  ", activityJobId, jobActivityInformation.getActivityName(), operation);

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, operation, jobProperties);

        recordActivitySucess(vranSoftwareUpgradeJobResponse, upgradePackageContext, nodeName, operation);

        taskBase.unSubscribeNotification(jobActivityInformation, upgradePackageContext, activityJobId);
        activityUtils.sendNotificationToWFS(upgradePackageContext.getJobEnvironment(), activityJobId, vranSoftwareUpgradeJobResponse.getActivityName(), processVariables);

        LOGGER.debug("ActivityJob ID - [{}] : {} activity has been completed successfully. Notifying to workflow service.", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
    }

    public void recordActivitySucess(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext, final String nodeName,
            final String operation) {
        final String jobLogMessage = notificationTaskUtils.prepareJobLogMessage(vranSoftwareUpgradeJobResponse, operation);
        final String jobEventName = notificationTaskUtils.assignJobEventNameBasedOnActivity(vranSoftwareUpgradeJobResponse.getActivityName());
        final String serviceName = notificationTaskUtils.assignServiceNameBasedOnActivity(vranSoftwareUpgradeJobResponse.getActivityName());

        activityUtils.recordEvent(jobEventName, nodeName, upgradePackageContext.getSoftwarePackageName(),
                activityUtils.additionalInfoForEvent(vranSoftwareUpgradeJobResponse.getActivityJobId(), nodeName, jobLogMessage));
        systemRecorder.recordCommand(serviceName, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upgradePackageContext.getSoftwarePackageName(),
                activityUtils.additionalInfoForCommand(vranSoftwareUpgradeJobResponse.getActivityJobId(), upgradePackageContext.getJobEnvironment().getNeJobId(), JobTypeEnum.UPGRADE));
    }

    public void trackActivityStatusOnVnfm(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext) {
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        final Map<String, Object> eventAttributes = notificationTaskUtils.prepareEventAttributes(upgradePackageContext, activityJobId);
        vranSoftwareUpgradeEventSender.sendUpgradeJobStatusRequest(vranSoftwareUpgradeJobResponse.getActivityName(), vranSoftwareUpgradeJobResponse.getJobId(), upgradePackageContext.getVnfmFdn(),
                eventAttributes);
    }

    public void recordNotification(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final long activityJobId, final UpgradePackageContext upgradePackageContext,
            final String jobEvent) {
        activityUtils.recordEvent(jobEvent, vranSoftwareUpgradeJobResponse.getNetworkElementName(), upgradePackageContext.getSoftwarePackageName(),
                activityUtils.additionalInfoForEvent(activityJobId, vranSoftwareUpgradeJobResponse.getNetworkElementName(), vranSoftwareUpgradeJobResponse.toString()));
    }

    public ProcessNotificationTask getNextProcessor() {
        return nextProcessor;
    }

    public void setNextProcessor(final ProcessNotificationTask nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    public String getActivityName() {
        return activityName;
    }

}
