package com.ericsson.oss.services.shm.backupservice.cpp.remote;

//*------------------------------------------------------------------------------
// *******************************************************************************
// * COPYRIGHT Ericsson 2012
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// *----------------------------------------------------------------------------*/

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.Semaphore;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notification.common.NotificationInformation;
import com.ericsson.oss.services.shm.notification.common.RemoteActivityNotificationHelper;
import com.ericsson.oss.services.shm.notifications.api.*;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(PowerMockRunner.class)
public class UploadCVRemoteServiceImplTest {

    @Mock
    @Inject
    private CommonCvOperations commonCvOperations;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
    private NotificationRegistry notificationRegistry;

    @Mock
    @Inject
    private ActivityUtils activityUtils;

    @InjectMocks
    private UploadCVRemoteServiceImpl uploadCVRemoteServiceImplMock;

    @Mock
    private NotificationCallbackResult result;

    @Mock
    private RemoteActivityNotificationHelper activityNotificationHelpermock;

    @Mock
    private FdnNotificationSubject fdnNotificationSubjectMock;

    @Mock
    private NotificationCallbackResult notificationCallbackResultMock;

    @Mock
    private Notification notificationMock;

    @Mock
    private DpsAttributeChangedEvent dpsAttributeChangedEventMock;

    @Mock
    private NotificationInformation notificationInformation;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private BackupUtils backupActionResultUtilityMock;

    @Mock
    private Semaphore semaphoreMock;

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private Map<String, AttributeChangeData> modifiedAttr;

    @Mock
    private ConfigurationVersionService configurationVersionService;

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckOnMo() {
        when(commonCvOperations.precheckForUploadCVAction(Matchers.anyMap(), Matchers.anyMap())).thenReturn(true);
        assertTrue(uploadCVRemoteServiceImplMock.precheckOnMo(Matchers.anyMap(), Matchers.anyMap()));
    }

