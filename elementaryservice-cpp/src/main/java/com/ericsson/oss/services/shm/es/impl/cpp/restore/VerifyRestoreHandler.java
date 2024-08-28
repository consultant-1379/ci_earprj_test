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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CVActionResultInformation;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionAdditionalInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionMainAndAdditionalResultHolder;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.shm.inventory.backup.entities.AdminProductData;

/**
 * Task handler for verify restore CV task.Will invoked from {@link VerifyRestoreService}
 * 
 * @author xchedoo
 * 
 */

@Traceable
public class VerifyRestoreHandler extends AbstractBackupActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyRestoreHandler.class);

    @Inject
    JobUpdateService jobUpdateService;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    NotificationRegistry notificationRegistry;

    @Inject
    ConfigurationVersionUtils cvUtility;

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    CommonCvOperations cvOperations;

    @Inject
    ConfigurationVersionService cvService;

    @Inject
    JobPropertyUtils jobPropertyUtils;

    @Inject
    FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    boolean isInstallPossible = false;

    /**
     * returns <code>true</code> if verify restore action invoked successfully else <code>false</code>
     * 
     * @param nodeName
     * @param cvName
     * @param jobActivityInfo
     * @return boolean
     */
    public boolean invokeVerifyRestore(final String cvName, final JobEnvironment jobEnv, final JobActivityInfo jobActivityInfo) {
        final String nodeName = jobEnv.getNodeName();
        LOGGER.debug("Entering invokeVerifyRestore() with  NodeName={} ,CvName={},activityJobId={}", nodeName, cvName, jobEnv.getActivityJobId());
        boolean isActionInvocationSuccess = true;
        String logMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropList = new ArrayList<Map<String, Object>>();
        logMessage = "CV name: " + cvName;
        activityUtils.addJobLog(logMessage, JobTypeEnum.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
        final ConfigurationVersionMO cvMO = cvService.getCvMOFromNode(nodeName);
        activityUtils.subscribeToMoNotifications(cvMO.getFdn(), jobEnv.getActivityJobId(), jobActivityInfo);
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);
        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnv.getMainJobId()), SHMEvents.VERIFY_RESTORE_CV, CommandPhase.STARTED, nodeName, cvMO.getFdn(), "CV name:" + cvName);
        try {
            invokeAction(jobEnv, jobLogList, jobPropList, cvMO, actionArguments);
            activityUtils.prepareJobPropertyList(jobPropList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
        } catch (final Exception e) {
            isActionInvocationSuccess = false;
            handleException(cvName, jobEnv, jobLogList, cvMO, e, jobActivityInfo);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(jobEnv.getActivityJobId(), jobPropList, jobLogList, null);
        return isActionInvocationSuccess;
    }

    private void handleException(final String cvName, final JobEnvironment jobEnv, final List<Map<String, Object>> jobLogList, final ConfigurationVersionMO cvMO, final Exception e,
            final JobActivityInfo jobActivityInfo) {
        final Throwable th = e.getCause();
        final String exceptionMessage = th != null ? th.getMessage() : e.toString();
        final String nodeExceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
        if (!exceptionMessage.isEmpty()) {
            activityUtils.addJobLog(String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.VERIFY_RESTORE_CV) + String.format(JobLogConstants.FAILURE_REASON, nodeExceptionMessage),
                    JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        } else {
            activityUtils.addJobLog(String.format(JobLogConstants.ACTION_TRIGGER_FAILED + JobLogConstants.FAILURE_DUE_TO_EXCEPTION, ActivityConstants.VERIFY_RESTORE_CV) + exceptionMessage,
                    JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }
        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnv.getMainJobId()), SHMEvents.VERIFY_RESTORE_CV, CommandPhase.FINISHED_WITH_ERROR, jobEnv.getNodeName(), cvMO.getFdn(),
                "Cv name:" + cvName);
        activityUtils.unSubscribeToMoNotifications(cvMO.getFdn(), jobEnv.getActivityJobId(), jobActivityInfo);
        LOGGER.error("Verify restore action invocation failed for NE: {} cvName : {} Reason : {}", jobEnv.getNodeName(), cvName, e);
    }

    private void invokeAction(final JobEnvironment jobEnv, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropList, final ConfigurationVersionMO cvMO,
            final Map<String, Object> actionArguments) {
        int actionId;
        String neType = null;
        final long activityJobId = jobEnv.getActivityJobId();
        final long activityStartTime = ((Date) jobEnv.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        try {
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(jobEnv.getNodeName()));
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            LOGGER.error("Exception while fetching neType of node :  {}", jobEnv.getNodeName());
        }
        actionId = cvOperations.executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMO.getFdn(), actionArguments);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
        final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.name(), BackupActivityConstants.ACTION_VERIFY);
        activityUtils.addJobLog(
                String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED_WITH_ID, ActivityConstants.VERIFY_RESTORE_CV, BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, actionId, activityTimeout),
                JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
        activityUtils.addJobProperty(ActivityConstants.ACTION_ID, String.valueOf(actionId), jobPropList);
        LOGGER.debug("For NodeName={},Verify restore action invoked with actionid={}", jobEnv.getNodeName(), actionId);
    }

    /**
     * @param nodeName
     * @param cvName
     * @return TODO
     */
    public JobResult verifyRestoreTimedOut(final String cvName, final JobEnvironment jobEnv, final JobActivityInfo jobActivityInfo) {
        final String nodeName = jobEnv.getNodeName();
        LOGGER.debug("Entering VerifyRestoreHandler.verifyRestoreTimedOut() with arguments: NodeName={},cvName={},activityJobId={}", nodeName, cvName, jobEnv.getActivityJobId());

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.addJobLog(String.format(JobLogConstants.OPERATION_TIMED_OUT, ActivityConstants.VERIFY_RESTORE_CV), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.WARN.toString());
        final ConfigurationVersionMO cvMO = cvService.getCvMOFromNode(nodeName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobResult jobResult = onVerifyRestoreCompleted(jobEnv, cvMO);
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(jobEnv.getActivityJobId(), jobPropertyList, jobLogList, null);
        activityUtils.unSubscribeToMoNotifications(cvMO.getFdn(), jobEnv.getActivityJobId(), jobActivityInfo);
        LOGGER.debug("jobResult{} ", jobResult);
        return jobResult;
    }

    protected JobResult onVerifyRestoreCompleted(final JobEnvironment jobEnv, final ConfigurationVersionMO cvMO) {
        LOGGER.debug("Entering onVerifyRestoreCompleted with NodeName={},CvFDN = {}", jobEnv.getNodeName(), cvMO.getFdn());
        String jobLog = null;
        String logLevel = "ERROR";
        JobResult jobResult = JobResult.FAILED;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        CommandPhase commandPhase = CommandPhase.FINISHED_WITH_ERROR;

        jobResult = processMainActionResult(jobEnv, cvMO);

        commandPhase = jobResult == JobResult.SUCCESS ? CommandPhase.FINISHED_WITH_SUCCESS : CommandPhase.FINISHED_WITH_ERROR;
        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnv.getMainJobId()), SHMEvents.VERIFY_RESTORE_CV, commandPhase, jobEnv.getNodeName(), cvMO.getFdn(),
                activityUtils.additionalInfoForCommand(jobEnv.getActivityJobId(), jobEnv.getMainJobId(), JobTypeEnum.RESTORE));
        if (jobResult == JobResult.SUCCESS) {
            jobLog = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.VERIFY_RESTORE_CV);
            logLevel = JobLogLevel.INFO.toString();
        } else if (jobResult == JobResult.FAILED) {
            jobLog = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.VERIFY_RESTORE_CV);
            logLevel = JobLogLevel.ERROR.toString();
        }
        activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, logLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(jobEnv.getActivityJobId(), null, jobLogList, null);
        LOGGER.debug("Exiting onVerifyRestoreCompleted with NodeName={},CvFDN = {}, JobResult = {}", jobEnv.getNodeName(), cvMO.getFdn(), jobResult.getJobResult());

        return jobResult;

    }

    /**
     * @param cvMO
     *            {@link ManagedObject}
     * 
     * @param jobEnv
     *            {@link JobEnvironment}
     * @return {@link JobResult}
     */
    private JobResult processMainActionResult(final JobEnvironment jobEnv, final ConfigurationVersionMO cvMO) {
        LOGGER.debug("Entering processMainActionResult() with NodeName={},CvFdn={}", jobEnv.getNodeName(), cvMO.getFdn());
        JobResult jobStatus = JobResult.FAILED;
        final CvActionMainAndAdditionalResultHolder actionResultInfoHolder = cvMO.getCvMainActionResultHolder();
        final int invokedActionId = activityUtils.getPersistedActionId(jobEnv);
        final String requestedRestoreType = getRequestedRestore(jobEnv.getMainJobAttributes(), jobEnv.getNodeName());
        String jobLog = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        LOGGER.debug("Main Action result of VerifyRestore task = {} , NodeName={}", actionResultInfoHolder.getCvActionMainResult().getMainResultMessage(), jobEnv.getNodeName());

        if (actionResultInfoHolder.getActionId() != invokedActionId) {
            jobLog = "Action result data not found for actionId " + invokedActionId + ". Assuming the operation failed.";
            activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(jobEnv.getActivityJobId(), null, jobLogList, null);
            return jobStatus;
        }

        reportActionResult(actionResultInfoHolder, jobLogList);
        reportActionAdditionalInfo(actionResultInfoHolder, jobLogList);
        reportMissingUps(cvMO, jobLogList);
        reportCorruptedUps(cvMO, jobLogList);

        boolean isNodeSupportsRestore = false;
        boolean isNodeSupportsForcedRestore = false;

        final List<CvActionAdditionalInfo> actionAdditionalInfo = actionResultInfoHolder.getActionAdditionalResult();
        for (final CvActionAdditionalInfo additionalInfo : actionAdditionalInfo) {
            if (additionalInfo.getInformation() == CVActionResultInformation.ACTION_RESTORE_IS_ALLOWED) {
                isNodeSupportsRestore = true;
            } else if (additionalInfo.getInformation() == CVActionResultInformation.ACTION_FORCED_RESTORE_IS_ALLOWED) {
                isNodeSupportsForcedRestore = true;
            }
        }

        if (isNodeSupportsForcedRestore && !isNodeSupportsRestore && BackupActivityConstants.ACTION_RESTORE_CV.equalsIgnoreCase(requestedRestoreType)) {
            jobLog = "Verify restore failed - only forced restore allowed by the node";
            activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        } else if (isNodeSupportsRestore || isNodeSupportsForcedRestore) {
            jobStatus = JobResult.SUCCESS;
            if (isNodeSupportsRestore) {
                jobLog = "Restore allowed by the node";
                activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
            }
            if (isNodeSupportsForcedRestore) {

                jobLog = "Forced Restore allowed by the node";
                activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
            }
            if (isNodeSupportsRestore && BackupActivityConstants.ACTION_FORCED_RESTORE_CV.equalsIgnoreCase(requestedRestoreType)) {
                final String isForcedRestore = "false";
                saveNodeSupportedRestore(isForcedRestore, jobEnv);

            }
        } else if (isInstallPossible) {
            jobStatus = JobResult.SUCCESS;
            jobLog = "Verify restore failed but missing/corrupted UPs can be installed";
            activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.WARN.toString());
        } else if (actionResultInfoHolder.getCvActionMainResult() == CVActionMainResult.EXECUTION_FAILED) {
            final List<CvActionAdditionalInfo> actionAdditionalInformation = actionResultInfoHolder.getActionAdditionalResult();
            final StringBuffer failedReason = new StringBuffer("Verify restore failed. Reasons: ");
            for (final CvActionAdditionalInfo additionalInfo : actionAdditionalInformation) {
                failedReason.append(additionalInfo.getInformation().getCVActionResultInformationDesc() + " ");
            }
            jobLog = failedReason.toString();
            activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());

        } else {
            jobLog = "No information if restore is possible was given. Assuming the operation failed.";
            activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }

        jobUpdateService.readAndUpdateRunningJobAttributes(jobEnv.getActivityJobId(), null, jobLogList, null);
        return jobStatus;

    }

    /**
     * @param isForcedRestore
     * @param jobEnv
     */
    private void saveNodeSupportedRestore(final String isForcedRestore, final JobEnvironment jobEnv) {
        LOGGER.debug("Entering saveNodeSupportedRestore() with isFrocedRestore={}, forActivityJobId={},Nodename={}", isForcedRestore, jobEnv.getNodeName(), jobEnv.getActivityJobId());
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        activityUtils.addJobProperty(BackupActivityConstants.FORCED_RESTORE, isForcedRestore, propertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(jobEnv.getNeJobId(), propertyList, null, null);
        LOGGER.debug("Exiting saveNodeSupportedRestore() method");

    }

    /**
     * report Missing UPs as node logs
     * 
     * @param cvMO
     * @param jobLogList
     */

    private void reportMissingUps(final ConfigurationVersionMO cvMO, final List<Map<String, Object>> jobLogList) {
        String jobLog = null;
        final List<AdminProductData> missingUps = cvMO.getMissingUps();
        for (final AdminProductData up : missingUps) {
            isInstallPossible = true;
            jobLog = "Missing Upgrade Package: ProductNumber=" + up.getProductNumber() + " ProductRevision= " + up.getProductRevision() + " ProductName=" + up.getProductName();
            activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.WARN.toString());
        }
        if (missingUps.isEmpty()) {
            jobLog = "No Missing Upgrade Packages reported.";
            activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());

        }
    }

    /**
     * report corrupted UPs as node logs
     * 
     * @param cvMO
     * @param jobLogList
     */

    private void reportCorruptedUps(final ConfigurationVersionMO cvMO, final List<Map<String, Object>> jobLogList) {
        String jobLog = null;
        final List<AdminProductData> corruptedUps = cvMO.getCorruptedUps();
        for (final AdminProductData up : corruptedUps) {
            isInstallPossible = true;
            jobLog = "Corrupted Upgrade Package: ProductNumber=" + up.getProductNumber() + " ProductRevision= " + up.getProductRevision() + " ProductName=" + up.getProductName();
            activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.WARN.toString());
        }
        if (corruptedUps.isEmpty()) {
            jobLog = "No Corrupted Upgrade Packages reported.";
            activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
    }

    /**
     * @param mainJobAttributes
     * @return operator requested restore(forced/normal)
     */
    @SuppressWarnings("unchecked")
    private String getRequestedRestore(final Map<String, Object> mainJobAttributes, final String neName) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(neName);
        final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
        final String neType = networkElementList.get(0).getNeType();
        final String platform = networkElementList.get(0).getPlatformType().name();
        final List<String> keyList = new ArrayList<String>();
        keyList.add(BackupActivityConstants.FORCED_RESTORE);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platform);
        String isForcedRestore = keyValueMap.get(BackupActivityConstants.FORCED_RESTORE);
        LOGGER.debug("isForcedRestore {}", isForcedRestore);
        if (isForcedRestore != null) {
            isForcedRestore = isForcedRestore.equalsIgnoreCase("true") ? BackupActivityConstants.ACTION_FORCED_RESTORE_CV : BackupActivityConstants.ACTION_RESTORE_CV;
        }
        LOGGER.debug("Restore Type {}", isForcedRestore);
        return isForcedRestore;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.VERIFY_RESTORE_CV;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.VERIFY_PROCESS_NOTIFICATION;
    }
}
