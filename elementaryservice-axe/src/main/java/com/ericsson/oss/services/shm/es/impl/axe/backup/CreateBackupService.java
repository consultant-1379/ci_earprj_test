/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup;

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
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ShmException;
import com.ericsson.oss.services.shm.common.job.activity.JobType;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.AsynchronousPollingActivity;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants;
import com.ericsson.oss.services.shm.es.axe.common.BackupPollResponse;
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
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

/**
 * @author xprapav
 */
@EServiceQualifier("AXE.BACKUP.createbackup")
@ActivityInfo(activityName = "createbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.AXE)
@Traceable
@Profiled
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CreateBackupService extends AbstractAxeBackupService implements Activity, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack {

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateBackupService.class);
    private static final ActivityInfo activityAnnotation = CreateBackupService.class.getAnnotation(ActivityInfo.class);

    /**
     * axe-node-backup-status/{sessionId}
     */
    private static final String AXE_CREATE_BACKUP_POLL_REST_URL = "axe-node-backup-status/%s";

    private static final String AXE_OVERWRITE_ROTATE_BACKUP_REST_URL = "axe-node-backup/%s/%s?nextFile=%s&rotateCpBackups=%s";

    @Inject
    private CancelBackupService cancelBackupService;
    
    @Inject
    ApgProductRevisionProviderImpl apgProductRevisionProviderImpl;

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {

        String jobBusinessKey = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            jobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            final String nodeName = neJobStaticData.getNodeName();
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityAnnotation.activityName());
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, jobBusinessKey, activityAnnotation.activityName());
                return;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, AxeConstants.CREATE_BACKUP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            final String componentType = getComponentType(nodeName);
            final Map<String, Object> jobConfigurationDetails = readJobConfigDetailsFromMainJob(neJobStaticData.getMainJobId());
            String componentProductRevision = null;
            if (nodeName.contains(ShmConstants.APG_COMPONENT_IDENTIFIER_FROM_UI)) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SUPPORT_SECURE_BKP_FOR_APG, AxeConstants.CREATE_BACKUP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                boolean isSecureBackupForApg = isSecureBackup(jobConfigurationDetails, componentType, neType);
                if (isSecureBackupForApg) {
                    componentProductRevision = apgProductRevisionProviderImpl.readNeProductRevisionValueFromSwInventory(neJobStaticData.getNodeName());
                    if (failActivityAndUpdateJobLogIfProdRevNotFoundOrInInvalidFormat(activityJobId, neJobStaticData, jobLogList, componentProductRevision)) {
                        return;
                    }else{
                        activityUtils.prepareJobPropertyList(jobPropertyList, ShmConstants.NE_PRODUCT_REVISION, componentProductRevision);
                    }
                }
            }
            final Map<String, String> encryptBackupWithPwdDetails = new HashMap<>();
            final List<String> selectedActivityParamsList = getSelectedActivityParams(jobConfigurationDetails, componentType, neType, componentProductRevision, encryptBackupWithPwdDetails, nodeName);
            LOGGER.info("Invoking WinFIOL RestCall on Node:{} with Type {}", nodeName, componentType);
            final Map<String, Object> headerMap = prepareHeadersInformation(neJobStaticData.getParentNodeName(), componentType, encryptBackupWithPwdDetails, activityAnnotation.activityName());
            final Map<String, Object> connectivityInfoMap = dpsUtil.getConnectivityInformation(neJobStaticData.getParentNodeName(), componentType);
            LOGGER.info("headerMap {} and connectivityInfoMap {}", headerMap, connectivityInfoMap);
            final String uri = getURI(componentType, connectivityInfoMap, selectedActivityParamsList);
            final SessionIdResponse sessionIdResponse = executeGetRequest(uri, AxeConstants.REST_HOST_NAME, headerMap, SessionIdResponse.class);
            LOGGER.info("createbackup action Response from WinFiol {}", sessionIdResponse);
            processSessionIdResponse(activityJobId, jobLogList, jobPropertyList, neJobStaticData, neType, selectedActivityParamsList, sessionIdResponse);
        } catch (JobDataNotFoundException e) {
            LOGGER.error("Job Information not retrieved. reason:{}", e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBusinessKey, activityAnnotation.activityName());
        } catch (Exception e) {
            LOGGER.error("NetworkElement not retrieved. reason:{}", e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, activityAnnotation.activityName(), e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBusinessKey, activityAnnotation.activityName());
        }

    }

    private boolean failActivityAndUpdateJobLogIfProdRevNotFoundOrInInvalidFormat(final long activityJobId, NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final String componentProductRevision) {
        if (componentProductRevision == null || componentProductRevision.isEmpty()) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILED_TO_GET_APGVERSION), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), activityAnnotation.activityName());
            return true;
        }else if(!isProductRevisionInValidFormat(componentProductRevision, neJobStaticData.getNodeName())){
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.GOT_APGVERSION_IN_INVALID_FORMAT,componentProductRevision), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), activityAnnotation.activityName());
            return true;
        }
        return false;
    }

    /**
     * @param componentProductRevision
     * @return
     */
    private boolean isProductRevisionInValidFormat(final String componentProductRevision, final String nodeName) {
        try {
            Double.valueOf(componentProductRevision.substring(0, componentProductRevision.indexOf('.') + 2));
            return true;
        } catch (NumberFormatException e) {
            LOGGER.error("Received apg version data in unexpected format {} for node {}", componentProductRevision, nodeName);
            return false;
        }
    }

    private void processSessionIdResponse(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList, NEJobStaticData neJobStaticData,
            final String neType, final List<String> selectedActivityParamsList, final SessionIdResponse sessionIdResponse) {
        if (sessionIdResponse.getSessionId() != null && !sessionIdResponse.getSessionId().isEmpty()) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.AXE.name(), JobTypeEnum.BACKUP.name(), activityAnnotation.activityName());
            updateJobLogWithSelectedParams(selectedActivityParamsList, jobLogList, activityTimeout, null, false, getComponentType(neJobStaticData.getNodeName()));
            activityUtils.prepareJobPropertyList(jobPropertyList, AxeConstants.SESSION_ID, sessionIdResponse.getSessionId());
            activityUtils.prepareJobPropertyList(jobPropertyList, AxeConstants.HOST_NAME, sessionIdResponse.getHostname());
            activityUtils.prepareJobPropertyList(jobPropertyList, AxeConstants.COOKIE_HEADER, sessionIdResponse.getCookie());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        } else {
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData.getNodeName(), activityAnnotation.activityName(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, sessionIdResponse.getError()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            if (activityUtils.cancelTriggered(activityJobId)) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.toString());
            } else {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), Collections.<String, Object> emptyMap());
        }
    }

    private List<String> getSelectedActivityParams(final Map<String, Object> jobConfigurationDetails, final String componentType, final String neType, final String componentProductRevision,
            final Map<String, String> encryptBackupWithPwdDetails, final String nodeName) {
        List<String> selectedActivityParamsList = new ArrayList<>();
        final List<Map<String, Object>> neTypeJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NETYPEJOBPROPERTIES);
        if (neTypeJobProperties != null && !neTypeJobProperties.isEmpty()) {
            for (final Map<String, Object> neTypeJobProperty : neTypeJobProperties) {
                if (neTypeJobProperty.get(ShmConstants.NETYPE).equals(neType)) {
                    selectedActivityParamsList.addAll(getSelectedParams(neTypeJobProperty, componentType, componentProductRevision, encryptBackupWithPwdDetails, nodeName));
                    break;
                }
            }
        }
        LOGGER.debug("CreateBackup selected Activity Params for Component {} are :{}", componentType, selectedActivityParamsList);
        return selectedActivityParamsList;
    }

    private boolean isSecureBackup(final Map<String, Object> jobConfigurationDetails, final String componentType, final String neType) {
        final List<Map<String, Object>> neTypeJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NETYPEJOBPROPERTIES);
        if (neTypeJobProperties != null && !neTypeJobProperties.isEmpty()) {
            return isPwdExistsInNeTypeJobProperties(componentType, neType, neTypeJobProperties);
        }
        return false;
    }

    private boolean isPwdExistsInNeTypeJobProperties(final String componentType, final String neType, final List<Map<String, Object>> neTypeJobProperties) {
        for (final Map<String, Object> neTypeJobProperty : neTypeJobProperties) {
            if (neTypeJobProperty.get(ShmConstants.NETYPE).equals(neType) && (componentType.startsWith(AxeConstants.APG_COMPONENT))) {
                final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neTypeJobProperty.get(ShmConstants.JOBPROPERTIES);
                return isPasswordExists(jobProperties);
            }

        }
        return false;
    }

    private boolean isPasswordExists(final List<Map<String, String>> jobProperties) {
        if (jobProperties != null && !jobProperties.isEmpty()) {
            for (final Map<String, String> jobProperty : jobProperties) {
                if (jobProperty.containsValue(JobPropertyConstants.SECURE_BACKUP_KEY) && (jobProperty.get(ShmConstants.VALUE) != null)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param mainJobId
     * @return
     */
    private Map<String, Object> readJobConfigDetailsFromMainJob(final long mainJobId) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(mainJobId);
        return (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
    }

    private List<String> getSelectedParams(final Map<String, Object> neTypeJobProperty, final String componentType, final String componentProductRevision,
            final Map<String, String> encryptBackupWithPwdDetails, final String nodeName) {
        final List<String> selectedParams = new ArrayList<>();
        final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neTypeJobProperty.get(ShmConstants.JOBPROPERTIES);
        if (componentType.startsWith(AxeConstants.CP_COMPONENT) || componentType.equals(AxeConstants.CLUSTER_BACKUP)) {
            prepareSelectedParams(selectedParams, jobProperties);
        } else if ((componentType.startsWith(AxeConstants.APG_COMPONENT) && getProductRevisionInValidFormat(componentProductRevision, nodeName) >= ShmConstants.BKPENCRYPTION_SUPPORT_STARTING_VERSION)) {
            prepareSelectedParams(encryptBackupWithPwdDetails, selectedParams, jobProperties);

        }
        return selectedParams;
    }

    private void prepareSelectedParams(final List<String> selectedParams, final List<Map<String, String>> jobProperties) {
        for (final Map<String, String> jobProperty : jobProperties) {
            if ((jobProperty.containsValue(AxeConstants.ROTATE) || jobProperty.containsValue(AxeConstants.OVERWRITE)) && jobProperty.get(ShmConstants.VALUE).equalsIgnoreCase(ShmConstants.TRUE)) {
                selectedParams.add(jobProperty.get(ShmConstants.KEY));
            }
        }
    }

    private void prepareSelectedParams(final Map<String, String> encryptBackupWithPwdDetails, final List<String> selectedParams, final List<Map<String, String>> jobProperties) {
        for (final Map<String, String> jobProperty : jobProperties) {
            if ((jobProperty.containsValue(JobPropertyConstants.SECURE_BACKUP_KEY)) || jobProperty.containsValue(JobPropertyConstants.USER_LABEL)) {
                selectedParams.add(jobProperty.get(ShmConstants.KEY));
                encryptBackupWithPwdDetails.put(jobProperty.get(ShmConstants.KEY), jobProperty.get(ShmConstants.VALUE));
            }
        }
    }

    private void updateJobLogWithSelectedParams(final List<String> selectedActivityParams, final List<Map<String, Object>> jobLogList, final int activityTimeout, final String backupName,
            final boolean isBackupCreated, final String componentName) {
        String backupMessage = "";
        switch (getBackupOption(selectedActivityParams, componentName)) {
        case AxeConstants.CREATE_NEW:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATE_BACKUP_WITH_OPTION, AxeConstants.CREATE_NEW_BACKUP, backupName)
                    : String.format(JobLogConstants.AXE_ASYNC_ACTION_TRIGGERED_WITH_PARAM, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, AxeConstants.CREATE_NEW_BACKUP, activityTimeout);
            break;
        case AxeConstants.OVERWRITE:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATE_BACKUP_WITH_OPTION, AxeConstants.OVERWRITE_BACKUP, backupName)
                    : String.format(JobLogConstants.AXE_ASYNC_ACTION_TRIGGERED_WITH_PARAM, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, AxeConstants.OVERWRITE_BACKUP, activityTimeout);

            break;
        case AxeConstants.CREATE_NEW_AND_ROTATE:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATE_BACKUP_WITH_ROTATE_OPTION, AxeConstants.CREATE_NEW_BACKUP, backupName, AxeConstants.ROTATE_BACKUP)
                    : String.format(JobLogConstants.AXE_ASYNC_ACTION_TRIGGERED_WITH_PARAM, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, AxeConstants.CREATE_NEW_AND_ROTATE_BACKUP, activityTimeout);
            break;
        case AxeConstants.OVERWRITE_AND_ROTATE:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATE_BACKUP_WITH_ROTATE_OPTION, AxeConstants.OVERWRITE_BACKUP, backupName, AxeConstants.ROTATE_BACKUP)
                    : String.format(JobLogConstants.AXE_ASYNC_ACTION_TRIGGERED_WITH_PARAM, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, AxeConstants.OVERWRITE_AND_ROTATE_BACKUP, activityTimeout);
            break;
        case AxeConstants.CREATE_ENCRYPTED_BACKUP:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATED_APG_BACKUP_SUCCESSFULLY, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, backupName)
                    : String.format(JobLogConstants.AXE_CREATEBKP_TRIGGERED_WITH_ENCRYPTION_SUPPORT, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, activityTimeout);
            break;
        case AxeConstants.CREATE_REGULAR_BACKUP:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATED_APG_BACKUP_SUCCESSFULLY, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, backupName)
                    : String.format(JobLogConstants.AXE_CREATE_REGULAR_BKP_TRIGGERED, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, activityTimeout);
            break;
        default:
            backupMessage = isBackupCreated ? String.format(JobLogConstants.CREATE_BACKUP_WITH_OPTION, AxeConstants.CREATE_NEW_BACKUP, backupName)
                    : String.format(JobLogConstants.AXE_ASYNC_ACTION_TRIGGERED_WITH_PARAM, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, AxeConstants.CREATE_NEW_BACKUP, activityTimeout);
            break;
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, backupMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private String getURI(final String componentType, final Map<String, Object> connectivityInfoMap, final List<String> selectedActivityParams) {
        final String componentName = (componentType.contains("AP")) ? "AP" : componentType;
        final String ipAddress = getNodeIpAddress(componentType, connectivityInfoMap);
        return getResolvedURI(componentName, ipAddress, selectedActivityParams);
    }

    private String getNodeIpAddress(final String componentType, final Map<String, Object> connectivityInfoMap) {
        String ipAddress = (String) connectivityInfoMap.get(AxeConstants.IP_ADDRESS);
        if (componentType.equalsIgnoreCase(AxeConstants.APG2_COMPONENT)) {
            final String ap2clusterIpAddress = (String) connectivityInfoMap.get(AxeConstants.AP2_CLUSTER_IP_ADDRESS);
            if (ap2clusterIpAddress == null) {
                throw new ShmException("Invalid Node IP configuration for APG2 Component");
            } else {
                ipAddress = ap2clusterIpAddress;
            }
        }
        return ipAddress;
    }

    private String getResolvedURI(final String componentName, final String ipAddress, final List<String> selectedActivityParams) {
        String uri = "";
        switch (getBackupOption(selectedActivityParams, componentName)) {
        case AxeConstants.CREATE_NEW:
            uri = String.format(AXE_OVERWRITE_ROTATE_BACKUP_REST_URL, ipAddress, componentName, Boolean.TRUE, Boolean.FALSE);
            break;
        case AxeConstants.OVERWRITE:
            uri = String.format(AXE_OVERWRITE_ROTATE_BACKUP_REST_URL, ipAddress, componentName, Boolean.FALSE, Boolean.FALSE);
            break;
        case AxeConstants.CREATE_NEW_AND_ROTATE:
            uri = String.format(AXE_OVERWRITE_ROTATE_BACKUP_REST_URL, ipAddress, componentName, Boolean.TRUE, Boolean.TRUE);
            break;
        case AxeConstants.OVERWRITE_AND_ROTATE:
            uri = String.format(AXE_OVERWRITE_ROTATE_BACKUP_REST_URL, ipAddress, componentName, Boolean.FALSE, Boolean.TRUE);
            break;
        default:
            uri = String.format(AXE_OVERWRITE_ROTATE_BACKUP_REST_URL, ipAddress, componentName, Boolean.TRUE, Boolean.FALSE);
            break;
        }
        return uri;
    }

    private String getBackupOption(final List<String> selectedActivityParams, final String componentName) {
        if (selectedActivityParams.contains(AxeConstants.ROTATE) && !selectedActivityParams.contains(AxeConstants.OVERWRITE)) {
            return AxeConstants.CREATE_NEW_AND_ROTATE;
        } else if (selectedActivityParams.contains(AxeConstants.ROTATE) && selectedActivityParams.contains(AxeConstants.OVERWRITE)) {
            return AxeConstants.OVERWRITE_AND_ROTATE;
        } else if (!selectedActivityParams.contains(AxeConstants.ROTATE) && selectedActivityParams.contains(AxeConstants.OVERWRITE)) {
            return AxeConstants.OVERWRITE;
        } else if (componentName.contains(AxeConstants.APG_COMPONENT)) {
            return selectedActivityParams.contains(JobPropertyConstants.SECURE_BACKUP_KEY) ? AxeConstants.CREATE_ENCRYPTED_BACKUP : AxeConstants.CREATE_REGULAR_BACKUP;
        }else if (selectedActivityParams.isEmpty()) {
            return AxeConstants.CREATE_NEW;
        }
        
        return AxeConstants.CREATE_NEW;
    }

    @Override
    public void subscribeForPolling(final long activityJobId) {
        subscribe(activityJobId, activityAnnotation);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        NEJobStaticData neJobStaticData;
        String nodeName = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        try {
            LOGGER.info("Inside  AXE CreateBackupService.processPollingResponse with activityJobId: {}", activityJobId);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getNeJobId(), jobStaticData);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String sessionId = activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.SESSION_ID);
            final String host = getHostName(activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER),
                    activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.HOST_NAME));
            nodeName = neJobStaticData.getNodeName();
            final String componentType = getComponentType(nodeName);
            final Map<String, Object> headerMap = getHeaders(shmJobExecUser, activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER));
            final Map<String, Object> createBackupPollResponseMap = (Map<String, Object>) executeGetRequest(String.format(AXE_CREATE_BACKUP_POLL_REST_URL, sessionId), host, headerMap, Map.class);
            LOGGER.info("WinFiolResponse in processPollingResponse {}", createBackupPollResponseMap);
            final String componentName = getComponentName(componentType);
            if (createBackupPollResponseMap.get(componentName.toLowerCase()) != null) {
                final BackupPollResponse backupPollResponse = parseWinFiolResponse(componentName, createBackupPollResponseMap);
                final JobResult jobResult = getJobResult(backupPollResponse, jobPropertyList, jobLogList, activityJobId, componentType, neJobStaticData);
                if (jobResult != null) {
                    pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityAnnotation.activityName(), neJobStaticData.getNodeName());
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, Double.valueOf(backupPollResponse.getPercentageDone()));
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
                    activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), Collections.<String, Object> emptyMap());
                    logActivityCompletionFlow(activityJobId, neJobStaticData, backupPollResponse.getBackupName(), SHMEvents.CREATE_BACKUP_SERVICE, jobResult,
                            ActivityConstants.COMPLETED_THROUGH_POLLING);

                } else {
                    updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, Double.valueOf(backupPollResponse.getPercentageDone()), activityJobId, neJobStaticData.getNeJobId());
                }
            } else {
                LOGGER.warn("Invalid Response from WinFiol in Createbackup activity polling for node {},..Retrying..", nodeName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception occured in processPollingResponse for node {}. reason:{},.Retrying..", nodeName, e);
        }
    }

    protected void updateActivityAndNeJobProgressPercentage(final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final Double progressPercentage,
            final long activityJobId, final long neJobId) {
        if (jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, progressPercentage)) {
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        }
    }

    private String getComponentName(final String componentType) {
        String componentName = null;
        switch (componentType) {
        case "APG1":
        case "APG2":
        case "APG":
            componentName = "ap";
            break;
        case "CP1":
        case "CP2":
        case "Cluster":
            componentName = "cp";
            break;
        default:
            componentName = componentType;
            break;
        }
        return componentName;
    }

    private void persistBackupNameInProperties(final NEJobStaticData neJobStaticData, final BackupPollResponse backupPollResponse, final List<String> selectedActivityParamsList) {
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        final String backupOption = getBackupOption(selectedActivityParamsList, getComponentType(neJobStaticData.getNodeName()));
        if (backupOption.equals(AxeConstants.CREATE_NEW_AND_ROTATE) || backupOption.equals(AxeConstants.OVERWRITE_AND_ROTATE)) {
            activityUtils.prepareJobPropertyList(neJobPropertyList, AxeConstants.INPUT_BACKUP_NAMES, AxeConstants.LATEST_BACKUPDATA_AFTER_ROTATE);
        } else {
            activityUtils.prepareJobPropertyList(neJobPropertyList, AxeConstants.INPUT_BACKUP_NAMES, backupPollResponse.getBackupName());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobStaticData.getNeJobId(), neJobPropertyList, null, null);
    }

    @SuppressWarnings("unchecked")
    private BackupPollResponse parseWinFiolResponse(final String componentType, final Map<String, Object> createBackupPollResponseMap) {
        final Map<String, Object> backupPollResponseMap = (Map<String, Object>) createBackupPollResponseMap.get(componentType.toLowerCase());
        return new BackupPollResponse(backupPollResponseMap);
    }

    private JobResult getJobResult(final BackupPollResponse backupPollResponse, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final long activityJobId,
            final String componentType, final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        JobResult jobResult = null;
        if (backupPollResponse.getStatus().contains(AxeConstants.BACKUP_COMPLETE)) {
            jobResult = JobResult.SUCCESS;
            final Map<String, String> encryptBackupWithPwdDetails = new HashMap();
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
            final Map<String, Object> jobConfigurationDetails = readJobConfigDetailsFromMainJob(neJobStaticData.getMainJobId());
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String componentProductRevision = activityUtils.getActivityJobAttributeValue(activityJobAttributes, ShmConstants.NE_PRODUCT_REVISION);
            final List<String> selectedActivityParamsList = getSelectedActivityParams(jobConfigurationDetails, componentType, neType, componentProductRevision, encryptBackupWithPwdDetails, neJobStaticData.getNodeName());
            persistBackupNameInProperties(neJobStaticData, backupPollResponse, selectedActivityParamsList);
            updateJobLogWithSelectedParams(selectedActivityParamsList, jobLogList, 0, backupPollResponse.getBackupName(), true, getComponentType(neJobStaticData.getNodeName()));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        } else if (backupPollResponse.getStatus().contains(AxeConstants.BACKUP_ONGOING)) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, backupPollResponse.toString(), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, AxeConstants.CREATE_BACKUP_DISPLAY_NAME), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.toString());
            jobResult = JobResult.CANCELLED;
        } else {
            jobResult = JobResult.FAILED;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, backupPollResponse.getStatusMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());

        }
        return jobResult;
    }

    private void logActivityCompletionFlow(final long activityJobId, final NEJobStaticData neJobStaticData, final String currentBackupName, final String event, final JobResult jobResult,
            final String flow) {
        final String eventName = activityUtils.getActivityCompletionEvent(activityAnnotation.platform(), activityAnnotation.jobType(), activityAnnotation.activityName());
        final CommandPhase commandPhase = jobResult == JobResult.SUCCESS ? CommandPhase.FINISHED_WITH_SUCCESS : CommandPhase.FINISHED_WITH_ERROR;
        systemRecorder.recordCommand(event, commandPhase, neJobStaticData.getNodeName(), currentBackupName,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        activityUtils.recordEvent(eventName, neJobStaticData.getNodeName(), currentBackupName,
                "SHM:" + activityJobId + ":CreateBackup activity completed" + String.format(ActivityConstants.COMPLETION_FLOW, flow));
    }

    @SuppressWarnings("unchecked")
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside  AXE CreateBackupService.asyncHandleTimeout with activityJobId: {}", activityJobId);
        String nodeName = null;
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        String jobBussinessKey = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();
            activityStartTime = neJobStaticData.getActivityStartTime();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getNeJobId(), jobStaticData);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityAnnotation.activityName(), nodeName);
            setSystemRecordEventData(activityJobId, neJobStaticData, activityAnnotation.activityName(), JobType.BACKUP.name(), SHMEvents.CREATE_BACKUP_TIME_OUT);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String sessionId = activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.SESSION_ID);
            final String host = getHostName(activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER),
                    activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.HOST_NAME));
            final String componentType = getComponentType(nodeName);
            final Map<String, Object> headerMap = getHeaders(shmJobExecUser, activityUtils.getActivityJobAttributeValue(activityJobAttributes, AxeConstants.COOKIE_HEADER));
            final String componentName = getComponentName(componentType);
            final Map<String, Object> createBackupPollResponseMap = (Map<String, Object>) executeGetRequest(String.format(AXE_CREATE_BACKUP_POLL_REST_URL, sessionId), host, headerMap, Map.class);
            LOGGER.info("WinFiolResponse in asyncHandleTimeout {}", createBackupPollResponseMap);
            if (createBackupPollResponseMap.get(componentName.toLowerCase()) != null) {
                final BackupPollResponse backupPollResponse = parseWinFiolResponse(componentName, createBackupPollResponseMap);
                final JobResult jobResult = getJobResult(backupPollResponse, jobPropertyList, jobLogList, activityJobId, componentType, neJobStaticData);
                if (jobResult != null) {
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, Double.valueOf(backupPollResponse.getPercentageDone()));
                    activityStepResultEnum = (JobResult.SUCCESS == jobResult) ? ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS : activityStepResultEnum;
                } else {
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, AxeConstants.CREATE_BACKUP_DISPLAY_NAME), new Date(),
                            JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                    updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, Double.valueOf(backupPollResponse.getPercentageDone()), activityJobId, neJobStaticData.getNeJobId());
                }
                LOGGER.info("Sending back ActivityStepResult to WorkFlow from ecim CreateBackupService.asyncHandleTimeout with result:{} for node {} with activityJobId {} and neJobId {}",
                        activityStepResultEnum, nodeName, activityJobId, neJobStaticData.getNeJobId());
            } else {
                LOGGER.debug("createBackup in asyncHandleTimeout is {}", createBackupPollResponseMap);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, AxeConstants.INVALID_RESPONSE),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            }
        } catch (Exception e) {
            LOGGER.error("Exception ocuured in asyncHandleTimeout().reason: {}", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (activityUtils.cancelTriggered(activityJobId)) {
            workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, jobBussinessKey);
        } else {
            activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, AxeConstants.CREATE_BACKUP_DISPLAY_NAME, activityStepResultEnum);
        }
        if (neJobStaticData != null) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, AxeConstants.CREATE_BACKUP_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        return cancelBackupService.cancel(activityJobId, AxeConstants.CREATE_BACKUP_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean isCancelRetriesExhausted) {
        return cancelBackupService.cancelTimeout(activityJobId, AxeConstants.CREATE_BACKUP_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        // Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        //Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public void processNotification(final Notification notification) {
        //Not Needed - Backward compatible legacy interface
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        //Not Needed - Backward compatible legacy interface
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        //Not Needed - Backward compatible legacy interface
    }

    private Double getProductRevisionInValidFormat(String neApgVersion, final String nodeName) {
        Double nodeApgVersion = Double.valueOf(0.0D);
        try {
            if (neApgVersion != null && !neApgVersion.isEmpty()) {
                nodeApgVersion = Double.valueOf(neApgVersion.substring(0, neApgVersion.indexOf('.') + 2));
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Received apg version data in unexpected format {} for node {}", neApgVersion, nodeName);
        }
        return nodeApgVersion;
    }

}
