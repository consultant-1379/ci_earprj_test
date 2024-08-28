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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
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
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.CmHeartbeatHandler;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class RestoreServiceTest {

    @InjectMocks
    RestoreService ecimRestoreServiceMock;

    @Mock
    JobUpdateService jobUpdateService;

    @Mock
    ActivityUtils activityUtils;

    @Mock
    EcimBackupUtils ecimUtils;

    @Mock
    EcimBackupInfo ecimBackupInfo;

    @Mock
    SystemRecorder systemRecorder;

    @Mock
    DpsWriterRetryProxy dpsWriter;

    @Mock
    Map<String, AttributeChangeData> modifiedAttributes;

    @Mock
    AsyncActionProgress progressReport;

    @Mock
    NotificationSubject notificationSubject;

    @Mock
    CancelBackupService cancelBackupService;

    @Mock
    Notification notification;

    @Mock
    FdnNotificationSubject fdnNotificationSubject;

    @Mock
    JobActivityInfo jobActivityInfo;

    @Mock
    DpsWriter dpsWriterMock;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private CmHeartbeatHandler cmHeartbeatHandler;

    @Mock
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Mock
    private NEJobProgressPercentageCache jobProgressPercentageCache;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private NetworkElementData networkElementInfo;

    @Mock
    JobLogUtil jobLogUtilMock;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    long activityJobId = 1243L;
    long neJobId = 1223L;
    long mainJobId = 1253L;
    long templateJobId = 1236L;
    String inputVersion = "inputVersion";
    String nodeName = "LTE0200001";
    String businessKey = "1230001";
    final String activityName = "restorebackup";
    final String brmBackupMoFdn = "xyz";
    private final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

    @Test
    public void testPrecheckSuccess() throws MoNotFoundException, UnsupportedFragmentException, SecurityViolationException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String backupName = "backup101";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        ecimRestoreServiceMock.precheck(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckShouldUpdatetheJobAsFailedWhenJobDataNotFoundExceptionThrown() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenThrow(JobDataNotFoundException.class);
        ecimRestoreServiceMock.precheck(activityJobId);
        Mockito.verify(activityUtils, times(1)).handleExceptionForPrecheckScenarios(activityJobId, "RestoreBackup", null);
    }

    @Test
    public void testPrecheckShouldUpdatetheJobAsFailedWhenMoNotFoundExceptionThrown() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup101";
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenThrow(MoNotFoundException.class);
        ecimRestoreServiceMock.precheck(activityJobId);
        Mockito.verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testPrecheckShouldUpdatetheJobAsFailedWhenUnsupportedFragmentExceptionThrown() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenThrow(UnsupportedFragmentException.class);
        ecimRestoreServiceMock.precheck(activityJobId);
        Mockito.verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testPrecheckShouldUpdatetheJobAsFailedWhenExceptionThrown() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenThrow(Exception.class);
        ecimRestoreServiceMock.precheck(activityJobId);
        Mockito.verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testPrecheckShouldFailtheJobWhenUserValidationFailed() throws JobDataNotFoundException, MoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(false);
        final ActivityStepResult activityStepResult = ecimRestoreServiceMock.precheck(activityJobId);
        assert (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testAsyncPrecheckSuccess() throws MoNotFoundException, UnsupportedFragmentException, SecurityViolationException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String backupName = "backup101";
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn("treatAsInfos");
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(inputVersion);
        ecimRestoreServiceMock.asyncPrecheck(activityJobId);
        Mockito.verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        Mockito.verify(jobLogUtilMock, times(3)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Mockito.verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.RESTORE_BACKUP, ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testAsyncPrecheckShouldUpdatetheJobAsFailedWhenJobNotFoundExceptionThrown() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenThrow(JobDataNotFoundException.class);
        ecimRestoreServiceMock.asyncPrecheck(activityJobId);
        Mockito.verify(activityUtils, times(1)).handleExceptionForPrecheckScenarios(activityJobId, "RestoreBackup", null);
    }

    @Test
    public void testAsyncPrecheckShouldFailtheJobWhenUserValidationFailed() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(false);
        ecimRestoreServiceMock.asyncPrecheck(activityJobId);
        Mockito.verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.RESTORE_BACKUP, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testAsyncPrecheckShouldUpdatetheJobAsFailedWhenMoNotFoundExceptionThrown() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup101";
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenThrow(MoNotFoundException.class);
        ecimRestoreServiceMock.asyncPrecheck(activityJobId);
        Mockito.verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testAsyncPrecheckShouldUpdatetheJobAsFailedWhenUnsupportedFragmentExceptionThrown() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenThrow(UnsupportedFragmentException.class);
        ecimRestoreServiceMock.asyncPrecheck(activityJobId);
        Mockito.verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testAsyncPrecheckShouldUpdatetheJobAsFailedWhenExceptionThrown() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.RESTORE_BACKUP)).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenThrow(Exception.class);
        ecimRestoreServiceMock.asyncPrecheck(activityJobId);
        Mockito.verify(jobLogUtilMock, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testPrecheckHandleTimeout() {
        final long activityJobId = 1;
        ecimRestoreServiceMock.precheckHandleTimeout(activityJobId);
        verify(activityUtils).failActivityForPrecheckTimeoutExpiry(activityJobId, EcimBackupConstants.RESTORE_BACKUP);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeSuccessTest() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup201";
        final String brmBackupMoFdn = "xyz";
        final String businessKey = "2";
        final int actionId = 111;
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(pollingActivityConfiguration.getShmHeartBeatIntervalForEcim()).thenReturn(180);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(activityUtils.subscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, RestoreService.class))).thenReturn(fdnNotificationSubject);
        when(dpsWriterMock.performAction(brmBackupMoFdn, EcimBackupConstants.RESTORE_BACKUP_ACTION, actionArguments)).thenReturn(actionId);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList(), Matchers.anyString())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementInfo.getNeType()).thenReturn("SGSN-MME");
        when(networkElementInfo.getNeFdn()).thenReturn("NetworkElement=abc");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.RESTORE_BACKUP)).thenReturn(2000);
        ecimRestoreServiceMock.execute(activityJobId);
        verify(cmHeartbeatHandler).sendHeartbeatIntervalChangeRequest(180, activityJobId, networkElementInfo.getNeFdn());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeFailTest1() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final String nodeName = "abc";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(ecimUtils.getBackup(neJobStaticData)).thenThrow(MoNotFoundException.class);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        ecimRestoreServiceMock.execute(activityJobId);
        verify(cmHeartbeatHandler, never()).sendHeartbeatIntervalChangeRequest(Matchers.anyInt(), Matchers.anyLong(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executewithMediationServiceException() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(ecimUtils.getBackup(neJobStaticData)).thenThrow(MoNotFoundException.class);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        when(brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupMoFdn, EcimBackupConstants.RESTORE_BACKUP_ACTION)).thenThrow(exception);
        ecimRestoreServiceMock.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, "", null);
        verify(cmHeartbeatHandler, never()).sendHeartbeatIntervalChangeRequest(Matchers.anyInt(), Matchers.anyLong(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeFailTest2() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final String nodeName = "abc";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(ecimUtils.getBackup(neJobStaticData)).thenThrow(UnsupportedFragmentException.class);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        ecimRestoreServiceMock.execute(activityJobId);
        verify(cmHeartbeatHandler, never()).sendHeartbeatIntervalChangeRequest(Matchers.anyInt(), Matchers.anyLong(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeFailTest3() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final String nodeName = "abc";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(ecimUtils.getBackup(neJobStaticData)).thenThrow(Exception.class);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        ecimRestoreServiceMock.execute(activityJobId);
    }

    @Test
    public void executeFailTest4() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementInfo.getNeType()).thenReturn("SGSN-MME");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.RESTORE_BACKUP)).thenReturn(2000);
        ecimRestoreServiceMock.execute(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @Test
    public void processNotificationSuccessTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(neJobStaticData.getNodeName(), EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.RESTORE_BACKUP_ACTION)).thenReturn(true);
        ecimRestoreServiceMock.processNotification(notification);
    }

    @Test
    public void processNotificationSuccessTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropList = new ArrayList<Map<String, Object>>();
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(progressReport.getState()).thenReturn(ActionStateType.RUNNING);
        when(progressReport.getProgressPercentage()).thenReturn((int) 56.00);
        when(cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.RESTORE_BACKUP_ACTION)).thenReturn(true);
        ecimRestoreServiceMock.processNotification(notification);
        Mockito.verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList, (double) progressReport.getProgressPercentage());
        Mockito.verify(jobProgressPercentageCache).bufferNEJobs(neJobStaticData.getNeJobId());
    }

    @Test
    public void processNotificationFailTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(neJobStaticData.getNodeName(), EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.RESTORE_BACKUP_ACTION)).thenReturn(false);
        ecimRestoreServiceMock.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotificationFailTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(neJobStaticData.getNodeName(), EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes)).thenThrow(UnsupportedFragmentException.class);
        ecimRestoreServiceMock.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotificationFailTest3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(neJobStaticData.getNodeName(), EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes)).thenThrow(MoNotFoundException.class);
        ecimRestoreServiceMock.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotificationCancelTest4() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        final Map<String, Object> reportProgressAttributes = new HashMap<String, Object>();
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(neJobStaticData.getNodeName(), EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes)).thenReturn(new AsyncActionProgress(reportProgressAttributes));
        when(cancelBackupService.isCancelActionTriggerred(anyMap())).thenReturn(true);
        ecimRestoreServiceMock.processNotification(notification);
    }

    @Test
    public void handleTimeoutSuccessTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup100";
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(networkElementInfo.getNeType()).thenReturn("RadioNode");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(Matchers.anyString(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(brmBackupMoFdn);
        when(cancelBackupService.isCancelActionTriggerred(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId))).thenReturn(false);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        when(brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(Matchers.anyString(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(progressReport);
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void handleTimeoutSuccessTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup100";
        final String nodeName = "abc";
        final String activityName = "xyz";
        final String brmBackupMoFdn = "xyz";
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.RESTORE_BACKUP_ACTION, ecimBackupInfo)).thenReturn(progressReport);
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void handleTimeoutFailTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup100";
        final String nodeName = "abc";
        final String activityName = "xyz";
        final String brmBackupMoFdn = "xyz";
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenThrow(MoNotFoundException.class);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void handleTimeoutFailTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup100";
        final String nodeName = "abc";
        final String activityName = "xyz";
        final String brmBackupMoFdn = "xyz";
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenThrow(UnsupportedFragmentException.class);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void handleTimeoutFailWhenExceptionThrownTest() throws MoNotFoundException, UnsupportedFragmentException {
        final String backupName = "backup100";
        final String nodeName = "abc";
        final String activityName = "xyz";
        final String brmBackupMoFdn = "xyz";
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenThrow(Exception.class);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimoutWithActionFailedOverNode() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        when(brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(Matchers.anyString(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(progressReport);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);
        when(progressReport.getResultInfo()).thenReturn("Failed");
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(jobLogUtilMock, times(3)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimoutWithActionFailedOverNodeWithReason() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        when(brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(Matchers.anyString(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(progressReport);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);
        when(progressReport.getResultInfo()).thenReturn("");
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(jobLogUtilMock, times(3)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimoutWithActionStillRunningOverNode() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getActivityStartTime()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        when(brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(Matchers.anyString(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(progressReport);
        when(progressReport.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        ecimRestoreServiceMock.handleTimeout(activityJobId);
        Mockito.verify(jobLogUtilMock, times(4)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cancelTest() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final String nodeName = "abc";
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.RESTORE_BACKUP)).thenReturn(2000);
        final ActivityStepResult activityStepResultOutput = null;
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(cancelBackupService.cancel(activityJobId, EcimBackupConstants.RESTORE_BACKUP, ecimBackupInfo)).thenReturn(activityStepResultOutput);
        when(neJobStaticData.getNodeName()).thenReturn("nodeName");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        ecimRestoreServiceMock.cancel(activityJobId);
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        ecimRestoreServiceMock.processNotification(notification);
        verify(activityUtils, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }

    @Before
    public void setUp() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
    }

}
