/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup;

import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.JOB_LOG_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
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

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;

@RunWith(MockitoJUnitRunner.class)
public class MiniLinkOutdoorJobUtilTest {

    @InjectMocks
    private MiniLinkOutdoorJobUtil objectUnderTest;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private JobPropertyUtils jobPropertyUtilsMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private EAccessControl accessControl;

    @Mock
    private ActiveSoftwareProvider activeSoftwareProvider;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private Notification notification;

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private JobActivityInfo jobActivityInfo;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;
    String neName = "CORE82MLTN01";
    String activityName = "BACKUP";
    public static final String BACKUP_NAME_TEST = "backupName";
    private static final Double PERCENT_ZERO = 0.0;
    private static final Double PERCENT_HUNDRED = 100.0;

    @SuppressWarnings("unchecked")
    @Test
    public void testGetBackupWithAutoGenerateTrue() throws JobDataNotFoundException {
        setJobEnvironment();
        final String nodeName = "CORE82MLTN01";
        final NEJobStaticData neJobStaticData1 = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.MINI_LINK_OUTDOOR.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, "true");
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(nodeName, "CXP9010021_1||R34S108");
        backupDataMap.put(Constants.BACKUP_NAME, BACKUP_NAME_TEST);
        when(activityUtils.getJobConfigurationDetails(activityJobId)).thenReturn(new HashMap<String, Object>());
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap())).thenReturn(backupDataMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData1);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData1.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(activeSoftwareProvider.getActiveSoftwareDetails(Matchers.anyList())).thenReturn(activeSoftwareMap);
        final String backupName = objectUnderTest.getBackupName(activityJobId, jobEnvironment);
        assertTrue(backupName != null);
        assertTrue(backupName.contains("CXP9010021"));
        verify(activityUtils, times(1)).prepareJobPropertyList(anyList(), anyString(), anyString());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(neJobId, new ArrayList<Map<String, Object>>(), null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetBackupWithAutoGenerateFalse() throws JobDataNotFoundException {
        setJobEnvironment();
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, "false");
        backupDataMap.put(Constants.BACKUP_NAME, BACKUP_NAME_TEST);
        when(activityUtils.getJobConfigurationDetails(activityJobId)).thenReturn(new HashMap<String, Object>());
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap())).thenReturn(backupDataMap);
        final String backupName = objectUnderTest.getBackupName(activityJobId, jobEnvironment);
        assertTrue(backupName != null);
        verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
        verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(neJobId, new ArrayList<Map<String, Object>>(), null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetBackupWithAutoGenerateNull() throws JobDataNotFoundException {
        setJobEnvironment();
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(Constants.BACKUP_NAME, BACKUP_NAME_TEST);
        when(activityUtils.getJobConfigurationDetails(activityJobId)).thenReturn(new HashMap<String, Object>());
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap())).thenReturn(backupDataMap);
        final String backupName = objectUnderTest.getBackupName(activityJobId, jobEnvironment);
        assertTrue(backupName != null);
        verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
        verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(neJobId, new ArrayList<Map<String, Object>>(), null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetBackupWithNoAutoGenerateAndBackupName() throws JobDataNotFoundException {
        setJobEnvironment();
        final Map<String, String> backupDataMap = new HashMap<>();
        when(activityUtils.getJobConfigurationDetails(activityJobId)).thenReturn(new HashMap<String, Object>());
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap())).thenReturn(backupDataMap);
        final String backupName = objectUnderTest.getBackupName(activityJobId, jobEnvironment);
        assertTrue(backupName == null);
        verify(activityUtils, times(0)).prepareJobPropertyList(anyList(), anyString(), anyString());
        verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(neJobId, new ArrayList<Map<String, Object>>(), null, null);
    }

    @Test
    public void testPrecheckSuccess() {
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME, activityName, getClass());
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheckSuccess(PERCENT_ZERO, backupActivityProperties).getActivityResultEnum());
    }

    @Test
    public void testPrecheckFailure() {
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME, activityName, getClass());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheckFailure(PERCENT_ZERO, backupActivityProperties, "precheckFailed").getActivityResultEnum());
    }

    @Test
    public void testTimeoutSuccess() {
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME, activityName, getClass());
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        when(activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)).thenReturn(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.timeoutSuccess(PERCENT_HUNDRED, backupActivityProperties).getActivityResultEnum());
    }

    @Test
    public void testTimeoutFail() {
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME, activityName, getClass());
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)).thenReturn(activityStepResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.timeoutFail(PERCENT_ZERO, backupActivityProperties).getActivityResultEnum());
    }

    @Test
    public void testSucceedBackupRestoreActivity() {
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME, activityName, getClass());
        objectUnderTest.succeedBackupRestoreActivity(neJobStaticData, backupActivityProperties.getActivityJobId(), backupActivityProperties.getActivityName());
    }

    @Test
    public void testFailWithException() {
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME, activityName, getClass());
        objectUnderTest.failBackupRestoreActivity(neJobStaticData, backupActivityProperties.getActivityJobId(), backupActivityProperties.getActivityName(), Constants.EXCEPTION_OCCURED_FAILURE_REASON,
                Constants.BACKUP_JOB);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFinishActivity() throws JobDataNotFoundException {
        List<Map<String, Object>> jobLogList = new ArrayList<>();
        Map<String, Object> jobLog = new HashMap<>();
        jobLog.put(JOB_LOG_MESSAGE, "logMessageTest");
        jobLogList.add(jobLog);
        when(jobActivityInfo.getActivityJobId()).thenReturn(activityJobId);
        when(activityUtils.getJobEnvironment(jobActivityInfo.getActivityJobId())).thenReturn(jobEnvironment);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        doNothing().when(activityUtils).sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        objectUnderTest.finishActivity(jobActivityInfo, null, JobResult.FAILED, jobLogList, activityName);
    }

    private void setJobEnvironment() {
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
    }
}
