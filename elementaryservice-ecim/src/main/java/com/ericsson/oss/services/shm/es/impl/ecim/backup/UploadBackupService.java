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
package com.ericsson.oss.services.shm.es.impl.ecim.backup;

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

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.CmSupervisionStatusProvider;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException;
import com.ericsson.oss.services.shm.common.exception.BrmBackupMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.ReportProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.es.instrumentation.impl.SmrsFileSizeInstrumentationService;
import com.ericsson.oss.services.shm.es.moaction.MoActionMTRManager;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;
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
 * This class facilitates the functionality of uploading the backup stored on ECIM node to an external location.
 *
 * @author erochat
 */

@EServiceQualifier("ECIM.BACKUP.uploadbackup")
@ActivityInfo(activityName = "uploadbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@SuppressWarnings("PMD")
public class UploadBackupService implements Activity, ActivityCallback, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack, MOActionCallBack {

    private static final Logger logger = LoggerFactory.getLogger(UploadBackupService.class);

    private static final String BACKUP_FILE_NOT_EXIST = "Backup File \"%s\" doesn't exist on node to upload.";

    private static final String ACTION_NOT_ALLOWED_EXCEPTION = "ActionNotAllowedException";

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private EcimCommonUtils ecimCommonUtils;

    @Inject
    private CancelBackupService cancelBackupService;

    @Inject
    private CmSupervisionStatusProvider cmSupervisionProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private NEJobProgressPercentageCache jobProgressPercentageCache;

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

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private MoActionMTRManager moActionMTRManager;

    @Inject
    private DpsRetryConfigurationParamProvider dpsRetryConfigurationParamProvider;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private SmrsFileSizeInstrumentationService smrsFileSizeInstrumentationService;

    /**
     * This method validates backup upload request to verify whether the upload activity can be started or not and sends back the activity result to Work Flow Service.
     *
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        logger.debug("Inside UploadBackupService precheck() with activityJobId:{}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        long activityStartTime = 0;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        StringBuilder jobLogMessage = new StringBuilder();
        String nodeName = null;

        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();
            final long mainJobId = neJobStaticData.getMainJobId();
            // Check whether the use case is normal backup job or manage backup job.
            final boolean createBackupJobwithUpload = isCreateBackupTriggeredAlongWithUpload(mainJobId);
            if (createBackupJobwithUpload) {
                jobLogMessage = processNormalBackupValidation(activityJobId, activityStepResult, jobLogList, jobPropertyList, nodeName, neJobStaticData);
            } else {
                jobLogMessage = jobLogMessage.append(String.format(JobLogConstants.PRECHECK_SUCCESS, EcimBackupConstants.UPLOAD_BACKUP));
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            }
        } catch (final BackupDataNotFoundException | JobDataNotFoundException ex) {
            logger.error("Exception occured in precheck of {} for the activity ID {} due to :: ", EcimBackupConstants.UPLOAD_BACKUP, activityJobId, ex);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.UPLOAD_BACKUP, ex.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            logger.error("Exception occured in precheck of {} for the activity ID {} due to :: ", EcimBackupConstants.UPLOAD_BACKUP, activityJobId, ex);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.UPLOAD_BACKUP, ex.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION
                || activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
            logger.debug("Skipping persisting step duration as activity is to be skipped.");
        } else {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        }
        updateJobLog(activityJobId, jobLogMessage.toString(), jobLogList, null, JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
        return activityStepResult;
    }

    private StringBuilder processNormalBackupValidation(final long activityJobId, final ActivityStepResult activityStepResult, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList, final String nodeName, final NEJobStaticData neJobStaticData) throws JobDataNotFoundException {
        boolean brmBackupExist = false;
        StringBuilder jobLogMessage = new StringBuilder();
        try {
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(neJobStaticData);
            brmBackupExist = brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo);
        } catch (final MoNotFoundException e) {
            brmBackupExist = false;
            logger.debug("Brm Backup MO does not exist. Caught MoNotFoundException :: {}", e.getMessage());
        } catch (final UnsupportedFragmentException e) {
            logger.error(e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
        if (brmBackupExist) {
            jobLogMessage = jobLogMessage.append(String.format(JobLogConstants.PRECHECK_SUCCESS, EcimBackupConstants.UPLOAD_BACKUP));
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } else {
            jobLogMessage = performPostBackupExistsValidation(activityJobId, activityStepResult, jobPropertyList, nodeName);
        }
        return jobLogMessage;
    }

    private StringBuilder performPostBackupExistsValidation(final long activityJobId, final ActivityStepResult activityStepResult, final List<Map<String, Object>> jobPropertyList,
            final String nodeName) {
        StringBuilder jobLogMessage = new StringBuilder();
        StringBuilder additionalMessageInJobLog = new StringBuilder();
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        additionalMessageInJobLog = getCMSyncStatusAndUpdateJobLog(nodeName, additionalMessageInJobLog);
        String neType = "";
        try {
            neType = networkElementRetrivalBean.getNeType(nodeName);
        } catch (final MoNotFoundException moNotFoundException) {
            jobLogMessage.append(String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.UPLOAD_BACKUP, moNotFoundException.getMessage()));
        }
        final Map<String, Integer> retryAttempts = new HashMap<>();
        final int maxRetryAttempts = activityTimeoutsService.getRepeatPrecheckRetryAttempt(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), EcimBackupConstants.UPLOAD_BACKUP);
        additionalMessageInJobLog = processIsRepeatRequiredOnPrecheck(activityJobId, activityStepResult, jobPropertyList, additionalMessageInJobLog, activityJobAttributes, neType, retryAttempts,
                maxRetryAttempts);
        jobLogMessage = jobLogMessage.append(String.format(JobLogConstants.BRM_BACKUP_MO_DOES_NOT_EXIST_IN_ENM_DB, additionalMessageInJobLog));
        final StringBuilder attemptJobLogMessage = new StringBuilder();
        jobLogMessage = addJobLogForAttempts(jobLogMessage, retryAttempts, maxRetryAttempts, attemptJobLogMessage);
        return jobLogMessage;
    }

    private StringBuilder getCMSyncStatusAndUpdateJobLog(final String nodeName, StringBuilder additionalMessageInJobLog) {
        String cmSyncStatus = "";
        try {
            cmSyncStatus = cmSupervisionProvider.getCmSyncStatus(nodeName, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        } catch (final MoNotFoundException moNotFoundException) {
            additionalMessageInJobLog = additionalMessageInJobLog.append(moNotFoundException.getMessage());
        } catch (final Exception ex) {
            additionalMessageInJobLog = additionalMessageInJobLog.append(ex.getMessage());
        }

        if ("".equals(additionalMessageInJobLog.toString())) {
            additionalMessageInJobLog = additionalMessageInJobLog.append(String.format(JobLogConstants.CM_SYNC_STATUS, cmSyncStatus));
        }
        return additionalMessageInJobLog;
    }

    /**
     * @param jobLogMessage
     * @param retryAttempts
     * @param maxRetryAttempts
     * @param attemptJobLogMessage
     * @return
     */
    private StringBuilder addJobLogForAttempts(StringBuilder jobLogMessage, final Map<String, Integer> retryAttempts, final int maxRetryAttempts, final StringBuilder attemptJobLogMessage) {
        final Integer attemptsForRepeatPrecheckAsInteger = retryAttempts.get(ActivityConstants.ATTEMPTS);
        if (attemptsForRepeatPrecheckAsInteger != 0 && attemptsForRepeatPrecheckAsInteger < maxRetryAttempts) {
            jobLogMessage = attemptJobLogMessage.append(String.format(JobLogConstants.ATTEMPT, attemptsForRepeatPrecheckAsInteger, jobLogMessage));
        } else if (attemptsForRepeatPrecheckAsInteger == maxRetryAttempts) {
            jobLogMessage = attemptJobLogMessage.append(String.format(JobLogConstants.FINAL_ATTEMPT, jobLogMessage));
        }
        return jobLogMessage;
    }

    private StringBuilder processIsRepeatRequiredOnPrecheck(final long activityJobId, final ActivityStepResult activityStepResult, final List<Map<String, Object>> jobPropertyList,
            StringBuilder additionalMessageInJobLog, final Map<String, Object> activityJobAttributes, final String neType, final Map<String, Integer> retryAttempts, final int maxRetryAttempts) {
        final boolean repeatRequiredOnPrecheck = activityUtils.isRepeatRequiredOnPrecheck(activityJobId, jobPropertyList, maxRetryAttempts, retryAttempts, activityJobAttributes);
        if (repeatRequiredOnPrecheck) {
            final int repeatPrecheckWaitInterval = activityTimeoutsService.getRepeatPrecheckWaitIntervalAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(),
                    EcimBackupConstants.UPLOAD_BACKUP);
            additionalMessageInJobLog = additionalMessageInJobLog.append(String.format(JobLogConstants.WAIT_TIME_BEFORE_REPEAT_PRECHECK, repeatPrecheckWaitInterval));
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_PRECHECK);
        } else {
            additionalMessageInJobLog = additionalMessageInJobLog.append("Failing the Activity.");
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
        return additionalMessageInJobLog;
    }

    @SuppressWarnings("unchecked")
    private boolean isCreateBackupTriggeredAlongWithUpload(final long mainJobId) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(mainJobId);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<Map<String, Object>> activities = (List<Map<String, Object>>) jobConfigurationDetails.get(JobModelConstants.ACTIVITIES);
        for (final Map<String, Object> everyActivity : activities) {
            if (ActivityConstants.CREATE_BACKUP.equals(everyActivity.get(JobModelConstants.ACTIVITY_NAME))) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action on the node.
     *
     * @param activityJobId
     */
    @Asynchronous
    @Override
    public void execute(final long activityJobId) {
        logger.debug("Inside UploadBackupService execute() with activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        String jobBussinessKey = null;
        String nodeName = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.UPLOAD_BACKUP);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, EcimBackupConstants.UPLOAD_BACKUP);
                return;
            }
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);

            initiateActivity(activityJobId, nodeName, activityJobAttributes);
            final boolean isPrecheckSuccess = true; // no validation required in case of upload backup
            // TODO Having this check even isPrecheckSuccess is true always because there is a future plan to add verification here.
            if (isPrecheckSuccess) {
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
                final String unProcessedBackups = getUnProcessedBackups(neJobStaticData, jobLogList);
                final String backup = getBrmBackupNameToBeProcessed(unProcessedBackups, activityJobAttributes);
                if (backup != null) {
                    final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
                    final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
                    final ExecuteResponse executeResponse = performActionOnNode(activityJobId, ecimBackupInfo, neJobStaticData, jobLogList, jobPropertyList);
                    doExecutePostValidation(activityJobId, executeResponse, ecimBackupInfo, jobLogList, jobPropertyList);
                } else {
                    // if backup returned from getBackupToBeProcessed is null then failing the job
                    moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
                    activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, EcimBackupConstants.UPLOAD_BACKUP);
                }
            }
        } catch (final JobDataNotFoundException ex) {
            logger.error("UploadBackupService.execute- Unable to trigger action. Reason:{} ", ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, EcimBackupConstants.UPLOAD_BACKUP);
        } catch (final BackupDataNotFoundException bkpDataNotFoundEx) {
            // Not failing the activity because now we are going to try triggering action through EBMC.
            logger.debug("Triggered action through EBMC as invocation failed through DPS based client for the node: {}. Failure reason is :", nodeName, bkpDataNotFoundEx);
        } catch (final MoNotFoundException ex) {
            final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimBackupConstants.UPLOAD_BACKUP, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            logger.error("UploadBackupService.execute- Unable to trigger action. Reason: ", ex.getMessage());
            moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, EcimBackupConstants.UPLOAD_BACKUP);
        } catch (final Exception ex) {
            logger.error("UploadBackupService.execute-Unable to trigger action. Reason:{} ", ex.getMessage());
            moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, EcimBackupConstants.UPLOAD_BACKUP);
        }
    }

    @SuppressWarnings("unchecked")
    private void initiateActivity(final long activityJobId, final String nodeName, final Map<String, Object> activityJobAttributes) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final boolean isPrecheckAlreadyDone = activityUtils.isPrecheckDone((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        if (!isPrecheckAlreadyDone) {
            logger.debug("Inside UploadBackupService initiateActivity with activityJobId {}", activityJobId);
            final String jobLogMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, EcimBackupConstants.UPLOAD_BACKUP);
            activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
            updateJobLog(activityJobId, jobLogMessage, jobLogList, jobPropertyList, JobLogLevel.INFO.toString());
        } else {
            logger.debug("Pre-Validation already done for the activityJobId:{} and node name: {}", activityJobId, nodeName);
        }
    }

    private String getUnProcessedBackups(final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        final String nodeName = neJobStaticData.getNodeName();
        String neType = null;
        String platform = null;
        try {
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            platform = neJobStaticData.getPlatformType();

        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_DOES_NOT_EXIST, String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName)), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            return null;
        }
        return getBackupManagerDetails(neJobStaticData, neType, platform);
    }

    @SuppressWarnings("unchecked")
    private String getBackupManagerDetails(final NEJobStaticData neJobStaticData, final String neType, final String platform) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<String> keyList = Arrays.asList(EcimBackupConstants.UPLOAD_BACKUP_DETAILS);
        final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId());

        Map<String, String> backupManagerDetails = ecimBackupUtils.getPropertyValue(keyList, neJobAttributes);
        if (backupManagerDetails.isEmpty()) {
            backupManagerDetails = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platform);
        }
        String backupNames = backupManagerDetails.get(EcimBackupConstants.UPLOAD_BACKUP_DETAILS);
        // Backward Compatibility
        if (backupNames == null) {
            final List<String> defaultKeyList = Arrays.asList(EcimBackupConstants.BRM_BACKUP_NAME, EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
            backupManagerDetails = ecimBackupUtils.getPropertyValue(defaultKeyList, neJobAttributes);
            if (backupManagerDetails.isEmpty()) {
                backupManagerDetails = jobPropertyUtils.getPropertyValue(defaultKeyList, jobConfigurationDetails, nodeName, neType, platform);
            }
            final String backupName = backupManagerDetails.get(EcimBackupConstants.BRM_BACKUP_NAME);
            final String backupManagerId = backupManagerDetails.get(EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
            backupNames = backupName + "/" + backupManagerId;
            return backupNames;
        }
        return backupManagerDetails.get(EcimBackupConstants.UPLOAD_BACKUP_DETAILS);
    }

    /**
     * This method processes the notifications by fetching the notification subject and validates the notification. It de-register from the notification as it founds activity is completed and notifies
     * to WorkFlowService or else it will wait for another notification.
     *
     * @param notification
     */
    @Override
    public void processNotification(final Notification notification) {
        logger.debug("Entered ECIM -upload backup - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            logger.debug("ECIM - uploadbackup - Discarding non-AVC notification.");
            return;
        }
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        logger.debug("modifiedAttributes in processNotification for activity {} : {}", activityJobId, modifiedAttributes);
        String nodeName = null;
        String logMessage = "";
        final String backup = getBackup(activityJobId);
        final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
        final String backupName = ecimBackupInfo.getBackupName();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.UPLOAD_BACKUP, modifiedAttributes);

            final boolean progressReportflag = cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP, EcimBackupConstants.BACKUP_EXPORT_ACTION);

            if (progressReportflag) {
                logger.warn("Discarding invalid notification,for the activityJobId {} and modifiedAttributes: {}", activityJobId, modifiedAttributes);
                return;
            }
            final String brmBackupMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
            processProgressReport(activityJobId, progressReport, notificationTime, brmBackupMoFdn, false);
            return;
        } catch (final UnsupportedFragmentException e) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final MoNotFoundException e) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, String.format(BACKUP_FILE_NOT_EXIST, backupName), EcimBackupConstants.BACKUP_MO_TYPE),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final JobDataNotFoundException ex) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            logger.error("UploadBackupService.processNotification- {} ", ex.getMessage());
        } catch (final Exception e) {
            logger.error("Notification processing failed for upload backup action with node:{} due to :: ", nodeName, e);
            logMessage = e.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
    }

    private void processProgressReport(final long activityJobId, final AsyncActionProgress progressReport, final Date receivingTime, final String brmBackupMoFdn,
            final boolean isCompletedThroughPolling) throws JobDataNotFoundException, MoNotFoundException {
        final String backup = getBackup(activityJobId);
        final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
        final String backupName = ecimBackupInfo.getBackupName();
        JobResult jobResult = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        double totalActivityProgressPercentage = 0.0;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        if (EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName())) {
            jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, receivingTime, EcimBackupConstants.UPLOAD_BACKUP);
        } else if (EcimBackupConstants.BACKUP_EXPORT_ACTION.equals(progressReport.getActionName()) || EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP.equals(progressReport.getActionName())) {
            jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, receivingTime, EcimBackupConstants.UPLOAD_BACKUP, backupName);
            totalActivityProgressPercentage = calculateActivityProgressPercentage(activityJobId, progressReport);
        }
        if (jobResult != null) {
            if (jobResult == JobResult.SUCCESS || jobResult == JobResult.CANCELLED) {
                logActivityCompletion(jobLogList, brmBackupMoFdn, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData, activityJobId, backupName, isCompletedThroughPolling);
            } else if (jobResult == JobResult.FAILED) {
                logActivityCompletion(jobLogList, brmBackupMoFdn, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData, activityJobId, backupName, isCompletedThroughPolling);
            }
            persistAndNotifyWFS(activityJobId, brmBackupMoFdn, jobResult, neJobStaticData, progressReport, jobLogList);

            if (jobResult == JobResult.SUCCESS) {
                final String nodeName = neJobStaticData.getNodeName();
                smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(nodeName, null, backupName);
            }
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, totalActivityProgressPercentage);
            jobProgressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
        }
    }

    @SuppressWarnings("unchecked")
    private String getBackup(final long activityJobId) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId).get(ShmConstants.JOBPROPERTIES);// ibutes().get(ShmConstants.JOBPROPERTIES);
        String backup = "";
        if (activityJobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
                if (EcimBackupConstants.CURRENT_BACKUP.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    backup = eachJobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        logger.debug("Current backup = {}", backup);
        return backup;
    }

    private Map<String, Object> evaluateRepeatRequiredAndActivityResult(final JobResult moActionResult, final List<Map<String, Object>> jobPropertyList, final long activityJobId) {
        logger.debug("Evaluate whether repeat is Required and activity result. moActionResult {}, jobPropertyList {}", moActionResult, jobPropertyList);
        boolean recentUploadFailed = false;
        boolean repeatExecute = true;
        boolean isActivitySuccess = false;
        JobResult activityJobResult = null;
        if (moActionResult == JobResult.FAILED) {
            recentUploadFailed = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        }
        final boolean allBackupsProcessed = isAllBackupsProcessed(activityJobId);
        if (allBackupsProcessed) {
            final boolean intermediateFailureHappened = isAnyIntermediateFailureHappened(activityJobId);
            if (intermediateFailureHappened || recentUploadFailed) {
                activityJobResult = JobResult.FAILED;
            } else {
                activityJobResult = JobResult.SUCCESS;
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
            repeatExecute = false;
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            activityJobResult = JobResult.CANCELLED;
            repeatExecute = false;
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
        }

        if (activityJobResult == JobResult.SUCCESS) {
            isActivitySuccess = true;
        }
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, isActivitySuccess);
        logger.debug("Is Repeat Required or ActivityResult evaluated : {}", repeatRequiredAndActivityResult);
        return repeatRequiredAndActivityResult;

    }

    @SuppressWarnings("unchecked")
    private boolean isAllBackupsProcessed(final long activityJobId) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId).get(ShmConstants.JOBPROPERTIES);// ibutes().get(ShmConstants.JOBPROPERTIES);
        final int processedBackups = getCountOfProcessedBackups(activityJobPropertyList);
        final int totalBackups = getCountOfTotalBackups(activityJobPropertyList);
        if (processedBackups == totalBackups) {
            return true;
        }
        return false;
    }

    private int getCountOfTotalBackups(final List<Map<String, String>> activityJobPropertyList) {
        int totalBackups = 0;
        if (activityJobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
                if (EcimBackupConstants.TOTAL_BACKUPS.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    totalBackups = Integer.parseInt(eachJobProperty.get(ShmConstants.VALUE));
                    break;
                }
            }
        }
        logger.debug("Total backups count = {}", totalBackups);
        return totalBackups;
    }

    @SuppressWarnings("unchecked")
    private boolean isAnyIntermediateFailureHappened(final long activityJobId) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId).get(ShmConstants.JOBPROPERTIES);// ibutes().get(ShmConstants.JOBPROPERTIES);
        for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
            if (BackupActivityConstants.INTERMEDIATE_FAILURE.equals(eachJobProperty.get(ShmConstants.KEY))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        logger.debug("Inside UploadBackupService  handleTimeout with activityJobID: {}", activityJobId);
        moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
        final ActivityStepResultEnum activityStepResultEnum = processTimeout(activityJobId);
        return activityUtils.getActivityStepResult(activityStepResultEnum);
    }

    private ActivityStepResultEnum processTimeout(final long activityJobId) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        JobResult jobResult = JobResult.FAILED;
        String jobLogMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

        String nodeName = null;
        String brmBackupMoFdn = null;
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, EcimBackupConstants.UPLOAD_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        final String backup = getBackup(activityJobId);
        final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
        final String backupName = ecimBackupInfo.getBackupName();
        long activityStartTime = 0; // don't persist if JobDataNotFoundException occurs
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, EcimBackupConstants.UPLOAD_BACKUP);
        // Get BrmBackupDetails
        try {
            final NEJobStaticData jobStaticContext = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = jobStaticContext.getNodeName();
            activityStartTime = jobStaticContext.getActivityStartTime();
            brmBackupMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.unSubscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, UploadBackupService.class));
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.UPLOAD_BACKUP, nodeName);
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.UPLOAD_BACKUP, ecimBackupInfo);
            if (progressReport != null) {
                if (isFileUploadedToSmrs(progressReport)) {
                    jobResult = JobResult.SUCCESS;
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FOR_BACKUP_COMPLETED_SUCCESSFULLY, EcimBackupConstants.UPLOAD_BACKUP, backupName);
                } else {
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.UPLOAD_BACKUP) + progressReport.getResultInfo();
                }
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.UPLOAD_BACKUP);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

            final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, activityJobId);
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);

            if (repeatRequired) {
                activityStepResultEnum = ActivityStepResultEnum.REPEAT_EXECUTE;
            } else if (isActivitySuccess) {
                activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
            }
            if (!repeatRequired) {
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
            }

            if (jobResult == JobResult.SUCCESS) {
                smrsFileSizeInstrumentationService.addInstrumentationForBackupFileSize(nodeName, null, backupName);
            }
        } catch (final MoNotFoundException e) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_DOES_NOT_EXIST, String.format(BACKUP_FILE_NOT_EXIST, backupName)), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException ex) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            logger.error("Exception occured in handleTimeout while evaluating the upload backup status.{}", ex);
            final String logMessage = String.format(JobLogConstants.STATUS_EVALUATION_FAILED, EcimBackupConstants.UPLOAD_BACKUP, ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        activityUtils.recordEvent(eventName, nodeName, brmBackupMoFdn,
                "SHM:" + activityJobId + ":" + jobLogMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
        return activityStepResultEnum;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
        final String backup = getBackup(activityJobId);
        final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
        return cancelBackupService.cancel(activityJobId, EcimBackupConstants.UPLOAD_BACKUP, ecimBackupInfo);
    }

    private ExecuteResponse performActionOnNode(final long activityJobId, final EcimBackupInfo ecimBackupInfo, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList) throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        int actionId = -1;
        String jobLogMessage = null;
        boolean actionSuccessfullyTriggered = false;
        final String nodeName = neJobStaticData.getNodeName();
        final String backupName = ecimBackupInfo.getBackupName();
        final String brmBackupMoFdn = getNotifiableMoFdn(activityJobId, ecimBackupInfo, neJobStaticData, jobLogList);
        if (brmBackupMoFdn == null || brmBackupMoFdn.isEmpty()) {
            throw new BackupDataNotFoundException(
                    "Backup Mo for Node name " + nodeName + " with Domain Name " + ecimBackupInfo.getDomainName() + "and Domain Type " + ecimBackupInfo.getBackupType() + " is not found.");
        }
        try {
            activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_EXECUTE, backupName, brmBackupMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final double activityProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
            final String unProcessedBackups = getUnProcessedBackups(neJobStaticData, jobLogList);
            final String backup = getBrmBackupNameToBeProcessed(unProcessedBackups, activityJobAttributes);
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.MO_ACTIVITY_END_PROGRESS, Double.toString(activityProgressPercentage));
            activityUtils.subscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, UploadBackupService.class));
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.STARTED, nodeName, brmBackupMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            actionId = brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupMoFdn, EcimBackupConstants.UPLOAD_BACKUP);
            actionSuccessfullyTriggered = true;
            if (actionId == 0) {
                updateProcessedBackups(activityJobId, backup, unProcessedBackups, activityJobAttributes);
            }
            final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
            if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {

                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            }
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            logger.error("Couldn't trigger upload mo action on node for backup {} because {}", backupName, unsupportedFragmentException.getMessage());
            final Throwable cause = unsupportedFragmentException.getCause();
            final String message = cause != null ? cause.getMessage() : unsupportedFragmentException.getMessage();
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP) + message;
        } catch (final ArgumentBuilderException argumentBuilderException) {
            logger.error("Couldn't trigger upload mo action on node for backup {} because {}", backupName, argumentBuilderException.getMessage());
            final Throwable cause = argumentBuilderException.getCause();
            final String message = cause != null ? cause.getMessage() : argumentBuilderException.getMessage();
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP) + message;
        } catch (final Exception exception) {
            logger.error("Triggering of an Upload action failed on the node for backup {} because {}", backupName, exception.getMessage());
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            exceptionMessage = exceptionMessage == null ? exception.getMessage() : exceptionMessage;
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimBackupConstants.UPLOAD_BACKUP, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP);
            }
        }
        if (jobLogMessage != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        if (actionId != 0) {
            logger.error("Upload Backup Action Trigger failed for backup {} because action ID found to be non-zero. ActionId found : {}", backupName, actionId);
            actionSuccessfullyTriggered = false;
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        final ExecuteResponse executeResponse = new ExecuteResponse(actionSuccessfullyTriggered, brmBackupMoFdn, actionId);
        return executeResponse;
    }

    private String getNotifiableMoFdn(final long activityJobId, final EcimBackupInfo ecimBackupInfo, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList)
            throws UnsupportedFragmentException, MoNotFoundException {
        String brmBackupMoFdn = null;
        final String nodeName = neJobStaticData.getNodeName();
        try {
            brmBackupMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo);
        } catch (final BrmBackupMoNotFoundException ex) {
            logger.warn("Preparing MTR attributes for Upload Backup MoAction with activityJobId {} for node {}. Reason : {}.", activityJobId, nodeName, ex);
            prepareActionMTR(activityJobId, ecimBackupInfo, neJobStaticData, jobLogList, ex.getMessage());
        }
        return brmBackupMoFdn;
    }

    private void doExecutePostValidation(final long activityJobId, final ExecuteResponse executeResponse, final EcimBackupInfo ecimBackupInfo, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList) throws JobDataNotFoundException, MoNotFoundException {
        moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
        final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        final String nodeName = neJobStaticData.getNodeName();
        final String backupName = ecimBackupInfo.getBackupName();
        if (executeResponse.isActionTriggered()) {
            final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getNodeName());
            logger.debug("Upload Backup Activity is triggered on BrmBackup MO {} for backup file {} with activityJobId {}; actionId returned is {}", executeResponse.getFdn(), backupName,
                    activityJobId, executeResponse.getActionId());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_ID, Integer.toString(executeResponse.getActionId()));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));
            final Integer uploadactivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(),
                    EcimBackupConstants.UPLOAD_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, EcimBackupConstants.UPLOAD_BACKUP, uploadactivityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, executeResponse.getFdn(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } else {
            List<String> failedBackupsList = new ArrayList<>();
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.UPLOAD_BACKUP, nodeName);
            activityUtils.unSubscribeToMoNotifications(executeResponse.getFdn(), activityJobId, activityUtils.getActivityInfo(activityJobId, UploadBackupService.class));
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, backupName,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final Map<String, String> failedBackupsMap = ecimBackupUtils.getPropertyValue(Arrays.asList(EcimBackupConstants.FAILED_BACKUPS), activityJobAttributes);
            if (failedBackupsMap == null || failedBackupsMap.isEmpty()) {
                failedBackupsList.add(backupName);
            } else {
                final String existingFailedBackups = failedBackupsMap.get(EcimBackupConstants.FAILED_BACKUPS);
                failedBackupsList = jobConfigurationService.convertStringToList(existingFailedBackups);
                failedBackupsList.add(backupName);
            }
            logger.debug("Failed backups for NodeName {} are {}", nodeName, failedBackupsList);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_FOR_BACKUP, EcimBackupConstants.UPLOAD_BACKUP, backupName), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.FAILED_BACKUPS, failedBackupsList.toString());
            final String unProcessedBackups = getUnProcessedBackups(neJobStaticData, jobLogList);
            updateProcessedBackups(activityJobId, backupName, unProcessedBackups, activityJobAttributes);
            checkAndProceedToNextBackups(activityJobId, jobLogList, jobPropertyList);
        }
    }

    private void checkAndProceedToNextBackups(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList) throws JobDataNotFoundException {
        final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        final boolean allBackupsProcessed = isAllBackupsProcessed(activityJobId);
        if (allBackupsProcessed) {
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), EcimBackupConstants.UPLOAD_BACKUP);
        } else {
            final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(JobResult.FAILED, jobPropertyList, activityJobId);
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            final Map<String, Object> processVariables = new HashMap<>();
            if (repeatRequired) {
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
            }
            if (!repeatRequired) {
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.EXECUTE, processVariables);
        }
    }

    private boolean isFileUploadedToSmrs(final AsyncActionProgress progressReport) {
        if (ActionResultType.SUCCESS == progressReport.getResult()) {
            return true;
        }
        return false;
    }

    private void updateJobLog(final long activityJobId, final String logMessage, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList, final String logLevel) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
    }

    @SuppressWarnings("unchecked")
    private String getBrmBackupNameToBeProcessed(final String backupsToBeProcessed, final Map<String, Object> activityJobAttributes) {
        final int processedBackups = getCountOfProcessedBackups((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        final String[] listOfBackups = backupsToBeProcessed.split(ActivityConstants.COMMA);
        final String backupToBeProcessed = listOfBackups[processedBackups];
        return backupToBeProcessed;
    }

    @SuppressWarnings("unchecked")
    private void updateProcessedBackups(final long activityJobId, final String backupToBeProcessed, final String unProcessedBackups, final Map<String, Object> activityJobAttributes) {
        final int processedBackups = getCountOfProcessedBackups((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.PROCESSED_BACKUPS, Integer.toString(processedBackups + 1));
        activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.CURRENT_BACKUP, backupToBeProcessed);
        activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.TOTAL_BACKUPS, Integer.toString(unProcessedBackups.split(ActivityConstants.COMMA).length));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
    }

    private int getCountOfProcessedBackups(final List<Map<String, String>> jobPropertyList) {
        int processedBackups = 0;
        if (jobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : jobPropertyList) {
                if (BackupActivityConstants.PROCESSED_BACKUPS.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    processedBackups = Integer.parseInt(eachJobProperty.get(ShmConstants.VALUE));
                    break;
                }
            }
        }
        logger.debug("Processed backups count = {}", processedBackups);
        return processedBackups;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        logger.debug("Entering {} cancelTimeout() for activityJobId={}", EcimBackupConstants.UPLOAD_BACKUP, activityJobId);
        JobResult jobResult = null;
        AsyncActionProgress progressReport;
        String brmBackupManagerMoFdn = null;

        String nodeName = null;
        try {
            moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
            final NEJobStaticData jobStaticContext = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = jobStaticContext.getNodeName();
            final long mainJobId = jobStaticContext.getMainJobId();

            final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, EcimBackupConstants.UPLOAD_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
            jobLogList.clear();
            final String backup = getBackup(activityJobId);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
            final String backupName = ecimBackupInfo.getBackupName();
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackupInfo);
            progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.BACKUP_CANCEL_ACTION, ecimBackupInfo);
            if (progressReport != null && EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName())) {
                jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, new Date(), EcimBackupConstants.UPLOAD_BACKUP);
            } else {
                progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.UPLOAD_BACKUP, ecimBackupInfo);
                if (progressReport != null && EcimBackupConstants.BACKUP_EXPORT_ACTION.equals(progressReport.getActionName())) {
                    jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, new Date(), EcimBackupConstants.UPLOAD_BACKUP, backupName);
                } else {
                    logger.warn("Discarding invalid notification,for the activityJobId {} ", activityJobId);
                }
            }
            if (jobResult != null) {
                final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, activityJobId);
                final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
                if (isActivitySuccess || jobResult == JobResult.SKIPPED) {
                    systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                            activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                } else if (jobResult == JobResult.FAILED || jobResult == JobResult.CANCELLED) {
                    systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                            activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                }
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, UploadBackupService.class));
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.UPLOAD_BACKUP, nodeName);
                jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            } else {
                if (finalizeResult) {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, EcimBackupConstants.BACKUP_CANCEL_ACTION), new Date(),
                            JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                } else {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
                }
                jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            }
            return activityStepResult;
        } catch (final MoNotFoundException e) {
            logger.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node:{}. Exception is:{}", nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            logger.error("Un supported fragment during time out evaluation of activity completion for the node:{}. Exception is:{}", nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException ex) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // TODO Auto-generated method stub
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        logger.debug("Inside ecim UploadBackupService.asyncHandleTimeout with activityJobId: {}", activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final ActivityStepResultEnum activityStepResultEnum = processTimeout(activityJobId);
            logger.info("Sending back ActivityStepResult to WorkFlow from ecim UploadBackupService.asyncHandleTimeout with result:{} for node {} with activityJobId {} and neJobId {}",
                    activityStepResultEnum, neJobStaticData.getNodeName(), activityJobId, neJobStaticData.getNeJobId());
            activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.UPLOAD_BACKUP, activityStepResultEnum);
        } catch (final Exception e) {
            logger.error("An exception occurred while processing ecim UploadBackupService.asyncHandleTimeout  with activityJobId: {}. Failure reason:{} ", activityJobId, e);
        }
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, EcimBackupConstants.UPLOAD_BACKUP);
    }

    /**
     * Calculates the upload backup activity progress percentage based on total number of backups.;
     *
     * @param activityJobId
     * @param progressReport
     * @return
     */
    private double calculateActivityProgressPercentage(final long activityJobId, final AsyncActionProgress progressReport) {
        final double totalActivityProgressPercentage = ecimCommonUtils.calculateActivityProgressPercentage(activityJobId, progressReport);
        return totalActivityProgressPercentage;
    }

    @Override
    @Asynchronous
    public void subscribeForPolling(final long activityJobId) {
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        try {
            final boolean isDpsAvailable = isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
            if (isDpsAvailable) {
                final String backup = getBackup(activityJobId);
                final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);
                final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
                final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName());
                final String brmBackupMoFdn = getBrmBackupMoFdn(ecimBackupInfo, neJobStaticData, activityJobId);

                if (brmBackupMoFdn != null) {
                    final List<String> moAttributes = Arrays.asList(EcimCommonConstants.ReportProgress.ASYNC_ACTION_PROGRESS);
                    pollingActivityManager.subscribe(jobActivityInfo, networkElementData, FragmentType.ECIM_BRM_TYPE.getFragmentName(), brmBackupMoFdn, moAttributes);
                    logger.debug("Polling subscription started for node {} in UploadBackupService with activityJobId {}", neJobStaticData.getNodeName(), activityJobId);
                }
            }
        } catch (final Exception ex) {
            logger.error("UploadBackupService-subscribeForPolling-Unable to subscribe for polling for activityJobId: {} .Reason:  ", activityJobId, ex);
            isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
        }
    }

    private String getBrmBackupMoFdn(final EcimBackupInfo ecimBackupInfo, final NEJobStaticData neJobStaticData, final long activityJobId) throws MoNotFoundException, UnsupportedFragmentException {
        String brmBackupMoFdn = null;
        try {
            brmBackupMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.UPLOAD_BACKUP, neJobStaticData.getNodeName(), ecimBackupInfo);
        } catch (final MoNotFoundException ex) {
            logger.error("Failed to get BrmBackup MO FDN for backupName:{} as BrmBackup MO does not exist. Reason: {}. Hence getting MO FDN from job configuration.", ecimBackupInfo.getBackupName(),
                    ex);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final Map<String, String> jobProperties = ecimBackupUtils.getPropertyValue(Arrays.asList(ShmConstants.FDN), activityJobAttributes);
            brmBackupMoFdn = jobProperties.get(ShmConstants.FDN);
        }
        return brmBackupMoFdn;
    }

    private boolean isDpsAvailable(final boolean isDataBaseAvaialble, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        if (!isDataBaseAvaialble) {
            pollingActivityManager.prepareAndAddPollingActivityDataToCache(activityJobId, jobActivityInfo);
            return false;
        }
        return true;
    }

    private boolean prepareActionMTR(final long activityJobId, final EcimBackupInfo ecimBackupInfo, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final String failureReason) {
        final String nodeName = neJobStaticData.getNodeName();
        boolean isPrepareActionMTR = false;
        try {
            int retryCount = 0;
            String brmBackupManagerMoFdn = null;
            final ShmEBMCMoActionData ebmcMoActionData = moActionMTRManager.getMoActionMTRFromCache(activityJobId);
            if (ebmcMoActionData != null) {
                retryCount = (int) ebmcMoActionData.getAdditionalInformation().get(PollingActivityConstants.RETRY_COUNT);
            }
            if (retryCount < dpsRetryConfigurationParamProvider.getdpsMoActionRetryCount()) {
                logger.debug("Preparing MTR attributes for Upload Backup MoAction with activityJobId {} for node {}.", activityJobId, nodeName);
                final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
                Map<String, Object> actionArguments = null;
                if (ebmcMoActionData != null) {
                    brmBackupManagerMoFdn = ebmcMoActionData.getMoFdn();
                    actionArguments = ebmcMoActionData.getMoActionAttributes();
                } else {
                    final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
                    brmBackupManagerMoFdn = brmMoServiceRetryProxy.getBrmBackupManagerMoFdn(networkElementData, nodeName, ecimBackupInfo);
                    actionArguments = brmMoServiceRetryProxy.prepareActionArgumentsForUploadBackup(networkElementData, nodeName);
                }
                final String backupName = ecimBackupInfo.getBackupName();

                moActionMTRManager.prepareAndSendMTRForMoAction(nodeName, brmBackupManagerMoFdn, backupName, ShmConstants.UPLOAD_BACKUP_ACTION, actionArguments, jobActivityInfo);
                isPrepareActionMTR = true;
            } else {
                String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP);
                moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
                logger.error("All retries exhausted, so failing {} activity having activityJobId {} for node {}.", EcimBackupConstants.UPLOAD_BACKUP, activityJobId, nodeName);
                if (failureReason != null && !failureReason.isEmpty()) {
                    jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, failureReason);
                }
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                return isPrepareActionMTR;
            }
        } catch (final Exception ex) {
            logger.error("UploadBackupService.prepareActionMTR, unable to prepare MTR Attributes for node {} having activityJobId {}. Reason : ", nodeName, activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimBackupConstants.UPLOAD_BACKUP, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), EcimBackupConstants.UPLOAD_BACKUP);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            return isPrepareActionMTR;
        }
        return isPrepareActionMTR;
    }

    @Override
    public void processMoActionResponse(final long activityJobId, final Map<String, Object> actionResponseAttributes) {
        logger.debug("MO Action response received in UploadBackupService.processMoActionResponse for activityJobId {} with responseAttributes as {}", activityJobId, actionResponseAttributes);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        String errorMessage = "";
        boolean isActionTriggered = false;
        int actionId = -1;
        String jobBussinessKey = null;
        try {
            final String moFdn = (String) actionResponseAttributes.get(ShmConstants.FDN);
            final String backupName = (String) actionResponseAttributes.get(PollingActivityConstants.MO_NAME);
            final boolean isActionAlreadyRunningOnTheNode = (boolean) actionResponseAttributes.get(PollingActivityConstants.IS_ACTION_ALREADY_RUNNING);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final Object actionIdFromResponse = actionResponseAttributes.get(ShmConstants.ACTION_RESPONSE);
            actionId = ecimBackupUtils.fetchActionIdFromResponseAttributes(actionIdFromResponse);
            jobBussinessKey = neJobStaticData.getNeJobBusinessKey();
            errorMessage = actionResponseAttributes.get(PollingActivityConstants.ERROR_MESSAGE) == null ? ActivityConstants.EMPTY
                    : (String) actionResponseAttributes.get(PollingActivityConstants.ERROR_MESSAGE);
            final String unProcessedBackups = getUnProcessedBackups(neJobStaticData, jobLogList);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String backup = getBrmBackupNameToBeProcessed(unProcessedBackups, activityJobAttributes);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfoForUpload(backup);

            if (!errorMessage.isEmpty()) {
                if (isMoActionForErrorResponseRetriggered(activityJobId, jobLogList, errorMessage, moFdn, neJobStaticData, ecimBackupInfo)) {
                    return;
                }
            } else if ((isActionAlreadyRunningOnTheNode && actionIdFromResponse == null) || (actionId == 0)) {
                // If action flow found that the action is already running on the node then flow returns isActionTriggeredOnNode = true and actionIdFromResponse = null
                isActionTriggered = true;
                activityUtils.prepareJobPropertyList(jobPropertyList, ShmConstants.FDN, moFdn);
                activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.BACKUP_NAME, backupName);
                activityUtils.subscribeToMoNotifications(moFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, UploadBackupService.class));
                // Need to decide with CNA/PO where to keep this system recorder, in prepareActionMTR or here.
                activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_EXECUTE, backupName, moFdn, "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName());
            } else if (actionIdFromResponse != null && actionId != 0) {
                logger.error("Upload Backup Action Trigger failed for backup {} because action ID found to be non-zero for node {}. ActionId found : {}", backupName, neJobStaticData.getNodeName(),
                        actionId);
                final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
            moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
            final ExecuteResponse executeResponse = new ExecuteResponse(isActionTriggered, moFdn, actionId);

            if (isActionTriggered) {
                updateProcessedBackups(activityJobId, backup, unProcessedBackups, activityJobAttributes);
            }
            doExecutePostValidation(activityJobId, executeResponse, ecimBackupInfo, jobLogList, jobPropertyList);
        } catch (final Exception ex) {
            logger.error("Exception occurred while processing execute response received from the activityJobId : {}. Exception is : ", activityJobId, ex);
            final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.UPLOAD_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, jobBussinessKey, EcimBackupConstants.UPLOAD_BACKUP);
            moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
        }
    }

    private boolean isMoActionForErrorResponseRetriggered(final long activityJobId, final List<Map<String, Object>> jobLogList, final String errorMessage, final String moFdn,
            final NEJobStaticData neJobStaticData, final EcimBackupInfo ecimBackupInfo) {
        if (errorMessage.contains(ACTION_NOT_ALLOWED_EXCEPTION) || errorMessage.contains("Could not find MO ID for the MO")) {
            final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.UPLOAD_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            // TODO, do not fail the activity, activity may contain another backups to upload(manage backup). If it final backup then fail the activity.
            // If it is not final backup then notify wfs with repeat execute.
        } else {
            logger.error("Received error message as : {} for MO Action call with MoFDN : {}", errorMessage, moFdn);
            if (prepareActionMTR(activityJobId, ecimBackupInfo, neJobStaticData, jobLogList, errorMessage)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        logger.debug("Polling response received in UploadBackupService for activityJobId {} with responseAttributes as {}", activityJobId, responseAttributes);
        String nodeName = ActivityConstants.EMPTY;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
            logger.debug("In processPollingResponse, is {} activity already completed : {}", EcimBackupConstants.UPLOAD_BACKUP, isActivityCompleted);
            if (isActivityCompleted) {
                logger.debug("Found UploadBackup activity result already persisted in ActivityJob PO, Assuming activity completed on the node for activityJobId: {}", activityJobId);
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.UPLOAD_BACKUP, nodeName);
                return;
            }
            if (responseAttributes != null && !responseAttributes.isEmpty()) {
                final Map<String, Object> moAttributes = (Map<String, Object>) responseAttributes.get(ShmConstants.MO_ATTRIBUTES);
                if (moAttributes != null && !moAttributes.isEmpty()) {
                    final Map<String, Object> unprocessedProgressReport = (Map<String, Object>) moAttributes.get(ReportProgress.ASYNC_ACTION_PROGRESS);
                    final AsyncActionProgress progressReport = new AsyncActionProgress(unprocessedProgressReport);
                    final boolean isInValidProgressReport = cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP,
                            EcimBackupConstants.BACKUP_EXPORT_ACTION);
                    if (isInValidProgressReport) {
                        logger.warn("Discarding invalid polling response for activityJobId {} having modifiedAttributes as : {}", activityJobId, moAttributes);
                        return;
                    }
                    final String brmMoFdn = (String) responseAttributes.get(ShmConstants.FDN);
                    final Date responseTime = new Date(System.currentTimeMillis());
                    processProgressReport(activityJobId, progressReport, responseTime, brmMoFdn, true);
                } else {
                    logger.error("Polling responseAttributes is null/empty or does not have expected attributes for activityJobId {} on node {}.", activityJobId, nodeName);
                }
            }
        } catch (final Exception ex) {
            logger.error("Exception occured during processing of polling response for upload backup action on node : {}. Reason : ", nodeName, ex);
        }
    }

    private void persistAndNotifyWFS(final long activityJobId, final String brmBackupMoFdn, final JobResult jobResult, final NEJobStaticData neJobStaticData, final AsyncActionProgress progressReport,
            final List<Map<String, Object>> jobLogList) {
        logger.debug("Entered in UploadBackupService.persistAndNotifyWFS with jobResult as : {} for activityJobId {} and node {}", jobResult.getJobResult(), activityJobId,
                neJobStaticData.getNodeName());
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.unSubscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, UploadBackupService.class));
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.UPLOAD_BACKUP, neJobStaticData.getNodeName());
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, activityJobId);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final Map<String, Object> processVariables = new HashMap<>();
        final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        } else {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
            if (isActivitySuccess) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimBackupConstants.UPLOAD_BACKUP), new Date(),
                        JobLogType.NE.toString(), JobLogLevel.INFO.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.UPLOAD_BACKUP), new Date(), JobLogType.NE.toString(),
                        JobLogLevel.INFO.toString());
            }
        }
        final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, (double) progressReport.getProgressPercentage());
        logger.debug("In UploadBackupService.persistAndNotifyWFS and isJobResultPersisted : {}", isJobResultPersisted);
        if (isJobResultPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.EXECUTE, processVariables);
        }
    }

    private void logActivityCompletion(final List<Map<String, Object>> jobLogList, final String moFdn, final CommandPhase commandPhase, final NEJobStaticData neJobStaticData, final long activityJobId,
            final String backupName, final boolean isCompletedThroughPolling) {
        String logMessage = "";
        String completionFlow = "";
        final String nodeName = neJobStaticData.getNodeName();
        if (isCompletedThroughPolling) {
            logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING.substring(0, JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING.length() - 1),
                    EcimBackupConstants.UPLOAD_BACKUP) + " for " + backupName;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
            completionFlow = ActivityConstants.COMPLETED_THROUGH_POLLING;
        } else {
            logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS.substring(0, JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS.length() - 2),
                    EcimBackupConstants.UPLOAD_BACKUP) + " for " + backupName;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
            completionFlow = ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS;
        }
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, EcimBackupConstants.UPLOAD_BACKUP);

        systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, commandPhase, nodeName, moFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        activityUtils.recordEvent(eventName, nodeName, moFdn, "SHM:" + activityJobId + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, completionFlow));
    }

    @Override
    public void repeatExecute(final long activityJobId) {
        execute(activityJobId);
    }
}
