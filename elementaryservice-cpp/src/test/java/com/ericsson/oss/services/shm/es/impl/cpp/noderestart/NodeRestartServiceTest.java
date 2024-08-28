/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.common.NodeRestartActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.common.NodeRestartServiceRetryProxy;
import com.ericsson.oss.services.shm.es.noderestart.NodeRestartActivityTimer;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartServiceTest {

    private static final String RESTART_WARM = "RESTART_WARM";
    private static final String PLANNED_RECONFIGURATION = "PLANNED_RECONFIGURATION";

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    @Inject
    private NodeRestartUtility nodeRestartUtility;

    @Mock
    @Inject
    private FdnServiceBean fdnServiceBean;

    @Mock
    @Inject
    protected SystemRecorder systemRecorder;

    @Mock
    @Inject
    protected ActivityUtils activityUtils;

    @Mock
    @Inject
    protected JobUpdateService jobUpdateService;

    @Mock
    @Inject
    private CppNodeRestartConfigParamListener cppNodeRestartConfigParamListener;

    @Mock
    @Inject
    protected NodeRestartJobActivityInfo nodeRestartJobActivityInfo;

    @Mock
    List<NetworkElement> neElementList;

    @Mock
    NetworkElement neElement;

    @Mock
    Map<String, Object> mainJobAttributes;

    @Mock
    Map<String, Object> jobConfigurationDetails;

    @InjectMocks
    NodeRestartService nodeRestartService;

    @Mock
    JobEnvironment jobEnvironment;

    @Mock
    @Inject
    JobPropertyUtils jobPropertyUtils;

    @Mock
    Map<String, String> actionArguments11;

    @Mock
    ActivityInfo activityInfoAnnotation;

    @Mock
    TimerService timerServiceMock;

    @Mock
    Timer timerMock;

    @Mock
    private NodeRestartActivityHandler nodeRestartActivityHandler;

    @Mock
    private NodeRestartServiceRetryProxy restoreCvServiceRetryProxy;

    @Mock
    private JobActivityInfo activityInfo;

    @Mock
    private NodeRestartActivityTimer nodeRestartActivityTimer;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Mock
    private ActivityTimeoutsService activityTimeoutsService;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;
    Map<String, Object> nodeRestartMoAttributesMap = new HashMap<String, Object>();
    int actionId = 5;

    Map<String, Object> templateJobAttr;
    Map<String, Object> mainJobAttr;
    Map<String, Object> neJobAttributes;

    String neName = "Some Ne Name";
    String nodeFdn = "Some Mo Fdn";
    String configurationVersionName = "Some CV Name";
    String identity = "Some Identity";
    String neType = "Standard";
    String platformType = "CPP";
    String operatorName = "Some Operator Name";
    String comment = "Some Comment";
    String jobName = "Some Job Name";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    String jobExecutionUser = "Test_user";

    @Test
    public void testPrecheckWithNoManagedElement() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(nodeRestartUtility.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "manualrestart")).thenReturn(true);
        when(nodeRestartActivityHandler.getActivityType()).thenReturn(RestartActivityConstants.RESTART_NODE);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, nodeRestartService.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
    }

    @Test
    public void testPrecheckWithManagedElement() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        nodeRestartMoAttributesMap.put(ShmConstants.FDN, "nodeFdn");
        nodeRestartMoAttributesMap.put(ShmConstants.MO_ATTRIBUTES, new HashMap<String, Object>());
        when(restoreCvServiceRetryProxy.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "manualrestart")).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, nodeRestartService.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator, times(2)).updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testAsyncPrecheckWithNoManagedElement() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(nodeRestartUtility.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "manualrestart")).thenReturn(true);
        nodeRestartService.asyncPrecheck(activityJobId);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        Mockito.verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testAsyncPrecheckWhenTBACFailed() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(nodeRestartUtility.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "manualrestart")).thenReturn(false);
        nodeRestartService.asyncPrecheck(activityJobId);
        Mockito.verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testAsyncPrecheckThrowsException() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(nodeRestartUtility.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        when(nodeRestartActivityHandler.getActivityType()).thenReturn(ActivityConstants.NODE_RESTART_ACTIVITY_NAME);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenThrow(new JobDataNotFoundException("JobData Not Found"));
        nodeRestartService.asyncPrecheck(activityJobId);
        Mockito.verify(activityUtils, times(1)).handleExceptionForPrecheckScenarios(activityJobId, ActivityConstants.NODE_RESTART_ACTIVITY_NAME, "JobData Not Found");
        Mockito.verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testPrecheckHandleTimeout() {
        final long activityJobId = 1;
        nodeRestartService.precheckHandleTimeout(activityJobId);
        verify(activityUtils).failActivityForPrecheckTimeoutExpiry(activityJobId, RestartActivityConstants.RESTART_NODE);
    }

    @Test
    public void testAsyncPrecheckWithManagedElement() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        nodeRestartMoAttributesMap.put(ShmConstants.FDN, "nodeFdn");
        nodeRestartMoAttributesMap.put(ShmConstants.MO_ATTRIBUTES, new HashMap<String, Object>());
        when(restoreCvServiceRetryProxy.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "manualrestart")).thenReturn(true);
        nodeRestartService.asyncPrecheck(activityJobId);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator, times(2)).updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        Mockito.verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.NODE_RESTART_ACTIVITY_NAME,
                ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteperformActionStatusSuccess() throws JobDataNotFoundException {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(nodeRestartUtility.getManagedElementFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementList);
        when(mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationDetails);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getNeType()).thenReturn(neType);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final Map<String, String> actionArguments = new HashMap<String, String>();
        actionArguments.put(RestartActivityConstants.RESTART_RANK, RESTART_WARM);
        actionArguments.put(RestartActivityConstants.RESTART_REASON, PLANNED_RECONFIGURATION);
        actionArguments.put(RestartActivityConstants.RESTART_INFO, "manual restarting node for testing purpose");
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType)).thenReturn(actionArguments);
        when(cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(2);
        when(cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType)).thenReturn(2);
        when(nodeRestartUtility.performAction(nodeFdn, jobEnvironment.getNodeName(), Collections.<String, Object> unmodifiableMap(actionArguments))).thenReturn(true);
        when(activityInfoAnnotation.activityName()).thenReturn("noderestart");
        when(activityInfoAnnotation.jobType()).thenReturn(JobTypeEnum.NODERESTART);
        when(activityInfoAnnotation.platform()).thenReturn(PlatformTypeEnum.CPP);
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        nodeRestartService.execute(activityJobId);

    }

    private NodeRestartJobActivityInfo getActivityInfo(final long activityJobId, final int maxTimeForCppNodeRestart, final int waitIntervalForEachRetry, final int cppNodeRestartSleepTime) {

        final ActivityInfo activityInfoAnnotation = NodeRestartService.class.getAnnotation(ActivityInfo.class);
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = new NodeRestartJobActivityInfo(activityJobId, activityInfoAnnotation.activityName(), activityInfoAnnotation.jobType(),
                activityInfoAnnotation.platform(), maxTimeForCppNodeRestart, waitIntervalForEachRetry, cppNodeRestartSleepTime);
        return nodeRestartJobActivityInfo;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteperformActionStatusFail() throws JobDataNotFoundException {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);

        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(nodeRestartUtility.getManagedElementFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementList);
        when(mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationDetails);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getNeType()).thenReturn(neType);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final Map<String, String> actionArguments = new HashMap<String, String>();
        actionArguments.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArguments.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArguments.put(RestartActivityConstants.RESTART_INFO, "manual restarting node for testing purpose");
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType)).thenReturn(actionArguments);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(neType, platformType, jobName, "manualrestart")).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(2);
        when(cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType)).thenReturn(2);
        when(nodeRestartUtility.performAction(nodeFdn, jobEnvironment.getNodeName(), Collections.<String, Object> unmodifiableMap(actionArguments))).thenReturn(true);
        Map<String, Object> args = new HashMap<String, Object>();
        for (Entry<String, String> keys : actionArguments.entrySet()) {
            args.put(keys.getKey(), keys.getValue());
        }
        when(activityInfoAnnotation.activityName()).thenReturn("noderestart");
        when(activityInfoAnnotation.jobType()).thenReturn(JobTypeEnum.NODERESTART);
        when(activityInfoAnnotation.platform()).thenReturn(PlatformTypeEnum.CPP);
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        NodeRestartJobActivityInfo jobActivityInfo = getActivityInfo(activityJobId, 2, 2, 2);
        nodeRestartActivityHandler.executeNodeRestartAction(jobEnvironment, args, jobActivityInfo);

        nodeRestartService.execute(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteThrowsException() throws JobDataNotFoundException {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(nodeRestartUtility.getManagedElementFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementList);
        when(mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationDetails);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getNeType()).thenReturn(neType);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final Map<String, String> actionArguments = new HashMap<String, String>();
        actionArguments.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArguments.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArguments.put(RestartActivityConstants.RESTART_INFO, "manual restarting node for testing purpose");
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType)).thenReturn(actionArguments);
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(cppNodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(2);
        when(cppNodeRestartConfigParamListener.getNodeRestartSleepTime(neType)).thenReturn(2);
        Map<String, Object> args = new HashMap<String, Object>();
        for (Entry<String, String> keys : actionArguments.entrySet()) {
            args.put(keys.getKey(), keys.getValue());
        }
        when(activityInfoAnnotation.activityName()).thenReturn("noderestart");
        when(activityInfoAnnotation.jobType()).thenReturn(JobTypeEnum.NODERESTART);
        when(activityInfoAnnotation.platform()).thenReturn(PlatformTypeEnum.CPP);
        NodeRestartJobActivityInfo jobActivityInfo = getActivityInfo(activityJobId, 2, 2, 2);
        Mockito.doThrow(new NullPointerException()).when(nodeRestartActivityHandler).executeNodeRestartAction(Matchers.any(), Matchers.any(), Matchers.any());

        nodeRestartService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(jobExecutionUser, SHMEvents.CPP_NODE_RESTART_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, neName, ActivityConstants.EMPTY,
                "Unable to proceed \"Node Restart\" activity because \"null\".");
    }

    @Test
    public void testHandleTimeoutwithNodeStartedException() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        mockActivityJobAttributes();
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE)).thenReturn(true);
        assertNotNull(nodeRestartService.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testOnActionCompletewithNodeStartedFail() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE)).thenReturn(false);
    }

    @Test
    public void testHandleTimeoutwithNodeStartedSuccess() {
        mockActivityJobAttributes();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE)).thenReturn(true);
        assertNotNull(nodeRestartService.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));

    }

    @Test
    public void testHandleTimeoutwithNodeStartedFail() {
        mockActivityJobAttributes();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        when(nodeRestartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE)).thenReturn(false);
        assertNotNull(nodeRestartService.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));

    }

    @Test
    public void testCancel() {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(nodeRestartActivityHandler.cancelNodeRestartAction(jobEnvironment)).thenReturn(activityStepResult);
        nodeRestartService.cancel(activityJobId);
        verify(nodeRestartActivityHandler, times(1)).cancelNodeRestartAction(jobEnvironment);
    }

    @Test
    public void testCancelTimeoutwithNodeStartedSuccess() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(nodeRestartUtility.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(true);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        when(nodeRestartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.FALSE)).thenReturn(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, nodeRestartService.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutForOnGoingNodeRestartActivityFail() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        when(nodeRestartActivityHandler.cancelTimeout(false, jobEnvironment, ActivityConstants.FALSE)).thenReturn(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING, nodeRestartService.cancelTimeout(activityJobId, false).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutFail() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(nodeRestartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.FALSE)).thenReturn(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, nodeRestartService.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @Test
    public void testOnActionCompletewithNodeStartedSuccess() {
        final long activityJobId = 11111l;
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(mainJobAttributes);
        List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        Map<String, Object> activityAttributes = new HashMap<String, Object>();
        activityAttributes.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED);
        activityAttributes.put(ActivityConstants.JOB_PROP_VALUE, ActivityConstants.RESTORE);
        when(mainJobAttributes.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(mainJobAttributes.get(ShmConstants.ACTIVITY_START_DATE)).thenReturn(new Date());
        nodeRestartService.onActionComplete(activityJobId);
    }

    private void mockActivityJobAttributes() {
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
    }

    @Test
    public void testPrecheckThrowsException() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(nodeRestartUtility.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        when(nodeRestartActivityHandler.getActivityType()).thenReturn(ActivityConstants.NODE_RESTART_ACTIVITY_NAME);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenThrow(new JobDataNotFoundException("JobData Not Found"));
        nodeRestartService.precheck(activityJobId);
        Mockito.verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, Collections.EMPTY_LIST, null);
    }

    @Test
    public void testPrecheckWhenTBACFailed() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(nodeRestartUtility.getManagedElementAttributes(neName)).thenReturn(nodeRestartMoAttributesMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        // when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        //when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        // when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        // when(nodeRestartActivityHandler.getActivityType()).thenReturn(ActivityConstants.NODE_RESTART_ACTIVITY_NAME);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "manualrestart")).thenReturn(false);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenThrow(new JobDataNotFoundException("JobData Not Found"));
        nodeRestartService.precheck(activityJobId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, nodeRestartService.precheck(activityJobId).getActivityResultEnum());
    }
}