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
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStatus;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVInvokedAction;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionAdditionalInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionMainAndAdditionalResultHolder;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
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
public class ConfirmRestoreServiceTest {

    @Mock
    @Inject
    private JobUpdateService jobUpdateService;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    private CommonCvOperations commonCvOperations;

    @Mock
    @Inject
    private DpsReader dpsReader;

    @Mock
    @Inject
    private ConfigurationVersionUtils configurationVersionUtils;

    @InjectMocks
    private ConfirmRestoreService objectUnderTest;

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

    @Mock
    @Inject
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private Notification notification;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private ActivityUtils activityUtilMock;

    @Mock
    private SystemRecorder systemRecoderMock;

    @Mock
    private WorkflowInstanceNotifier workflowMock;

    @Mock
    private ConfigurationVersionUtils cvUtilMock;

    @Mock
    private ConfigurationVersionService cvServiceMock;

    @Mock
    private JobEnvironment jobEnvMock;

    @Mock
    private ConfigurationVersionMO cvMoMock;

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private Map<String, Object> neJobAttributesMapMock;

    @Mock
    private Map<String, Object> activityJobAttributesMapMock;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private RestorePrecheckHandler restorePrecheckHandlerMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

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

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxyMock;

    long activityJobId = 123L;
    String nodeName = "TestErbs";

    private final String businessKey = "businesskey";
    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;

    String neName = "Some Ne Name";
    String cvMoFdn = "Some Cv Mo Fdn";
    String configurationVersionName = "Some CV Name";
    String identity = "Some Identity";
    String type = "Standard";
    String operatorName = "Some Operator Name";
    String comment = "Some Comment";
    String jobName = "Some Job Name";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    String jobExecutionUser = "TEST_USER";

    int actionId = 5;

    Map<String, Object> templateJobAttr;
    Map<String, Object> mainJobAttr;

    private static final String CONFIRM_ACTIVITY_NAME = "confirm";

