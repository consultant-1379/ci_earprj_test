/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.service;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_JOB_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.shm.models.UpgradeJobTaskRequest;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper.SoftwareUpgradeJobService;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper.UpgradeJobInformation;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks.NotificationTask;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks.PrecheckTask;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class performs download activity of Software Upgrade job on Mini Link Outdoor nodes.
 *
 * @author NightsWatch
 *
 */
@EServiceQualifier("MINI_LINK_OUTDOOR.UPGRADE.confirm")
@ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
@Stateless
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmService.class);

    @Inject
    private PrecheckTask precheckTask;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private SoftwareUpgradeJobService softwareUpgradeActivityService;

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> eventSender;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private HandleTimeoutTask handleTimeoutTask;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NotificationTask notificationTask;

    @Inject
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Inject
    protected SystemRecorder systemRecorder;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return precheckTask.activityPreCheck(activityJobId, jobActivityInfo.getActivityName());
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Executing Mini-Link Outdoor Confirm activity of software upgrade on node.", activityJobId);
        NEJobStaticData neJobStaticData = null;
        UpgradeJobInformation upgradeInformation = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final String activityName = jobActivityInfo.getActivityName();
        try {
            upgradeInformation = softwareUpgradeActivityService.buildUpgradeInformation(activityJobId);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            if (upgradeInformation == null) {
                LOGGER.error("Unable to fetch the required information of the activityId {} ", activityJobId);
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, activityName), JobLogLevel.INFO.getLogLevel()));
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, Collections.<String, Object> emptyMap());
                return;
            }

            final String nodeName = upgradeInformation.getNeName();
            final String nodeFdn = upgradeInformation.getNodeFdn();
            LOGGER.debug("nodeFdn : {}, activityName : {}", nodeFdn, activityName);
            final String taskId = nodeFdn + "_" + activityName;
            activityUtils.subscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, activityName), activityJobId, jobActivityInfo);
            final UpgradeJobTaskRequest request = new UpgradeJobTaskRequest(nodeFdn, taskId, activityName, upgradeInformation.getFileName(), upgradeInformation.getProductRevision(),
                    upgradeInformation.getProductNumber());
            eventSender.send(request);
            systemRecorder.recordCommand(SHMEvents.UPGRADE_EXECUTE, CommandPhase.STARTED, nodeName, nodeFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
        } catch (final Exception exception) {
            LOGGER.error("failure in executing  Mini-Link Outdoor Confirm activity of software upgrade on node : {}", exception);
        }
    }

    @Override
    public void processNotification(final Notification message) {
        LOGGER.info("ActivityJob ID - [{}] : processNotification of confirm activity of software upgrade on Mini Link Outdoor node.", message);
        final SHMCommonCallBackNotificationJobProgressBean notification = (SHMCommonCallBackNotificationJobProgressBean) message;
        final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_JOB_ID);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        notificationTask.processRecivedNotification(message, Constants.CONFIRM, jobActivityInformation);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Handling Mini-Link Outdoor Confirm Activity of software upgrade in timeout.", activityJobId);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return handleTimeoutTask.handleTimeout(jobActivityInformation);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        activityUtils.addJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, "true", jobPropertyList);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, null);
        LOGGER.debug("cancel() triggered for activityJobId {}", activityJobId);
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
            activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, jobActivityInfo.getActivityName()), activityJobId,
                    jobActivityInfo);
        } catch (final Exception exception) {
            LOGGER.error("failure in executing Mini-Link Outdoor Confirm activity of software upgrade on node : {}", exception);
        }
        activityUtils.logCancelledByUser(jobLogList, neJobStaticData, Constants.CONFIRM);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("ActivityJob ID - [{}] : Processing timeout for Mini-Link Outdoor cancel action", activityJobId);
        return new ActivityStepResult();
    }

}
