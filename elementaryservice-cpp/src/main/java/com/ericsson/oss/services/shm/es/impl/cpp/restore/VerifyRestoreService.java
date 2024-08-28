/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
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
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This service will verify the restore CV for CPP based nodes.
 *
 * @author xchedoo
 *
 */

@EServiceQualifier("CPP.RESTORE.verify")
@ActivityInfo(activityName = "verify", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class VerifyRestoreService extends AbstractBackupActivity implements Activity, ActivityCallback, ActivityCompleteCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyRestoreService.class);

    @Inject
    private ConfigurationVersionUtils cvUtility;

    @Inject
    private VerifyRestoreHandler verifyRestoreCvHandler;

    @Inject
    private ConfigurationVersionService cvService;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private RestorePrecheckHandler restorePrecheckHandler;

    @Inject
    protected ActivityCompleteTimer activityCompleteTimer;

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheck(long)
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        return restorePrecheckHandler.getRestorePrecheckResult(activityJobId, ActivityConstants.VERIFY_RESTORE_CV, ActivityConstants.VERIFY);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#execute(long)
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Entering VerifyRestoreService.execute() for  activityJobId={}", activityJobId);
        JobResult executeStatus = JobResult.FAILED;
        JobEnvironment jobEnv = null;
        String cvName = null;
        boolean isActionInvocationSucess = false;
        String nodeName = "";
        try {
            jobEnv = activityUtils.getJobEnvironment(activityJobId);
            nodeName = (jobEnv.getNodeName() != null) ? jobEnv.getNodeName() : nodeName;
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnv.getNeJobId());
            cvName = getCvNameToVerify(jobEnv);
            isActionInvocationSucess = verifyRestoreCvHandler.invokeVerifyRestore(cvName, jobEnv, activityUtils.getActivityInfo(activityJobId, VerifyRestoreService.class));
            if (isActionInvocationSucess) {
                executeStatus = JobResult.SUCCESS;
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnv.getNeJobId());
            } else {
                updateActivityJobStatus(executeStatus, jobEnv);
            }
        } catch (final Exception e) {
            final String errorMsg = "Exception while invoking verify restore action with activityJobId:" + activityJobId + "Exception is: ";
            LOGGER.error(errorMsg, e);
        }
        LOGGER.debug("Exiting VerifyRestoreService.execute() for  activityJobId={},NodeName={},CvName={}, with status={}", activityJobId, nodeName, cvName, executeStatus);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Entering VerifyRestoreService.handleTimeout() for  activityJobId={}", activityJobId);
        JobResult jobResult = JobResult.FAILED;
        ActivityStepResultEnum activityStepResult = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String nodeName = null;
        String cvName = null;
        final JobEnvironment jobEnv = activityUtils.getJobEnvironment(activityJobId);
        final long activityStartTime = ((Date) jobEnv.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        try {
            nodeName = jobEnv.getNodeName();
            cvName = getCvNameToVerify(jobEnv);
            jobResult = verifyRestoreCvHandler.verifyRestoreTimedOut(cvName, jobEnv, activityUtils.getActivityInfo(activityJobId, VerifyRestoreService.class));

            LOGGER.debug("Going to workflow Instance Notifier to send activate for Verify Restore activity {}", activityJobId);
            activityUtils.sendNotificationToWFS(jobEnv, activityJobId, ActivityConstants.VERIFY_RESTORE_CV, null);
            activityStepResult = (jobResult == JobResult.SUCCESS) ? ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS : ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            if (activityStepResult == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnv.getNeJobId());
            }
        } catch (final Exception e) {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            LOGGER.error("Exception while handleTimeout for NodeName: {} Exception is: {}", nodeName, e);
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        }
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        LOGGER.debug("Exiting VerifyRestoreService.handleTimeout() for  activityJobId={},NodeName={},CvName={},result = {}", activityJobId, nodeName, cvName, activityStepResult);
        return activityUtils.getActivityStepResult(activityStepResult);
    }

    /**
     *
     * @param jobEnv
     * @return CvName that needs to be verify before restore
     */
    @SuppressWarnings("unchecked")
    private String getCvNameToVerify(final JobEnvironment jobEnv) {
        String neType = null;
        String platform = null;
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobEnv.getMainJobAttributes().get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(jobEnv.getNodeName());
        final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(neFdns);
        if (!networkElementList.isEmpty()) {
            neType = networkElementList.get(0).getNeType();
            platform = networkElementList.get(0).getPlatformType().name();
        }
        final List<String> keyList = new ArrayList<String>();
        keyList.add(BackupActivityConstants.CV_NAME);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, jobEnv.getNodeName(), neType, platform);
        final String cvName = keyValueMap.get(BackupActivityConstants.CV_NAME);
        return cvName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside VerifyRestoreService cancel() with activityJobId:{}", activityJobId);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.VERIFY_RESTORE_CV);
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.VERIFY_RESTORE_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");

        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);

        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered cpp verify restore activity - processNotification with event type : {} and notification : {} ", notification.getNotificationEventType(), notification);
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp verify restore activity - Discarding non-AVC notification.");
            return;
        }
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropList = new ArrayList<Map<String, Object>>();
        JobEnvironment jobEnv = null;
        boolean isActionResultNotified = false;
        long activityJobId = 0;
        String cvMoFDN = " ";
        try {
            activityJobId = activityUtils.getActivityJobId(notification.getNotificationSubject());
            cvMoFDN = ((FdnNotificationSubject) notification.getNotificationSubject()).getFdn();
            final CvActivity newCvActivity = cvUtility.getNewCvActivity(notification);
            final CvActivity oldCvActivity = cvUtility.getOldCvActivity(notification);
            LOGGER.debug("Processing notification for activityJobId={} , with newCvActivity={},oldCvActivity={}", activityJobId, newCvActivity, oldCvActivity);
            jobEnv = activityUtils.getJobEnvironment(activityJobId);
            if (cvUtility.isCVActivityChanged(newCvActivity)) {
                cvUtility.reportNotification(newCvActivity, jobLogList);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList, null);
            } else {
                isActionResultNotified = cvUtility.isInvokedActionResultNotified(activityUtils.getPersistedActionId(jobEnv), notification);
            }

            if (isActionCompleted(isActionResultNotified)) {
                final long activityStartTime = ((Date) jobEnv.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
                startTimer(activityJobId, cvMoFDN);
            }
        } catch (final Exception e) {
            final String logMessage = String.format("Exception while processing notification : %s Exception is : ", notification);
            LOGGER.error(logMessage, e);
        }
        LOGGER.debug("Exiting VerifyRestoreService.processNotification() with notificationSubject={},ActivityJobId={}", notification.getNotificationSubject(), activityJobId);
    }

    /**
     * Start the EJB timer and will invoke onActionComple() method after the timer time elapsed.
     *
     * @param activityJobId
     * @param CvMoFdn
     *            TODO
     */

    private void startTimer(final long activityJobId, final String CvMoFdn) {
        LOGGER.debug("VerifyRestore action execution completed on FDN={}", CvMoFdn);
        activityUtils.unSubscribeToMoNotifications(CvMoFdn, activityJobId, getActivityInfo(activityJobId));
        final JobActivityInfo jobActivityInfo = getActivityInfo(activityJobId);
        activityCompleteTimer.startTimer(jobActivityInfo);
        LOGGER.debug("Activity wait timer is started for {} activity with activityJobId:{}", ActivityConstants.VERIFY_RESTORE_CV, activityJobId);
    }

    /**
     * @param activityJobId
     * @return
     */
    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, this.getClass());
    }

    /**
     * @param isActionResultNotified
     * @return
     */
    private boolean isActionCompleted(final boolean isActionResultNotified) {

        return isActionResultNotified;
    }

    private void updateActivityJobStatus(final JobResult jobResult, final JobEnvironment jobEnv) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final long activityJobId = jobEnv.getActivityJobId();
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);
        final boolean isJobResultedPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        LOGGER.debug("Invoking workflow Instance Notifier to call sendactivate for Verify Restore activity {}", jobEnv.getActivityJobId());
        if (isJobResultedPersisted) {
            activityUtils.sendNotificationToWFS(jobEnv, activityJobId, ActivityConstants.VERIFY_RESTORE_CV, null);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack#onActionComplete(long)
     */
    @Override
    @Asynchronous
    public void onActionComplete(final long activityJobId) {
        LOGGER.debug("Entering VerifyRestoreService.onActionComplete() for  activityJobId={}", activityJobId);

        JobEnvironment jobEnv = null;
        JobResult jobResult = JobResult.FAILED;

        try {
            jobEnv = activityUtils.getJobEnvironment(activityJobId);
            final ConfigurationVersionMO cvMO = cvService.getCvMOFromNode(jobEnv.getNodeName());
            activityUtils.unSubscribeToMoNotifications(cvMO.getFdn(), activityJobId, getActivityInfo(activityJobId));
            jobResult = verifyRestoreCvHandler.onVerifyRestoreCompleted(jobEnv, cvMO);
            updateActivityJobStatus(jobResult, jobEnv);
        } catch (final Exception e) {
            LOGGER.error("exception while onActionComplete", e);

        }
        LOGGER.debug("Exiting VerifyRestoreService.onActionComplete() for  activityJobId={}", activityJobId);

    }

    @Override
    public String getActivityType() {
        return ActivityConstants.VERIFY_RESTORE_CV;
    }

    @Override
    public String getNotificationEventType() {
        return SHMEvents.VERIFY_RESTORE_CV;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return cancelTimeoutActivity(activityJobId, finalizeResult);
    }
}
