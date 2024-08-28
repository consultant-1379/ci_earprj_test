package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNAUTHORIZED_USER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.BackupSmrs;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class BackupServiceTest {

    public static final long ACTIVITY_JOB_ID = 123;
    public static final String NODE_NAME = "ML-TN";
    public static final int FTP_TABLE_ENTRY_INDEX = 15;
    public static final int FTP_ENTRY_INDEX = 1;
    public static final String PATH_ON_SERVER = "pathOnServer";
    public static final String BACKUP_NAME_TEST = "backup";
    public static final String MINI_LINK_OUTDOOR = "MINI_LINK_OUTDOOR";
    public static final String CONFIG_FILE_EXTENSION = "cdb";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";
    public static final String EXPECTED_BACKUP_FILE_NAME = BACKUP_NAME_TEST + UNDERSCORE + NODE_NAME + DOT + CONFIG_FILE_EXTENSION;
    public static final String BACKUP_FILE_PATH_PREFIX = MINI_LINK_OUTDOOR + SLASH + EXPECTED_BACKUP_FILE_NAME;

    @InjectMocks
    private BackupService backupService;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private ManagedObject managedObject;

    @Mock
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Mock
    private BackupActivityProperties backupActivityProperties;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private DPSUtils dpsUtils;

    @Mock
    private SmrsAccountInfo smrsAccountInfo;

    @Mock
    private BackupSmrs backupSmrs;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private SmrsFileStoreService smrsFileStoreService;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private EventSender<MediationTaskRequest> backupJobRequest;

    @Mock
    private SHMCommonCallBackNotificationJobProgressBean notification;

    @Mock
    private SHMCommonCallbackNotification commonNotification;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupServiceTest.class);

    public static final String NETWORKELEMENT = "NetworkElement=";
    public static final String OSSMODELIDENTITY = "ossModelIdentity";
    public static final String MODEL_IDENTITY = "ML-6352";
    private static final Double PERCENT_ZERO = 0.0;
    private static final Double PERCENT_HUNDRED = 100.0;
    private static final String EXCEPTION = "Exception caught :: {}";

    @Test
    public void testPrecheckFailedSkipExecution() {
        try {
            BackupActivityProperties backupActProperties = new BackupActivityProperties(ACTIVITY_JOB_ID, jobEnvironment, BACKUP_NAME_TEST, BACKUP,
                    BackupService.class);
            when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                    backupActProperties);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.BACKUP)).thenReturn(false);
            ActivityStepResult activityStepResult = new ActivityStepResult();
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            when(miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActProperties, UNAUTHORIZED_USER))
                    .thenReturn(activityStepResult);
            final ActivityStepResult precheckResult = backupService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testPrecheckBackupNameDoesNotExist() {
        try {
            when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                    backupActivityProperties);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.BACKUP)).thenReturn(true);
            ActivityStepResult activityStepResult = new ActivityStepResult();
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            when(miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST))
                    .thenReturn(activityStepResult);
            final ActivityStepResult precheckResult = backupService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testPrecheckSucceedsInValidState() {
        try {
            BackupActivityProperties backupActProperties = new BackupActivityProperties(ACTIVITY_JOB_ID, jobEnvironment, BACKUP_NAME_TEST, BACKUP,
                    BackupService.class);
            when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                    backupActProperties);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.BACKUP)).thenReturn(true);
            ActivityStepResult activityStepResult = new ActivityStepResult();
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            when(miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActProperties)).thenReturn(activityStepResult);
            when(backupActProperties.getNodeName()).thenReturn("nodename");
            when(dpsUtils.isInventorySupervisionEnabled(backupActProperties.getNodeName())).thenReturn(true);
            final ActivityStepResult precheckResult = backupService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testPrecheckExceptionHandling() {
        try {
            when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                    backupActivityProperties);
            when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                    backupActivityProperties);
            when(backupActivityProperties.getNodeName()).thenReturn("nodeName");
            when(backupActivityProperties.getActivityName()).thenReturn("activityName");
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.BACKUP)).thenReturn(true);
            ActivityStepResult activityStepResult = new ActivityStepResult();
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            when(miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST)).thenReturn(activityStepResult);
            final ActivityStepResult result = backupService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        } catch (Exception e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testExecute() throws JobDataNotFoundException {
        BackupActivityProperties backupActProperties = new BackupActivityProperties(ACTIVITY_JOB_ID, jobEnvironment, BACKUP_NAME_TEST, BACKUP,
                BackupService.class);
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActProperties);
        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(dpsUtils.getNeType(BACKUP_FILE_PATH_PREFIX)).thenReturn(MINI_LINK_OUTDOOR);
        when(smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, BACKUP_FILE_PATH_PREFIX)).thenReturn(smrsAccountInfo);
        doNothing().when(backupSmrs).prepareBackupDirectory(backupActProperties, smrsAccountInfo);
        when(smrsAccountInfo.getPathOnServer()).thenReturn(PATH_ON_SERVER);
        when(backupActProperties.getBackupFileWithPath()).thenReturn(BACKUP_FILE_PATH_PREFIX);
        when(backupActProperties.getNodeName()).thenReturn(NODE_NAME);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(0L);
        when(activityUtils.additionalInfoForCommand(ACTIVITY_JOB_ID, 0L, JobTypeEnum.BACKUP)).thenReturn("test");
        when(smrsAccountInfo.getServerIpAddress()).thenReturn("ipAddress");
        when(smrsAccountInfo.getUser()).thenReturn("user");
        when(smrsAccountInfo.getPassword()).thenReturn(new char[] { 'p' });
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.STARTED, NODE_NAME, "", "test");
        backupService.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testProcessNotificationSuccess() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(commonNotification.getState()).thenReturn(Constants.COMPLETE);
        when(commonNotification.getProgressPercentage()).thenReturn("100");
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        when(backupActivityProperties.getNodeName()).thenReturn(NODE_NAME);
        when(neJobStaticData.getMainJobId()).thenReturn(0L);
        when(activityUtils.additionalInfoForCommand(ACTIVITY_JOB_ID, 0L, JobTypeEnum.BACKUP)).thenReturn("test");
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, NODE_NAME, "fdn", "test");
        when(dpsUtils.getNeType(backupActivityProperties.getNodeName())).thenReturn(MINI_LINK_OUTDOOR);
        when(smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, backupActivityProperties.getNodeName())).thenReturn(smrsAccountInfo);
        when(backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsAccountInfo)).thenReturn(true);
        backupService.processNotification(notification);
    }

    @Test
    public void testProcessNotificationBackupFileNotCreated() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(commonNotification.getState()).thenReturn(Constants.COMPLETE);
        when(commonNotification.getProgressPercentage()).thenReturn("100");
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, NODE_NAME, "fdn", "test");
        when(dpsUtils.getNeType(backupActivityProperties.getNodeName())).thenReturn(MINI_LINK_OUTDOOR);
        when(smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, backupActivityProperties.getNodeName())).thenReturn(smrsAccountInfo);
        when(backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsAccountInfo)).thenReturn(false);
        backupService.processNotification(notification);
    }

    @Test
    public void testProcessNotificationUploading() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(commonNotification.getState()).thenReturn(Constants.UPLOADING);
        when(commonNotification.getProgressPercentage()).thenReturn("0");
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        when(backupActivityProperties.getNodeName()).thenReturn(NODE_NAME);
        backupService.processNotification(notification);
    }

    @Test
    public void testProcessNotificationActivityResultFailed() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(commonNotification.getState()).thenReturn("");
        when(commonNotification.getProgressPercentage()).thenReturn("0");
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        when(backupActivityProperties.getNodeName()).thenReturn(NODE_NAME);
        when(neJobStaticData.getMainJobId()).thenReturn(0L);
        when(activityUtils.additionalInfoForCommand(ACTIVITY_JOB_ID, 0L, JobTypeEnum.BACKUP)).thenReturn("test");
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, NODE_NAME, "fdn", "test");
        backupService.processNotification(notification);
    }

    @Test
    public void testHandleTimeoutWithException() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)).thenReturn(activityStepResult);
        final ActivityStepResult result = backupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutWithJobSuccess() {
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        when(dpsUtils.getNeType(backupActivityProperties.getNodeName())).thenReturn(MINI_LINK_OUTDOOR);
        when(smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, backupActivityProperties.getNodeName())).thenReturn(smrsAccountInfo);
        when(backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsAccountInfo)).thenReturn(true);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        when(miniLinkOutdoorJobUtil.timeoutSuccess(PERCENT_HUNDRED, backupActivityProperties)).thenReturn(activityStepResult);
        final ActivityStepResult result = backupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutWithJobFail() {
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        when(dpsUtils.getNeType(backupActivityProperties.getNodeName())).thenReturn(MINI_LINK_OUTDOOR);
        when(smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, backupActivityProperties.getNodeName())).thenReturn(smrsAccountInfo);
        when(backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsAccountInfo)).thenReturn(false);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(miniLinkOutdoorJobUtil.timeoutFail(PERCENT_ZERO, backupActivityProperties)).thenReturn(activityStepResult);
        final ActivityStepResult result = backupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @Test
    public void testCancel() {
        when(miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.BACKUP, BackupService.class)).thenReturn(
                backupActivityProperties);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(backupActivityProperties.getNodeName(), BACKUP)).thenReturn("subscriptionKey");
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, BackupService.class)).thenReturn(jobActivityInfo);
        when(activityUtils.unSubscribeToMoNotifications("subscriptionKey", ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        when(activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED)).thenReturn(activityStepResult);
        final ActivityStepResult result = backupService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
    }

    @Test
    public void testCancelTimeout() {
        backupService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }
}
