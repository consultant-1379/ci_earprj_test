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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
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
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DpsDataChangedEvent.class })
public class DownloadBackupServiceTest {

    @InjectMocks
    DownloadBackupService objectUnderTest;

    @Mock
    JobUpdateService jobUpdateService;

    @Mock
    JobEnvironment jobEnvironment;

    @Mock
    ActivityUtils activityUtils;

    @Mock
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Mock
    EcimBackupUtils ecimUtils;

    @Mock
    EcimBackupInfo ecimBackupInfo;

    @Mock
    ActivityStepResult activityStepResult;

    @Mock
    SystemRecorder systemRecorder;

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
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    DpsDataChangedEvent dpsDataChangeEvent;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private RestoreJobHandlerRetryProxy restoreJobHandlerRetryProxy;

    @Mock
    private NEJobProgressPercentageCache jobProgressPercentageCache;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    JobLogUtil jobLogUtilMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private NetworkElementData networkElementInfo;

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
    String nodeName = "LTE0200001";
    String businessKey = "1230001";
    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    private Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    private Map<String, Object> neJobAttributes = new HashMap<String, Object>();

    @Before
    public void setUp() throws JobDataNotFoundException, MoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
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

    @Test
    public void precheckSuccessTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        objectUnderTest.precheck(activityJobId);
    }

    @Test
    public void precheckSuccessTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        final String brmBackupManagerMoFdn = "brmFdn";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @Test
    public void precheckSuccessTest3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        final String brmBackupManagerMoFdn = "brmFdn";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)).thenReturn(false);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @Test
    public void precheckSuccessTest4() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        final String brmBackupManagerMoFdn = "brmFdn";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void precheckFailTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        final String brmBackupManagerMoFdn = "brmFdn";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void precheckFailTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        final String brmBackupManagerMoFdn = "brmFdn";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @SuppressWarnings({ "unused", "unchecked" })
    @Test
    public void precheckFailTest3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackup(jobEnvironment)).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @SuppressWarnings({ "unused", "unchecked" })
    @Test
    public void precheckFailTest4() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackup(jobEnvironment)).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.precheck(activityJobId);
    }

    @Test
    public void testPrecheck_invalidENMBackupSupplied_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("", "", "");
        ecimBackupInfoMock.setBackupFileName("");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_invalidNodeBackupSupplied_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException, SecurityViolationException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("");
        ecimBackupInfoMock.setBackupLocation("NODE");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(true);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_validENMBackupSupplied_precheckSuccessProceedExecution() throws MoNotFoundException, UnsupportedFragmentException, SecurityViolationException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        final String inputVersion = "version";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("backup.zip");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenThrow(MoNotFoundException.class);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(true);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION));
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testPrecheck_validENMBackupSupplied_precheckSuccessSkipExecution() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        final String inputVersion = "version";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("backup.zip");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(true);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_validENMBackupButNoBRMBackupMgrFound_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("backup.zip");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfoMock)).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_validENMBackupButUnsupportedFragment_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("backup.zip");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfoMock)).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION));
    }

    @Test
    public void testPrecheck_validENMBackupSuppliedWithNodeBackupInImproperState_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        final String inputVersion = "version";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("backup.zip");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenReturn(false);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_InvalidFragmentENMBackupSupplied_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        final String inputVersion = "version";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("backup.zip");
        ecimBackupInfoMock.setBackupLocation("ENM");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenThrow(UnsupportedFragmentException.class);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_InvalidFragmentNodeBackupSupplied_precheckFailSkipExecution() throws MoNotFoundException, UnsupportedFragmentException, SecurityViolationException,
            JobDataNotFoundException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("");
        ecimBackupInfoMock.setBackupLocation("NODE");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(true);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION));
        Mockito.verify(jobUpdateService).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_FailSkipExecution_WhenTbacFailed() throws MoNotFoundException, UnsupportedFragmentException, SecurityViolationException, JobDataNotFoundException {
        final String nodeName = "abc";
        final String brmBackupManagerMOFdn = "xyz";
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        final EcimBackupInfo ecimBackupInfoMock = new EcimBackupInfo("domain", "backup", "type");
        ecimBackupInfoMock.setBackupFileName("");
        ecimBackupInfoMock.setBackupLocation("NODE");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfoMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMOFdn);
        when(brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfoMock)).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(false);
        Assert.assertTrue(objectUnderTest.precheck(activityJobId).getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeSuccessTest() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(actionArguments);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupMoFdn);
        when(activityUtils.subscribeToMoNotifications(brmBackupMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, RestoreService.class))).thenReturn(fdnNotificationSubject);

        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(2000);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeFailTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(actionArguments);
        when(ecimUtils.getBackup(jobEnvironment)).thenThrow(MoNotFoundException.class);

        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(2000);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executewithMediationServiceException() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final String nodeName = "abc";
        final String brmBackupManagerMoFdn = "brmFdn";
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(actionArguments);
        when(ecimUtils.getBackup(jobEnvironment)).thenThrow(MoNotFoundException.class);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(2000);
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackupInfo, brmBackupManagerMoFdn, EcimBackupConstants.IMPORT_BACKUP_ACTION)).thenThrow(exception);
        objectUnderTest.execute(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeFailTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(actionArguments);
        when(ecimUtils.getBackup(jobEnvironment)).thenThrow(UnsupportedFragmentException.class);

        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(2000);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeFailTest3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(actionArguments);
        when(ecimUtils.getBackup(jobEnvironment)).thenThrow(Exception.class);

        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(2000);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotification_successTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropList = new ArrayList<Map<String, Object>>();
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(progressReport.getProgressPercentage()).thenReturn((int) 56.00);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.RUNNING);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(progressReport);
        objectUnderTest.processNotification(notification);
        Mockito.verify(jobLogUtilMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Mockito.verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, jobPropList, jobLogList, (double) progressReport.getProgressPercentage());
        Mockito.verify(jobProgressPercentageCache).bufferNEJobs(neJobStaticData.getNeJobId());
    }

    @Test
    public void processNotification_successTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(progressReport);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(restoreJobHandlerRetryProxy.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn(true);
        objectUnderTest.processNotification(notification);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        Mockito.verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
    }

    @Test
    public void processNotification_successTest3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        final String brmBackupMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "test", "type");
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(dpsDataChangeEvent.getFdn()).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getBackupNameFromBrmBackupMOFdn(brmBackupMoFdn)).thenReturn("test");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(activityUtils.getParentFdn(brmBackupMoFdn)).thenReturn(brmBackupManagerMoFdn);
        when(restoreJobHandlerRetryProxy.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED)).thenReturn(true);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        objectUnderTest.processNotification(notification);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
    }

    @Test
    public void processNotification_InvalidScenario_Test1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        final String brmBackupMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final Map<String, Object> activityJobAttributesMap = new HashMap<String, Object>();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "test", "type");

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(dpsDataChangeEvent.getFdn()).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getBackupNameFromBrmBackupMOFdn(brmBackupMoFdn)).thenReturn("test");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(null);
        when(activityUtils.getParentFdn(brmBackupMoFdn)).thenReturn(brmBackupManagerMoFdn);

        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMap);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn(Boolean.toString(true));
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED)).thenReturn(Boolean.toString(true));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
    }

    @Test
    public void processNotification_InvalidScenario_Test2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        final String brmBackupMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final Map<String, Object> activityJobAttributesMap = new HashMap<String, Object>();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "test", "type");

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(dpsDataChangeEvent.getFdn()).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getBackupNameFromBrmBackupMOFdn(brmBackupMoFdn)).thenReturn("testNew");
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(activityUtils.getParentFdn(brmBackupMoFdn)).thenReturn(brmBackupManagerMoFdn);

        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMap);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn(Boolean.toString(true));
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED)).thenReturn(Boolean.toString(true));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
    }

    @Test
    public void processNotification_InvalidScenario_Test3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        final String brmBackupMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final Map<String, Object> activityJobAttributesMap = new HashMap<String, Object>();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "test", "type");

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(dpsDataChangeEvent.getFdn()).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getBackupNameFromBrmBackupMOFdn(brmBackupMoFdn)).thenReturn(null);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(activityUtils.getParentFdn(brmBackupMoFdn)).thenReturn(brmBackupManagerMoFdn);

        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMap);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn(Boolean.toString(true));
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED)).thenReturn(Boolean.toString(true));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
    }

    @Test
    public void processNotification_InvalidScenario_Test4() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        final String brmBackupMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final Map<String, Object> activityJobAttributesMap = new HashMap<String, Object>();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "test", "type");

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangeEvent);
        when(dpsDataChangeEvent.getFdn()).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getBackupNameFromBrmBackupMOFdn(brmBackupMoFdn)).thenReturn(null);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DOWNLOAD_BACKUP, nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(activityUtils.getParentFdn(brmBackupMoFdn)).thenReturn(brmBackupManagerMoFdn + "New");

        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMap);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn(Boolean.toString(true));
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED)).thenReturn(Boolean.toString(true));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
    }

    @Test
    public void processNotification_failureTest1_downloadFailed() throws MoNotFoundException, UnsupportedFragmentException {
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(progressReport);
        objectUnderTest.processNotification(notification);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        Mockito.verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
    }

    @Test
    public void processNotification_failureTest2_createNotificationNotYettReceived() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(restoreJobHandlerRetryProxy.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn(false);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
    }

    @Test
    public void processNotification_failureTest3_createNotificationNotYettReceived() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";
        final Map<String, Object> activityJobAttributesMap = new HashMap<String, Object>();

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(activityUtils.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMap);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL)).thenReturn("");
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributesMap, EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED)).thenReturn("");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
    }

    @Test
    public void processNotificationInvalidNotification() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(brmMoServiceRetryProxy.validateActionProgressReport(nodeName, progressReport, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        Mockito.verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        Mockito.verify(activityUtils, times(0)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
    }

    @Test
    public void processNotificationDuringCancelTrigger() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "NE01";
        final String brmBackupManagerMoFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=test";

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupManagerMoFdn);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(progressReport);
        when(brmMoServiceRetryProxy.validateActionProgressReport(nodeName, progressReport, EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(false);
        when(cancelBackupService.isCancelActionTriggerred(jobEnvironment.getActivityJobAttributes())).thenReturn(true);
        when(activityUtils.getActivityInfo(activityJobId, DownloadBackupService.class)).thenReturn(jobActivityInfo);
        objectUnderTest.processNotification(notification);
        Mockito.verify(cancelBackupService, times(1)).evaluateCancelProgress(progressReport, jobActivityInfo, brmBackupManagerMoFdn, neJobStaticData, EcimBackupConstants.DOWNLOAD_BACKUP);
    }

    @Test
    public void processNotificationSuccessTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(jobEnvironment.getNodeName(), EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.IMPORT_BACKUP_ACTION)).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
    }

    @Test
    public void processNotificationFailTest1() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(jobEnvironment.getNodeName(), EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenReturn(progressReport);
        when(cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.IMPORT_BACKUP_ACTION)).thenReturn(false);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotificationFailTest2() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(jobEnvironment.getNodeName(), EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotificationFailTest3() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(jobEnvironment.getNodeName(), EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processNotificationCancelTest4() throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = "abc";
        final String backupName = "backup100";
        final String brmBackupMoFdn = "xyz";
        final Map<String, Object> map = new HashMap<String, Object>();
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(jobEnvironment.getNodeName()).thenReturn(nodeName);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn(backupName);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(brmBackupMoFdn);
        final Map<String, Object> reportProgressAttributes = new HashMap<String, Object>();
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(jobEnvironment.getNodeName(), EcimBackupConstants.DOWNLOAD_BACKUP, modifiedAttributes)).thenReturn(
                new AsyncActionProgress(reportProgressAttributes));
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(map);
        when(cancelBackupService.isCancelActionTriggerred(anyMap())).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.processNotification(notification);
    }

    @Test
    public void handleTimeoutSuccessTest1() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(eq(EcimBackupConstants.DOWNLOAD_BACKUP), anyString(), eq(ecimBackupInfo))).thenReturn("Fdn");
        when(brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(anyString(), eq(ecimBackupInfo))).thenReturn(progressReport);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResultOutput = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResultOutput.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void handleTimeoutFailTest1() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenThrow(MoNotFoundException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResultOutput = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResultOutput.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void handleTimeoutFailTest2() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(eq(EcimBackupConstants.DOWNLOAD_BACKUP), anyString(), eq(ecimBackupInfo))).thenReturn("Fdn");
        when(brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(anyString(), eq(ecimBackupInfo))).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResultOutput = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResultOutput.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void handleTimeoutFailTest3() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(eq(EcimBackupConstants.DOWNLOAD_BACKUP), anyString(), eq(ecimBackupInfo))).thenReturn("Fdn");
        when(brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(anyString(), eq(ecimBackupInfo))).thenReturn(progressReport);
        when(brmMoServiceRetryProxy.isBackupExist(anyString(), eq(ecimBackupInfo))).thenThrow(UnsupportedFragmentException.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResultOutput = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResultOutput.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void handleTimeoutFailWhenExceptionThrownTest() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceRetryProxy.getNotifiableMoFdn(eq(EcimBackupConstants.DOWNLOAD_BACKUP), anyString(), eq(ecimBackupInfo))).thenReturn("Fdn");
        when(brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(anyString(), eq(ecimBackupInfo))).thenThrow(Exception.class);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        final ActivityStepResult activityStepResultOutput = objectUnderTest.handleTimeout(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResultOutput.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings({ "unused", "unchecked" })
    @Test
    public void cancelTest() throws MoNotFoundException, UnsupportedFragmentException {
        final Integer defaultactivityTimeout = 999;
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.RESTORE.toString(), EcimBackupConstants.DOWNLOAD_BACKUP)).thenReturn(2000);
        final ActivityStepResult activityStepResultOutput = null;
        when(ecimUtils.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(cancelBackupService.cancel(activityJobId, EcimBackupConstants.DOWNLOAD_BACKUP, ecimBackupInfo)).thenReturn(activityStepResultOutput);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn("nodeName");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        objectUnderTest.cancel(activityJobId);
    }

}
