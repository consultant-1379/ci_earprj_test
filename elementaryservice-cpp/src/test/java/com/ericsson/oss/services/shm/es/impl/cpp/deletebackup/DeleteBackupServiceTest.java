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

package com.ericsson.oss.services.shm.es.impl.cpp.deletebackup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class DeleteBackupServiceTest {

    @InjectMocks
    private DeleteBackupService objectUnderTest;

    @Mock
    @Inject
    private DpsReader dpsReadermock;

    @Mock
    @Inject
    private ActivityUtils activityUtils;

    @Mock
    @Inject
    private CommonCvOperations commonCvOperationsmock;

    @Mock
    @Inject
    private JobLogUtil jobLogUtil;

    @Mock
    @Inject
    private ActivityUtils activityUtilsmock;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    @Inject
    private JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    private ConfigurationVersionService configurationVersionService;

    @Mock
    @Inject
    private JobUpdateService jobUpdateService;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private DeleteSmrsBackupUtil deleteSmrsBackupServiceMock;

    @Mock
    JobEnvironment jobEnvironment;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private SmrsRetryPolicies smrsRetryPolicies;

    @Mock
    private RetryPolicy retryPolicy;

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    private FileResource fileResource;

    @Mock
    private Resource resource;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxyMock;

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

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;

    String neName = "Some Ne Name";
    String cvMoFdn = "Some Cv Mo Fdn";
    String configurationVersionName = "Some CV Name";
    String identity = "Some Identity";
    String type = "Standard";
    String operatorName = "Some Operator Name";
    String comment = "Some Comment";
    String jobName = "Some Job Name";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";
    Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    final Map<String, Object> processVariables = new HashMap<String, Object>();
    String neType = "ERBS";

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckOnNode() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> map = new HashMap<String, String>();
        neJobProperties.put(ActivityConstants.ROLL_BACK, false);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(neJobProperties);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     * @throws MoNotFoundException
     * @throws JobDataNotFoundException
     * @throws SecurityViolationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckOnEnm() throws JobDataNotFoundException, MoNotFoundException {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> map = new HashMap<String, String>();
        final List<String> cvNamesOnSmrs = new ArrayList<String>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        cvNamesOnSmrs.add("cv_test3");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|ENM");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "deletecv")).thenReturn(true);
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckStartableOnNode() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        neJobProperties.put(ActivityConstants.ROLL_BACK, false);
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test3");
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test3|NODE");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test3");
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(neJobProperties);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckOnlyStartable() {
        setActivityJobPoPrecheckFail();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test");
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     * @throws MoNotFoundException
     * @throws JobDataNotFoundException
     * @throws SecurityViolationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckLoadedOnENM() throws JobDataNotFoundException, MoNotFoundException {
        setActivityJobPo();
        final Map<String, String> map = new HashMap<String, String>();
        final List<String> bkpname = new ArrayList<String>();
        bkpname.add("cv_test3");
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test3");
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "deletecv")).thenReturn(true);
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckLoadedOnNODE() {
        setActivityJobPo();
        final Map<String, String> map = new HashMap<String, String>();
        final List<String> bkpname = new ArrayList<String>();
        bkpname.add("cv_test3");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test3");
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        neJobProperties.put(ActivityConstants.ROLL_BACK, false);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test3");
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(neJobProperties);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheckOnlyLoaded() {
        setActivityJobPoPrecheckFail();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test");
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_precheck_CVMoNull() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        assertNotNull(objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckFailure_ThrowsException() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtilsmock).recordEvent(any(String.class), any(String.class), any(String.class), any(String.class));
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenThrow(Exception.class);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_executeOnNode() {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test4");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test5");

        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test3");
        rollBackList.add("cv_test4");
        rollBackList.add("cv_test5");
        rollBackList.add("cv_test6");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.ROLL_BACK, "TRUE");
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     * @throws ArgumentBuilderException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_executeOnEnm() throws MoNotFoundException, ArgumentBuilderException {
        setActivityJobPo();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|ENM");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test4");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test5");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test4");
        rollBackList.add("cv_test5");
        rollBackList.add("cv_test6");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "neType")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("neType");
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "neType")).thenReturn(true);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     * @throws MoNotFoundException
     * @throws ArgumentBuilderException
     */
    @SuppressWarnings({ "unchecked", "unused" })
    @Test
    public void test_executeRollBackTrue() throws ArgumentBuilderException, MoNotFoundException {
        setActivityJobPoRollBackTrue();
        setupJobAttributes();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "neType")).thenReturn(false);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_executeTrue() {
        setActivityJobPoRollBackTrue();
        setupJobAttributes();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        final Map<String, String> map = new HashMap<>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        activityJobAttributes = new HashMap<>();
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_executeWhenBackupIsUnavailableOnNode() {
        setActivityJobPoRollBackTrue();
        setupJobAttributes();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(new RuntimeException(JobLogConstants.CV_DOES_NOT_EXISTS));
        final Map<String, String> map = new HashMap<>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    /**
     * 
     */
    private void setupJobAttributes() {
        final Map<String, Object> cvMoMap = new HashMap<>();
        final Map<String, Object> cvMoAttr = new HashMap<>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<>();
        final Map<String, String> storedConfigurationVersion = new HashMap<>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test41");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test51");
        final List<String> rollBackList = new ArrayList<>();
        rollBackList.add("cv_test3");
        rollBackList.add("cv_test41");
        rollBackList.add("cv_test51");
        rollBackList.add("cv_test61");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_executeOnEnmWhenBackupIsUnavailableOnSMRS() throws MoNotFoundException, ArgumentBuilderException {
        setActivityJobPo();
        setupJobAttributes();
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "neType")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("neType");
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "neType")).thenReturn(false);
        final Map<String, String> map = new HashMap<>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithMediationServiceException() {
        setActivityJobPoRollBackTrue();
        setupJobAttributes();
        final Exception exception = new RuntimeException();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(exception);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));

    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_executeRollbackTrue() {
        setupJobAttributes();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(new RuntimeException("Failed as the backup not found"));
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        activityJobAttributes = new HashMap<String, Object>();
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     * @throws MoNotFoundException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_handleTimeOutForDeleteonENM() throws MoNotFoundException {
        setActivityJobPoRollBackTrue();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ShmConstants.KEY, BackupActivityConstants.CURRENT_BACKUP);
        map.put(ShmConstants.VALUE, "cv_test41|ENM");
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> workflowAttributes = new HashMap<String, String>();
        workflowAttributes.put(ShmConstants.BUSINESS_KEY, "abc");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test41");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test412|ENM");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test512|ENM");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test4");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("neType");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.NE_NAME, neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicy);
        final String smrsFilePath = "dummyPath";
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicy)).thenReturn(smrsFilePath);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(true);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    /**
     * This method performs delete operation after checking CV state(RollbackList, Startable)
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_handleTimeOutForDeleteonnode() {
        setActivityJobPoRollBackTrue();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test41|NODE");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test412|NODE");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test512|NODE");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test4|NODE");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    /**
     * This method performs cancel operation
     * 
     * @return
     */
    @Test
    public void test_cancel() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);

        objectUnderTest.cancel(activityJobId);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeoutForDeleteonNode() {
        setActivityJobPoRollBackTrue();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test41|NODE");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test412|NODE");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test512|NODE");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test4|NODE");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResult = objectUnderTest.cancelTimeout(activityJobId, false);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeoutForDeleteonENM() throws MoNotFoundException {
        setActivityJobPoRollBackTrue();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(BackupActivityConstants.CURRENT_BACKUP, "cv_test41|ENM");
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> workflowAttributes = new HashMap<String, String>();
        workflowAttributes.put(ShmConstants.BUSINESS_KEY, "abc");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test41");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test412|ENM");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test512|ENM");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test4");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("neType");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, activityStepResult.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    /**
     * This method performs the PreCheck for Delete backup activity
     * 
     * @return
     * @throws MoNotFoundException
     * @throws JobDataNotFoundException
     * @throws SecurityViolationException
     */
    @Test
    public void test_precheckLoadedOnENM_WhenTbacNotEnabled() throws JobDataNotFoundException, MoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "deletecv")).thenReturn(false);
        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);
        assertNotNull(activityStepResult.getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    private void setActivityJobPo() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, activityJobId);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, activityJobId);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, activityJobId);
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

        final List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put("key", ActivityConstants.ROLL_BACK);
        jobProperty.put("value", "FALSE");
        mainJobConfPropertyList.add(jobProperty);
        final Map<String, Object> neJobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, String>> properties = new ArrayList<>();
        final Map<String, String> propertiesmap = new HashMap<String, String>();
        propertiesmap.put("key", ActivityConstants.CV_NAME);
        propertiesmap.put("value", "cv_test|NODE,cv_test3|NODE,cv_test4|NODE");
        properties.add(propertiesmap);
        neJobPropertyMap.put(ShmConstants.NE_NAME, neName);
        neJobPropertyMap.put(ActivityConstants.JOB_PROPERTIES, properties);
        neJobPropertyList.add(neJobPropertyMap);

        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, "cv_test|NODE");
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        activityJobAttr.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobUpdateService.retrieveJobWithRetry(123L)).thenReturn(activityJobAttr);
        when(activityUtils.getPoAttributes(123L)).thenReturn(activityJobAttr);

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
    }

    private void setActivityJobPoPrecheckFail() {

        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, activityJobId);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, activityJobId);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, activityJobId);
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

        final List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put("key", ActivityConstants.ROLL_BACK);
        jobProperty.put("value", "FALSE");
        mainJobConfPropertyList.add(jobProperty);
        final Map<String, Object> neJobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, String>> properties = new ArrayList<>();
        final Map<String, String> propertiesmap = new HashMap<String, String>();
        propertiesmap.put("key", ActivityConstants.CV_NAME);
        propertiesmap.put("value", "cv_test");
        properties.add(propertiesmap);
        neJobPropertyMap.put(ShmConstants.NE_NAME, neName);
        neJobPropertyMap.put(ActivityConstants.JOB_PROPERTIES, properties);
        neJobPropertyList.add(neJobPropertyMap);

        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, "cv_test|NODE");
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(123L)).thenReturn(activityJobAttr);
        when(activityUtils.getPoAttributes(123L)).thenReturn(activityJobAttr);

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
    }

    private void setActivityJobPoRollBackTrue() {

        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, activityJobId);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, activityJobId);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, activityJobId);
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

        final List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put("key", ActivityConstants.ROLL_BACK);
        jobProperty.put("value", "TRUE");
        mainJobConfPropertyList.add(jobProperty);
        final Map<String, Object> neJobPropertyMap = new HashMap<String, Object>();
        final List<Map<String, String>> properties = new ArrayList<>();
        final Map<String, String> propertiesmap = new HashMap<String, String>();
        propertiesmap.put("key", ActivityConstants.CV_NAME);
        propertiesmap.put("value", "cv_test|NODE,cv_test3|NODE,cv_test4|NODE");
        properties.add(propertiesmap);
        neJobPropertyMap.put(ShmConstants.NE_NAME, neName);
        neJobPropertyMap.put(ActivityConstants.JOB_PROPERTIES, properties);
        neJobPropertyList.add(neJobPropertyMap);

        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, "cv_test|NODE");
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(123L)).thenReturn(activityJobAttr);
        when(activityUtils.getPoAttributes(123L)).thenReturn(activityJobAttr);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_handleTimeOutForDeleteonENMWhenBackupNotDeleted() throws MoNotFoundException {
        setActivityJobPoRollBackTrue();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ShmConstants.KEY, BackupActivityConstants.CURRENT_BACKUP);
        map.put(ShmConstants.VALUE, "cv_test41|ENM");
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> workflowAttributes = new HashMap<String, String>();
        workflowAttributes.put(ShmConstants.BUSINESS_KEY, "abc");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test41");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test412|ENM");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test512|ENM");
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test4");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("neType");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.NE_NAME, neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicy);
        final String smrsFilePath = "dummyPath";
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicy)).thenReturn(smrsFilePath);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(false);
        final ActivityStepResult activityStepResult = objectUnderTest.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, activityStepResult.getActivityResultEnum());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testPrecheckForSuccessfulcvscount() throws JobDataNotFoundException, MoNotFoundException {

        setActivityJobPoRollBackTrue();
        setAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        precheckCommon();

    }

    @Test
    public void testUpdateJobLogsForDeletionFailedCVs() throws JobDataNotFoundException, MoNotFoundException {

        setActivityJobPoRollBackTrue();
        setAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        precheckCommon();

    }

    @Test
    public void testDeleteCvFromEnm() throws MoNotFoundException {

        setupJobAttributes();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");

        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(false);
        executeCommon(2);

    }

    @Test
    public void testDeleteCvFromEnmThrowsMoNotFoundException() throws MoNotFoundException {

        setupJobAttributes();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");

        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenThrow(MoNotFoundException.class);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(true);
        executeCommon(1);
    }

    @Test
    public void testDeleteCvFromEnmThrowsException() throws MoNotFoundException {

        setupJobAttributes();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|ENM,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "ERBS")).thenThrow(Exception.class);
        executeCommon(2);

    }

    @Test
    public void testDeleteCvFromNode() throws MoNotFoundException {

        setAttributesForDelete();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test51|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        executeCommon(2);

    }

    @Test
    public void testDeleteCvFromNodeForCvMoAttrIsNull() throws MoNotFoundException {

        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(null);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(false);

        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test51|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        executeCommon(2);

    }

    @Test
    public void testDeleteCvFromNodeThrowsException() throws MoNotFoundException {
        setData();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(new RuntimeException(JobLogConstants.CV_DOES_NOT_EXISTS));
        executeCommon(1);

    }

    @Test
    public void testDeleteCvFromNodeThrowsRuntimeException() throws MoNotFoundException {
        setData();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(new RuntimeException(JobLogConstants.FAILURE_DUE_TO_EXCEPTION));
        executeCommon(2);

    }

    @Test
    public void testEligibleCVsForDeletion() throws JobDataNotFoundException, MoNotFoundException {

        setActivityJobPoRollBackTrue();
        setAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<>();
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test3");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test3");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test3");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "cv_test3");

        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        precheckCommon();

    }

    @Test
    public void testEligibleCVsForDeletionForOtherCondition() throws JobDataNotFoundException, MoNotFoundException {

        setActivityJobPoRollBackTrue();
        activityJobAttributes = new HashMap<String, Object>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ShmConstants.KEY, BackupActivityConstants.CURRENT_BACKUP);
        map.put(ShmConstants.VALUE, "cv_test41|ENM");
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        map.put(ActivityConstants.ROLL_BACK, "TRUE");
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "deletecv")).thenReturn(true);

        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<>();
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        final List<String> rollBackList = new ArrayList<String>();
        rollBackList.add("cv_test3");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollBackList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test3");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test3");
        cvMoAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "cv_test3");

        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        precheckCommon();

    }

    private void executeCommon(int x) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        objectUnderTest.execute(activityJobId);
        verify(jobUpdateService, times(x)).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    private void precheckCommon() {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final ActivityStepResult activityStepResult = objectUnderTest.precheck(activityJobId);
        assertNotNull(activityStepResult.getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressPercentageCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);

    }

    private void setAttributes() throws JobDataNotFoundException, MoNotFoundException {
        activityJobAttributes = new HashMap<String, Object>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ShmConstants.KEY, BackupActivityConstants.CURRENT_BACKUP);
        map.put(ShmConstants.VALUE, "cv_test41|ENM");
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        map.put(ActivityConstants.ROLL_BACK, "FALSE");
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(map);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "deletecv")).thenReturn(true);
    }

    private void setData() throws MoNotFoundException {
        setAttributesForDelete();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(ActivityConstants.CV_NAME, "cv_test3|NODE,defg,hijk");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString(), anyString(), anyString())).thenReturn(map);
    }

    private void setAttributesForDelete() throws MoNotFoundException {
        final Map<String, Object> cvMoMap = new HashMap<>();
        final Map<String, Object> cvMoAttr = new HashMap<>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<>();
        final Map<String, String> storedConfigurationVersion = new HashMap<>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, configurationVersionName);
        storedConfigurationVersionList.add(storedConfigurationVersion);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test41");
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test51");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, null);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(configurationVersionService.getCVMoAttr(neName)).thenReturn(cvMoMap);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        Mockito.doNothing().when(systemRecorder).recordCommand(any(String.class), any(CommandPhase.class), any(String.class), any(String.class), any(String.class));
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(deleteSmrsBackupServiceMock.getNetworkElement(neName)).thenReturn(neElementMock);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(neName, "cv_test3", "ERBS")).thenReturn(false);

    }

}
