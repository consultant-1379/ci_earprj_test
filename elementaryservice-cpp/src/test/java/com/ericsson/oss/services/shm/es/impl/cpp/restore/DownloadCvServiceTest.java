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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVInvokedAction;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class DownloadCvServiceTest {

    @InjectMocks
    private DownloadCvService downloadCvService;

    @Mock
    @Inject
    private ConfigurationVersionUtils configurationVersionUtils;

    @Mock
    private ConfigurationVersionService cvService;

    @Mock
    @Inject
    private JobUpdateService jobUpdateService;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    private ActivityUtils activityUtils, activityUtilsCopy;

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    private RestorePrecheckHandler restorePrecheckHandlerMock;

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    @Inject
    private CommonCvOperations commonCvOperations;

    @Mock
    @Inject
    private AbstractBackupActivity abstractBackupActivity;

    @Mock
    private Notification notification;

    @Mock
    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    @Inject
    private Resource resource;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private SmrsRetryPolicies smrsRetryPolicies;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private NetworkElement networkElement;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProviderMock;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    long activityJobId = 123L;
    long neJobId = 1234L;
    long mainJobId = 123L;
    long templateJobId = 123L;
    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    int actionId = 5;
    String businessKey = "abc1";
    String key = BackupActivityConstants.CV_NAME;
    String jobExecutionUser = "TEST_USER";

    String identity = "Some Identity";
    String type = "Standard";
    String neName = "Some Ne Name";
    String neType = "ERBS";
    String configurationVersionName = "Some CV Name";
    String configurationVersionType = "Some CV Type";

    String cvMoFdn = "Some Cv Mo Fdn";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    String pathOnFtpServer = "Some Path";
    String ftpServerIpAddress = "Some IP";
    String ftpServerUserId = "Some User";
    char[] ftpServerPassword = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
    String cvBackupNameOnFtpServer = "Some CV Name on FTP Server";
    String smrsFilePath = "/";
    private static final String DOWNLOAD_ACTIVITY_NAME = "download";

    @Test
    public void testPrecheckWithPass() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.when(restorePrecheckHandlerMock.getRestorePrecheckResult(11111111l, ActivityConstants.DOWNLOAD_CV, DOWNLOAD_ACTIVITY_NAME)).thenReturn(activityStepResult);
        final ActivityStepResult activityStepResultOutput = downloadCvService.precheck(11111111l);
        Assert.assertTrue(activityStepResultOutput.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
    }

    @Test
    public void testExecute() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(0));
        jobPropertyList.add(jobProperty);
        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenReturn(smrsDetails);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, configurationVersionName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvBackupNameOnFtpServer);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
        when(activityUtils.getActivityInfo(activityJobId, DownloadCvService.class)).thenReturn(jobActivityInfoMock);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.toString(), "install")).thenReturn(2000);
        when(commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_UPLOAD_CV, cvMoFdn, actionArguments)).thenReturn(actionId);
        downloadCvService.execute(activityJobId);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));

    }

    @Test
    public void testExecuteWithSmrsIssue() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        setConfigVersionMo();
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        final Exception exception = new RuntimeException();
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenThrow(exception);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        downloadCvService.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testExecutecvMoFdn() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        setConfigVersionMocvMoFdn();
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        final Exception exception = new RuntimeException();
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenThrow(exception);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        downloadCvService.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithUnableToPerformAction() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenReturn(smrsDetails);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        final Exception exception = new RuntimeException();
        Mockito.when(activityUtils.getActivityInfo(activityJobId, DownloadCvService.class)).thenReturn(jobActivityInfoMock);
        when(commonCvOperations.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(exception);
        downloadCvService.execute(activityJobId);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMediationServiceException() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnFtpServer);
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(ftpServerUserId);
        smrsDetails.setPassword(ftpServerPassword);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, neName)).thenReturn(smrsDetails);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        Mockito.doNothing().when(systemRecorder).recordCommand("UPLOAD_CV_SERVICE", CommandPhase.STARTED, neName, cvMoFdn, Long.toString(mainJobId));
        final Exception exception = new RuntimeException();
        Mockito.when(activityUtils.getActivityInfo(activityJobId, DownloadCvService.class)).thenReturn(jobActivityInfoMock);
        when(commonCvOperations.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(exception);
        downloadCvService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.DOWNLOAD_RESTORE_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, null);
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.DOWNLOAD_RESTORE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, neName, cvMoFdn, null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsInmportBackupCv() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IMPORTING_BACKUP_CV");
        currentMainActivityMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);

    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsImportBackup() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IMPORT_OF_BACKUP_CV_REQUESTED");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultData() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);
        when(activityUtils.getPersistedActionId(jobEnvironment)).thenReturn(111);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultDataAndWithoutHavingActionIdPersistedInDb() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.toString());
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.GET_FROM_FTP_SERVER.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);
        when(activityUtils.getPersistedActionId(jobEnvironment)).thenReturn(-1);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsIdleAndPreviousCurrentMainActivityAsDeletingCV() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "DELETING_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithMainActivitiesAsNull() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "EXPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "EXECUTION_FAILED");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "SAVING_FINAL_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithMainActivitiesAsNullAndCurrentDetailedActivityNotNull() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setActivityJobPo();
        setNeJobPo();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, null);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "GET_FROM_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, configurationVersionType);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "IMPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "IMPORT_OF_BACKUP_CV_REQUESTED");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "RETREIVING_BACKUP_FROM_REMOTE_SERVER");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithMainActivitiesAsNullAndCurrentDetailedActivityIdle() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setActivityJobPo();
        setNeJobPo();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, null);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "GET_FROM_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, configurationVersionType);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "IMPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "UNPACKING_RETREIVED_BACKUP");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "UNPACKING_RETREIVED_BACKUP");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();

        downloadCvService.processNotification(notification);

    }

    @Test
    public void testHandleTimeout() throws JobDataNotFoundException {
        mockJobActivityInfo();
        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        when(activityUtils.getActivityInfo(activityJobId, DownloadCvService.class)).thenReturn(jobActivityInfoMock);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn("nodeName");
        final Map<String, Object> map = setConfigVersionMo();
        when(cvService.getCVMoAttr("nodeName")).thenReturn(map);
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        assertNotNull(downloadCvService.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeoutWhenCvMoNotExists() throws JobDataNotFoundException {
        mockJobActivityInfo();
        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        when(activityUtils.getActivityInfo(activityJobId, DownloadCvService.class)).thenReturn(jobActivityInfoMock);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn("nodeName");
        when(cvService.getCVMoAttr("nodeName")).thenReturn(new HashMap<String, Object>());
        jobPropertiesList.add(jobProperty);
        when(jobConfigurationService.fetchJobProperty(activityJobId)).thenReturn(jobPropertiesList);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, downloadCvService.handleTimeout(activityJobId).getActivityResultEnum());
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setConfigVersionMo() {
        setActivityJobPo();
        setNeJobPo();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IMPORTING_BACKUP_CV");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "TRANSFERING_BACKUP_TO_REMOTE_SERVER");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "GET_FROM_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, configurationVersionType);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        return cvMoMap;
    }

    @SuppressWarnings("unchecked")
    private void setConfigVersionMocvMoFdn() {
        setActivityJobPo();
        setNeJobPo();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IMPORTING_BACKUP_CV");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "TRANSFERING_BACKUP_TO_REMOTE_SERVER");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "GET_FROM_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, configurationVersionType);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, null);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
    }

    private void setActivityJobPo() {

        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NETYPE, neType);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobConfPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(123L)).thenReturn(activityJobAttr);
        when(activityUtils.getPoAttributes(123L)).thenReturn(activityJobAttr);
    }

    private void setNeJobPo() {
        final Map<String, Object> neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.NE_NAME, neName);
        neJobAttr.put(ShmConstants.NETYPE, neType);
        neJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttr.put(ShmConstants.BUSINESS_KEY, businessKey);
        final List<Map<String, String>> neJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> neJobProperty = new HashMap<String, String>();
        neJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        neJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        neJobPropertyList.add(neJobProperty);
        neJobAttr.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(neJobId)).thenReturn(neJobAttr);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttr);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithMainActivitiesAsNullAndCurrentDetailedActivityNotRestore() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setActivityJobPo();
        setNeJobPo();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, null);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "GET_FROM_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, configurationVersionType);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> currentMainActivityMap = new HashMap<String, Object>();
        currentMainActivityMap.put("notifiableAttributeValue", "IDLE");
        currentMainActivityMap.put("previousNotifiableAttributeValue", "IMPORTING_BACKUP_CV");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY)).thenReturn(currentMainActivityMap);
        final Map<String, Object> currentDetailedActivityMap = new HashMap<String, Object>();
        currentDetailedActivityMap.put("notifiableAttributeValue", "CREATING_BACKUP");
        currentDetailedActivityMap.put("previousNotifiableAttributeValue", "RETREIVING_BACKUP_FROM_REMOTE_SERVER");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY)).thenReturn(currentDetailedActivityMap);
        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", null);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        downloadCvService.processNotification(notification);

    }

    @Test
    public void testCancel() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        downloadCvService.cancel(activityJobId);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        downloadCvService.processNotification(notification);
        verify(activityUtils, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }
}
