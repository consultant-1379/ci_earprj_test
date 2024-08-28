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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CVActionResultInformation;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionAdditionalInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionMainAndAdditionalResultHolder;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class VerifyRestoreHandlerTest {

    @Mock
    JobUpdateService jobUpdateServiceMock;

    @Mock
    ActivityUtils activityUtilMock;

    @Mock
    SystemRecorder systemRecoderMock;

    @Mock
    ConfigurationVersionUtils cvUtilMock;

    @Mock
    @Inject
    SystemRecorder systemRecorder;

    @Mock
    ConfigurationVersionService cvServiceMock;

    @InjectMocks
    VerifyRestoreHandler verifyRestoreHandler;

    @Mock
    Notification notification;

    @Mock
    NotificationSubject notificationSubject;

    @Mock
    CvActivity cvActivityNewMock;

    @Mock
    CvActivity cvActivityOldMock;

    @Mock
    CvActivity cvActivityMock;

    @Mock
    CommonCvOperations cvOperationsMock;

    @Mock
    ConfigurationVersionMO cvMoMock;

    @Mock
    CvActionMainAndAdditionalResultHolder cvActionResultMock;

    @Mock
    CvActionAdditionalInfo cvActionAdditionalInfoMock;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    JobPropertyUtils jobPropertyUtils;

    @Mock
    JobEnvironment jobEnvMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    long activityJobId = 1234;
    String nodeName = "TestErbs";
    String businessKey = "businesskey";
    String cvName = "TestCV";
    String cvMoFdn = "CvMoFDN";
    int invokedActionId = 1234;
    int actionIdFromNode = 23;
    JobEnvironment jobEnv = null;
    String jobExecutionUser = "TEST_USER";
    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

    @Test
    public void testInvokeVerifyRestoreShouldReturnTrueIfActionInvocationSuccess() {

        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(cvUtilMock.isCvFunctionBusy(cvActivityMock)).thenReturn(false);

        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);

        Mockito.when(cvOperationsMock.executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMoFdn, actionArguments)).thenReturn(invokedActionId);

        mockJobEnvironment();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.RESTORE.toString(), "verify")).thenReturn(2000);
        Assert.assertTrue(verifyRestoreHandler.invokeVerifyRestore(cvName, jobEnvMock, jobActivityInfoMock));
        Mockito.verify(cvOperationsMock).executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMoFdn, actionArguments);
        //Mockito.verify(activityUtilMock).subscribeToMoNotifications(cvMoFdn, activityJobId, activityUtilMock.getActivityInfo(activityJobId, VerifyRestoreService.class));
        Mockito.verify(activityUtilMock).addJobProperty(ActivityConstants.ACTION_ID, String.valueOf(invokedActionId), new ArrayList<Map<String, Object>>());
    }

    private void mockJobEnvironment() {
        Mockito.when(jobEnvMock.getActivityJobId()).thenReturn(activityJobId);
        Mockito.when(jobEnvMock.getNodeName()).thenReturn(nodeName);
    }

    @Test
    public void testInvokeVerifyRestoreShouldReturnFalseIfActionInvocationThrowException() {

        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(cvUtilMock.isCvFunctionBusy(cvActivityMock)).thenReturn(false);
        mockJobEnvironment();
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);

        Mockito.when(cvOperationsMock.executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMoFdn, actionArguments)).thenThrow(new RuntimeException());
        Mockito.when(activityUtilMock.getActivityInfo(activityJobId, VerifyRestoreService.class)).thenReturn(jobActivityInfoMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertFalse(verifyRestoreHandler.invokeVerifyRestore(cvName, jobEnvMock, jobActivityInfoMock));
        Mockito.verify(cvOperationsMock).executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMoFdn, actionArguments);
        //Mockito.verify(activityUtilMock).unSubscribeToMoNotifications(cvMoFdn, activityJobId, jobActivityInfoMock);
    }

    @Test
    public void testInvokeVerifyRestoreWithMediationServiceException() {

        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(cvUtilMock.isCvFunctionBusy(cvActivityMock)).thenReturn(false);
        mockJobEnvironment();
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);
        when(activityUtilMock.getJobExecutionUser(jobEnvMock.getMainJobId())).thenReturn(jobExecutionUser);
        Mockito.when(cvOperationsMock.executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMoFdn, actionArguments)).thenThrow(new RuntimeException());
        Mockito.when(activityUtilMock.getActivityInfo(activityJobId, VerifyRestoreService.class)).thenReturn(jobActivityInfoMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertFalse(verifyRestoreHandler.invokeVerifyRestore(cvName, jobEnvMock, jobActivityInfoMock));
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.VERIFY_RESTORE_CV, CommandPhase.STARTED, nodeName, cvMoFdn, "CV name:" + cvName);
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.VERIFY_RESTORE_CV, CommandPhase.FINISHED_WITH_ERROR, nodeName, cvMoFdn, "Cv name:" + cvName);
        Mockito.verify(cvOperationsMock).executeActionOnMo(BackupActivityConstants.ACTION_VERIFY_RESTORE_CV, cvMoFdn, actionArguments);
        //Mockito.verify(activityUtilMock).unSubscribeToMoNotifications(cvMoFdn, activityJobId, jobActivityInfoMock);
    }

    //@Test
    public void testVerifyRestoreTimedOutShouldReturnFailedIfActionIdDifferent() {

        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnvMock)).thenReturn(invokedActionId);
        Mockito.when(cvMoMock.getCvMainActionResultHolder()).thenReturn(cvActionResultMock);
        Mockito.when(cvActionResultMock.getCvActionMainResult()).thenReturn(CVActionMainResult.EXECUTION_FAILED);
        //Mockito.when(activityUtilMock.getMainJobAttributeValue(jobEnv.getMainJobAttributes(), BackupActivityConstants.FORCED_RESTORE, jobEnv.getNodeName())).thenReturn(""); //restore not selected in the same job of verify
        Mockito.when(cvActionResultMock.getActionId()).thenReturn(23);
        Mockito.when(activityUtilMock.getNodeName(activityJobId)).thenReturn(nodeName);
        Mockito.when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        Mockito.when(neElementListMock.get(0)).thenReturn(neElementMock);
        Mockito.when(neElementMock.getNeType()).thenReturn("ERBS");
        Mockito.when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Assert.assertTrue(JobResult.FAILED == verifyRestoreHandler.verifyRestoreTimedOut(cvName, jobEnvMock, jobActivityInfoMock));
        Mockito.verify(activityUtilMock).unSubscribeToMoNotifications(cvMoFdn, jobEnvMock.getActivityJobId(), activityUtilMock.getActivityInfo(activityJobId, VerifyRestoreService.class));
    }

    //@Test
    public void testVerifyRestoreTimedOutShouldReturnFailedIfActionIdSameAndExecutionFailed() {

        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnvMock)).thenReturn(invokedActionId);
        Mockito.when(cvMoMock.getCvMainActionResultHolder()).thenReturn(cvActionResultMock);
        Mockito.when(cvActionResultMock.getCvActionMainResult()).thenReturn(CVActionMainResult.EXECUTION_FAILED);
        //Mockito.when(activityUtilMock.getMainJobAttributeValue(jobEnv.getMainJobAttributes(), BackupActivityConstants.FORCED_RESTORE, jobEnv.getNodeName())).thenReturn(""); //restore not selected in the same job of verify
        Mockito.when(cvActionResultMock.getActionId()).thenReturn(invokedActionId);
        Mockito.when(activityUtilMock.getNodeName(activityJobId)).thenReturn(nodeName);
        Mockito.when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        Mockito.when(neElementListMock.get(0)).thenReturn(neElementMock);
        Mockito.when(neElementMock.getNeType()).thenReturn("ERBS");
        Mockito.when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Assert.assertTrue(JobResult.FAILED == verifyRestoreHandler.verifyRestoreTimedOut(cvName, jobEnvMock, jobActivityInfoMock));
        Mockito.verify(activityUtilMock).unSubscribeToMoNotifications(cvMoFdn, jobEnvMock.getActivityJobId(), activityUtilMock.getActivityInfo(activityJobId, VerifyRestoreService.class));
    }

    public void testHandleNotificationShouldPersistNodeSupportedRestoreIfFrocedRestoreChosen() {

        final List<CvActionAdditionalInfo> cvActionAdditionalInfo = new ArrayList<CvActionAdditionalInfo>();
        Mockito.when(cvUtilMock.getNewCvActivity(notification)).thenReturn(cvActivityNewMock);
        Mockito.when(cvUtilMock.getOldCvActivity(notification)).thenReturn(cvActivityOldMock);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnv)).thenReturn(invokedActionId);
        Mockito.when(cvUtilMock.isInvokedActionResultNotified(invokedActionId, notification)).thenReturn(true);
        cvActionAdditionalInfo.add(cvActionAdditionalInfoMock);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnv)).thenReturn(invokedActionId);
        Mockito.when(cvMoMock.getCvMainActionResultHolder()).thenReturn(cvActionResultMock);
        Mockito.when(cvActionResultMock.getCvActionMainResult()).thenReturn(CVActionMainResult.EXECUTED);
        Mockito.when(activityUtilMock.getNodeName(activityJobId)).thenReturn(nodeName);
        Mockito.when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        Mockito.when(neElementListMock.get(0)).thenReturn(neElementMock);
        Mockito.when(neElementMock.getNeType()).thenReturn("ERBS");
        Mockito.when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        //jobPropertyUtils.getPropertyValue(BackupActivityConstants.FORCED_RESTORE, jobConfigurationDetails, neName);
        //Mockito.when(jobPropertyUtils.getPropertyValue(Matchers.eq(BackupActivityConstants.FORCED_RESTORE), Matchers.anyMap(), Matchers.eq(jobEnv.getNodeName()))).thenReturn("true"); //restore not selected in the same job of verify
        Mockito.when(cvActionResultMock.getActionId()).thenReturn(invokedActionId);
        Mockito.when(cvActionResultMock.getActionAdditionalResult()).thenReturn(cvActionAdditionalInfo);
        Mockito.when(cvActionAdditionalInfoMock.getInformation()).thenReturn(CVActionResultInformation.ACTION_RESTORE_IS_ALLOWED);
        Mockito.when(cvActionAdditionalInfoMock.getAdditionalInformation()).thenReturn("additional Information");
        verifyRestoreHandler.onVerifyRestoreCompleted(jobEnv, cvMoMock);
        Mockito.verify(activityUtilMock).addJobProperty(BackupActivityConstants.FORCED_RESTORE, "false", new ArrayList<Map<String, Object>>());

    }

    //@Test
    public void testHandleNotificationShouldFaileIfNodeSupportsForcedRestoreAndRestoreChosen() {

        final List<CvActionAdditionalInfo> cvActionAdditionalInfo = new ArrayList<CvActionAdditionalInfo>();
        Mockito.when(cvUtilMock.getNewCvActivity(notification)).thenReturn(cvActivityNewMock);
        Mockito.when(cvUtilMock.getOldCvActivity(notification)).thenReturn(cvActivityOldMock);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnv)).thenReturn(invokedActionId);
        Mockito.when(cvUtilMock.isInvokedActionResultNotified(invokedActionId, notification)).thenReturn(true);
        cvActionAdditionalInfo.add(cvActionAdditionalInfoMock);
        Mockito.when(cvServiceMock.getCvMOFromNode(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getCvActivity()).thenReturn(cvActivityMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(cvMoFdn);
        Mockito.when(activityUtilMock.getPersistedActionId(jobEnv)).thenReturn(invokedActionId);
        Mockito.when(cvMoMock.getCvMainActionResultHolder()).thenReturn(cvActionResultMock);
        Mockito.when(cvActionResultMock.getCvActionMainResult()).thenReturn(CVActionMainResult.EXECUTED);
        Mockito.when(activityUtilMock.getNodeName(activityJobId)).thenReturn(nodeName);
        Mockito.when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        Mockito.when(neElementListMock.get(0)).thenReturn(neElementMock);
        Mockito.when(neElementMock.getNeType()).thenReturn("ERBS");
        Mockito.when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        //Mockito.when(activityUtilMock.getMainJobAttributeValue(jobEnv.getMainJobAttributes(), BackupActivityConstants.FORCED_RESTORE, jobEnv.getNodeName())).thenReturn("false"); //restore not selected in the same job of verify
        //Mockito.when(jobPropertyUtils.getPropertyValue(Matchers.eq(BackupActivityConstants.FORCED_RESTORE), Matchers.anyMap(), Matchers.eq(jobEnv.getNodeName()))).thenReturn("false");
        Mockito.when(cvActionResultMock.getActionId()).thenReturn(invokedActionId);
        Mockito.when(cvActionResultMock.getActionAdditionalResult()).thenReturn(cvActionAdditionalInfo);
        Mockito.when(cvActionAdditionalInfoMock.getInformation()).thenReturn(CVActionResultInformation.ACTION_FORCED_RESTORE_IS_ALLOWED);
        Mockito.when(cvActionAdditionalInfoMock.getAdditionalInformation()).thenReturn("additional Information");
        final JobResult jobResult = verifyRestoreHandler.onVerifyRestoreCompleted(jobEnv, cvMoMock);
        Assert.assertTrue(JobResult.FAILED == jobResult);

    }

    @Before
    public void setUpJobEnvironment() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final long neJobId = 12345l;
        final long mainJobId = 6789l;
        Mockito.when(activityUtilMock.getPoAttributes(mainJobId)).thenReturn(neJobAttributes);
        Mockito.when(activityUtilMock.getPoAttributes(neJobId)).thenReturn(activityJobAttributes);
        jobEnv = new JobEnvironment(activityJobId, activityUtilMock);

        Mockito.when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        Mockito.when(neElementListMock.get(0)).thenReturn(neElementMock);
        Mockito.when(neElementMock.getNeType()).thenReturn("ERBS");
        Mockito.when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
    }
}
