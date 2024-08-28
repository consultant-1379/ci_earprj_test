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
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.CVMO_CORRUPTED_UPGRADE_PACKAGES;
import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.CVMO_MISSING_UPGRADE_PACKAGES;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.resources.Resources;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.InstallActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.shm.backup.constants.CppBackupConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Resources.class)
@SuppressWarnings("unchecked")
public class RestorePrecheckHandlerTest {

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private ConfigurationVersionService cvService;

    @Mock
    private ConfigurationVersionUtils configurationVersionUtils;

    @Mock
    private SmrsRetryPolicies smrsRetryPolicies;

    @Mock
    private RetryPolicy retryPolicy;

    @Mock
    private Resource resource;

    @Mock
    private ManagedObject cvMo;

    @Mock
    private JobEnvironment jobEnvMock;

    @InjectMocks
    private RestorePrecheckHandler restorePrecheckHandlerMock;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private Map<String, String> jobConfigMapMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private ActivityStepResult activityStepResult;

    @Mock
    private UpgradePackageService upgradePackageService;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private InstallActivityHandler localInstallationService;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

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

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    protected SystemRecorder systemRecorder;

    //private JobActivityInfo jobActivityInfoMock;
    private static final String PRODUCT_NUMBER = "null";
    private static final String PRODUCT_REVISION = "null";
    private final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    private static final long ACTIVITY_JOB_ID = 1234l;
    private final long neJobId = 789546l;
    private final long mainJobId = 123456l;
    private final String neName = "neName";
    final String businessKey = "Some Business Key";
    private final String MISSING_PKG_SELECTION = "INSTALL_MISSING_UPGRADE_PACKAGES";
    public static final String CORRUPTED_PKG_SELECTION = "REPLACE_CORRUPTED_UPGRADE_PACKAGES";

    private final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    private final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
    private String jobExecutedUser = "xprapav";

    @Before
    public void mockJobEnvironment() throws JobDataNotFoundException, MoNotFoundException {
        //Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        //Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        //Mockito.when(jobActivityInfoMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(neJobStaticDataProvider.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());

        when(configurationServiceRetryProxy.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttributes);
        when(configurationServiceRetryProxy.getNeJobAttributes(Matchers.anyLong())).thenReturn(neJobAttributes);
        when(configurationServiceRetryProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        when(networkElementRetrivalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
        //when(activityUtils.getPersistedActionId(Matchers.anyMap(), Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString())).thenReturn(ACTION_ID);
        when(activityUtils.isTreatAs(anyString())).thenReturn(neName);
    }

    @Test
    public void getRestorePrecheckHandlerTestForVerifyRestoreFAILED() {
        downloadAndVerifyMock("name", "STANDARD", "abcd");
        smrsMock();
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.VERIFY_RESTORE_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);

    }

    @Test
    public void getRestorePrecheckHandlerTestForVerifyRestoreSUCCESS() {
        downloadAndVerifyMock("name", "DOWNLOADED", "name");
        smrsMock();
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.VERIFY_RESTORE_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void getRestorePrecheckHandlerTestForVerifyRestoreSUCCESS1() {
        downloadAndVerifyMock("name", "downloaded", "name");
        smrsMock();
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.VERIFY_RESTORE_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void getRestorePrecheckHandlerTestForConfirmRestoreSUCCESS() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "AWAITING_RESTORE_CONFIRMATION");
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cvNameForDownload");
        map.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, "DOWNLOADED");
        list.add(map);
        attributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, list);
        final Map<String, Object> cvMo = new HashMap<String, Object>();
        cvMo.put(ShmConstants.FDN, ShmConstants.FDN);
        cvMo.put(ShmConstants.MO_ATTRIBUTES, attributeMap);
        when(cvService.getCVMoAttr(Matchers.anyString())).thenReturn(cvMo);

