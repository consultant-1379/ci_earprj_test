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
package com.ericsson.oss.services.shm.job.service.cpp.backup;

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
import com.ericsson.oss.services.shm.job.remote.api.ShmBackupJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;

@RunWith(MockitoJUnitRunner.class)
public class CppBackupNeTypePropertiesProviderTest {

    @Mock
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    @InjectMocks
    CppBackupNeTypePropertiesProvider cppBackupNeTypePropertiesProvider;

    @Mock
    NeTypePropertiesHelper neTypePropertiesHelper;

    @Mock
    Map<String, Object> mapMock;

    private static final String CV_NAME = "CV_NAME";
    private static final String CV_TYPE = "CV_TYPE";
    private static List<Map<String, Object>> neTypeProperties = new ArrayList<Map<String, Object>>();

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareNeTypePropertiesforBackup() {
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(CV_NAME);
        activityProperties.add(CV_TYPE);
        shmBackupJobData.setBackupName("backup01");
        shmBackupJobData.setBackupType("TEST");
        setNeTypeProperties();
        when(neTypePropertiesHelper.createPropertyMap(Matchers.anyString(), Matchers.anyString())).thenReturn(mapMock);
        when(neTypePropertiesHelper.getNeTypeProperties(Matchers.anyList(), Matchers.anyList())).thenReturn(neTypeProperties);
        List<Map<String, Object>> neTypePropertiesList = cppBackupNeTypePropertiesProvider.getNeTypeProperties(activityProperties, shmBackupJobData);
        assertTrue(neTypePropertiesList != null);
        assertTrue(neTypePropertiesList.size() > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareNeTypePropertiesforBackupWithNullBackupType() {
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(CV_NAME);
        activityProperties.add(CV_TYPE);
        shmBackupJobData.setBackupName("backup01");
        setNeTypeProperties();
        when(neTypePropertiesHelper.createPropertyMap(Matchers.anyString(), Matchers.anyString())).thenReturn(mapMock);
        when(neTypePropertiesHelper.getNeTypeProperties(Matchers.anyList(), Matchers.anyList())).thenReturn(neTypeProperties);
        List<Map<String, Object>> neTypePropertiesList = cppBackupNeTypePropertiesProvider.getNeTypeProperties(activityProperties, shmBackupJobData);
        assertTrue(neTypePropertiesList != null);
        assertTrue(neTypePropertiesList.size() > 0);
    }

    private void setNeTypeProperties() {
        Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put(CV_NAME, "backup01");
        propertiesMap.put(CV_TYPE, "TEST");
        neTypeProperties.add(propertiesMap);
    }

}
