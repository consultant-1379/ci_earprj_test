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
import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY;
import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.CancelCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.BackupUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentDetailedActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentMainActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.NodeRestartActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.common.SetStartableActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.CppNodeRestartConfigParamListener;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.NodeRestartConfiguartionParamProvider;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.RestartActivityConstants;
import com.ericsson.oss.services.shm.es.polling.PollingActivityStatusManager;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivity;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.es.polling.api.ReadCallStatusEnum;
import com.ericsson.oss.services.shm.job.cpp.activity.NodeRestartActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.shm.inventory.backup.entities.AdminProductData;

/**
 * This class facilitates the restore of backup configuration version of CPP based node by invoking the ConfigurationVersion MO action that initializes the restore activity.
 *
 * @author tcsdivc
 *
 */

@EServiceQualifier("CPP.RESTORE.restore")
@ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@SuppressWarnings("PMD.TooManyFields")
public class RestoreService extends AbstractBackupActivity implements Activity, ActivityCallback, ActivityCompleteCallBack, PollingActivity, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreService.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

    @Inject
    private RestorePrecheckHandler restorePrecheckHandler;

    @Inject
    private CancelCompleteTimer cancelCompleteTimer;

    @Inject
    private BackupUtils notificationHelper;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private NodeRestartActivityHandler nodeRestartActivityHandler;

    @Inject
    private SetStartableActivityHandler setStartableActivityHandler;

    @Inject
    private NodeRestartConfiguartionParamProvider nodeRestartConfiguartionParamProvider;

    @Inject
    private CppNodeRestartConfigParamListener cppNodeRestartConfigParamListener;

    @Inject
    private PollingActivityStatusManager pollingActivityStatusManager;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    public static final String DOWNLOADED_CV_TYPE = "DOWNLOADED";
    private static final String ACTIVITY_NAME = "restore";

    /**
     * This method validates CV MO is available or not if Restore activity can be started or not and sends back the activity result to Work Flow Service.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        return restorePrecheckHandler.getRestorePrecheckResult(activityJobId, ActivityConstants.RESTORE, ACTIVITY_NAME);
    }

    /**
     * This method asynchronously validates CV MO is available or not if Restore activity can be started or not and notifies the activity result to Work Flow Service.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        LOGGER.debug("Inside async precheck method of activityJobId {}", activityJobId);
        final ActivityStepResultEnum activityStepResultEnum = restorePrecheckHandler.getRestorePrecheckResult(activityJobId, ActivityConstants.RESTORE, ACTIVITY_NAME).getActivityResultEnum();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(jobEnvironment, ActivityConstants.RESTORE, activityStepResultEnum);
        LOGGER.debug("Sending back Activity Step Result to WorkFlow in Restore activity Precheck with status : {} for node {} with activityJobId {} and neJobId {}", activityStepResultEnum,
                jobEnvironment.getNodeName(), jobEnvironment.getActivityJobId(), jobEnvironment.getNeJobId());
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This performs the MO action and sends back activity result to Work Flow Service
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Inside execute method activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        String configurationVersionName = "";
        try {
            configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, BackupActivityConstants.CV_NAME);
            final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(nodeName);
            String configurationVersionMoFdn = null;
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
            if (moAttributesMap != null) {
                configurationVersionMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
            }
            final String cvType = cvTypeIfPresentOnNode(jobEnvironment);
            if (cvType != null) {
                if (DOWNLOADED_CV_TYPE.equalsIgnoreCase(cvType)) {
                    LOGGER.debug("Proceeding to trigger restore action");
                    executeRestoreAction(activityJobId, jobEnvironment, configurationVersionMoFdn, configurationVersionName);
                } else if (!DOWNLOADED_CV_TYPE.equalsIgnoreCase(cvType)) {
                    LOGGER.debug("proceed to restore required CV by making it as seStartable first and then node restart");
                    final String logMessge = String.format(JobLogConstants.RESTORE_BY_SETSTARTABLE_NODERESTART, configurationVersionName);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessge, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
                    executeNodeRestartAction(jobEnvironment, configurationVersionMoFdn, configurationVersionName);
                }
            } else {
                jobLogList.clear();
                LOGGER.debug("Restore activity has failed. Reason CV {} does not exists on the node {}", configurationVersionName, nodeName);
                final String logMessge = String.format(JobLogConstants.RESTORE_ACTIVITY_FAILED_CV_DOES_NOT_EXISTS, configurationVersionName);
                final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessge, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, getActivityType(), null);
            }
        } catch (final Exception e) {
            LOGGER.error("Restore activity has failed. Reason for nodeName {} due to unknown exception :: {}", nodeName, e);
            final String logMessge = String.format(JobLogConstants.RESTORE_ACTIVITY_FAILED, configurationVersionName, e.getMessage());
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessge, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, getActivityType(), null);
        }
    }

    @SuppressWarnings("unchecked")
    private void executeRestoreAction(final long activityJobId, final JobEnvironment jobEnvironment, final String configurationVersionMoFdn, final String configurationVersionName) {
        LOGGER.debug("Inside executeRestoreAction method activityJobId {}", activityJobId);
        boolean executedWithoutException = false;
        String actionType = null;
        FdnNotificationSubject fdnNotificationSubject = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        JobResult activityResult = JobResult.FAILED;
        Integer actionId = -1;
        final String nodeName = jobEnvironment.getNodeName();
        final long mainJobId = jobEnvironment.getMainJobId();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        String neType = null;
        String platform = null;
        String jobExecutionUser = null;
        try {
            final List<NetworkElement> networkElementList = getNetworkElements(Arrays.asList(nodeName));
            jobExecutionUser = activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId());
            if (!networkElementList.isEmpty()) {
                neType = networkElementList.get(0).getNeType();
                platform = networkElementList.get(0).getPlatformType().name();
            }
            final Map<String, String> autoconfigurationMap = getActivityParameters(nodeName, neType, platform, jobConfigurationDetails);
            final String autoConfigurationInformation = autoconfigurationMap.get(ActivityConstants.AUTO_CONFIGURATION);
            actionType = getActionType(autoconfigurationMap);

            LOGGER.debug("ConfigurationVersionName {} autoConfigurationInformation {} ,actionType {}", configurationVersionName, autoConfigurationInformation, actionType);

            systemRecorder.recordEvent(jobExecutionUser, SHMEvents.RESTORE_EXECUTE, EventLevel.COARSE, nodeName, configurationVersionMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "\"Restore\" activity for the Configuration Version: " + configurationVersionName + " is triggering on the node.", new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
            fdnNotificationSubject = new FdnNotificationSubject(configurationVersionMoFdn, activityJobId, jobActivityInfo);
            notificationRegistry.register(fdnNotificationSubject);
            final Map<String, Object> actionArguments = prepareActionArguments(configurationVersionName, autoConfigurationInformation, actionType);
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.RESTORE_SERVICE, CommandPhase.STARTED, nodeName, configurationVersionMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));

            actionId = commonCvOperations.executeActionOnMo(actionType, configurationVersionMoFdn, actionArguments);
            executedWithoutException = true;
            activityResult = JobResult.SUCCESS;
            final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            LOGGER.debug("Restore action performed successfuly for activty {}", activityJobId);
        } catch (final Exception e) {
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.RESTORE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, configurationVersionMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));
            notificationRegistry.removeSubject(fdnNotificationSubject);
            String logMessage = null;
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.RESTORE) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.RESTORE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
            logMessage = "Unable to start MO Action with action: " + actionType + " on CV MO having FDN: " + configurationVersionMoFdn;
            LOGGER.error("{} for activity {} because:{}", logMessage, activityJobId, e);

        }
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        if (executedWithoutException) {
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.name(),
                    BackupActivityConstants.ACTION_RESTORE_CV);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ADDING_CV, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED_WITH_ID, ActivityConstants.RESTORE, actionType, actionId, activityTimeout),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.RESTORE_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, configurationVersionMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE));

            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_ID, Integer.toString(actionId));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ShmConstants.TRUE.toLowerCase());
            subscribeForPolling(activityJobId, configurationVersionMoFdn);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        }
        if (activityResult == JobResult.FAILED) {
            // Persist Result as Failed in case of unable to trigger action.
            LOGGER.error(" Restore activity with activityjobid:{} has failed", activityJobId);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityResult.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, getActivityType(), null);
        }
    }

    private Map<String, String> getActivityParameters(final String nodeName, final String neType, final String platform, final Map<String, Object> jobConfigurationDetails) {
        final List<String> resorekeyList = Arrays.asList(ActivityConstants.AUTO_CONFIGURATION, ActivityConstants.FORCED_RESTORE);
        return jobPropertyUtils.getPropertyValue(resorekeyList, jobConfigurationDetails, nodeName, neType, platform);
    }

    private String getActionType(final Map<String, String> autoconfigurationMap) {
        final String isForcedRestore = autoconfigurationMap.get(ActivityConstants.FORCED_RESTORE);
        final String actionType = ActivityConstants.CHECK_TRUE.equalsIgnoreCase(isForcedRestore) ? BackupActivityConstants.ACTION_FORCED_RESTORE : BackupActivityConstants.ACTION_RESTORE;
        LOGGER.debug("Action type to be execute : {}", actionType);
        return actionType;
    }

    private void executeNodeRestartAction(final JobEnvironment jobEnvironment, final String configurationVersionMoFdn, final String cvName) throws MoNotFoundException {
        LOGGER.debug("Entering into executeNodeRestartAction with activityJobId : {} and cvName: {}", jobEnvironment.getActivityJobId(), cvName);
        final boolean performActionStatus = setStartableActivityHandler.executeSetStartableMoAction(jobEnvironment, cvName, configurationVersionMoFdn);
        if (performActionStatus) {
            final String neType = networkElementRetrievalBean.getNeType(jobEnvironment.getNodeName());
            final String platform = platformTypeProviderImpl.getPlatformType(neType).name();
            final Map<String, Object> nodereStartActionArguments = prepareNodeRestartActionArguments(cvName);
            final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = getActivityInfo(jobEnvironment.getActivityJobId(),
                    activityTimeoutsService.getActivityTimeoutAsInteger(neType, platform, JobTypeEnum.NODERESTART.name(), NodeRestartActivityConstants.MANUALRESTART),
                    cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval(), cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType));
            nodeRestartActivityHandler.executeNodeRestartAction(jobEnvironment, nodereStartActionArguments, nodeRestartJobActivityInfo);
        } else {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            final String logMessge = String.format(JobLogConstants.ACTIVITY_FAILED, setStartableActivityHandler.getActivityType());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessge, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(jobEnvironment.getActivityJobId(), jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(jobEnvironment, jobEnvironment.getActivityJobId(), setStartableActivityHandler.getActivityType(), null);
        }
    }

    private Map<String, Object> prepareNodeRestartActionArguments(final String cvName) {
        final Map<String, Object> nodereStartActionArguments = new HashMap<>();
        final String restartrank = nodeRestartConfiguartionParamProvider.getRestartRankConfigParameter(RestartActivityConstants.RESTART_RANK_KEY);
        final String restartReason = nodeRestartConfiguartionParamProvider.getRestartReasonConfigParameter(RestartActivityConstants.RESTART_REASON_KEY);
        LOGGER.debug("restartRank : {} and restartReason : {}", restartrank, restartReason);
        nodereStartActionArguments.put(RestartActivityConstants.RESTART_RANK, restartrank);
        nodereStartActionArguments.put(RestartActivityConstants.RESTART_REASON, restartReason);
        nodereStartActionArguments.put(RestartActivityConstants.RESTART_INFO, String.format(RestartActivityConstants.RESTART_INFO_MESSAGE, cvName));
        return nodereStartActionArguments;
    }

    private NodeRestartJobActivityInfo getActivityInfo(final long activityJobId, final int maxTimeForCppNodeRestart, final int waitIntervalForEachRetry, final int cppNodeRestartSleepTime) {
        final ActivityInfo activityInfoAnnotation = this.getClass().getAnnotation(ActivityInfo.class);
        return new NodeRestartJobActivityInfo(activityJobId, activityInfoAnnotation.activityName(), activityInfoAnnotation.jobType(), activityInfoAnnotation.platform(), maxTimeForCppNodeRestart,
                waitIntervalForEachRetry, cppNodeRestartSleepTime);
    }

    /**
     * @param neFdns
     * @return networkElementsList
     */
    private List<NetworkElement> getNetworkElements(final List<String> neFdns) {
        List<NetworkElement> networkElementsList = new ArrayList<>();
        try {
            networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);

        } catch (RetriableCommandException | IllegalArgumentException ex) {
            LOGGER.error("Exception while fetching networkEement of node : {}", neFdns);
        }
        return networkElementsList;
    }

    /**
     * This method processes the notifications by fetching the notification subject and validates the notification. It de-register from the notification as it founds activity is completed and notifies
     * to WorkFlowService or else it will wait for another notification.
     *
     * @param notification
     * @return void
     *
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered cpp restore activity - processNotification with event type : {} ", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp restore activity - Discarding non-AVC notification.");
            return;
        }
        LOGGER.debug("Entering RestoreService.processNotification() with notification: {}", notification);

        try {

            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
            final CvActivity newCvActivity = configurationVersionUtils.getNewCvActivity(notification);
            final String cvMoFdn = activityUtils.getNotifiedFDN(notification);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            final boolean isRestoreCompleted = processAndReportActionResult(cvMoFdn, activityJobId, newCvActivity);
            if (isRestoreCompleted) {
                // just to check how many nodes passed by getting notifications.
                final String logMessage = "Restore activity completed in processNotification for the moFdn " + notificationSubject.getKey();
                final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, ACTIVITY_NAME);
                systemRecorder.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventName, EventLevel.COARSE, notificationSubject.getKey(), cvMoFdn,
                        "SHM:" + activityJobId + ":" + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS));
            }
        } catch (final Exception ex) {
            final String logMessage = String.format("Exception occurred while process notification of activty : %s  with notification : %s. Exception is : ", ActivityConstants.RESTORE, notification);
            LOGGER.error(logMessage, ex);
        }
    }

    /**
     * @param cvMoFdn
     * @param activityJobId
     * @param newCvActivity
     */
    private boolean processAndReportActionResult(final String cvMoFdn, final long activityJobId, final CvActivity newCvActivity) {
        final JobEnvironment jobEnv = activityUtils.getJobEnvironment(activityJobId);
        if (newCvActivity.getDetailedActivity() != CVCurrentDetailedActivity.UNKNOWN) {
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            activityUtils.addJobLog(newCvActivity.getDetailedActivityDesc(), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        if (newCvActivity.getMainActivity() == CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV) {
            final List<Map<String, Object>> jobPropList = new ArrayList<>();
            activityUtils.addJobProperty(ActivityConstants.HAVE_SEEN_RESTORING, ShmConstants.TRUE.toLowerCase(), jobPropList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropList, null, null);
        }
        boolean isRestoreCompleted = false;
        if (isActionCompleted(jobEnv, newCvActivity)) {
            isRestoreCompleted = true;
            LOGGER.info("Restore action execution completed on NodeName={}", jobEnv.getNodeName());
            activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(jobEnv.getActivityJobId(), this.getClass()));
            final long activityStartTime = ((Date) jobEnv.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            if (newCvActivity.getDetailedActivity() == CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION) {
                final List<Map<String, Object>> jobLogList = new ArrayList<>();
                activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.RESTORE), JobLogType.SYSTEM.toString(), jobLogList,
                        JobLogLevel.INFO.toString());
                pollingActivityStatusManager.unsubscribeFromPolling(pollingActivityStatusManager.getActivityPollingCacheKey(cvMoFdn, activityJobId));
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
                sendNotificationResultToWfs(jobLogList, jobEnv, cvMoFdn, JobResult.SUCCESS);
            } else {
                final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(jobEnv.getActivityJobId(), this.getClass());
                activityCompleteTimer.startTimer(jobActivityInfo);
                LOGGER.debug("Activity wait timer is started for {} activity with activityJobId:{}", ActivityConstants.RESTORE, jobEnv.getActivityJobId());
            }
        }
        return isRestoreCompleted;
    }

    /**
     * @param jobEnv
     * @param newCvActivity
     * @param jobLogs
     * @param notificationTime
     * @return
     */
    private boolean isActionCompleted(final JobEnvironment jobEnv, final CvActivity newCvActivity) {
        boolean isActionCompleted = false;
        final CVCurrentDetailedActivity currentDetailedActivity = newCvActivity.getDetailedActivity();
        final CVCurrentMainActivity currentMainActivity = newCvActivity.getMainActivity();
        if (currentDetailedActivity == CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION || currentDetailedActivity == CVCurrentDetailedActivity.EXECUTION_FAILED
                || (currentMainActivity == CVCurrentMainActivity.IDLE && isRestoreStarted(jobEnv))) {
            isActionCompleted = true;
        }
        return isActionCompleted;
    }

    private boolean isRestoreStarted(final JobEnvironment jobEnv) {
        final String haveBeenRestoring = activityUtils.getActivityJobAttributeValue(jobEnv.getActivityJobAttributes(), ActivityConstants.HAVE_SEEN_RESTORING);
        return Boolean.parseBoolean(haveBeenRestoring);
    }

    /**
     * This method handles timeout scenario for UploadCvService Activity and checks the actionResult on node to see if it is failed or success.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();

        activityStepResult.setActivityResultEnum(handleTimeout(jobEnvironment));

        return activityStepResult;
    }

    /**
     * This method handles timeout scenario asynchronously for Restore Activity and sends node read calls to check if Restore activity or Node Restart is successful.
     * 
     * @param activityJobId
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside async HandleTimeout method of activityJobId {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final ActivityStepResultEnum activityStepResultEnum = handleTimeout(jobEnvironment);
        LOGGER.debug("Sending back Activity Step Result to WorkFlow in Restore activity handletimeout with status : {} for node {} with activityJobId {} and neJobId {}", activityStepResultEnum,
                jobEnvironment.getNodeName(), jobEnvironment.getActivityJobId(), jobEnvironment.getNeJobId());
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(jobEnvironment, ActivityConstants.RESTORE, activityStepResultEnum);
    }

    /**
     * @param jobEnvironment
     */
    private ActivityStepResultEnum handleTimeout(final JobEnvironment jobEnvironment) {
        final long activityJobId = jobEnvironment.getActivityJobId();
        LOGGER.debug("Entering RestoreService.handleTimeout() for  activityJobId  {}", activityJobId);
        ActivityStepResultEnum activityStepResult = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        try {
            final String lastTriggeredAction = getLastTriggeredActionName(jobEnvironment);
            if (BackupActivityConstants.ACTION_SET_STARTABLE_CV.equalsIgnoreCase(lastTriggeredAction)) {
                activityStepResult = setStartableActivityHandler.handleTimeoutSetStartableAction(jobEnvironment).getActivityResultEnum();
            } else if (RestartActivityConstants.ACTION_NAME.equalsIgnoreCase(lastTriggeredAction)) {
                nodeRestartActivityHandler.cancelTimer(activityJobId);
                final String logMessage = String.format(JobLogConstants.OPERATION_TIMED_OUT, RestartActivityConstants.RESTART_NODE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

                final boolean isActionCompleted = nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE);
                activityStepResult = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
                if (!isActionCompleted) {
                    activityStepResult = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
                }
                final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
            } else {
                final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
                activityStepResult = handleTimeoutRestoreAction(activityJobId, jobEnvironment, activityStartTime).getActivityResultEnum();
            }
        } catch (final Exception ex) {
            LOGGER.error("An exception occured while handlingTimeout for activityJobId {} and Exception is: ", activityJobId, ex);
            final String logMessage = String.format(JobLogConstants.FAILURE_REASON, ExceptionParser.getReason(ex));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        LOGGER.debug("Exiting from RestoreService.handleTimeout() for  activityJobId = {}", activityJobId);
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    private String getLastTriggeredActionName(final JobEnvironment jobEnvironment) {
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (activityJobPropertyList != null) {
            for (final Map<String, Object> activityAttribute : activityJobPropertyList) {
                if (ActivityConstants.ACTION_TRIGGERED.equalsIgnoreCase((String) activityAttribute.get(ActivityConstants.JOB_PROP_KEY))) {
                    return (String) activityAttribute.get(ActivityConstants.JOB_PROP_VALUE);

                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ActivityStepResult handleTimeoutRestoreAction(final long activityJobId, final JobEnvironment jobEnvironment, final long activityStartTime) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String cvMoFdn = null;
        Map<String, Object> cvAttributes = new HashMap<>();

        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName());
        if (moAttributesMap != null) {
            cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
            cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
        }

        pollingActivityStatusManager.unsubscribeFromPolling(pollingActivityStatusManager.getActivityPollingCacheKey(cvMoFdn, activityJobId));
        activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.RESTORE);

        final JobResult jobResult = evaluateJobResult(jobEnvironment, cvAttributes, jobLogList);
        if (jobResult == JobResult.SUCCESS) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, ACTIVITY_NAME);
        systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), eventName, EventLevel.COARSE, jobEnvironment.getNodeName(), cvMoFdn,
                "SHM:" + activityJobId + ":" + jobEnvironment.getNodeName() + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        LOGGER.debug("Exiting from  NodeRestartService.handleTimeoutRestoreAction()");
        return activityStepResult;
    }

    /**
     * @param cvName
     * @param autoConfigurationInformation
     * @param restoreType
     * @return
     */
    private Map<String, Object> prepareActionArguments(final String cvName, final String autoConfigurationInformation, final String restoreType) {
        final Map<String, Object> actionArguments = new HashMap<>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);
        if (restoreType == BackupActivityConstants.ACTION_FORCED_RESTORE) {
            actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_AUTO_CONFIGURATION_ON, autoConfigurationInformation);
        } else {
            actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_AUTO_CONFIGURATION_INFO, autoConfigurationInformation);
        }
        return actionArguments;
    }

    /**
     * This method invoke cancelRestore action towards node if the cancel is called while restore activity
     * 
     * @author esinstu
     * @param activityJobId
     * 
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside Restore Service cancel() with activityJobId : {} ", activityJobId);
        ActivityStepResult activityStepResult = null;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        final String lastTriggeredAction = getLastTriggeredActionName(jobEnvironment);

        if (BackupActivityConstants.ACTION_SET_STARTABLE_CV.equalsIgnoreCase(lastTriggeredAction)) {
            activityStepResult = setStartableActivityHandler.cancelSetStartableAction(jobEnvironment);
        } else if (RestartActivityConstants.ACTION_NAME.equalsIgnoreCase(lastTriggeredAction)) {
            activityStepResult = nodeRestartActivityHandler.cancelNodeRestartAction(jobEnvironment);
        } else {
            activityStepResult = cancelRestoreAction(activityJobId, jobEnvironment);
        }

        return activityStepResult;

    }

    private ActivityStepResult cancelRestoreAction(final long activityJobId, final JobEnvironment jobEnvironment) {

        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.RESTORE);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, ShmConstants.TRUE.toLowerCase());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);

        jobPropertyList.clear();
        jobLogList.clear();

        String cvMoFdn = null;
        final String nodeName = jobEnvironment.getNodeName();
        final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
        if (moAttributesMap != null) {
            cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
        }
        LOGGER.debug("cvMoFdn : {}", cvMoFdn);
        final String actionType = BackupActivityConstants.ACTION_CANCEL_RESTORE;
        final Map<String, Object> actionArguments = new HashMap<>();
        int actionId = -1;
        String logMessage = null;

        try {
            actionId = commonCvOperations.executeActionOnMo(actionType, cvMoFdn, actionArguments);
            LOGGER.debug("actionId : {}", actionId);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
            logMessage = String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, ActivityConstants.RESTORE) + " on node: " + nodeName;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, ActivityConstants.RESTORE), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.RESTORE_CANCEL, EventLevel.COARSE, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(jobEnvironment.getActivityJobId(), this.getClass());
            LOGGER.debug("Timer starting in RestoreService Cancel");
            cancelCompleteTimer.startTimer(jobActivityInfo);
        } catch (final Exception e) {
            LOGGER.error("Unable to start cancel restore Action {}", e.getMessage());
            logMessage = "Unable to start cancel restore Action with action: " + actionType + " on CV MO having FDN: " + cvMoFdn + " because " + e.getMessage();
            LOGGER.debug(logMessage);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.CANCEL_RESTORE) + " " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.CANCEL_RESTORE), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            }
            systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.RESTORE_CANCEL, EventLevel.COARSE, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
        }
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack# onActionComplete(long)
     */
    @Override
    @Asynchronous
    public void onActionComplete(final long activityJobId) {
        LOGGER.debug("Entered onActionComplete with activity job id {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String lastTriggeredAction = getLastTriggeredActionName(jobEnvironment);
        if (RestartActivityConstants.ACTION_NAME.equalsIgnoreCase(lastTriggeredAction)) {
            final boolean isActivityCompleted = nodeRestartActivityHandler.isRestoreActionCompleted(jobEnvironment, ActivityConstants.TRUE);
            if (isActivityCompleted) {
                activityUtils.sendNotificationToWFS(jobEnvironment, jobEnvironment.getActivityJobId(), nodeRestartActivityHandler.getActivityType(), null);
            }
        } else {
            final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName());
            onActionCompleteForRestore(moAttributesMap, jobEnvironment);
        }
        final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
    }

    @SuppressWarnings("unchecked")
    private void onActionCompleteForRestore(final Map<String, Object> moAttributesMap, final JobEnvironment jobEnvironment) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        String cvMoFdn = null;
        Map<String, Object> cvAttributes = new HashMap<>();
        if (moAttributesMap != null) {
            cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
            cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
        }
        pollingActivityStatusManager.unsubscribeFromPolling(pollingActivityStatusManager.getActivityPollingCacheKey(cvMoFdn, jobEnvironment.getActivityJobId()));
        final JobResult jobResult = evaluateJobResult(jobEnvironment, cvAttributes, jobLogList);
        sendNotificationResultToWfs(jobLogList, jobEnvironment, cvMoFdn, jobResult);
    }

    /**
     * @param jobEnvironment
     * @param cvAttributes
     * @param jobLogList
     * @return
     */
    @SuppressWarnings("unchecked")
    private JobResult evaluateJobResult(final JobEnvironment jobEnvironment, final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList) {
        final long activityJobId = jobEnvironment.getActivityJobId();
        JobResult jobResult = JobResult.FAILED;
        try {
            final CVCurrentDetailedActivity currentDetailedActivity = configurationVersionUtils.getCvActivity(cvAttributes).getDetailedActivity();
            if (currentDetailedActivity == CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION) {
                jobResult = JobResult.SUCCESS;
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.RESTORE), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
            } else {
                final int actionIdFromDatabase = configurationVersionUtils.getActionId(jobEnvironment.getActivityJobAttributes());
                final Map<String, Object> currentActionResultData = (Map<String, Object>) cvAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
                final int actionIdFromNode = (int) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_ID);
                if (actionIdFromNode == actionIdFromDatabase) {
                    if (currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT) != null) {
                        final String cvActionMainResultValue = (String) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT);
                        if (cvActionMainResultValue != null && !cvActionMainResultValue.equals("")) {
                            final CVActionMainResult cvActionMainResult = CVActionMainResult.getCvActionMainResult(cvActionMainResultValue);
                            if (notificationHelper.isActionFailed(cvActionMainResult)) {
                                processFailureReason(cvAttributes, jobLogList);
                            }
                        }
                    }
                } else if (isRestoreStarted(jobEnvironment)) {
                    activityUtils.addJobLog("Action result data not found for actionId " + actionIdFromDatabase + ". Assuming the operation failed.", JobLogType.SYSTEM.toString(), jobLogList,
                            JobLogLevel.ERROR.toString());
                } else {
                    activityUtils.addJobLog("Action did not seem to start and no action result found. Assuming the operation failed.", JobLogType.SYSTEM.toString(), jobLogList,
                            JobLogLevel.ERROR.toString());
                }
                addCorruptedAndMissingUpDetailsInLog(cvAttributes, jobLogList);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while deriving jobResult for activityJobId : {}, Exception : {} ", activityJobId, ex);
        }
        return jobResult;
    }

    /**
     * @param cvAttributes
     * @param jobLogList
     */
    private void addCorruptedAndMissingUpDetailsInLog(final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList) {
        final List<AdminProductData> corruptedUps = configurationVersionUtils.getCorrputedUps(cvAttributes);
        addUpInlogs(corruptedUps, jobLogList, JobLogConstants.CORRUPTED);
        final List<AdminProductData> missingUps = configurationVersionUtils.getMissingUps(cvAttributes);
        addUpInlogs(missingUps, jobLogList, JobLogConstants.MISSING);
    }

    /**
     * @param corruptedUps
     * @param jobLogList
     */
    private void addUpInlogs(final List<AdminProductData> corruptedUps, final List<Map<String, Object>> jobLogList, final String upType) {
        if (corruptedUps != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.UP_AS_FOLLOWS, upType, corruptedUps.size()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            int count = 1;
            for (final AdminProductData corruptedUp : corruptedUps) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.UP_DETAILS, upType, count, corruptedUp.toString()), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                count++;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity #getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.RESTORE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity #getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.RESTORE_EXECUTE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("Entering into cancelTimeout for the activity Job ID : {}", activityJobId);
        ActivityStepResult activityStepResult = null;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        final String lastTriggeredAction = getLastTriggeredActionName(jobEnvironment);

        if (BackupActivityConstants.ACTION_SET_STARTABLE_CV.equalsIgnoreCase(lastTriggeredAction)) {
            activityStepResult = setStartableActivityHandler.cancelTimeoutSetStartable(finalizeResult, jobEnvironment);
        } else if (RestartActivityConstants.ACTION_NAME.equalsIgnoreCase(lastTriggeredAction)) {
            activityStepResult = nodeRestartActivityHandler.cancelTimeout(finalizeResult, jobEnvironment, ActivityConstants.TRUE);
        } else {
            activityStepResult = cancelTimeoutActivity(activityJobId, finalizeResult);
        }
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    private String cvTypeIfPresentOnNode(final JobEnvironment jobEnvironment) {
        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName());
        final String configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(jobEnvironment.getMainJobAttributes(), jobEnvironment.getNodeName(), BackupActivityConstants.CV_NAME);
        final Map<String, Object> cvMoAttribute = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
        return restorePrecheckHandler.getCVTypeIfPresentOnNode(((List<Map<String, Object>>) cvMoAttribute.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)), configurationVersionName);
    }

    private void subscribeForPolling(final long activityJobId, final String cvMoFdn) {
        final boolean pollingEnabled = pollingActivityConfiguration.isPollingEnabledForJobsOnCppNodes();
        if (pollingEnabled) {
            final int waitTimeToStartPolling = pollingActivityConfiguration.getCppRestoreJobRestoreActivityWaitTimeToStartPolling();
            final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
            pollingActivityStatusManager.subscribeForPolling(cvMoFdn, jobActivityInfo, waitTimeToStartPolling);
        }
    }

    /**
     * This will be called at every polling timer expire. Default interval time value for polling timer is 1min.
     * 
     * @param activityJobId
     * @param cvMoFdn
     */
    @Asynchronous
    @Override
    public void readActivityStatus(final long activityJobId, final String cvMoFdn) {
        LOGGER.debug("Entered into RestoreService.readActivityStatus - CPP with activityJobId: {} ", activityJobId);
        try {
            final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
            if (!isActivityCompleted) {
                final Map<String, Object> attributeData = getCvMoAttributesByFdn(activityJobId, cvMoFdn);
                LOGGER.info("CV MO attribute values resturned from node in RestoreService.readActivityStatus are: {} for the activityJobId: {} and FDN: {}", attributeData, activityJobId, cvMoFdn);
                if (!attributeData.isEmpty()) {
                    final CvActivity newCvActivity = configurationVersionUtils.getCvActivity(attributeData);
                    final boolean isRestoreCompleted = processAndReportActionResult(cvMoFdn, activityJobId, newCvActivity);
                    if (isRestoreCompleted) {
                        // to check how many nodes passed in polling.
                        final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
                        final String logMessage = "Restore activity completed in polling for the moFdn " + cvMoFdn;
                        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, ACTIVITY_NAME);
                        systemRecorder.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventName, EventLevel.COARSE, cvMoFdn, cvMoFdn,
                                "SHM:" + activityJobId + ":" + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_POLLING));
                        final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING, ACTIVITY_NAME);
                        final List<Map<String, Object>> jobLogList = new ArrayList<>();
                        activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.DEBUG.toString());
                        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
                    }
                }
            } else {
                LOGGER.info(
                        "Found Activity result already persisted in ActivityJob PO, Assuming activity completed on the node and hence Unsubscribing for polling for the activityJobId: {} and FDN: {}.",
                        activityJobId, cvMoFdn);
                pollingActivityStatusManager.unsubscribeFromPolling(pollingActivityStatusManager.getActivityPollingCacheKey(cvMoFdn, activityJobId));
            }
        } catch (final Exception ex) {
            LOGGER.error("An Exception occurred in RestoreService.readActivityStatus for the activityJobId:{} and UP MO FDN: {}. Exception is: ", activityJobId, cvMoFdn, ex);
        }
    }

    private Map<String, Object> getCvMoAttributesByFdn(final long activityJobId, final String cvMoFdn) {

        pollingActivityStatusManager.updateReadCallStatus(activityJobId, cvMoFdn, ReadCallStatusEnum.IN_PROGRESS);
        final String[] cvMoAttributes = { CURRENT_MAIN_ACTIVITY, CURRENT_DETAILED_ACTIVITY };
        Map<String, Object> attributeData = new HashMap<>();
        try {
            attributeData = commonCvOperations.getCVMoAttributesFromNode(cvMoFdn, cvMoAttributes);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while reading data from the node with cvMoFdn : {}. Exception is:", cvMoFdn, ex);
        }
        pollingActivityStatusManager.updateReadCallStatus(activityJobId, cvMoFdn, ReadCallStatusEnum.COMPLETED);
        return attributeData;
    }

    @Override
    protected void sendNotificationResultToWfs(final List<Map<String, Object>> jobLogList, final JobEnvironment jobEnvironment, final String cvMoFdn, final JobResult jobResult) {
        final String activityType = getActivityType();
        final long activityJobId = jobEnvironment.getActivityJobId();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.toString(), jobPropertyList);
        final boolean isJobResultedPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isJobResultedPersisted) {
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, activityType, null);
        }

    }

    /**
     * This method will fail the Restore activity if the timeout of precheck is reached.
     * 
     * @param activityJobId
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside Restore Service AsyncPrecheckTimeout with activityJobID : {}", activityJobId);
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.RESTORE);
    }

    /**
     * This method will fail the Restore activity if the timeout of HandleTimeout is reached.
     * 
     * @param activityJobId
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside Restore Service Service timeoutForAsyncHandleTimeout with activityJobID : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.RESTORE);
    }
}