    @Test
    public void testPrecheck() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        when(restorePrecheckHandlerMock.getRestorePrecheckResult(11111111l, ActivityConstants.CONFIRM_RESTORE_CV, ActivityConstants.CONFIRM)).thenReturn(activityStepResult);
        final ActivityStepResult activityStepResultOutput = objectUnderTest.precheck(11111111l);
        Assert.assertTrue(activityStepResult == activityStepResultOutput);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfullExecute() {
        List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
        jobPropertyList.add(jobProperty);
        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        Mockito.doNothing().when(activityUtilMock).recordEvent(SHMEvents.CONFIRM_RESTORE_EXECUTE, neName, cvMoFdn, "SHM:" + activityJobId + ":" + neName);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(activityUtilMock.getActivityInfo(activityJobId, ConfirmRestoreService.class)).thenReturn(jobActivityInfoMock);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when((jobUpdateService).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, cvMoFdn, actionArguments)).thenReturn(actionId);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.toString(), BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV))
                .thenReturn(2000);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
        verify(commonCvOperations, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtilMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithException() {
        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Mockito.doNothing().when(activityUtilMock).recordEvent(BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, neName, cvMoFdn, "SHM:" + activityJobId + ":" + neName);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(activityUtilMock.getActivityInfo(activityJobId, ConfirmRestoreService.class)).thenReturn(jobActivityInfoMock);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when((jobUpdateService).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        final Exception exception = new RuntimeException();
        when(commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, cvMoFdn, actionArguments)).thenThrow(exception);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        objectUnderTest.execute(activityJobId);
        verify(commonCvOperations, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMediationServiceException() {
        setConfigVersionMo();
        mockJobActivityInfo();
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        Mockito.doNothing().when(activityUtilMock).recordEvent(BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, neName, cvMoFdn, "SHM:" + activityJobId + ":" + neName);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(activityUtilMock.getActivityInfo(activityJobId, ConfirmRestoreService.class)).thenReturn(jobActivityInfoMock);
        when(activityUtilMock.getJobExecutionUser(jobEnvMock.getMainJobId())).thenReturn(jobExecutionUser);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when((jobUpdateService).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        final Exception exception = new RuntimeException();
        when(commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_CONFIRM_RESTORE_CV, cvMoFdn, actionArguments)).thenThrow(exception);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.CONFIRM_RESTORE_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, neName, cvMoFdn, null);
        verify(commonCvOperations, times(1)).executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testProcessNotificationWithCurrentMainActivityAsConfirmingRestore() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtilMock.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        when(jobEnvMock.getNeJobAttributes()).thenReturn(neJobAttributesMapMock);
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributesMapMock);
        when(activityUtilMock.getActivityJobAttributeValue(jobEnvMock.getActivityJobAttributes(), BackupActivityConstants.CONFIRM_RESTORE_CV_COMPLETED)).thenReturn("false");
        when(activityUtilMock.getActivityJobAttributeValue(activityJobAttributesMapMock, ActivityConstants.ACTIVITY_RESULT)).thenReturn("");
        when(neJobAttributesMapMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultData() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtilMock.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtilMock.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);
        when(activityUtilMock.getPersistedActionId(jobEnvMock)).thenReturn(111);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        when(activityUtilMock.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);
        // Mockito.verify(activityUtilMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION)); 

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithCurrentActionResultDataAndWithoutHavingActionIdPersistedInDb() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        setConfigVersionMo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();

        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);

        final Map<String, Object> notifiableAttributesForActionResultData = new HashMap<String, Object>();
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 111);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.toString());
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        notifiableAttributesForActionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.CONFIRM_RESTORE.toString());

        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtilMock.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtilMock.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        when(configurationVersionUtils.getActionId(Matchers.anyMap())).thenReturn(111);
        when(activityUtilMock.getPersistedActionId(jobEnvMock)).thenReturn(-1);

        final Map<String, Object> currentActionResultDataMap = new HashMap<String, Object>();
        currentActionResultDataMap.put("notifiableAttributeValue", notifiableAttributesForActionResultData);
        currentActionResultDataMap.put("previousNotifiableAttributeValue", null);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtilMock.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT)).thenReturn(currentActionResultDataMap);
        setActivityJobPo();
        objectUnderTest.processNotification(notification);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithException() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

        final JobResult jobResult = JobResult.SUCCESS;

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        when(jobEnvMock.getNeJobAttributes()).thenThrow(Exception.class);
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributesMapMock);
        when(activityUtilMock.getActivityJobAttributeValue(jobEnvMock.getActivityJobAttributes(), BackupActivityConstants.CONFIRM_RESTORE_CV_COMPLETED)).thenReturn("true");
        when(activityUtilMock.getActivityJobAttributeValue(activityJobAttributesMapMock, ActivityConstants.ACTIVITY_RESULT)).thenReturn("");
        when(neJobAttributesMapMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtilMock.getActivityJobId(notificationSubject)).thenReturn(123L);
        objectUnderTest.processNotification(notification);

    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notification);
        verify(activityUtilMock, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());

    }

    @Test
    public void testHandleTimeoutShouldReturnFailIfActionExecutionFailed() throws JobDataNotFoundException {
        final ActivityStepResult mockResult = new ActivityStepResult();
        mockResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        when(cvUtilMock.isValidCvMO(cvMoMock)).thenReturn(true);
        when(activityUtilMock.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)).thenReturn(mockResult);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        final ActivityStepResult result = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertTrue(result.getActivityResultEnum() == (ActivityStepResultEnum.TIMEOUT_RESULT_FAIL));
    }

    @Test
    public void testHandleTimeoutShouldReturnSuccessIfActionExecutionSuccess() throws JobDataNotFoundException {
        setConfigVersionMo();
        final ActivityStepResult mockResult = new ActivityStepResult();
        mockResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        activityJobAttributesMapMock = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> activityJobPropertyAttribute = new HashMap<String, String>();
        activityJobPropertyAttribute.put(ActivityConstants.ACTION_ID, String.valueOf(actionId));
        activityJobPropertiesList.add(activityJobPropertyAttribute);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertiesList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMapMock);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(neJobStaticDataMock.getNodeName()).thenReturn(neName);
        when(neJobStaticDataMock.getNeJobId()).thenReturn(neJobId);
        when(cvServiceMock.getCvMOFromNode(neName)).thenReturn(cvMoMock);
        when(configurationVersionUtils.getActionId(activityJobAttributesMapMock)).thenReturn(actionId);
        when(configurationVersionUtils.retrieveActionResultDataWithAddlInfo(Matchers.anyMap()))
                .thenReturn(new CvActionMainAndAdditionalResultHolder(actionId, CVActionMainResult.EXECUTED, null, new ArrayList<CvActionAdditionalInfo>()));
        when(cvUtilMock.isValidCvMO(cvMoMock)).thenReturn(true);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(activityUtilMock.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)).thenReturn(mockResult);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        final ActivityStepResult result = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertTrue(result.getActivityResultEnum() == (ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS));
        verify(activityUtilMock).persistStepDurations(eq(activityJobId), Matchers.anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    private void setActivityJobPo() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobConfPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(activityUtilMock.getPoAttributes(123L)).thenReturn(activityJobAttr);
    }

    private void setConfigVersionMo() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IDLE");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "AWAITING_RESTORE_CONFIRMATION");
        cvMoAttr.put(ActivityConstants.ACTIVITY_STATUS, ActivityStatus.ON_GOING);
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "confirmRestore");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        actionResult.put(ActivityConstants.ACTIVITY_STATUS, ActivityStatus.ON_GOING);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, actionId);
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvServiceMock.getCVMoAttr(eq(neName))).thenReturn(cvMoMap);
    }

    private void setConfigVersionMoForPrecheckFail() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IDLE");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, configurationVersionName);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "PUT_TO_FTP_SERVER");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, "EXECUTED");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_TIME, "Some Time");
        cvMoAttr.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(cvServiceMock.getCVMoAttr(neName)).thenReturn(cvMoMap);

    }

    private void setConfigVersionMoAsNull() {
        setActivityJobPo();
    }

    private void mockJobActivityInfo() {
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testCancel() {
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);

        objectUnderTest.cancel(activityJobId);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtilMock, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }
}
