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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.CppNodeRestartConfigParamListener;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.NodeRestartUtility;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.RestartActivityConstants;
import com.ericsson.oss.services.shm.es.noderestart.NodeRestartActivityTimer;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartActivityHandlerTest {

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
    private CppNodeRestartConfigParamListener nodeRestartConfigParamListener;

    @Mock
    protected NodeRestartJobActivityInfo nodeRestartJobActivityInfo;

    @Mock
    List<NetworkElement> neElementList;

    @Mock
    NetworkElement neElement;

    @Mock
    Map<String, Object> mainJobAttributes;

    @Mock
    Map<String, Object> jobConfigurationDetails;

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
    NodeRestartActivityTimer nodeRestartActivityTimer;

    @InjectMocks
    NodeRestartActivityHandler restartActivityHandler;

    @Mock
    private NodeRestartServiceRetryProxy restoreCvServiceRetryProxy;

    @Mock
    private ConfigurationVersionUtils configurationVersionUtils;

    @Mock
    private JobActivityInfo activityInfo;

    @Mock
    JobLogUtil jobLogUtil;

    @Mock
    private ConfigurationVersionService configurationVersionService;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Mock
    private ActivityTimeoutsService activityTimeoutsService;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;
    Map<String, Object> nodeRestartMoAttributesMap = new HashMap<>();
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
    String jobExecutionUser = "TEST_USER";
    String cvName = "Some CV name";

    @Test
    public void testHandleTimeoutwithNodeStartedSuccess() throws JobDataNotFoundException {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(neName)).thenReturn(true);
        assertNotNull(restartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE));

    }

    @Test
    public void testHandleTimeoutwithNodeStartedFail() throws JobDataNotFoundException {
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(false);
        assertNotNull(restartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE));

    }

    @Test
    public void testHandleTimeoutwithNodeStartedException() throws JobDataNotFoundException {
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        assertNotNull(restartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.FALSE));
    }

    @Test
    public void testOnActionCompletewithNodeStartedSuccess() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(true);
        restartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE);

    }

    @Test
    public void testOnActionCompletewithNodeStartedFail() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(false);
        restartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE);
    }

    @Test
    public void testOnActionCompletewithNodeStartedException() throws JobDataNotFoundException {
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        restartActivityHandler.getActionCompletionStatus(jobEnvironment, ActivityConstants.TRUE);
    }

    @Test
    public void testCancelTimeoutwithNodeStartedSuccess() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionUtils.getNeJobPropertyValue(jobEnvironment.getMainJobAttributes(), jobEnvironment.getNodeName(), BackupActivityConstants.CV_NAME)).thenReturn(cvName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(true);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, restartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.FALSE).getActivityResultEnum());
        verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList());
    }

    @Test
    public void testCancelTimeoutwithNodeStartedFail_1() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(false);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, restartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.TRUE).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutwithNodeStartedFail_2() throws JobDataNotFoundException {
        final String cvName = configurationVersionUtils.getNeJobPropertyValue(jobEnvironment.getMainJobAttributes(), jobEnvironment.getNodeName(), BackupActivityConstants.CV_NAME);

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(true);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, restartActivityHandler.cancelTimeout(false, jobEnvironment, ActivityConstants.TRUE).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutwithNodeStartedFail() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(restoreCvServiceRetryProxy.isNodeReachable(jobEnvironment.getNodeName())).thenReturn(false);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        assertEquals(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING, restartActivityHandler.cancelTimeout(false, jobEnvironment, ActivityConstants.TRUE).getActivityResultEnum());
    }

    @Test
    public void testCancel() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        restartActivityHandler.cancelNodeRestartAction(jobEnvironment);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteperformActionStatusSuccess() {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(restoreCvServiceRetryProxy.getManagedObjectFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementList);
        when(mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationDetails);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getNeType()).thenReturn(neType);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final Map<String, String> actionArguments = new HashMap<>();
        actionArguments.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArguments.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArguments.put(RestartActivityConstants.RESTART_INFO, "manual restarting nodeor tesdting purpose");
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType)).thenReturn(actionArguments);
        Map<String, Object> args = new HashMap<>();
        for (final String key : actionArguments.keySet()) {
            args.put(key, args.get(key));
        }
        when(restoreCvServiceRetryProxy.performAction(neName, nodeFdn, RestartActivityConstants.ACTION_NAME, Collections.<String, Object> unmodifiableMap(args))).thenReturn(true);
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(nodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(10);
        when(activityInfoAnnotation.activityName()).thenReturn("noderestart");
        when(activityInfoAnnotation.jobType()).thenReturn(JobTypeEnum.NODERESTART);
        when(activityInfoAnnotation.platform()).thenReturn(PlatformTypeEnum.CPP);
        restartActivityHandler.executeNodeRestartAction(jobEnvironment, args, nodeRestartJobActivityInfo);
        verify(activityUtils, times(0)).sendNotificationToWFS(jobEnvironment, activityJobId, "Node Restart", null);
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteperformActionStatusFail() {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(restoreCvServiceRetryProxy.getManagedObjectFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementList);
        when(mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationDetails);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getNeType()).thenReturn(neType);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final Map<String, String> actionArguments = new HashMap<>();
        actionArguments.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArguments.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArguments.put(RestartActivityConstants.RESTART_INFO, "manual restarting nodeor tesdting purpose");
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType)).thenReturn(actionArguments);
        Map<String, Object> args = new HashMap<>();
        for (final String key : actionArguments.keySet()) {
            args.put(key, args.get(key));
        }
        when(restoreCvServiceRetryProxy.performAction(neName, nodeFdn, "nodeRestart", Collections.<String, Object> unmodifiableMap(args))).thenReturn(false);

        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(nodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(10);
        when(activityInfoAnnotation.activityName()).thenReturn("noderestart");
        when(activityInfoAnnotation.jobType()).thenReturn(JobTypeEnum.NODERESTART);
        when(activityInfoAnnotation.platform()).thenReturn(PlatformTypeEnum.CPP);
        restartActivityHandler.executeNodeRestartAction(jobEnvironment, args, nodeRestartJobActivityInfo);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "Node Restart", null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithException() {

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(restoreCvServiceRetryProxy.getManagedObjectFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementList);
        when(mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationDetails);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getNeType()).thenReturn(neType);
        final List<String> keyList = new ArrayList<>();
        keyList.add(RestartActivityConstants.RESTART_RANK);
        keyList.add(RestartActivityConstants.RESTART_REASON);
        keyList.add(RestartActivityConstants.RESTART_INFO);
        final Map<String, String> actionArguments = new HashMap<>();
        actionArguments.put(RestartActivityConstants.RESTART_RANK, "RESTART_WARM");
        actionArguments.put(RestartActivityConstants.RESTART_REASON, "PLANNED_RECONFIGURATION");
        actionArguments.put(RestartActivityConstants.RESTART_INFO, "manual restarting nodeor tesdting purpose");
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType)).thenReturn(actionArguments);
        Map<String, Object> args = new HashMap<>();
        for (final String key : actionArguments.keySet()) {
            args.put(key, args.get(key));
        }
        when(restoreCvServiceRetryProxy.performAction(neName, nodeFdn, "manualRestart", Collections.<String, Object> unmodifiableMap(args))).thenReturn(false);
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(10);
        when(nodeRestartConfigParamListener.getCppNodeRestartRetryInterval()).thenReturn(10);
        when(activityInfoAnnotation.activityName()).thenReturn("noderestart");
        when(activityInfoAnnotation.jobType()).thenReturn(JobTypeEnum.NODERESTART);
        when(activityInfoAnnotation.platform()).thenReturn(PlatformTypeEnum.CPP);
        restartActivityHandler.executeNodeRestartAction(jobEnvironment, args, nodeRestartJobActivityInfo);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "Node Restart", null);
    }

    @Test
    public void testRestoreActionCompletedSuccess() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(nodeRestartActivityTimer.isWaitTimeElapsed(activityJobId)).thenReturn(false);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        final Map<String, Object> moAttributesMap = new HashMap<>();
        Map<String, Object> cvAttrs = new HashMap<>();
        cvAttrs.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, configurationVersionName);
        moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, cvAttrs);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName);
        when(restoreCvServiceRetryProxy.getManagedObjectFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(moAttributesMap);
        restartActivityHandler.isRestoreActionCompleted(jobEnvironment, ActivityConstants.TRUE);
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(nodeRestartActivityTimer, times(0)).isWaitTimeElapsed(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testRestoreActionCompletedInProgress() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        final Map<String, Object> moAttributesMap = new HashMap<>();
        Map<String, Object> cvAttrs = new HashMap<>();
        cvAttrs.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, configurationVersionName);
        moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, cvAttrs);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName + "s");
        when(restoreCvServiceRetryProxy.getManagedObjectFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(moAttributesMap);
        when(nodeRestartActivityTimer.isWaitTimeElapsed(activityJobId)).thenReturn(false);
        restartActivityHandler.isRestoreActionCompleted(jobEnvironment, ActivityConstants.TRUE);
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(nodeRestartActivityTimer, times(1)).isWaitTimeElapsed(activityJobId);
        verify(activityUtils, times(0)).prepareJobPropertyList(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testRestoreActionCompletedFailed() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        final Map<String, Object> moAttributesMap = new HashMap<>();
        Map<String, Object> cvAttrs = new HashMap<>();
        cvAttrs.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, configurationVersionName);
        moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, cvAttrs);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(configurationVersionName + "s");
        when(restoreCvServiceRetryProxy.getManagedObjectFdn(Matchers.anyString())).thenReturn(nodeFdn);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(moAttributesMap);
        when(nodeRestartActivityTimer.isWaitTimeElapsed(activityJobId)).thenReturn(true);
        restartActivityHandler.isRestoreActionCompleted(jobEnvironment, ActivityConstants.TRUE);
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), any(Date.class), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
        verify(nodeRestartActivityTimer, times(1)).isWaitTimeElapsed(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(Matchers.anyList(), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testIsRestoreActionCompletedThrowsException() throws JobDataNotFoundException {
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        restartActivityHandler.isRestoreActionCompleted(jobEnvironment, ActivityConstants.TRUE);
        assertEquals(false, restartActivityHandler.isRestoreActionCompleted(jobEnvironment, ActivityConstants.TRUE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeoutThrowsException() {
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        when(jobEnvironment.getNodeName()).thenThrow(Exception.class);
        restartActivityHandler.cancelTimeout(true, jobEnvironment, ActivityConstants.TRUE);
    }

    @Test
    public void testCancelNodeRestartActionThrowsException() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenThrow(Exception.class);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutionUser);
        restartActivityHandler.cancelNodeRestartAction(jobEnvironment);

    }
}
