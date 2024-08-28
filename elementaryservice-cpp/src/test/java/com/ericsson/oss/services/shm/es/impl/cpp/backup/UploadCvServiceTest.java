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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionAdditionalInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionMainAndAdditionalResultHolder;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.moaction.retry.cpp.backup.BackupRetryPolicy;
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
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class UploadCvServiceTest {

    @Mock
    @Inject
    private JobUpdateService jobUpdateService;

    @Mock
    private ConfigurationVersionService configurationVersionService;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    private ActivityUtils activityUtils;

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    private CommonCvOperations commonCvOperations;

    @Mock
    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    @Inject
    private ResourceOperations resourceOperations;

    @InjectMocks
    private UploadCvService objectUnderTest;

    @Mock
    private Notification notification;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    @Inject
    private ConfigurationVersionUtils configurationVersionUtils;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private ActivityCompleteTimer activityCompleteTimer;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private BackupRetryPolicy backupMoActionRetryPolicyMock;

    @Mock
    private DpsDataChangedEvent dpsDataChangedEvent;

    @Mock
    private Map<String, AttributeChangeData> modifiedAttr;

    @Mock
    private BackupUtils backupActionResultUtility;

    @Mock
    private Map<String, Object> actionResultData;

    @Mock
    private CvActionMainAndAdditionalResultHolder actionMainAndAdditionalResultHolder;

    @Mock
    private CvActionAdditionalInfo actionAdditionalInfo;

    @Mock
    private List<CvActionAdditionalInfo> actionAdditionalInfos;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProviderMock;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigRetryProxyMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBeanMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private CvNameProvider cvNameProvider;

    @Mock
    private PollingActivityManager pollingActivityManager;

    Map<String, Object> neJobAttributes;
    Map<String, Object> activityJobAttributes;

    long activityJobId = 123L;
    long neJobId = 456L;
    long mainJobId = 789L;
    long templateJobId = 324L;

    int actionId = 5;

    String identity = "Some Identity";
    String jobExecutionUser = "TEST_USER";
    String type = "Standard";
    String neName = "Some Ne Name";
    private static final String neType = "ERBS";
    String configurationVersionName = "Some CV Name";
    String cvMoFdn = "Some Cv Mo Fdn";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    String pathOnFtpServer = "Some Path";
    String relativePathFromNetworkType = "Some Relative Path";
    String ftpServerIpAddress = "Some IP";
    String ftpServerUserId = "Some User";
    char[] ftpServerPassword = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
    String cvBackupNameOnFtpServer = "Some CV Name on FTP Server";
    private static final String ACTIVITY_NAME = "exportcv";

    private void mockNeJobStaticData() throws JobDataNotFoundException {
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getActivityStartTime()).thenReturn(new Date().getTime());
        when(neJobStaticDataMock.getNodeName()).thenReturn(neName);
        when(neJobStaticDataMock.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
    }

    @Test
    public void testPrecheckWithNoCvMo() throws JobDataNotFoundException {
        mockNeJobStaticData();
        setConfigVersionMoAsNull();
        activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        final ActivityStepResultEnum activityStepResultEnum = objectUnderTest.precheck(activityJobId).getActivityResultEnum();
        verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        assertNotNull(activityStepResultEnum);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResultEnum);
    }

    @Test
    public void testPrecheckWithCvMo() throws JobDataNotFoundException {
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");
        activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        when(jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        Mockito.doNothing().when(activityAndNEJobProgressPercentageCalculator).updateNEJobProgressPercentage(neJobId);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        final ActivityStepResultEnum activityStepResultEnum = objectUnderTest.precheck(activityJobId).getActivityResultEnum();
        verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        assertNotNull(activityStepResultEnum);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteSuccess() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        final Map<String, String> activityJobProperty = new HashMap<>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, ActivityStepsEnum.PRECHECK + ShmConstants.EQUALS + "1.0");
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        doNothing().when(resourceOperations).createDirectory(pathOnFtpServer, neName);
        when(resourceOperations.isDirectoryExistsWithWritePermissions(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(networkElementRetrievalBeanMock.getNeType("nodeName")).thenReturn(neType);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        smrsDetails.setRelativePathToSmrsRoot(relativePathFromNetworkType);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, "nodeName")).thenReturn(smrsDetails);
        doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        final Map<String, Object> actionArguments = new HashMap<>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, configurationVersionName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvBackupNameOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
        when(backupMoActionRetryPolicyMock.getDpsMoActionRetryPolicy()).thenReturn(retryPolicyMock);
        when(commonCvOperations.executeActionOnMo(anyString(), anyString(), anyMap(), Matchers.any(RetryPolicy.class))).thenReturn(actionId);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.BACKUP.toString(), "exportcv")).thenReturn(2000);
        doNothing().when(jobLogUtilMock).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
        when(activityUtils.getActivityInfo(activityJobId, UploadCvService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getJobExecutionUser()).thenReturn(jobExecutionUser);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        verify(commonCvOperations, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class));
        verify(activityUtils, times(7)).prepareJobPropertyList(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
        verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
        verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
        verify(activityUtils, times(0)).recordEvent(jobExecutionUser, SHMEvents.UPLOAD_BACKUP_PRECHECK, neJobStaticDataMock.getNodeName(), cvMoFdn,
                "SHM:" + activityJobId + ":" + neJobStaticDataMock.getNodeName() + ":" + "Proceeding Upload CV.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithSmrsIssue() throws JobDataNotFoundException, MoNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(networkElementRetrievalBeanMock.getNeType("nodeName")).thenReturn(neType);
        final Exception exception = new RuntimeException();
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenThrow(exception);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).failActivity(activityJobId, new ArrayList<Map<String, Object>>(), neJobStaticDataMock.getNeJobBusinessKey(), ActivityConstants.UPLOAD_CV);
        verify(activityUtils, times(4)).prepareJobPropertyList(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testExecuteWithSmrsPath() throws JobDataNotFoundException, MoNotFoundException {
        mockNeJobStaticData();
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        final Map<String, String> activityJobProperty = new HashMap<>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, ActivityStepsEnum.PRECHECK + ShmConstants.EQUALS + "1.0");
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        doNothing().when(resourceOperations).createDirectory(pathOnFtpServer, neName);
        when(resourceOperations.isDirectoryExistsWithWritePermissions(anyString(), anyString())).thenReturn(true);
        when(networkElementRetrievalBeanMock.getNeType("nodeName")).thenReturn(neType);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        smrsDetails.setRelativePathToSmrsRoot(relativePathFromNetworkType);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, "nodeName")).thenReturn(smrsDetails);
        when(activityUtils.getActivityInfo(activityJobId, UploadCvService.class)).thenReturn(jobActivityInfoMock);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(jobActivityInfoMock.getActivityName()).thenReturn("upload");
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));

        final Map<String, Object> actionArguments = new HashMap<>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, configurationVersionName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvBackupNameOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
        when(commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_UPLOAD_CV, cvMoFdn, actionArguments)).thenReturn(actionId);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteSmrsDirectoryDoesNotExists() throws JobDataNotFoundException, MoNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        final Map<String, String> activityJobProperty = new HashMap<>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");

        doNothing().when(resourceOperations).createDirectory(pathOnFtpServer, neName);
        when(resourceOperations.isDirectoryExistsWithWritePermissions(Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(networkElementRetrievalBeanMock.getNeType("nodeName")).thenReturn(neType);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        smrsDetails.setRelativePathToSmrsRoot(relativePathFromNetworkType);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, "nodeName")).thenReturn(smrsDetails);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UploadCvService.class)).thenReturn(jobActivityInfoMock);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(jobActivityInfoMock.getActivityName()).thenReturn("upload");
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithUnableToPerformAction() throws JobDataNotFoundException, MoNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        final Map<String, String> activityJobProperty = new HashMap<>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, ActivityStepsEnum.PRECHECK + ShmConstants.EQUALS + "1.0");
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");

        doNothing().when(resourceOperations).createDirectory(pathOnFtpServer, neName);
        when(resourceOperations.isDirectoryExistsWithWritePermissions(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UploadCvService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getActivityName()).thenReturn("upload");
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(networkElementRetrievalBeanMock.getNeType("nodeName")).thenReturn(neType);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        smrsDetails.setRelativePathToSmrsRoot(relativePathFromNetworkType);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenReturn(smrsDetails);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));

        final Exception exception = new RuntimeException();
        when(commonCvOperations.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(exception);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).failActivity(activityJobId, new ArrayList<Map<String, Object>>(), neJobStaticDataMock.getNeJobBusinessKey(), ActivityConstants.UPLOAD_CV);
        verify(activityUtils, times(0)).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMediationServiceException() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        final Map<String, String> activityJobProperty = new HashMap<>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        doNothing().when(resourceOperations).createDirectory(pathOnFtpServer, neName);
        when(resourceOperations.isDirectoryExistsWithWritePermissions(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(networkElementRetrievalBeanMock.getNeType("nodeName")).thenReturn(neType);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        smrsDetails.setRelativePathToSmrsRoot(relativePathFromNetworkType);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, "nodeName")).thenReturn(smrsDetails);
        doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        final Map<String, Object> actionArguments = new HashMap<>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, configurationVersionName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvBackupNameOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
        when(backupMoActionRetryPolicyMock.getDpsMoActionRetryPolicy()).thenReturn(retryPolicyMock);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.BACKUP.toString(), "exportcv")).thenReturn(2000);
        doNothing().when(jobLogUtilMock).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UploadCvService.class)).thenReturn(jobActivityInfoMock);
        final Throwable cause1 = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceException", cause1);
        when(commonCvOperations.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(exception);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).failActivity(activityJobId, new ArrayList<Map<String, Object>>(), neJobStaticDataMock.getNeJobBusinessKey(), ActivityConstants.UPLOAD_CV);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsExportingBackupCv() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "EXPORTING_BACKUP_CV");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultData() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);
        when(activityUtils.getPersistedActionId(activityJobId, neName, activityJobAttributes)).thenReturn(111);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultDataAndWithoutHavingActionIdPersistedInDb() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.toString());
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.PUT_TO_FTP_SERVER.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);
        when(activityUtils.getPersistedActionId(activityJobId, neName, activityJobAttributes)).thenReturn(-1);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultDataFailed1() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultDataFailed2() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("", "EXECUTED");
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onActionCompleteTest() throws JobDataNotFoundException {
        setConfigVersionMo("", "EXECUTED");
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        objectUnderTest.onActionComplete(activityJobId, neJobStaticDataMock);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsCreatingCv() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "CREATING_CV");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsCreatingCv() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "CREATING_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsExportingBackupCvAndCurrentDetailedActivityAsIdle() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);

        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "IDLE");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsExportingBackupCvAndCurrentDetailedActivityAsIdleWithActionResultAsExecutionFailed()
            throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMoWithActionResultAsExecutionFailed();

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);

        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "IDLE");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);

        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsExportingBackupCvAndCurrentDetailedActivityAsIdleWithInvokedActionAsRestore()
            throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMoWithWrongActionResultData();

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);

        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "IDLE");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsExportingBackupCvAndCurrentDetailedActivityAsExecutionFailed() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);

        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "EXECUTION_FAILED");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "SAVING_FINAL_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsNull() throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);

        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAndPreviousCurrentMainActivityAsNullAndCurrentDetailedActivityAsIdleAndPreviousCurrentDetailedActivityAsCreatingBackup()
            throws JobDataNotFoundException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockNeJobStaticData();
        setConfigVersionMo("abcd", "EXECUTED");

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "EXECUTION_FAILED");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "EXPORT_OF_BACKUP_CV_REQUESTED");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWhenCancelIsTriggeredBeforeUploadingAllBackups() throws JobDataNotFoundException {
        mockNeJobStaticData();
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubject);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttr);
        when(activityUtils.getActivityJobId(fdnNotificationSubject)).thenReturn(activityJobId);
        when(backupActionResultUtility.getActionResultData(modifiedAttr)).thenReturn(actionResultData);
        when(backupActionResultUtility.isActionResultNotified(actionResultData)).thenReturn(true);
        when(actionResultData.get(ConfigurationVersionMoConstants.ACTION_ID)).thenReturn(actionId);
        when(activityUtils.getPersistedActionId(activityJobId, neName, activityJobAttributes)).thenReturn(actionId + 1);
        when(activityUtils.cancelTriggered(activityJobId)).thenReturn(true);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        objectUnderTest.processNotification(notification);
        verify(backupActionResultUtility, times(0)).isJobSuccess(actionResultData);
    }

    @Test
    public void testProcessNotificationWhenSuccess() throws JobDataNotFoundException {
        mockNeJobStaticData();
        activityJobAttributes = new HashMap<String, Object>();
        final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS.substring(0, JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS.length() - 1),
                ActivityConstants.UPLOAD_CV) + " for " + configurationVersionName;
        final String eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.BACKUP, ActivityConstants.UPLOAD);
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CURRENT_BACKUP);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getJobExecutionUser()).thenReturn(jobExecutionUser);
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubject);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttr);
        when(activityUtils.getActivityJobId(fdnNotificationSubject)).thenReturn(activityJobId);
        when(backupActionResultUtility.getActionResultData(modifiedAttr)).thenReturn(actionResultData);
        when(backupActionResultUtility.isActionResultNotified(actionResultData)).thenReturn(true);
        when(actionResultData.get(ConfigurationVersionMoConstants.ACTION_ID)).thenReturn(actionId);
        when(actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME)).thenReturn(configurationVersionName);
        when(actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION)).thenReturn(CVInvokedAction.PUT_TO_FTP_SERVER.toString());
        when(activityUtils.getPersistedActionId(activityJobId, neName, activityJobAttributes)).thenReturn(actionId + 1);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(backupActionResultUtility.isJobSuccess(actionResultData)).thenReturn(true);
        objectUnderTest.processNotification(notification);
        verify(backupActionResultUtility, times(1)).isJobSuccess(actionResultData);
        verify(activityUtils, times(0)).recordEvent(jobExecutionUser, eventName, neJobStaticDataMock.getNodeName(), null,
                "SHM:" + activityJobId + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutSuccess() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(activityUtils.cancelTriggered(activityJobId)).thenReturn(false);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.REPEAT_EXECUTE, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(anyLong(), anyList(), anyList(), anyDouble());
        verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutSuccessWithoutRepeat() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationVersionUtils.retrieveActionResultDataWithAddlInfo(Matchers.anyMap())).thenReturn(actionMainAndAdditionalResultHolder);
        when(actionMainAndAdditionalResultHolder.getCvActionMainResult()).thenReturn(CVActionMainResult.EXECUTED);
        final List<CvActionAdditionalInfo> list = new ArrayList<CvActionAdditionalInfo>();
        final CvActionAdditionalInfo actionAdditionalInfo = new CvActionAdditionalInfo();
        actionAdditionalInfo.setAdditionalInformation("additionalInformation");
        list.add(actionAdditionalInfo);

        when(actionMainAndAdditionalResultHolder.getActionAdditionalResult()).thenReturn(list);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        setConfigVersionMo("abcd", "EXECUTED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(anyLong(), anyList(), anyList(), anyDouble());
        verify(activityUtils, times(2)).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeoutFailure() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);
        setConfigVersionMo("abcd", "EXECUTION_FAILED");
        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutRepeatWhenCancelIsTriggered() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "EXECUTED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        when(activityUtils.cancelTriggered(activityJobId)).thenReturn(true);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        verify(activityUtils, times(2)).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testCancel() throws JobDataNotFoundException {
        mockNeJobStaticData();
        objectUnderTest.cancel(activityJobId);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }

    @Test
    public void testCancelTimeout() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        setConfigVersionMo("abcd", "EXECUTED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutWhenAllBackupsNotProcessed() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "0");
        activityJobPropertyList.add(processedBackups);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        setConfigVersionMo("abcd", "EXECUTED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutHavingIntermediateFailures() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);

        final Map<String, String> intermediateFailures = new HashMap<String, String>();
        intermediateFailures.put(ShmConstants.KEY, BackupActivityConstants.INTERMEDIATE_FAILURE);
        intermediateFailures.put(ShmConstants.VALUE, "true");
        activityJobPropertyList.add(intermediateFailures);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        setConfigVersionMo("abcd", "EXECUTED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutFailureAfterProcessingAllBackups() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.UPLOAD_CV_NAME)).thenReturn(configurationVersionName);
        setConfigVersionMo("abcd", "EXECUTION_FAILED");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutStillOngoing() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(configurationVersionUtils.getActionId(activityJobAttributes)).thenReturn(2);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING, objectUnderTest.cancelTimeout(activityJobId, false).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutStillOngoingButNeedToFinalizeTheResult() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobProperty = new HashMap<String, String>();
        activityJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        activityJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        activityJobPropertyList.add(activityJobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        final Map<String, String> processedBackups = new HashMap<String, String>();
        processedBackups.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        processedBackups.put(ShmConstants.VALUE, "1");
        activityJobPropertyList.add(processedBackups);
        when(jobConfigRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        setConfigVersionMo("abcd", "");

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        when(configurationVersionUtils.getActionId(activityJobAttributes)).thenReturn(2);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }

    private void setConfigVersionMo(final String additionalInformation, final String result) {
        setActivityJobPo();
        setNeJobPo();
        when(neJobStaticDataMock.getNodeName()).thenReturn("nodeName");
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUpgradePackage);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IDLE");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, 0);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "PUT_TO_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, result);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, Object>> additionalActionResultList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> additionalActionResult = new HashMap<String, Object>();
        additionalActionResult.put("information", "CV_NAME_ALREADY_EXISTS");
        additionalActionResult.put("additionalInformation", additionalInformation);
        additionalActionResultList.add(additionalActionResult);
        cvMoAttr.put("additionalActionResultData", additionalActionResultList);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(anyString())).thenReturn(cvMoMap);
        when(configurationVersionService.getCVMoFdn("nodeName")).thenReturn(cvMoFdn);

        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);

    }

    private void setConfigVersionMoAsNull() {
        setActivityJobPo();
        setNeJobPo();
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(null);
    }

    private void setConfigVersionMoWithActionResultAsExecutionFailed() {
        setActivityJobPo();
        setNeJobPo();

        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUpgradePackage);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IDLE");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "PUT_TO_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTION_FAILED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);

        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);

    }

    private void setConfigVersionMoWithWrongActionResultData() {
        setActivityJobPo();
        setNeJobPo();

        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUpgradePackage);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IDLE");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "RESTORE");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTION_FAILED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);

        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
    }

    private Map<String, Object> setActivityJobPo() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        return activityJobAttr;
    }

    private Map<String, Object> setNeJobPo() {
        final Map<String, Object> neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.NE_NAME, neName);
        neJobAttr.put(ShmConstants.NETYPE, neType);
        neJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final List<Map<String, String>> neJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> neJobProperty = new HashMap<String, String>();
        neJobProperty.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
        neJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        neJobPropertyList.add(neJobProperty);
        neJobAttr.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(neJobId)).thenReturn(neJobAttr);
        return neJobAttr;
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

}