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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedAttributeException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.UpgradeJobConfigurationListener;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.FragmentHandler;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.upgrade.api.UpgradePrecheckResponse;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DpsDataChangedEvent.class })
public class PrepareServiceTest {

    long activityJobId = 1;
    long mainJobId = 3;
    long neJobId = 3;

    String nodeName = "SomeNodeName";
    String upMoFdn = "Some UP MO Fdn";
    String swmMoFdn = "Some SwM MO Fdn";
    String businessKey = "Some Business Key";
    short progressPercentage = 10;
    private static final String PREPARE_MO_ACTION = "prepare";
    private static final String CREATE_MO_ACTION = "createUpgradePackage";
    private static final String PREPARE_ACTIVITY_NAME = "prepare";
    private static final String CREATE_ACTIVITY_NAME = "createUpgradePackage";

    String inputVersion = "inputVersion";

    Map<String, Object> mainJobAttributes;
    Map<String, Object> activityJobAttributes;

    @Mock
    private EcimSwMUtils ecimSwmUtils;

    @Mock
    private UpMoServiceRetryProxy upMoService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private Notification notification;

    @Mock
    private NotificationSubject notificationSubject;

    @InjectMocks
    private PrepareService objectUnderTest;

    @Mock
    private AsyncActionProgress asyncActionProgress;

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private OssModelInfoProvider ossModelInfoProviderMock;

    @Mock
    private OssModelInfo ossModelInfoMock;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private DpsDataChangedEvent dpsDataChangedEvent;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private PrepareServiceHandler prepareServiceHandler;

    @Mock
    private EcimUpgradeInfo ecimUpgradeInfo;

    @Mock
    private UpgradeJobConfigurationListener upgradeJobConfigurationListener;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    private FragmentHandler fragmentHandler;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private UpgradePrecheckResponse precheckResponse;

    @Mock
    private List<Map<String, Object>> jobLogList;

    @Mock
    private ExecuteResponse executeResponse;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPrecheckWhenUpMoDoesNotExists() throws UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, MoNotFoundException, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo ecimUpgradeInfo = mockUpgradeJobEnvironment();