        //when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        //activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvNameForDownload");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        doNothing().when(activityUtils).recordEvent(anyString(), anyString(), anyString(), anyString());
        doNothing().when(activityUtils).addJobLog(anyString(), anyString(), anyList(), anyString());
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.CONFIRM_RESTORE_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));

    }

    @Test
    public void getRestorePrecheckHandlerTestForConfirmRestoreFailure() {
        when(activityUtils.getJobEnvironment(111111111l)).thenReturn(jobEnvMock);
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "name");
        map.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, "DOWNLOADED");
        list.add(map);
        attributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, list);
        final Map<String, Object> cvMo = new HashMap<String, Object>();
        cvMo.put(ShmConstants.FDN, ShmConstants.FDN);
        cvMo.put(ShmConstants.MO_ATTRIBUTES, attributeMap);
        when(jobEnvMock.getNodeName()).thenReturn("nodeName");
        when(cvService.getCVMoAttr(Matchers.anyString())).thenReturn(cvMo);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvNameForDownload");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        doNothing().when(activityUtils).recordEvent(anyString(), anyString(), anyString(), anyString());
        doNothing().when(activityUtils).addJobLog(anyString(), anyString(), anyList(), anyString());
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.CONFIRM_RESTORE_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void getRestorePrecheckHandlerTestForDownloadCvSkip() {
        downloadAndVerifyMock("name", "STANDARD", "name");
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.DOWNLOAD_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
    }

    @Test
    public void getRestorePrecheckHandlerTestForDownloadCvFailure2() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        downloadAndVerifyMock("name", "STANDARD", "ffff");
        smrsMock();
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.DOWNLOAD_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));

    }

    @Test
    public void getRestorePrecheckHandlerTestForDownloadCvFailure3() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        downloadAndVerifyMock("name", "STANDARD", "ffff");
        when(smrsRetryPolicies.getSmrsGeneralRetryPolicy()).thenThrow(Exception.class);
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.DOWNLOAD_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));

    }

    @Test
    public void getRestorePrecheckHandlerTestForDownloadCvSuccess1() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        downloadAndVerifyMock("name", "DOWNLOADED", "name");
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.DOWNLOAD_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void getRestorePrecheckHandlerTestForDownloadCvSuccess2() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        downloadAndVerifyMock("name", "STANDARD", "abcd");
        smrsMock();
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.DOWNLOAD_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));

    }

    @Test
    public void getRestorePrecheckHandlerTestForDownloadCvSuccess3() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        downloadAndVerifyMock("name", "downloaded", "name");
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.DOWNLOAD_CV);
        assertEquals(activityStepResult.getActivityResultEnum(), ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    private void downloadAndVerifyMock(final String name, final String type, final String cvNameForDownload) {
        when(activityUtils.getJobEnvironment(111111111l)).thenReturn(jobEnvMock);
        final Map<String, Object> cvMo = new HashMap<String, Object>();
        cvMo.put(ShmConstants.FDN, ShmConstants.FDN);
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        final Map<String, Object> cv = new HashMap<String, Object>();
        cv.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, name);
        cv.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, type);
        final List<Map<String, Object>> storedConfigurationVersion = new ArrayList<Map<String, Object>>();
        storedConfigurationVersion.add(cv);
        attributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersion);
        cvMo.put(ShmConstants.MO_ATTRIBUTES, attributeMap);
        when(jobEnvMock.getNodeName()).thenReturn("nodeName");
        when(cvService.getCVMoAttr(Matchers.anyString())).thenReturn(cvMo);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(cvNameForDownload);
        doNothing().when(activityUtils).recordEvent(anyString(), anyString(), anyString(), anyString());
        doNothing().when(activityUtils).addJobLog(anyString(), anyString(), anyList(), anyString());
    }

    @SuppressWarnings("deprecation")
    private void smrsMock() {
        when(smrsRetryPolicies.getSmrsGeneralRetryPolicy()).thenReturn(retryPolicy);
        when(smrsServiceUtil.getSmrsPath("nodeName", CppBackupConstants.BACKUP_ACCOUNT_TYPE, retryPolicy)).thenReturn("/abcd/defg/");
        mockStatic(Resources.class);
        when(Resources.getFileSystemResource("/abcd/defg/nodeName")).thenReturn(resource);
        final List<Resource> list = new ArrayList<Resource>();
        list.add(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.list()).thenReturn(list);
        when(resource.getName()).thenReturn("abcd.zip");
    }

    @Test
    public void testPreCheckWithEmptyMoList() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(mapMock.isEmpty()).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        verify(localInstallationService, never()).precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class));
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testPreCheck_invalidCVTypeNull() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(mapMock.size()).thenReturn(2);
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        verify(localInstallationService, never()).precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class));
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testPreCheck_invalidCVTypeNotDownloaded() {
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(mapMock.size()).thenReturn(2);
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("type");

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        verify(localInstallationService, never()).precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class));
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testPreCheck_noPkgsSelectedTobeInstalled() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        verify(localInstallationService, never()).precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class));
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testPreCheck_missingPkgsTobeChecked_skipExecution() {

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobEnvMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);

        when(mapMock.get(CVMO_MISSING_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Arrays.asList(MISSING_PKG_SELECTION), mapMock, neName, "ERBS", PlatformTypeEnum.CPP.name())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(MISSING_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
        verify(upgradePackageService, never()).getUpPoData(PRODUCT_NUMBER, PRODUCT_REVISION);
    }

    @Test
    public void testPreCheck_missingPkgsTobeChecked_noUPPoData_skipExecution() {
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);

        when(mapMock.get(CVMO_MISSING_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(MISSING_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(upgradePackageService).getUpPoData(null, null);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testPreCheck_missingPkgsTobeChecked_proceedExecution() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);

        when(mapMock.get(CVMO_MISSING_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(MISSING_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        when(upgradePackageService.getUpPoData(anyString(), anyString())).thenReturn(mapMock);
        when(mapMock.isEmpty()).thenReturn(false);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(upgradePackageService).getUpPoData(null, null);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
    }

    @Test
    public void testPreCheck_corruptedPkgsTobeChecked_skipExecution() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);

        when(mapMock.get(CVMO_CORRUPTED_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(CORRUPTED_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
        verify(upgradePackageService, never()).getUpPoData(PRODUCT_NUMBER, PRODUCT_REVISION);
    }

    @Test
    public void testPreCheck_corruptedPkgsTobeChecked_noUPPoData_skipExecution() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);

        when(mapMock.get(CVMO_CORRUPTED_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(CORRUPTED_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(upgradePackageService).getUpPoData(null, null);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        verify(jobUpdateService, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testPreCheck_corruptedPkgsTobeChecked_proceedExecution() {
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        //Mockito.when(jobActivityInfoMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(mapMock.get(CVMO_CORRUPTED_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.RESTORE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(CORRUPTED_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        when(upgradePackageService.getUpPoData(anyString(), anyString())).thenReturn(mapMock);
        when(mapMock.isEmpty()).thenReturn(false);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(upgradePackageService).getUpPoData(null, null);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
    }

    @Test
    public void testPreCheck_corruptedPkgsAndMissingPkgsTobeChecked_failExecution() {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvMock);
        when(cvService.getCVMoAttr(anyString())).thenReturn(mapMock);
        when(jobEnvMock.getNodeName()).thenReturn(neName);
        when(jobEnvMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(jobEnvMock.getMainJobAttributes()).thenReturn(mapMock);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(mapMock.size()).thenReturn(2);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvName");
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).thenReturn("cvName");
        when(mapMock.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE)).thenReturn("DOWNLOADED");
        when(mapMock.get(ShmConstants.MO_ATTRIBUTES)).thenReturn(mapMock);

        when(mapMock.get(CVMO_CORRUPTED_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(CVMO_MISSING_UPGRADE_PACKAGES)).thenReturn(Arrays.asList(mapMock));
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, RestoreInstallService.class)).thenReturn(jobActivityInfoMock);
        when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);

        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(CORRUPTED_PKG_SELECTION)).thenReturn("true");
        when(jobPropertyUtils.getPropertyValue(Arrays.asList(MISSING_PKG_SELECTION), mapMock, neName, "ERBS", PlatformTypeEnum.CPP.name())).thenReturn(jobConfigMapMock);
        when(jobConfigMapMock.get(MISSING_PKG_SELECTION)).thenReturn("true");
        when(localInstallationService.precheck(anyLong(), anyString(), anyString(), anyString(), Matchers.any(NEJobStaticData.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        restorePrecheckHandlerMock.getRestorePrecheckStatus(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV);
        verify(upgradePackageService, times(2)).getUpPoData(null, null);
        verify(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), anyList(), anyList(), anyDouble());
    }

    @Test
    public void getRestorePrecheckHandlerTestForVerifyRestoreSUCCESS_WhenTbacSuccess() throws JobDataNotFoundException, MoNotFoundException {
        downloadAndVerifyMock("name", "DOWNLOADED", "name");
        smrsMock();
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.VERIFY)).thenReturn(true);
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckResult(ACTIVITY_JOB_ID, ActivityConstants.VERIFY_RESTORE_CV, ActivityConstants.VERIFY);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void getRestorePrecheckHandlerTestForVerifyRestoreSUCCESS_WhenTbacFailed() throws JobDataNotFoundException, MoNotFoundException {
        downloadAndVerifyMock("name", "DOWNLOADED", "name");
        smrsMock();
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.VERIFY)).thenReturn(false);
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckResult(ACTIVITY_JOB_ID, ActivityConstants.VERIFY_RESTORE_CV, ActivityConstants.VERIFY);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @Test
    public void getRestorePrecheckHandlerTestForDefault() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "AWAITING_RESTORE_CONFIRMATION");
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cvNameForDownload");
        map.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, "DOWNLOADED");
        list.add(map);
        attributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, list);
        final Map<String, Object> cvMo = new HashMap<String, Object>();
        cvMo.put(ShmConstants.FDN, ShmConstants.FDN);
        cvMo.put(ShmConstants.MO_ATTRIBUTES, attributeMap);
        when(cvService.getCVMoAttr(Matchers.anyString())).thenReturn(cvMo);
        when(jobEnvMock.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn(jobExecutedUser);
        when(configurationVersionUtils.getNeJobPropertyValue(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn("cvNameForDownload");
        when(jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);
        doNothing().when(activityUtils).recordEvent(anyString(), anyString(), anyString(), anyString());
        doNothing().when(activityUtils).addJobLog(anyString(), anyString(), anyList(), anyString());
        doCallRealMethod().when(activityUtils).getActivityStepResult(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        final ActivityStepResult activityStepResult = restorePrecheckHandlerMock.getRestorePrecheckStatus(111111111l, ActivityConstants.UPLOAD_CV);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityStepResult.getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(111111111l, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(111111111l), anyLong(), eq(ActivityStepsEnum.PRECHECK));

    }
}
