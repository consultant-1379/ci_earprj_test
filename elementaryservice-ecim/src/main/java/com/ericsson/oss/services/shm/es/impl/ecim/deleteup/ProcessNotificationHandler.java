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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectDeletedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@Traceable
@RequestScoped
@SuppressWarnings("PMD.TooManyFields")
public class ProcessNotificationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNotificationHandler.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private EcimCommonUtils ecimCommonUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private DeleteUpgradePackageDataCollector deleteUpJobDataCollector;

    @Inject
    private DeleteUpgradePackageJobDataCollectorRetryProxy deleteUpDataCollectRetryProxy;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    private String nodeName = "";
    private JobEnvironment jobEnvironment = null;
    private NetworkElementData networkElement = null;

    private NEJobStaticData neJobStaticData = null;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM -delete upgradepackage - processNotification with event type : {}", notification.getNotificationEventType());

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);

        initializeVariables(activityJobId);

        Map<String, AttributeChangeData> modifiedAttributes = new HashMap<>();
        if (NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        } else if (NotificationEventTypeEnum.DELETE.equals(notification.getNotificationEventType())) {
            DpsObjectDeletedEvent event = (DpsObjectDeletedEvent) notification.getDpsDataChangedEvent();
            LOGGER.info("DpsObjectDeletedEvent event getAttributeValues: {}, getFdn {}, getPoId {}", event.getAttributeValues(), event.getFdn(), event.getPoId());
        } else {
            LOGGER.info("ECIM - delete upgradepackage - Discarding non-AVC/DELETE notification.");
            return;
        }
        try {
            AsyncActionProgress progressReport = new AsyncActionProgress(new HashMap<String, Object>());
            if (NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
                progressReport = deleteUpDataCollectRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes, networkElement.getNeType(),
                        networkElement.getOssModelIdentity());
            }
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
            if (progressReport == null) {
                LOGGER.warn("Discarding invalid notification received nodeName {} and expected action is removeUpgradePackage", nodeName);
                return;
            }
            if (EcimBackupConstants.BACKUP_DELETE_ACTION.equals(progressReport.getActionName()) || NotificationEventTypeEnum.DELETE.equals(notification.getNotificationEventType())) {
                evaluteBackupDeletion(activityJobId, progressReport, notificationSubject, notificationTime, notification);
            } else if (DeleteUpgradePackageConstants.REMOVE_UP_ACTION_NAME.equalsIgnoreCase(progressReport.getActionName())) {
                evaluteUpDeletion(activityJobId, progressReport, notificationSubject, notificationTime);
            } else if (EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName())) {
                ecimCommonUtils.handleCancelProgressReportState(jobLogs, activityJobId, progressReport, notificationTime, EcimBackupConstants.DELETE_BACKUP);
            }
        } catch (UnsupportedFragmentException e) {
            LOGGER.error("Unsupported fragment for the corresponding notification recieved", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
            jobLogs.clear();
        } catch (final Exception e) {
            String logMessage = null;
            LOGGER.error("An exception occurred in process notifications with activityJobId : {} nodeName : {} and can not be proceed further. Details are: ", activityJobId, nodeName, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format("An exception occurred for activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                logMessage = String.format("An exception occurred for activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            jobLogs.clear();
        }
    }

    private void evaluteBackupDeletion(final long activityJobId, final AsyncActionProgress progressReport, final NotificationSubject notificationSubject, final Date notificationTime, final Notification notification) throws MoNotFoundException {

        final String brmBackupManagerMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        String deletedBkp = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.CURRENT_BKPNAME);

        final String upsWithSyscrBksData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.UPS_WITH_SYSCR_BACKUPS);
        Map<String, Set<String>> upsWithSyscrBkps = deleteUpJobDataCollector.converToMap(upsWithSyscrBksData);
        // On identifying upsWithSYSCR Backups map, it will be known that AP MO's rbsConfigLevel is corrected so that a SYSCR backup gets deleted by Node.
        if (deletedBkp.isEmpty() && !upsWithSyscrBkps.isEmpty()) {
            if(NotificationEventTypeEnum.DELETE.equals(notification.getNotificationEventType())){
                final String brmBackupMoFdn = notification.getNotificationSubject().getKey();
                deletedBkp = deleteUpJobDataCollector.getDeletedSysCreatedBackupInfo(upsWithSyscrBkps, brmBackupMoFdn);
            } else {
                deletedBkp = deleteUpJobDataCollector.checkForDeletedSysCreatedBackupInfo(upsWithSyscrBkps, nodeName);    
            }
            deleteUpJobDataCollector.unSubscribeToBrmBackupMosNotifications(upsWithSyscrBkps, activityJobId);
            if(!deletedBkp.isEmpty()) {
                String[] backupData = deletedBkp.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                systemRecorder.recordCommand(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, EcimCommonConstants.AutoProvisioningMoConstants.AP_MO,
                        String.format(JobLogConstants.RESET_AP_MO_RBS_CONFIG_VALUE_DELETE_BACKUP, backupData[0]));
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.RESET_AP_MO_RBS_CONFIG_VALUE_DELETE_BACKUP, backupData[0]), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            } else {
                final Map<String, Object> processVariables = new HashMap<>();
                systemRecorder.recordCommand(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE, CommandPhase.FINISHED_WITH_ERROR, nodeName, EcimCommonConstants.AutoProvisioningMoConstants.AP_MO,
                        String.format(JobLogConstants.NO_BACKUP_DELETED_ON_AP_MO_CHANGE, ActivityConstants.DELETE_UP_DISPLAY_NAME));
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.NO_BACKUP_DELETED_ON_AP_MO_CHANGE, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, ShmConstants.FALSE.toLowerCase());
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SKIPPED.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
                jobLogs.clear();
                return;
            }
        }
        JobResult jobResult = null;
        if (NotificationEventTypeEnum.DELETE.equals(notification.getNotificationEventType())) {
            jobResult = JobResult.SUCCESS;
        } else {
            final String[] bkpData = deletedBkp.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogs, activityJobId, progressReport, notificationTime, EcimBackupConstants.DELETE_BACKUP, bkpData[0]);
        }

        if (jobResult != null) {
            systemRecorder.recordEvent(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_PROCESS_NOTIFICATION, EventLevel.COARSE, ActivityConstants.DELETE_UP_DISPLAY_NAME, nodeName, jobResult.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            jobLogs.clear();

            activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));

            final boolean isRepeat = deleteUpJobDataCollector.setBackupDataForNextItration(activityJobId, deletedBkp);

            final String isIntermediateFailureOnBackup = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.UP_INTERMEDIATE_FAILURE);
            final Map<String, Object> processVariables = new HashMap<String, Object>();
            if (isRepeat) {
                if (JobResult.FAILED.toString().equals(isIntermediateFailureOnBackup)) {
                    jobResult = JobResult.FAILED;
                    systemRecorder.recordEvent(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_PROCESS_NOTIFICATION, EventLevel.COARSE, ActivityConstants.DELETE_UP_DISPLAY_NAME, nodeName, jobResult.toString());
                } else if (activityUtils.cancelTriggered(activityJobId)) {
                    LOGGER.debug("Cancelled triggered in evaluteBackupDeletion repeat true");
                    jobResult = JobResult.FAILED;
                    processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, "false");
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
                } else {
                    processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, isRepeat);
                }
            }
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
        }
    }

    private void evaluteUpDeletion(final long activityJobId, final AsyncActionProgress progressReport, final NotificationSubject notificationSubject, final Date notificationTime) {
        final String upMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        final String persistedCurrentUpMoData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.CURRENT_UP_MO_DATA);
        JobResult jobResult = handleUPProgressReportState(activityJobId, progressReport, persistedCurrentUpMoData, notificationTime);
        final double totalActivityProgressPercentage = deleteUpJobDataCollector.calculateActivityProgressPercentage(jobEnvironment, progressReport.getProgressPercentage());
        if (jobResult != null) {
            activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
            //Handling remaining UPs
            final boolean isRepeat = deleteUpJobDataCollector.setUpAndBackupDataForNextItration(activityJobId, activityJobAttributes, persistedCurrentUpMoData);
            final String isIntermediateFailureOnUp = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.UP_INTERMEDIATE_FAILURE);
            final Map<String, Object> processVariables = new HashMap<String, Object>();
            if (isRepeat) {
                if (activityUtils.cancelTriggered(activityJobId)) {
                    LOGGER.debug("Cancelled triggered in evaluteUpDeletion repeat true");
                    jobResult = JobResult.FAILED;
                    processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, "false");
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
                } else {
                    processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, isRepeat);
                }
            } else {
                String logMessage = null;
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
                final String isAnyActiveUp = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.IS_ANY_ACTIVE_UP);
                if (isAnyActiveUp != null && Boolean.valueOf(isAnyActiveUp) || JobResult.FAILED.toString().equals(isIntermediateFailureOnUp)) {
                    jobResult = JobResult.FAILED;
                    logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                } else {
                    logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }
                LOGGER.info("Delete upgrade job final result {}", jobResult);
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, totalActivityProgressPercentage);
            jobLogs.clear();
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, totalActivityProgressPercentage);
            jobLogs.clear();
            progressPercentageCache.bufferNEJobs(jobEnvironment.getNeJobId());
        }
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        } catch (final MoNotFoundException moNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, moNotFoundException);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

    private JobResult handleUPProgressReportState(final long activityJobId, final AsyncActionProgress progressReport, final String upMoProductData, final Date notificationTime) {
        final ActionStateType state = progressReport.getState();
        final ActionResultType result = progressReport.getResult();
        JobResult jobResult = null;

        final String upProductData[] = upMoProductData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
        String jobLogMessage = "";
        switch (state) {
        case RUNNING:
            jobLogMessage = progressReport.toString() + String.format(JobLogConstants.UP_WITH_PNUM_PREV, upProductData[0], upProductData[1]);
            updateJobLog(activityJobId, jobLogMessage, notificationTime, JobLogLevel.INFO.toString());
            break;
        case FINISHED:
            jobLogMessage = progressReport.toString() + String.format(JobLogConstants.UP_WITH_PNUM_PREV, upProductData[0], upProductData[1]);
            updateJobLog(activityJobId, jobLogMessage, notificationTime, JobLogLevel.INFO.toString());
            if (ActionResultType.SUCCESS.equals(result)) {
                jobResult = JobResult.SUCCESS;
                jobLogMessage = String.format(JobLogConstants.UP_DELETED_SUCCESSFULLY, upProductData[0], upProductData[1]);
                LOGGER.debug(jobLogMessage);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, jobLogMessage, notificationTime, JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            } else if (ActionResultType.FAILURE.equals(result)) {
                jobResult = JobResult.FAILED;
                jobLogMessage = String.format(JobLogConstants.DELETEUP_ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME, upProductData[0], upProductData[1]) + progressReport.getResultInfo();
                LOGGER.debug(jobLogMessage);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, jobLogMessage, notificationTime, JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());

                activityUtils.prepareJobPropertyList(jobProperties, DeleteUpgradePackageConstants.UP_INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
            }
            recordEvent(activityJobId, "", jobLogMessage, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            jobLogs.clear();
            break;
        case CANCELLING:
            jobLogMessage = String.format(JobLogConstants.CANCEL_IN_PROGRESS, progressReport.getProgressPercentage(), progressReport.getProgressInfo());
            if (progressReport.getAdditionalInfo() != null && progressReport.getAdditionalInfo() != "") {
                jobLogMessage = jobLogMessage + " Additional Information = \"" + progressReport.getAdditionalInfo() + "\"";
            }
            updateJobLog(activityJobId, jobLogMessage, notificationTime, JobLogLevel.INFO.toString());
            break;
        case CANCELLED:
            jobResult = JobResult.FAILED;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME) + progressReport.getResultInfo();
            LOGGER.debug(jobLogMessage);
            updateJobLog(activityJobId, jobLogMessage, notificationTime, JobLogLevel.ERROR.toString());
            break;
        default:
            jobResult = JobResult.FAILED;
            LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
        return jobResult;
    }

    private void updateJobLog(final long activityJobId, final String logMessage, final Date notificationTime, final String logLevel) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, notificationTime, JobLogType.SYSTEM.toString(), logLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
        jobLogs.clear();
    }

    private void recordEvent(final long activityJobId, final String upgradePackageFdn, final String jobLogMessage, final String flow) {
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.DELETE_UPGRADEPACKAGE, ActivityConstants.DELETE_UP_DISPLAY_NAME);
        activityUtils.recordEvent(eventName, upgradePackageFdn, upgradePackageFdn, "SHM:" + activityJobId + ":" + jobLogMessage + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }
}
