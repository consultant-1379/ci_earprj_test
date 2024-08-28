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

package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.restore;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_JOB_ID;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUPFILE_UNAVAILABLE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.COMPLETE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.IDLE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.INVENTORY_SUPERVISION_DISABLED;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LOG_EXCEPTION;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.RESTORE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.RESTORE_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNAUTHORIZED_USER;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UPLOADING;

import java.util.ArrayList;
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
import com.ericsson.oss.mediation.shm.models.RestoreJobTaskRequest;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.BackupActivityProperties;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.BackupSmrs;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_OUTDOOR.RESTORE.restore")
@ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
@Stateless
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RestoreService implements Activity, ActivityCallback {

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> restoreJobRequest;

    @Inject
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private DPSUtils dpsUtils;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private SmrsFileStoreService smrsFileStoreService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private BackupSmrs backupSmrs;

    @Inject
    protected SystemRecorder systemRecorder;

    private static final Double PERCENT_ZERO = 0.0;
    private static final Double PERCENT_HUNDRED = 100.0;
    private static final String THIS_ACTIVITY = "restore";

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreService.class);

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Precheck for MINI-LINK-OUTDOOR RESTORE ActivityJob Id:{}", activityJobId);
        final BackupActivityProperties backupActivityProperties = getActivityProperties(activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            miniLinkOutdoorJobUtil.writeToJobLog(PERCENT_ZERO, backupActivityProperties.getActivityJobId(),
                    String.format(JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY),
                    String.format(JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY));
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY);
            final boolean isBackupExists = checkBackupFile(backupActivityProperties);
            if (!isUserAuthorized) {
                return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, UNAUTHORIZED_USER);
            } else if (!isBackupExists) {
                return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, BACKUPFILE_UNAVAILABLE);
            } else if (!dpsUtils.isInventorySupervisionEnabled(backupActivityProperties.getNodeName())) {
                return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, INVENTORY_SUPERVISION_DISABLED);
            } else {
                return miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActivityProperties);
            }
        } catch (final Exception e) {
            LOGGER.error(
                    String.format(LOG_EXCEPTION, RESTORE_JOB, THIS_ACTIVITY, backupActivityProperties.getNodeName()), e);
            return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, e.getMessage());
        }
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Execution started for MINI-LINK-OUTDOOR RESTORE ActivityJob Id:{}", activityJobId);
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        final String fileName = activityProperties.getBackupFileName();
        activityUtils.subscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(activityProperties.getNodeName(), RESTORE), activityJobId,
                activityUtils.getActivityInfo(activityJobId, this.getClass()));
        final RestoreJobTaskRequest request = new RestoreJobTaskRequest(NETWORKELEMENT + activityProperties.getNodeName(), RESTORE
                + activityProperties.getNodeName(), RESTORE, fileName, BACKUP);
        restoreJobRequest.send(request);
        LOGGER.debug("Mediation Task Request sent for the node : {}", activityProperties.getNodeName());
        miniLinkOutdoorJobUtil.writeToJobLog(PERCENT_ZERO, activityProperties.getActivityJobId(),
                String.format(JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY),
                String.format(JobLogConstants.EXECUTION_TRIGGERED_EVALUATE_RESULT, THIS_ACTIVITY));
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            systemRecorder.recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.STARTED, activityProperties.getNodeName(), "",
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.RESTORE));
        } catch (final Exception e) {
            LOGGER.error(
                    String.format(LOG_EXCEPTION, RESTORE_JOB, activityProperties.getActivityName(), activityProperties.getNodeName()), e);
        }
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(activityProperties.getNodeName(), RESTORE),
                activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Timeout happened for activity: MINI-LINK-OUTDOOR RESTORE restore ActivityJob Id:{}", activityJobId);
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        return miniLinkOutdoorJobUtil.timeoutFail(PERCENT_ZERO, activityProperties);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    private BackupActivityProperties getActivityProperties(final long activityJobId) {
        return miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, THIS_ACTIVITY, RestoreService.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("Notification Process started for MINI-LINK-OUTDOOR restore job:{}", message);
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final SHMCommonCallBackNotificationJobProgressBean notification = (SHMCommonCallBackNotificationJobProgressBean) message;
        final SHMCommonCallbackNotification commonNotification = notification.getCommonNotification();
        if (commonNotification != null) {
            LOGGER.debug("Notification received from mediation for MINI-LINK-OUTDOOR restore activity is {} for {}", message, commonNotification.getFdn());
            final long activityJobId = (long) commonNotification.getAdditionalAttributes().get(ACTIVITY_JOB_ID);
            final BackupActivityProperties backupActivityProperties = getActivityProperties(activityJobId);
            final String nodeName = backupActivityProperties.getNodeName();
            try {
                final String activityName = (String) commonNotification.getAdditionalAttributes().get(ACTIVITY_NAME);
                final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
                if (commonNotification.getState() != null) {
                    final String result = commonNotification.getResult();
                    final String state = commonNotification.getState();
                    double progressPercent = Double.parseDouble(commonNotification.getProgressPercentage().trim());
                    if (COMPLETE.equalsIgnoreCase(state)
                            || IDLE.equalsIgnoreCase(state)) {
                        progressPercent = PERCENT_HUNDRED;
                        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
                        activityUtils.addJobLog("Restore job is Successful.", JobLogType.SYSTEM.getLogType(), jobLogs, JobLogLevel.INFO.getLogLevel());
                        systemRecorder.recordCommand(SHMEvents.RESTORE_PROCESS_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, commonNotification.getFdn(),
                                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.RESTORE));
                        miniLinkOutdoorJobUtil.succeedBackupRestoreActivity(neJobStaticData, activityJobId, activityName);
                    } else if (UPLOADING.equalsIgnoreCase(state)) {
                        LOGGER.debug("MINI-LINK-OUTDOOR RESTORE getActivityResult : {} , progressPercent : {}", result, progressPercent);
                    } else {
                        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                        activityUtils.addJobLog("Restore job failed.", JobLogType.SYSTEM.getLogType(), jobLogs, JobLogLevel.INFO.getLogLevel());
                        systemRecorder.recordCommand(SHMEvents.RESTORE_PROCESS_NOTIFICATION, CommandPhase.FINISHED_WITH_ERROR, nodeName, commonNotification.getFdn(),
                                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.RESTORE));
                        miniLinkOutdoorJobUtil.failBackupRestoreActivity(neJobStaticData, activityJobId, activityName, state, result);
                    }
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs,
                            progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                }
            } catch (Exception e) {
                final String activityName = backupActivityProperties.getActivityName();
                LOGGER.error(String.format(LOG_EXCEPTION, RESTORE_JOB, activityName, nodeName), e);
            }
        }
    }

    private boolean checkBackupFile(final BackupActivityProperties activityProperties) {
        final String neType = dpsUtils.getNeType(activityProperties.getNodeName());
        final SmrsAccountInfo smrsAccountInfo = smrsFileStoreService.getSmrsDetails(BACKUP, neType, activityProperties.getNodeName());
        final boolean fileExists = backupSmrs.checkExistenceOfBackupFile(activityProperties, smrsAccountInfo);
        if (fileExists) {
            LOGGER.info("MINI-LINK-OUTDOOR RESTORE Job Backup file exists.");
        } else {
            LOGGER.debug("MINI-LINK-OUTDOOR RESTORE Job Backup file not found!");
        }
        return fileExists;
    }
}
