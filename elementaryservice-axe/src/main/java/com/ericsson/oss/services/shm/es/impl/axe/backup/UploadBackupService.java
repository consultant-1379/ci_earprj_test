/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup;

import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.AsynchronousPollingActivity;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants;
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse;
import com.ericsson.oss.services.shm.es.axe.common.WinFIOLRequestStatus;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.instrumentation.impl.SmrsFileSizeInstrumentationService;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

@EServiceQualifier("AXE.BACKUP.uploadbackup")
@ActivityInfo(activityName = "uploadbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.AXE)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class UploadBackupService extends AbstractAxeBackupService implements Activity, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadBackupService.class);
    private static final ActivityInfo activityAnnotation = UploadBackupService.class.getAnnotation(ActivityInfo.class);
    private static final String INTERNAL_CURRENT_BACKUP_NAME = "currentBackupName";
    private static final String INTERNAL_INPUT_BACKUPS = "inputBackups";
    private static final String INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT = "previouslyProcessedBackupsCount";
    private static final String INTERNAL_ACTIVITY_JOBID = "activityJobId";

    /**
     * axe-node-backup-transfer/{ipAddress}/{backupFileName}
     */
    private static final String WINFIOL_BACKUP_TRANSFER_REQUEST_URI = "axe-node-backup-transfer/%s/%s";
    /**
     * axe-node-backup-transfer/{sessionId}
     */
    private static final String WINFIOL_BACKUP_TRANSFER_POLLING_URI = "axe-node-backup-transfer/%s";

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private SmrsFileSizeInstrumentationService smrsFileSizeInstrumentationService;

    @Inject
    private CancelBackupService cancelBackupService;

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        String jobBussinessKey = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();

            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityAnnotation.activityName());
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, activityAnnotation.activityName());
                return;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, UPLOAD_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final int previouslyProcessedBackupsCount = getProcessedBackupsCount(activityJobAttributes);
            final String inputBackups = getInputBackupNames(neJobStaticData, neType);
            final String currentBackupName = getCurrentBackup(inputBackups, previouslyProcessedBackupsCount);
            final String componentType = getComponentType(neJobStaticData.getNodeName());
            LOGGER.info("Invoking WinFIOL end point to upload the backup: {} on Node:{} and component {}", currentBackupName, neJobStaticData.getNodeName(), componentType);
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_EXECUTE, CommandPhase.STARTED, neJobStaticData.getParentNodeName(), currentBackupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));

            final String destinationPath = getDestinationDirectory(neType, neJobStaticData, componentType);
            final Map<String, Object> bodyMap = Collections.singletonMap(AxeConstants.DESTINATION_PATH, (Object) destinationPath);
            final Map<String, Object> headerMap = prepareHeadersInformation(neJobStaticData.getParentNodeName(),componentType, new HashMap(), activityAnnotation.activityName());
            final Map<String, Object> connectivityInfoMap = dpsUtil.getConnectivityInformation(neJobStaticData.getParentNodeName(), componentType);
            LOGGER.info("headerMap {} and connectivityInfoMap {}", headerMap, connectivityInfoMap);
            final String uri = getURI(componentType, currentBackupName, connectivityInfoMap);

            final SessionIdResponse sessionIdResponse = executePostRequest(uri, AxeConstants.REST_HOST_NAME, headerMap, bodyMap, SessionIdResponse.class);
            LOGGER.debug("Backup Upload Response from WinFIOL is: {}", sessionIdResponse);

            final Map<String, Object> internalMethodArguments = generateInternalMethodArguments(activityJobId, currentBackupName, inputBackups, previouslyProcessedBackupsCount);
            evaluateExecuteResponse(jobLogList, neJobStaticData, neType, activityJobAttributes, sessionIdResponse, internalMethodArguments);

        } catch (JobDataNotFoundException e) {
            LOGGER.error("Job Information not retrieved. reason: {}", e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, activityAnnotation.activityName());
        } catch (Exception e) {
            LOGGER.error("Exception Occured in execute() failure reason : ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, UPLOAD_DISPLAY_NAME, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, activityAnnotation.activityName());
        }
    }

    private String getURI(final String componentType, final String currentBackupName, final Map<String, Object> connectivityInfoMap) {
        final String nodeIpAdress = (String) connectivityInfoMap.get(AxeConstants.IP_ADDRESS);
        final String ap2clusterIpAddress = (String) connectivityInfoMap.get(AxeConstants.AP2_CLUSTER_IP_ADDRESS);
        String backupNameOnNode = currentBackupName;
        if (!componentType.contains(AxeConstants.AP_COMPONENT)) {
            final String[] backupNames = currentBackupName.split(AxeConstants.BACKUPNAME_AND_EXTENSION_DELIMITER);
            backupNameOnNode = backupNames[0];
        }
        final String ipAddress = (AxeConstants.APG2_COMPONENT.equalsIgnoreCase(componentType)) ? ap2clusterIpAddress : nodeIpAdress;
        return String.format(WINFIOL_BACKUP_TRANSFER_REQUEST_URI, ipAddress, backupNameOnNode);
    }

    private static Map<String, Object> generateInternalMethodArguments(final long activityJobId, final String currentBackupName, final String inputBackups, final int previouslyProcessedBackupsCount) {
        final Map<String, Object> internalMethodArguments = new HashMap<>();
        internalMethodArguments.put(INTERNAL_ACTIVITY_JOBID, activityJobId);
        internalMethodArguments.put(INTERNAL_CURRENT_BACKUP_NAME, currentBackupName);
        internalMethodArguments.put(INTERNAL_INPUT_BACKUPS, inputBackups);
        internalMethodArguments.put(INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT, previouslyProcessedBackupsCount);
        return internalMethodArguments;
    }

    private void evaluateExecuteResponse(final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final String neType, final Map<String, Object> activityJobAttributes,
            final SessionIdResponse response, final Map<String, Object> internalMethodArguments) {
        final long activityJobId = (long) internalMethodArguments.get(INTERNAL_ACTIVITY_JOBID);
        final int previouslyProcessedBackupsCount = (int) internalMethodArguments.get(INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT);
        final String inputBackups = (String) internalMethodArguments.get(INTERNAL_INPUT_BACKUPS);
        final String currentBackupName = (String) internalMethodArguments.get(INTERNAL_CURRENT_BACKUP_NAME);

        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobProperties, CURRENT_BACKUP, currentBackupName);
        activityUtils.prepareJobPropertyList(jobProperties, TOTAL_BACKUPS, Integer.toString(inputBackups.split(ActivityConstants.COMMA).length));

        if (response.getSessionId() != null) {
            LOGGER.debug("Upload request submitted to winfiol successfully with response: {}", response);
            final String existingStepDurations = (String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS);
            if (existingStepDurations == null || !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            }
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
            activityUtils.prepareJobPropertyList(jobProperties, SESSION_ID, response.getSessionId());
            activityUtils.prepareJobPropertyList(jobProperties, AxeConstants.HOST_NAME, response.getHostname());
            activityUtils.prepareJobPropertyList(jobProperties, AxeConstants.COOKIE_HEADER, response.getCookie());
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));

            final Integer uploadactivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.AXE.getName(), JobTypeEnum.BACKUP.name(),
                    activityAnnotation.activityName());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, UPLOAD_DISPLAY_NAME, uploadactivityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), currentBackupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, null);

        } else {
            activityUtils.prepareJobPropertyList(jobProperties, PROCESSED_BACKUPS, Integer.toString(previouslyProcessedBackupsCount + 1));
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData.getParentNodeName(), currentBackupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            final Map<String, Object> processVariables = evaluateRepetition(JobResult.FAILED, jobLogList, activityJobAttributes, response.getError(), jobProperties, internalMethodArguments);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, null);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), processVariables);
        }
    }

    private Map<String, Object> evaluateRepetition(final JobResult jobResult, final List<Map<String, Object>> jobLogList, final Map<String, Object> activityJobAttributes, final String errorMessage,
            final List<Map<String, Object>> jobProperties, final Map<String, Object> internalMethodArguments) {
        final long activityJobId = (long) internalMethodArguments.get(INTERNAL_ACTIVITY_JOBID);
        final int previouslyProcessedBackupsCount = (int) internalMethodArguments.get(INTERNAL_PREVIOUSLY_PROCESSED_BACKUPS_COUNT);
        final String inputBackups = (String) internalMethodArguments.get(INTERNAL_INPUT_BACKUPS);
        final String currentBackupName = (String) internalMethodArguments.get(INTERNAL_CURRENT_BACKUP_NAME);

        if (jobResult == JobResult.FAILED) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTIVITY_FAILED_FOR_BACKUP.concat(". ").concat(JobLogConstants.FAILURE_REASON), UPLOAD_DISPLAY_NAME, currentBackupName, errorMessage), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());

            final String failedBackupProperty = activityUtils.getActivityJobAttributeValue(activityJobAttributes, FAILED_BACKUPS);
            final String updatedFailedBackups = failedBackupProperty.isEmpty() ? currentBackupName : failedBackupProperty.concat(",").concat(currentBackupName);
            activityUtils.prepareJobPropertyList(jobProperties, FAILED_BACKUPS, updatedFailedBackups);
            activityUtils.prepareJobPropertyList(jobProperties, INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        } else if (jobResult == JobResult.SUCCESS) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FOR_BACKUP_COMPLETED_SUCCESSFULLY, UPLOAD_DISPLAY_NAME, currentBackupName), new Date(),
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

    @SuppressWarnings("unchecked")
    private String getInputBackupNames(final NEJobStaticData neJobStaticData, final String neType) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId());

        //TODO from where to get INPUT_BACKUP_NAMES from inputs incase of manage backup
        String backupNames = activityUtils.getActivityJobAttributeValue(neJobAttributes, INPUT_BACKUP_NAMES);
        if (backupNames.isEmpty()) {
            final Map<String, String> backupNamesMap = jobPropertyUtils.getPropertyValue(Arrays.asList(UPLOAD_BACKUP_DETAILS), jobConfigurationDetails, neJobStaticData.getNodeName(), neType,
                    neJobStaticData.getPlatformType());
            backupNames = backupNamesMap.get(UPLOAD_BACKUP_DETAILS);
        }
        LOGGER.debug("In getInputBackupNames for AXE nodes, BackupNames {} ", backupNames);
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

    private String getDestinationDirectory(final String neType, final NEJobStaticData neJobStaticData, final String componentType) {
        final String nodeAndComponentPath = FilenameUtils.concat(neJobStaticData.getParentNodeName(), componentType);
        //TODO : Path retrieval from SMRS is avoided due to a technical debt: https://jira-nam.lmera.ericsson.se/browse/TORF-324635
        return FilenameUtils.concat("/ericsson/tor/smrs/smrsroot/backup/" + neType.toLowerCase(), nodeAndComponentPath);
    }

    @Override
    @Asynchronous
    public void subscribeForPolling(final long activityJobId) {
        subscribe(activityJobId, activityAnnotation);
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
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            LOGGER.debug("Evaluating time out step for businesskey={}, activityJobId={}", jobBussinessKey, activityJobId);

            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String sessionId = activityUtils.getActivityJobAttributeValue(activityJobAttributes, SESSION_ID);
            final String currentBackupName = activityUtils.getActivityJobAttributeValue(activityJobAttributes, CURRENT_BACKUP);
            final int processedBackupsCount = getProcessedBackupsCount(activityJobAttributes);
            final String inputBackups = getInputBackupNames(neJobStaticData, neType);

            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityAnnotation.activityName(), neJobStaticData.getNodeName());
            final String host = getHostName(activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER),
                    activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.HOST_NAME));
            final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getNeJobId(), jobStaticData);
            final Map<String, Object> headerMap = getHeaders(shmJobExecUser, activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER));
            final UploadBackupPollResponse response = executeGetRequest(String.format(WINFIOL_BACKUP_TRANSFER_POLLING_URI, sessionId), host, headerMap, UploadBackupPollResponse.class);
            final JobResult jobResult = response.getStatus() == 1 ? JobResult.SUCCESS : JobResult.FAILED;
            final Map<String, Object> internalMethodArguments = generateInternalMethodArguments(activityJobId, currentBackupName, inputBackups, processedBackupsCount);
            processVariables = evaluateRepetition(jobResult, jobLogList, activityJobAttributes, response.getStatusMsg(), jobProperties, internalMethodArguments);
            final String jobPropertyResult = activityUtils.getActivityJobAttributeValue(Collections.singletonMap(ActivityConstants.JOB_PROPERTIES, (Object) jobProperties),
                    ActivityConstants.ACTIVITY_RESULT);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, null);
            if (JobResult.CANCELLED.toString().equalsIgnoreCase(jobPropertyResult)) {
                workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, neJobStaticData.getNeJobBusinessKey());
            } else {
                activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityAnnotation.activityName(), processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
            }
            logActivityCompletionFlow(activityJobId, neJobStaticData, currentBackupName, SHMEvents.UPLOAD_BACKUP_TIME_OUT, jobResult, ActivityConstants.COMPLETED_THROUGH_TIMEOUT);

            if (jobResult == JobResult.SUCCESS) {
                final String component = getComponentType(neJobStaticData.getNodeName());
                smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(neJobStaticData.getParentNodeName(), component, currentBackupName);
            }

        } catch (Exception e) {
            LOGGER.error("Timeout evaluation failed. reason: {}", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, UPLOAD_DISPLAY_NAME, e.getMessage()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, null);
            activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityAnnotation.activityName(), processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);

        }
        if (neJobStaticData != null
                && (!processVariables.containsKey(JobVariables.ACTIVITY_REPEAT_EXECUTE) || !Boolean.parseBoolean(processVariables.get(JobVariables.ACTIVITY_REPEAT_EXECUTE).toString()))) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        }
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, UPLOAD_DISPLAY_NAME);
    }

    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        try {
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String sessionId = activityUtils.getActivityJobAttributeValue(activityJobAttributes, SESSION_ID);
            final String currentBackupName = activityUtils.getActivityJobAttributeValue(activityJobAttributes, CURRENT_BACKUP);
            final int processedBackupsCount = getProcessedBackupsCount(activityJobAttributes);
            final String inputBackups = getInputBackupNames(neJobStaticData, neType);
            final List<Map<String, Object>> jobProperties = new ArrayList<>();
            final String host = getHostName(activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER),
                    activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.HOST_NAME));
            final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getNeJobId(), jobStaticData);
            final Map<String, Object> headerMap = getHeaders(shmJobExecUser, activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER));
            final UploadBackupPollResponse response = executeGetRequest(String.format(WINFIOL_BACKUP_TRANSFER_POLLING_URI, sessionId), host, headerMap, UploadBackupPollResponse.class);
            final JobResult jobResult = getResult(response.getStatus(), currentBackupName, jobLogList);
            LOGGER.debug("polling response while uploading {} is {}", currentBackupName, response);
            if (jobResult != null) {
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityAnnotation.activityName(), neJobStaticData.getNodeName());
                final Map<String, Object> internalMethodArguments = generateInternalMethodArguments(activityJobId, currentBackupName, inputBackups, processedBackupsCount);
                final Map<String, Object> processVariables = evaluateRepetition(jobResult, jobLogList, activityJobAttributes, response.getStatusMsg(), jobProperties, internalMethodArguments);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, response.getPercentageDone());
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), processVariables);
                logActivityCompletionFlow(activityJobId, neJobStaticData, currentBackupName, SHMEvents.UPLOAD_BACKUP_SERVICE, jobResult, ActivityConstants.COMPLETED_THROUGH_POLLING);
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
                if (jobResult == JobResult.SUCCESS) {
                    final String nodeName = neJobStaticData.getParentNodeName();
                    final String component = getComponentType(neJobStaticData.getNodeName());
                    smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(nodeName, component, currentBackupName);
                }
            } else {
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, response.getPercentageDone());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to evaluate job result. wait till the next elapse or a complete timeout.", e);
        }
    }

    private void logActivityCompletionFlow(final long activityJobId, final NEJobStaticData neJobStaticData, final String currentBackupName, final String event, final JobResult jobResult,
            final String flow) {
        final String eventName = activityUtils.getActivityCompletionEvent(activityAnnotation.platform(), activityAnnotation.jobType(), activityAnnotation.activityName());
        final CommandPhase commandPhase = jobResult == JobResult.SUCCESS ? CommandPhase.FINISHED_WITH_SUCCESS : CommandPhase.FINISHED_WITH_ERROR;
        systemRecorder.recordCommand(event, commandPhase, neJobStaticData.getNodeName(), currentBackupName,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        activityUtils.recordEvent(eventName, neJobStaticData.getNodeName(), currentBackupName,
                "SHM:" + activityJobId + ":Upload activity completed" + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }

    private JobResult getResult(final int responseCode, final String currentBackupName, final List<Map<String, Object>> jobLogList) {
        JobResult jobResult = null;
        final WinFIOLRequestStatus status = WinFIOLRequestStatus.getEnum(responseCode);
        if (status == WinFIOLRequestStatus.FAILED) {
            jobResult = JobResult.FAILED; //log message will be updated during repetition evaluation
        } else if (status == WinFIOLRequestStatus.OK) {
            jobResult = JobResult.SUCCESS;//log message will be updated during repetition evaluation
        } else if (status == WinFIOLRequestStatus.NOT_FOUND) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format("Upload progress for \"%s\" is not available currently", currentBackupName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format("Upload is ongoing for \"%s\"", currentBackupName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }

        return jobResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        return cancelBackupService.cancel(activityJobId, AxeConstants.UPLOAD_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean isCancelRetriesExhausted) {
        return cancelBackupService.cancelTimeout(activityJobId, AxeConstants.UPLOAD_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public void processNotification(final Notification notification) {
        //        Not Needed - Backward compatible legacy interface
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        //        Not Needed - Backward compatible legacy interface
    }

}