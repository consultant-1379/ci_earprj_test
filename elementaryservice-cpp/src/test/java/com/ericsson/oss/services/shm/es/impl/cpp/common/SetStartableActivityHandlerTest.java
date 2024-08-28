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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.moaction.retry.cpp.backup.BackupRetryPolicy;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class SetStartableActivityHandlerTest {

    @InjectMocks
    SetStartableActivityHandler setStartableActivityHandler;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private CommonCvOperations commonCvOperations;

    @Mock
    private BackupRetryPolicy backupRetryPolicy;

    @Mock
    ConfigurationVersionService configurationVersionService;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProviderMock;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxyMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculatorMock;

    @Mock
    private CvNameProvider cvNameProvider;
    
    @Mock
    private JobStaticData jobStaticData;
    
    @Mock
    private JobStaticDataProvider jobStaticDataProvider;
    
    String cvName = "ConfigurationVersion1";
    String cvMoFdn = "ConfigurationVersion=1";

    Map<String, Object> cvMoAttr;
    Map<String, Object> neJobAttributes;

    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;

    String identity = "Some Identity";
    String type = "Standard";
    String jobExecutionUser = "TEST_USER";

    String neName = "Some Ne Name";
    String configurationVersionName = "Some CV Name";
    // String cvMoFdn = "Some Cv Mo Fdn";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    Map<String, Object> actionArguments;

    long activityJobId = 123l;

    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

    private void mockNeJobStaticData() throws JobDataNotFoundException {
        when(neJobStaticDataProviderMock.getNeJobStaticData(Matchers.eq(activityJobId), anyString())).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getActivityStartTime()).thenReturn(new Date().getTime());
        when(neJobStaticDataMock.getNodeName()).thenReturn(neName);
        when(neJobStaticDataMock.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
    }

    @Test
    public void testCancel() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        setActivityJobPo();
        setNeJobPo();
        setMainJobPo();
        setConfigVersionMo();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        assertEquals(null, setStartableActivityHandler.cancelSetStartableAction(activityJobId, neJobStaticDataMock).getActivityResultEnum());
    }

    @Test
    public void executeSetStartableMoAction() throws JobDataNotFoundException {
        mockNeJobStaticData();
        when(commonCvOperations.executeActionOnMo(anyString(), anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenReturn(1);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        final boolean result = setStartableActivityHandler.executeSetStartableMoAction(activityJobId, neJobStaticDataMock, cvName, cvMoFdn);
        assertTrue(result);
        verify(activityUtils, times(3)).prepareJobPropertyList(anyList(), anyString(), anyString());
    }

    @Test
    public void executeSetStartableMoAction_Failed() throws JobDataNotFoundException {
        mockNeJobStaticData();
        when(commonCvOperations.executeActionOnMo(anyString(), anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(Exception.class);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        final boolean result = setStartableActivityHandler.executeSetStartableMoAction(activityJobId, neJobStaticDataMock, cvName, cvMoFdn);
        assertTrue(!result);
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), anyList(), anyList(), Matchers.anyDouble());
    }

    @Test
    public void handleTimeout_Fail() throws JobDataNotFoundException {
        mockNeJobStaticData();
        when(commonCvOperations.executeActionOnMo(anyString(), anyString(), Matchers.anyMap(), Matchers.any(RetryPolicy.class))).thenThrow(Exception.class);
        final ActivityStepResult activityStepResult = setStartableActivityHandler.handleTimeoutSetStartableAction(activityJobId, neJobStaticDataMock);
        assertTrue(activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        verify(activityUtils, times(1)).prepareJobPropertyList(anyList(), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), anyList(), anyList(), Matchers.anyDouble());
    }

    @Test
    public void testCancelTimeout() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.STARTABLE_CV_NAME)).thenReturn(configurationVersionName);
        setActivityJobPo();
        setNeJobPo();
        setMainJobPo();
        setConfigVersionMo();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, setStartableActivityHandler.cancelTimeoutSetStartable(neJobStaticDataMock, true).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeout() throws JobDataNotFoundException {
        mockNeJobStaticData();
        neJobAttributes = setNeJobPo();
        when(jobConfigServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        setActivityJobPo();
        setNeJobPo();
        setMainJobPo();
        setConfigVersionMo();
        when(jobStaticDataProvider.getJobStaticData(neJobStaticDataMock.getMainJobId())).thenReturn(jobStaticData);
        when(cvNameProvider.getConfigurationVersionName(neJobStaticDataMock, BackupActivityConstants.STARTABLE_CV_NAME)).thenReturn(configurationVersionName);
        final ActivityStepResult activityStepResult = setStartableActivityHandler.handleTimeoutSetStartableAction(activityJobId, neJobStaticDataMock);
        assertNotNull(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
    }

    private Map<String, Object> setNeJobPo() {
        final Map<String, Object> neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.NE_NAME, neName);
        neJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final List<Map<String, String>> neJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> neJobProperty = new HashMap<String, String>();
        neJobProperty.put(ShmConstants.KEY, BackupActivityConstants.STARTABLE_CV_NAME);
        neJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        neJobPropertyList.add(neJobProperty);
        neJobAttr.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);
        return neJobAttr;
    }

    private void setMainJobPo() {
        final Map<String, Object> mainJobAttr = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.STARTABLE_CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttr);
    }

    private void setActivityJobPo() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();

        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);

        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobConfPropertyList.add(mainJobPropertyCvType);

        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(123L)).thenReturn(activityJobAttr);
        when(activityUtils.getPoAttributes(123L)).thenReturn(activityJobAttr);
    }

    private void setConfigVersionMo() {
        setActivityJobPo();
        setNeJobPo();
        cvMoAttr = new HashMap<String, Object>();
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUpgradePackage);

        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, configurationVersionName);

        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUpgradePackage);
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);

        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final Map<String, String> adminData = new HashMap<String, String>();
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "CXP102051/1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, "R4D21");
        upMoAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
    }

}