        final ActivityAllowed preparePrecheck = new ActivityAllowed();

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        preparePrecheck.setActivityAllowed(false);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPrecheckWhenUpMoExistsWithValidState() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo ecimUpgradeInfo = mockUpgradeJobEnvironment();
        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    @Test
    public void testasyncPrecheckWhenUpMoExistsWithInvalidState() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo ecimUpgradeInfo = mockUpgradeJobEnvironment();
        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        preparePrecheck.setActivityAllowed(false);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPrecheckWhenIsActivityAllowedThrowsUnsupportedFragmentException() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo environmentAttributes = mockUpgradeJobEnvironment();
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        doThrow(UnsupportedFragmentException.class).when(upMoService).isActivityAllowed(PREPARE_MO_ACTION, environmentAttributes);

        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        when(precheckResponse.getActivityStepResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        when(prepareServiceHandler.handleExceptionForPrecheck(Matchers.anyString(), Matchers.any(EcimUpgradeInfo.class), Matchers.anyList(), Matchers.anyString())).thenReturn(precheckResponse);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.PREPARE, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPrecheckWhenIsActivityAllowedThrowsSoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo environmentAttributes = mockUpgradeJobEnvironment();

        doThrow(SoftwarePackageNameNotFound.class).when(upMoService).isActivityAllowed(PREPARE_MO_ACTION, environmentAttributes);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        when(precheckResponse.getActivityStepResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        when(prepareServiceHandler.handleExceptionForPrecheck(Matchers.anyString(), Matchers.any(EcimUpgradeInfo.class), Matchers.anyList(), Matchers.anyString())).thenReturn(precheckResponse);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.PREPARE, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testasyncPrecheckWhenIsActivityAllowedThrowsSoftwarePackagePoNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo environmentAttributes = mockUpgradeJobEnvironment();

        doThrow(SoftwarePackagePoNotFound.class).when(upMoService).isActivityAllowed(PREPARE_MO_ACTION, environmentAttributes);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        when(precheckResponse.getActivityStepResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        when(prepareServiceHandler.handleExceptionForPrecheck(Matchers.anyString(), Matchers.any(EcimUpgradeInfo.class), Matchers.anyList(), Matchers.anyString())).thenReturn(precheckResponse);
        objectUnderTest.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.PREPARE, ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testPrecheckWhenUpMoDoesNotExists() throws UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, MoNotFoundException, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo ecimUpgradeInfo = mockUpgradeJobEnvironment();

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(false);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheckWhenUpMoExistsWithValidState() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo ecimUpgradeInfo = mockUpgradeJobEnvironment();

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheckWhenUpMoExistsWithInvalidState() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo ecimUpgradeInfo = mockUpgradeJobEnvironment();

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(false);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheckWhenIsActivityAllowedThrowsUnsupportedFragmentException() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo environmentAttributes = mockUpgradeJobEnvironment();
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        doThrow(UnsupportedFragmentException.class).when(upMoService).isActivityAllowed(PREPARE_MO_ACTION, environmentAttributes);
        when(prepareServiceHandler.handleExceptionForPrecheck(Matchers.anyString(), Matchers.any(EcimUpgradeInfo.class), Matchers.anyList(), Matchers.anyString())).thenReturn(precheckResponse);
        when(precheckResponse.getActivityStepResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheckWhenIsActivityAllowedThrowsSoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo environmentAttributes = mockUpgradeJobEnvironment();

        doThrow(SoftwarePackageNameNotFound.class).when(upMoService).isActivityAllowed(PREPARE_MO_ACTION, environmentAttributes);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        when(precheckResponse.getActivityStepResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        when(prepareServiceHandler.handleExceptionForPrecheck(Matchers.anyString(), Matchers.any(EcimUpgradeInfo.class), Matchers.anyList(), Matchers.anyString())).thenReturn(precheckResponse);

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheckWhenIsActivityAllowedThrowsSoftwarePackagePoNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, JobDataNotFoundException, UpgradePackageMoNotFoundException {
        final EcimUpgradeInfo environmentAttributes = mockUpgradeJobEnvironment();

        doThrow(SoftwarePackagePoNotFound.class).when(upMoService).isActivityAllowed(PREPARE_MO_ACTION, environmentAttributes);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");

        when(precheckResponse.getActivityStepResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        when(prepareServiceHandler.handleExceptionForPrecheck(Matchers.anyString(), Matchers.any(EcimUpgradeInfo.class), Matchers.anyList(), Matchers.anyString())).thenReturn(precheckResponse);

        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testExecuteWhenPrepareActionTriggeredSuccessfully() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_MO_ACTION);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);
        objectUnderTest.execute(activityJobId);

        verify(prepareServiceHandler).markActivityWithTriggeredAction(activityJobId, PREPARE_ACTIVITY_NAME);
        verify(prepareServiceHandler).triggerPrepareAction(eq(upMoFdn), eq(ecimUpgradeInfo), Matchers.any(JobActivityInfo.class), Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenUpdateSmrsDetailsThrowsException() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UnsupportedAttributeException, UpgradePackageMoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_MO_ACTION);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);
        doThrow(Exception.class).when(prepareServiceHandler).updateSmrsDetailsIfRequired(ecimUpgradeInfo, jobLogList);

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(prepareServiceHandler).markActivityWithTriggeredAction(activityJobId, PREPARE_ACTIVITY_NAME);
        verify(prepareServiceHandler).triggerPrepareAction(eq(upMoFdn), eq(ecimUpgradeInfo), Matchers.any(JobActivityInfo.class), Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenCreateActionTriggeredSuccessfully() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(null);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(false);

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_ACTIVITY_NAME, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(prepareServiceHandler).markActivityWithTriggeredAction(activityJobId, CREATE_ACTIVITY_NAME);
        verify(prepareServiceHandler).triggerCreateAction(eq(ecimUpgradeInfo), Matchers.any(JobActivityInfo.class), Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenCreateActionTriggeredSuccessfullyButUpMoDoesnotExist() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(null);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_MO_ACTION);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(false);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.MO_DOES_NOT_EXIST, "Upgrade Package Mo is not found")), Matchers.any(Date.class),
                eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(activityUtils).failActivity(activityJobId, jobLogList, businessKey, PREPARE_ACTIVITY_NAME);
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);

    }

    @Test
    public void testExecuteWhenPrepareActionTriggerredThrowsSoftwarePackagePoNotFound() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(null);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        doThrow(SoftwarePackagePoNotFound.class).when(prepareServiceHandler).triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.PREPARE)), Matchers.any(Date.class),
                eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(prepareServiceHandler).doExecutePostValidationForPrepareAction((EcimUpgradeInfo) Matchers.anyObject(), (ExecuteResponse) Matchers.anyObject(), (JobActivityInfo) Matchers.anyObject(),
                Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenPrepareActionTriggerredThrowsArgumentBuilderException() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(null);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        doThrow(ArgumentBuilderException.class).when(prepareServiceHandler).triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.PREPARE)), Matchers.any(Date.class),
                eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(prepareServiceHandler).doExecutePostValidationForPrepareAction((EcimUpgradeInfo) Matchers.anyObject(), (ExecuteResponse) Matchers.anyObject(), (JobActivityInfo) Matchers.anyObject(),
                Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenPrepareActionTriggerredThrowsMediationServiceException() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(null);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        doThrow(exception).when(prepareServiceHandler).triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.PREPARE, "MediationServiceException")),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(prepareServiceHandler).doExecutePostValidationForPrepareAction((EcimUpgradeInfo) Matchers.anyObject(), (ExecuteResponse) Matchers.anyObject(), (JobActivityInfo) Matchers.anyObject(),
                Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenCreateActionTriggeredThrowsUnsupportedFragmentException() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(null);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(null);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(false);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        doThrow(UnsupportedFragmentException.class).when(prepareServiceHandler).triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName)), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.ERROR.toString()));
        verify(prepareServiceHandler).doExecutePostValidationForCreateAction((EcimUpgradeInfo) Matchers.anyObject(), (ExecuteResponse) Matchers.anyObject(), (JobActivityInfo) Matchers.anyObject(),
                Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testExecuteWhenPrepareAction_WhenTbacFailed() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_MO_ACTION);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_MO_ACTION, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(false);

        objectUnderTest.execute(activityJobId);

        verify(activityUtils).failActivity(activityJobId, jobLogList, businessKey, PREPARE_ACTIVITY_NAME);
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);

    }

    @Test
    public void testExecuteWhenCreateActionTriggeredThrowsException() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, JobDataNotFoundException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        final ActivityAllowed preparePrecheck = new ActivityAllowed();
        preparePrecheck.setActivityAllowed(true);
        preparePrecheck.setUpMoFdn(upMoFdn);
        when(upMoService.isActivityAllowed(PREPARE_ACTIVITY_NAME, ecimUpgradeInfo)).thenReturn(preparePrecheck);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("neType");
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(null);
        when(upMoService.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(false);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        doThrow(Exception.class).when(prepareServiceHandler).triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, PREPARE_ACTIVITY_NAME)).thenReturn(true);

        objectUnderTest.execute(activityJobId);

        verify(prepareServiceHandler).doExecutePostValidationForCreateAction((EcimUpgradeInfo) Matchers.anyObject(), (ExecuteResponse) Matchers.anyObject(), (JobActivityInfo) Matchers.anyObject(),
                Matchers.anyList());
        InOrder inorder = inOrder(neJobStaticDataProvider, ecimSwmUtils);
        inorder.verify(neJobStaticDataProvider).getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        inorder.verify(ecimSwmUtils).getEcimUpgradeInformation(activityJobId);
    }

    @Test
    public void testProcessNotificationForAVCNotifications() {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

        objectUnderTest.processNotification(notification);
        verify(prepareServiceHandler).processAVCNotifications(notification, jobActivityInfo);
    }

    @Test
    public void testProcessNotificationForCreateNotifications() {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);

        objectUnderTest.processNotification(notification);
        verify(prepareServiceHandler).processCreateNotifications(notification, jobActivityInfo);
    }

    @Test
    public void testHandleTimeoutForCreateActivityAsSuccess() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgress);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.REPEAT_EXECUTE, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForCreateActivityAsFailed() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgress);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.FAILED;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForPrepareActivityAsSuccess() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(PREPARE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgress);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForPrepareActivity(upMoFdn, ecimUpgradeInfo, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForPrepareActivityAsFailed() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(PREPARE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgress);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.FAILED;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForPrepareActivity(upMoFdn, ecimUpgradeInfo, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForCancelAsSuccess() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgress);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCancel(upMoFdn, ecimUpgradeInfo, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForOthers() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
            NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn("");
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);
        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(asyncActionProgress);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCancel(upMoFdn, ecimUpgradeInfo, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWhenThrowsSoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenThrow(SoftwarePackageNameNotFound.class);
        when(prepareServiceHandler.handleExceptionForTimeout(Matchers.anyString(), Matchers.anyString(), Matchers.anyList())).thenReturn(JobResult.FAILED);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWhenThrowsSoftwarePackagePoNotFound() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenThrow(SoftwarePackagePoNotFound.class);
        when(prepareServiceHandler.handleExceptionForTimeout(Matchers.anyString(), Matchers.anyString(), Matchers.anyList())).thenReturn(JobResult.FAILED);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWhenThrowsMoNotFoundException() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenThrow(MoNotFoundException.class);
        when(prepareServiceHandler.handleExceptionForTimeout(Matchers.anyString(), Matchers.anyString(), Matchers.anyList())).thenReturn(JobResult.FAILED);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWhenThrowsUnsupportedFragmentException() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenThrow(UnsupportedFragmentException.class);
        when(prepareServiceHandler.handleExceptionForTimeout(Matchers.anyString(), Matchers.anyString(), Matchers.anyList())).thenReturn(JobResult.FAILED);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWhenThrowsArgumentBuilderException() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound,
            ArgumentBuilderException, NodeAttributesReaderException, JobDataNotFoundException {
        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(activityUtils.getActivityInfo(activityJobId, PrepareService.class)).thenReturn(jobActivityInfo);

        when(upMoService.getAsyncActionProgress(ecimUpgradeInfo)).thenThrow(ArgumentBuilderException.class);
        when(prepareServiceHandler.handleExceptionForTimeout(Matchers.anyString(), Matchers.anyString(), Matchers.anyList())).thenReturn(JobResult.FAILED);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(prepareServiceHandler.validateActionProgress(asyncActionProgress, nodeName, ecimUpgradeInfo, jobLogList, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);
        final JobResult jobResult = JobResult.SUCCESS;
        when(upMoService.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(prepareServiceHandler.handleTimeoutForCreateActivity(upMoFdn, ecimUpgradeInfo, asyncActionProgress, jobActivityInfo)).thenReturn(jobResult);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testprecheckHandleTimeout() {
        objectUnderTest.precheckHandleTimeout(activityJobId);
        verify(prepareServiceHandler).precheckHandleTimeout(activityJobId);
    }

    private EcimUpgradeInfo mockUpgradeJobEnvironment() throws JobDataNotFoundException, MoNotFoundException {

        final EcimUpgradeInfo ecimUpgradeInfo = new EcimUpgradeInfo();
        mockJobEnvironment();
        ecimUpgradeInfo.setNeJobStaticData(neJobStaticData);
        ecimUpgradeInfo.setActivityJobId(activityJobId);

        when(ecimSwmUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);

        return ecimUpgradeInfo;
    }

    @Before
    public void mockJobEnvironment() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);

        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(jobEnvironment.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> actionIdProperty = new HashMap<String, String>();
        actionIdProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        actionIdProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(3));
        jobPropertyList.add(actionIdProperty);

        activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
    }

}
