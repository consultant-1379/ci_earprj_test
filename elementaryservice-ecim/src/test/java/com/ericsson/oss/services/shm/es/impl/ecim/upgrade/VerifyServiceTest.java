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
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
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
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.FragmentHandler;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class VerifyServiceTest {

    @Mock
    private AsyncActionProgress asyncActionProgressMock;

    @Mock
    private UpMoServiceRetryProxy upMoServiceMock;

    @Mock
    private ActivityUtils activityUtilsMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private NotificationRegistry notificationRegistryMock;

    @Mock
    private SystemRecorder systemRecorder;

    @InjectMocks
    private VerifyService objectUnderTest;

    @Mock
    private Notification notificationMock;

    @Mock
    private EcimSwMUtils ecimSwMUtilsMock;

    @Mock
    private EcimUpgradeInfo ecimUpgradeInfo;

    @Mock
    private ActivityStepResult activityStepResultMock;

    @Mock
    private NotificationSubject notificationSubjectMock;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private Map<String, Object> mainJobAttrsMock;

    @Mock
    private Map<String, Object> neJobAttrsMock;

    @Mock
    private Map<String, Object> activityJobAttrsMock;

    @Mock
    private DpsDataChangedEvent dpsDataChangedEvent;

    @Mock
    private Map<String, AttributeChangeData> modifiedAttributes;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private ActionResult actionResult;

    @Mock
    private ActivityAllowed activityAllowed;

    @Mock
    private CancelUpgradeService cancelUpgradeService;

    @Mock
    private OssModelInfoProvider ossModelInfoProviderMock;

    @Mock
    OssModelInfo ossModelInfoMock;

    @Mock
    FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    private FragmentHandler fragmentHandler;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    long activityJobId = 143L;
    long neJobId = 133L;
    long mainJobId = 123L;
    long templateJobId = 113L;

    String identity = "Some Identity";
    String type = "Standard";
    String neName = "Some Ne Name";
    String sWmMoFdn = "SomeSwMMoFdn";
    String upMoFdn = "SomeupMoFdn";
    String businessKey = "dummyKey";
    String activityName = "verify";
    String inputVersion = "inputVersion";
    private static final String ACTION_STATUS = "actionStatus";

    //String actionName = "verify";

    @Before
    public void runBeforeTestMethod() {
        activityJobAttrsMock = new HashMap<String, Object>();
        activityJobAttrsMock.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttrsMock);
    }

    @Test
    public void asyncprecheckFailureTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(false);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobUpdateServiceMock, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @Test
    public void asyncprecheckFailureTest_unsupportedState() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(UnsupportedFragmentException.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void asyncprecheckFailureTest_MoNotExist() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {

        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(MoNotFoundException.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("neType");
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("neType");
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void asyncprecheckFailureTest_SoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(SoftwarePackageNameNotFound.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void asyncprecheckFailureTest_SoftwarePackagePoNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(SoftwarePackagePoNotFound.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtilsMock, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void asyncprecheckSuccessTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(activityUtilsMock, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.VERIFY,
                ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        verify(activityUtilsMock, times(1)).recordEvent(anyString(), anyString(), anyString(), anyString());

    }

    @Test
    public void precheckFailureTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {

        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(false);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @Test
    public void precheckFailureTest_unsupportedState() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(UnsupportedFragmentException.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void precheckFailureTest_MoNotExist() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(MoNotFoundException.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void precheckFailureTest_SoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(SoftwarePackageNameNotFound.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void precheckFailureTest_SoftwarePackagePoNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenThrow(SoftwarePackagePoNotFound.class);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void precheckSuccessTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, times(1)).recordEvent(anyString(), anyString(), anyString(), anyString());
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @Test
    public void executeFailureTest() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(neName, FragmentType.ECIM_SWM_TYPE, jobLogList);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(actionResult.isTriggerSuccess()).thenReturn(false);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        verify(activityUtilsMock, times(0)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(upMoServiceMock, times(0)).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE);
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
    }

    @Test
    public void executeFailureTestForNullPointerException() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        Mockito.doThrow(new NullPointerException()).when(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);
        objectUnderTest.execute(activityJobId);
        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(eq(activityJobId), Mockito.any(NEJobStaticData.class));
        verify(activityUtilsMock, times(0)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(upMoServiceMock, times(0)).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE);
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
    }

    @Test
    public void executeFailureTestForJobDataNotFound() throws JobDataNotFoundException {
        Mockito.doThrow(new JobDataNotFoundException()).when(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        objectUnderTest.execute(activityJobId);
        verify(activityUtilsMock, times(0)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(jobUpdateServiceMock, times(0)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
    }

    @Test
    public void executeFailureTest_WhenTbacFailed() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(neName, FragmentType.ECIM_SWM_TYPE, jobLogList);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(actionResult.isTriggerSuccess()).thenReturn(false);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(false);
        objectUnderTest.execute(activityJobId);
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
    }

    @Test
    public void executeFailureTest_unsupportedFragment() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(neName, FragmentType.ECIM_SWM_TYPE, jobLogList);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenThrow(UnsupportedFragmentException.class);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        verify(activityUtilsMock, times(1)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
        verify(systemRecorder, atLeast(1)).recordCommand(eq(SHMEvents.VERIFY_EXECUTE), eq(CommandPhase.STARTED), anyString(), anyString(), anyString());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());

        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);
    }

    @Test
    public void executeFailureTest_SoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(neName, FragmentType.ECIM_SWM_TYPE, jobLogList);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenThrow(SoftwarePackageNameNotFound.class);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        verify(activityUtilsMock, times(1)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
        verify(systemRecorder, atLeast(1)).recordCommand(eq(SHMEvents.VERIFY_EXECUTE), eq(CommandPhase.STARTED), anyString(), anyString(), anyString());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());

        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);
    }

    @Test
    public void executeFailureTest_SoftwarePackagePoNotFound() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenThrow(SoftwarePackagePoNotFound.class);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        verify(activityUtilsMock, times(1)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
        verify(systemRecorder, atLeast(1)).recordCommand(eq(SHMEvents.VERIFY_EXECUTE), eq(CommandPhase.STARTED), anyString(), anyString(), anyString());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());

        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);
    }

    @Test
    public void executeFailureTest_MONotFoundException() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(neName, FragmentType.ECIM_SWM_TYPE, jobLogList);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenThrow(MoNotFoundException.class);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);

        verify(activityUtilsMock, times(1)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).failActivity(anyLong(), anyList(), anyString(), anyString());
        verify(systemRecorder, atLeast(1)).recordCommand(eq(SHMEvents.VERIFY_EXECUTE), eq(CommandPhase.STARTED), anyString(), anyString(), anyString());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
    }

    @Test
    public void executeFailurewithMediationServiceException() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(neName, FragmentType.ECIM_SWM_TYPE, jobLogList);

        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenThrow(exception);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);

        verify(systemRecorder, times(0)).recordCommand(SHMEvents.VERIFY_EXECUTE, CommandPhase.STARTED, null, upMoFdn, null);
    }

    @Test
    public void executeSuccessTest() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId, neJobStaticData)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);

        when(upMoServiceMock.isActivityAllowed(activityName, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);

        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceMock.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.VERIFY_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.toString(), activityName)).thenReturn(2000);
        when(actionResult.isTriggerSuccess()).thenReturn(true);
        when(networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName())).thenReturn("SGSN_MME");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, activityName)).thenReturn(true);

        objectUnderTest.execute(activityJobId);
        verify(activityUtilsMock, times(1)).subscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(0)).sendNotificationToWFS((NEJobStaticData) anyObject(), anyLong(), anyString(), anyMap());
        verify(systemRecorder, times(1)).recordCommand(eq(SHMEvents.VERIFY_EXECUTE), eq(CommandPhase.STARTED), anyString(), anyString(), anyString());
        verify(systemRecorder, times(1)).recordCommand(eq(SHMEvents.VERIFY_EXECUTE), eq(CommandPhase.FINISHED_WITH_SUCCESS), anyString(), anyString(), anyString());

        final InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwMUtilsMock);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwMUtilsMock).getEcimUpgradeInformation(activityJobId, neJobStaticData);
    }

    @Test
    public void calcelServiceFailureTest() {
        final ActivityStepResult activityStepResult = objectUnderTest.cancel(activityJobId);
        assertNull(activityStepResult);
    }

    @Test
    public void handletimeoutFaillureTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgressMock);
        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo)).thenReturn(true);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.CANCELLING);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(upMoServiceMock.isActivityAllowed(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(true);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
    }

    @Test
    public void handletimeoutFaillureTest_WithLessPercantage() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgressMock);
        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo)).thenReturn(true);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(upMoServiceMock.isActivityAllowed(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(activityAllowed);
        when(activityAllowed.getActivityAllowed()).thenReturn(false);

        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
    }

    @Test
    public void handletimeoutFaillureTest_throwsUnsupportedFragment() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenThrow(UnsupportedFragmentException.class);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);

        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
    }

    @Test
    public void handletimeoutJobSuccessWhenProgress100() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgressMock);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo)).thenReturn(true);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(100);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.FAILURE);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(upMoServiceMock.isActivityCompleted(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(true);

        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
    }

    @Test
    public void handletimeoutJobSuccessWhenStateFINISHED() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgressMock);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo)).thenReturn(true);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        final Map<String, Object> activityState = new HashMap<String, Object>();
        activityState.put(ACTION_STATUS, true);
        activityState.put(ActivityConstants.JOB_RESULT, JobResult.SUCCESS);
        when(upMoServiceMock.isActionCompleted(asyncActionProgressMock)).thenReturn(activityState);

        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, times(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
    }

    @Test
    public void handletimeoutFailsWhenResultFAILURE() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgressMock);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> activityState = new HashMap<String, Object>();
        activityState.put(ACTION_STATUS, true);
        activityState.put(ActivityConstants.JOB_RESULT, JobResult.FAILED);
        when(upMoServiceMock.isActionCompleted(asyncActionProgressMock)).thenReturn(activityState);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo)).thenReturn(true);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(upMoServiceMock.isActivityCompleted(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(true);

        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);
        assertNotNull(activityStepResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, atLeast(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).recordEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void handletimeoutStepSuccess() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upMoServiceMock.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgressMock);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        final Map<String, Object> activityState = new HashMap<String, Object>();
        activityState.put(ACTION_STATUS, true);
        activityState.put(ActivityConstants.JOB_RESULT, JobResult.SUCCESS);
        when(upMoServiceMock.isActionCompleted(asyncActionProgressMock)).thenReturn(activityState);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getNeJobStaticData().getNodeName()).thenReturn(neName);
        when(activityUtilsMock.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo)).thenReturn(true);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(100);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(upMoServiceMock.isActivityCompleted(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(true);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);

        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
        verify(activityUtilsMock, times(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(activityUtilsMock, times(1)).recordEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void proccessNotification_withDiffAction()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);

        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getNeJobStaticData().getNodeName()).thenReturn(neName);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(upMoServiceMock.getValidAsyncActionProgress(neName, modifiedAttributes)).thenReturn(asyncActionProgressMock);
        final Map<String, Object> activityState = new HashMap<String, Object>();
        activityState.put(ACTION_STATUS, true);
        activityState.put(ActivityConstants.JOB_RESULT, JobResult.SUCCESS);

        when(upMoServiceMock.isActionCompleted(asyncActionProgressMock)).thenReturn(activityState);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);

        objectUnderTest.processNotification(notificationMock);

        verify(activityUtilsMock, times(1)).sendNotificationToWFS((NEJobStaticData) anyObject(), anyLong(), anyString(), anyMap());
        verify(activityUtilsMock, times(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @Test
    public void proccessNotification_ActionIsInRunningState()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getNeJobStaticData().getNodeName()).thenReturn(neName);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(upMoServiceMock.getValidAsyncActionProgress(neName, modifiedAttributes)).thenReturn(asyncActionProgressMock);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.RUNNING);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);

        objectUnderTest.processNotification(notificationMock);
        verify(jobUpdateServiceMock, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
    }

    @Test
    public void proccessNotification_actionTypeIsInCancelling()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getNeJobStaticData().getNodeName()).thenReturn(neName);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(upMoServiceMock.getValidAsyncActionProgress(neName, modifiedAttributes)).thenReturn(asyncActionProgressMock);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.CANCELLING);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);

        objectUnderTest.processNotification(notificationMock);
        verify(jobUpdateServiceMock, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
    }

    @Test
    public void proccessNotification_actionCancelled()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getNeJobStaticData().getNodeName()).thenReturn(neName);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(upMoServiceMock.getNotifiableMoFdn(EcimSwMConstants.VERIFY_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(upMoServiceMock.getValidAsyncActionProgress(neName, modifiedAttributes)).thenReturn(asyncActionProgressMock);
        final Map<String, Object> activityState = new HashMap<String, Object>();
        activityState.put(ACTION_STATUS, true);
        activityState.put(ActivityConstants.JOB_RESULT, JobResult.SUCCESS);
        when(upMoServiceMock.isActionCompleted(asyncActionProgressMock)).thenReturn(activityState);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.CANCELLED);
        when(asyncActionProgressMock.getActionName()).thenReturn(activityName);
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);

        objectUnderTest.processNotification(notificationMock);
        verify(activityUtilsMock, times(1)).sendNotificationToWFS((NEJobStaticData) anyObject(), anyLong(), anyString(), anyMap());
        verify(activityUtilsMock, times(1)).unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject());
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @Test
    public void proccessNotification_MoNotFoundException()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(ecimSwMUtilsMock.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getNeJobStaticData().getNodeName()).thenReturn(neName);
        when(notificationMock.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(upMoServiceMock.getNotifiableMoFdn(ActivityConstants.VERIFY, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);
        when(upMoServiceMock.getValidAsyncActionProgress(neName, modifiedAttributes)).thenThrow(MoNotFoundException.class);
        when(asyncActionProgressMock.getProgressPercentage()).thenReturn(80);
        when(asyncActionProgressMock.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        when(asyncActionProgressMock.getState()).thenReturn(ActionStateType.CANCELLED);

        objectUnderTest.processNotification(notificationMock);
        verify(jobLogUtil, atLeast(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(jobUpdateServiceMock, atLeast(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notificationMock);
        verify(activityUtilsMock, times(0)).getModifiedAttributes(notificationMock.getDpsDataChangedEvent());
    }

    @Test
    public void testprecheckHandleTimeout() {
        final long activityJobId = 1;
        objectUnderTest.precheckHandleTimeout(activityJobId);
        verify(activityUtilsMock).failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.VERIFY);
    }

}
