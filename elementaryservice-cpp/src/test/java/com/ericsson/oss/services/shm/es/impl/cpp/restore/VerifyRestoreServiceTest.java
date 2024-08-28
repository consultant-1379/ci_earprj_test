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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class VerifyRestoreServiceTest {

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private RestorePrecheckHandler restorePrecheckHandlerMock;

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
    private VerifyRestoreHandler verifyRestoreHandlerMock;

    @InjectMocks
    private VerifyRestoreService verifyRestoreService;

    @Mock
    private JobEnvironment jobEnvMock;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private ConfigurationVersionMO cvMoMock;

    @Mock
    private Notification notification;

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private Map<String, Object> neJobAttributesMapMock;

    @Mock
    private Map<String, String> mapMock;

    @Mock
    private Map<String, Object> activityJobAttributesMapMock;

    @Mock
    private ActivityCompleteTimer activityCompleteTimerMock;

    @Mock
    private CvActivity cvActivityNewMock;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private FdnServiceBean fdnServiceBean;

    @Mock
    private NetworkElement neMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    JobPropertyUtils jobPropertyUtils;
    long activityJobId = 1234;
    String nodeName = "TestErbs";

    private final String businessKey = "businesskey";
    private static final String VERIFY_ACTIVITY_NAME = "verify";
    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

    @Test
    public void testPrecheckPass() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.when(restorePrecheckHandlerMock.getRestorePrecheckResult(11111111l, ActivityConstants.VERIFY_RESTORE_CV, ActivityConstants.VERIFY)).thenReturn(activityStepResult);
        final ActivityStepResult activityStepResultOutput = verifyRestoreService.precheck(11111111l);
        Assert.assertTrue(activityStepResultOutput == activityStepResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteShouldReturnSucessIfActionInvocationSuccess() {
        final ActivityStepResult mockResult = new ActivityStepResult();
        final String cvName = "testCV";
        mockResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvUtilMock.isValidCvMO(cvMoMock)).thenReturn(true);
        when(activityUtilMock.getActivityInfo(activityJobId, VerifyRestoreService.class)).thenReturn(jobActivityInfoMock);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(mapMock);
        when(mapMock.get(BackupActivityConstants.CV_NAME)).thenReturn(cvName);
        Mockito.when(verifyRestoreHandlerMock.invokeVerifyRestore(cvName, jobEnvMock, jobActivityInfoMock)).thenReturn(true);
        Mockito.when(activityUtilMock.getActivityStepResult(ActivityStepResultEnum.EXECUTION_SUCESS)).thenReturn(mockResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        verifyRestoreService.execute(activityJobId);
        verify(activityUtilMock, times(1)).getJobEnvironment(activityJobId);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_END_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testExecuteShouldReturnFailIfActionInvocationFail() {
        final ActivityStepResult mockResult = new ActivityStepResult();
        final String cvName = "testCV";
        mockResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvUtilMock.isValidCvMO(cvMoMock)).thenReturn(true);
        Mockito.when(verifyRestoreHandlerMock.invokeVerifyRestore(cvName, jobEnvMock, jobActivityInfoMock)).thenReturn(false);
        Mockito.when(activityUtilMock.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED)).thenReturn(mockResult);
        verifyRestoreService.execute(activityJobId);
        verify(activityUtilMock, times(1)).getJobEnvironment(activityJobId);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testHandleTimeoutShouldReturnFailIfActionExecutionFailed() {
        final ActivityStepResult mockResult = new ActivityStepResult();
        final String cvName = "testCV";
        mockResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvUtilMock.isValidCvMO(cvMoMock)).thenReturn(true);
        Mockito.when(verifyRestoreHandlerMock.verifyRestoreTimedOut(eq(cvName), eq(jobEnvMock), Mockito.any(JobActivityInfo.class))).thenReturn(JobResult.FAILED);
        Mockito.when(activityUtilMock.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)).thenReturn(mockResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult result = verifyRestoreService.handleTimeout(activityJobId);
        Assert.assertTrue(result.getActivityResultEnum() == (ActivityStepResultEnum.TIMEOUT_RESULT_FAIL));
        Mockito.verify(activityUtilMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutShouldReturnSuccessIfActionExecutionSuccess() {
        final ActivityStepResult mockResult = new ActivityStepResult();
        final String cvName = "testCV";
        mockResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        Mockito.when(fdnServiceBean.getNetworkElementsByNeNames(anyList())).thenReturn(Arrays.asList(neMock));
        Mockito.when(neMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(mapMock);
        Mockito.when(mapMock.get(BackupActivityConstants.CV_NAME)).thenReturn(cvName);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvUtilMock.isValidCvMO(cvMoMock)).thenReturn(true);
        Mockito.when(verifyRestoreHandlerMock.verifyRestoreTimedOut(eq(cvName), eq(jobEnvMock), Mockito.any(JobActivityInfo.class))).thenReturn(JobResult.SUCCESS);
        Mockito.when(activityUtilMock.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)).thenReturn(mockResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult result = verifyRestoreService.handleTimeout(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
        verify(activityUtilMock, times(1)).getActivityInfo(anyLong(), eq(VerifyRestoreService.class));
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtilMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testonActionCompletedShouldSendActivateToWFSIfActionExecutionSuccess() {
        final JobResult jobResult = JobResult.SUCCESS;
        Mockito.when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubject);
        Mockito.when(activityUtilMock.getActivityJobId(fdnNotificationSubject)).thenReturn(activityJobId);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(jobEnvMock.getNeJobAttributes()).thenReturn(neJobAttributesMapMock);
        Mockito.when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributesMapMock);
        Mockito.when(activityUtilMock.getActivityJobAttributeValue(jobEnvMock.getActivityJobAttributes(), BackupActivityConstants.VERIFY_RESTORE_CV_COMPLETED)).thenReturn("false");
        Mockito.when(activityUtilMock.getActivityJobAttributeValue(activityJobAttributesMapMock, ActivityConstants.ACTIVITY_RESULT)).thenReturn("");
        Mockito.when(neJobAttributesMapMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        Mockito.when(verifyRestoreHandlerMock.onVerifyRestoreCompleted(jobEnvMock, cvMoMock)).thenReturn(jobResult);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(jobEnvMock.getActivityJobId()).thenReturn(activityJobId);
        Mockito.when(cvMoMock.getFdn()).thenReturn("CVMO-FDN");
        Mockito.when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(Matchers.anyInt(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);
        verifyRestoreService.onActionComplete(activityJobId);
        Mockito.verify(activityUtilMock).sendNotificationToWFS(jobEnvMock, activityJobId, ActivityConstants.VERIFY_RESTORE_CV, null);
    }

    @Test
    public void testProcessNotificationShouldStartTimerIfActionResultNotified() {
        final int invokedActionId = 1;
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        Mockito.when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubject);
        Mockito.when(activityUtilMock.getActivityJobId(fdnNotificationSubject)).thenReturn(activityJobId);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(jobEnvMock.getNeJobAttributes()).thenReturn(neJobAttributesMapMock);
        Mockito.when(neJobAttributesMapMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        Mockito.when(activityUtilMock.getActivityInfo(activityJobId, verifyRestoreService.getClass())).thenReturn(jobActivityInfoMock);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnvMock)).thenReturn(invokedActionId);
        Mockito.when(cvUtilMock.getNewCvActivity(notification)).thenReturn(cvActivityNewMock);
        Mockito.when(cvActivityNewMock.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.UNKNOWN);
        Mockito.when(cvActivityNewMock.getMainActivity()).thenReturn(CVCurrentMainActivity.UNKNOWN);
        Mockito.when(cvUtilMock.isInvokedActionResultNotified(anyInt(), (Notification) anyObject())).thenReturn(true);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(jobEnvMock.getActivityJobId()).thenReturn(activityJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Mockito.when(fdnNotificationSubject.getFdn()).thenReturn("CVMO-FDN");
        verifyRestoreService.processNotification(notification);
        Mockito.verify(activityCompleteTimerMock).startTimer(jobActivityInfoMock);
        Mockito.verify(activityUtilMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
    }

    @Test
    public void testProcessNotificationShouldNotStartTimerForCVActivityNotifications() {
        final int invokedActionId = 1;
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        Mockito.when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        Mockito.when(activityUtilMock.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(jobEnvMock.getNeJobAttributes()).thenReturn(neJobAttributesMapMock);
        Mockito.when(neJobAttributesMapMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        Mockito.when(activityUtilMock.getActivityInfo(activityJobId, verifyRestoreService.getClass())).thenReturn(jobActivityInfoMock);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnvMock)).thenReturn(invokedActionId);
        Mockito.when(cvUtilMock.getNewCvActivity(notification)).thenReturn(cvActivityNewMock);
        Mockito.when(cvActivityNewMock.getDetailedActivity()).thenReturn(CVCurrentDetailedActivity.ACTIVATE_ROBUST_RECONFIG_REQUESTED);
        Mockito.when(cvActivityNewMock.getMainActivity()).thenReturn(CVCurrentMainActivity.UNKNOWN);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(jobEnvMock.getActivityJobId()).thenReturn(activityJobId);
        when(fdnNotificationSubject.getFdn()).thenReturn("CVMO-FDN");

        verifyRestoreService.processNotification(notification);

        Mockito.verifyZeroInteractions(activityCompleteTimerMock);

    }

    /**
     * process Notification should only invoke timer which will evaluate the job status after time time is elapased.
     */

    @Test
    public void testProcessNotificationShouldNotSendActivateToWFS() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        Mockito.when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        Mockito.when(activityUtilMock.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        Mockito.when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
        Mockito.when(jobEnvMock.getNeJobAttributes()).thenReturn(neJobAttributesMapMock);
        Mockito.when(neJobAttributesMapMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        verifyRestoreService.processNotification(notification);

        Mockito.verifyZeroInteractions(workflowMock);
    }

    @Test
    public void testCancel() {
        when(activityUtilMock.getJobEnvironment(activityJobId)).thenReturn(jobEnvMock);

        verifyRestoreService.cancel(activityJobId);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtilMock, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        verifyRestoreService.processNotification(notification);
        verify(activityUtilMock, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }
}
