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
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY;
import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.CancelCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentDetailedActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentMainActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.NodeRestartActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.common.SetStartableActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.CppNodeRestartConfigParamListener;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.NodeRestartConfiguartionParamProvider;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.RestartActivityConstants;
import com.ericsson.oss.services.shm.es.polling.PollingActivityStatusManager;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.es.polling.api.ReadCallStatusEnum;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
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
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.shm.inventory.backup.entities.AdminProductData;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class RestoreServiceTest {

    @InjectMocks
    private RestoreService restoreServiceMock;

    @Mock
    @Inject
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationServiceMock;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    private ActivityUtils activityUtilsMock;

    @Mock
    @Inject
    private SmrsFileStoreService smrsServiceUtilMock;

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistryMock;

    @Mock
    @Inject
    private FdnNotificationSubject fdnNotificationSubjectMock;

    @Mock
    @Inject
    private SystemRecorder systemRecorderMock;

    @Mock
    @Inject
    private CommonCvOperations commonCvOperationsMock;

    @Mock
    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifierMock;

    @Mock
    private ConfigurationVersionUtils configurationVersionUtilsMock;

    @Mock
    private Notification notificationMock;

    @Mock
    @Inject
    private ConfigurationVersionService configurationVersionServiceMock;

    @Mock
    @Inject
    private AbstractBackupActivity abstractBackupActivityMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private Notification notification;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private DpsDataChangedEvent dpsDataChangeEvent;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private ActivityCompleteTimer activityCompleteTimer;

    @Mock
    private CvActivity cvActivity;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private CancelCompleteTimer cancelCompleteTimer;

    @Mock
    private RestorePrecheckHandler restorePrecheckHandler;

    @Mock
    private SetStartableActivityHandler setStartableActivityHandler;

    @Mock
    private NodeRestartConfiguartionParamProvider nodeRestartConfiguartionParamProvider;

    @Mock
    private CppNodeRestartConfigParamListener cppNodeRestartConfigParamListener;

    @Mock
    private NodeRestartActivityHandler nodeRestartActivityHandler;

    @Mock
    protected NodeRestartJobActivityInfo nodeRestartJobActivityInfo;

    @Mock
    private JobActivityInfo activityInfo;

    @Mock
    private PollingActivityStatusManager pollingActivityStatusManager;

    @Mock
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProviderMock;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Mock
    private List<Map<String, Object>> cvMoAttributes;

    long activityJobId = 1243L;
    long neJobId = 1223L;
    long mainJobId = 1253L;
    long templateJobId = 1236L;
    private final Map<String, Object> activityJobAttributes = new HashMap<>();

    int actionId = 5;
    String businessKey = "abc1";

    String identity = "Some Identity";
    String type = "Standard";
    String neName = "Some Ne Name";
    String neType = "ERBS";
    String configurationVersionName = "Some CV Name";
    String cvMoFdn = "Some Cv Mo Fdn";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    String pathOnFtpServer = "Some Path";
    String ftpServerIpAddress = "Some IP";
    String ftpServerUserId = "Some User";
    char[] ftpServerPassword = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
    String cvBackupNameOnFtpServer = "Some CV Name on FTP Server";
    private static final String RESTORE_ACTIVITY_NAME = "restore";
    private String jobExecutionUser = "TEST_USER";

    @Test
    public void testPrecheckWithPass() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.when(restorePrecheckHandler.getRestorePrecheckResult(11111111l, ActivityConstants.RESTORE, RESTORE_ACTIVITY_NAME)).thenReturn(activityStepResult);
        final ActivityStepResult activityStepResultOutput = restoreServiceMock.precheck(11111111l);
        assertTrue(activityStepResultOutput.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
    }

    @Test
    public void testAsyncPrecheckWithPass() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();

        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.when(restorePrecheckHandler.getRestorePrecheckResult(11111111l, ActivityConstants.RESTORE, "restore")).thenReturn(activityStepResult);
        Mockito.when(activityUtilsMock.getJobEnvironment(anyLong())).thenReturn(jobEnvironment);
        restoreServiceMock.asyncPrecheck(11111111l);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(jobEnvironment, ActivityConstants.RESTORE, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void testExecute() throws JobDataNotFoundException {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        mockJobActivityInfo();
        setConfigVersionMo();
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);

        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtilsMock.getPoAttributes(activityJobId)).thenReturn(activityMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(notificationRegistryMock).register(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        when((abstractBackupActivityMock).persistActivityJobAttributes(Matchers.anyLong(), Matchers.anyMap(), Matchers.anyMap(), Matchers.anyList())).thenReturn(true);
        final Map<String, Object> actionArguments = new HashMap<>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, configurationVersionName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvBackupNameOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
        when(commonCvOperationsMock.executeActionOnMo("forcedRestore", cvMoFdn, actionArguments)).thenReturn(actionId);
        Mockito.when(activityUtilsMock.getActivityInfo(activityJobId, RestoreService.class)).thenReturn(jobActivityInfo);
        final Map<String, String> forcedRestoreMap = new HashMap<>();
        forcedRestoreMap.put(ActivityConstants.FORCED_RESTORE, "true");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(forcedRestoreMap);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.toString(), "restore")).thenReturn(2000);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        restoreServiceMock.execute(activityJobId);
        verify(commonCvOperationsMock, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));

    }

    @Test
    public void testExecute_NodeRestart() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        setConfigVersionMo();
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);

        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtilsMock.getPoAttributes(activityJobId)).thenReturn(activityMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(Matchers.anyString())).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(notificationRegistryMock).register(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        when((abstractBackupActivityMock).persistActivityJobAttributes(Matchers.anyLong(), Matchers.anyMap(), Matchers.anyMap(), Matchers.anyList())).thenReturn(true);
        Mockito.when(activityUtilsMock.getActivityInfo(activityJobId, RestoreService.class)).thenReturn(jobActivityInfo);
        final Map<String, String> forcedRestoreMap = new HashMap<String, String>();
        forcedRestoreMap.put(ActivityConstants.FORCED_RESTORE, "true");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(forcedRestoreMap);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.toString(), "restore")).thenReturn(2000);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("OTHER");
        when(setStartableActivityHandler.executeSetStartableMoAction(jobEnvironment, configurationVersionName, cvMoFdn)).thenReturn(true);
        when(nodeRestartConfiguartionParamProvider.getRestartRankConfigParameter(RestartActivityConstants.RESTART_RANK_KEY)).thenReturn("RESTART_WARM");
        when(nodeRestartConfiguartionParamProvider.getRestartReasonConfigParameter(RestartActivityConstants.RESTART_REASON_KEY)).thenReturn("PLANNED_RECONFIGURATION");
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType)).thenReturn(10);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        final Map<String, Object> actionArgumentsNodeRestart = new HashMap<>();
        actionArgumentsNodeRestart.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArgumentsNodeRestart.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArgumentsNodeRestart.put(RestartActivityConstants.RESTART_INFO, "manual restarting nodeor tesdting purpose");
        Mockito.doNothing().when(nodeRestartActivityHandler).executeNodeRestartAction(jobEnvironment, actionArgumentsNodeRestart, nodeRestartJobActivityInfo);
        restoreServiceMock.execute(activityJobId);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testExecute_FailedCVDoesNotExists() throws JobDataNotFoundException {
        mockJobActivityInfo();
        setConfigVersionMo();
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);

        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtilsMock.getPoAttributes(activityJobId)).thenReturn(activityMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(notificationRegistryMock).register(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        when((abstractBackupActivityMock).persistActivityJobAttributes(Matchers.anyLong(), Matchers.anyMap(), Matchers.anyMap(), Matchers.anyList())).thenReturn(true);
        Mockito.when(activityUtilsMock.getActivityInfo(activityJobId, RestoreService.class)).thenReturn(jobActivityInfo);
        final Map<String, String> forcedRestoreMap = new HashMap<>();
        forcedRestoreMap.put(ActivityConstants.FORCED_RESTORE, "true");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(forcedRestoreMap);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.toString(), "restore")).thenReturn(2000);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn(null);
        when(setStartableActivityHandler.executeSetStartableMoAction(jobEnvironment, configurationVersionName, cvMoFdn)).thenReturn(true);
        when(nodeRestartConfiguartionParamProvider.getRestartRankConfigParameter(RestartActivityConstants.RESTART_RANK_KEY)).thenReturn("RESTART_WARM");
        when(nodeRestartConfiguartionParamProvider.getRestartReasonConfigParameter(RestartActivityConstants.RESTART_REASON_KEY)).thenReturn("PLANNED_RECONFIGURATION");
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType)).thenReturn(10);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        final Map<String, Object> actionArgumentsNodeRestart = new HashMap<>();
        actionArgumentsNodeRestart.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArgumentsNodeRestart.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArgumentsNodeRestart.put(RestartActivityConstants.RESTART_INFO, "manual restarting nodeor tesdting purpose");
        Mockito.doNothing().when(nodeRestartActivityHandler).executeNodeRestartAction(jobEnvironment, actionArgumentsNodeRestart, nodeRestartJobActivityInfo);
        restoreServiceMock.execute(activityJobId);
        verify(activityUtilsMock, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.RESTORE, null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testExecuteWithUnableToPerformAction() throws JobDataNotFoundException {

        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);

        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");

        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        when(smrsServiceUtilMock.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenReturn(smrsDetails);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn(neType);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(notificationRegistryMock).register(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("RESTORE_EXECUTE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        Mockito.when(activityUtilsMock.getActivityInfo(activityJobId, RestoreService.class)).thenReturn(jobActivityInfo);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Exception exception = new RuntimeException();
        when(commonCvOperationsMock.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(exception);
        restoreServiceMock.execute(activityJobId);
        verify(commonCvOperationsMock, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testExecuteWithMediationServiceException() throws JobDataNotFoundException {

        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtilsMock.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        when(smrsServiceUtilMock.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenReturn(smrsDetails);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn(neType);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(notificationRegistryMock).register(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("RESTORE_EXECUTE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        Mockito.when(activityUtilsMock.getActivityInfo(activityJobId, RestoreService.class)).thenReturn(jobActivityInfo);
        final Exception exception = new RuntimeException();
        when(commonCvOperationsMock.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(exception);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        restoreServiceMock.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.RESTORE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, neName, cvMoFdn, null);
        verify(commonCvOperationsMock, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testProcessNotificationWhenMainActivityIsRestoring() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setProcessNotificationMocks(CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV, null);
        restoreServiceMock.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWhenDetailedActivityIsIntermediateState() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        setProcessNotificationMocks(null, CVCurrentDetailedActivity.RESTORE_INITIATED);
        restoreServiceMock.processNotification(notification);
    }

    @Test
    public void testProcessNotificationCompletedSuccessfully() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(fdnNotificationSubjectMock.getKey()).thenReturn(cvMoFdn).thenReturn(cvMoFdn).thenReturn(cvMoFdn);
        setProcessNotificationMocks(null, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(notificationRegistryMock.removeSubject(fdnNotificationSubjectMock)).thenReturn(true);
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(neJobStaticDataProviderMock.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(activityUtilsMock.getJobExecutionUser(neJobStaticDataMock.getMainJobId())).thenReturn(jobExecutionUser);
        when(activityUtilsMock.getNotifiedFDN(Matchers.any())).thenReturn(cvMoFdn);
        restoreServiceMock.processNotification(notification);
        final String logMessage = "Restore activity completed in processNotification for the moFdn " + cvMoFdn;
        verify(systemRecorder, times(1)).recordEvent(jobExecutionUser, eventName, EventLevel.COARSE, cvMoFdn, cvMoFdn,
                "SHM:" + 111111l + ":" + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS));
        // Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @Test
    public void testProcessNotificationThrowsException() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setProcessNotificationMocks(null, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(neJobStaticDataProviderMock.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);

        when(notification.getDpsDataChangedEvent()).thenThrow(new EJBException());
        when(notificationRegistryMock.removeSubject(fdnNotificationSubjectMock)).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        restoreServiceMock.processNotification(notification);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(111111l), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
    }

    @Test
    public void testProcessNotificationFailed() throws JobDataNotFoundException {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setProcessNotificationMocks(null, CVCurrentDetailedActivity.EXECUTION_FAILED);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(notificationRegistryMock.removeSubject(fdnNotificationSubjectMock)).thenReturn(true);
        when(jobEnvironment.getActivityJobId()).thenReturn(111111l);
        when(activityUtilsMock.getActivityInfo(jobEnvironment.getActivityJobId(), RestoreService.class)).thenReturn(jobActivityInfo);
        doNothing().when(activityCompleteTimer).startTimer(jobActivityInfo);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(Matchers.anyLong())).thenReturn(jobStaticDataMock);

        restoreServiceMock.processNotification(notification);
    }

    @Test
    public void onActionComplete() {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(mapMock.get(ShmConstants.ACTIVITY_START_DATE)).thenReturn(new Date());
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        restoreServiceMock.onActionComplete(activityJobId);
        verify(activityUtilsMock, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.RESTORE, null);
        final String logMessage = "Restore activity completed in processNotification for the moFdn " + cvMoFdn;
        verify(activityUtilsMock, times(0)).recordEvent(eventName, cvMoFdn, cvMoFdn,
                "SHM:" + 111111l + ":" + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS));
    }

    @Test
    public void onActionComplete_NodeRestart() {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);

        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(mapMock.get(ShmConstants.ACTIVITY_START_DATE)).thenReturn(new Date());
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("OTHER");
        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE)).thenReturn(true);
        restoreServiceMock.onActionComplete(activityJobId);
    }

    @Test
    public void cancelTimeout_SetStartable() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(Matchers.anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        when(setStartableActivityHandler.cancelTimeoutSetStartable(true, jobEnvironment)).thenReturn(activityStepResult);
        restoreServiceMock.cancelTimeout(activityJobId, true);
    }

    @Test
    public void cancelTimeout_setStartable() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        when(nodeRestartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.TRUE)).thenReturn(activityStepResult);
        restoreServiceMock.cancelTimeout(activityJobId, true);
    }

    @Test
    public void cancelTimeout_NodeRestart() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, RestartActivityConstants.ACTION_NAME);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        when(nodeRestartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.TRUE)).thenReturn(activityStepResult);
        restoreServiceMock.cancelTimeout(activityJobId, true);
    }

    @Test
    public void cancelTimeout_Restore() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        restoreServiceMock.cancelTimeout(activityJobId, true);
        verify(jobUpdateServiceMock, times(1)).readAndUpdateJobAttributesForCancel(anyLong(), anyList(), anyList());
    }

    @Test
    public void cancelTimeout_RestoreThrowsException() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenThrow(new EJBException());
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        when(nodeRestartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.TRUE)).thenReturn(activityStepResult);
        restoreServiceMock.cancelTimeout(activityJobId, true);
        verify(jobUpdateServiceMock, times(1)).readAndUpdateJobAttributesForCancel(anyLong(), anyList(), anyList());
    }

    @Test
    public void testHandleTimeoutFail() throws JobDataNotFoundException {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getActionId(anyMap())).thenReturn(100);
        when(activityUtilsMock.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        final List<AdminProductData> corruptedUps = new ArrayList<>();
        corruptedUps.add(new AdminProductData());
        when(configurationVersionUtilsMock.getCorrputedUps(anyMap())).thenReturn(corruptedUps);

        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtilsMock.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);

        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        final ActivityStepResult activityStepResult = restoreServiceMock.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.RESTORE);
        verify(systemRecorder, times(1)).recordEvent(jobExecutionUser, eventName, EventLevel.COARSE, neName, cvMoFdn,
                "SHM:" + activityJobId + ":" + neName + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
    }

    @Test
    public void testHandleTimeoutNodeRestart() {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);

        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        // when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, RestartActivityConstants.ACTION_NAME);
        activityJobPropertyList.add(activityAttributes);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        // when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getActionId(anyMap())).thenReturn(100);

        when(activityUtilsMock.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);

        final List<AdminProductData> corruptedUps = new ArrayList<>();
        corruptedUps.add(new AdminProductData());
        when(configurationVersionUtilsMock.getCorrputedUps(anyMap())).thenReturn(corruptedUps);

        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE)).thenReturn(true);

        final ActivityStepResult activityStepResult = restoreServiceMock.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(0l), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));

    }

    @Test
    public void testHandleTimeoutSuccess() throws JobDataNotFoundException {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getActionId(anyMap())).thenReturn(100);
        when(activityUtilsMock.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        final List<AdminProductData> corruptedUps = new ArrayList<>();
        corruptedUps.add(new AdminProductData());
        when(configurationVersionUtilsMock.getCorrputedUps(anyMap())).thenReturn(corruptedUps);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtilsMock.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        final ActivityStepResult activityStepResult = restoreServiceMock.handleTimeout(activityJobId);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.RESTORE);
        verify(systemRecorder, times(1)).recordEvent(jobExecutionUser, eventName, EventLevel.COARSE, neName, cvMoFdn,
                "SHM:" + activityJobId + ":" + neName + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
    }

    public void setProcessNotificationMocks(final CVCurrentMainActivity cvCurrentMainActivity, final CVCurrentDetailedActivity cvCurrentDetailedActivity) {
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(fdnNotificationSubjectMock)).thenReturn(111111l);
        when(activityUtilsMock.getJobEnvironment(111111l)).thenReturn(jobEnvironment);
        final Map<String, Object> map = new HashMap<>();
        map.put(ShmConstants.BUSINESS_KEY, ShmConstants.BUSINESS_KEY);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(map);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        when(activityUtilsMock.getNotificationTimeStamp(notification.getNotificationSubject())).thenReturn(new Date());
        final CvActivity newCvActivity = new CvActivity(cvCurrentMainActivity, cvCurrentDetailedActivity);
        when(configurationVersionUtilsMock.getNewCvActivity(notification)).thenReturn(newCvActivity);
        doNothing().when(activityUtilsMock).addJobProperty(anyString(), anyObject(), anyList());
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        when(workflowInstanceNotifierMock.sendActivate(ShmConstants.BUSINESS_KEY, null)).thenReturn(true);
    }

    @Test
    public void testAsyncHandleTimeoutFail() throws JobDataNotFoundException {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtilsMock.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getActionId(anyMap())).thenReturn(100);
        when(activityUtilsMock.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        final List<AdminProductData> corruptedUps = new ArrayList<>();
        corruptedUps.add(new AdminProductData());
        when(configurationVersionUtilsMock.getCorrputedUps(anyMap())).thenReturn(corruptedUps);

        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        when(jobEnvironment.getNodeName()).thenReturn(neName);

        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        restoreServiceMock.asyncHandleTimeout(activityJobId);
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.RESTORE);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForTimeoutAndNotifyWfs(jobEnvironment, ActivityConstants.RESTORE, ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        verify(systemRecorder, times(1)).recordEvent(jobExecutionUser, eventName, EventLevel.COARSE, neName, cvMoFdn,
                "SHM:" + activityJobId + ":" + neName + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
    }

    @Test
    public void testAsyncHandleTimeoutNodeRestart() {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> map = setConfigVersionMo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, RestartActivityConstants.ACTION_NAME);
        activityJobPropertyList.add(activityAttributes);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        // when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        // when(activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getActionId(anyMap())).thenReturn(100);
        when(activityUtilsMock.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        // when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        final List<AdminProductData> corruptedUps = new ArrayList<>();
        corruptedUps.add(new AdminProductData());
        when(configurationVersionUtilsMock.getCorrputedUps(anyMap())).thenReturn(corruptedUps);

        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE)).thenReturn(true);
        restoreServiceMock.asyncHandleTimeout(activityJobId);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForTimeoutAndNotifyWfs(jobEnvironment, ActivityConstants.RESTORE, ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(0l), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testAsyncHandleTimeoutSuccess() throws JobDataNotFoundException {
        final long activityJobId = 11111l;
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtilsMock.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> map = setConfigVersionMo();
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        when(configurationVersionServiceMock.getCVMoAttr(anyString())).thenReturn(map);
        when(configurationVersionUtilsMock.getActionId(anyMap())).thenReturn(100);
        when(activityUtilsMock.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        doNothing().when(activityUtilsMock).recordEvent(anyString(), anyString(), anyString(), anyString());
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        final List<AdminProductData> corruptedUps = new ArrayList<>();
        corruptedUps.add(new AdminProductData());
        when(configurationVersionUtilsMock.getCorrputedUps(anyMap())).thenReturn(corruptedUps);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("Some CV Name");

        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("DOWNLOADED");
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        ;
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        restoreServiceMock.asyncHandleTimeout(activityJobId);
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.RESTORE);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForTimeoutAndNotifyWfs(jobEnvironment, ActivityConstants.RESTORE, ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        verify(systemRecorder, times(1)).recordEvent(jobExecutionUser, eventName, EventLevel.COARSE, neName, cvMoFdn,
                "SHM:" + activityJobId + ":" + neName + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
    }

    @Test
    public void testPrecheckHandleTimeout() {
        restoreServiceMock.precheckHandleTimeout(activityJobId);
        verify(activityUtilsMock).failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.RESTORE);
    }

    @Test
    public void testTimeoutForHandleTimeout() {
        restoreServiceMock.timeoutForAsyncHandleTimeout(activityJobId);
        verify(activityUtilsMock).failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.RESTORE);
    }

    @Test
    public void testCancel() {
        final long activityJobId = 111111111l;
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final List<String> neFdns = new ArrayList<>();
        neFdns.add(neName);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType(neType);
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(networkElement);
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NE_JOB_ID, 22222222l);
        final Map<String, Object> neMap = new HashMap<String, Object>();
        neMap.put(ShmConstants.NE_NAME, "Node1");
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> moAttributesMap = new HashMap<>();
        final String cvMo = "MeContext=Node1,,ManagedElement=1,SwManagement=1,ConfigurationVersion=1";
        moAttributesMap.put(ShmConstants.FDN, cvMo);
        when(configurationVersionServiceMock.getCVMoAttr("Node1")).thenReturn(moAttributesMap);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> actionArguments = new HashMap<>();
        when(commonCvOperationsMock.executeActionOnMo(BackupActivityConstants.ACTION_CANCEL_RESTORE, cvMo, actionArguments)).thenReturn(1);
        when((jobUpdateServiceMock).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(neFdns)).thenReturn(networkElementList);
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        restoreServiceMock.cancel(activityJobId);
    }

    @Test
    public void testCancelWithException() {
        final long activityJobId = 111111111l;
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final List<String> neFdns = new ArrayList<>();
        neFdns.add(neName);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType(neType);
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(networkElement);
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NE_JOB_ID, 22222222l);
        final Map<String, Object> neMap = new HashMap<String, Object>();
        neMap.put(ShmConstants.NE_NAME, "Node1");
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        final Map<String, Object> moAttributesMap = new HashMap<>();
        final String cvMo = "MeContext=Node1,,ManagedElement=1,SwManagement=1,ConfigurationVersion=1";
        moAttributesMap.put(ShmConstants.FDN, cvMo);
        when(configurationVersionServiceMock.getCVMoAttr("Node1")).thenReturn(moAttributesMap);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> actionArguments = new HashMap<>();
        when(commonCvOperationsMock.executeActionOnMo(BackupActivityConstants.ACTION_CANCEL_RESTORE, cvMo, actionArguments)).thenThrow(NullPointerException.class);
        when((jobUpdateServiceMock).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(neFdns)).thenReturn(networkElementList);
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        restoreServiceMock.cancel(activityJobId);
    }

    @Test
    public void testCancelWithMediationServiceException() {
        final long activityJobId = 111111111l;
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(neName);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType(neType);
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(networkElement);
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NE_JOB_ID, 22222222l);
        final Map<String, Object> neMap = new HashMap<>();
        neMap.put(ShmConstants.NE_NAME, "Node1");
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mapMock);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        activityJobPropertyList.add(activityAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        final Map<String, Object> moAttributesMap = new HashMap<>();
        final String cvMo = "MeContext=Node1,,ManagedElement=1,SwManagement=1,ConfigurationVersion=1";
        moAttributesMap.put(ShmConstants.FDN, cvMo);
        when(configurationVersionServiceMock.getCVMoAttr("Node1")).thenReturn(moAttributesMap);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> actionArguments = new HashMap<>();
        when(commonCvOperationsMock.executeActionOnMo(BackupActivityConstants.ACTION_CANCEL_RESTORE, cvMo, actionArguments)).thenThrow(NullPointerException.class);
        when((jobUpdateServiceMock).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(neFdns)).thenReturn(networkElementList);
        when(activityUtilsMock.getNodeName(activityJobId)).thenReturn(neName);
        restoreServiceMock.cancel(activityJobId);
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        restoreServiceMock.processNotification(notification);
        verify(activityUtilsMock, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }

    @Test
    public void testEvaluateActivityStatus_ActivityPassedInPolling_WhenActivityStateAwaitingConfirmation() throws JobDataNotFoundException {
        final long activityJobId = 1;
        final String cvMoFdn = "cvMoFdn";
        final String[] CVMO_ATTRIBUTES = { CURRENT_MAIN_ACTIVITY, CURRENT_DETAILED_ACTIVITY };
        when(jobConfigurationServiceMock.isJobResultEvaluated(activityJobId)).thenReturn(false);
        final Map<String, Object> cvMoData = new HashMap<>();
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtilsMock.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        cvMoData.put(CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV);
        cvMoData.put(CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(commonCvOperationsMock.getCVMoAttributesFromNode(cvMoFdn, CVMO_ATTRIBUTES)).thenReturn(cvMoData);
        when(pollingActivityStatusManager.getActivityPollingCacheKey(cvMoFdn, activityJobId)).thenReturn(cvMoFdn + "_" + activityJobId);
        when(pollingActivityStatusManager.getReadCallStatus(activityJobId, cvMoFdn)).thenReturn(ReadCallStatusEnum.NOT_TRIGGERED);
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(cvActivity.getMainActivity()).thenReturn(CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV);
        when(jobEnvironment.getNodeName()).thenReturn("NodeName");
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        restoreServiceMock.readActivityStatus(activityJobId, cvMoFdn);
        verify(pollingActivityStatusManager, times(1)).unsubscribeFromPolling(Matchers.anyString());
        verify(pollingActivityStatusManager, times(1)).updateReadCallStatus(activityJobId, cvMoFdn, ReadCallStatusEnum.IN_PROGRESS);
        verify(pollingActivityStatusManager, times(1)).updateReadCallStatus(activityJobId, cvMoFdn, ReadCallStatusEnum.COMPLETED);
        final String logMessage = "Restore activity completed in polling for the moFdn " + cvMoFdn;

    }

    @Test
    public void testEvaluateActivityStatus_ActivityPassedInPolling_WhenActivityStateUnKnown() {
        final long activityJobId = 1;
        final String cvMoFdn = "cvMoFdn";
        final String[] CVMO_ATTRIBUTES = { CURRENT_MAIN_ACTIVITY, CURRENT_DETAILED_ACTIVITY };
        when(jobConfigurationServiceMock.isJobResultEvaluated(activityJobId)).thenReturn(false);
        final Map<String, Object> cvMoData = new HashMap<>();
        cvMoData.put(CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV);
        cvMoData.put(CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.UNKNOWN);
        when(commonCvOperationsMock.getCVMoAttributesFromNode(cvMoFdn, CVMO_ATTRIBUTES)).thenReturn(cvMoData);
        when(pollingActivityStatusManager.getActivityPollingCacheKey(cvMoFdn, activityJobId)).thenReturn(cvMoFdn + "_" + activityJobId);
        when(pollingActivityStatusManager.getReadCallStatus(activityJobId, cvMoFdn)).thenReturn(ReadCallStatusEnum.NOT_TRIGGERED);
        when(configurationVersionUtilsMock.getCvActivity(anyMap())).thenReturn(cvActivity);
        when(cvActivity.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.UNKNOWN);
        when(cvActivity.getMainActivity()).thenReturn(CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV);
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn("NodeName");
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        restoreServiceMock.readActivityStatus(activityJobId, cvMoFdn);
        verify(pollingActivityStatusManager, times(0)).unsubscribeFromPolling(Matchers.anyString());
        verify(pollingActivityStatusManager, times(1)).updateReadCallStatus(activityJobId, cvMoFdn, ReadCallStatusEnum.IN_PROGRESS);
        verify(pollingActivityStatusManager, times(1)).updateReadCallStatus(activityJobId, cvMoFdn, ReadCallStatusEnum.COMPLETED);
        final String logMessage = "Restore activity completed in polling for the moFdn " + cvMoFdn;
        verify(activityUtilsMock, times(0)).recordEvent(eventName, cvMoFdn, cvMoFdn,
                "SHM:" + activityJobId + ":" + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_POLLING));
    }

    @Test
    public void testEvaluateActivityStatus_ActivityAlreadyCompleted() {
        final long activityJobId = 1;
        final String cvMoFdn = "cnMoFdn";
        when(jobConfigurationServiceMock.isJobResultEvaluated(activityJobId)).thenReturn(true);
        when(pollingActivityStatusManager.getReadCallStatus(activityJobId, cvMoFdn)).thenReturn(ReadCallStatusEnum.NOT_TRIGGERED);
        final String eventName = "CPP.RESTORE.RESTORE_COMPLETED";
        when(activityUtilsMock.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.RESTORE, "restore")).thenReturn(eventName);
        restoreServiceMock.readActivityStatus(activityJobId, cvMoFdn);
        verify(pollingActivityStatusManager, times(1)).unsubscribeFromPolling(Matchers.anyString());
        verify(pollingActivityStatusManager, times(0)).updateReadCallStatus(Matchers.anyLong(), Matchers.anyString(), (ReadCallStatusEnum) Matchers.anyObject());
        final String logMessage = "Restore activity completed in polling for the moFdn " + cvMoFdn;
        when(activityUtilsMock.getJobExecutionUser(neJobStaticDataMock.getMainJobId())).thenReturn(jobExecutionUser);
        verify(activityUtilsMock, times(0)).recordEvent(jobExecutionUser, eventName, cvMoFdn, cvMoFdn,
                "SHM:" + activityJobId + ":" + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_POLLING));
    }

    private Map<String, Object> setConfigVersionMo() {

        setActivityJobPo();

        setNeJobPo();

        setMainJobPo();

        final Map<String, Object> cvMoAttr = new HashMap<>();
        final Map<String, Object> cvMoMap = new HashMap<>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUpgradePackage);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "RESTORE");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTION_FAILED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, 100);
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<>();
        final Map<String, String> storedConfigurationVersion = new HashMap<>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionServiceMock.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(configurationVersionUtilsMock.getConfigurationVersionName(Matchers.anyMap(), Matchers.anyMap())).thenReturn(configurationVersionName);
        final Map<String, Object> upMoAttributes = new HashMap<>();
        final Map<String, String> adminData = new HashMap<>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        return cvMoMap;

    }

    private void setActivityJobPo() {

        final Map<String, Object> activityJobAttr = new HashMap<>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobConfPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<>();
        final Map<String, String> mainJobProperty = new HashMap<>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        final Map<String, String> actionidProp = new HashMap<>();
        actionidProp.put(ShmConstants.KEY, ActivityConstants.ACTION_ID);
        actionidProp.put(ShmConstants.VALUE, "100");
        mainJobPropertyList.add(actionidProp);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(jobConfigurationServiceMock.retrieveJob(activityJobId)).thenReturn(activityJobAttr);
        when(activityUtilsMock.getPoAttributes(activityJobId)).thenReturn(activityJobAttr);
    }

    private void setNeJobPo() {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

        final Map<String, Object> neJobAttr = new HashMap<>();
        neJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttr.put(ShmConstants.NE_NAME, neName);
        neJobAttr.put(ShmConstants.BUSINESS_KEY, businessKey);
        neJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(jobConfigurationServiceMock.retrieveJob(neJobId)).thenReturn(neJobAttr);
        when(activityUtilsMock.getPoAttributes(neJobId)).thenReturn(neJobAttr);
    }

    private void setMainJobPo() {
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        final Map<String, Object> mainJobAttr = new HashMap<>();
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<>();
        final Map<String, String> mainJobProperty = new HashMap<>();
        mainJobProperty.put(ShmConstants.KEY, ActivityConstants.FORCED_RESTORE);
        mainJobProperty.put(ShmConstants.VALUE, "true");
        mainJobProperty.put(ShmConstants.KEY, ActivityConstants.AUTO_CONFIGURATION);
        mainJobProperty.put(ShmConstants.VALUE, "on");
        mainJobPropertyList.add(mainJobProperty);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        mainJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);

        when(jobConfigurationServiceMock.retrieveJob(mainJobId)).thenReturn(mainJobAttr);
        when(activityUtilsMock.getPoAttributes(mainJobId)).thenReturn(mainJobAttr);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfo.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        Mockito.when(jobActivityInfo.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testRestoreWithRestart() throws MoNotFoundException {
        when(activityUtilsMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(activityJobAttributes);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(configurationVersionServiceMock.getCVMoAttr(Matchers.anyString())).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.FDN)).thenReturn(cvMoFdn);
        when(configurationVersionUtilsMock.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(cvMoAttributes);
        when(restorePrecheckHandler.getCVTypeIfPresentOnNode(Matchers.anyList(), Matchers.anyString())).thenReturn("OTHER");
        when(setStartableActivityHandler.executeSetStartableMoAction(Matchers.any(), Matchers.anyString(), Matchers.anyString())).thenReturn(true);

        when(networkElementRetrievalBean.getNeType(Matchers.anyString())).thenReturn("RNC");
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);

        when(nodeRestartConfiguartionParamProvider.getRestartRankConfigParameter(RestartActivityConstants.RESTART_RANK_KEY)).thenReturn("RESTART_WARM");
        when(nodeRestartConfiguartionParamProvider.getRestartReasonConfigParameter(RestartActivityConstants.RESTART_REASON_KEY)).thenReturn("PLANNED_RECONFIGURATION");

        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getNodeRestartSleepTime(Matchers.anyString())).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(10);

        restoreServiceMock.execute(activityJobId);

        verify(nodeRestartActivityHandler).executeNodeRestartAction(Matchers.any(), Matchers.anyMap(), Matchers.any());
    }

}
