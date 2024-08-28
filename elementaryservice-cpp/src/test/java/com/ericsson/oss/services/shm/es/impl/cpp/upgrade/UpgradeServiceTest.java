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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.common.NodeReadAttributeFailedException;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
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
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class UpgradeServiceTest extends AbstractUpgradeActivityTest {

    @Mock
    @Inject
    private ActivityUtils activityUtils;

    @Mock
    private UpgradePackageService upgradePackageService;

    @Mock
    @Inject
    private JobUpdateService jobUpdateService;

    @Mock
    @Inject
    private DpsReader dpsReaderMock;

    @Mock
    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

    @Mock
    @Inject
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private Notification notification;

    @InjectMocks
    private UpgradeService objectUnderTest;

    @Mock
    @Inject
    private DpsWriterRetryProxy dpsWriterMock;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private ActivityCompleteTimer activityCallBackHandlerMock;

    List<Map<String, Object>> actionResultDataListMock = new ArrayList<>();

    Map<String, Object> actionResultData = new HashMap<>();

    @Mock
    Map<String, Object> upMoAttr;

    @Mock
    Map<String, String> mapMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    @DefaultActionRetryPolicy
    private ActionRetryPolicy moActionRetryPolicyMock;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private NetworkElementData networkElementInfo;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private PollingActivityManager pollingActivityManager;

    private static final long ACTIVITYJOB_ID = 1;
    private static final long NEJOB_ID = 2;
    private static final long MAINJOB_ID = 4;
    private static final String UPMO_FDN = "Some FDN";
    private static final String NODE_NAME = "Some Node";
    private static final String BUSINESS_KEY = "Some Business Key";
    private static final String INSTALL_NOT_COMPLETED = "INSTALL_NOT_COMPLETED";
    private static final String NOTIFIABLE_ATTRIBUTES = "notifiableAttributeValue";
    private static final String INSTALLED_COMPLETED = "INSTALL_COMPLETED";
    private static final String ACTIVITY_NAME = "upgrade";
    private static final String UPGRADE_COMPLETED = "UPGRADE_COMPLETED";
    private static final String DOWNLOADING_FILES = "DOWNLOADING_FILES";
    private static final String EVENT_NAME = "SHM.UPGRADE.UPGRADE_COMPLETED";

    private static final String INSTALL_EXECUTING = "INSTALL_EXECUTING";
    private static final String NOT_INSTALLED = "NOT_INSTALLED";

    private final Map<String, Object> activityJobAttributes = new HashMap<>();
    private final Map<String, Object> mainJobAttributes = new HashMap<>();
    private final Map<String, Object> neJobAttributes = new HashMap<>();

    @Before
    public void setup() throws JobDataNotFoundException, MoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITYJOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
        when(neJobStaticData.getNeJobId()).thenReturn(NEJOB_ID);
        when(neJobStaticData.getMainJobId()).thenReturn(MAINJOB_ID);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(BUSINESS_KEY);
        when(neJobStaticData.getActivityStartTime()).thenReturn(new Date().getTime());

        when(configurationServiceRetryProxy.getMainJobAttributes(MAINJOB_ID)).thenReturn(mainJobAttributes);
        when(configurationServiceRetryProxy.getNeJobAttributes(NEJOB_ID)).thenReturn(neJobAttributes);
        when(configurationServiceRetryProxy.getActivityJobAttributes(ACTIVITYJOB_ID)).thenReturn(activityJobAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        final Object actionId = 3;
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, actionId);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTED);
        actionResultDataListMock.add(actionResultData);

        mockData();
        mockGeneralRetryPolicies();
        Mockito.doNothing().when(activityCallBackHandlerMock).startTimer(any(JobActivityInfo.class));

    }

    @Test
    public void testPreCheckWithEmptyMoList() throws IOException, SAXException {
        final long activityJobId = 1;
        final long neJobId = 2;
        final Map<String, Object> activityAttributesMap = new HashMap<>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };

        final Map<String, Object> upMoData = new HashMap<>();
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData);

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testAsyncPreCheckWithEmptyMoList() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };

        final Map<String, Object> upMoData = new HashMap<>();
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData);
        when(jobStaticDataProvider.getJobStaticData(MAINJOB_ID)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE.toLowerCase())).thenReturn(true);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtilMock, times(3)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(activityUtils).buildProcessVariablesForPrecheckAndNotifyWfs(Matchers.anyLong(), Matchers.any(NEJobStaticData.class), Matchers.anyString(), (ActivityStepResultEnum) Matchers.any());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(PRECHECK_START_PROGRESS_PERCENTAGE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPreCheckWithTbacFailed() throws JobDataNotFoundException, MoNotFoundException {
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(ACTIVITYJOB_ID, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE.toLowerCase())).thenReturn(false);
        objectUnderTest.asyncPrecheck(ACTIVITYJOB_ID);
        verify(jobLogUtilMock, times(0)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(activityUtils).buildProcessVariablesForPrecheckAndNotifyWfs(Matchers.anyLong(), Matchers.any(NEJobStaticData.class), Matchers.anyString(), (ActivityStepResultEnum) Matchers.any());
    }

    @Test
    public void testPreCheckHavingMoWithValidState() throws IOException, SAXException {
        final long activityJobId = 1;
        final long neJobId = 2;

        final String nodeName = null;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoData = new HashMap<>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        final Map<String, Object> activityPoAttrMap = new HashMap<>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        activityPoAttrMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityPoAttrMap);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityPoAttrMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, UPMO_FDN);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testAsyncPreCheckHavingMoWithValidState() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long neJobId = 2;

        final String nodeName = null;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoData = new HashMap<>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        final Map<String, Object> activityPoAttrMap = new HashMap<>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityPoAttrMap);
        activityPoAttrMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityPoAttrMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, UPMO_FDN);
        when(jobStaticDataProvider.getJobStaticData(MAINJOB_ID)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE.toLowerCase())).thenReturn(true);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtilMock, times(3)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(activityUtils).buildProcessVariablesForPrecheckAndNotifyWfs(Matchers.anyLong(), Matchers.any(NEJobStaticData.class), Matchers.anyString(), (ActivityStepResultEnum) Matchers.any());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(PRECHECK_START_PROGRESS_PERCENTAGE));

    }

    @Test
    public void testPreCheckHavingMoWithInvalidState() throws IOException, SAXException {
        final long activityJobId = 1;
        final long neJobId = 2;

        final Map<String, Object> activityAttributesMap = new HashMap<>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoData = new HashMap<>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALL_EXECUTING);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, "", activityPoAttrMap, nePoAttrMap);

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testAsyncPreCheckHavingMoWithInvalidState() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long neJobId = 2;
        final Map<String, Object> activityAttributesMap = new HashMap<>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityAttributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };

        final Map<String, Object> upMoData = new HashMap<>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALL_EXECUTING);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, "", activityPoAttrMap, nePoAttrMap);
        when(jobStaticDataProvider.getJobStaticData(MAINJOB_ID)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE.toLowerCase())).thenReturn(true);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtilMock, times(3)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(activityUtils).buildProcessVariablesForPrecheckAndNotifyWfs(Matchers.anyLong(), Matchers.any(NEJobStaticData.class), Matchers.anyString(), (ActivityStepResultEnum) Matchers.any());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(PRECHECK_START_PROGRESS_PERCENTAGE));

    }

    @Test
    public void testExecuteWithMoFdnFromNeJobProperties() throws JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long mainJobId = 4;
        final String nodeName = null;
        final long neJobId = 2;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UPMO_FDN);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> mainJobProperty = new HashMap<>();
        mainJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.REBOOTNODEUPGRADE);
        mainJobProperty.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.CHECK_TRUE);
        mainJobPropertyList.add(mainJobProperty);
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoData = new HashMap<>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        Mockito.doNothing().when(systemRecorder).recordCommand("UPGRADE_SERVICE", CommandPhase.STARTED, nodeName, UPMO_FDN, Long.toString(mainJobId));

        final String actionType = UpgradeActivityConstants.ACTION_REBOOT_NODE_UPGRADE;
        final Map<String, Object> actionArguments = new HashMap<>();
        when(dpsWriterMock.performAction(UPMO_FDN, actionType, actionArguments)).thenReturn(3);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(nodeName), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UpgradeService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).subscribeToMoNotifications(UPMO_FDN, activityJobId, jobActivityInfoMock);
    }

    @Test
    public void testExecuteWithMoFdnFromDps() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long neJobId = 2;
        final long mainJobId = 4;
        final String nodeName = null;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> activityPoAttrMap = new HashMap<>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityPoAttrMap);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> mainJobProperty = new HashMap<>();
        mainJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.REBOOTNODEUPGRADE);
        mainJobProperty.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.CHECK_TRUE);
        mainJobPropertyList.add(mainJobProperty);
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        Mockito.doNothing().when(systemRecorder).recordCommand("UPGRADE_SERVICE", CommandPhase.STARTED, nodeName, UPMO_FDN, Long.toString(mainJobId));
        final Map<String, Object> upMoData = new HashMap<>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);
        final String actionType = UpgradeActivityConstants.ACTION_REBOOT_NODE_UPGRADE;
        final Map<String, Object> actionArguments = new HashMap<>();
        when(dpsWriterMock.performAction(UPMO_FDN, actionType, actionArguments)).thenReturn(3);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(nodeName), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);

        mockReadAttributeRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(UPMO_FDN);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UpgradeService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).subscribeToMoNotifications(UPMO_FDN, activityJobId, jobActivityInfoMock);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(MOACTION_START_PROGRESS_PERCENTAGE));

    }

    @Test
    public void testExecuteWithUpgradeOnly() throws JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long mainJobId = 4;
        final String nodeName = null;
        final long neJobId = 2;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UPMO_FDN);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        final Map<String, Object> upPoMap = new HashMap<>();
        final List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activity = new HashMap<>();
        activity.put(UpgradeActivityConstants.UP_PO_ACTIVITY_NAME, "Upgrade");
        activities.add(activity);
        upPoMap.put(UpgradeActivityConstants.UP_PO_ACTIVITIES, activities);
        when(upgradePackageService.getUpPoData(activityJobId)).thenReturn(upPoMap);

        Mockito.doNothing().when(systemRecorder).recordCommand("UPGRADE_SERVICE", CommandPhase.STARTED, nodeName, UPMO_FDN, Long.toString(mainJobId));
        final Map<String, Object> upMoData = new HashMap<>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);
        final String actionType = UpgradeActivityConstants.ACTION_UPGRADE;
        final Map<String, Object> actionArguments = new HashMap<>();
        when(dpsWriterMock.performAction(UPMO_FDN, actionType, actionArguments)).thenReturn(3);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(nodeName), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UpgradeService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).subscribeToMoNotifications(UPMO_FDN, activityJobId, jobActivityInfoMock);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(MOACTION_START_PROGRESS_PERCENTAGE));
    }

    @Test
    public void testExecuteWithUpdateOnly() throws JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long neJobId = 2;
        final long mainJobId = 4;
        final String nodeName = null;
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UPMO_FDN);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        final Map<String, Object> upMoData = new HashMap<>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        final Map<String, Object> upPoMap = new HashMap<>();
        final List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activity = new HashMap<>();
        activity.put(UpgradeActivityConstants.UP_PO_ACTIVITY_NAME, "Update");
        activities.add(activity);
        upPoMap.put(UpgradeActivityConstants.UP_PO_ACTIVITIES, activities);
        when(upgradePackageService.getUpPoData(activityJobId)).thenReturn(upPoMap);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(nodeName), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);

        Mockito.doNothing().when(systemRecorder).recordCommand("UPGRADE_SERVICE", CommandPhase.STARTED, nodeName, UPMO_FDN, Long.toString(mainJobId));

        final String actionType = UpgradeActivityConstants.ACTION_UPDATE;
        final Map<String, Object> actionArguments = new HashMap<>();
        when(dpsWriterMock.performAction(UPMO_FDN, actionType, actionArguments)).thenReturn(3);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UpgradeService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).subscribeToMoNotifications(UPMO_FDN, activityJobId, jobActivityInfoMock);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(MOACTION_START_PROGRESS_PERCENTAGE));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithUnableToPerformAction() throws JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long mainJobId = 4;
        final String nodeName = null;
        final long neJobId = 2;
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UPMO_FDN);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> mainJobProperty = new HashMap<>();
        mainJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.REBOOTNODEUPGRADE);
        mainJobProperty.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.CHECK_TRUE);
        mainJobPropertyList.add(mainJobProperty);
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        Mockito.doNothing().when(systemRecorder).recordCommand("UPGRADE_SERVICE", CommandPhase.STARTED, nodeName, UPMO_FDN, Long.toString(mainJobId));
        final Map<String, Object> upMoData = new HashMap<>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);
        final IllegalArgumentException ex = new IllegalArgumentException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(nodeName), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UpgradeService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).subscribeToMoNotifications(UPMO_FDN, activityJobId, jobActivityInfoMock);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(MOACTION_START_PROGRESS_PERCENTAGE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMediationServiceException() throws JobDataNotFoundException, MoNotFoundException {
        final long activityJobId = 1;
        final long mainJobId = 4;
        final long neJobId = 2;
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, NODE_NAME);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UPMO_FDN);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);
        final Map<String, Object> upMoData = new HashMap<>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> mainJobProperty = new HashMap<>();
        mainJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.REBOOTNODEUPGRADE);
        mainJobProperty.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.CHECK_TRUE);
        mainJobPropertyList.add(mainJobProperty);
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        Mockito.doNothing().when(systemRecorder).recordCommand("UPGRADE_SERVICE", CommandPhase.STARTED, NODE_NAME, UPMO_FDN, Long.toString(mainJobId));

        final IllegalArgumentException ex = new IllegalArgumentException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(NullPointerException.class);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(NODE_NAME), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, UpgradeService.class)).thenReturn(jobActivityInfoMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.UPGRADE_SERVICE, CommandPhase.FINISHED_WITH_ERROR, NODE_NAME, UPMO_FDN, null);
        verify(activityUtils, times(1)).subscribeToMoNotifications(UPMO_FDN, activityJobId, jobActivityInfoMock);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(), eq(MOACTION_START_PROGRESS_PERCENTAGE));
    }

    @Test
    public void testProcessNotificationWithUpStateUpgradeCompleted() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockJobActivityInfo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubject);
        when(activityUtils.getActivityJobId(fdnNotificationSubject)).thenReturn(ACTIVITYJOB_ID);

        final Map<String, Object> progressHeaderMap = new HashMap<String, Object>();
        progressHeaderMap.put(NOTIFIABLE_ATTRIBUTES, DOWNLOADING_FILES);
        progressHeaderMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);

        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put(NOTIFIABLE_ATTRIBUTES, UPGRADE_COMPLETED);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);
        Mockito.when(activityUtils.getActivityInfo(ACTIVITYJOB_ID, UpgradeService.class)).thenReturn(jobActivityInfoMock);

        when(fdnNotificationSubject.getKey()).thenReturn(UPMO_FDN).thenReturn(UPMO_FDN);

        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ActivityConstants.UPGRADE)).thenReturn(EVENT_NAME);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        objectUnderTest.processNotification(notification);

        final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS, ActivityConstants.UPGRADE);
        verify(activityUtils, times(1)).recordEvent(EVENT_NAME, NODE_NAME, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_NOTIFICATIONS));

        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
    }

    @Test
    public void testProcessNotificationStartTimerFailureInUpHeader() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(ACTIVITYJOB_ID);

        final Map<String, Object> progressHeaderMap = new HashMap<>();
        progressHeaderMap.put(NOTIFIABLE_ATTRIBUTES, "VERIFICATION_FAILED");
        progressHeaderMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);

        final Map<String, Object> stateMap = new HashMap<>();

        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        objectUnderTest.processNotification(notification);

        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);

        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.FAILED.getJobResult());

        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());

    }

    @Test
    public void testProcessNotificationStartTimerFailureInUpState() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(ACTIVITYJOB_ID);

        final Map<String, Object> progressHeaderMap = new HashMap<>();
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);

        final Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(NOTIFIABLE_ATTRIBUTES, INSTALLED_COMPLETED);
        stateMap.put("previousNotifiableAttributeValue", "VERIFICATION_EXECUTING");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        objectUnderTest.processNotification(notification);

        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());

    }

    @Test
    public void testProcessNotificationStartTimerFailureInUpState2() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(ACTIVITYJOB_ID);

        final Map<String, Object> progressHeaderMap = new HashMap<>();
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);

        final Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(NOTIFIABLE_ATTRIBUTES, "UPGRADE_EXECUTING");
        stateMap.put("previousNotifiableAttributeValue", INSTALLED_COMPLETED);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        objectUnderTest.processNotification(notification);

        verify(activityUtils, times(1)).addJobProperty(eq(UpgradeActivityConstants.UPGRADE_EXECUTING_SEEN), eq(String.valueOf(true)), Matchers.anyList());

        verify(workflowInstanceNotifier, times(0)).sendActivate(Matchers.anyString(), Matchers.anyMap());

    }

    @Test
    public void testHandleTimeout() throws IOException, SAXException {
        final long activityJobId = 1;
        final long neJobId = 2;
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, NOT_INSTALLED);
        jobPropertyList.add(jobProperty);

        final Map<String, Object> upMoData = new HashMap<>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALL_EXECUTING);
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "IDLE");

        final Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);
        attributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(attributesMap);

        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("", upMoData, jobPropertyList);

        when(activityUtils.getActivityStepResult(any(ActivityStepResultEnum.class))).thenCallRealMethod();

        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());

        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.UPGRADE);
        verify(activityUtils, times(1)).recordEvent(EVENT_NAME, NODE_NAME, UPMO_FDN, "SHM:" + activityJobId + ":" + NODE_NAME + ":" + logMessage + String.format(ActivityConstants.COMPLETION_FLOW, ActivityConstants.COMPLETED_THROUGH_TIMEOUT));
    }

    @Test
    public void test_Cancel_Succes() throws IOException, SAXException {

        final long activityJobId = 1;
        final long neJobId = 2;
        final String nodeName = null;

        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, MAINJOB_ID);

        final Map<String, Object> upPoMap = new HashMap<>();
        final List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activity = new HashMap<>();
        activity.put(UpgradeActivityConstants.UP_PO_ACTIVITY_NAME, "Upgrade");
        activities.add(activity);
        upPoMap.put(UpgradeActivityConstants.UP_PO_ACTIVITIES, activities);

        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);
        when(upgradePackageService.getUpMoFdn(activityJobId, null, null)).thenReturn(UPMO_FDN);
        when(upgradePackageService.getUpPoData(activityJobId)).thenReturn(upPoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), eq(nodeName), eq((String) null), eq("CPP"))).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.REBOOTNODEUPGRADE)).thenReturn(ActivityConstants.CHECK_TRUE);
        final ActivityStepResult activityStepResult = objectUnderTest.cancel(activityJobId);
        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, activityStepResult.getActivityResultEnum());

    }

    @Test
    public void actionCompletedExecutionFailed() {

        mockActionResultData("EXECUTION_FAILED");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);

        objectUnderTest.onActionComplete(ACTIVITYJOB_ID);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.CANCELLED.getJobResult());

    }

    @Test
    public void actionCompletedActionResultFailed() {

        mockActionResultData("DELTA_INSTALL_MERGE_ERROR");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);
        objectUnderTest.onActionComplete(ACTIVITYJOB_ID);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.FAILED.getJobResult());
    }

    @Test
    public void actionCompletedUpgradeExecutionSeenCancelTriggered() {

        mockActionResultData("EXECUTED_WITH_WARNINGS");
        mockActivityJobAttributes(true, null);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);
        objectUnderTest.onActionComplete(ACTIVITYJOB_ID);
        verify(activityUtils, times(1)).addJobLog(eq(UpgradeActivityConstants.UPGRADE_CANCELLED), Matchers.anyString(), Matchers.anyList(), eq(JobLogLevel.ERROR.toString()));
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.FAILED.getJobResult());
    }

    @Test
    public void actionCompletedUpgradeProgressHasErrors() {

        mockActionResultData("DELTA_INSTALL_MERGE_ERROR");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        mockActivityJobAttributes(true, UpgradeProgressInformation.AUE_FAILURE);
        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);
        objectUnderTest.onActionComplete(ACTIVITYJOB_ID);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.FAILED.getJobResult());
    }

    @Test
    public void unknownActionResultTest() {

        mockActionResultData("JVM_RESTART_FROM_STATE");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        mockActivityJobAttributes(true, UpgradeProgressInformation.AUE_FAILURE);
        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);
        objectUnderTest.onActionComplete(ACTIVITYJOB_ID);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.FAILED.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onActionCompleteExceptionTest() {

        mockActionResultData("JVM_RESTART_FROM_STATE");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        mockActivityJobAttributes(true, UpgradeProgressInformation.AUE_FAILURE);
        when(upgradePackageService.getUpMoAttributesByFdn(UPMO_FDN, null)).thenThrow(Exception.class);
        when(activityUtils.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, ACTIVITY_NAME)).thenReturn(EVENT_NAME);
        objectUnderTest.onActionComplete(ACTIVITYJOB_ID);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, ACTIVITYJOB_ID, "Upgrade", null);
        verify(activityUtils, times(0)).recordEvent(EVENT_NAME, UPMO_FDN, UPMO_FDN, "SHM:" + ACTIVITYJOB_ID + ":" + NODE_NAME + ":" + ActivityConstants.UPGRADE + JobResult.FAILED.getJobResult());
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }

    @Test
    public void testprecheckHandleTimeout() {
        final long activityJobId = 1;
        final int precheckTimeout = 5;
        when(activityTimeoutsServiceMock.getPrecheckTimeoutAsInteger()).thenReturn(precheckTimeout);
        final Map<String, Object> logEntry = new HashMap<>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.UPGRADE, precheckTimeout));
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());
        when(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.UPGRADE, precheckTimeout), JobLogLevel.ERROR.toString())).thenReturn(logEntry);

        objectUnderTest.precheckHandleTimeout(activityJobId);
        verify(activityUtils).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(logEntry);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, new ArrayList<Map<String, Object>>(), jobLogList, null);
    }

    @Test
    public void testCancelActionExecuted() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        mockJobActivityInfo();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        final Map<String, Object> stateMap = new HashMap<>();
        stateMap.put(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE, INSTALLED_COMPLETED);
        final Map<String, Object> previousStateMap = new HashMap<>();

        previousStateMap.put(ConfigurationVersionMoConstants.PREVIOUS_NOTIFIABLE_ATTRIBUTE, "UPGRADE_EXECUTING");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap).thenReturn(previousStateMap);

        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubject);
        when(activityUtils.getActivityJobId(fdnNotificationSubject)).thenReturn(ACTIVITYJOB_ID);
        when(activityUtils.cancelTriggered(ACTIVITYJOB_ID)).thenReturn(true);
        mockActionResultData(INSTALL_NOT_COMPLETED);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList())).thenReturn(true);
        objectUnderTest.processNotification(notification);
        verify(workflowInstanceNotifier, times(1)).sendCancelMOActionDone(Matchers.anyString(), Matchers.anyMap());
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        verify(activityUtils, times(1)).addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.getJobResult(), jobPropertyList);
    }

    @Test
    public void testCancelTimeout() {
        mockJobActivityInfo();
        mockActionResultData(INSTALL_NOT_COMPLETED);
        final ActivityStepResult activityStepResult = objectUnderTest.cancelTimeout(ACTIVITYJOB_ID, false);
        assertEquals(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING, activityStepResult.getActivityResultEnum());
        verify(activityUtils, times(0)).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

    @Test
    public void testCancelTimeoutWhenRetriesExhausted() {
        mockJobActivityInfo();
        mockActionResultData(INSTALL_NOT_COMPLETED);
        final ActivityStepResult activityStepResult = objectUnderTest.cancelTimeout(ACTIVITYJOB_ID, true);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtils, times(1)).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

    @Test
    public void testCancelTimeoutForNodeReadAttributeFailedException() {
        mockJobActivityInfo();
        mockActionResultDataForNodeReadAttributeFailedException(INSTALL_NOT_COMPLETED);
        final ActivityStepResult activityStepResult = objectUnderTest.cancelTimeout(ACTIVITYJOB_ID, true);
        assertEquals(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING, activityStepResult.getActivityResultEnum());
        verify(activityUtils, never()).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

    public void mockNeJobProperties() {

        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, MAINJOB_ID);
        neJobAttributes.put(ShmConstants.NE_NAME, NODE_NAME);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);

        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UPMO_FDN);
        neJobPropertyList.add(neJobProperty);
        neJobAttributes.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(neJobAttributes);
        when(activityUtils.getNeJobAttributes(ACTIVITYJOB_ID)).thenReturn(neJobAttributes);

    }

    public void mockActionResultData(final String actionResultInfo) {
        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoAttr = new HashMap<>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actionResultData = new HashMap<>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, actionResultInfo);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO);
        actionResultDataList.add(actionResultData);
        upMoAttr.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        upMoAttr.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(ACTIVITYJOB_ID, attributeNames, null, null)).thenReturn(upMoAttr);
        when(upgradePackageService.getUpMoAttributesByFdn(UPMO_FDN, attributeNames)).thenReturn(upMoAttr);
    }

    private void mockActionResultDataForNodeReadAttributeFailedException(final String actionResultInfo) {
        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoAttr = new HashMap<>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actionResultData = new HashMap<>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, actionResultInfo);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO);
        actionResultDataList.add(actionResultData);
        upMoAttr.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        upMoAttr.put(UpgradePackageMoConstants.UP_MO_STATE, INSTALLED_COMPLETED);
        when(upgradePackageService.getUpMoData(ACTIVITYJOB_ID, attributeNames, null, null)).thenReturn(upMoAttr);
        doThrow(new NodeReadAttributeFailedException(new NodeReadAttributeFailedException("TEST"))).when(upgradePackageService).getUpMoAttributesByFdn(UPMO_FDN, attributeNames);
    }

    public void mockActivityJobAttributes(final boolean upgradeExecutingSeen, final UpgradeProgressInformation upgradeProgressInformation) {
        final Map<String, Object> attributesMap = new HashMap<>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobPropertyForHeader = new HashMap<>();
        activityJobPropertyForHeader.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.PROG_HEADER);
        activityJobPropertyForHeader.put(ActivityConstants.JOB_PROP_VALUE, DOWNLOADING_FILES);
        activityJobPropertyList.add(activityJobPropertyForHeader);
        final Map<String, Object> activityJobPropertyForActionId = new HashMap<>();
        activityJobPropertyForActionId.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        activityJobPropertyForActionId.put(ActivityConstants.JOB_PROP_VALUE, "3");
        activityJobPropertyList.add(activityJobPropertyForActionId);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, NEJOB_ID);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(attributesMap);

        when(activityUtils.getActivityJobAttributeValue(attributesMap, UpgradeActivityConstants.UPGRADE_EXECUTING_SEEN)).thenReturn(String.valueOf(upgradeExecutingSeen));
        if (upgradeProgressInformation != null) {
            when(activityUtils.getActivityJobAttributeValue(attributesMap, UpgradeActivityConstants.FAILURE_IN_UP_HEADER)).thenReturn(upgradeProgressInformation.toString());
        } else {
            when(activityUtils.getActivityJobAttributeValue(attributesMap, UpgradeActivityConstants.FAILURE_IN_UP_HEADER)).thenReturn(null);
        }
    }

    public void mockJobEnvironment() {
        when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(3);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
    }

    public void mockData() {
        mockNeJobProperties();
        mockActivityJobAttributes(false, UpgradeProgressInformation.AUE_CONF_WITH_WARNING);
        mockJobEnvironment();

    }

    public void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.when(jobActivityInfoMock.getActivityName()).thenReturn(ActivityConstants.VERIFY);
    }
}