package com.ericsson.oss.services.shm.es.impl.ecim.licensing;

///*------------------------------------------------------------------------------
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
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants.ReportProgress;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLicensingInfo;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLmUtils;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
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
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class FailsafeActivateDeactivateServiceTest {

    @InjectMocks
    private InstallLicenseKeyFileService installLicenseKeyFileService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private EcimLmUtils ecimLmUtils;

    @Mock
    private EcimLicensingInfo ecimLicensingInfo;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private LicenseMoService licenseMoService;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private Notification notification;

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private DpsDataChangedEvent dpsDataChangedEvent;

    @Mock
    private AsyncActionProgress reportProgress;

    @Mock
    private RetryManager retryManager;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @InjectMocks
    private FailsafeActivateDeactivateService failsafeActviateDeactivateService;

    @Mock
    private JobParameterChangeListener jobParameterChangeListener;

    @Mock
    private ManagedObject managedobject;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private Map<String, Object> actionResult;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    NEJobStaticData neJobStaticData;

    @Mock
    private NetworkElementData networkElement;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    private static final long activityJobId = 123l;
    private static final short actionId = 1;
    private static final long mainJobId = 124l;
    private static final String businessKey = "businessKey";
    private static final String nodeName = "NE01";
    private static final String ACTIVITY_NAME = "install";
    public static final String BRM_BACKUP_FAIL_SAFE_MO = "BrmFailsafeBackup";
    private static final String FAILSAFE_ACTIVATE = "ACTIVATE";
    private static final String FAILSAFE_DEACTIVATE = "DEACTIVATE";
    private static final String neType = "RadioNode";
    private static final String ossModelIdentity = "2042-630-876";

    @Before
    public void setUp() throws UnsupportedFragmentException, MoNotFoundException, JobDataNotFoundException {
        when(ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement)).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.getBusinessKey()).thenReturn(businessKey);
        when(ecimLicensingInfo.getActionId()).thenReturn(actionId);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElement);
        when(neJobStaticDataProvider.getNeJobStaticData(actionId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElement.getNeType()).thenReturn(neType);
        when(networkElement.getOssModelIdentity()).thenReturn(ossModelIdentity);
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

    }

    @Test
    public void testTriggerBrmFailsafeForActivate() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP)).thenReturn("activityJobId");
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType(neType);
        networkElement.setName(jobEnvironment.getNodeName());
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(jobEnvironment.getNodeName()))).thenReturn(networkElementsList);

        failsafeActviateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, FAILSAFE_ACTIVATE, jobActivityInfo);
        verify(brmMoServiceRetryProxy, times(1)).performBrmFailSafeActivate(anyString(), anyString());

    }

    @Test
    public void testTriggerBrmFailsafeForDeActivate() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE)).thenReturn("activityJobId");
        when(managedobject.getFdn()).thenReturn(nodeName);
        failsafeActviateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, FAILSAFE_ACTIVATE, jobActivityInfo);
        verify(brmMoServiceRetryProxy, times(1)).getBrmFailsafeBackupMo(anyString());
        Assert.assertEquals(0, brmMoServiceRetryProxy.performBrmFailSafeActivate(anyString(), anyString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTriggerBrmFailsafeActivateDeActivateWhenMoNotFound() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenThrow(new MoNotFoundException("Mo not found exception"));
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType(neType);
        networkElement.setName(jobEnvironment.getNodeName());
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(jobEnvironment.getNodeName()))).thenReturn(networkElementsList);

        failsafeActviateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, FAILSAFE_ACTIVATE, jobActivityInfo);
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTriggerBrmFailsafe_activateFailureCase() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(null);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType(neType);
        networkElement.setName(jobEnvironment.getNodeName());
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(jobEnvironment.getNodeName()))).thenReturn(networkElementsList);

        failsafeActviateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, FAILSAFE_ACTIVATE, jobActivityInfo);
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void handleTimeoutForActivateDeactivateActivity() throws MoNotFoundException, UnsupportedFragmentException {
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, FAILSAFE_DEACTIVATE.toLowerCase());
        actionResult.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO);
        actionResult.put(ActivityConstants.JOB_LOG_MESSAGE, "Job executed successfully");
        actionResult.put(ActivityConstants.JOB_RESULT, JobResult.SUCCESS);
        final AsyncActionProgress asyreportProgress = new AsyncActionProgress(actionResult);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(ecimLmUtils.getActionStatus(asyreportProgress, FAILSAFE_DEACTIVATE.toLowerCase())).thenReturn(actionResult);
        failsafeActviateDeactivateService.handleTimeoutForActivateDeactivateActivity(ecimLicensingInfo, activityJobId, nodeName, asyreportProgress, jobActivityInfo);
        verify(activityUtils, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void handleProgressReportState() {
        when(reportProgress.getState()).thenReturn(ActionStateType.FINISHED);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        failsafeActviateDeactivateService.handleProgressReportState(nodeName, ecimLicensingInfo, activityJobId, neJobStaticData, reportProgress, BRM_BACKUP_FAIL_SAFE_MO, jobActivityInfo);
        verify(activityUtils, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void handleProgressReportStateWithReult() {
        when(reportProgress.getState()).thenReturn(ActionStateType.FINISHED);
        when(reportProgress.getResult()).thenReturn(ActionResultType.SUCCESS);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        failsafeActviateDeactivateService.handleProgressReportState(nodeName, ecimLicensingInfo, activityJobId, neJobStaticData, reportProgress, BRM_BACKUP_FAIL_SAFE_MO, jobActivityInfo);
        verify(activityUtils, times(2)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
    }

    @Test
    public void validateActionProgressReport() {
        assertTrue(failsafeActviateDeactivateService.validateActionProgressReport(reportProgress));
    }

}
