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

package com.ericsson.oss.services.shm.es.impl.minilink.deletebackup;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.NO_BACKUP_FILE_SELECTED;

import java.util.ArrayList;
import java.util.Collections;
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
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupActivityProperties;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("MINI_LINK_INDOOR.DELETEBACKUP.deletebackup")
@ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.MINI_LINK_INDOOR)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteBackupService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupService.class);
    private static final String THIS_ACTIVITY = ActivityConstants.DELETE_BACKUP;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private MiniLinkJobUtil miniLinkJobUtil;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private DeleteSmrsBackupUtil deleteSmrsBackupService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    private static final String BACKUP_DATA_SPLIT_CHARACTER = "\\|";

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final BackupActivityProperties activityProperties = getActivityProperties(activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.DELETE_BACKUP);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final String nodeName = jobEnvironment.getNodeName();
            final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
            final List<String> inputBackupDataList = getBackupDataToBeDeleted(mainJobAttributes, nodeName);

            if (!inputBackupDataList.isEmpty()) {
                return miniLinkJobUtil.precheckSuccess(activityProperties);
            } else {
                return miniLinkJobUtil.precheckFailure(activityProperties, NO_BACKUP_FILE_SELECTED);
            }
        } catch (Exception exception) {
            LOGGER.error("Exception occured in precheck() for activityJobId:{}. Reason: ", activityJobId, exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            miniLinkJobUtil.precheckFailure(activityProperties, exception.getMessage());
        }
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Mo action execution starts for MINI-LINK delete backup activity with activityJobId: {}", activityJobId);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("Delete Backup for NodeName :{}", nodeName);
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        final List<String> inputBackupDataList = getBackupDataToBeDeleted(mainJobAttributes, nodeName);
        final String inputBackupData = getBackupNameToBeProcessed(inputBackupDataList, activityJobAttributes, activityJobId);
        deleteBackupFromENM(getBackupNameFromBackupData(inputBackupData), nodeName, activityJobId);
    }

    @SuppressWarnings("unchecked")
    private List<String> getBackupDataToBeDeleted(final Map<String, Object> mainJobAttributes, final String nodeName) {
        LOGGER.debug("Prepare Backup Data To Be Deleted for node name : {}", nodeName);
        List<String> backupDataList = new ArrayList<>();

        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        LOGGER.debug("job Configuration in getBackupDataToBeDeleted() {}", jobConfigurationDetails);

        final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Collections.singletonList(nodeName));
        final String neType = networkElementList.get(0).getNeType();
        final String platform = networkElementList.get(0).getPlatformType().name();
        LOGGER.debug("NeType {}, platform {} ", neType, platform);

        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(Collections.singletonList("BACKUP_NAME"), jobConfigurationDetails, nodeName, neType, platform);
        final String backupData = keyValueMap.get("BACKUP_NAME");

        LOGGER.debug("backupData from job property is : {} ", backupData);

        // Preparing the list of Backup names from comma separated string
        if (backupData != null && !backupData.isEmpty()) {
            backupDataList = prepareBackupDataList(backupData);
        } else {
            LOGGER.error("No Backups provided to delete: {}", nodeName);
        }

        return backupDataList;
    }

    /**
     * This method prepares the list of Backup data. Eg: The backup Data - "BkUp_1|domain|type, Bkup_2|domain|type, Bkup_3|domain|type" from UI will be converted to separate Strings and stored in a
     * List.
     * 
     * @param backupData
     * @return backupDataList
     */
    private List<String> prepareBackupDataList(final String backupData) {
        final List<String> backupDataList = new ArrayList<>();
        if (backupData.contains(",")) {
            final String[] backupDetails = backupData.split(",");
            Collections.addAll(backupDataList, backupDetails);
        } else {
            backupDataList.add(backupData);
        }
        return backupDataList;
    }

    private String getBackupNameFromBackupData(final String backupData) {
        return backupData.split(BACKUP_DATA_SPLIT_CHARACTER)[0];
    }

    private String getBackupNameToBeProcessed(final List<String> backupDataList, final Map<String, Object> activityJobAttributes, final long activityJobId) {
        final int processedBackups = getCountOfProcessedBackups(activityJobAttributes);
        final String[] listOfBackups = backupDataList.toArray(new String[0]);
        final String backupToBeProcessed = listOfBackups[processedBackups];
        LOGGER.debug("processedBackups : {} , listOfBackups : {} ,backupToBeProcessed : {}", processedBackups, listOfBackups, backupToBeProcessed);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, MiniLinkConstants.PROCESSED_BACKUPS, Integer.toString(processedBackups + 1));
        activityUtils.prepareJobPropertyList(jobPropertyList, MiniLinkConstants.CURRENT_BACKUP, backupToBeProcessed);
        activityUtils.prepareJobPropertyList(jobPropertyList, MiniLinkConstants.TOTAL_BACKUPS, Integer.toString(listOfBackups.length));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null);
        return backupToBeProcessed;
    }

    private int getCountOfProcessedBackups(final Map<String, Object> activityJobAttributes) {
        int processedBackups = 0;
        final String processedBackupsString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, MiniLinkConstants.PROCESSED_BACKUPS);
        if (processedBackupsString != null && !processedBackupsString.isEmpty()) {
            processedBackups = Integer.parseInt(processedBackupsString);
        }
        LOGGER.debug("Processed backups count = {}", processedBackups);
        return processedBackups;
    }

    private void deleteBackupFromENM(final String backupName, final String nodeName, final long activityJobId) {
        LOGGER.debug("Deleting backup from ENM with backupName : {} , nodeName : {} , and activityJobId: {}", backupName, nodeName, activityJobId);
        String neType = "";
        JobResult jobResult;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
        final String logMessage = String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, backupName);
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        if (backupName == null || backupName.isEmpty()) {
            return;
        }
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        try {
            neType = getNetworkElement(nodeName).getNeType();
        } catch (final MoNotFoundException e) {
            LOGGER.error("deleteBackupOnEnm has failed due to {}", e);
            activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        }
        if (executeForBkpsOnEnm(nodeName, backupName, neType)) {
            jobResult = JobResult.SUCCESS;
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Backup On Enm " + backupName + " has been deleted successfully.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } else {
            jobResult = JobResult.FAILED;
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Backup On Enm:" + backupName + " cannot be deleted .", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, jobEnvironment);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
    }

    private Map<String, Object> evaluateRepeatRequiredAndActivityResult(final JobResult moActionResult, final List<Map<String, Object>> jobPropertyList, final JobEnvironment jobEnvironment) {
        LOGGER.debug("Evaluate whether repeat is Required and activity result. moActionResult {}, jobPropertyList {}", moActionResult, jobPropertyList);
        boolean recentUploadFailed = false;
        boolean repeatExecute = true;
        JobResult activityJobResult = null;
        final long activityJobId = jobEnvironment.getActivityJobId();
        if (moActionResult == JobResult.FAILED) {
            recentUploadFailed = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, MiniLinkConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        }
        if (isAllBackupsProcessed(jobEnvironment)) {
            if (isAnyIntermediateFailureHappened(jobEnvironment) || recentUploadFailed) {
                activityJobResult = JobResult.FAILED;
            } else {
                activityJobResult = JobResult.SUCCESS;
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
            repeatExecute = false;
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            activityJobResult = JobResult.FAILED;
            repeatExecute = false;
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
        }
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, activityJobResult == JobResult.SUCCESS);
        LOGGER.debug("Is Repeat Required or ActivityResult evaluated : {}", repeatRequiredAndActivityResult);
        return repeatRequiredAndActivityResult;
    }

    private boolean isAllBackupsProcessed(final JobEnvironment jobEnvironment) {
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        final int processedBackups = getCountOfProcessedBackups(activityJobAttributes);
        final int totalBackups = getCountOfTotalBackups(activityJobAttributes);
        if (processedBackups == totalBackups) {
            return true;
        }
        return false;
    }

    private int getCountOfTotalBackups(final Map<String, Object> activityJobAttributes) {
        int totalBackups = 0;
        final String totalBackupsString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, MiniLinkConstants.TOTAL_BACKUPS);
        if (totalBackupsString != null && !totalBackupsString.isEmpty()) {
            totalBackups = Integer.parseInt(totalBackupsString);
        }
        LOGGER.debug("Count of total backups: {}", totalBackups);
        return totalBackups;
    }

    private boolean isAnyIntermediateFailureHappened(final JobEnvironment jobEnvironment) {
        final String intermediateFailure = activityUtils.getActivityJobAttributeValue(jobEnvironment.getActivityJobAttributes(), MiniLinkConstants.INTERMEDIATE_FAILURE);
        if (intermediateFailure != null && !intermediateFailure.isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean executeForBkpsOnEnm(final String nodeName, final String backupToBeDeletedOnSmrs, final String neType) {
        boolean result = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        LOGGER.debug("Deleting Backup on Smrs");
        final String logMessage = String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, backupToBeDeletedOnSmrs);
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        if (deleteSmrsBackupService.deleteBackupOnSmrs(nodeName, backupToBeDeletedOnSmrs, neType)) {
            result = true;
        }
        return result;
    }

    private NetworkElement getNetworkElement(final String nodeName) throws MoNotFoundException {
        final List<NetworkElement> networkElements = fdnServiceBean.getNetworkElementsByNeNames(Collections.singletonList(nodeName));
        if (networkElements.isEmpty()) {
            throw new MoNotFoundException("Network element is not found for the node name=" + nodeName);
        }
        return networkElements.get(0);
    }

    @Override
    public void processNotification(final Notification message) {
        // Notification is not expected.
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, THIS_ACTIVITY);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        JobResult jobResult = null;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, THIS_ACTIVITY), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        jobLogList.clear();
        final String nodeName = jobEnvironment.getNodeName();
        // Retrieve the backup that is currently being processed for deletion. It will be of the form BackupName|Domain|Type.
        final String inputBackupData = getBackup(jobEnvironment);
        jobResult = handleTimeoutFordeleteBackupOnENM(getBackupNameFromBackupData(inputBackupData), nodeName, activityJobId, jobLogList);
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, jobEnvironment);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        if (repeatRequired) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        } else if (isActivitySuccess) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    private String getBackup(final JobEnvironment jobEnvironment) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobEnvironment.getActivityJobAttributes().get(ShmConstants.JOBPROPERTIES);
        String backup = "";
        if (activityJobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
                if (MiniLinkConstants.CURRENT_BACKUP.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    backup = eachJobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        LOGGER.debug("Current backup = {}", backup);
        return backup;
    }

    private JobResult handleTimeoutFordeleteBackupOnENM(final String backupName, final String nodeName, final long activityJobId, final List<Map<String, Object>> jobLogList) {
        LOGGER.debug("handleTimeoutFordeleteBackupOnENM with backupName : {} , nodeName : {} , and activityJobId: {}", backupName, nodeName, activityJobId);
        String neType = "";
        JobResult jobResult;
        try {
            final NetworkElement networkElement = deleteSmrsBackupService.getNetworkElement(nodeName);
            neType = networkElement.getNeType();
        } catch (final MoNotFoundException e) {
            LOGGER.error("deleteBackupOnEnm has failed due to {}", e);
            activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        }
        if (isBackupDeletionCompletedOnSmrs(nodeName, backupName, neType)) {
            jobResult = JobResult.SUCCESS;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, backupName, Long.toString(activityJobId));
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Backup On Enm " + backupName + " has been deleted successfully after timeout.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } else {
            jobResult = JobResult.FAILED;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, backupName, Long.toString(activityJobId));
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Deletion of Backup " + backupName + " failed on ENM", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return jobResult;
    }

    private boolean isBackupDeletionCompletedOnSmrs(final String nodeName, final String backupToBeDeletedOnSmrs, final String neType) {
        boolean result = true;
        LOGGER.debug("Verying Backup Exists on Smrs");
        if (deleteSmrsBackupService.isBackupExistsOnSmrs(nodeName, backupToBeDeletedOnSmrs, neType)) {
            result = false;
        }
        return result;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        JobResult jobResult;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, THIS_ACTIVITY);
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        final List<String> inputBackupDataList = getBackupDataToBeDeleted(mainJobAttributes, nodeName);
        final String inputBackupData = getBackupNameToBeProcessed(inputBackupDataList, activityJobAttributes, activityJobId);
        final String backupName = getBackupNameFromBackupData(inputBackupData);

        jobResult = handleCancelTimeoutOnENM(backupName, nodeName, jobEnvironment, activityJobId, jobLogList);

        if (jobResult != null) {
            final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, jobEnvironment);
            final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
            if (isActivitySuccess) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            } else if (jobResult == JobResult.FAILED) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        } else {
            if (finalizeResult) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, ActivityConstants.CANCEL_DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
            }
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    private JobResult handleCancelTimeoutOnENM(final String backupName, final String nodeName, final JobEnvironment jobEnvironment, final long activityJobId, final List<Map<String, Object>> jobLogList) {
        LOGGER.debug("handleTimeoutFordeleteBackupOnENM with backupName : {} , nodeName : {} , and activityJobId: {}", backupName, nodeName, activityJobId);
        JobResult jobResult;
        if (isAllBackupsProcessed(jobEnvironment)) {
            jobResult = JobResult.SUCCESS;
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Backup On Enm " + backupName + " has been deleted successfully after timeout.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } else {
            jobResult = JobResult.FAILED;
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Deletion of Backup " + backupName + " failed on ENM", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return jobResult;
    }

    private BackupActivityProperties getActivityProperties(final long activityJobId) {
        return miniLinkJobUtil.getBackupActivityProperties(activityJobId, THIS_ACTIVITY, DeleteBackupService.class);
    }
}
