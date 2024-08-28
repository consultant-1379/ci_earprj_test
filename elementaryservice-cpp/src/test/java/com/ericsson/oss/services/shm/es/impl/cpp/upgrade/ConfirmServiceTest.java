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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
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
public class ConfirmServiceTest extends AbstractUpgradeActivityTest {

    @Mock
    @Inject
    private ActivityUtils activityUtils;

    @Mock
    @Inject
    private UpgradePackageService upgradePackageService;

    @Mock
    private JobEnvironment jobEnvironment;

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
    SystemRecorder systemRecorder;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private Notification notification;

    @Mock
    private AbstractUpgradeActivity AbstractUpgradeActivitymock;

    @InjectMocks
    private ConfirmService objectUnderTest;

    @Mock
    @Inject
    private DpsWriterRetryProxy dpsWriterMock;

    @Mock
    private ActivityCompleteTimer activityCallBackHandlerMock;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

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

    private final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    private final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    private final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
    final long activityJobId = 1;
    final long neJobId = 2;
    final long mainJobId = 4;
    final String upMoFdn = "Some FDN";
    final String nodeName = "Some Node";
    final String businessKey = "Some Business Key";

    @Before
    public void setup() throws JobDataNotFoundException, MoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPreCheckWithEmptyMoList() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        final List<String> attributeNames = new ArrayList<String>();
        attributeNames.add(UpgradePackageMoConstants.UP_MO_STATE);
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CONFIRM.toLowerCase())).thenReturn(true);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(Matchers.anyLong(), Matchers.any(NEJobStaticData.class), Matchers.anyString(),
                (ActivityStepResultEnum) Matchers.any());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(PRECHECK_START_PROGRESS_PERCENTAGE));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPreCheckHavingMoWithValidState() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityAttributesMap);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };

        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.toString());

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityPoAttrMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CONFIRM.toLowerCase())).thenReturn(true);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtilMock, times(2)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(PRECHECK_START_PROGRESS_PERCENTAGE));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPreCheckHavingMoWithStateAsUpgradeCompleted() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityAttributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_COMPLETED.toString());
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CONFIRM.toLowerCase())).thenReturn(true);
        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, activityPoAttrMap, nePoAttrMap);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtilMock, times(2)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(PRECHECK_START_PROGRESS_PERCENTAGE));
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPreCheckHavingMoWithOtherInvalidState() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.CONFIRM), JobLogLevel.INFO.toString()));
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, neJobId);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityAttributesMap);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.toString());

        final Map<String, Object> administrativeData = new HashMap<String, Object>();
        administrativeData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER, "R5L01");
        administrativeData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION, "CXP");

        upMoData.put(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA, administrativeData);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, activityPoAttrMap, nePoAttrMap);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CONFIRM.toLowerCase())).thenReturn(true);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtils, times(2)).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator, times(2)).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(),
                (List<Map<String, Object>>) Matchers.anyObject(), eq(PRECHECK_START_PROGRESS_PERCENTAGE));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPreCheckWithTbacFailed() throws JobDataNotFoundException, MoNotFoundException {
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CONFIRM.toLowerCase())).thenReturn(false);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtilMock, times(0)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(activityUtils).buildProcessVariablesForPrecheckAndNotifyWfs(Matchers.anyLong(), Matchers.any(NEJobStaticData.class), Matchers.anyString(), (ActivityStepResultEnum) Matchers.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreCheckWithEmptyMoList() throws IOException, SAXException {
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);

        final List<String> attributeNames = new ArrayList<String>();
        attributeNames.add(UpgradePackageMoConstants.UP_MO_STATE);
        final Map<String, Object> upMoData = new HashMap<String, Object>();

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData);

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreCheckHavingMoWithValidState() throws IOException, SAXException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };

        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.toString());

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityPoAttrMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreCheckHavingMoWithStateAsUpgradeCompleted() throws IOException, SAXException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_COMPLETED.toString());
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, activityPoAttrMap, nePoAttrMap);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreCheckHavingMoWithOtherInvalidState() throws IOException, SAXException {
        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        activityAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.toString());

        final Map<String, Object> administrativeData = new HashMap<String, Object>();
        administrativeData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER, "R5L01");
        administrativeData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION, "CXP");

        upMoData.put(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA, administrativeData);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoData, activityPoAttrMap, nePoAttrMap);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());

        final String logMessage = "Confirm upgrade failed for " + activityJobId + " because upgrade package [ProductNumber="
                + administrativeData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER) + ",ProductRevision="
                + administrativeData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION) + "] is not in awaiting confirmation. Current UP state is: "
                + upMoData.get(UpgradePackageMoConstants.UP_MO_STATE);

        verify(activityUtils, times(1)).recordEvent(SHMEvents.CONFIRM_PRECHECK, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
    }

    @Test
    public void testExecuteWithMoFdnFromNeJobProperties() {
        new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.ACTION_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(0));
        jobPropertyList.add(jobProperty);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), "confirm")).thenReturn(2000);
        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        Mockito.doNothing().when(systemRecorder).recordCommand("CONFIRM_SERVICE", CommandPhase.STARTED, nodeName, upMoFdn, Long.toString(mainJobId));

        final String actionType = UpgradeActivityConstants.ACTION_CONFIRM_UPGRADE;
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(dpsWriterMock.performAction(upMoFdn, actionType, actionArguments)).thenReturn(3);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, ConfirmService.class)).thenReturn(jobActivityInfoMock);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ERBS");
        networkElement.setName(jobEnvironment.getNodeName());
        networkElementsList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(jobEnvironment.getNodeName()))).thenReturn(networkElementsList);

        objectUnderTest.execute(activityJobId);
        fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, jobActivityInfoMock);
        verify(notificationRegistry, times(1)).register(fdnNotificationSubject);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_START_PROGRESS_PERCENTAGE));
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_END_PROGRESS_PERCENTAGE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMoFdnFromDps() throws IOException, SAXException {
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> activityPoAttrMap = new HashMap<String, Object>();
        activityPoAttrMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityPoAttrMap);
        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        Mockito.doNothing().when(systemRecorder).recordCommand("CONFIRM_SERVICE", CommandPhase.STARTED, nodeName, upMoFdn, Long.toString(mainJobId));

        final String actionType = UpgradeActivityConstants.ACTION_CONFIRM_UPGRADE;
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(dpsWriterMock.performAction(upMoFdn, actionType, actionArguments)).thenReturn(3);

        Mockito.when(activityUtils.getActivityInfo(activityJobId, ConfirmService.class)).thenReturn(jobActivityInfoMock);
        mockReadAttributeRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.toString(), "confirm")).thenReturn(2000);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ERBS");
        networkElement.setName(jobEnvironment.getNodeName());
        networkElementsList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(jobEnvironment.getNodeName()))).thenReturn(networkElementsList);

        objectUnderTest.execute(activityJobId);
        fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, jobActivityInfoMock);
        verify(notificationRegistry, times(1)).register(fdnNotificationSubject);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_START_PROGRESS_PERCENTAGE));
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_END_PROGRESS_PERCENTAGE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithUnableToPerformAction() {
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        Mockito.doNothing().when(systemRecorder).recordCommand("CONFIRM_SERVICE", CommandPhase.STARTED, nodeName, upMoFdn, Long.toString(mainJobId));

        final IllegalArgumentException ex = new IllegalArgumentException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, ConfirmService.class)).thenReturn(jobActivityInfoMock);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ERBS");
        networkElement.setName(jobEnvironment.getNodeName());
        networkElementsList.add(networkElement);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(jobEnvironment.getNodeName()))).thenReturn(networkElementsList);

        objectUnderTest.execute(activityJobId);
        fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, ConfirmService.class));
        verify(notificationRegistry, times(1)).register(fdnNotificationSubject);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_START_PROGRESS_PERCENTAGE));
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_END_PROGRESS_PERCENTAGE));
    }

    @Test
    public void testExecuteWithMediationServiceException() {
        mockJobActivityInfo();
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);

        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
        neJobPropertyList.add(neJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, neJobPropertyList);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(nePoAttrMap);

        Mockito.doNothing().when(notificationRegistry).register(fdnNotificationSubject);

        final List<Map<String, Object>> mainJobPropertyList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(mainJobPropertyList);

        Mockito.doNothing().when(systemRecorder).recordCommand("CONFIRM_SERVICE", CommandPhase.STARTED, nodeName, upMoFdn, Long.toString(mainJobId));

        final IllegalArgumentException ex = new IllegalArgumentException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(ex);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, ConfirmService.class)).thenReturn(jobActivityInfoMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenThrow(NullPointerException.class);
        objectUnderTest.execute(activityJobId);
        fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, ConfirmService.class));
        verify(notificationRegistry, times(1)).register(fdnNotificationSubject);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(eq(activityJobId), Matchers.anyList(), (List<Map<String, Object>>) Matchers.anyObject(),
                eq(MOACTION_START_PROGRESS_PERCENTAGE));

    }

    @Test
    public void testProcessNotificationWithProgressCountOnly() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> progressCountMap = new HashMap<String, Object>();
        progressCountMap.put("notifiableAttributeValue", 1);
        progressCountMap.put("previousNotifiableAttributeValue", 0);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_COUNT)).thenReturn(progressCountMap);

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.PROG_COUNT);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "0");
        activityJobPropertyList.add(activityJobProperty);
        activityAttributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityAttributesMap);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @Test
    public void testProcessNotificationWithProgressTotalOnly() {

        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> progressTotalMap = new HashMap<String, Object>();
        progressTotalMap.put("notifiableAttributeValue", 10);
        progressTotalMap.put("previousNotifiableAttributeValue", 8);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_TOTAL)).thenReturn(progressTotalMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.PROG_TOTAL);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "8");
        activityJobPropertyList.add(activityJobProperty);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @Test
    public void testProcessNotificationWithProgressHeaderOnly() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> progressHeaderMap = new HashMap<String, Object>();
        progressHeaderMap.put("notifiableAttributeValue", "DOWNLOADING_FILES");
        progressHeaderMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.PROG_HEADER);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "IDLE");
        activityJobPropertyList.add(activityJobProperty);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_PROGRESS_HEADER_ADD_INFO };
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        final List<String> progHeaderAdditionalInfo = new ArrayList<String>();
        progHeaderAdditionalInfo.add("Additional Info 1");
        progHeaderAdditionalInfo.add("Additional Info 2");
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROGRESS_HEADER_ADD_INFO, progHeaderAdditionalInfo);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);

    }

    @Test
    public void testProcessNotificationWithStateAsUpgradeExecuting() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("notifiableAttributeValue", UpgradePackageState.UPGRADE_EXECUTING.toString());
        stateMap.put("previousNotifiableAttributeValue", "INSTALL_COMPLETED");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_STATE);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "INSTALL_COMPLETED");
        activityJobPropertyList.add(activityJobProperty);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithStateAsUpgradeCompleted() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("notifiableAttributeValue", UpgradePackageState.UPGRADE_COMPLETED.toString());
        stateMap.put("previousNotifiableAttributeValue", UpgradePackageState.UPGRADE_EXECUTING.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upMoAttr = new HashMap<String, Object>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, "EXECUTED");
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "additional info");
        actionResultDataList.add(actionResultData);
        upMoAttr.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoAttr);

        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, UpgradePackageState.UPGRADE_COMPLETED.toString());
        jobPropertyList.add(jobProperty);

        final Map<String, String> upMoData = new HashMap<String, String>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_COMPLETED.toString());
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "IDLE");

        final Map<String, Object> neJobIdMap = new HashMap<String, Object>();
        neJobIdMap.put(ShmConstants.NE_JOB_ID, activityJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(neJobIdMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn, upMoFdn, jobPropertyList, upMoData);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @Test
    public void testProcessNotificationWithStateAsInstallCompleted() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("notifiableAttributeValue", "INSTALL_COMPLETED");
        stateMap.put("previousNotifiableAttributeValue", UpgradePackageState.UPGRADE_EXECUTING.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_STATE);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UpgradePackageState.UPGRADE_EXECUTING.toString());
        activityJobPropertyList.add(activityJobProperty);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @Test
    public void testProcessNotificationWithAllNotifiableAttributes() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> progressCountMap = new HashMap<String, Object>();
        progressCountMap.put("notifiableAttributeValue", 1);
        progressCountMap.put("previousNotifiableAttributeValue", 0);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_COUNT)).thenReturn(progressCountMap);

        final Map<String, Object> progressTotalMap = new HashMap<String, Object>();
        progressTotalMap.put("notifiableAttributeValue", 10);
        progressTotalMap.put("previousNotifiableAttributeValue", 8);
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_TOTAL)).thenReturn(progressTotalMap);

        final Map<String, Object> progressHeaderMap = new HashMap<String, Object>();
        progressHeaderMap.put("notifiableAttributeValue", "DOWNLOADING_FILES");
        progressHeaderMap.put("previousNotifiableAttributeValue", "IDLE");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);

        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("notifiableAttributeValue", UpgradePackageState.UPGRADE_COMPLETED.toString());
        stateMap.put("previousNotifiableAttributeValue", UpgradePackageState.INSTALL_COMPLETED.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_STATE);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, UpgradePackageState.INSTALL_COMPLETED.toString());
        activityJobPropertyList.add(activityJobProperty);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithException() throws IOException, SAXException {
        final long activityJobId = 1;
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenThrow(Exception.class);

        objectUnderTest.processNotification(notification);
    }

    @Test
    public void testProcessNotificationWithNoInfoFromActionResult() throws IOException, SAXException {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("notifiableAttributeValue", UpgradePackageState.UPGRADE_COMPLETED.toString());
        stateMap.put("previousNotifiableAttributeValue", UpgradePackageState.UPGRADE_EXECUTING.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobPropertyForHeader = new HashMap<String, Object>();
        activityJobPropertyForHeader.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_STATE);
        activityJobPropertyForHeader.put(ActivityConstants.JOB_PROP_VALUE, UpgradePackageState.INSTALL_EXECUTING.toString());
        activityJobPropertyList.add(activityJobPropertyForHeader);
        final Map<String, Object> activityJobPropertyForActionId = new HashMap<String, Object>();
        activityJobPropertyForActionId.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.ACTION_ID);
        activityJobPropertyForActionId.put(ActivityConstants.JOB_PROP_VALUE, "3");
        activityJobPropertyList.add(activityJobPropertyForActionId);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT };

        final Map<String, Object> upMoAttr = new HashMap<String, Object>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "additional info");
        actionResultDataList.add(actionResultData);
        upMoAttr.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoAttr);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutSuccessScenario() throws IOException, SAXException {
        mockJobActivityInfo();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, UpgradePackageState.UPGRADE_COMPLETED.toString());
        jobPropertyList.add(jobProperty);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_PROG_HEADER };
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_COMPLETED.toString());
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "IDLE");

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);
        attributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(attributesMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoData);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, ConfirmService.class)).thenReturn(jobActivityInfoMock);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutFailureScenario1() throws IOException, SAXException {

        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "NOT_INSTALLED");
        jobPropertyList.add(jobProperty);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> attributeNames = new ArrayList<String>();
        attributeNames.add(UpgradePackageMoConstants.UP_MO_STATE);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_HEADER);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_COUNT);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_TOTAL);
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_EXECUTING.toString());
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "IDLE");

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn, upMoData);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutExceptionScenario() throws IOException, SAXException {

        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "NOT_INSTALLED");
        jobPropertyList.add(jobProperty);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> attributeNames = new ArrayList<String>();
        attributeNames.add(UpgradePackageMoConstants.UP_MO_STATE);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_HEADER);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_COUNT);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_TOTAL);
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_EXECUTING.toString());
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "IDLE");

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobPropertyList, upMoData);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutFailureScenario2() throws IOException, SAXException {
        final long activityJobId = 1;
        final long neJobId = 2;

        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "NOT_INSTALLED");
        jobPropertyList.add(jobProperty);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> attributeNames = new ArrayList<String>();
        attributeNames.add(UpgradePackageMoConstants.UP_MO_STATE);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_HEADER);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_COUNT);
        attributeNames.add(UpgradePackageMoConstants.UP_MO_PROG_TOTAL);
        final Map<String, Object> upMoData = new HashMap<String, Object>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_EXECUTING.toString());
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "EXECUTION_FAILED");

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn, upMoData);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutFailureScenario3() throws IOException, SAXException {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final Map<String, Object> upMoData = new HashMap<String, Object>();

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn, upMoData);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationWithProgressHeaderAsExecutionFailed() throws IOException, SAXException {

        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData();
        modifiedAttr.put(avc.getName(), avc);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);

        final Map<String, Object> progressHeaderMap = new HashMap<String, Object>();
        progressHeaderMap.put("notifiableAttributeValue", "EXECUTION_FAILED");
        progressHeaderMap.put("previousNotifiableAttributeValue", "DOWNLOADING_FILES");
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER)).thenReturn(progressHeaderMap);

        final Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("notifiableAttributeValue", UpgradePackageState.UPGRADE_EXECUTING.toString());
        stateMap.put("previousNotifiableAttributeValue", UpgradePackageState.UPGRADE_EXECUTING.toString());
        when(activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE)).thenReturn(stateMap);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobPropertyForHeader = new HashMap<String, Object>();
        activityJobPropertyForHeader.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.PROG_HEADER);
        activityJobPropertyForHeader.put(ActivityConstants.JOB_PROP_VALUE, "DOWNLOADING_FILES");
        activityJobPropertyList.add(activityJobPropertyForHeader);
        final Map<String, Object> activityJobPropertyForActionId = new HashMap<String, Object>();
        activityJobPropertyForActionId.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.ACTION_ID);
        activityJobPropertyForActionId.put(ActivityConstants.JOB_PROP_VALUE, "3");
        activityJobPropertyList.add(activityJobPropertyForActionId);
        attributesMap.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        attributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(attributesMap);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };

        final Map<String, Object> upMoAttr = new HashMap<String, Object>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> actionResultData = new HashMap<String, Object>();
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3);
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, "EXECUTED");
        actionResultData.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "additional info");
        actionResultDataList.add(actionResultData);
        upMoAttr.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        when(upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null)).thenReturn(upMoAttr);

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        final Map<String, Object> neJobIdMap = new HashMap<String, Object>();
        neJobIdMap.put(ShmConstants.NE_JOB_ID, activityJobId);

        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradePackageMoConstants.UP_MO_STATE);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "NOT_INSTALLED");
        jobPropertyList.add(jobProperty);

        final Map<String, String> upMoData = new HashMap<String, String>();
        upMoData.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_EXECUTING.toString());
        upMoData.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "IDLE");

        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(neJobIdMap);

        mockGeneralRetryPolicies();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(upMoFdn, upMoFdn, jobPropertyList, upMoData);

        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(1)).getNotificationTimeStamp(notificationSubject);

    }

    @Test
    public void testCancel() {
        final long activityJobId = 1;
        assertNotNull(objectUnderTest.cancel(activityJobId));
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notification);
        verify(activityUtils, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }

    @Test
    public void testprecheckHandleTimeout() {
        final int precheckTimeout = 5;
        when(activityTimeoutsServiceMock.getPrecheckTimeoutAsInteger()).thenReturn(precheckTimeout);
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.CONFIRM, precheckTimeout));
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());
        when(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.CONFIRM, precheckTimeout), JobLogLevel.ERROR.toString())).thenReturn(logEntry);

        objectUnderTest.precheckHandleTimeout(activityJobId);
        verify(activityUtils).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(logEntry);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, new ArrayList<Map<String, Object>>(), jobLogList);

    }

    protected void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
    }
}
