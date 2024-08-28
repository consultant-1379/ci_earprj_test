/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.backuphousekeeping;

import java.util.*;

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.backuphousekeeping.BackupHouseKeepingCriteria;
import com.ericsson.oss.services.shm.es.backuphousekeeping.NodeBackupHousekeepingConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.*;
import com.ericsson.oss.services.shm.es.impl.*;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.*;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.*;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * @author tcssmal
 */
@EServiceQualifier("ECIM.BACKUP_HOUSEKEEPING.deletebackup")
@ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.BACKUP_HOUSEKEEPING, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@SuppressWarnings("PMD")
public class CleanBackupService implements Activity, ActivityCallback, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanBackupService.class);
    private static final String BACKUP_DELIMITER = "|";

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private CancelBackupService cancelBackupService;

    @Inject
    private DeleteBackupUtility deleteBackupUtility;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private FaBuildingBlockResponseProvider buildingBlockResponseProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    /**
     * This method performs the PreCheck for Housekeeping of Backups on Node
     *
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        JobStaticData jobStaticData = null;
        long activityStartTime = 0L;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityStartTime = neJobStaticData.getActivityStartTime();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.DELETE_BACKUP);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            LOGGER.debug("Performing precheck for Backup House-keeping on the node : {}", nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, EcimBackupConstants.DELETE_BACKUP, nodeName), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final List<BrmBackup> totalBrmBackups = brmMoServiceRetryProxy.getBrmBackups(nodeName);
            final int totalNumberOfBackupsOnNode = totalBrmBackups.size();
            final BackupHouseKeepingCriteria backupHouseKeepingCriteria = getHousekeepingCriteria(mainJobAttributes, nodeName);
            final int maxBackupsToKeepOnNode = backupHouseKeepingCriteria.getMaxbackupsToKeepOnNode();
            if (totalNumberOfBackupsOnNode > maxBackupsToKeepOnNode) {
                prepareForDeletion(jobPropertyList, totalBrmBackups, maxBackupsToKeepOnNode);
                updateActivityTimeout(activityJobId, neJobStaticData, totalBrmBackups, maxBackupsToKeepOnNode);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, EcimBackupConstants.DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
            } else {
                final String logMessage = String.format(JobLogConstants.BACKUPS_AVAILABLE_LESS_THAN_OR_EQUAL_TO_BACKUPS_TO_KEEP_ON_NODE, totalNumberOfBackupsOnNode, maxBackupsToKeepOnNode);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_SKIP, EcimBackupConstants.DELETE_BACKUP, logMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()));
            return activityStepResult;
        } catch (final Exception ex) {
            final String logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DELETE_BACKUP, ex.getMessage());
            LOGGER.error(logMessage + " for nodeName : {} with Exception : {} ", nodeName, ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        }
        final boolean isPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (!isPersisted) {
            LOGGER.error("Precheck failed as required job properties are not persisted for activity : {} nodeName : {}", EcimBackupConstants.DELETE_BACKUP, nodeName);
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped or failed.");
        }
        return activityStepResult;
    }

    private void updateActivityTimeout(final long activityJobId, final NEJobStaticData neJobStaticData, final List<BrmBackup> brmbackupsOnNode, final int maxBackupsToKeepOnNode) {
        try {
            final int totalNumberOfBackupsOnNode = brmbackupsOnNode.size();
            final int numberOfBackupsToDelete = totalNumberOfBackupsOnNode - maxBackupsToKeepOnNode;
            int updatedActivityTimeout = 0;
            if (numberOfBackupsToDelete > 0) {
                final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(neJobStaticData.getNodeName());
                final String neType = networkElement.getNeType();
                final Integer timeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, neJobStaticData.getPlatformType(), JobTypeEnum.BACKUP_HOUSEKEEPING.name(),
                        ActivityConstants.DELETE_BACKUP);
                updatedActivityTimeout = timeout * numberOfBackupsToDelete;
            } else {
                LOGGER.warn("No backups to be deleted for the activity: {}. So, no valid response to send for FA", activityJobId);
            }
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
            buildingBlockResponseProvider.sendUpdatedActivityTimeout(activityJobId, neJobStaticData, activityJobAttributes, ActivityConstants.DELETE_BACKUP, updatedActivityTimeout);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while updating activityTimeout to FA for Activity: {} with activityJobId: {} due to {}:", ActivityConstants.DELETE_BACKUP, activityJobId, ex);
        }
    }

    private void prepareForDeletion(final List<Map<String, Object>> jobPropertyList, final List<BrmBackup> brmbackupsOnNode, final int maxBackupsToKeepOnNode) {
        ecimBackupUtils.sortBrmBackUpsByCreationTime(brmbackupsOnNode, true);
        final int totalNumberOfBackupsOnNode = brmbackupsOnNode.size();
        final int numberOfBackupsToDelete = totalNumberOfBackupsOnNode - maxBackupsToKeepOnNode;
        final List<BrmBackup> brmBackupsToBeDeleted = new ArrayList<BrmBackup>();
        for (int i = 0; i < numberOfBackupsToDelete; i++) {
            brmBackupsToBeDeleted.add(brmbackupsOnNode.get(i));
        }
        final String backupsToBeDeleted = convertBackupDataToJobProperty(brmBackupsToBeDeleted);
        LOGGER.debug("backupsToBeDeleted from node : {}", backupsToBeDeleted);
        activityUtils.prepareJobPropertyList(jobPropertyList, NodeBackupHousekeepingConstants.BACKUPS_TO_BE_PROCESSED_FOR_DELETION, backupsToBeDeleted);
    }

    /**
     * convertBackupDataToJobProperty method returns backups in the format of BackupName1|Domain1|Type1,BackupName2|Domain2|Type2
     *
     * @param brmBackups
     * @return
     */
    private String convertBackupDataToJobProperty(final List<BrmBackup> brmBackups) {
        final StringBuilder backupsToBeDelete = new StringBuilder();
        for (final BrmBackup brmBackup : brmBackups) {
            final BrmBackupManager brmBackupManager = brmBackup.getBrmBackupManager();
            final String backupName = brmBackup.getBackupName();
            final String backupDomain = brmBackupManager.getBackupDomain();
            final String backupType = brmBackupManager.getBackupType();
            if (isFirstBackupInTheList(backupsToBeDelete)) {
                backupsToBeDelete.append(NodeBackupHousekeepingConstants.COMMA_SEPARATOR);
            }
            backupsToBeDelete.append(backupName);
            backupsToBeDelete.append(BACKUP_DELIMITER);
            backupsToBeDelete.append(backupDomain);
            backupsToBeDelete.append(BACKUP_DELIMITER);
            backupsToBeDelete.append(backupType);
        }
        return backupsToBeDelete.toString();
    }

    private boolean isFirstBackupInTheList(final StringBuilder backupsToBeDelete) {
        return backupsToBeDelete.length() != 0;
    }

    /**
     * @param mainJobAttr
     * @param nodeName
     * @return
     */
    private BackupHouseKeepingCriteria getHousekeepingCriteria(final Map<String, Object> mainJobAttr, final String nodeName) {
        final List<String> jobPropertyKeyList = new ArrayList<String>();
        jobPropertyKeyList.add(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);
        final Map<String, String> jobPropertyKeyValueMap = jobPropertyUtils.getPropertyValue(jobPropertyKeyList, mainJobAttr, nodeName, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
        LOGGER.debug("keyValueMap in getHousekeepingCriteria {}", jobPropertyKeyValueMap);
        return new BackupHouseKeepingCriteria(jobPropertyKeyValueMap);
    }

    /**
     * This method performs HouseKeeping of backups on node.
     *
     * @param activityJobId
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
            nodeName = neJobStaticData.getNodeName();
            final String backupsToBeDeletedFromNode = activityUtils.getActivityJobAttributeValue(activityJobAttributes, NodeBackupHousekeepingConstants.BACKUPS_TO_BE_PROCESSED_FOR_DELETION);
            final List<String> backupDataList = ecimBackupUtils.prepareBackupDataList(backupsToBeDeletedFromNode);
            final String currentBackupToBeDelete = deleteBackupUtility.getBackupNameToBeProcessed(backupDataList, activityJobAttributes, activityJobId);
            final double activityProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.MO_ACTIVITY_END_PROGRESS, Double.toString(activityProgressPercentage));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
            if (currentBackupToBeDelete != null) {
                LOGGER.debug("currentBackupToBeDelete to be deleted from node : {} for activityJobId {} is {}.", nodeName, activityJobId, currentBackupToBeDelete);
                deleteBackupUtility.deleteBackupFromNode(activityJobId, neJobStaticData, currentBackupToBeDelete, JobTypeEnum.BACKUP_HOUSEKEEPING, getActivityInfo(activityJobId));
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action. Reason : ", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, EcimBackupConstants.DELETE_BACKUP);
            return;
        } catch (final Exception ex) {
            final String logMessage = "Exception occured while deleting the backup." + String.format(JobLogConstants.FAILURE_REASON, ex.getMessage());
            LOGGER.error("Exception occurred in execute of Delete backup on node {} due to {}:", nodeName, ex);
            deleteBackupUtility.handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, logMessage);
        }
    }

    /**
     * @param activityJobId
     * @return
     */
    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, CleanBackupService.class);
    }

    /**
     * Processes the AVC Notification sent when there has been an update to the attribute progressReport on the node. If the job is finished and is successful we de-register from notifications and
     * save the state of the job as SUCCESS. If the job is finished and is failed we de-register from notifications and save the state of the job as FAILED.
     *
     * @param notification avc notification
     * @return void
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM - backup housekeeping - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("ECIM - backup housekeeping - Discarding non-AVC notification.");
            return;
        }
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        String currentBackupUnderDeletion = null;
        long activityJobId = -1;
        NEJobStaticData neJobStaticData = null;
        try {
            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            activityJobId = activityUtils.getActivityJobId(notificationSubject);
            final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("modifiedAttributes in processNotification for activity {} : {}", activityJobId, modifiedAttributes);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            currentBackupUnderDeletion = deleteBackupUtility.getBackupUnderDeletion(activityJobId);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(currentBackupUnderDeletion);
            deleteBackupUtility.handleNotification(ecimBackupInfo, notificationSubject, modifiedAttributes, neJobStaticData, JobTypeEnum.BACKUP_HOUSEKEEPING, getActivityInfo(activityJobId));
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to process notification for backup house keeping. Reason : ", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.failActivity(activityJobId, jobLogList, null, EcimBackupConstants.DELETE_BACKUP);
        } catch (final Exception exception) {
            LOGGER.error("Exception occured during processing of notifications for delete backup action on backup {} on node {} with exception : ", currentBackupUnderDeletion, nodeName, exception);
            final String jobLogMessage = JobLogConstants.ACTION_FAILED + JobLogConstants.FAILURE_DUE_TO_EXCEPTION + exception.getMessage();
            deleteBackupUtility.handleFailureOrException(activityJobId, neJobStaticData, jobLogList, jobPropertyList, jobLogMessage);
        }
    }

    /**
     * This method handles timeout scenario for Housekeeping of backups on node.
     * <p>
     * This method has been Deprecated, use {@link CleanBackupService.asyncHandleTimeout(long) method instead.
     *
     * @param activityJobId
     * @return activityStepResult
     */
    @Deprecated
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Inside ECIM CleanBackupService.handleTimeout with activityJobId : {}", activityJobId);
        NEJobStaticData neJobStaticData = null;
        try
        {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
        }
        catch (Exception e){
            LOGGER.error("Unable to get neJobStaticData for backup house keeping. Reason : ", e);
        }
        return processTimeOut(activityJobId, neJobStaticData);
    }

    private ActivityStepResult processTimeOut(final long activityJobId, NEJobStaticData neJobStaticData) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        long activityStartTime = 0;
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        JobResult jobResult = null;
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, EcimBackupConstants.DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        jobLogList.clear();
        try {
            if (neJobStaticData == null) {
                neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            }
            activityStartTime = neJobStaticData.getActivityStartTime();
            final String currentBackupUnderDeletion = deleteBackupUtility.getBackupUnderDeletion(activityJobId);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(currentBackupUnderDeletion);
            jobResult = deleteBackupUtility.evaluateJobResult(ecimBackupInfo, neJobStaticData, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, getActivityInfo(activityJobId));
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout. Reason : ", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            return activityStepResult;
        } catch (final Exception ex) {
            jobResult = JobResult.FAILED;
            LOGGER.error("An exception occurred while processing ECIM CleanBackupService timeout with activityJobId : {}. Failure reason : ", activityJobId, ex);
            final String failureMessage = String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.DELETE_BACKUP, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, failureMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        }
        final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (repeatRequired) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        } else if (isActivitySuccess) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        }
        if (!repeatRequired) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return activityStepResult;
    }

    /**
     * This method cancels the ongoing deletebackup action.
     *
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final String currentBackupUnderDeletion = deleteBackupUtility.getBackupUnderDeletion(activityJobId);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(currentBackupUnderDeletion);
            return cancelBackupService.cancel(activityJobId, EcimBackupConstants.DELETE_BACKUP, ecimBackupInfo);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to cancel backup house keeping job. Reason : {}", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(null, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, null, null, EcimBackupConstants.DELETE_BACKUP);
            return new ActivityStepResult();
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            LOGGER.error("Exception occured while fetching neType of node : {}. Reason : ", nodeName, e);
        } catch (final Exception e) {
            LOGGER.error("Exception occured while cancelling the delete backup action on node : {}. Reason : ", nodeName, e);
        }
        return new ActivityStepResult();
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        JobResult jobResult = null;
        final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, EcimBackupConstants.DELETE_BACKUP);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
        jobLogList.clear();
        NEJobStaticData neJobStaticData = null;
        try {
            // Retrieve the backup that is currently being processed for deletion. It will be of the form BackupName|Domain|Type.
            final String currentBackupUnderDeletion = deleteBackupUtility.getBackupUnderDeletion(activityJobId);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(currentBackupUnderDeletion);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            jobResult = deleteBackupUtility.evaluateJobResult(ecimBackupInfo, neJobStaticData, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, getActivityInfo(activityJobId));
        } catch (final JobDataNotFoundException jdnfEx) {
            jobResult = JobResult.FAILED;
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            LOGGER.error("Unable to complete cancel timeout. Reason : {}", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(null, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, null, null, EcimBackupConstants.DELETE_BACKUP);
            return activityStepResult;
        } catch (final Exception exception) {
            jobResult = JobResult.FAILED;
            LOGGER.error("Unable to find the result in cancelTimeout, due to :: {}", exception);
            final String failureMessage = String.format(JobLogConstants.ECIM_FAILURE_REASON, EcimBackupConstants.DELETE_BACKUP, exception.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, failureMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        if (jobResult != null) {
            final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
            final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
            if (isActivitySuccess || jobResult == JobResult.SKIPPED) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            } else if (jobResult == JobResult.FAILED) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            }
        } else {
            if (finalizeResult) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, EcimBackupConstants.BACKUP_CANCEL_ACTION), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
            }
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        // Not needed if there is no node read calls in precheck or async threads are still not increased.
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // Implement this when (if) precheck is made async.
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside ECIM CleanBackupService.asyncHandleTimeout with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        long neJobId = -1;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobId = neJobStaticData.getNeJobId();
            activityStepResultEnum = processTimeOut(activityJobId, neJobStaticData).getActivityResultEnum();
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout. Reason : ", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } catch (final Exception ex) {
            LOGGER.error("An exception has occured in async asyncHandleTimeout activity for node {} with activityJobId : {}. Exception is : ", nodeName, activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.DELETE_BACKUP, exceptionMessage);
        }
        LOGGER.info("Sending back ActivityStepResult to WorkFlow from ECIM CleanBackupService.asyncHandleTimeout with result : {} for node {} with activityJobId {} and neJobId {}",
                activityStepResultEnum, nodeName, activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.DELETE_BACKUP, activityStepResultEnum);
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Failing the Backup House-keeping activity and sending back to WorkFlow from ECIM CleanBackupService.timeoutForAsyncHandleTimeout for activityJobId : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, EcimBackupConstants.DELETE_BACKUP);
    }

}
