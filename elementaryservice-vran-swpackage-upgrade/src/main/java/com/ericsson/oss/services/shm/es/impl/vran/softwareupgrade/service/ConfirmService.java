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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ConfirmActivityCancelProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ConfirmActivityExecuteProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ConfirmActivityNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.DeleteActivityNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.JobCancelActivityNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.PrecheckTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

/**
 * This class performs Confirm activity of Software Upgrade on vRAN nodes.
 *
 * @author xgudpra
 *
 */
@EServiceQualifier("vRAN.UPGRADE.confirm")
@ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private PrecheckTask precheckTask;

    @Inject
    private ConfirmActivityExecuteProcessor confirmActivityExecuteProcessor;

    @Inject
    private HandleTimeoutTask handleTimeoutTask;

    @Inject
    private ConfirmActivityCancelProcessor confirmActivityCancelProcessor;

    @Inject
    private TaskBase taskBase;

    @Inject
    private DeleteActivityNotificationProcessor deleteActivityNotificationProcessor;

    @Inject
    private JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Inject
    private ConfirmActivityNotificationProcessor confirmActivityNotificationProcessor;

    @Inject
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Entered precheck of Confirm activity of software upgrade on node.", activityJobId);
        return precheckTask.activityPreCheck(activityJobId, ActivityConstants.CONFIRM);
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Executing Confirm activity of software upgrade on node.", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        confirmActivityExecuteProcessor.executeTask(jobActivityInfo);
    }

    @Override
    public void processNotification(final Notification message) {
        final VranNotificationJobProgressBean notification = (VranNotificationJobProgressBean) message;
        final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse = notification.getVranNotification();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        LOGGER.info("ActivityJob ID - [{}] : Notification received from mediation for Confirm activity is {}", activityJobId, vranSoftwareUpgradeJobResponse);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final UpgradePackageContext upgradePackageContext = taskBase.buildUpgradeJobContext(activityJobId);
        final String actionTriggered = upgradePackageContext.getActionTriggered();

        final ProcessNotificationTask confirmNotificationActivities = buildChainOfActivities();
        confirmNotificationActivities.perform(actionTriggered, jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Handling Confirm Activity of software upgrade in timeout.", activityJobId);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return handleTimeoutTask.handleTimeout(jobActivityInformation);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Processing cancel action", activityJobId);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final UpgradePackageContext upgradePackageContext = vranUpgradeJobContextBuilder.build(activityJobId);

        if (lastActionTriggeredIsConfirm(upgradePackageContext)) {
            return confirmActivityCancelProcessor.processCancelForUnSupportedActivity(jobActivityInformation);
        } else {
            //This will handle if cancel is triggered when confirm activity selected as Manual execution.  
            return confirmActivityCancelProcessor.processCancelForSupportedActivity(jobActivityInformation);
        }

    }

    private boolean lastActionTriggeredIsConfirm(final UpgradePackageContext upgradePackageContext) {
        return upgradePackageContext.getActionTriggered() != null && ActivityConstants.CONFIRM.equals(upgradePackageContext.getActionTriggered());
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("ActivityJob ID - [{}] : Processing timeout for cancel action", activityJobId);
        return new ActivityStepResult();
    }

    private ProcessNotificationTask buildChainOfActivities() {
        final ProcessNotificationTask confirmActivtityProcessor = confirmActivityNotificationProcessor;
        final ProcessNotificationTask deleteActivityProcesor = deleteActivityNotificationProcessor;
        final ProcessNotificationTask jobCancelProcessor = jobCancelActivityNotificationProcessor;

        confirmActivtityProcessor.setNextProcessor(deleteActivityProcesor);
        deleteActivityProcesor.setNextProcessor(jobCancelProcessor);

        return confirmActivtityProcessor;
    }

}
