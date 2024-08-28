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
package com.ericsson.oss.services.shm.job.service.cpp.noderestart;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.remote.api.ShmNodeRestartJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;

@RunWith(MockitoJUnitRunner.class)
public class CppNodeRestartNeTypePropertiesProviderTest {

    @Mock
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    @InjectMocks
    CppNodeRestartNeTypePropertiesProvider cppNodeRestartNeTypePropertiesProvider;

    @Mock
    NeTypePropertiesHelper neTypePropertiesHelper;

    @Mock
    Map<String, Object> mapMock;

    private static final String RESTART_RANK = "restartRank";
    private static final String RESTART_REASON = "restartReason";
    private static final String RESTART_INFO = "restartInfo";
    private static List<Map<String, Object>> neTypeProperties = new ArrayList<Map<String, Object>>();

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareNeTypePropertiesforBackup() {
        ShmNodeRestartJobData shmBackupJobData = new ShmNodeRestartJobData();
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(RESTART_RANK);
        activityProperties.add(RESTART_REASON);
        shmBackupJobData.setRestartRank("1");
        shmBackupJobData.setRestartReason("TEST");
        setNeTypeProperties();
        when(neTypePropertiesHelper.createPropertyMap(Matchers.anyString(), Matchers.anyString())).thenReturn(mapMock);
        when(neTypePropertiesHelper.getNeTypeProperties(Matchers.anyList(), Matchers.anyList())).thenReturn(neTypeProperties);
        List<Map<String, Object>> neTypePropertiesList = cppNodeRestartNeTypePropertiesProvider.getNeTypeProperties(activityProperties, shmBackupJobData);
        assertTrue(neTypePropertiesList != null);
        assertTrue(neTypePropertiesList.size() > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareNeTypePropertiesforBackupWithNullRestartInfo() {
        ShmNodeRestartJobData shmBackupJobData = new ShmNodeRestartJobData();
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(RESTART_RANK);
        activityProperties.add(RESTART_REASON);
        shmBackupJobData.setRestartRank("1");
        shmBackupJobData.setRestartReason("TEST");
        setNeTypeProperties();
        when(neTypePropertiesHelper.createPropertyMap(Matchers.anyString(), Matchers.anyString())).thenReturn(mapMock);
        when(neTypePropertiesHelper.getNeTypeProperties(Matchers.anyList(), Matchers.anyList())).thenReturn(neTypeProperties);
        List<Map<String, Object>> neTypePropertiesList = cppNodeRestartNeTypePropertiesProvider.getNeTypeProperties(activityProperties, shmBackupJobData);
        assertTrue(neTypePropertiesList != null);
        assertTrue(neTypePropertiesList.size() > 0);
    }

    private void setNeTypeProperties() {
        Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put(RESTART_RANK, "backup01");
        propertiesMap.put(RESTART_REASON, "TEST");
        propertiesMap.put(RESTART_INFO, "Restart");
        neTypeProperties.add(propertiesMap);
    }

}
