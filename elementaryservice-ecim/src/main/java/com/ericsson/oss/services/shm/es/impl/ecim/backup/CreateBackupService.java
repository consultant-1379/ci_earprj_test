package com.ericsson.oss.services.shm.es.impl.ecim.backup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.ReportProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupPrecheckResponse;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.SecureEcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("ECIM.BACKUP.createbackup")
@ActivityInfo(activityName = "createbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CreateBackupService implements Activity, ActivityCallback, AsynchronousActivity, AsynchronousPollingActivity, PollingCallBack {

    private static final Logger logger = LoggerFactory.getLogger(CreateBackupService.class);

    private static final String BACKUP_FILE_EXIST = "Backup with name \"%s\" already exists on node.";

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

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
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private CheckPeriodicity checkPeriodicity;

    @Inject
    private NeJobStaticDataProvider neJobsStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private JobConfigurationService jobConfigurationService;

    /**
     * Returns the result of the pre-check as "Precheck Failed Skip Execution" if the specified BrmBackupManagerMo is not available from the dps. Returns "Precheck Success Proceed with Execution" if
     * the specified BrmBackupManagerMo is available from the dps.
     * 
     * @param activityJobId
     *            uniquely identifies an activity job
     * @return the result of this step of the activity job
     * @See ActivityStepResult
     */

    @SuppressWarnings("unchecked")
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum preCheckStatus = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, EcimBackupConstants.CREATE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
        } catch (final JobDataNotFoundException ex) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            activityStepResult.setActivityResultEnum(preCheckStatus);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.CREATE_BACKUP, ex.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            logger.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.CREATE_BACKUP, ex.getMessage()));
            return activityStepResult;
        }
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());

        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        String logMessage = null;
        try {
            final String inputVersion = brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName);
            logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion);
            if (logMessage != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        } catch (final MoNotFoundException ex) {
            logger.error("MoNotFoundException occurred ,Reason : {}", ex);
            logMessage = ex.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        } catch (final UnsupportedFragmentException ex) {
            logger.error("UnsupportedFragmentException occurred ,Reason : {} ", ex);
            logMessage = ex.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        try {
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(neJobStaticData);
            String backupName = ecimBackupInfo.getBackupName();
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.MAIN_SCHEDULE);
            final List<Map<String, Object>> schedulePropertiesList = (List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES);
            final boolean isPeriodicJob = checkPeriodicity.isJobPeriodic(schedulePropertiesList);
            if (isPeriodicJob) {
                backupName = getBackupNameForPeriodicJobs(neJobStaticData, backupName, activityJobId, nodeName);
            }
            //Updating NE Job property with backupName
            updateNeJobProperty(neJobStaticData.getNeJobId(), jobConfigServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId()), backupName,
                    ecimBackupInfo.getDomainName() + "/" + ecimBackupInfo.getBackupType());
            logMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, EcimBackupConstants.CREATE_BACKUP);
            preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        } catch (final BackupDataNotFoundException backupDataNotFoundException) {
            logger.error("BackupDataNotFoundException occurred, Reason : {}", backupDataNotFoundException);
            logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.CREATE_BACKUP, backupDataNotFoundException.getMessage());
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        } catch (final Exception exception) {
            logger.error("Exception occurred, Reason : ", exception);
            logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.CREATE_BACKUP, exception.getMessage());
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

        activityStepResult.setActivityResultEnum(preCheckStatus);
        if (activityStepResult.getActivityResultEnum() != ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            logger.debug("Skipping persisting step duration as activity is to be skipped.");
        } else {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        }
        return activityStepResult;
    }

    private String appendTimeStamp(final String backupName) {
        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(EcimBackupConstants.DATE_FORMAT_TOBE_APPENDED_TO_BACKUPNAME);
        final String dateValue = formatter.format(dateTime);
        if (backupName != null) {
            return backupName.concat(ActivityConstants.UNDERSCORE).concat(dateValue);
        } else {
            return dateValue;
        }

    }

    private void updateNeJobProperty(final long neJobId, final EcimBackupInfo ecimBackupInfo) {

        final String brmBackupManagerId = ecimBackupInfo.getDomainName() + "/" + ecimBackupInfo.getBackupType();

        final List<Map<String, Object>> neJobPropertiesList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(neJobPropertiesList, EcimBackupConstants.BRM_BACKUP_NAME, ecimBackupInfo.getBackupName());
        activityUtils.prepareJobPropertyList(neJobPropertiesList, EcimBackupConstants.BRM_BACKUP_MANAGER_ID, brmBackupManagerId);
        activityUtils.prepareJobPropertyList(neJobPropertiesList, EcimBackupConstants.UPLOAD_BACKUP_DETAILS, ecimBackupInfo.getBackupName() + "/" + brmBackupManagerId);
        logger.debug("Updating NE Job property for : {} with attributes {}", neJobId, neJobPropertiesList);
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobPropertiesList, null, null);
    }

    /**
     * Registers the node for AVC Notifications, the createBackup action is then performed on the node. If the action is not triggered successfully on the node we de-register for notifications and
     * save the state of the job as failed.
     * 
     * @param activityJobId
     *            uniquely identifies an activity job
     * @return void
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        logger.debug("Entered into CreatebackupService.execute fort he activityJobId: {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        String neJobBusinessKey = null;
        try {
            neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.CREATE_BACKUP);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), EcimBackupConstants.CREATE_BACKUP);
                return;
            }
            //Initiate
            final String brmVersion = initiateActivityAndReturnBrmVersion(neJobStaticData.getNodeName(), jobLogList);
            //preValidate
            final EcimBackupPrecheckResponse backupPrecheckResponse = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList);
            //performAction
            if (backupPrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                logger.debug("Mo action execution starts for create backup activity with activityJobId: {}", activityJobId);
                final ExecuteResponse executeResponse = performActionOnNode(activityJobId, neJobStaticData, backupPrecheckResponse, jobLogList, brmVersion);
                //post validation
                doExecutePostValidation(activityJobId, executeResponse, backupPrecheckResponse.getNetworkElementData(), neJobStaticData, jobLogList);
            } else {
                final String businessKey = neJobStaticData.getNeJobBusinessKey();
                activityUtils.failActivity(activityJobId, jobLogList, businessKey, EcimBackupConstants.CREATE_BACKUP);
            }
        } catch (final JobDataNotFoundException ex) {
            logger.error("CreateBackupService.execute-Unable to trigger action. Reason: {} ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, EcimBackupConstants.CREATE_BACKUP);
        } catch (final Exception ex) {
            logger.error("CreateBackupService.execute-Unable to trigger action. Reason: ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, ex.getMessage(), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, EcimBackupConstants.CREATE_BACKUP);
        }
    }

    private String initiateActivityAndReturnBrmVersion(final String nodeName, final List<Map<String, Object>> jobLogList) {
        String inputVersion = null;
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, EcimBackupConstants.CREATE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        String logMessage = "";
        try {
            inputVersion = brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName);
            logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion);
        } catch (final MoNotFoundException ex) {
            logger.error("MoNotFoundException occurred ,Reason : {}", ex);
            logMessage = ex.getMessage();
        } catch (final UnsupportedFragmentException ex) {
            logger.error("UnsupportedFragmentException occurred ,Reason : {}", ex);
            logMessage = ex.getMessage();
        }
        if (logMessage != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        return inputVersion;
    }

    @SuppressWarnings("unchecked")
    private EcimBackupPrecheckResponse getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        final String nodeName = neJobStaticData.getNodeName();
        EcimBackupInfo ecimBackupInfo = null;
        NetworkElementData networkElementData = null;
        try {
            networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
            final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            ecimBackupInfo = ecimBackupUtils.getBackupWithAutoGeneratedName(neJobStaticData, jobLogList);
            String backupName = ecimBackupInfo.getBackupName();
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.MAIN_SCHEDULE);
            final List<Map<String, Object>> schedulePropertiesList = (List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES);
            final boolean isPeriodicJob = checkPeriodicity.isJobPeriodic(schedulePropertiesList);
            if (isPeriodicJob) {
                ecimBackupInfo.setBackupName(getBackupNameForPeriodicJobs(neJobStaticData, backupName, activityJobId, nodeName));
            }
            updateNeJobProperty(neJobStaticData.getNeJobId(), ecimBackupInfo);

            final String logMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, EcimBackupConstants.CREATE_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
            return new EcimBackupPrecheckResponse(ecimBackupInfo, networkElementData, ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } catch (final BackupDataNotFoundException backupDataNotFoundException) {
            logger.error("BackupDataNotFoundException occurred, Reason : {}", backupDataNotFoundException);
            final String logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.CREATE_BACKUP, backupDataNotFoundException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            return new EcimBackupPrecheckResponse(ecimBackupInfo, networkElementData, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        } catch (final Exception ex) {
            final String logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.CREATE_BACKUP, ex.getMessage());
            logger.error("Exception occurred during create backup service pre-validation, Reason : ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            return new EcimBackupPrecheckResponse(ecimBackupInfo, networkElementData, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
    }

    private String getBackupNameForPeriodicJobs(final NEJobStaticData neJobStaticData, String backupName, final long activityJobId, final String nodeName)
            throws MoNotFoundException, JobDataNotFoundException {
        final String inputBackupName = ecimBackupUtils.getInputBackupName(neJobStaticData);
        logger.debug("inputBackupName for ecim nodes in getPrecheckResponse {} for activityJobId {} and nodeName {} ", inputBackupName, activityJobId, nodeName);
        if (inputBackupName != null && !inputBackupName.contains(ShmConstants.TIMESTAMP_PLACEHOLDER)) {
            backupName = appendTimeStamp(backupName);
        }
        return backupName;
    }

    private void doExecutePostValidation(final long activityJobId, final ExecuteResponse executeResponse, final NetworkElementData networkElement, final NEJobStaticData neJobStaticData,
            final List<Map<String, Object>> jobLogList) {
        if (executeResponse.isActionTriggered()) {
            logger.debug("Create Backup Activity is triggered on BrmBackupManager MO {} with activityJobId {}", executeResponse.getFdn(), activityJobId);
            final String neType = networkElement.getNeType();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), EcimBackupConstants.CREATE_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, EcimBackupConstants.CREATE_BACKUP, activityTimeout), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(System.currentTimeMillis()));

            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, networkElement.getNeFdn(), executeResponse.getFdn(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } else {
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.CREATE_BACKUP, neJobStaticData.getNodeName());
            activityUtils.unSubscribeToMoNotifications(executeResponse.getFdn(), activityJobId, getActivityInfo(activityJobId));
            final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.CREATE_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, networkElement.getNeFdn(), executeResponse.getFdn(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            final String businessKey = neJobStaticData.getNeJobBusinessKey();
            activityUtils.failActivity(activityJobId, jobLogList, businessKey, EcimBackupConstants.CREATE_BACKUP);
        }
    }

    /**
     * Processes the AVC Notification sent when there has been an update to the attribute progressReport on the node. If the job is finished and is successful we deregister from notifications and save
     * the state of the job as SUCCESS. If the job is finished and is failed we deregister from notifications and save the state of the job as FAILED.
     * 
     * @param notification
     *            avc notification
     * @return void
     */
    @Override
    public void processNotification(final Notification notification) {
        logger.debug("Entered ECIM -create backup - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            logger.debug("ECIM - Createbackup - Discarding non-AVC notification.");
            return;
        }
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        logger.debug("modifiedAttributes in processNotification for activity {} : {}", activityJobId, modifiedAttributes);
        String nodeName = null;
        try {
            final NEJobStaticData neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.CREATE_BACKUP, modifiedAttributes);

            final String backupName = getBackupName(neJobStaticData);

            final boolean isValidProgressReport = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                    EcimBackupConstants.BACKUP_CREATE_ACTION, backupName);
            if (!isValidProgressReport) {
                logger.warn("Discarding invalid notification,for backup name {} , activityJobId {}, modifiedAttributes {} and progressReport {}", backupName, activityJobId, modifiedAttributes,
                        progressReport);
                return;
            }
            final String brmMoFdn = ((FdnNotificationSubject) notification.getNotificationSubject()).getFdn();
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
            processProgressReport(activityJobId, progressReport, notificationTime, brmMoFdn, false);
            return;
        } catch (final BackupDataNotFoundException ex) {
            logger.error("BackupDataNotFoundException occurred. Reason : {} ", ex);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, EcimBackupConstants.CREATE_BACKUP, ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final MoNotFoundException e) {
            logger.error("BrmBackupManagerMo not found for corresponding notification. Reason : {} ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            logger.error("Unsupported fragment for the corresponding notification recieved. Reason : {} ", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException ex) {
            logger.error("ProcessNotification.JobDataNotFoundException occurred. Reason : {} ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception e) {
            logger.error("Exception occured during processing of notifications for create backup action with node name:{} Reason : ", nodeName, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String errorMessage = "";
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = e.getMessage();
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Notification processing failed " + JobLogConstants.FAILURE_DUE_TO_EXCEPTION + errorMessage, new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    private String getBackupName(final NEJobStaticData neJobStaticData) {
        final Map<String, Object> neJobAttributes = jobConfigServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        final List<String> keyList = Arrays.asList(EcimBackupConstants.BRM_BACKUP_NAME);
        final Map<String, String> backupManagerDetails = ecimBackupUtils.getPropertyValue(keyList, neJobAttributes);
        return backupManagerDetails.get(EcimBackupConstants.BRM_BACKUP_NAME);
    }

    private void processProgressReport(final long activityJobId, final AsyncActionProgress progressReport, final Date receivingTime, final String brmMoFdn, final boolean isCompletedThroughPolling)
            throws JobDataNotFoundException, MoNotFoundException {
        JobResult jobResult = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final NEJobStaticData neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        if (EcimBackupConstants.BACKUP_CANCEL_ACTION.equalsIgnoreCase(progressReport.getActionName())) {
            jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, receivingTime, EcimBackupConstants.CREATE_BACKUP);
        } else if (EcimBackupConstants.BACKUP_CREATE_ACTION.equalsIgnoreCase(progressReport.getActionName())
                || EcimBackupConstants.BACKUP_CREATE_ACTION_BSP.equalsIgnoreCase(progressReport.getActionName())) {
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(neJobStaticData);
            jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, receivingTime, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo.getBackupName());
        }
        if (jobResult != null) {
            activityUtils.unSubscribeToMoNotifications(brmMoFdn, activityJobId, getActivityInfo(activityJobId));
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.CREATE_BACKUP, neJobStaticData.getNodeName());
            if (jobResult == JobResult.SUCCESS || jobResult == JobResult.CANCELLED) {
                logActivityCompletion(jobLogList, brmMoFdn, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData, activityJobId, isCompletedThroughPolling);
            } else if (jobResult == JobResult.FAILED) {
                logActivityCompletion(jobLogList, brmMoFdn, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData, activityJobId, isCompletedThroughPolling);
            }
            persistAndNotifyWFS(activityJobId, jobResult, neJobStaticData, progressReport, jobLogList);
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, (double) progressReport.getProgressPercentage());
            progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
        }
    }

    /**
     * If no notification is received, the WFS calls this method handleTimeout We de-register from notifications as these will not be needed after the timeout has been handled. It checks to see if the
     * correct BrmBackupMo exists in the system by using the CV_NAME to check the DPS. If the BrmBackupMo exists and the status is BRM_BACKUP_COMPLETE the jobResult is SUCCESS and ActivityStepResult
     * TIMEOUT_RESULT_SUCCESS is returned. If the BrmBackupMo does not exist the jobResult is saved as FAILED and ActivityStepResult TIMEOUT_RESULT_FAILURE is returned.
     * 
     * @param activityJobId
     *            uniquely identifies an activityJob
     * @return the result of this step of the activity job
     * @See ActivityStepResult
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResultEnum activityStepResultEnum = processTimeout(activityJobId);
        return activityUtils.getActivityStepResult(activityStepResultEnum);

    }

    private ActivityStepResultEnum processTimeout(final long activityJobId) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        String brmBackupManagerMoFdn = null;
        String jobLogMessage = null;
        String resultInfo = "";

        String nodeName = null;
        JobResult jobResult = JobResult.FAILED;
        final AsyncActionProgress progressReport;
        long activityStartTime = 0; //do not persist in case of JobDataNotFoundException.
        try {
            final NEJobStaticData jobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = jobStaticData.getNodeName();
            activityStartTime = jobStaticData.getActivityStartTime();
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(jobStaticData);
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo);
            final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
            notificationRegistry.removeSubject(fdnNotificationSubject);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.CREATE_BACKUP, nodeName);
            final String logMessage = "Create Backup has Timed Out.";
            activityUtils.recordEvent(SHMEvents.CREATE_BACKUP_TIME_OUT, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

            final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
            final Boolean isActivityTriggered = Boolean.valueOf(activityUtils.getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.IS_ACTIVITY_TRIGGERED));
            if (isActivityTriggered != null && isActivityTriggered) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, EcimBackupConstants.CREATE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                progressReport = brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(nodeName, ecimBackupInfo);
                resultInfo = progressReport.getResultInfo();
                jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, new Date(), EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo.getBackupName());
                if (jobResult == JobResult.SUCCESS) {
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FOR_BACKUP_COMPLETED_SUCCESSFULLY, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo.getBackupName());
                    activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
                } else if (jobResult == JobResult.FAILED) {
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, EcimBackupConstants.CREATE_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, resultInfo);
                } else {
                    jobResult = JobResult.FAILED;
                    jobLogMessage = String.format(JobLogConstants.STILL_RUNNING, EcimBackupConstants.CREATE_BACKUP);
                }
                activityUtils.recordEvent(SHMEvents.CREATE_BACKUP_TIME_OUT, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
            } else {
                jobResult = JobResult.FAILED;
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGERED_FAILED, EcimBackupConstants.CREATE_BACKUP);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            return activityStepResultEnum;
        } catch (final BackupDataNotFoundException backupDataNotFoundException) {
            logger.error("BackupDataNotFoundException occurred. Reason : {} ", backupDataNotFoundException);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, EcimBackupConstants.CREATE_BACKUP, backupDataNotFoundException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final MoNotFoundException e) {
            logger.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node: {}. Exception is: {} ", nodeName, e);
            jobLogMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            logger.error("Unsupported fragment during time out evaluation of activity completion for the node: {}.Exception is: {} ", nodeName, e);
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException ex) {
            logger.error("ProcessTimeout.JobDataNotFoundException occurred. Reason : {} ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            logger.error("Exception occured in handleTimeout while evaluating the create backup status. Reason : ", ex);
            final String logMessage = String.format(JobLogConstants.STATUS_EVALUATION_FAILED, EcimBackupConstants.CREATE_BACKUP, ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return activityStepResultEnum;
    }

    private ExecuteResponse performActionOnNode(final long activityJobId, final NEJobStaticData neJobStaticData, final EcimBackupPrecheckResponse precheckResponse,
            final List<Map<String, Object>> jobLogList, final String brmVersion) {
        boolean isActionTriggered = false;
        int actionInvocationResult = -1;
        String brmBackupManagerMoFdn = "";
        String backupName = "";
        String password = null;
        try {
            logger.debug("Mo action execution starts for create backup activity with activityJobId: {}", activityJobId);
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            final String nodeName = neJobStaticData.getNodeName();
            final EcimBackupInfo ecimBackupInfo = precheckResponse.getEcimBackupInfo();
            backupName = ecimBackupInfo.getBackupName();
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.recordEvent(SHMEvents.CREATE_BACKUP_EXECUTE, nodeName, brmBackupManagerMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            activityUtils.subscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
            if (ecimBackupInfo instanceof SecureEcimBackupInfo) {
                password = ((SecureEcimBackupInfo) ecimBackupInfo).getPassword();
            }
            logger.debug("nodeName {} brmversion :{} and password :{}", nodeName, brmVersion, password);

            if (password != null && !password.isEmpty() && getValidFormatOfBrmVersion(brmVersion, nodeName) >= ShmConstants.BKPENCRYPTION_SUPPORT_STARTING_VERSION) {
                actionInvocationResult = brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupManagerMoFdn, EcimBackupConstants.CREATE_SECURE_BACKUP_MOACTION);
            } else {
                actionInvocationResult = brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupManagerMoFdn, EcimBackupConstants.CREATE_BACKUP);
            }
            if (actionInvocationResult == 0) {
                isActionTriggered = true;
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            }
        } catch (final UnsupportedFragmentException ex) {
            logger.error("UnsupportedFragmentException occurred. Reason : {}", ex);
            final String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            final String jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.CREATE_BACKUP) + message;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            logger.error("Exception occurred. Reason : ", exception);
            String jobLogMessage = null;
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                //Checking if the backup name already exists on the node
                if (exceptionMessage.contains("mo_already_defined")) {
                    jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.CREATE_BACKUP)
                            + String.format(JobLogConstants.FAILURE_REASON, String.format(BACKUP_FILE_EXIST, backupName));
                } else {
                    jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.CREATE_BACKUP) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
                }
            } else {
                exceptionMessage = exceptionMessage.isEmpty() ? exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage() : exceptionMessage;
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, EcimBackupConstants.CREATE_BACKUP) + exceptionMessage;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return new ExecuteResponse(isActionTriggered, brmBackupManagerMoFdn, actionInvocationResult);
    }
    
    private Double getValidFormatOfBrmVersion(String neBrmVersion, final String nodeName) {
        Double brmFragmentVersion = Double.valueOf(0.0D);
        try {
            if (neBrmVersion != null && !neBrmVersion.isEmpty()) {
                brmFragmentVersion = Double.valueOf(neBrmVersion.substring(0, neBrmVersion.indexOf('.') + 2));
            }
        } catch (NumberFormatException e) {
            logger.error("Received BrM version in unexpected format {} for node {}", neBrmVersion, nodeName);
        }
        return brmFragmentVersion;
    }
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        EcimBackupInfo ecimBackupInfo = null;
        try {
            final NEJobStaticData jobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            ecimBackupInfo = ecimBackupUtils.getBackup(jobStaticData);
        } catch (final BackupDataNotFoundException backupDataNotFoundException) {
            logger.error("Exception while fetching Backup Information during Create backup activity with activityJob: {}. Reason : {} ", activityJobId, backupDataNotFoundException);
        } catch (final MoNotFoundException e) {
            logger.error("Exception while fetching Backup Information during Create backup activity with activityJob: {}. Reason : {} ", activityJobId, e);
        } catch (final JobDataNotFoundException ex) {
            logger.error("Exception while fetching NE Job static Data during Create backup activity with activityJob: {}. Reason : {} ", activityJobId, ex);
        }
        return cancelBackupService.cancel(activityJobId, EcimBackupConstants.CREATE_BACKUP, ecimBackupInfo);
    }

    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, CreateBackupService.class);
    }

    /**
     * If no notification is received after cancel is triggered, the WFS calls this method cancelTimeout. We de-register from notifications as these will not be needed after the timeout has been
     * handled. It checks to see if the correct BrmBackupMo exists in the system by using the CV_NAME to check the DPS. If the BrmBackupMo exists and the status is BRM_BACKUP_COMPLETE the jobResult is
     * SUCCESS and ActivityStepResult TIMEOUT_RESULT_SUCCESS is returned. If the BrmBackupMo does not exist we check the BrmBackupMo ProgressReport on the node, based on the Result and State we update
     * the Job Result and return ActivityStepResult.
     * 
     * @param activityJobId
     *            uniquely identifies an activityJob
     * @param finalizeResult
     * @return the result of this step of the activity job
     * @See ActivityStepResult
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        logger.debug("Entering {} cancelTimeout() for activityJobId={}", EcimBackupConstants.CREATE_BACKUP, activityJobId);
        JobResult jobResult = null;
        boolean isBackupExists = false;
        AsyncActionProgress progressReport;
        String brmBackupManagerMoFdn = null;

        String nodeName = null;
        long mainJobId = 0;
        NEJobStaticData jobStaticData = null;
        try {
            jobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = jobStaticData.getNodeName();
            mainJobId = jobStaticData.getMainJobId();

            final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, EcimBackupConstants.CREATE_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
            jobLogList.clear();
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(jobStaticData);
            isBackupExists = brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo);
        } catch (final BackupDataNotFoundException backupDataNotFoundException) {
            logger.error("Exception while fetching Backup Information during Create backup activity with activityJob: {}. Reason : {} ", activityJobId, backupDataNotFoundException);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final MoNotFoundException e) {
            logger.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node: {}, Exception : {} ", nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format("BrmBackupManager MO not found for the node \"%s\", Verifying the ProgessReport", nodeName), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            logger.error("Unsupported fragment during time out evaluation of activity completion for the node:{}. Exception : {} ", nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException ex) {
            logger.error("CancelTimeout.JobDataNotFoundException occurred. Reason : {} ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            return activityStepResult;
        }
        try {
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(jobStaticData);
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo);
            if (isBackupExists) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimBackupConstants.CREATE_BACKUP), new Date(),
                        JobLogType.NE.toString(), JobLogLevel.INFO.toString());
                jobResult = JobResult.SUCCESS;
            } else {
                logger.info("Backup doesnot exists on the node hence verifying the ProgessReport, NodeName: {}", nodeName);
                progressReport = brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(nodeName, ecimBackupInfo);

                final boolean progressReportflag = cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                        EcimBackupConstants.BACKUP_CREATE_ACTION);
                if (progressReportflag) {
                    logger.warn("Discarding invalid notification,for the activityJobId {}", activityJobId);
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                    return activityStepResult;
                }
                jobResult = evaluateJobResult(activityJobId, jobLogList, jobResult, progressReport, ecimBackupInfo.getBackupName());
            }
            if (jobResult != null) {
                setActivityResult(activityJobId, activityStepResult, jobResult, brmBackupManagerMoFdn, nodeName, mainJobId);
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.CREATE_BACKUP, nodeName);
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
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
            logger.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node:{}.Exception : {} ", nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } catch (final UnsupportedFragmentException e) {
            logger.error("Un supported fragment during time out evaluation of activity completion for the node: {}.Exception : {} ", nodeName, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException ex) {
            logger.error("JobDataNotFoundException occurred. Reason : {} ", ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    private JobResult evaluateJobResult(final long activityJobId, final List<Map<String, Object>> jobLogList, JobResult jobResult, final AsyncActionProgress progressReport, final String backupName) {
        if (EcimBackupConstants.BACKUP_CANCEL_ACTION.equals(progressReport.getActionName())) {
            jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, new Date(), EcimBackupConstants.CREATE_BACKUP);
        } else if (EcimBackupConstants.BACKUP_CREATE_ACTION.equals(progressReport.getActionName())) {
            jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, new Date(), EcimBackupConstants.CREATE_BACKUP, backupName);
        }
        return jobResult;
    }

    private void setActivityResult(final long activityJobId, final ActivityStepResult activityStepResult, final JobResult jobResult, final String brmBackupManagerMoFdn, final String nodeName,
            final long mainJobId) {
        if (jobResult == JobResult.SUCCESS || jobResult == JobResult.SKIPPED) {
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else if (jobResult == JobResult.FAILED) {
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else if (jobResult == JobResult.CANCELLED) {
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
    }

    private void updateNeJobProperty(final long neJobId, final Map<String, Object> neJobAttributes, final String backupName, final String brmBackupManagerId) {
        logger.debug("Updating NE Job property for : {} with attributes {}", neJobId, neJobAttributes);
        final List<Map<String, Object>> neJobPropertiesList = new ArrayList<Map<String, Object>>();

        activityUtils.prepareJobPropertyList(neJobPropertiesList, EcimBackupConstants.BRM_BACKUP_NAME, backupName);
        activityUtils.prepareJobPropertyList(neJobPropertiesList, EcimBackupConstants.BRM_BACKUP_MANAGER_ID, brmBackupManagerId);
        activityUtils.prepareJobPropertyList(neJobPropertiesList, EcimBackupConstants.UPLOAD_BACKUP_DETAILS, backupName + "/" + brmBackupManagerId);

        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertiesList);
        logger.debug("Updating NE Job property {} exit", neJobPropertiesList);
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobPropertiesList, null, null);
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        //Maintaining this method for backward compatibility. Implemented this method in Upgrade jobs.
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        //Maintaining this method for backward compatibility. Currently in Upgrade job, provided implementation for this method.
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        logger.debug("Inside  ecim CreateBackupService.asyncHandleTimeout with activityJobId: {}", activityJobId);
        String nodeName = null;
        try {
            final NEJobStaticData neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final ActivityStepResultEnum activityStepResultEnum = processTimeout(activityJobId);
            logger.info("Sending back ActivityStepResult to WorkFlow from ecim CreateBackupService.asyncHandleTimeout with result:{} for node {} with activityJobId {} and neJobId {}",
                    activityStepResultEnum, nodeName, activityJobId, neJobStaticData.getNeJobId());
            activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.CREATE_BACKUP, activityStepResultEnum);
        } catch (final Exception e) {
            logger.error("An exception occurred while processing ecim CreateBackupService.asyncHandleTimeout  with activityJobId: {} and nodeName: {}. Failure reason: ", activityJobId, nodeName, e);
        }
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, EcimBackupConstants.CREATE_BACKUP);
    }

    @Override
    @Asynchronous
    public void subscribeForPolling(final long activityJobId) {
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        try {
            final boolean isDpsAvailable = isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
            if (isDpsAvailable) {
                final NEJobStaticData neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
                final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackup(neJobStaticData);
                final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName());
                final String brmBackupMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, neJobStaticData.getNodeName(), ecimBackupInfo);
                final List<String> moAttributes = Arrays.asList(EcimCommonConstants.ReportProgress.ASYNC_ACTION_PROGRESS);
                pollingActivityManager.subscribe(jobActivityInfo, networkElementData, FragmentType.ECIM_BRM_TYPE.getFragmentName(), brmBackupMoFdn, moAttributes);
                logger.debug("Polling subscription started for node {} in CreateBackupService with activityJobId {}", neJobStaticData.getNodeName(), activityJobId);
            }
        } catch (final RuntimeException ex) {
            logger.error("CreateBackupService-subscribeForPolling-Unable to subscribe for polling for activityJobId: {} .Reason:  ", activityJobId, ex);
            isDpsAvailable(dpsStatusInfoProvider.isDatabaseAvailable(), activityJobId, jobActivityInfo);
        } catch (final Exception ex) {
            logger.error("CreateBackupService-subscribeForPolling-Unable to subscribe for polling for activityJobId: {} .Reason:  ", activityJobId, ex);
        }
    }

    private boolean isDpsAvailable(final boolean isDataBaseAvaialble, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        if (!isDataBaseAvaialble) {
            pollingActivityManager.prepareAndAddPollingActivityDataToCache(activityJobId, jobActivityInfo);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes) {
        logger.debug("Polling response received in CreateBackupService for activityJobId {} with responseAttributes as {}", activityJobId, responseAttributes);
        String nodeName = ActivityConstants.EMPTY;
        try {
            final NEJobStaticData neJobStaticData = neJobsStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
            logger.debug("In processPollingResponse, is {} activity already completed : {}", ActivityConstants.CREATE_BACKUP, isActivityCompleted);
            if (isActivityCompleted) {
                logger.debug("Found CreateBackup activity result already persisted in ActivityJob PO, assuming activity completed on the node {} having activityJobId as : {}", nodeName,
                        activityJobId);
                pollingActivityManager.unsubscribeByActivityJobId(activityJobId, EcimBackupConstants.CREATE_BACKUP, nodeName);
                return;
            }
            if (responseAttributes != null && !responseAttributes.isEmpty()) {
                final Map<String, Object> moAttributes = (Map<String, Object>) responseAttributes.get(ShmConstants.MO_ATTRIBUTES);
                if (moAttributes != null && !moAttributes.isEmpty()) {
                    final Map<String, Object> unprocessedProgressReport = (Map<String, Object>) moAttributes.get(ReportProgress.ASYNC_ACTION_PROGRESS);
                    final AsyncActionProgress progressReport = new AsyncActionProgress(unprocessedProgressReport);
                    final boolean isInValidProgressReport = cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                            EcimBackupConstants.BACKUP_CREATE_ACTION);
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
            logger.error("Exception occured during processing of polling response for create backup action on node : {}. Reason : ", nodeName, ex);
        }
    }

    private void persistAndNotifyWFS(final long activityJobId, final JobResult jobResult, final NEJobStaticData neJobStaticData, final AsyncActionProgress progressReport,
            final List<Map<String, Object>> jobLogList) {
        final String nodeName = neJobStaticData.getNodeName();
        logger.debug("Entered in persistAndNotifyWFS with jobResult as : {} for activityJobId {} and node {}", jobResult.getJobResult(), activityJobId, nodeName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, (double) progressReport.getProgressPercentage());
        logger.debug("In persistAndNotifyWFS and isJobResultPersisted : {}", isJobResultPersisted);
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
        if (isJobResultPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.EXECUTE, null);
        }
    }

    private void logActivityCompletion(final List<Map<String, Object>> jobLogList, final String moFdn, final CommandPhase commandPhase, final NEJobStaticData neJobStaticData, final long activityJobId,
            final boolean isCompletedThroughPolling) {
        String logMessage = "";
        String completionFlow = "";
        final String nodeName = neJobStaticData.getNodeName();
        if (isCompletedThroughPolling) {
            logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_POLLING, EcimBackupConstants.CREATE_BACKUP);
            completionFlow = ActivityConstants.COMPLETED_THROUGH_POLLING;
        } else {
            logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS, EcimBackupConstants.CREATE_BACKUP);
            completionFlow = ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS;
        }
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, ActivityConstants.CREATE_BACKUP);
        systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, commandPhase, nodeName, moFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        activityUtils.recordEvent(eventName, nodeName, moFdn, "SHM:" + activityJobId + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, completionFlow));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.DEBUG.toString());
    }

}
