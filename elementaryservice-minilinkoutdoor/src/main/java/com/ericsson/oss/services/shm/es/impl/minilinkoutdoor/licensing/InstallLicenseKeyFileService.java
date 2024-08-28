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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.licensing;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_JOB_ID;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_CORRUPT_SIGNATURE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_NO_SPACE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_NO_STORAGE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_SEQUENCE_NUMBER;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_SYSTEM_ERROR;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_TRANSFER_FAILED;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_XML_SYNTAX;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.INVENTORY_SUPERVISION_DISABLED;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LICENCE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LICENSE_FILEPATH;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LICENSE_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LKF_CONFIGURING;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LKF_DOWNLOADING;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LKF_ENABLING;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LKF_INSTALLING;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LKF_VALIDATING;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LOG_EXCEPTION;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NOOP;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNAUTHORIZED_USER;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_FINGERPRINT;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_FINGERPRINT_METHOD;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_SIGNATURE_TYPE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_VERSION;

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
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.shm.models.LicenceJobTaskRequest;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_OUTDOOR.LICENSE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstallLicenseKeyFileService implements Activity, ActivityCallback {
    private static final String THIS_ACTIVITY = ActivityConstants.INSTALL_LICENSE;
    private static final String INSTALLSTATE_CHANGED_TO = "Installstate changed to: {} on Node: {} for activityID: {}";
    private static final double LKF_ENABLING_PROGRESS_PERCENTAGE = 90.0;
    private static final double LKF_INSTALLING_PROGRESS_PERCENTAGE = 70.0;
    private static final double LKF_VALIDATION_PROGRESS_PERCENTAGE = 50.0;
    private static final double LKF_DOWNLOAD_PROGRESS_PERCENTAGE = 40.0;
    private static final double LKF_CONFIGURING_PROGRESS_PERCENTAGE = 30.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallLicenseKeyFileService.class);

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> licenceJobTaskRequest;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private DPSUtils dpsUtils;

    @Inject
    protected SystemRecorder systemRecorder;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("Precheck started for {} activity. activityJobId: {}, nodeName: {}", THIS_ACTIVITY, activityJobId, nodeName);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY), JobLogLevel.INFO.getLogLevel()));
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY), JobLogLevel.INFO.getLogLevel()));
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData,
                    ActivityConstants.INSTALL_LICENSE_ACTIVITY);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, UNAUTHORIZED_USER),
                        JobLogLevel.ERROR.getLogLevel()));
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY),
                        JobLogLevel.INFO.getLogLevel()));
            } else if (!dpsUtils.isInventorySupervisionEnabled(nodeName)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                jobLogList.add(activityUtils.createNewLogEntry(
                        String.format(JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, INVENTORY_SUPERVISION_DISABLED),
                        JobLogLevel.ERROR.getLogLevel()));
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY),
                        JobLogLevel.INFO.getLogLevel()));
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_SUCCESS, THIS_ACTIVITY),
                        JobLogLevel.INFO.getLogLevel()));
            }

        } catch (final Exception exception) {
            LOGGER.error(String.format(LOG_EXCEPTION, LICENSE_JOB, THIS_ACTIVITY, activityUtils.getJobEnvironment(activityJobId).getNodeName()),
                    exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            final String errorMessage = String.format(JobLogConstants.FAILURE_REASON, exception.getMessage());
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, errorMessage), JobLogLevel.ERROR.getLogLevel()));
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, THIS_ACTIVITY), JobLogLevel.ERROR.getLogLevel()));
        }
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(getNeJobId(activityJobId));
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Execute started for {} Install License Key File.", THIS_ACTIVITY);
        LOGGER.debug("neJobId: {}", activityUtils.getPoAttributes(activityJobId).get(ShmConstants.NE_JOB_ID));
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.EXECUTING, THIS_ACTIVITY), JobLogLevel.INFO.getLogLevel()));
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
        final String nodeName = activityUtils.getJobEnvironment(activityJobId).getNodeName();
        final String moFdn = miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, LICENCE);
        activityUtils.subscribeToMoNotifications(moFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        final Map<String, String> licenseFile = jobPropertyUtils.getPropertyValue(Collections.singletonList(LICENSE_FILEPATH),
                activityUtils.getMainJobAttributes(activityJobId), nodeName);
        final String licenseFileName = licenseFile.get(LICENSE_FILEPATH);
        LOGGER.debug("License File {} is to install on {}: ", licenseFileName, nodeName);
        final LicenceJobTaskRequest request = new LicenceJobTaskRequest(NETWORKELEMENT + nodeName, LICENCE + nodeName, LICENCE, licenseFileName,
                LICENCE);
        licenceJobTaskRequest.send(request);
        LOGGER.debug("Mediation Task Request sent for the node : {}", nodeName);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, nodeName, moFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
        } catch (final Exception e) {
            LOGGER.error(String.format(LOG_EXCEPTION, LICENSE_JOB, THIS_ACTIVITY, nodeName), e);
        }
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.info("Timeout happened for activity: {}", THIS_ACTIVITY);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final String jobLogMessage = String.format(JobLogConstants.TIMEOUT, THIS_ACTIVITY);
        final String nodeName = activityUtils.getJobEnvironment(activityJobId).getNodeName();
        activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, LICENCE), activityJobId,
                activityUtils.getActivityInfo(activityJobId, this.getClass()));
        activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.WARN.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList,
                HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(getNeJobId(activityJobId));
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.addJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, "true", jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, 0.0);
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
        activityUtils.unSubscribeToMoNotifications(
                miniLinkOutdoorJobUtil.getSubscriptionKey(activityUtils.getJobEnvironment(activityJobId).getNodeName(), LICENCE), activityJobId,
                activityUtils.getActivityInfo(activityJobId, this.getClass()));
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("processNotification :{}", message);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final SHMCommonCallBackNotificationJobProgressBean notification = (SHMCommonCallBackNotificationJobProgressBean) message;
        LOGGER.debug("Notification received from mediation for activate activity is {} for {}", message, notification.getCommonNotification()
                .getFdn());
        try {
            final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_JOB_ID);
            final String activityName = (String) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_NAME);
            final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            if (notification.getCommonNotification() != null) {
                final String state = notification.getCommonNotification().getState();
                evaluateResult(jobActivityInformation, miniLinkOutdoorJobUtil.getSubscriptionKey(activityUtils.getJobEnvironment(activityJobId).getNodeName(), activityName),
                        state, notification.getCommonNotification().getFdn(), neJobStaticData.getMainJobId());
            }
        } catch (Exception e) {
            final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_JOB_ID);
            LOGGER.error(String.format(LOG_EXCEPTION, LICENSE_JOB, THIS_ACTIVITY, activityUtils.getJobEnvironment(activityJobId).getNodeName()), e);
            final String errorMessage = String.format(JobLogConstants.FAILURE_REASON, e.getMessage());
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, THIS_ACTIVITY, errorMessage), JobLogLevel.ERROR.getLogLevel()));
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

    private void evaluateResult(final JobActivityInfo jobActivityInfo, final String subscriptionKey, final String operStatus, final String fdn, final long mainJobId) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> neJobAttributes = jobEnvironment.getNeJobAttributes();
        final String nodeName = activityUtils.getJobEnvironment(jobActivityInfo.getActivityJobId()).getNodeName();
        LOGGER.trace("neJobAttributes: {} for nodeName: {}", neJobAttributes, nodeName);
        LOGGER.debug("evaluateResult started, subscriptionKey: {}, operStatus: {}, activityId: {}, nodeName: {}", subscriptionKey, operStatus,
                activityJobId, nodeName);
        evaluateResultUsingOperStatus(jobActivityInfo, subscriptionKey, operStatus, fdn, mainJobId, activityJobId, nodeName);
    }

    private void evaluateResultUsingOperStatus(final JobActivityInfo jobActivityInfo, final String subscriptionKey, final String operStatus, final String fdn,
            final long mainJobId, final long activityJobId, final String nodeName) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        switch (operStatus) {
        case NOOP:
            activityUtils.addJobLog("The license file is downloaded,stored in RMM successfully and is currently active.",
                    JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
            miniLinkOutdoorJobUtil.finishActivity(jobActivityInfo, subscriptionKey, JobResult.SUCCESS, jobLogList, THIS_ACTIVITY);
            systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, fdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));
            break;
        case LKF_CONFIGURING:
            updateLogAndOngoingJobDetails(activityJobId, nodeName, operStatus, fdn, mainJobId,
                    "The license file is being configuring on node to dowwnlaod from the FTP server", LKF_CONFIGURING_PROGRESS_PERCENTAGE);
            break;
        case LKF_DOWNLOADING:
            updateLogAndOngoingJobDetails(activityJobId, nodeName, operStatus, fdn, mainJobId,
                    "The license file is being downloaded from the FTP server", LKF_DOWNLOAD_PROGRESS_PERCENTAGE);
            break;
        case LKF_VALIDATING:
            updateLogAndOngoingJobDetails(activityJobId, nodeName, operStatus, fdn, mainJobId,
                    "The license file is being validated", LKF_VALIDATION_PROGRESS_PERCENTAGE);
            break;
        case LKF_INSTALLING:
            updateLogAndOngoingJobDetails(activityJobId, nodeName, operStatus, fdn, mainJobId,
                    "The license file is being stored in RMM", LKF_INSTALLING_PROGRESS_PERCENTAGE);
            break;
        case LKF_ENABLING:
            updateLogAndOngoingJobDetails(activityJobId, nodeName, operStatus, fdn, mainJobId,
                    "The license file is being activated", LKF_ENABLING_PROGRESS_PERCENTAGE);
            break;
        case ERROR_NO_STORAGE:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The RMM is not available and is unable to store it in RMM. The file is discarded.");
            break;
        case ERROR_NO_SPACE:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "here is no more space in RMM for the license file and is discarded.");
            break;
        case UNKNOWN_VERSION:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The format version of the license file is not supported by the current software.The license file is considered as invalid.");
            break;
        case UNKNOWN_SIGNATURE_TYPE:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The signature type of the license file is not supported by the current software.The license file is considered as invalid.");
            break;
        case UNKNOWN_FINGERPRINT_METHOD:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The finger print method is not supported by the current software. The license file is then considered as invalid.");
            break;
        case UNKNOWN_FINGERPRINT:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The finger print the license file is not supported by the current software. The license file is considered as invalid.");
            break;
        case ERROR_CORRUPT_SIGNATURE:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The signature of the license file is corrupt and is discarded.");
            break;
        case ERROR_TRANSFER_FAILED:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The unable to download license file due to transfer error.");
            break;
        case ERROR_SEQUENCE_NUMBER:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The error in sequence number of the license file and is discarded.");
            break;
        case ERROR_XML_SYNTAX:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId,
                    "The error in XML syntax of license file and is discarded.");
            break;
        case ERROR_SYSTEM_ERROR:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId, "Error_system_error.");
            break;
        default:
            updateLogAndFailJob(jobActivityInfo, subscriptionKey, nodeName, operStatus, fdn, mainJobId, "License Install failed with error state:" + operStatus);
            break;
        }
    }

    private void updateLogAndOngoingJobDetails(final long activityJobId, final String nodeName, final String operStatus, final String fdn, final long mainJobId,
            final String logMessage, final double progressPercent) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
        LOGGER.debug(INSTALLSTATE_CHANGED_TO, operStatus, nodeName, activityJobId);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, Collections.<Map<String, Object>> emptyList(),
                jobLogList, progressPercent);
        systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, CommandPhase.ONGOING, nodeName, fdn,
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));
    }

    private void updateLogAndFailJob(final JobActivityInfo jobActivityInfo, final String subscriptionKey, final String nodeName,
                                     final String operStatus, final String fdn, final long mainJobId, final String logMessage) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final long activityJobId = jobActivityInfo.getActivityJobId();
        LOGGER.debug(INSTALLSTATE_CHANGED_TO, operStatus, nodeName, activityJobId);
        activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.ERROR.getLogLevel());
        miniLinkOutdoorJobUtil.finishActivity(jobActivityInfo, subscriptionKey, JobResult.FAILED, jobLogList, THIS_ACTIVITY);
        systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, CommandPhase.FINISHED_WITH_ERROR, nodeName, fdn,
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));
    }

}
