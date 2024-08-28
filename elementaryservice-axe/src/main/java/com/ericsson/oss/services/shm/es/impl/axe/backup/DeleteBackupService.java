/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*;

import java.io.UnsupportedEncodingException;
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

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants;
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * 
 * @author xrajeke
 *
 */
@EServiceQualifier("AXE.DELETEBACKUP.deletebackup")
@ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.AXE)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteBackupService extends AbstractAxeBackupService implements Activity, AsynchronousActivity {

    private static final String UNABLE_TO_DELETE_BACKUP_FROM_ENM = "Unable to delete Backup from ENM";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupService.class);
    private static final ActivityInfo activityAnnotation = DeleteBackupService.class.getAnnotation(ActivityInfo.class);

    private static final String INTERNAL_CURRENT_BACKUP_NAME = "currentBackupName";
    private static final String INTERNAL_INPUT_BACKUPS = "inputBackups";
    private static final String INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT = "previouslyProcessedBackupsCount";
    private static final String INTERNAL_ERROR_MESSAGE = "errorMessage";
    private static final String INTERNAL_ACTIVITY_JOBID = "activityJobId";
    private static final String INTERNAL_CURRENT_BKP_PROP = "currentBkpProp";
    /**
     * axe-node-backup-delete/{neId}/{file}/{component}
     * 
     */
    private static final String WINFIOL_BACKUP_DELETE_REQUEST_URI = "axe-node-backup-delete/%s/%s/%s";

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private DeleteSmrsBackupUtil deleteSmrsBackupService;

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        String jobBussinessKey = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);

            //Make sure the initiation happens only for the 1st backup iteration
            if (!Boolean.valueOf(activityUtils.getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.IS_ACTIVITY_TRIGGERED))) {
                final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityAnnotation.activityName());
                if (!isUserAuthorized) {
                    activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, activityAnnotation.activityName());
                    return;
                }
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, DELETE_BKP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
            }
            final int previouslyProcessedBackupsCount = getProcessedBackupsCount(activityJobAttributes);
            final String inputBackups = getInputBackupNames(neJobStaticData, neType);
            final String currentBackupProperty = getCurrentBackup(inputBackups, previouslyProcessedBackupsCount);
            final String[] currentItem = currentBackupProperty.split(ShmConstants.BACKUP_LOCATION_SPLIT_DELIMITER);
            final String currentBackupName = currentItem[0];

            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.STARTED, neJobStaticData.getParentNodeName(), currentBackupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), activityAnnotation.jobType()));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.EXECUTION_STARTED, DELETE_BKP_DISPLAY_NAME, currentBackupName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());

            final Map<String, Object> internalMethodArguments = generateInternalMethodArguments(activityJobId, currentBackupName, currentBackupProperty, inputBackups, previouslyProcessedBackupsCount);

            if (SHMJobConstants.ENM_LOCATION.equalsIgnoreCase(currentItem[1])) {
                deleteFromENM(jobLogList, neJobStaticData, neType, activityJobAttributes, internalMethodArguments);
            } else if (SHMJobConstants.NODE_LOCATION.equalsIgnoreCase(currentItem[1])) {
                deleteFromNode(jobLogList, neJobStaticData, activityJobAttributes, internalMethodArguments);
            } else {
                LOGGER.error("Unable to find the target location to delete the backup:{} from node:{}", currentBackupName, neJobStaticData.getNodeName());
            }

        } catch (JobDataNotFoundException e) {
            LOGGER.error("Job Information not retrieved. reason: {}", e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, activityAnnotation.activityName());
        } catch (Exception e) {
            LOGGER.error("Execution of delete backup failed. due to: ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, DELETE_BKP_DISPLAY_NAME, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, activityAnnotation.activityName());
        }
    }

    private static Map<String, Object> generateInternalMethodArguments(final long activityJobId, final String currentBackupName, final String currentBackupProperty, final String inputBackups,
            final int previouslyProcessedBackupsCount) {
        final Map<String, Object> internalMethodArguments = new HashMap<>();
        internalMethodArguments.put(INTERNAL_ACTIVITY_JOBID, activityJobId);
        internalMethodArguments.put(INTERNAL_CURRENT_BACKUP_NAME, currentBackupName);
        internalMethodArguments.put(INTERNAL_INPUT_BACKUPS, inputBackups);
        internalMethodArguments.put(INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT, previouslyProcessedBackupsCount);
        internalMethodArguments.put(INTERNAL_CURRENT_BKP_PROP, currentBackupProperty);
        return internalMethodArguments;
    }

    private void deleteFromENM(final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final String neType, final Map<String, Object> activityJobAttributes,
            final Map<String, Object> internalMethodArguments) {
        final String currentBackupName = (String) internalMethodArguments.get(INTERNAL_CURRENT_BACKUP_NAME);
        final String nodeComponentPath = FilenameUtils.concat(neJobStaticData.getParentNodeName(), getComponentType(neJobStaticData.getNodeName()));
        final boolean isBackupDeleted = deleteSmrsBackupService.deleteBackupOnSmrs(nodeComponentPath, currentBackupName, neType);
        internalMethodArguments.put(INTERNAL_ERROR_MESSAGE, UNABLE_TO_DELETE_BACKUP_FROM_ENM); //default error message in failure case
        evaluateExecuteResponse(jobLogList, neJobStaticData, activityJobAttributes, isBackupDeleted, internalMethodArguments);
    }

    private void deleteFromNode(final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final Map<String, Object> activityJobAttributes,
            final Map<String, Object> internalMethodArguments) throws UnsupportedEncodingException {
        final String currentBackupName = (String) internalMethodArguments.get(INTERNAL_CURRENT_BACKUP_NAME);
        LOGGER.info("Invoking WinFIOL end point to delete the backup: {} on Node:{}", currentBackupName, neJobStaticData.getNodeName());
        final SessionIdResponse deleteBackupResponse = triggerDeleteRequest(neJobStaticData, currentBackupName);
        LOGGER.debug("Backup Deletion Response from WinFIOL is: {}", deleteBackupResponse);
        internalMethodArguments.put(INTERNAL_ERROR_MESSAGE, deleteBackupResponse.getError());
        evaluateExecuteResponse(jobLogList, neJobStaticData, activityJobAttributes, isBackupDeletedOnNode(deleteBackupResponse), internalMethodArguments);
    }

    private SessionIdResponse triggerDeleteRequest(final NEJobStaticData neJobStaticData, final String currentBackupName) throws UnsupportedEncodingException {
        final Map<String, Object> headerMap = prepareHeadersInformation(neJobStaticData.getParentNodeName(),neJobStaticData.getNodeName(), new HashMap(), activityAnnotation.activityName());
        final String componentType = getComponentType(neJobStaticData.getNodeName());
        final Map<String, Object> connectivityInfoMap = dpsUtil.getConnectivityInformation(neJobStaticData.getParentNodeName(), componentType);
        final String uri = generateDeleteBackupURI(componentType, currentBackupName, connectivityInfoMap);

        return executeDeleteRequest(uri, AxeConstants.REST_HOST_NAME, headerMap, SessionIdResponse.class);
    }

    private String generateDeleteBackupURI(final String componentType, final String currentBackupName, final Map<String, Object> connectivityInfoMap) {
        final String nodeIpAdress = (String) connectivityInfoMap.get(AxeConstants.IP_ADDRESS);
        final String ap2clusterIpAddress = (String) connectivityInfoMap.get(AxeConstants.AP2_CLUSTER_IP_ADDRESS);
        String componentName;
        //This logic is support Winfiol Rest Endpoint based on each component
        if (componentType.contains("BC")) {
            componentName = AxeConstants.CLUSTER_BACKUP;
        } else if (componentType.contains(AxeConstants.AP_COMPONENT)) {
            componentName = AxeConstants.AP_COMPONENT;
        } else {
            componentName = componentType;
        }
        String backupNameOnNode = currentBackupName;
        if (!componentType.contains(AxeConstants.AP_COMPONENT)) {
            final String[] backupNames = currentBackupName.split(AxeConstants.BACKUPNAME_AND_EXTENSION_DELIMITER);
            backupNameOnNode = backupNames[0];
        }
        final String ipAddress = (componentType.equalsIgnoreCase(AxeConstants.APG2_COMPONENT)) ? ap2clusterIpAddress : nodeIpAdress;
        return String.format(WINFIOL_BACKUP_DELETE_REQUEST_URI, ipAddress, backupNameOnNode, componentName);
    }

    private void evaluateExecuteResponse(final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final Map<String, Object> activityJobAttributes,
            final boolean isBackupDeleted, final Map<String, Object> internalMethodArguments) {
        final long activityJobId = (long) internalMethodArguments.get(INTERNAL_ACTIVITY_JOBID);
        final String inputBackups = (String) internalMethodArguments.get(INTERNAL_INPUT_BACKUPS);
        final String currentBackupName = (String) internalMethodArguments.get(INTERNAL_CURRENT_BACKUP_NAME);

        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobProperties, CURRENT_BACKUP, currentBackupName);
        final int totalBackups = inputBackups.split(ActivityConstants.COMMA).length;
        activityUtils.prepareJobPropertyList(jobProperties, TOTAL_BACKUPS, Integer.toString(totalBackups));
        Double currentProgress = 0.0;
        JobResult jobResult = null;

        if (isBackupDeleted) {
            LOGGER.debug("Backup[{}] has been deleted from targetLocation.", currentBackupName);
            final String existingStepDurations = (String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS);
            if (existingStepDurations == null || !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            }
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), currentBackupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), activityAnnotation.jobType()));
            currentProgress = calculateCurrentProgress(activityJobAttributes, totalBackups);
            jobResult = JobResult.SUCCESS;

        } else {
            jobResult = JobResult.FAILED;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData.getParentNodeName(), currentBackupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), activityAnnotation.jobType()));
        }
        final Map<String, Object> processVariables = evaluateRepetition(jobResult, jobLogList, activityJobAttributes, jobProperties, internalMethodArguments);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, currentProgress);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), processVariables);
    }

    private Map<String, Object> evaluateRepetition(final JobResult jobResult, final List<Map<String, Object>> jobLogList, final Map<String, Object> activityJobAttributes,
            final List<Map<String, Object>> jobProperties, final Map<String, Object> internalMethodArguments) {
        final long activityJobId = (long) internalMethodArguments.get(INTERNAL_ACTIVITY_JOBID);
        final int previouslyProcessedBackupsCount = (int) internalMethodArguments.get(INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT);
        final String inputBackups = (String) internalMethodArguments.get(INTERNAL_INPUT_BACKUPS);
        final String currentBackupName = (String) internalMethodArguments.get(INTERNAL_CURRENT_BACKUP_NAME);
        final String currentBackupProp = (String) internalMethodArguments.get(INTERNAL_CURRENT_BKP_PROP);

        if (jobResult == JobResult.FAILED) {
            final String errorMessage = (String) internalMethodArguments.get(INTERNAL_ERROR_MESSAGE);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTIVITY_FAILED_FOR_BACKUP.concat(". ").concat(JobLogConstants.FAILURE_REASON), DELETE_BKP_DISPLAY_NAME, currentBackupName, errorMessage), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());

            final String failedBackupProperty = activityUtils.getActivityJobAttributeValue(activityJobAttributes, FAILED_BACKUPS);
            final String updatedFailedBackups = failedBackupProperty.isEmpty() ? currentBackupProp : failedBackupProperty.concat(",").concat(currentBackupProp);
            activityUtils.prepareJobPropertyList(jobProperties, FAILED_BACKUPS, updatedFailedBackups);
            activityUtils.prepareJobPropertyList(jobProperties, INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        } else if (jobResult == JobResult.SUCCESS) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FOR_BACKUP_COMPLETED_SUCCESSFULLY, DELETE_BKP_DISPLAY_NAME, currentBackupName), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        activityUtils.prepareJobPropertyList(jobProperties, PROCESSED_BACKUPS, Integer.toString(previouslyProcessedBackupsCount + 1));
        final boolean isAllBackupsProcessed = (previouslyProcessedBackupsCount + 1 == inputBackups.split(ActivityConstants.COMMA).length);
        final Map<String, Object> processVariables = new HashMap<>();
        if (isAllBackupsProcessed) {
            final JobResult finalResult = isAnyIntermediateFailureHappened(activityJobAttributes) ? JobResult.FAILED : jobResult;
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, finalResult.toString());
        } else if (activityUtils.cancelTriggered(activityJobId)) { //DPS call needed to get the latest property to avoid the check on stale data 
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.toString());
        } else {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        }

        return processVariables;
    }

    private boolean isAnyIntermediateFailureHappened(final Map<String, Object> activityJobAttributes) {
        final String intermFailure = activityUtils.getActivityJobAttributeValue(activityJobAttributes, INTERMEDIATE_FAILURE);
        return !intermFailure.isEmpty();
    }

    private Double calculateCurrentProgress(final Map<String, Object> activityJobAttributes, final int totalBackups) {
        final double previousProgress = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
        return (double) Math.round(previousProgress + ACTIVITY_END_PROGRESS_PERCENTAGE / totalBackups);
    }

    @SuppressWarnings("unchecked")
    private String getInputBackupNames(final NEJobStaticData neJobStaticData, final String neType) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId());

        String backupNames = activityUtils.getActivityJobAttributeValue(neJobAttributes, BACKUP_NAME);
        if (backupNames.isEmpty()) {
            final Map<String, String> backupNamesMap = jobPropertyUtils.getPropertyValue(Arrays.asList(BACKUP_NAME), jobConfigurationDetails, neJobStaticData.getNodeName(), neType,
                    neJobStaticData.getPlatformType());
            backupNames = backupNamesMap.get(BACKUP_NAME);
        }
        return backupNames;
    }

    private static String getCurrentBackup(final String backupNames, final int processedBackupsCount) {
        final String[] listOfBackups = backupNames.split(ActivityConstants.COMMA);
        return listOfBackups[processedBackupsCount];
    }

    private int getProcessedBackupsCount(final Map<String, Object> activityJobAttributes) {
        final String processedBackups = activityUtils.getActivityJobAttributeValue(activityJobAttributes, PROCESSED_BACKUPS);
        return (processedBackups != null && !processedBackups.isEmpty()) ? Integer.parseInt(processedBackups) : 0;
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        String jobBussinessKey = "";
        NEJobStaticData neJobStaticData = null;
        Map<String, Object> processVariables = new HashMap<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            LOGGER.debug("Evaluating time out step for businesskey={}, activityJobId={}", jobBussinessKey, activityJobId);

            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final int previouslyProcessedBackupsCount = getProcessedBackupsCount(activityJobAttributes);
            final String inputBackups = getInputBackupNames(neJobStaticData, neType);
            final String currentBackupProperty = activityUtils.getActivityJobAttributeValue(activityJobAttributes, CURRENT_BACKUP);
            final String[] currentItem = currentBackupProperty.split(ShmConstants.BACKUP_LOCATION_SPLIT_DELIMITER);
            final String currentBackupName = currentItem[0];
            final Map<String, Object> internalMethodArguments = generateInternalMethodArguments(activityJobId, currentBackupName, currentBackupProperty, inputBackups, previouslyProcessedBackupsCount);
            JobResult jobResult = JobResult.FAILED;

            if (SHMJobConstants.ENM_LOCATION.equalsIgnoreCase(currentItem[1])) {
                final String nodeComponentPath = FilenameUtils.concat(neJobStaticData.getParentNodeName(), getComponentType(neJobStaticData.getNodeName()));
                jobResult = deleteSmrsBackupService.isBackupExistsOnSmrs(nodeComponentPath, currentBackupName, neType) ? JobResult.SUCCESS : JobResult.FAILED;
                internalMethodArguments.put(INTERNAL_ERROR_MESSAGE, UNABLE_TO_DELETE_BACKUP_FROM_ENM); //default error message in failure case
            } else if (SHMJobConstants.NODE_LOCATION.equalsIgnoreCase(currentItem[1])) {
                final SessionIdResponse deleteBackupResponse = triggerDeleteRequest(neJobStaticData, currentBackupName);
                jobResult = isBackupDeletedOnNode(deleteBackupResponse) ? JobResult.SUCCESS : JobResult.FAILED;
                internalMethodArguments.put(INTERNAL_ERROR_MESSAGE, deleteBackupResponse.getError());
            } else {
                LOGGER.error("Unable to find the target location during timeout evaluation for the backup:{} on node:{}", currentBackupName, neJobStaticData.getNodeName());
            }
            final Double currentProgress = jobResult == JobResult.FAILED ? null : calculateCurrentProgress(activityJobAttributes, inputBackups.split(ActivityConstants.COMMA).length);
            processVariables = evaluateRepetition(jobResult, jobLogList, activityJobAttributes, jobProperties, internalMethodArguments);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, currentProgress);
            activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityAnnotation.activityName(), processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);

            logActivityCompletionFlow(activityJobId, neJobStaticData, currentBackupName, SHMEvents.DELETE_BACKUP_SERVICE, jobResult, ActivityConstants.COMPLETED_THROUGH_TIMEOUT);

        } catch (Exception e) {
            LOGGER.error("Delete Backup Job timeout step evaluation failed due to: ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, DELETE_BKP_DISPLAY_NAME, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, null);
            activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityAnnotation.activityName(), null, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);

        }
        if (neJobStaticData != null
                && (!processVariables.containsKey(JobVariables.ACTIVITY_REPEAT_EXECUTE) || !Boolean.parseBoolean(processVariables.get(JobVariables.ACTIVITY_REPEAT_EXECUTE).toString()))) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        }
    }

    private void logActivityCompletionFlow(final long activityJobId, final NEJobStaticData neJobStaticData, final String currentBackupName, final String event, final JobResult jobResult,
            final String flow) {
        final String eventName = activityUtils.getActivityCompletionEvent(activityAnnotation.platform(), activityAnnotation.jobType(), activityAnnotation.activityName());
        final CommandPhase commandPhase = jobResult == JobResult.SUCCESS ? CommandPhase.FINISHED_WITH_SUCCESS : CommandPhase.FINISHED_WITH_ERROR;
        systemRecorder.recordCommand(event, commandPhase, neJobStaticData.getNodeName(), currentBackupName,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), activityAnnotation.jobType()));
        activityUtils.recordEvent(eventName, neJobStaticData.getNodeName(), currentBackupName,
                "SHM:" + activityJobId + ":Delete Backup activity completed" + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }

    private boolean isBackupDeletedOnNode(final SessionIdResponse deleteBackupResponse) {
        return (deleteBackupResponse.getError() == null && deleteBackupResponse.getCode() == null)
                || (deleteBackupResponse.getError() != null && deleteBackupResponse.getError().contains("File was not found"));
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, DELETE_BKP_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        //        Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public void asyncPrecheck(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
    }

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        return activityStepResult;
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
        return null;
    }

}
