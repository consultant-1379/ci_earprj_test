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

package com.ericsson.oss.services.shm.es.impl.minilink.backup;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.BACKUP_FILE_DOES_NOT_EXIST;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.BACKUP_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.FTP_BUSY;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.GENERATE_BACKUP_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.INCORRECT_CONFIG_STATE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOAD_FAILED;
import static com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum.AVC;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupActivityProperties;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupRestoreJobsCommon;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@EServiceQualifier("MINI_LINK_INDOOR.BACKUP.backup")
@ActivityInfo(activityName = "backup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BackupService implements Activity, ActivityCallback {

    public static final String LOG_BACKUP_FILE_IS_CREATED = "Backup file is created.";

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BackupDps backupDps;

    @Inject
    private BackupSmrs backupSmrs;

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private MiniLinkJobUtil miniLinkJobUtil;

    @Inject
    private BackupRestoreJobsCommon backupRestoreJobsCommon;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @Inject
    private CheckPeriodicity checkPeriodicity;

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData,
                    ActivityConstants.BACKUP);
            if (!isUserAuthorized) {
                final ActivityStepResult activityStepResult = new ActivityStepResult();
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.BACKUP),
                    String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.BACKUP));
            if (activityProperties.getBackupName() == null) {
                return miniLinkJobUtil.precheckFailure(activityProperties, JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST);
            }
            if (activityPreconditionValid(activityProperties)) {
                return miniLinkJobUtil.precheckSuccess(activityProperties);
            } else {
                return miniLinkJobUtil.precheckFailure(activityProperties, INCORRECT_CONFIG_STATE);
            }
        } catch (final Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, BACKUP_JOB, activityProperties.getActivityName(),
                    activityProperties.getNodeName()), e);
            return miniLinkJobUtil.precheckFailure(activityProperties, e.getMessage());
        }
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        String backupName;
        String initialBackupName;
        String backupFileName;
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        backupName = activityProperties.getBackupName();
        initialBackupName = backupName;
        backupFileName = activityProperties.getBackupFileName();
        final boolean isPeriodicJob = checkJobPeriodicity(activityJobId);
        final boolean isAutogenerateBackUp = checkAutoGenerateBackUp(activityJobId);
        LOGGER.debug("Is job Periodic: {} and is backupName Autogenerated: {}", isPeriodicJob, isAutogenerateBackUp);
        if (isPeriodicJob && !isAutogenerateBackUp) {
            backupFileName = appendTimeStamp(backupFileName);
        }
        final String backupFileNameWithPath = activityProperties.getNodeName() + MiniLinkConstants.SLASH + backupFileName;
        String timestampWithExtension;
        String newBackupFileName;
        String backupFileNameWithoutTimestamp;
        if (backupFileNameWithPath.length() > MiniLinkConstants.BACKUP_FILE_NAME_LENGTH_LIMIT) {
            timestampWithExtension = backupFileName.substring(backupFileName.length() - MiniLinkConstants.TIMESTAMP_WITH_EXT_LENGTH,
                    backupFileName.length());
            backupFileNameWithoutTimestamp = backupFileName.substring(0, backupFileName.length() - MiniLinkConstants.TIMESTAMP_WITH_EXT_LENGTH);
            newBackupFileName = backupFileNameWithoutTimestamp.substring(0,
                    backupFileNameWithoutTimestamp.length() - (backupFileNameWithPath.length() - MiniLinkConstants.BACKUP_FILE_NAME_LENGTH_LIMIT));
            backupFileName = newBackupFileName + timestampWithExtension;
        }
        backupName = backupFileName.substring(0, backupFileName.length() - MiniLinkConstants.EXTENSION_LENGTH);
        if (!backupName.equals(initialBackupName)) {
            activityProperties.setBackupName(backupName);
            NEJobStaticData neJobStaticData;
            try {
                neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
                miniLinkJobUtil.updateBackupInNeJob(neJobStaticData.getNeJobId(), backupName);
            } catch (JobDataNotFoundException e1) {
                LOGGER.error(
                        String.format(MiniLinkConstants.LOG_EXCEPTION, MiniLinkConstants.BACKUP_JOB, activityJobId, activityProperties.getNodeName()),
                        e1);
            }
        }
        miniLinkJobUtil.writeToJobLog(activityProperties,
                String.format(JobLogConstants.EXECUTION_STARTED, ActivityConstants.BACKUP, activityProperties.getBackupFileName()));
        try {
            miniLinkDps.setupFtpForNode(activityProperties);
            backupSmrs.prepareBackupDirectory(activityProperties);
            backupDps.initBackup(activityProperties);
            miniLinkDps.subscribeToLoadObjectsNotifications(activityProperties);
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, BACKUP_JOB, e);
        }
    }

    @Override
    public void processNotification(final Notification message) {
        if (!AVC.equals(message.getNotificationEventType())) {
            return;
        }

        final BackupActivityProperties activityProperties = getActivityProperties(message);
        try {
            handleConfigStatusChange(activityProperties);
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, BACKUP_JOB, e);
        }
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(activityProperties);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Timeout happened for activity: MINI-LINK-INDOOR BACKUP backup");
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        try {
            final boolean jobSucceeded = checkBackupFile(activityProperties);
            if (jobSucceeded) {
                return backupRestoreJobsCommon.timeoutSuccess(activityProperties);
            } else {
                return backupRestoreJobsCommon.timeoutFail(activityProperties);
            }
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, BACKUP_JOB, e);
            return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    private boolean activityPreconditionValid(final BackupActivityProperties activityProperties) {
        final String configStatus = miniLinkDps.getConfigStatus(activityProperties);
        return !CONFIG_UP_LOADING.getStatusValue().equals(configStatus) && !CONFIG_DOWN_LOADING.getStatusValue().equals(configStatus);
    }

    private void handleConfigStatusChange(final BackupActivityProperties activityProperties) {
        final String configStatus = miniLinkDps.getConfigStatus(activityProperties);
        final String nodeName = activityProperties.getNodeName();
        final String ossModelIdentityDps = miniLinkDps.getOssModelIdentity(nodeName);
        LOGGER.debug("BackupService configStatus: {}", configStatus);
        switch (XfConfigStatus.fromStatusValue(configStatus)) {
            case CONFIG_UP_LOAD_OK:
                checkBackupFileAndFinish(activityProperties);
                break;
            case CONFIG_UP_LOADING:
                return;
            default:
                if ((UnsecureFTPModelIdentity.CN210.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)) 
                    || UnsecureFTPModelIdentity.CN510R1.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)
                    || UnsecureFTPModelIdentity.TN11B.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)
                        && (configStatus.equals(CONFIG_UP_LOAD_FAILED.getStatusValue()))) {
                    LOGGER.debug("configStatus check failed:{}", configStatus);
                    backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, FTP_BUSY, configStatus);
                } else {
                    LOGGER.debug("Config status check failed: {}", configStatus);
                    backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, INCORRECT_CONFIG_STATE, configStatus);
                }
        }
    }

    private void checkBackupFileAndFinish(final BackupActivityProperties activityProperties) {
        final boolean backupExist = checkBackupFile(activityProperties);
        if (backupExist) {
            miniLinkJobUtil.writeToJobLog(activityProperties, LOG_BACKUP_FILE_IS_CREATED);
            backupRestoreJobsCommon.succeedBackupRestoreActivity(activityProperties);
        } else {
            backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, BACKUP_FILE_DOES_NOT_EXIST, activityProperties.getBackupFileName());
        }
    }

    private boolean checkBackupFile(final BackupActivityProperties activityProperties) {
        final boolean fileExists = backupSmrs.checkExistenceOfBackupFile(activityProperties);
        if (fileExists) {
            LOGGER.debug("Backup file exists.");
        } else {
            LOGGER.debug("Backup file not found!");
        }
        return fileExists;
    }

    private BackupActivityProperties getActivityProperties(final long activityJobId) {
        return miniLinkJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class);
    }

    private BackupActivityProperties getActivityProperties(final Notification message) {
        return miniLinkJobUtil.getBackupActivityProperties(message, ActivityConstants.BACKUP, BackupService.class);
    }


    public String appendTimeStamp(final String backupName) {
        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(MiniLinkConstants.DATE_FORMAT_TOBE_APPENDED_TO_BACKUPNAME);
        final String dateValue = formatter.format(dateTime);
        String newBackupName;
        if (backupName != null) {
            final String extension = MiniLinkConstants.DOT + MiniLinkConstants.CONFIG_FILE_EXTENSION;
            newBackupName = backupName.replace(backupName.substring(backupName.length() - MiniLinkConstants.TIMESTAMP_WITH_EXT_LENGTH),
                    MiniLinkConstants.UNDERSCORE + dateValue + extension);
            return newBackupName;
        } else {
            return dateValue;
        }

    }

    /**
     * Checks whether Backup Job is Periodic or not.
     * @param activityJobId
     * @return
     */

    public boolean checkJobPeriodicity(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.MAIN_SCHEDULE);
            final List<Map<String, Object>> schedulePropertiesList = (List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES);
            final boolean isPeriodicJob = checkPeriodicity.isJobPeriodic(schedulePropertiesList);
            if (isPeriodicJob) {
                return true;
            }
        } catch (JobDataNotFoundException e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, MiniLinkConstants.BACKUP_JOB, activityJobId, activityProperties.getNodeName()),e);
        }
        return false;
    }

    /**
     * Checks whether BackupName is Autogenerated or not.
     * @param activityJobId
     * @return
     */

    public boolean checkAutoGenerateBackUp(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        try {
            String isAutogenerateBackUp = null;
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<Map<String, Object>> neTypeJobProperties = (List<Map<String, Object>>) jobConfigurationDetails
                    .get(ShmConstants.NETYPEJOBPROPERTIES);
            for (Map<String, Object> neTypeJobPropertiesMap : neTypeJobProperties) {
                final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neTypeJobPropertiesMap.get(ShmConstants.JOBPROPERTIES);
                for (final Map<String, String> jobPropertiesMap : jobProperties) {
                    if (jobPropertiesMap.get("key") != null && jobPropertiesMap.get("key").equals(GENERATE_BACKUP_NAME)) {
                        isAutogenerateBackUp = jobPropertiesMap.get("value");
                    }
                }
            }
            if (isAutogenerateBackUp == null || !isAutogenerateBackUp.equals("true")) {
                return false;
            }
        } catch (JobDataNotFoundException e) {
            LOGGER.error(
                    String.format(MiniLinkConstants.LOG_EXCEPTION, MiniLinkConstants.BACKUP_JOB, activityJobId, activityProperties.getNodeName()), e);
        }
        return true;
    }

}
