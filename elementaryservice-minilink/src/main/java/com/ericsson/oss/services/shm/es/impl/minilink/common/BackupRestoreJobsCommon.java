package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.EXCEPTION_OCCURED_FAILURE_REASON;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOAD_OK;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BackupRestoreJobsCommon {

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private MiniLinkJobUtil miniLinkJobUtil;

    @Inject
    private ActivityUtils activityUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreJobsCommon.class);

    public void failWithException(final BackupActivityProperties activityProperties, final String jobType, final Exception e) {
        LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, jobType, activityProperties.getActivityName(), activityProperties.getNodeName()),
                e);
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(activityProperties);
        failBackupRestoreActivity(activityProperties, EXCEPTION_OCCURED_FAILURE_REASON, e.getMessage());
    }

    public ActivityStepResult timeoutFail(final BackupActivityProperties activityProperties) {
        miniLinkJobUtil.updateJobProperty(activityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.TIMEOUT, activityProperties.getActivityName()));
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(activityProperties);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    public ActivityStepResult timeoutSuccess(final BackupActivityProperties activityProperties) {
        miniLinkJobUtil.updateJobProperty(activityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        miniLinkDps.setXfConfigStatusWithoutMediation(activityProperties.getNodeName(), CONFIG_UP_LOAD_OK.getStatusValue());
        miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.TIMEOUT, activityProperties.getActivityName()));
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(activityProperties);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
    }

    /**
     * Writes to the job log and updates the job's property in case of a successful job finish.
     * @param backupActivityProperties
     */
    public void succeedBackupRestoreActivity(final BackupActivityProperties backupActivityProperties) {
        miniLinkJobUtil.writeToJobLog(
                backupActivityProperties, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, backupActivityProperties.getActivityName()));
        miniLinkJobUtil.updateJobProperty(backupActivityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(backupActivityProperties);
        miniLinkJobUtil.sendNotificationToWFS(backupActivityProperties);
    }

    /**
     * Writes to the job log and updates the job's property in case of a job failure.
     * @param backupActivityProperties
     */
    public void failBackupRestoreActivity(final BackupActivityProperties backupActivityProperties, final String reason, final String subject) {
        final String activityName = backupActivityProperties.getActivityName();
        LOGGER.info("{} - {} activity failed. Reason: {} Subject: {}", backupActivityProperties.getNodeName(), activityName, reason, subject);
        miniLinkJobUtil.writeToJobLog(backupActivityProperties,
            String.format(JobLogConstants.ACTIVITY_FAILED, activityName),
            String.format(JobLogConstants.ADDITIONAL_FAILURE_RESULT, activityName, reason, subject));
        miniLinkJobUtil.updateJobProperty(backupActivityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        miniLinkDps.unsubscribeFromLoadObjectsNotifications(backupActivityProperties);
        miniLinkJobUtil.sendNotificationToWFS(backupActivityProperties);
    }

}
