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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

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

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
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
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 *
 * @author estusin
 *
 */
@EServiceQualifier("ECIM.RESTORE.downloadbackup")
@ActivityInfo(activityName = "downloadbackup", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DownloadBackupService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadBackupService.class);

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private CancelBackupService cancelBackupService;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private RestoreJobHandlerRetryProxy restoreJobHandlerRetryProxy;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    /**
     * This method checks if BrmBackupManagerMo is present, then checks if corresponding BrmBackup is present.
     *
     * It logs all the success and failure message while deciding if action must be executed or not.
     *
     * @param activityJobId
     * @return
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        EcimBackupInfo ecimBackupInfo = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            final String nodeName = neJobStaticData.getNodeName();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.DOWNLOAD_BACKUP);
            if (!isUserAuthorized) {
                return activityStepResult;
            }
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            LOGGER.debug("For node :: [{}], Backup information :: File = [{}], Loc = [{}], Name = [{}], type = [{}]", nodeName, ecimBackupInfo.getBackupFileName(), ecimBackupInfo.getBackupLocation(),
                    ecimBackupInfo.getBackupName(), ecimBackupInfo.getBackupType());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, EcimBackupConstants.DOWNLOAD_BACKUP) + populateBackupName(ecimBackupInfo), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            if (isBackupLocatedOnNode(ecimBackupInfo.getBackupLocation())) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_SKIP, EcimBackupConstants.DOWNLOAD_BACKUP, "the selected backup is located on Node"),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
            } else {
                handleActivityResultForENMBackup(activityJobId, activityStepResult, ecimBackupInfo, jobLogList, nodeName);
            }
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred", ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            String errorMessage = "";
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = ex.getMessage();
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DOWNLOAD_BACKUP, errorMessage), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION
                || activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped.");
        } else {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResult;
    }

    private void handleActivityResultForENMBackup(final long activityJobId, final ActivityStepResult activityStepResult, final EcimBackupInfo ecimBackupInfo,
            final List<Map<String, Object>> jobLogList, final String nodeName) {
        if (!isValidBackup(ecimBackupInfo.getBackupName())) {
            final String errorMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DOWNLOAD_BACKUP, JobLogConstants.BACKUP_NOT_FOUND);
            LOGGER.error(errorMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, errorMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } else {
            updateActivityResultForValidBackup(activityJobId, activityStepResult, nodeName, ecimBackupInfo, jobLogList);
        }
    }

    private String populateBackupName(final EcimBackupInfo ecimBackupInfo) {
        return isBackupLocatedOnNode(ecimBackupInfo.getBackupLocation()) ? String.format(JobLogConstants.BACKUP_NAME, ecimBackupInfo.getBackupName())
                : String.format(JobLogConstants.BACKUP_FILE_NAME, ecimBackupInfo.getBackupFileName());
    }

    private boolean isBackupLocatedOnNode(final String backupLocation) {
        LOGGER.debug("Is the selected backup present on node? = [{}]", backupLocation.equals(EcimBackupConstants.LOCATION_NODE));
        return backupLocation.equals(EcimBackupConstants.LOCATION_NODE);
    }

    private void updateActivityResultForValidBackup(final long activityJobId, final ActivityStepResult activityStepResult, final String nodeName, final EcimBackupInfo ecimBackupInfo,
            final List<Map<String, Object>> jobLogList) {
        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.RESTORE_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        final String brmBackupManagerMOFdn = obtainBrmBackupManagerMOFdn(activityJobId, nodeName, ecimBackupInfo, jobLogList);
        if (brmBackupManagerMOFdn.length() > 0) {
            updateActivityResult(activityStepResult, jobLogList, nodeName, ecimBackupInfo);
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, EcimBackupConstants.DOWNLOAD_BACKUP, EcimBackupConstants.BACKUP_MANAGER_MO_TYPE), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
    }

    private String obtainBrmBackupManagerMOFdn(final long activityJobId, final String nodeName, final EcimBackupInfo ecimBackupInfo, final List<Map<String, Object>> jobLogList) {
        String brmBackupManagerMOFdn = "";
        try {
            final String inputVersion = brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName);
            final String logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion);
            if (logMessage != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
            brmBackupManagerMOFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo);
        } catch (final MoNotFoundException e) {
            LOGGER.error("BrmBackupManagerMo not found to proceed with import backup action" + e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("Unable to proceed import backup activity for activityJobId : ", activityJobId, "because :", unsupportedFragmentException);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return brmBackupManagerMOFdn;
    }

    private boolean isValidBackup(final String backupName) {
        return backupName.length() != 0;
    }

    private ActivityStepResult updateActivityResult(final ActivityStepResult activityStepResult, final List<Map<String, Object>> jobLogList, final String nodeName,
            final EcimBackupInfo ecimBackupInfo) {
        try {
            if (brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.PRE_CHECK_SUCCESS_SKIP_EXECUTION, EcimBackupConstants.DOWNLOAD_BACKUP, "Backup already exists on the node"), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DOWNLOAD_BACKUP, "Backup is already present on the node, but in improper state"), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (final MoNotFoundException e) {
            LOGGER.debug("MoNotFoundException occuried while updating activity result :: ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, EcimBackupConstants.DOWNLOAD_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } catch (final UnsupportedFragmentException e) {
            LOGGER.error("UnsupportedFragmentException occuried while updating activity result :: ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DOWNLOAD_BACKUP, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return activityStepResult;
    }

    @Asynchronous
    @Override
    public void execute(final long activityJobId) {
        LOGGER.debug("Mo action execution starts for create backup activity with activityJobId : {}", activityJobId);
        String nodeName = null;
        String brmBackupManagerMoFdn = null;
        boolean actionSuccessfullyTriggered = false;
        String businessKey = null;
        String neType = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.subscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
            brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupManagerMoFdn, EcimBackupConstants.DOWNLOAD_BACKUP);
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.name(), EcimBackupConstants.DOWNLOAD_BACKUP);

            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, EcimBackupConstants.DOWNLOAD_BACKUP, activityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.recordEvent(SHMEvents.IMPORT_BACKUP_EXECUTE, nodeName, brmBackupManagerMoFdn,
                    "SHM:" + activityJobId + ":" + nodeName + ": " + String.format(JobLogConstants.ACTION_TRIGGERED, EcimBackupConstants.DOWNLOAD_BACKUP));
            actionSuccessfullyTriggered = true;
        } catch (MoNotFoundException | UnsupportedFragmentException | ArgumentBuilderException e) {
            LOGGER.error("Action Invocation failed due to:: ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.DOWNLOAD_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            String jobLogMessage;
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.DOWNLOAD_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.DOWNLOAD_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, exception.getMessage());
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        if (!actionSuccessfullyTriggered) {
            activityUtils.recordEvent(SHMEvents.IMPORT_BACKUP_EXECUTE, nodeName, brmBackupManagerMoFdn,
                    "SHM:" + activityJobId + ":" + nodeName + ": " + String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.DOWNLOAD_BACKUP));
            activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.name());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.DOWNLOAD_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            activityUtils.sendActivateToWFS(businessKey, null);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification notification) {
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        LOGGER.info("Inside DownloadBackupService processNotification: with notificationSubject : {}", notificationSubject);
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        String nodeName = "";
        String brmBackupManagerMoFdn = null;
        NEJobStaticData neJobStaticData = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropList = new ArrayList<Map<String, Object>>();

        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            brmBackupManagerMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            //Process the Notification received on BrmBackupManager.
            if (NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
                final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
                LOGGER.info("modifiedAttributes in processNotification for download backup activity during restore {} : {}", activityJobId, modifiedAttributes);

                final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes);
                final boolean isInvalidProgressReport = brmMoServiceRetryProxy.validateActionProgressReport(nodeName, progressReport, EcimBackupConstants.DOWNLOAD_BACKUP);

                if (isInvalidProgressReport) {
                    LOGGER.warn("Discarding invalid notification,for the activityJobId {} and modifiedAttributes", activityJobId, modifiedAttributes);
                } else if (cancelBackupService.isCancelActionTriggerred(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId))) {
                    cancelBackupService.evaluateCancelProgress(progressReport, getActivityInfo(activityJobId), brmBackupManagerMoFdn, neJobStaticData, EcimBackupConstants.DOWNLOAD_BACKUP);
                } else {
                    handleProgressReportState(neJobStaticData, progressReport, brmBackupManagerMoFdn, jobLogList, jobPropList, activityJobId);
                }
            }
            //Process the Notification received on BrmBackup.
            else if (NotificationEventTypeEnum.CREATE.equals(notification.getNotificationEventType())) {
                LOGGER.info("Processing BrmBackup creation notificaton for activity job id : {}", activityJobId);
                String backupName_FromCreateNotification = null;

                final String brmBackupMOFdn_FromCreateNotification = notification.getDpsDataChangedEvent().getFdn();
                LOGGER.debug("brmBackupMOFdnFromCreateNotification : {}", brmBackupMOFdn_FromCreateNotification);

                backupName_FromCreateNotification = brmMoServiceRetryProxy.getBackupNameFromBrmBackupMOFdn(brmBackupMOFdn_FromCreateNotification);
                // Determine the received create notification is for the backup that is downloaded. i.e, compare backup name, domain and type.
                // Step-1 : Retrieve the BrmBackupManager MO Fdn for the specified domain & type.
                final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
                final String backupName_selectedForRestore = ecimBackupInfo.getBackupName();
                LOGGER.debug("ecimBackupInfo selected for restore : {}, {} and {}", backupName_selectedForRestore, ecimBackupInfo.getDomainName(), ecimBackupInfo.getBackupType());
                final String brmBackupMgrFdn_of_selectedBackup = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo);

                // Step-2 : Get BrmBackupManager FDN from BrmBackup create notification. (i.e, its parent FDN)
                final String brmBackupMgrFDN_From_BackupCreateNotification = activityUtils.getParentFdn(brmBackupMOFdn_FromCreateNotification);

                LOGGER.debug("backupmanager fdn : selected : {}, from notification : {}", brmBackupMgrFdn_of_selectedBackup, brmBackupMgrFDN_From_BackupCreateNotification);
                LOGGER.debug("backupName: selected : {}, from notification : {}", backupName_selectedForRestore, backupName_FromCreateNotification);

                // Step-3 : Compare backup manager FDNs and backup names.
                if (brmBackupMgrFdn_of_selectedBackup != null && brmBackupMgrFdn_of_selectedBackup.equals(brmBackupMgrFDN_From_BackupCreateNotification)) {
                    if (backupName_FromCreateNotification != null && backupName_FromCreateNotification.equals(backupName_selectedForRestore)) {
                        LOGGER.info("Expected backup create notification received for : {}", backupName_FromCreateNotification);
                        final boolean isActivityCompleted = restoreJobHandlerRetryProxy.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName,
                                EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED);
                        if (isActivityCompleted) {
                            finishActivity(activityJobId, brmBackupManagerMoFdn, neJobStaticData.getNeJobBusinessKey());
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Notification processing failed due to :: ", e);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Entered into handletimeout in Download activity with activityJobId : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        AsyncActionProgress progressReport = null;
        String nodeName = null;
        String brmBackupManagerMoFdn = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, EcimBackupConstants.DOWNLOAD_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        JobResult jobResult = JobResult.FAILED;
        String failureMessage = null;
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
            progressReport = brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(nodeName, ecimBackupInfo);
            if (cancelBackupService.isCancelActionTriggerred(jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId))) {
                return cancelBackupService.verifyCancelHandleTimeout(progressReport, activityJobId);
            }
            if (brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)) {
                handleTimeoutStatus = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
                activityStepResult.setActivityResultEnum(handleTimeoutStatus);
            }
            jobResult = evaluateJobResult(progressReport, jobLogList);
            handleTimeoutStatus = jobResult == JobResult.SUCCESS ? ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS : ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        } catch (MoNotFoundException | UnsupportedFragmentException | JobDataNotFoundException e) {
            LOGGER.error("Unable to find the result in handleTimeout, due to :: ", e);
            failureMessage = String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.DOWNLOAD_BACKUP, e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, failureMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            LOGGER.error("Unable to find the result in handleTimeout due to :: ", exception);
            failureMessage = String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.DOWNLOAD_BACKUP, exception.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, failureMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        activityUtils.prepareJobPropertyList(jobPropList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList);
        activityStepResult.setActivityResultEnum(handleTimeoutStatus);
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        String nodeName = "";
        EcimBackupInfo ecimBackupInfo = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
        } catch (final RetriableCommandException | IllegalArgumentException | MoNotFoundException | UnsupportedFragmentException | JobDataNotFoundException e) {
            LOGGER.error("Exception occured while cancel of DownlaodBackup of node : {} Reason {} ", nodeName, e);
        }
        return cancelBackupService.cancel(activityJobId, EcimBackupConstants.DOWNLOAD_BACKUP, ecimBackupInfo);
    }

    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, DownloadBackupService.class);
    }

    /**
     * Method to handle notifications for download backup activity on BrmBackupManager.
     *
     * @param neJobStaticData
     * @param progressReport
     * @param brmBackupManagerMoFdn
     * @param jobLogList
     * @param jobPropList
     */
    private void handleProgressReportState(final NEJobStaticData neJobStaticData, final AsyncActionProgress progressReport, final String brmBackupManagerMoFdn,
            final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropList, final long activityJobId) {
        final String nodeName = neJobStaticData.getNodeName();
        final ActionStateType state = progressReport.getState();
        if (state == ActionStateType.RUNNING) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList, (double) progressReport.getProgressPercentage());
            progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
        } else if (state == ActionStateType.FINISHED) {
            LOGGER.info("Received FINISHED notification for download activity !");
            final JobResult jobResult = evaluateJobResult(progressReport, jobLogList);
            final String businessKey = neJobStaticData.getNeJobBusinessKey();
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList);
            if (JobResult.SUCCESS.equals(jobResult)) {
                LOGGER.info("Download activity result is success...so check for BrmBackup creation before notifying workflow.");
                final boolean isActivityCompleted = restoreJobHandlerRetryProxy.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName,
                        EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL);
                if (isActivityCompleted) {
                    finishActivity(activityJobId, brmBackupManagerMoFdn, businessKey);
                }
            } else if (JobResult.FAILED.equals(jobResult)) {
                LOGGER.info("Download activity result is failure....so, notifying workflow without waiting for create backup notifications.");
                activityUtils.prepareJobPropertyList(jobPropList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList);
                activityUtils.sendActivateToWFS(businessKey, null);
            }
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
        } else {
            LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
    }

    /**
     * This method evaluates the activity job result based on the notification from the node on BrmBackupManager MO.
     *
     * @param progressReport
     * @param jobLogList
     * @param jobPropertyList
     * @return jobResult
     */
    private JobResult evaluateJobResult(final AsyncActionProgress progressReport, final List<Map<String, Object>> jobLogList) {
        JobResult jobResult = JobResult.FAILED;
        if (isImportSuccessful(progressReport)) {
            jobResult = JobResult.SUCCESS;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimBackupConstants.DOWNLOAD_BACKUP), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.DOWNLOAD_BACKUP, progressReport.getResultInfo()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.DOWNLOAD_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        }
        return jobResult;
    }

    private boolean isImportSuccessful(final AsyncActionProgress progressReport) {
        if (ActionResultType.SUCCESS == progressReport.getResult()) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        // TODO Auto-generated method stub
        return new ActivityStepResult();
    }

    private void finishActivity(final long activityJobId, final String brmBackupManagerMoFdn, final String businessKey) {
        LOGGER.info("found that both the steps in download activity are done...sending to wfs");

        //Unsubscribe from notifications
        activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));

        //Persist ActivityResult in ActivityJobProperty
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);

        //Notify WFS.
        if (isJobResultPersisted) {
            activityUtils.sendActivateToWFS(businessKey, null);
        }
    }
}
