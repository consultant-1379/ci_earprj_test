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
package com.ericsson.oss.services.shm.job.service.cpp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;

@RunWith(MockitoJUnitRunner.class)
public class DeleteBackupJobDetailsProviderTest {

    @Mock
    JobPropertyUtils jobPropertyUtils;

    @Mock
    private ActivityParamMapper activityParamMapper;

    @InjectMocks
    DeleteBackupJobDetailsProvider deleteBackupJobDetailsProvider;

    @Test
    public void testWhenMulipleBackupsDeletedOnNode() {
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final String DELETED_BACKUP_INFO = "backuptest1.zip|ENM,backuptest1|NODE,backuptest2|NODE";
        Map<String, String> deleteBackupJobDetailsMap = setup(DELETED_BACKUP_INFO);
        final PlatformTypeEnum platformType = PlatformTypeEnum.CPP;
        final String neName = "LTE2ERBS0001";
        final String neType = "ERBS";
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(deleteBackupJobDetailsMap);
        final List<Map<String, String>> deleteBackupValueList = deleteBackupJobDetailsProvider.getJobConfigurationDetails(jobConfiguration, platformType, neType, neName);
        assertEquals(3, deleteBackupValueList.size());
        for (int i = 0; i < deleteBackupValueList.size(); i++) {
            Map<String, String> deleteBackupValueMap = deleteBackupValueList.get(i);
            assertEquals(2, deleteBackupValueMap.size());
            switch (i) {
            case 0:
                assertEquals(true, deleteBackupValueMap.containsKey("fileName"));
                assertEquals(true, deleteBackupValueMap.containsValue("backuptest1.zip"));
                assertEquals(true, deleteBackupValueMap.containsKey("location"));
                assertEquals(true, deleteBackupValueMap.containsValue("ENM"));
                assertEquals(false, deleteBackupValueMap.containsKey("backupName"));
                break;
            case 1:
                assertEquals(true, deleteBackupValueMap.containsKey("backupName"));
                assertEquals(true, deleteBackupValueMap.containsValue("backuptest1"));
                assertEquals(true, deleteBackupValueMap.containsKey("location"));
                assertEquals(true, deleteBackupValueMap.containsValue("NODE"));
                assertEquals(false, deleteBackupValueMap.containsKey("fileName"));
                break;
            case 2:
                assertEquals(true, deleteBackupValueMap.containsKey("backupName"));
                assertEquals(true, deleteBackupValueMap.containsValue("backuptest2"));
                assertEquals(true, deleteBackupValueMap.containsKey("location"));
                assertEquals(true, deleteBackupValueMap.containsValue("NODE"));
                assertEquals(false, deleteBackupValueMap.containsKey("fileName"));
                break;
            }
        }

    }

    @Test
    public void testWhenSingleBackupDeletedOnNode() {
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final String DELETED_BACKUP_INFO = "singleBackup.zip|ENM";
        Map<String, String> deleteBackupJobDetailsMap = setup(DELETED_BACKUP_INFO);
        final PlatformTypeEnum platformType = PlatformTypeEnum.CPP;
        final String neName = "LTE2ERBS0001";
        final String neType = "ERBS";
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(deleteBackupJobDetailsMap);
        final List<Map<String, String>> deleteBackupValueList = deleteBackupJobDetailsProvider.getJobConfigurationDetails(jobConfiguration, platformType, neType, neName);
        for (Map<String, String> deleteBackupValueMap : deleteBackupValueList) {
            assertNotNull(deleteBackupValueMap);
            assertEquals(2, deleteBackupValueMap.size());
            assertEquals(true, deleteBackupValueMap.containsValue("singleBackup.zip"));
            assertEquals(true, deleteBackupValueMap.containsValue("ENM"));
        }
    }

    private Map<String, String> setup(final String deleteBackupDetails) {
        Map<String, String> deleteBackupJobDetailsMap = new HashMap<String, String>();
        deleteBackupJobDetailsMap.put(JobPropertyConstants.CV_NAME, deleteBackupDetails);
        return deleteBackupJobDetailsMap;
    }
}
