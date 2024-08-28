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

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.*;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.xfConfigAccept.acceptFTP;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfNERestartCommands.WARM_RESTART;
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
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_INDOOR.RESTORE.restore")
@ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RestoreService implements Activity, ActivityCallback {

    public static final String LOG_NODE_IS_RESTARTING = "Node is restarting...";
    public static final String LOG_NODE_RESTARTED_SUCCESSFULLY = "Node restarted successfully.";

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private MiniLinkJobUtil miniLinkJobUtil;

    @Inject
    private RestoreJobCommon restoreJobCommon;

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private BackupRestoreJobsCommon backupRestoreJobsCommon;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreService.class);
    private static final String THIS_ACTIVITY = "restore";

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("activityJobId restore-precheck:: {} ", activityJobId);
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            final boolean verifyflag = restoreJobCommon.ifVerifyActivity(activityJobId);
            LOGGER.debug("verify flag :: {}", verifyflag);
            if (!verifyflag) {
                restoreJobCommon.verifyPrecheck(activityJobId);
                restoreJobCommon.verifyExecute(activityJobId);
            }
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY));
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY));
            return restoreJobCommon.restorePrecheck(activityProperties);
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
        miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, activityProperties.getBackupFileName()));
        try {
            initRestart(activityProperties);
            watchForNodeRestart(activityProperties);
            miniLinkJobUtil.writeToJobLog(activityProperties, LOG_NODE_IS_RESTARTING);
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, RESTORE_JOB, e);
        }
    }

    /**
     * Checking the change of xfConfigStatus attibute of xfConfigLoadObjects MO. When the node restarts then this value should be different than configUpLoading.
     * 
     * @param message
     */
    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("RestoreServcice.processNotification");
        if (!AVC.equals(message.getNotificationEventType())) {
            return;
        }
        LOGGER.debug(message.getDpsDataChangedEvent().toString());

        final BackupActivityProperties activityProperties = getActivityProperties(message);
        try {
            final String configStatus = miniLinkDps.getConfigStatus(activityProperties);
            if (configStatus != XfConfigStatus.CONFIG_UP_LOADING.getStatusValue()) {
                miniLinkJobUtil.writeToJobLog(activityProperties, LOG_NODE_RESTARTED_SUCCESSFULLY);
                backupRestoreJobsCommon.succeedBackupRestoreActivity(activityProperties);
            }
        } catch (final Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, RESTORE_JOB, e);
        }

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("MINI-LINK-INDOOR restore job restore activity timeout.");
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        return backupRestoreJobsCommon.timeoutFail(activityProperties);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("MINI-LINK-INDOOR restore job restore activity cancel.");
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(activityProperties);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    /**
     * Sets up to node to restart.
     * 
     * @param activityProperties
     */
    private void initRestart(final BackupActivityProperties activityProperties) {
        final String nodeName = activityProperties.getNodeName();
        miniLinkDps.updateManagedObjectAttribute(nodeName, XF_CONFIG_LOAD_OBJECTS, XF_CONFIG_ACCEPT, acceptFTP.toString());
        miniLinkDps.updateManagedObjectAttribute(nodeName, XF_NE_RESTART_OBJECTS, XF_NE_RESTART_COMMANDS, WARM_RESTART.getStatusValue());

    }

    /**
     * sets the xfConfigStatus attibute of xfConfigLoadObjects MO to configUpLoading so that the it can be checked that the node has restarted because this value will be different after a restart.
     * 
     * @param activityProperties
     */
    private void watchForNodeRestart(final BackupActivityProperties activityProperties) {
        miniLinkDps.setXfConfigStatusWithoutMediation(activityProperties.getNodeName(), XfConfigStatus.CONFIG_UP_LOADING.getStatusValue());
        miniLinkDps.subscribeToLoadObjectsNotifications(activityProperties);
    }

    private BackupActivityProperties getActivityProperties(final long activityJobId) {
        return miniLinkJobUtil.getBackupActivityProperties(activityJobId, THIS_ACTIVITY, RestoreService.class);
    }

    private BackupActivityProperties getActivityProperties(final Notification message) {
        return miniLinkJobUtil.getBackupActivityProperties(message, THIS_ACTIVITY, RestoreService.class);

    }

}