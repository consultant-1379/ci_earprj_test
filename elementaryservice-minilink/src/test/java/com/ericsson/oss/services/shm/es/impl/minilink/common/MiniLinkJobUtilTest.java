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
package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;

@RunWith(MockitoJUnitRunner.class)
public class MiniLinkJobUtilTest {

    @InjectMocks
    private MiniLinkJobUtil objectUnderTest;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private JobPropertyUtils jobPropertyUtilsMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

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

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;
    String neName = "CORE82MLTN01";

    @SuppressWarnings("unchecked")
    @Test
    public void testGetBackupWithAutoGenerateTrue() throws JobDataNotFoundException {
        setJobEnvironment();
        final String nodeName = "CORE82MLTN01";
        final NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.MINI_LINK_INDOOR.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, "true");
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(nodeName, "CXP9010021_1||R34S108");
        backupDataMap.put(MiniLinkConstants.BACKUP_NAME, "backupName");
        when(activityUtils.getJobConfigurationDetails(activityJobId)).thenReturn(new HashMap<String, Object>());
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap())).thenReturn(backupDataMap);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
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
        backupDataMap.put(MiniLinkConstants.BACKUP_NAME, "backupName");
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
        backupDataMap.put(MiniLinkConstants.BACKUP_NAME, "backupName");
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

    private void setJobEnvironment() {
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
    }
}
