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
package com.ericsson.oss.services.shm.es.impl.minilink.licensing;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.*;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfLicenseInstallAdminStatus.DOWNLOAD_INSTALL;

import java.util.*;

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.*;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.*;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.upgrade.MiniLinkActivityUtil;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.*;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

@EServiceQualifier("MINI_LINK_INDOOR.LICENSE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstallLicenseKeyFileService implements Activity, ActivityCallback {
    private static final String THIS_ACTIVITY = ActivityConstants.INSTALL_LICENSE;
    private static final String NODE_IS_NOT_IN_RIGHT_STATE = "Node is not in the right state.";
    private static final double LKF_ENABLING_PROGRESS_PERCENTAGE = 90.0;
    private static final double LKF_INSTALLING_PROGRESS_PERCENTAGE = 60.0;
    private static final double LKF_VALIDATION_PROGRESS_PERCENTAGE = 30.0;
    private static final double LKF_DOWNLOAD_PROGRESS_PERCENTAGE = 40.0;
    private static final String INSTALLSTATE_CHANGED_TO = "Installstate changed to: {} on Node: {} for activityID: {}";
    private static final String INVALID_LKF = "The node considers the LKF file invalid. Error code is: %s";
    private static final String NO_SPACE_IN_RMM = "There is no more space in RMM for the license file and is discarded. Error Code: \"%s\"";
    private static final String RMM_NOT_AVAILABLE = "The RMM is not available and is unable to store it in RMM. The file is discarded. Error Code: \"%s\"";
    private static final String LKF_DOWNLOAD_STARTED = "lkfDownloadStarted";

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallLicenseKeyFileService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private MiniLinkActivityUtil miniLinkActivityUtil;

    @Inject
    private LicenseUtil licenseUtil;

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("Precheck started for {} activity. activityJobId: {}, nodeName: {}", THIS_ACTIVITY, activityJobId, nodeName);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY));
        jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY));
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final String licenseInstallState = licenseUtil.getLicenseInstallOperStatus(activityJobId);
            precheckLicenseInstall(licenseInstallState, activityStepResult, activityJobId, nodeName);
            switch (activityStepResult.getActivityResultEnum()) {
                case PRECHECK_SUCCESS_PROCEED_EXECUTION:
                    jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.PRE_CHECK_SUCCESS, THIS_ACTIVITY));
                    break;
                case PRECHECK_FAILED_SKIP_EXECUTION:
                    jobLogList.add(
                            miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, NODE_IS_NOT_IN_RIGHT_STATE));
                    jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
                    break;
                default:
                    break;
            }
        } catch (final Exception exception) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, LICENSE_JOB, THIS_ACTIVITY,
                    activityUtils.getJobEnvironment(activityJobId).getNodeName()), exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            final String errorMessage = String.format(JobLogConstants.FAILURE_REASON,
                    exceptionMessage.isEmpty() ? exception.getMessage() : exceptionMessage);
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, errorMessage));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
        }
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(getNeJobId(activityJobId));
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Execute started for {} Install License Key File  .", THIS_ACTIVITY);
        LOGGER.debug("neJobId: {}", activityUtils.getPoAttributes(activityJobId).get(ShmConstants.NE_JOB_ID));
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.INFO, JobLogConstants.EXECUTING, THIS_ACTIVITY));
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
        String xfLicenseInstallObjectsFdn = null;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class);
        final String nodeName = jobEnvironment.getNodeName();
        try {
            licenseUtil.setSmrsFtpOnNode(activityJobId, nodeName);
            xfLicenseInstallObjectsFdn = licenseUtil.getXfLicenseInstallObjectsFdn(activityJobId);
            miniLinkActivityUtil.subscribeToMoNotifications(xfLicenseInstallObjectsFdn, activityJobId, jobActivityInfo);
            final Map<String, String> licenseFile = jobPropertyUtils.getPropertyValue(Collections.singletonList(MiniLinkConstants.LICENSE_FILEPATH),
                    activityUtils.getMainJobAttributes(activityJobId), nodeName);
            final String licenseFilePath = licenseFile.get(MiniLinkConstants.LICENSE_FILEPATH);
            final String licenseFileName = licenseFilePath.replace(MiniLinkConstants.SMRS_PATH, "");
            LOGGER.info("License File {} is to install on {}: ", licenseFileName, nodeName);

            miniLinkDps.updateManagedObjectAttribute(nodeName, XF_LICENSE_INSTALL_OBJECTS, XF_LICENSE_INSTALL_FILE_NAME, licenseFileName);
            miniLinkDps.updateManagedObjectAttribute(nodeName, XF_LICENSE_INSTALL_OBJECTS, XF_LICENSE_INSTALL_ADMIN_STATUS,
                    DOWNLOAD_INSTALL.getStatusValue());
            miniLinkDps.setXfLicenseInstallOperStatusWithoutMediation(nodeName, LKF_DOWNLOAD_STARTED);
        } catch (final Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, LICENSE_JOB, THIS_ACTIVITY,
                    activityUtils.getJobEnvironment(activityJobId).getNodeName()), e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (exceptionMessage.isEmpty()) {
                exceptionMessage = e.getMessage();
            }
            jobLogList.add(activityUtils.createNewLogEntry(
                    String.format(JobLogConstants.UNABLE_TO_TRIGGER_ACTIVITY_FAILED_REASON, THIS_ACTIVITY, exceptionMessage),
                    JobLogLevel.INFO.getLogLevel()));
            jobLogList.add(miniLinkActivityUtil.createNewLogEntry(JobLogLevel.ERROR, JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY));
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, xfLicenseInstallObjectsFdn, JobResult.FAILED, jobLogList, THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
        }
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(getNeJobId(activityJobId));
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        miniLinkActivityUtil.setJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, "true", activityJobId);
        LOGGER.debug("cancel() triggered for activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
        } catch (JobDataNotFoundException e) {
           LOGGER.error("NE job static data not found in neJob cache and failed to get from DPS. {}", e);
        }
        activityUtils.logCancelledByUser(jobLogList, neJobStaticData, THIS_ACTIVITY);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, 0.0);

        if (isOperStatusNoInstall(licenseUtil.getLicenseInstallOperStatus(activityJobId))) {
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, getClass());
            logCancelAndUnsubscribe(jobLogList, jobActivityInfo, licenseUtil.getXfLicenseInstallObjectsFdn(activityJobId));
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
        LOGGER.info("Timeout happened for activity: {}", THIS_ACTIVITY);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, getClass());
        final String jobLogMessage = String.format(JobLogConstants.TIMEOUT, THIS_ACTIVITY);
        miniLinkActivityUtil.unsubscribeFromMoNotifications(licenseUtil.getXfLicenseInstallObjectsFdn(activityJobId), activityJobId, jobActivityInfo);
        activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.WARN.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList,
                HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(getNeJobId(activityJobId));
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    @Override
    public void processNotification(final Notification message) {
        final NotificationEventTypeEnum eventType = message.getNotificationEventType();
        LOGGER.debug("MINI-LINK License activate activity - processNotification with event type : {} ", eventType);
        if (!MiniLinkJobUtil.GOOD_EVENT_TYPES.contains(eventType)) {
            return;
        }
        final NotificationSubject notificationSubject = message.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final JobActivityInfo activityInfo = activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class);

        if (NotificationEventTypeEnum.AVC.equals(message.getNotificationEventType())) {
            processAVCNotification(message, activityInfo);
        }
    }

    private void logCancelAndUnsubscribe(final List<Map<String, Object>> jobLogList, final JobActivityInfo jobActivityInfo, final String fdn) {
        activityUtils.addJobLog("License install is properly cancelled on node.", JobLogType.SYSTEM.getLogType(), jobLogList,
                JobLogLevel.INFO.getLogLevel());
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, THIS_ACTIVITY), JobLogType.SYSTEM.getLogType(),
                jobLogList, JobLogLevel.INFO.getLogLevel());
        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, fdn, JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
    }

    private void precheckLicenseInstall(final String licenseInstallState, final ActivityStepResult activityStepResult, final long activityJobId, final String nodeName) {
        if (isOperStatusNoInstall(licenseInstallState)) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } else {
            LOGGER.error("Precheck Step failed - xfLicenseInstallOperStatus: {} activityJobId: {}, nodeName: {}", licenseInstallState, activityJobId, nodeName);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
    }

    private boolean isOperStatusNoInstall(final String licenseInstallState) {
        return licenseInstallState != null && !(isNoInstall(licenseInstallState));
    }

    private boolean isNoInstall(final String licenseInstallState) {
        return (licenseInstallState.equals(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_DOWNLOAD_STARTED.toString()))
                || (licenseInstallState.equals(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_VALIDATION_STARTED.toString()))
                || (licenseInstallState.equals(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_INSTALLING_ON_RMM.toString()))
                || (licenseInstallState.equals(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_ENABLING.toString()));
    }

    private void evaluateGlobalState(final JobActivityInfo jobActivityInfo, final DpsDataChangedEvent event, final String installState) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> neJobAttributes = jobEnvironment.getNeJobAttributes();
        final String nodeName = activityUtils.getJobEnvironment(jobActivityInfo.getActivityJobId()).getNodeName();
        LOGGER.trace("neJobAttributes: {} for nodeName: {}", neJobAttributes, nodeName);
        LOGGER.debug("evaluateGlobalState started, event: {}, installState: {}, activityId: {}, nodeName: {}", event, installState, activityJobId,
                nodeName);
        if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_INSTALL_FINISHED.getStatus())) {
            activityUtils.addJobLog("The license file is downloaded,stored in RMM successfully and is currently active.",
                    JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.SUCCESS, jobLogList, THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo,THIS_ACTIVITY);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_DOWNLOAD_STARTED.getStatus())) {
            activityUtils.addJobLog("The license file is being downloaded from the FTP server", JobLogType.SYSTEM.getLogType(), jobLogList,
                    JobLogLevel.INFO.getLogLevel());
            LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, activityJobId);
            jobUpdateService.readAndUpdateRunningJobAttributes(jobActivityInfo.getActivityJobId(), Collections.<Map<String, Object>> emptyList(),
                    jobLogList, LKF_DOWNLOAD_PROGRESS_PERCENTAGE);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_VALIDATION_STARTED.getStatus())) {
            activityUtils.addJobLog("The license file is being validated", JobLogType.SYSTEM.getLogType(), jobLogList,
                    JobLogLevel.INFO.getLogLevel());
            LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, activityJobId);
            jobUpdateService.readAndUpdateRunningJobAttributes(jobActivityInfo.getActivityJobId(), Collections.<Map<String, Object>> emptyList(),
                    jobLogList, LKF_VALIDATION_PROGRESS_PERCENTAGE);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_INSTALLING_ON_RMM.getStatus())) {
            activityUtils.addJobLog("The license file is being stored in RMM", JobLogType.SYSTEM.getLogType(), jobLogList,
                    JobLogLevel.INFO.getLogLevel());
            LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, jobActivityInfo.getActivityJobId());
            jobUpdateService.readAndUpdateRunningJobAttributes(jobActivityInfo.getActivityJobId(), Collections.<Map<String, Object>> emptyList(),
                    jobLogList, LKF_INSTALLING_PROGRESS_PERCENTAGE);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.LKF_ENABLING.getStatus())) {
            activityUtils.addJobLog("The license file is being activated", JobLogType.SYSTEM.getLogType(), jobLogList,
                    JobLogLevel.INFO.getLogLevel());
            LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, jobActivityInfo.getActivityJobId());
            jobUpdateService.readAndUpdateRunningJobAttributes(jobActivityInfo.getActivityJobId(), Collections.<Map<String, Object>> emptyList(),
                    jobLogList, LKF_ENABLING_PROGRESS_PERCENTAGE);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.ERROR_RMM_UNAVAILABLE.getStatus())) {
            activityUtils.addJobLog(String.format(RMM_NOT_AVAILABLE, installState), JobLogType.SYSTEM.getLogType(), jobLogList,
                    JobLogLevel.ERROR.getLogLevel());
            LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, activityJobId);
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.FAILED, jobLogList, THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo,THIS_ACTIVITY);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.ERROR_NO_SPACE_ON_RMM.getStatus())) {
            activityUtils.addJobLog(String.format(NO_SPACE_IN_RMM, installState), JobLogType.SYSTEM.getLogType(), jobLogList,
                    JobLogLevel.ERROR.getLogLevel());
            LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, activityJobId);
            miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.FAILED, jobLogList, THIS_ACTIVITY);
            miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.UNKNOWN_VERSION.getStatus())) {
            finishwithErrorCode(event, installState, jobActivityInfo, jobLogList);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.UNKNOWN_SIGNATURETYPE.getStatus())) {
            finishwithErrorCode(event, installState, jobActivityInfo, jobLogList);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.UNKNOWN_FINGERPRINT_METHOD.getStatus())) {
            finishwithErrorCode(event, installState, jobActivityInfo, jobLogList);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.UNKNOWN_FINGERPRINT.getStatus())) {
            finishwithErrorCode(event, installState, jobActivityInfo, jobLogList);
        } else if (installState.contains(MiniLinkConstants.xfLicenseInstallOperStatus.ERROR_CORRUPT_SIGNATURE.getStatus())) {
            finishwithErrorCode(event, installState, jobActivityInfo, jobLogList);
        } else {
            finishAsError(jobActivityInfo, event, installState, jobLogList);
        }
    }

    private void finishAsError(final JobActivityInfo jobActivityInfo, final DpsDataChangedEvent event, final String installState,
                               final List<Map<String, Object>> jobLogList) {
        activityUtils.addJobLog(
                String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, THIS_ACTIVITY, installState),
                JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.ERROR.getLogLevel());

        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);
    }

    private void finishwithErrorCode(final DpsDataChangedEvent event, final String installState, final JobActivityInfo jobActivityInfo,
                                                            final List<Map<String, Object>> jobLogList) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(jobActivityInfo.getActivityJobId());
        final String nodeName = jobEnvironment.getNodeName();
        activityUtils.addJobLog(
                String.format(INVALID_LKF, installState),
                JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.ERROR.getLogLevel());
        LOGGER.debug(INSTALLSTATE_CHANGED_TO, installState, nodeName, jobActivityInfo.getActivityJobId());
        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, event.getFdn(), JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        miniLinkActivityUtil.sendNotification(jobActivityInfo, THIS_ACTIVITY);

    }

    private void processAVCNotification(final Notification notification, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Process AVC Notification() with notificationSubject: {}", notification.getNotificationSubject());
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();
        for (final AttributeChangeData change : event.getChangedAttributes()) {
            if (MiniLinkConstants.XF_LICENSE_INSTALL_OPER_STATUS.equals(change.getName())) {
                final String installState = String.valueOf(change.getNewValue());
                evaluateGlobalState(jobActivityInfo, event, installState);
                return;
            }
        }
    }

    private long getNeJobId(final long activityJobId) {
        long neJobId = 0;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            neJobId = neJobStaticData.getNeJobId();
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while processing for install activity with activityJobId :" + activityJobId
                    + ". Exception is: ";
            LOGGER.error(errorMsg, e);
        }
        return neJobId;
    }

}
