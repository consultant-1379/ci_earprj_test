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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xml.sax.SAXException;

import com.ericsson.nms.security.smrs.api.CommonAccountType;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.UpgradeJobConfigurationListener;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NetworkTypeProvider;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
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
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class InstallActivityHandlerTest extends AbstractUpgradeActivityTest {

    @Mock
    @Inject
    ActivityUtils activityUtils;

    @Mock
    UpgradePackageService upgradePackageService;

    @Mock
    JobUpdateService jobUpdateService;

    @Mock
    DpsReader dpsReader;

    @Mock
    DpsWriterRetryProxy dpsWriterMock;

    @Mock
    @NotificationTypeQualifier(type = NotificationType.JOB)
    NotificationRegistry notificationRegistry;

    @Mock
    FdnNotificationSubject fdnNotificationSubject;

    @Mock
    SystemRecorder systemRecorder;

    @Mock
    JobConfigurationService jobConfigurationService;

    @Mock
    WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    Notification notification;

    @InjectMocks
    InstallActivityHandler objectUnderTest;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    Map<String, String> productDetailsMap;

    @Mock
    Map<String, Object> mainJobAttributesMock;

    @Mock
    Map<String, Object> jobConfigurationMock;

    @Mock
    List<Map<String, Object>> listMock;

    @Mock
    SmrsFileStoreService smrsServiceUtilMock;

    @Mock
    JobPropertyUtils jobPropertyUtils;

    @Mock
    List<String> keyListMock;

    @Mock
    Map<String, String> keymapMock;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    NetworkTypeProvider networkTypeProviderMock;

    @Mock
    SmrsAccountInfo smrsAccountInfo;

    @Mock
    JobEnvironment jobEnvironment;

    @Mock
    ActivityStepResult activityStepResultMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private UpgradeJobConfigurationListener upgradeJobConfigurationListener;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NetworkElementData networkElementInfo;

    @Mock
    private JobLogUtil jobLogUtil;

    private final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    private final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    private final Map<String, Object> neJobAttributes = new HashMap<String, Object>();

    private final long neJobId = 789546l;
    private final long activityJobId = 123456l;
    private final String productNumber = null;
    private final String productRevision = null;
    private final String pkgTypeTobeLogged = null;
    private final String UPGRADE = "UPGRADE";
    private final String RESTORE = "RESTORE";
    private final String INSTALL_NOT_COMPLETED = "INSTALL_NOT_COMPLETED";

    private final Integer ACTION_ID = 3;
    final String upMoFdn = "Some FDN";
    final String nodeName = "ERBS01_Node";
    final long mainJobId = 4;
    final String businessKey = "Some Business Key";
    String userName = "UserName";
    String password = "Passw0rd";
    String ipAddress = "10.123.14.1";

    private final String ucfFilePath = "/ericsson/smrs/lran/CXP1020511_R4D26_1/CXP9012123_R4D261_1.xml";

    @Test
    public void testPreCheckWithEmptyMoList() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, UPGRADE, neJobStaticData).getActivityResultEnum());
    }

    @Test
    public void testPreCheckHavingMoWithValidCompleteState() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();
        mockUpMoAttributesWithState("INSTALL_COMPLETED");
        when(upgradeJobConfigurationListener.isSkipInstallActivityEnabled()).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, UPGRADE, neJobStaticData).getActivityResultEnum());
    }

    @Test
    public void testPreCheckHavingMoWithValidIncompleteState() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();
        mockUpMoAttributesWithState("INSTALL_EXECUTING");

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, UPGRADE, neJobStaticData).getActivityResultEnum());
    }

    @Test
    public void testPreCheckHavingMoValidStateAndTreatAsNull() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();
        mockUpMoAttributesWithState("NOT_INSTALLED");
        when(activityUtils.isTreatAs(anyString())).thenReturn(null);

    }

    @Test
    public void testPreCheckHavingStateAsNull() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();
        mockUpMoAttributesWithState(null);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, UPGRADE, neJobStaticData).getActivityResultEnum());
    }

    @Test
    public void testRestorePreCheckWithEmptyMoList() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, RESTORE, neJobStaticData).getActivityResultEnum());
    }

    @Test
    public void testRestorePreCheckHavingMoWithValidCompleteState() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();
        mockUpMoAttributesWithState("INSTALL_COMPLETED");
        when(upgradeJobConfigurationListener.isSkipInstallActivityEnabled()).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, RESTORE, neJobStaticData).getActivityResultEnum());
    }

    @Test
    public void testRestorePreCheckHavingMoWithValidIncompleteState() throws IOException, SAXException {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockGeneralRetryPolicies();
        mockUpMoAttributesWithState("INSTALL_EXECUTING");

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId, null, null, RESTORE, neJobStaticData).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithDeltaForceInstallActionType() {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockNeList();
        mockSmrsAndUcfDetails();
        mockJobConfiguration(null, "delta");
        mockUpMoAttributesWithUserNamePassword();
        objectUnderTest.execute(productNumber, productRevision, pkgTypeTobeLogged, jobActivityInfoMock);
        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithSelectivrDeltaForceInstallActionType() {

        mockActivityJobAttributes();
        mockNeAttributes(upMoFdn);
        mockNeList();
        mockSmrsAndUcfDetails();
        mockJobConfiguration(UpgradeActivityConstants.TYPE_SELECTIVE, UpgradeActivityConstants.TRANSFER_TYPE_FULL);
        mockUpMoAttributesWithUserNamePassword();
        objectUnderTest.execute(productNumber, productRevision, pkgTypeTobeLogged, jobActivityInfoMock);
        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithCreateMo() {

        mockActivityJobAttributes();
        mockNeList();
        mockSmrsAndUcfDetails();
        mockJobConfiguration(null, "delta");
        mockUpMoAttributesWithUserNamePassword();
        when(upgradePackageService.createUpgradeMO(anyLong(), anyString(), anyString())).thenReturn(upMoFdn);
        objectUnderTest.execute(productNumber, productRevision, pkgTypeTobeLogged, jobActivityInfoMock);
        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithCreateMoWithException() {
        final IllegalArgumentException ex = new IllegalArgumentException(" some error");
        mockActivityJobAttributes();
        mockNeList();
        mockSmrsAndUcfDetails();
        mockJobConfiguration(null, "delta");
        mockUpMoAttributesWithUserNamePassword();
        mockRetryPolicy();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("").thenThrow(ex);
        when(upgradePackageService.createUpgradeMO(anyLong(), anyString(), anyString())).thenThrow(Exception.class);
        when(workflowInstanceNotifier.sendActivate(anyString(), anyMap())).thenThrow(WorkflowServiceInvocationException.class);
        objectUnderTest.execute(productNumber, productRevision, pkgTypeTobeLogged, jobActivityInfoMock);

        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithCreateMoWithWorkFlowException() {

        mockActivityJobAttributes();
        mockNeList();
        mockSmrsAndUcfDetails();
        mockJobConfiguration(null, "delta");
        mockUpMoAttributesWithUserNamePassword();
        when(upgradePackageService.createUpgradeMO(123456, productNumber, productRevision)).thenThrow(Exception.class);
        objectUnderTest.execute(productNumber, productRevision, pkgTypeTobeLogged, jobActivityInfoMock);
        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMediationServiceException() {

        final long mainJobId = 4;
        final String upMoFdn = "Some FDN";
        final String nodeName = null;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        when(activityUtils.getNodeName(activityJobId)).thenReturn("neName");
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> selectiveMainJobProperty = new HashMap<String, Object>();
        selectiveMainJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SELECTIVEINSTALL);
        selectiveMainJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "not selective");
        mainJobPropertyList.add(selectiveMainJobProperty);
        final Map<String, Object> forcedMainJobProperty = new HashMap<String, Object>();
        forcedMainJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.FORCEINSTALL);
        forcedMainJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "delta");
        mainJobPropertyList.add(forcedMainJobProperty);
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        when(activityUtils.getPoAttributes(neJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);

        Mockito.doNothing().when(systemRecorder).recordCommand("INSTALL_SERVICE", CommandPhase.STARTED, nodeName, upMoFdn, Long.toString(mainJobId));
        final IllegalArgumentException ex = new IllegalArgumentException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityProperty = new HashMap<String, Object>();
        activityJobPropertyList.add(activityProperty);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        objectUnderTest.execute(productNumber, productRevision, pkgTypeTobeLogged, jobActivityInfoMock);
        verify(workflowInstanceNotifier, times(1)).sendActivate(Matchers.anyString(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithUnableToPerformAction() {

    }

    @Test
    public void testProcessCancelMoNotificationForFailureCase() {

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final Map<String, Object> upStateMap = new HashMap<String, Object>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        modifiedAttr.put(avc.getName(), avc);
        modifiedAttr.put(avc.getName(), avc);
        upStateMap.put("notifiableAttributeValue", "INSTALL_EXECUTING");
        upStateMap.put("previousNotifiableAttributeValue", "INSTALL_COMPLETED");
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        mockNotificationSubject();

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.STATE, "CANCELLING");
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_STATE);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "NOT_INSTALLED");
        activityJobPropertyList.add(activityJobProperty);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(upStateMap);
        upStateMap.put(UpgradeActivityConstants.PROG_COUNT, "progressCount");
        upStateMap.put(UpgradeActivityConstants.PROG_TOTAL, "progressTotal");
        upStateMap.put(UpgradeActivityConstants.PROG_HEADER, "progressHeader");
        objectUnderTest.processNotification(notification, null);
    }

    @Test
    public void testProcessNotificationWithAllNotifiableAttributes() throws IOException, SAXException {

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        mockAtrributeChangeData(modifiedAttr);
        mockNotificationSubject();
        mockUPProgressHeader(UpgradeProgressInformation.IDLE, UpgradeProgressInformation.DOWNLOADING_FILES, modifiedAttr);
        mockUPState(UpgradePackageState.NOT_INSTALLED, UpgradePackageState.INSTALL_EXECUTING, modifiedAttr);

        final Map<String, Object> jobState = objectUnderTest.processNotification(notification, jobActivityInfoMock);
        assertFalse((boolean) jobState.get(ActivityConstants.ACTIVITY_STATUS));

        verify(activityUtils, times(2)).addJobLog(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    public void testProcessNotificationUpstateInstallCompleted() throws IOException, SAXException {

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        mockAtrributeChangeData(modifiedAttr);
        mockNotificationSubject();
        mockUPState(UpgradePackageState.INSTALL_EXECUTING, UpgradePackageState.INSTALL_COMPLETED, modifiedAttr);
        final Map<String, Object> jobState = objectUnderTest.processNotification(notification, jobActivityInfoMock);
        assertTrue((boolean) jobState.get(ActivityConstants.ACTIVITY_STATUS));
    }

    @Test
    public void testProcessNotificationUpstateUpgradeCompleted() throws IOException, SAXException {

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        mockAtrributeChangeData(modifiedAttr);
        mockNotificationSubject();
        mockUPState(UpgradePackageState.INSTALL_EXECUTING, UpgradePackageState.UPGRADE_COMPLETED, modifiedAttr);
        final Map<String, Object> jobState = objectUnderTest.processNotification(notification, jobActivityInfoMock);
        assertTrue((boolean) jobState.get(ActivityConstants.ACTIVITY_STATUS));
    }

    @Test
    public void testProcessNotificationActionResultExecutionFailed() throws IOException, SAXException {
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        mockAtrributeChangeData(modifiedAttr);
        mockNotificationSubject();
        mockUPProgressHeader(UpgradeProgressInformation.IDLE, UpgradeProgressInformation.DOWNLOADING_FILES, modifiedAttr);
        mockUPState(UpgradePackageState.INSTALL_NOT_COMPLETED, UpgradePackageState.INSTALL_NOT_COMPLETED, modifiedAttr);
        when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(5);
        mockActionResultData(ActionResultInformation.EXECUTION_FAILED, modifiedAttr);
        final Map<String, Object> jobState = objectUnderTest.processNotification(notification, jobActivityInfoMock);
        final String jobResult = jobState.get(ActivityConstants.ACTIVITY_RESULT).toString();
        assertEquals(JobResult.FAILED.toString(), jobResult);
    }

    @Test
    public void testProcessNotificationActionResultExecuted() throws IOException, SAXException {

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        mockAtrributeChangeData(modifiedAttr);
        mockNotificationSubject();
        mockUPProgressHeader(UpgradeProgressInformation.IDLE, UpgradeProgressInformation.DOWNLOADING_FILES, modifiedAttr);
        mockUPState(UpgradePackageState.INSTALL_EXECUTING, UpgradePackageState.INSTALL_COMPLETED, modifiedAttr);
        when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(5);
        mockActionResultData(ActionResultInformation.EXECUTED, modifiedAttr);
        final Map<String, Object> jobState = objectUnderTest.processNotification(notification, jobActivityInfoMock);
        assertTrue((boolean) jobState.get(ActivityConstants.ACTIVITY_STATUS));
    }

    @Test
    public void testProcessNotificationActionResultExecutedWithWarnings() throws IOException, SAXException {

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        mockAtrributeChangeData(modifiedAttr);
        mockNotificationSubject();
        mockUPProgressHeader(UpgradeProgressInformation.IDLE, UpgradeProgressInformation.DOWNLOADING_FILES, modifiedAttr);
        mockUPState(UpgradePackageState.INSTALL_EXECUTING, UpgradePackageState.INSTALL_COMPLETED, modifiedAttr);
        when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(5);
        mockActionResultData(ActionResultInformation.EXECUTED_WITH_WARNINGS, modifiedAttr);
        final Map<String, Object> jobState = objectUnderTest.processNotification(notification, jobActivityInfoMock);
        assertTrue((boolean) jobState.get(ActivityConstants.ACTIVITY_STATUS));
    }

    @Test
    public void testHandleTimeoutSuccess() {
        mockNeAttributes(upMoFdn);
        mockUpMoAttributes("EXECUTED", "INSTALL_COMPLETED");
        mockActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(5);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(jobActivityInfoMock);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutFail() {
        mockNeAttributes(upMoFdn);
        mockUpMoAttributes("EXECUTION_FAILED", "NOT_INSTALLED");
        mockActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(jobActivityInfoMock);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutFailWithEmptyUpMOFdn() {
        final String upMoFdnAsEmpty = null;
        final String neName = "LTE01";
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdnAsEmpty);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        mockNeAttributes(upMoFdnAsEmpty);
        mockUpMoAttributes("EXECUTION_FAILED", "NOT_INSTALLED");
        mockActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(jobActivityInfoMock);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtils, times(1)).addJobLog(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    public void testCancel() {

        final String upMoFdn = "";
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.name(), "install")).thenReturn(40);
        mockGeneralRetryPolicies();
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn);
        assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, objectUnderTest.cancel(jobActivityInfoMock).getActivityResultEnum());
    }

    @Test
    public void testCancelFailure() {
        final String upMoFdn = "upMoFdn";
        final IllegalStateException illegalStateException = new IllegalStateException();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> upMoProperty = new HashMap<String, Object>();
        upMoProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        upMoProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
        neJobProperties.add(upMoProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributes);
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(illegalStateException);

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, objectUnderTest.cancel(jobActivityInfoMock).getActivityResultEnum());
    }

    private void mockNeList() {
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
    }

    private void mockSmrsAndUcfDetails() {
        when(smrsServiceUtilMock.getSmrsDetails(eq(CommonAccountType.SOFTWARE.name()), anyString(), eq(nodeName))).thenReturn(smrsAccountInfo);
        when(upgradePackageService.getUcfFile(anyString(), anyString())).thenReturn(ucfFilePath);
        when(upgradePackageService.getProductNumberAndRevision(anyString())).thenReturn(productDetailsMap);
        when(upgradePackageService.readUCFItemsFromDB(anyString(), anyString())).thenReturn(ucfFilePath);
        when(upgradePackageService.getSwPkgNameandUcfName(anyLong())).thenReturn(productDetailsMap);
        when(smrsAccountInfo.getServerIpAddress()).thenReturn(ipAddress);
        when(smrsAccountInfo.getUser()).thenReturn(userName);
        when(smrsAccountInfo.getPassword()).thenReturn(password.toCharArray());
        when(productDetailsMap.get(UpgradeActivityConstants.SWP_NAME)).thenReturn("SoftwarePackage1");
        when(productDetailsMap.get(UpgradeActivityConstants.UCF)).thenReturn("Ucf1");
        when(productDetailsMap.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER)).thenReturn("ProductNumber");
        when(productDetailsMap.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION)).thenReturn("ProductRevision");

        when(smrsAccountInfo.getSmrsRootDirectory()).thenReturn("/ericsson/smrs/lran/");
    }

    private void mockJobConfiguration(final String selectiveInstall, final String forceInstall) {

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mainJobAttributesMock);
        when(mainJobAttributesMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(jobConfigurationMock);
        when(jobPropertyUtils.getPropertyValue(anyList(), eq(jobConfigurationMock), anyString(), anyString(), anyString())).thenReturn(keymapMock);
        when(keymapMock.get(UpgradeActivityConstants.SELECTIVEINSTALL)).thenReturn(forceInstall);
        when(keymapMock.get(UpgradeActivityConstants.FORCEINSTALL)).thenReturn(forceInstall);
    }

    public void mockActivityStepResult(final ActivityStepResultEnum activityStepResultEnum) {
        final ActivityStepResult mockResult = new ActivityStepResult();
        mockResult.setActivityResultEnum(activityStepResultEnum);
        when(activityUtils.getActivityStepResult(activityStepResultEnum)).thenReturn(mockResult);
    }

    private void mockNeAttributes(final String moFdn) {
        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, moFdn);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
    }

    private void mockActivityJobAttributes() {
        final long neJobId = 2;
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        attributesMap.put(ShmConstants.NE_NAME, nodeName);
        attributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        attributesMap.put(ShmConstants.BUSINESS_KEY, "businesskey");

        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(attributesMap);

    }

    public void mockUpMoAttributesWithState(final String upMoState) {

        final Map<String, Object> upMoAttr = new HashMap<String, Object>();
        upMoAttr.put(UpgradePackageMoConstants.UP_MO_STATE, upMoState);
        final String[] attributes = { UpgradePackageMoConstants.UP_MO_STATE };
        when(upgradePackageService.getUpMoData(activityJobId, attributes, null, null)).thenReturn(upMoAttr);
        final String[] upmoAttributes = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        when(upgradePackageService.getUpMoAttributesByFdn(upMoFdn, upmoAttributes)).thenReturn(upMoAttr);
    }

    public void mockUpMoAttributesWithUserNamePassword() {
        final String[] attributes1 = { UpgradePackageMoConstants.UP_MO_STATE };
        when(upgradePackageService.getUpMoData(activityJobId, attributes1, null, null)).thenReturn(mapMock);
        final String[] attributes = { UpgradePackageMoConstants.UP_MO_USER, UpgradePackageMoConstants.UP_MO_PASSWORD, UpgradePackageMoConstants.UP_MO_FTP_SERVER_IP_ADDRESS,
                UpgradePackageMoConstants.UP_MO_UP_FILEPATH_ON_FTP_SERVER };
        when(upgradePackageService.getUpMoAttributesByFdn(upMoFdn, attributes)).thenReturn(mapMock);
        when(mapMock.get(UpgradePackageMoConstants.UP_MO_USER)).thenReturn(userName);
        when(mapMock.get(UpgradePackageMoConstants.UP_MO_PASSWORD)).thenReturn(password);
        when(mapMock.get(UpgradePackageMoConstants.UP_MO_FTP_SERVER_IP_ADDRESS)).thenReturn(ipAddress);
        when(mapMock.get(UpgradePackageMoConstants.UP_MO_UP_FILEPATH_ON_FTP_SERVER)).thenReturn(ucfFilePath);
    }

    public void mockUpMoAttributes(final String actionResultInfo, final String upMoState) {
        final List<String> attributeNames = new ArrayList<String>();
        attributeNames.add(UpgradePackageMoConstants.UP_ACTION_RESULT);
        final Map<String, Object> upMoAttr = new HashMap<String, Object>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, actionResultInfo);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO);
        actionResultDataList.add(actionResultData);
        upMoAttr.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        upMoAttr.put(UpgradePackageMoConstants.UP_MO_STATE, upMoState);
        final String[] attributes1 = { UpgradePackageMoConstants.UP_MO_STATE };
        when(upgradePackageService.getUpMoData(activityJobId, attributes1, null, null)).thenReturn(upMoAttr);
        final String[] upmoAttributes = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        when(upgradePackageService.getUpMoAttributesByFdn(upMoFdn, upmoAttributes)).thenReturn(upMoAttr);
    }

    private void mockNotificationSubject() {
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
    }

    private void mockAtrributeChangeData(final Map<String, AttributeChangeData> modifiedAttr) {
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
    }

    @Before
    public void mockJobEnvironment() throws JobDataNotFoundException, MoNotFoundException {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.when(jobActivityInfoMock.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(networkElementRetrivalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(ACTION_ID);
        when(activityUtils.isTreatAs(anyString())).thenReturn(nodeName);
    }

    public void mockUPProgressHeader(final UpgradeProgressInformation previousValue, final UpgradeProgressInformation currentValue, final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> progressHeaderMap = new HashMap<String, Object>();
        progressHeaderMap.put("notifiableAttributeValue", currentValue.toString());
        progressHeaderMap.put("previousNotifiableAttributeValue", previousValue.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), "install")).thenReturn(2000);
    }

    public void mockUPState(final UpgradePackageState previousValue, final UpgradePackageState currentValue, final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("previousNotifiableAttributeValue", previousValue.toString());
        stateMap.put("notifiableAttributeValue", currentValue.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testCancelFailureWithMediationServiceException() {

        final String upMoFdn = "upMoFdn";
        final String actionType = "cancelInstall";
        final IllegalStateException illegalStateException = new IllegalStateException();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> upMoProperty = new HashMap<String, Object>();
        upMoProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        upMoProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        neJobProperties.add(upMoProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributes);
        when(dpsWriterMock.performAction(upMoFdn, actionType, actionArguments)).thenThrow(illegalStateException);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Exception ex = new RuntimeException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(ex);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, objectUnderTest.cancel(jobActivityInfoMock).getActivityResultEnum());

    }

    @Test
    public void testprecheckHandleTimeout() {
        final long activityJobId = 1;
        final int precheckTimeout = 5;
        when(activityTimeoutsServiceMock.getPrecheckTimeoutAsInteger()).thenReturn(precheckTimeout);
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.INSTALL, precheckTimeout));
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());
        when(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.INSTALL, precheckTimeout), JobLogLevel.ERROR.toString())).thenReturn(logEntry);

        objectUnderTest.precheckHandleTimeout(activityJobId);
        verify(activityUtils).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(logEntry);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, new ArrayList<Map<String, Object>>(), jobLogList);
    }

    @Test
    public void testCancelTimeout() {
        mockNeAttributes(upMoFdn);
        mockUpMoAttributes("EXECUTED", INSTALL_NOT_COMPLETED);
        final ActivityStepResult activityStepResult = objectUnderTest.cancelTimeout(activityJobId, false);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtils, times(0)).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

    @Test
    public void testCancelTimeoutWhenRetriesExhausted() {
        mockNeAttributes(upMoFdn);
        mockUpMoAttributes("EXECUTED", INSTALL_NOT_COMPLETED);
        final ActivityStepResult activityStepResult = objectUnderTest.cancelTimeout(activityJobId, true);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtils, times(1)).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

    public void mockActionResultData(final ActionResultInformation actionResultInfo, final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> actionResultNotifiableAttribute = new HashMap<String, Object>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, actionResultInfo.toString());
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "additional info");
        actionResultDataList.add(actionResultData);

        actionResultNotifiableAttribute.put("notifiableAttributeValue", actionResultDataList);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_ACTION_RESULT)).thenReturn(actionResultNotifiableAttribute);
    }

}
