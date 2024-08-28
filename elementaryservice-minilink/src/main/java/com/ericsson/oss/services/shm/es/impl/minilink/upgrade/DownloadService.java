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
import java.util.HashMap;
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
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.exception.SmrsServiceUnavailableException;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.xfSwGlobalState;
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

/**
 * This class facilitates the download of upgrade package of MINI-LINK-Indoor
 */
@EServiceQualifier("MINI_LINK_INDOOR.UPGRADE.download")
@ActivityInfo(activityName = "download", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DownloadService implements Activity, ActivityCallback {
    private static final String THIS_ACTIVITY = ActivityConstants.DOWNLOAD;
    private static final String REASON_FOR_FAILURE = "Node is not in the right state.";
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadService.class);

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

    @SuppressWarnings("incomplete-switch")
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
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
            if (miniLinkActivityUtil.isRAUPackage(activityJobId)) {
                precheckRAU(globalState, activityStepResult);
            } else {
                precheckSBL(activityJobId, globalState, activityStepResult);
            }
            switch (activityStepResult.getActivityResultEnum()) {
            case PRECHECK_SUCCESS_PROCEED_EXECUTION:
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.PRE_CHECK_SUCCESS, THIS_ACTIVITY));
                break;
            case PRECHECK_FAILED_SKIP_EXECUTION:
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, REASON_FOR_FAILURE));
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
                break;
            }
        } catch (final Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, UPGRADE_JOB, THIS_ACTIVITY, activityUtils.getJobEnvironment(activityJobId).getNodeName()), e);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            final String errorMessage = String.format(JobLogConstants.FAILURE_REASON, exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage);
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, errorMessage));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, new ArrayList<Map<String, Object>>());
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, DownloadService.class);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        final String nodeName = jobEnvironment.getNodeName();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        String xfSwObjectsFdn = null;
        try {
            LOGGER.info("Perform Download, activityJobId: {}", activityJobId);
            LOGGER.debug("neJobId: {}", activityUtils.getPoAttributes(activityJobId).get(ShmConstants.NE_JOB_ID));
            miniLinkActivityUtil.setSmrsFtpOnNode(activityJobId, nodeName);
            if (!miniLinkActivityUtil.isSmrsDetailFoundOnNode(activityJobId)) {
                throw new SmrsServiceUnavailableException("SMRS FtpEntry not Found on the node: " + nodeName);
            }
            xfSwObjectsFdn = miniLinkActivityUtil.getXfSwObjectsFdn(activityJobId);
            miniLinkActivityUtil.subscribeToMoNotifications(xfSwObjectsFdn, activityJobId, jobActivityInfo);
            final String packageName = miniLinkActivityUtil.getSwPkgName(activityJobId);
            if (miniLinkActivityUtil.isRAUPackage(packageName)) {
                // Update Commit type
                miniLinkActivityUtil.updateManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS,
                        Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_COMMIT_TYPE, MiniLinkConstants.xfswCommitType.operatorCommit.toString()));

                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, String.format(MiniLinkConstants.DOWNLOAD_PACKAGE, packageName, "Radio unit"), THIS_ACTIVITY));
                if (!miniLinkActivityUtil.updateXfSwLmUpgradeTable(activityJobId, xfSwObjectsFdn, packageName)) {
                    LOGGER.error("RAU module is not found, upgrade failed.");
                    jobLogList.add(activityUtils.createNewLogEntry(JobLogConstants.ACTION_FAILED + ". " + String.format(JobLogConstants.FAILURE_REASON, "Radio unit module was not found."),
                            JobLogLevel.ERROR.getLogLevel()));
                    miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, xfSwObjectsFdn, JobResult.FAILED, jobLogList, THIS_ACTIVITY);
                    miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
                } else {
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
                    miniLinkActivityUtil.setXfSwGlobalStateWithoutMediation(activityJobId, xfSwGlobalState.manualStarted);
                }
            } else { // SBL package
                updateBootTimeAndCommitType(activityJobId);
                jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, String.format(MiniLinkConstants.DOWNLOAD_PACKAGE, packageName, "Software baseline"), THIS_ACTIVITY));
                updateXfSwUpgradePreferences(activityJobId);
                updateXfSwReleaseEntry(activityJobId, packageName);
                updateXfSwReleaseEntryAdminStatus(activityJobId);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
                miniLinkActivityUtil.setXfSwGlobalStateWithoutMediation(activityJobId, xfSwGlobalState.sblStarted);
            }
        } catch (final Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, UPGRADE_JOB, THIS_ACTIVITY, nodeName), e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (exceptionMessage.isEmpty()) {
                exceptionMessage = e.getMessage();
            }
            jobLogList.add(activityUtils.createNewLogEntry(JobLogConstants.ACTION_FAILED + ". " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), JobLogLevel.ERROR.getLogLevel()));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, xfSwObjectsFdn, JobResult.FAILED, jobLogList,THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
        }
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        miniLinkActivityUtil.setJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, "true", activityJobId);
        LOGGER.debug("Inside miniLinkJobUtil.cancel() with activityJobId {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        final boolean isRAU = miniLinkActivityUtil.isRAUPackage(activityJobId);
        if (isRAU) {
            miniLinkActivityUtil.abortUpdateInXfSwLmUpgradeEntry(activityJobId);
        } else {
            abortUpdateInXfSwReleaseEntry(activityJobId);
        }

        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, THIS_ACTIVITY);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);

        if (isGlobalStateNoUpgrade(miniLinkActivityUtil.getGlobalState(activityJobId))) {
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, DownloadService.class);
            logCancelAndUnsubscribe(jobLogList, jobActivityInfo, miniLinkActivityUtil.getXfSwObjectsFdn(activityJobId));
        }
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();

        if (finalizeResult) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        }

        return activityStepResult;
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("In handle timeout with activity id {}", activityJobId);
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
        LOGGER.debug("MINI-LINK Upgrade download activity - processNotification with event type : {} ", eventType);
        if (!MiniLinkJobUtil.GOOD_EVENT_TYPES.contains(eventType)) {
            LOGGER.debug(String.format("MINI-LINK Upgrade Download activity - Discarding '%s' notification.", eventType.getEventType()));
            return;
        }
        final NotificationSubject notificationSubject = message.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final JobActivityInfo activityInfo = activityUtils.getActivityInfo(activityJobId, DownloadService.class);
        if (NotificationEventTypeEnum.AVC.equals(message.getNotificationEventType())) {
            processAVCNotification(message, activityInfo);
        } else if (NotificationEventTypeEnum.CREATE.equals(message.getNotificationEventType())) {
            processCreateNotification(message, activityInfo);
        }
    }

    private void updateXfSwUpgradePreferences(final Long activityJobId) {
        miniLinkActivityUtil.updateManagedObject(activityJobId, MiniLinkConstants.XF_SW_UPGRADE_PREFERENCES,
                Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_VERSION_CONTROL, MiniLinkConstants.xfSwVersionControl.enable.toString()));
    }

    private void abortUpdateInXfSwReleaseEntry(final Long activityJobId) {
        miniLinkActivityUtil.updateXfSwReleaseEntry(activityJobId,
                Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS, MiniLinkConstants.XfSwReleaseAdminStatus.upgradeAborted.toString()));
    }

    private void evaluateGlobalState(final JobActivityInfo jobActivityInfo, final DpsDataChangedEvent event, final List<String> globalState) {
        LOGGER.debug("Entering miniLinkJobUtil.evaluateGlobalState() with globalState: {}", globalState);

        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        final boolean isRAU = miniLinkActivityUtil.isRAUPackage(jobActivityInfo.getActivityJobId());
        // Check failure at the beginning
        if (isRAU) {
            if (miniLinkActivityUtil.isRAUUpgradeFailure(jobActivityInfo.getActivityJobId())) {
                miniLinkActivityUtil.abortUpdateInXfSwLmUpgradeEntry(jobActivityInfo.getActivityJobId());
                finishAsError(jobActivityInfo, event, globalState, jobLogList, isRAU);
                return;
            }
        }
        if (isReadyForActivate(isRAU, globalState)) {
            activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.SUCCESS, jobLogList, THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
        } else if (isDownloadStarted(globalState)) {
            activityUtils.addJobLog(MiniLinkConstants.DOWNLOAD_STARTED, JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
            jobUpdateService.readAndUpdateRunningJobAttributes(jobActivityInfo.getActivityJobId(), Collections.<Map<String, Object>> emptyList(), jobLogList);
        } else if (isSuccesfullyCancelled(jobActivityInfo, globalState)) {
            logCancelAndUnsubscribe(jobLogList, jobActivityInfo, event.getFdn());
        } else {
            finishAsError(jobActivityInfo, event, globalState, jobLogList, isRAU);
        }
    }

    private void logCancelAndUnsubscribe(final List<Map<String, Object>> jobLogList, final JobActivityInfo jobActivityInfo, final String fdn) {
        activityUtils.addJobLog("Download is properly cancelled on node.", JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, fdn, JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
    }

    private void finishAsError(final JobActivityInfo jobActivityInfo, final DpsDataChangedEvent event, final List<String> globalState, final List<Map<String, Object>> jobLogList, final boolean isRAU) {
        miniLinkActivityUtil.getErrorJobLog(jobLogList, isRAU, jobActivityInfo.getActivityJobId(), globalState);
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.ERROR.getLogLevel());
        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
    }

    private boolean isDownloadStarted(final List<String> globalState) {
        return globalState.contains(MiniLinkConstants.xfSwGlobalState.sblStarted.name()) || globalState.contains(MiniLinkConstants.xfSwGlobalState.manualStarted.name());
    }

    private boolean isGlobalStateNoUpgrade(final List<String> globalState) {
        return globalState != null && globalState.contains(MiniLinkConstants.xfSwGlobalState.noUpgrade.toString());
    }

    private boolean isJobCancelled(final long activityJobId) {
        try {
            final String jobProperty = miniLinkActivityUtil.fetchJobProperty(activityJobId, ActivityConstants.IS_CANCEL_TRIGGERED);
            return Boolean.parseBoolean(jobProperty);
        } catch (final ServerInternalException exception) {
            LOGGER.error("Exception occured in fetching job Property. Failure reason is : {}", exception);
            return false;
        }
    }

    private boolean isOperStatusCorrect(final String operStatus) {
        return operStatus != null
                && !(MiniLinkConstants.XfSwReleaseOperStatus.running.toString().equals(operStatus) || MiniLinkConstants.XfSwReleaseOperStatus.upgradeStarted.toString().equals(operStatus)
                        || MiniLinkConstants.XfSwReleaseOperStatus.testing.toString().equals(operStatus) || MiniLinkConstants.XfSwReleaseOperStatus.testingFromManual.toString().equals(operStatus) || MiniLinkConstants.XfSwReleaseOperStatus.upgradeFinished
                        .toString().equals(operStatus));
    }

    private boolean isReadyForActivate(final boolean isRAU, final List<String> globalState) {
        return isRAU && globalState.contains(MiniLinkConstants.xfSwGlobalState.manualWaitForActivate.name()) || !isRAU
                && globalState.contains(MiniLinkConstants.xfSwGlobalState.sblWaitForActivate.name());
    }

    private boolean isSuccesfullyCancelled(final JobActivityInfo jobActivityInfo, final List<String> globalState) {
        return isJobCancelled(jobActivityInfo.getActivityJobId()) && globalState.contains(MiniLinkConstants.xfSwGlobalState.noUpgrade.name());
    }

    private void precheckRAU(final List<String> globalState, final ActivityStepResult activityStepResult) {
        if (isGlobalStateNoUpgrade(globalState)) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } else {
            LOGGER.error("Precheck Step failed - xfSwGlobalState: {}", globalState);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
    }

    private void precheckSBL(final long activityJobId, final List<String> globalState, final ActivityStepResult activityStepResult) {
        final String operStatus = miniLinkActivityUtil.getXfSwReleaseOperStatus(activityJobId);
        if (isOperStatusCorrect(operStatus) && isGlobalStateNoUpgrade(globalState)) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } else {
            LOGGER.error("Precheck Step failed for activityJobId: {} - XfSwReleaseOperStatus: {}, xfSwGlobalState: {}", activityJobId, operStatus, globalState);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
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

    @SuppressWarnings({ "unchecked" })
    private void processCreateNotification(final Notification notification, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Process Create Notification() with notificationSubject: {}", notification.getNotificationSubject());
        final DpsObjectCreatedEvent event = (DpsObjectCreatedEvent) notification.getDpsDataChangedEvent();
        if (MiniLinkConstants.XF_SW_OBJECTS.equals(event.getType())) {
            final List<String> globalState = (List<String>) event.getAttributeValues().get(MiniLinkConstants.XF_SW_GLOBAL_STATE);
            evaluateGlobalState(jobActivityInfo, event, globalState);
            return;
        }
    }

    private void updateXfSwReleaseEntry(final Long activityJobId, final String swPkgName) {
        final String[] productNumberAndRevision = miniLinkActivityUtil.getProductNumberAndRevisionSBL(swPkgName);

        final Map<String, Object> xfSwReleaseEntryActionArguments = new HashMap<>();
        xfSwReleaseEntryActionArguments.put(MiniLinkConstants.XF_SW_RELEASE_PRODUCT_NUMBER, productNumberAndRevision[0]);
        xfSwReleaseEntryActionArguments.put(MiniLinkConstants.XF_SW_RELEASE_REVISION, productNumberAndRevision[1]);
        miniLinkActivityUtil.updateXfSwReleaseEntry(activityJobId, xfSwReleaseEntryActionArguments);
    }

    private void updateBootTimeAndCommitType(final Long activityJobId) {
        final Map<String, Object> attributesOfXfSwObjectsMO = new HashMap<>();
        attributesOfXfSwObjectsMO.put(MiniLinkConstants.XF_SW_COMMIT_TYPE, MiniLinkConstants.xfswCommitType.operatorCommit.toString());
        attributesOfXfSwObjectsMO.put(MiniLinkConstants.XF_SW_BOOT_TIME, null);
        miniLinkActivityUtil.updateManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS, attributesOfXfSwObjectsMO);
    }

    private void updateXfSwReleaseEntryAdminStatus(final Long activityJobId) {
        final Map<String, Object> xfSwReleaseEntryActionArguments = new HashMap<>();
        xfSwReleaseEntryActionArguments.put(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS, MiniLinkConstants.XfSwReleaseAdminStatus.upgradeStarted.toString());
        miniLinkActivityUtil.updateXfSwReleaseEntry(activityJobId, xfSwReleaseEntryActionArguments);
    }
}