    @Test
    public void testExecuteAction() {
        final String nodeName = "Some Node Name";
        final String cvMoFdn = "Some Cv Mo Fdn";
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        final String cvActionMainResultAsString = "success";
        final String actionType = BackupActivityConstants.ACTION_UPLOAD_CV;
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, cvActionMainResultAsString);
        final Map<String, Object> map = new HashMap<String, Object>();
        when(activityNotificationHelpermock.subscribeToNotification(nodeName, cvMoFdn, UploadCVRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, cvMoFdn, ":Proceeding to upload CV by service request");
        Mockito.doNothing().when(activityNotificationHelpermock).unSubscribeToNotification(fdnNotificationSubjectMock, cvMoFdn);
        when(activityNotificationHelpermock.waitForProcessNotifications(cvMoFdn)).thenReturn(notificationCallbackResultMock);
        when(commonCvOperations.executeActionOnMo(actionType, cvMoFdn, map)).thenReturn(1);
        when(fdnNotificationSubjectMock.getNotificationCallBackResult()).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(true);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(true);
        when(backupActionResultUtilityMock.isJobSuccess(actionResultData)).thenReturn(true);
        final int resultstatus = uploadCVRemoteServiceImplMock.executeAction(cvMoFdn, nodeName, map);
        Assert.assertEquals(1, resultstatus);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteThrowsException() {
        final String nodeName = "Some Node Name";
        final String cvMoFdn = "Some Cv Mo Fdn";
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        final String cvActionMainResultAsString = "success";
        final String actionType = BackupActivityConstants.ACTION_UPLOAD_CV;
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, cvActionMainResultAsString);
        final Map<String, Object> map = new HashMap<String, Object>();
        when(activityNotificationHelpermock.subscribeToNotification(nodeName, cvMoFdn, UploadCVRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, cvMoFdn, ":Proceeding to upload CV by service request");
        Mockito.doNothing().when(activityNotificationHelpermock).unSubscribeToNotification(fdnNotificationSubjectMock, cvMoFdn);
        when(activityNotificationHelpermock.waitForProcessNotifications(cvMoFdn)).thenReturn(notificationCallbackResultMock);
        when(commonCvOperations.executeActionOnMo(actionType, cvMoFdn, map)).thenThrow(CVOperationRemoteException.class);
        final int resultstatus = uploadCVRemoteServiceImplMock.executeAction(cvMoFdn, nodeName, map);
        Assert.assertEquals(0, resultstatus);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testprocessNotification_WhenResultSuccess() throws UnsupportedFragmentException, MoNotFoundException {
        final int activityJobId = 1;
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        final String cvActionMainResultAsString = "success";
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, cvActionMainResultAsString);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsAttributeChangedEventMock);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsAttributeChangedEventMock);
        when(backupActionResultUtilityMock.getActionResultData(Matchers.anyMap())).thenReturn(actionResultData);
        when(notificationMock.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(activityNotificationHelpermock.getNotificationInformation(Matchers.anyString())).thenReturn(notificationInformation);
        when(notificationInformation.getNotificationCallbackResult()).thenReturn(notificationCallbackResultMock);
        final Set<AttributeChangeData> attrs = new LinkedHashSet<AttributeChangeData>();
        final AttributeChangeData firstAvc = new AttributeChangeData(BackupActivityConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE, CVCurrentMainActivity.EXPORTING_BACKUP_CV,
                "deltaRemoved", "deltaAdded");
        attrs.add(firstAvc);
        final AttributeChangeData secondAvc = new AttributeChangeData(BackupActivityConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.EXPORTING_BACKUP_CV, CVCurrentMainActivity.IDLE,
                "deltaRemoved", "deltaAdded");
        attrs.add(secondAvc);
        when(notificationCallbackResultMock.getActionId()).thenReturn(activityJobId);
        when(fdnNotificationSubjectMock.getNotificationCallBackResult()).thenReturn(notificationCallbackResultMock);
        when(dpsAttributeChangedEventMock.getChangedAttributes()).thenReturn(attrs);
        when(backupActionResultUtilityMock.isCorrectActionResult(actionResultData, notificationCallbackResultMock)).thenReturn(true);
        when(backupActionResultUtilityMock.isJobSuccess(actionResultData)).thenReturn(true);
        when(notificationInformation.getPermit()).thenReturn(semaphoreMock);
        Mockito.doNothing().when(semaphoreMock).release();
        uploadCVRemoteServiceImplMock.processNotification(notificationMock);
        Mockito.verify(notificationCallbackResultMock).setSuccess(true);

    }

    @Test
    public void testExecuteFail() {
        final String nodeName = "Some Node Name";
        final String cvMoFdn = "Some Cv Mo Fdn";
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        final String cvActionMainResultAsString = "success";
        final String actionType = BackupActivityConstants.ACTION_UPLOAD_CV;
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, cvActionMainResultAsString);
        final Map<String, Object> map = new HashMap<String, Object>();
        when(activityNotificationHelpermock.subscribeToNotification(nodeName, cvMoFdn, UploadCVRemoteServiceImpl.class)).thenReturn(fdnNotificationSubjectMock);
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, cvMoFdn, ":Proceeding to upload CV by service request");
        Mockito.doNothing().when(activityNotificationHelpermock).unSubscribeToNotification(fdnNotificationSubjectMock, cvMoFdn);
        when(activityNotificationHelpermock.waitForProcessNotifications(cvMoFdn)).thenReturn(notificationCallbackResultMock);
        when(commonCvOperations.executeActionOnMo(actionType, cvMoFdn, map)).thenReturn(1);
        when(fdnNotificationSubjectMock.getNotificationCallBackResult()).thenReturn(notificationCallbackResultMock);
        when(notificationCallbackResultMock.isSuccess()).thenReturn(false);
        when(notificationCallbackResultMock.isActionTimedOut()).thenReturn(false);
        when(backupActionResultUtilityMock.isJobSuccess(actionResultData)).thenReturn(true);
        final int resultstatus = uploadCVRemoteServiceImplMock.executeAction(cvMoFdn, nodeName, map);
        Assert.assertEquals(0, resultstatus);
    }
}
