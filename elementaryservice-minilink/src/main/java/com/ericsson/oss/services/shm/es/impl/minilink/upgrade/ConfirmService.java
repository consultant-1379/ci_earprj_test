/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.minilink.upgrade;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.UPGRADE_JOB;

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

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
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
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_INDOOR.UPGRADE.confirm")
@ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmService implements Activity, ActivityCallback {
    private static final String THIS_ACTIVITY = ActivityConstants.CONFIRM;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private MiniLinkActivityUtil miniLinkActivityUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Precheck started for {} activity.", THIS_ACTIVITY);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY));
            final List<String> globalState = miniLinkActivityUtil.getGlobalState(activityJobId);
            final String xfSwCommitType = miniLinkActivityUtil.getCommitType(activityJobId);
            final boolean isRAU = miniLinkActivityUtil.isRAUPackage(activityJobId);

            if (!miniLinkActivityUtil.isRAUPackage(activityJobId)) {
                final Integer activeRelease = miniLinkActivityUtil.getActiveRelease(activityJobId);
                miniLinkActivityUtil.setJobProperty(ShmConstants.KEY_ACTIVE_RELEASE, activeRelease.toString(), activityJobId);
            }
            if (rauPackageGlobalStateIsNotManualWaitForCommit(isRAU, globalState) || !MiniLinkConstants.xfswCommitType.operatorCommit.toString().equals(xfSwCommitType)
                    || sblPackageGlobalStateIsNotSblWaitForCommit(isRAU, globalState)) {
                LOGGER.error("Precheck Step failed: xfSwGlobalState: {}, xfSwCommitType:{}", globalState, xfSwCommitType);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY,
                        String.format("Precheck Step failed: xfSwGlobalState: %s, xfSwCommitType: %s", globalState, xfSwCommitType)));
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.PRE_CHECK_SUCCESS, THIS_ACTIVITY));
            }
        } catch (final Exception exception) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, UPGRADE_JOB, THIS_ACTIVITY, activityUtils.getJobEnvironment(activityJobId).getNodeName()), exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            final String errorMessage = String.format(JobLogConstants.FAILURE_REASON, exceptionMessage.isEmpty() ? exception.getMessage() : exceptionMessage);
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, errorMessage));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Execute started for {} activity.", THIS_ACTIVITY);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.EXECUTING, THIS_ACTIVITY));
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, ConfirmService.class);
        String xfSwObjectsFdn = null;
        try {
            xfSwObjectsFdn = miniLinkActivityUtil.getXfSwObjectsFdn(activityJobId);

            miniLinkActivityUtil.subscribeToMoNotifications(xfSwObjectsFdn, activityJobId, jobActivityInfo);

            if (miniLinkActivityUtil.isRAUPackage(activityJobId)) {
                miniLinkActivityUtil.updateXfSwLmUpgradeTable(activityJobId, xfSwObjectsFdn,
                        Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS, MiniLinkConstants.xfSwLmUpgradeAdminStatus.activeAndRunning.toString()));
            } else {
                miniLinkActivityUtil.updateXfSwReleaseEntry(activityJobId,
                        Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS, MiniLinkConstants.XfSwReleaseAdminStatus.activeAndRunning.toString()));
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        } catch (final Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, UPGRADE_JOB, THIS_ACTIVITY, activityUtils.getJobEnvironment(activityJobId).getNodeName()), e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (exceptionMessage.isEmpty()) {
                exceptionMessage = e.getMessage();
            }
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTION_TRIGGER_FAILED, THIS_ACTIVITY) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage),
                    JobLogLevel.INFO.getLogLevel()));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, xfSwObjectsFdn, JobResult.FAILED, jobLogList, THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
        }
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Timeout happened for activity: {}", THIS_ACTIVITY);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, getClass());
        final String jobLogMessage = String.format(JobLogConstants.TIMEOUT, THIS_ACTIVITY);
        miniLinkActivityUtil.unsubscribeFromMoNotifications(miniLinkActivityUtil.getXfSwObjectsFdn(activityJobId), activityJobId, jobActivityInfo);
        activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.WARN.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    @Override
    public void processNotification(final Notification message) {
        final NotificationEventTypeEnum eventType = message.getNotificationEventType();
        LOGGER.debug("MINI-LINK Upgrade confirm activity - processNotification with event type : {} ", eventType);
        if (!MiniLinkJobUtil.GOOD_EVENT_TYPES.contains(eventType)) {
            LOGGER.debug(String.format("MINI-LINK Upgrade confirm activity - Discarding '%s' notification.", message.getNotificationEventType().getEventType()));
            return;
        }
        final NotificationSubject notificationSubject = message.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final JobActivityInfo activityInfo = activityUtils.getActivityInfo(activityJobId, ConfirmService.class);
        if (NotificationEventTypeEnum.AVC.equals(message.getNotificationEventType())) {
            processAVCNotification(message, activityInfo);
        } else {
            processCreateNotification(message, activityInfo);
        }
    }

    private void evaluateGlobalState(final JobActivityInfo jobActivityInfo, final DpsDataChangedEvent event, final List<String> globalState) {
        LOGGER.debug("ConfirmService.evaluateGlobalState started, event: {}, globalState: {}", event, globalState);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        if (globalState.contains(MiniLinkConstants.xfSwGlobalState.noUpgrade.toString())) {
            if (miniLinkActivityUtil.isRAUPackage(jobActivityInfo.getActivityJobId())) {
                if (miniLinkActivityUtil.isXfSwBoardTableUpdated(jobActivityInfo.getActivityJobId(), jobLogList)) {
                    activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
                    miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.SUCCESS, jobLogList, THIS_ACTIVITY);
                    miniLinkActivityUtil.sendNotification(jobActivityInfo,THIS_ACTIVITY);
                    return;
                } else {
                    miniLinkActivityUtil.getErrorJobLog(jobLogList, true, jobActivityInfo.getActivityJobId(), globalState);
                }
            } else {
                final int formerActiveRelease = Integer.parseInt(miniLinkActivityUtil.fetchJobProperty(jobActivityInfo.getActivityJobId(), ShmConstants.KEY_ACTIVE_RELEASE));
                final int activeRelease = miniLinkActivityUtil.getActiveRelease(jobActivityInfo.getActivityJobId());
                LOGGER.debug("Active release has changed from {} to {}.", formerActiveRelease, activeRelease);

                if (formerActiveRelease != activeRelease) {
                    activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
                    miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.SUCCESS, jobLogList, THIS_ACTIVITY);
                    miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
                    return;
                } else {
                    activityUtils.addJobLog("Active release has not changed.", JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.WARN.getLogLevel());
                }
                final String operStatus = miniLinkActivityUtil.getXfSwReleaseOperStatus(jobActivityInfo.getActivityJobId());
                activityUtils.addJobLog(String.format("Unexpected xfSwGlobalState: %s xfSwReleaseOperStatus: %s", globalState, operStatus), JobLogType.SYSTEM.getLogType(), jobLogList,
                        JobLogLevel.WARN.getLogLevel());
            }
        }
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.ERROR.getLogLevel());
        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        miniLinkActivityUtil.sendNotification(jobActivityInfo,THIS_ACTIVITY);

    }

    @SuppressWarnings("unchecked")
    private void processAVCNotification(final Notification notification, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Process AVC Notification() with notificationSubject: {}", notification.getNotificationSubject());
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();
        for (final AttributeChangeData change : event.getChangedAttributes()) {
            if (MiniLinkConstants.XF_SW_GLOBAL_STATE.equals(change.getName())) {
                final List<String> globalState = (List<String>) change.getNewValue();
                evaluateGlobalState(jobActivityInfo, event, globalState);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processCreateNotification(final Notification notification, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Process Create Notification() with notificationSubject: {}", notification.getNotificationSubject());
        final DpsObjectCreatedEvent event = (DpsObjectCreatedEvent) notification.getDpsDataChangedEvent();
        if (MiniLinkConstants.XF_SW_OBJECTS.equals(event.getType())) {
            final List<String> globalState = (List<String>) event.getAttributeValues().get(MiniLinkConstants.XF_SW_GLOBAL_STATE);
            evaluateGlobalState(jobActivityInfo, event, globalState);
        }
    }

    private boolean rauPackageGlobalStateIsNotManualWaitForCommit(final boolean isRAU, final List<String> globalState) {
        return isRAU && (globalState == null || !globalState.contains(MiniLinkConstants.xfSwGlobalState.manualWaitForCommit.toString()));
    }

    private boolean sblPackageGlobalStateIsNotSblWaitForCommit(final boolean isRAU, final List<String> globalState) {
        return !isRAU && (globalState == null || !globalState.contains(MiniLinkConstants.xfSwGlobalState.sblWaitForCommit.toString()));
    }

}
