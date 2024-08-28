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
package com.ericsson.oss.services.shm.es.impl.cpp.backuphousekeeping;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
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

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.backuphousekeeping.NodeBackupHousekeepingConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class CleanupCvServiceTest {

    @InjectMocks
    private CleanupCvService objectUnderTest;

    @Mock
    @Inject
    private ConfigurationVersionService configurationVersionService;

    @Mock
    @Inject
    private ActivityUtils activityUtils;

    @Mock
    @Inject
    private JobLogUtil jobLogUtil;

    @Mock
    @Inject
    private JobUpdateService jobUpdateService;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    @Inject
    private ConfigurationVersionUtils cvUtil;

    @Mock
    @Inject
    private CommonCvOperations commonCvOperationsmock;

    @Mock
    @Inject
    private SystemRecorder systemRecorder;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    long activityJobId = 123L;

    String cvMoFdn = "Some Cv Mo Fdn";
    String neName = "Some Ne Name";

    private static final String CLEANCV_ACTIVITY_NAME = "cleancv";

    Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSuccess() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        setfirstInrollBackList.add("cv_test1");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "1");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSkipExecution_WhenRollbacklistandBackupsOnNodeFailed() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "45");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "55");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSuccess_whenCVSOnNodeGreaterThanCVSToKeepOnNode() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "2");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "25");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSuccess_whenCVSInRollBackListGreaterThanCVSToKeepInRollBackList() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        setfirstInrollBackList.add("cv_test1");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "25");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSkipped_whenCVSOnNodeActivityIsSelected() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        setfirstInrollBackList.add("cv_test1");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "25");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "-1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSkipped_whenCVSInRollBackListActivityIsSelected() throws JobDataNotFoundException, MoNotFoundException {
        mockActivityJobAttributes();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        setfirstInrollBackList.add("cv_test1");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "-1");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "25");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testPrecheck_Failure() throws JobDataNotFoundException, MoNotFoundException {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        mockActivityJobAttributes();
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, CLEANCV_ACTIVITY_NAME)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck_FailureWhenJobDataNotFound() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY)).thenThrow(Exception.class);
        when(neJobStaticData.getMainJobId()).thenReturn(123L);
        when(activityUtils.getJobExecutionUser(123L)).thenReturn("TEST_USER");
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteClearAllBackups() {
        mockActivityJobAttributes();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "FALSE");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        when(activityAndNEJobProgressCalculator.calculateActivityProgressPercentage((JobEnvironment) Matchers.anyObject(), Matchers.anyInt(), anyDouble()))
                .thenReturn(ACTIVITY_END_PROGRESS_PERCENTAGE);
        objectUnderTest.execute(activityJobId);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityAndNEJobProgressCalculator, times(9)).updateActivityJobProgressPercentage(anyLong(), anyList(), anyList(), anyDouble());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteClearEligibleBackups() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        when(activityAndNEJobProgressCalculator.calculateActivityProgressPercentage((JobEnvironment) Matchers.anyObject(), Matchers.anyInt(), anyDouble()))
                .thenReturn(ACTIVITY_END_PROGRESS_PERCENTAGE);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityAndNEJobProgressCalculator, times(5)).updateActivityJobProgressPercentage(anyLong(), anyList(), anyList(), anyDouble());
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenImproperDateFormatOnNode() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getImproperStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteRemoveFromNode() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "-1");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteRemoveFromRbl() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "-1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteRemoveFromNodeFailed() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "-1");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(Exception.class);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteRemoveFromRblFailed() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "-1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(Exception.class);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenExceptionWhileDeletingFromRbl() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        final Exception exception = new RuntimeException();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2).thenThrow(exception).thenReturn(2).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String logMessage = "Unable to remove CV : cv_test_rollback1 from Rollback List.Failure reason: \"null\"";
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test_rollback2 removed from Rollback List."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test has been deleted successfully from Node."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.INFO.toString()));
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenExceptionWhileDeletingFromNode() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        final Exception exception = new RuntimeException();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2).thenReturn(2).thenThrow(exception).thenReturn(2).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test_rollback2 removed from Rollback List."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test_rollback1 removed from Rollback List."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList),
                eq("Number of CV's in RollbackList : 3 , Number of CV's to keep in RollbackList : 1 , Total No of CV's removed from Rollback List : 2"), Matchers.any(Date.class),
                eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("Unable to delete CV : cv_test. Failure reason: \"null\""), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.ERROR.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test1 has been deleted successfully from Node."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.INFO.toString()));
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenCancelTriggered() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "3");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        when(activityUtils.cancelTriggered(activityJobId)).thenReturn(false).thenReturn(true);
        objectUnderTest.execute(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test_rollback2 removed from Rollback List."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.ACTION_CANCELLED, ActivityConstants.CLEAN_CV)), Matchers.any(Date.class),
                eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenNoBackupsToDelete() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "10");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "5");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("No CV was removed from the Rollback list. Number of CV's in RollbackList : 3 , Number of CV's to keep in RollbackList : 5"),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("No CV was deleted from Node. Number of CV's on Node : 6, Number of CV's to keep on Node : 10"), Matchers.any(Date.class),
                eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenCvInRollbackList() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test");
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test_startable");
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test_loaded");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        when(cvUtil.getCvNames(storedConfigurationVersionList)).thenReturn(cvNameList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "FALSE");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        final Exception exception = new RuntimeException();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(exception).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        final String logMessage = "Unable to remove CV : cv_test from Rollback List.Failure reason: \"null\"";
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("Number of CV's in RollbackList : 1 , Number of CV's to keep in RollbackList : 0 , No CV was removed from the Rollback list"),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test cannot be deleted as it is in RollbackList."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.ERROR.toString()));
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenCvIsSetStartable() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test_rollback");
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test");
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test_loaded");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        when(cvUtil.getCvNames(storedConfigurationVersionList)).thenReturn(cvNameList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "FALSE");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        final Exception exception = new RuntimeException();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(exception).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        final String logMessage = "Unable to remove CV : cv_test_rollback from Rollback List.Failure reason: \"null\"";
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("Number of CV's in RollbackList : 1 , Number of CV's to keep in RollbackList : 0 , No CV was removed from the Rollback list"),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test cannot be deleted as it is startable CV"), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.ERROR.toString()));

        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWhenCvIsCurrentLoaded() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test_rollback");
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test_startable");
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        when(cvUtil.getCvNames(storedConfigurationVersionList)).thenReturn(cvNameList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "FALSE");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        final Exception exception = new RuntimeException();
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenThrow(exception).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        final String logMessage = "Unable to remove CV : cv_test_rollback from Rollback List.Failure reason: \"null\"";
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("Number of CV's in RollbackList : 1 , Number of CV's to keep in RollbackList : 0 , No CV was removed from the Rollback list"),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq("CV : cv_test cannot be deleted as it is the current loaded CV."), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.ERROR.toString()));
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutFailed() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "2");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        cvNameList.add("cv_test2");
        cvNameList.add("cv_test3");
        cvNameList.add("cv_test4");
        cvNameList.add("cv_test5");
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test_rollback");
        setfirstInrollBackList.add("cv_test_rollback1");
        setfirstInrollBackList.add("cv_test_rollback2");
        final String logMessage = "Cleanup CV has failed. Number of CV's in RollbackList : 3 , Number of CV's to keep in RollbackList : 1. Number of CV's on Node : 6 , Number of CV's to keep on Node : 2";
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutSuccess() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "6");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "3");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        cvNameList.add("cv_test2");
        cvNameList.add("cv_test3");
        cvNameList.add("cv_test4");
        cvNameList.add("cv_test5");
        final String logMessage = "Deletion of backups on the Node and from Rollback list is successful";
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void test_cancel() {
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        objectUnderTest.cancel(activityJobId);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeoutFailed() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "2");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        cvNameList.add("cv_test2");
        cvNameList.add("cv_test3");
        cvNameList.add("cv_test4");
        cvNameList.add("cv_test5");
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test_rollback");
        setfirstInrollBackList.add("cv_test_rollback1");
        setfirstInrollBackList.add("cv_test_rollback2");
        final String logMessage = "Cleanup CV has failed. Number of CV's in RollbackList : 3 , Number of CV's to keep in RollbackList : 1. Number of CV's on Node : 6 , Number of CV's to keep on Node : 2";
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.cancelTimeout(activityJobId, false).getActivityResultEnum());
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeoutSuccess() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttr(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "6");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "3");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        cvNameList.add("cv_test2");
        cvNameList.add("cv_test3");
        cvNameList.add("cv_test4");
        cvNameList.add("cv_test5");
        final String logMessage = "Deletion of backups on the Node and from Rollback list is successful";
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.cancelTimeout(activityJobId, false).getActivityResultEnum());
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(logMessage), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));
    }

    private void getCvMoAttr(final List<Map<String, String>> storedConfigurationVersionList) {
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final List<String> setfirstInrollBackList = new ArrayList<String>();
        setfirstInrollBackList.add("cv_test_rollback");
        setfirstInrollBackList.add("cv_test_rollback1");
        setfirstInrollBackList.add("cv_test_rollback2");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test_startable");
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test_loaded");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        cvNameList.add("cv_test2");
        cvNameList.add("cv_test3");
        cvNameList.add("cv_test4");
        cvNameList.add("cv_test5");
        when(cvUtil.getCvNames(storedConfigurationVersionList)).thenReturn(cvNameList);
    }

    private void getCvMoAttrForRbl(final List<Map<String, String>> storedConfigurationVersionList) {
        final Map<String, Object> cvMoAttr = new HashMap<String, Object>();
        final Map<String, Object> cvMoMap = new HashMap<String, Object>();
        final List<String> setfirstInrollBackList = new ArrayList<String>();

        setfirstInrollBackList.add("cv_test_rollback1");
        setfirstInrollBackList.add("cv_test_rollback2");
        setfirstInrollBackList.add("cv_test2");
        cvMoAttr.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        cvMoAttr.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "cv_test_startable");
        cvMoAttr.put(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION, "cv_test_loaded");
        cvMoAttr.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, setfirstInrollBackList);
        cvMoMap.put(ShmConstants.FDN, cvMoFdn);
        cvMoMap.put(ShmConstants.MO_ATTRIBUTES, cvMoAttr);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName())).thenReturn(cvMoMap);
        final List<String> cvNameList = new ArrayList<String>();
        cvNameList.add("cv_test");
        cvNameList.add("cv_test1");
        cvNameList.add("cv_test2");
        cvNameList.add("cv_test3");
        cvNameList.add("cv_test4");
        cvNameList.add("cv_test5");
        when(cvUtil.getCvNames(storedConfigurationVersionList)).thenReturn(cvNameList);
    }

    @Test
    public void testExecuteRemoveFromNodeAndNode() {
        mockActivityJobAttributes();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        getCvMoAttrForRbl(storedConfigurationVersionList);
        getNetworkElement();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS, "FALSE");
        map.put(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS, "TRUE");
        map.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "1");
        map.put(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST, "1");
        map.put(NodeBackupHousekeepingConstants.DELETE_ELIGIBLE_BACKUPS_FROM_ROLLBACK_LIST, "TRUE");
        when(jobPropertyUtils.getPropertyValue(anyList(), anyMap(), anyString())).thenReturn(map);
        Mockito.doNothing().when(activityUtils).recordEvent(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorder).recordCommand(SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, neName, cvMoFdn, "additionalInfoForCommand");
        when(commonCvOperationsmock.executeActionOnMo(any(String.class), any(String.class), any(Map.class))).thenReturn(2);
        objectUnderTest.execute(activityJobId);
        verify(activityUtils, times(1)).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        verify(activityUtils, times(1)).sendNotificationToWFS(jobEnvironment, activityJobId, ActivityConstants.CLEAN_CV, null);
        Mockito.verify(activityUtils).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    private List<Map<String, String>> getStoredConfigurationList() {
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion3 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion4 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion5 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersion3.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test3|NODE2");
        storedConfigurationVersion3.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersion4.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test4|NODE2");
        storedConfigurationVersion4.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersion5.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test5|NODE2");
        storedConfigurationVersion5.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        storedConfigurationVersionList.add(storedConfigurationVersion3);
        storedConfigurationVersionList.add(storedConfigurationVersion4);
        storedConfigurationVersionList.add(storedConfigurationVersion5);
        return storedConfigurationVersionList;
    }

    private List<Map<String, String>> getImproperStoredConfigurationList() {
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "7");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "7");
        final List<Map<String, String>> storedConfigurationVersionList = getStoredConfigurationList();
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        return storedConfigurationVersionList;
    }

    @SuppressWarnings("unchecked")
    private void getNetworkElement() {
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
    }

    private void mockActivityJobAttributes() {
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
    }
}
