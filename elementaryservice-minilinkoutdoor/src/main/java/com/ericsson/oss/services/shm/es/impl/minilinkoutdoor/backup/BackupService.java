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

package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_JOB_ID;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ACTIVITY_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP_FILE_DOES_NOT_EXIST;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.COMPLETE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.INVENTORY_SUPERVISION_DISABLED;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LOG_EXCEPTION;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NETWORKELEMENT;
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
import com.ericsson.oss.mediation.shm.models.BackupJobTaskRequest;
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
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.BackupSmrs;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_OUTDOOR.BACKUP.backup")
@ActivityInfo(activityName = "backup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.MINI_LINK_OUTDOOR)
@Stateless
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BackupService implements Activity, ActivityCallback {

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> backupJobRequest;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("inside precheck:{}", activityJobId);
        final BackupActivityProperties backupActivityProperties = getActivityProperties(activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            miniLinkOutdoorJobUtil.writeToJobLog(PERCENT_ZERO, backupActivityProperties.getActivityJobId(),
                    String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.BACKUP),
                    String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.BACKUP));
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData,
                    ActivityConstants.BACKUP);
            if (!isUserAuthorized) {
                return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, UNAUTHORIZED_USER);
            } else if (backupActivityProperties.getBackupName() == null) {
                return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST);
            } else if (!dpsUtils.isInventorySupervisionEnabled(backupActivityProperties.getNodeName())) {
                return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, INVENTORY_SUPERVISION_DISABLED);
            } else {
                return miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActivityProperties);
            }
        } catch (final Exception e) {
            LOGGER.error(
                    String.format(LOG_EXCEPTION, BACKUP_JOB, backupActivityProperties.getActivityName(), backupActivityProperties.getNodeName()), e);
            return miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, e.getMessage());
        }
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("inside execute::{}", activityJobId);
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        final String neType = dpsUtils.getNeType(activityProperties.getNodeName());
        final SmrsAccountInfo smrsAccountInfo = smrsFileStoreService.getSmrsDetails(BACKUP, neType, activityProperties.getNodeName());
        backupSmrs.prepareBackupDirectory(activityProperties, smrsAccountInfo);
        final String fileName = activityProperties.getBackupFileName();
        activityUtils.subscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(activityProperties.getNodeName(), BACKUP), activityJobId,
                activityUtils.getActivityInfo(activityJobId, this.getClass()));
        final BackupJobTaskRequest request = new BackupJobTaskRequest(NETWORKELEMENT + activityProperties.getNodeName(), BACKUP
                + activityProperties.getNodeName(), BACKUP, fileName, BACKUP);
        backupJobRequest.send(request);
        LOGGER.debug("Mediation Task Request sent for the node : {}", activityProperties.getNodeName());
        miniLinkOutdoorJobUtil.writeToJobLog(PERCENT_ZERO, activityProperties.getActivityJobId(),
                String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.BACKUP),
                String.format(JobLogConstants.EXECUTION_TRIGGERED_EVALUATE_RESULT, ActivityConstants.BACKUP));
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.STARTED, activityProperties.getNodeName(), "",
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        } catch (final Exception e) {
            LOGGER.error(
                    String.format(LOG_EXCEPTION, BACKUP_JOB, activityProperties.getActivityName(), activityProperties.getNodeName()), e);
        }
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(activityProperties.getNodeName(), BACKUP),
                activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Timeout happened for activity: MINI-LINK-OUTDOOR BACKUP backup");
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final boolean jobSucceeded = checkBackupFile(activityProperties);
            if (jobSucceeded) {
                return miniLinkOutdoorJobUtil.timeoutSuccess(PERCENT_HUNDRED, activityProperties);
            } else {
                return miniLinkOutdoorJobUtil.timeoutFail(PERCENT_ZERO, activityProperties);
            }
        } catch (final Exception e) {
            miniLinkOutdoorJobUtil.failWithException(neJobStaticData, activityProperties, BACKUP_JOB, e);
            return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    private BackupActivityProperties getActivityProperties(final long activityJobId) {
        return miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("processNotification :{}", message);
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final SHMCommonCallBackNotificationJobProgressBean notification = (SHMCommonCallBackNotificationJobProgressBean) message;
        LOGGER.debug("Notification received from mediation for activate activity is {} for {}", message, notification.getCommonNotification()
                .getFdn());
        final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_JOB_ID);
        final BackupActivityProperties backupActivityProperties = getActivityProperties(activityJobId);
        try {
            final String activityName = (String) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_NAME);
            final String nodeName = getActivityProperties(activityJobId).getNodeName();
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            if (notification.getCommonNotification() != null) {
                final String result = notification.getCommonNotification().getResult();
                final String state = notification.getCommonNotification().getState();
                final double progressPercent = Double.parseDouble(notification.getCommonNotification().getProgressPercentage().trim());
                if (COMPLETE.equalsIgnoreCase(state)) {
                    checkBackupFileAndFinish(backupActivityProperties, jobProperties, progressPercent, activityJobId, activityName, neJobStaticData, notification);
                } else if (UPLOADING.equalsIgnoreCase(state)) {
                    LOGGER.debug("getActivityResult : {} , progressPercent : {}", result, progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs,
                            progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                } else {
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                    activityUtils.addJobLog("BackUp job failed.", JobLogType.SYSTEM.getLogType(), jobLogs, JobLogLevel.INFO.getLogLevel());
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs,
                            progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR,
                            nodeName, notification.getCommonNotification().getFdn(),
                            activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
                    miniLinkOutdoorJobUtil.failBackupRestoreActivity(neJobStaticData, activityJobId, activityName, state, result);
                }
            }
        } catch (Exception e) {
            LOGGER.error(
                    String.format(LOG_EXCEPTION, BACKUP_JOB, backupActivityProperties.getActivityName(), backupActivityProperties.getNodeName()), e);
        }
    }

    private void checkBackupFileAndFinish(final BackupActivityProperties activityProperties, final List<Map<String, Object>> jobProperties,
            final double progressPercent, final long activityJobId, final String activityName, final NEJobStaticData neJobStaticData, final SHMCommonCallBackNotificationJobProgressBean notification) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        if (checkBackupFile(activityProperties)) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
            activityUtils.addJobLog("BackUp job is Successful.", JobLogType.SYSTEM.getLogType(), jobLogs, JobLogLevel.INFO.getLogLevel());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs, progressPercent);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, activityProperties.getNodeName(), notification.getCommonNotification().getFdn(), activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            miniLinkOutdoorJobUtil.succeedBackupRestoreActivity(neJobStaticData, activityJobId, activityName);
        } else {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            activityUtils.addJobLog("BackUp job failed.", JobLogType.SYSTEM.getLogType(), jobLogs, JobLogLevel.INFO.getLogLevel());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs, progressPercent);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, activityProperties.getNodeName(), notification.getCommonNotification().getFdn(), activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            miniLinkOutdoorJobUtil.failBackupRestoreActivity(neJobStaticData, activityJobId, activityName,
                        BACKUP_FILE_DOES_NOT_EXIST, activityProperties.getBackupFileName());
        }
    }

    private boolean checkBackupFile(final BackupActivityProperties activityProperties) {
        final String neType = dpsUtils.getNeType(activityProperties.getNodeName());
        final SmrsAccountInfo smrsAccountInfo = smrsFileStoreService.getSmrsDetails(BACKUP, neType, activityProperties.getNodeName());
        final boolean fileExists = backupSmrs.checkExistenceOfBackupFile(activityProperties, smrsAccountInfo);
        if (fileExists) {
            LOGGER.info("Backup file exists.");
        } else {
            LOGGER.debug("Backup file not found!");
        }
        return fileExists;
    }
}
