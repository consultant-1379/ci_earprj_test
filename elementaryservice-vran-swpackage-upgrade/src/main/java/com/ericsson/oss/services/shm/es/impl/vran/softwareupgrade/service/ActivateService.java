/*------------------------------------------------------------------------------
 *******************************************************************************
0 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service;

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.*;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.*;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

/**
 * This class performs Activate activity of Software Upgrade job on vRAN nodes.
 *
 * @author xgudpra
 *
 */
@EServiceQualifier("vRAN.UPGRADE.activate")
@ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ActivateService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateService.class);

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private PrecheckTask precheckTask;

    @Inject
    private ActivateActivityExecuteProcessor activateActivityExecuteTask;

    @Inject
    private HandleTimeoutTask handleTimeoutTask;

    @Inject
    private ActivateActivityCancelProcessor activateActivityCancelProcessor;

    @Inject
    private TaskBase taskBase;

    @Inject
    private JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Inject
    private ActivateActivityNotificationProcessor activateActivityNotificationProcessor;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : precheck of Activate activity of software upgrade on VRAN node.", activityJobId);
        return precheckTask.activityPreCheck(activityJobId, ActivityConstants.ACTIVATE);
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Executing Activate activity of software upgrade on node.", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        activateActivityExecuteTask.executeTask(jobActivityInfo);
    }

    @Override
    public void processNotification(final Notification message) {
        final VranNotificationJobProgressBean notification = (VranNotificationJobProgressBean) message;
        final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse = notification.getVranNotification();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        LOGGER.info("ActivityJob ID - [{}] : Notification received from mediation for Activate activity is {}", activityJobId, vranSoftwareUpgradeJobResponse);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final UpgradePackageContext upgradePackageContext = taskBase.buildUpgradeJobContext(activityJobId);
        final String actionTriggered = upgradePackageContext.getActionTriggered();

        final ProcessNotificationTask activateNotificationActivities = buildChainOfActivities();
        activateNotificationActivities.perform(actionTriggered, jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Handling Activate Activity of software upgrade in timeout.", activityJobId);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return handleTimeoutTask.handleTimeout(jobActivityInformation);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("ActivityJob ID - [{}] : Processing cancel action", activityJobId);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return activateActivityCancelProcessor.processCancelForSupportedActivity(jobActivityInformation);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("ActivityJob ID - [{}] : Processing timeout for cancel action", activityJobId);
        return new ActivityStepResult();
    }

    private ProcessNotificationTask buildChainOfActivities() {
        final ProcessNotificationTask activateActivityProcessor = activateActivityNotificationProcessor;
        final ProcessNotificationTask jobCancelActivityProcessor = jobCancelActivityNotificationProcessor;
        activateActivityProcessor.setNextProcessor(jobCancelActivityProcessor);
        return activateActivityProcessor;
    }

}
