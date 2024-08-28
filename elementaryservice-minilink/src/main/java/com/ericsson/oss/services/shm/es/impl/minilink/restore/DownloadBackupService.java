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

package com.ericsson.oss.services.shm.es.impl.minilink.restore;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.INCORRECT_CONFIG_STATE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.RESTORE_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_COMMAND;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigLoadCommand.CONFIG_DOWNLOAD;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWNLOAD_FAILED;
import static com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum.AVC;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.FTP_BUSY;

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
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.backup.UnsecureFTPModelIdentity;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupActivityProperties;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupRestoreJobsCommon;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_INDOOR.RESTORE.download")
@ActivityInfo(activityName = "download", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DownloadBackupService implements Activity, ActivityCallback {

    public static final String LOG_CONFIGURATION_IS_DOWNLOADED = "Configuration is downloaded.";

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private MiniLinkJobUtil miniLinkJobUtil;

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private RestoreJobCommon restoreJobCommon;

    @Inject
    private BackupRestoreJobsCommon backupRestoreJobsCommon;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadBackupService.class);
    private static final String THIS_ACTIVITY = ActivityConstants.DOWNLOAD;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData,
                    ActivityConstants.DOWNLOAD);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY));
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY));
            return restoreJobCommon.activityPrecheck(activityProperties);
        } catch (Exception exception) {
            LOGGER.error("Exception occured in precheck() for activityJobId:{}. Reason: ", activityJobId, exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            miniLinkJobUtil.writeToJobLog(activityProperties, exception.getMessage());
        }
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);

        miniLinkJobUtil.writeToJobLog(activityProperties,
                String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, activityProperties.getBackupFileName()));
        try {
            miniLinkDps.setupFtpForNode(activityProperties);
            initDownload(activityProperties);
            miniLinkDps.subscribeToLoadObjectsNotifications(activityProperties);
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, RESTORE_JOB, e);
        }
    }

    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("DownloadBackupService.processNotification");
        if (!AVC.equals(message.getNotificationEventType())) {
            return;
        }
        LOGGER.debug(message.getDpsDataChangedEvent().toString());

        final BackupActivityProperties activityProperties = getActivityProperties(message);

        try {
            final String configStatus = miniLinkDps.getConfigStatus(activityProperties);
            final String nodeName = activityProperties.getNodeName();
            final String ossModelIdentityDps = miniLinkDps.getOssModelIdentity(nodeName);
            LOGGER.debug("DownloadBackupService.processNotification configStatus = {}", configStatus);
            switch (XfConfigStatus.fromStatusValue(configStatus)) {
                case CONFIG_DOWN_LOAD_OK:
                    miniLinkJobUtil.writeToJobLog(activityProperties, LOG_CONFIGURATION_IS_DOWNLOADED);
                    backupRestoreJobsCommon.succeedBackupRestoreActivity(activityProperties);
                    break;
                case CONFIG_DOWN_LOADING:
                    return;
                default:
                    if ((UnsecureFTPModelIdentity.CN210.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)
                        || UnsecureFTPModelIdentity.CN510R1.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)
                        || UnsecureFTPModelIdentity.TN11B.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps))
                            && (configStatus.equals(CONFIG_DOWNLOAD_FAILED.getStatusValue()))) {
                        LOGGER.debug("configStatus check failed:{}", configStatus);
                        backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, FTP_BUSY, configStatus);
                    } else {
                        LOGGER.debug("configStatus check failed:{}", configStatus);
                        backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, INCORRECT_CONFIG_STATE, configStatus);
                    }
            }
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, RESTORE_JOB, e);
        }
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("MINI-LINK-INDOOR restore job download activity timeout.");
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        return backupRestoreJobsCommon.timeoutFail(activityProperties);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(activityProperties);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    private void initDownload(final BackupActivityProperties activityProperties) {
        miniLinkDps.updateManagedObjectAttribute(activityProperties.getNodeName(), XF_CONFIG_LOAD_OBJECTS, XF_CONFIG_LOAD_COMMAND,
                CONFIG_DOWNLOAD.getStatusValue());
        miniLinkDps.setXfConfigStatusWithoutMediation(activityProperties.getNodeName(), CONFIG_DOWN_LOADING.getStatusValue());
    }

    private BackupActivityProperties getActivityProperties(final long activityJobId) {
        return miniLinkJobUtil.getBackupActivityProperties(activityJobId, THIS_ACTIVITY, DownloadBackupService.class);
    }

    private BackupActivityProperties getActivityProperties(final Notification message) {
        return miniLinkJobUtil.getBackupActivityProperties(message, THIS_ACTIVITY, DownloadBackupService.class);
    }

}