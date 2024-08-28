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
package com.ericsson.oss.services.shm.job.remote.impl;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.job.remote.api.ShmBackupJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmNodeRestartJobData;

@RunWith(MockitoJUnitRunner.class)
public class NeTypePropertiesUtilTest {

    @Mock
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    @InjectMocks
    NeTypePropertiesUtil neTypePropertiesUtil;

    @Mock
    NeTypePropertiesProviderFactory neTypePropertiesProviderFactory;

    @Mock
    NeTypePropertiesProvider neTypePropertiesProvider;

    @Mock
    List<Map<String, Object>> listMapMock;

    private static final String CV_NAME = "CV_NAME";
    private static final String CV_TYPE = "CV_TYPE";
    private static final String BACKUP_DOMAIN_TYPE = "BACKUP_DOMAIN_TYPE";
    private static List<Map<String, Object>> neTypeProperties = new ArrayList<Map<String, Object>>();

    @Test
    public void testPrepareNeTypePropertiesforBackup() {
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(CV_NAME);
        activityProperties.add(BACKUP_DOMAIN_TYPE);
        activityProperties.add(CV_TYPE);
        shmBackupJobData.setBackupType("STANDARD");
        shmBackupJobData.setDomainBackupType("Domain/Type");
        setNeTypeProperties();
        when(jobActivitiesProviderImpl.getActivityProperties(PlatformTypeEnum.CPP.name(), "ERBS", "BACKUP")).thenReturn(activityProperties);
        when(neTypePropertiesProviderFactory.getNeTypePropertiesProvider(PlatformTypeEnum.CPP, JobType.fromValue("BACKUP"))).thenReturn(neTypePropertiesProvider);
        when(neTypePropertiesProvider.getNeTypeProperties(activityProperties, shmBackupJobData)).thenReturn(neTypeProperties);
        final List<Map<String, Object>> neTypeProperties = neTypePropertiesUtil.prepareNeTypeProperties(PlatformTypeEnum.CPP, "ERBS", "BACKUP", shmBackupJobData);
        Assert.assertNotNull(neTypeProperties);
        Assert.assertTrue(neTypeProperties.size() > 0);
    }

    @Test
    public void testPrepareNeTypePropertiesWithDefaultDomainAndType() {
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(CV_NAME);
        activityProperties.add(BACKUP_DOMAIN_TYPE);
        activityProperties.add(CV_TYPE);
        setNeTypeProperties();
        when(jobActivitiesProviderImpl.getActivityProperties(PlatformTypeEnum.CPP.name(), "ERBS", "BACKUP")).thenReturn(activityProperties);
        when(neTypePropertiesProviderFactory.getNeTypePropertiesProvider(PlatformTypeEnum.CPP, JobType.fromValue("BACKUP"))).thenReturn(neTypePropertiesProvider);
        when(neTypePropertiesProvider.getNeTypeProperties(activityProperties, shmBackupJobData)).thenReturn(neTypeProperties);
        final List<Map<String, Object>> neTypeProperties = neTypePropertiesUtil.prepareNeTypeProperties(PlatformTypeEnum.CPP, "ERBS", "BACKUP", shmBackupJobData);
        Assert.assertNotNull(neTypeProperties);
        Assert.assertTrue(neTypeProperties.size() > 0);
    }

    private void setNeTypeProperties() {
        Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put(CV_NAME, "backup01");
        propertiesMap.put(CV_TYPE, "STANDARD");
        neTypeProperties.add(propertiesMap);
    }

    @Test
    public void testPrepareNeTypePropertiesforNodeRestart() {
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(CV_NAME);
        activityProperties.add(BACKUP_DOMAIN_TYPE);
        activityProperties.add(CV_TYPE);
        ShmNodeRestartJobData shmNodeRestartJobData = new ShmNodeRestartJobData();
        shmNodeRestartJobData.setRestartInfo("someInfo");
        shmNodeRestartJobData.setRestartRank("RESTART_WARM");
        shmNodeRestartJobData.setRestartReason("PLANNED_RECONFIGURATION");
        when(jobActivitiesProviderImpl.getActivityProperties(PlatformTypeEnum.CPP.name(), "ERBS", "NODERESTART")).thenReturn(activityProperties);
        when(neTypePropertiesProviderFactory.getNeTypePropertiesProvider(PlatformTypeEnum.CPP, JobType.fromValue("BACKUP"))).thenReturn(neTypePropertiesProvider);
        when(neTypePropertiesProvider.getNeTypeProperties(activityProperties, shmNodeRestartJobData)).thenReturn(neTypeProperties);
        final List<Map<String, Object>> neTypeProperties = neTypePropertiesUtil.prepareNeTypeProperties(PlatformTypeEnum.CPP, "LTE01", JobType.NODERESTART.toString(), shmNodeRestartJobData);
        Assert.assertNotNull(neTypeProperties);
    }

    @Test
    public void testPrepareNeTypePropertiesForDefaultRankAndInfo() {
        List<String> activityProperties = new ArrayList<String>();
        activityProperties.add(CV_NAME);
        activityProperties.add(BACKUP_DOMAIN_TYPE);
        activityProperties.add(CV_TYPE);
        ShmNodeRestartJobData shmNodeRestartJobData = new ShmNodeRestartJobData();
        shmNodeRestartJobData.setRestartReason("PLANNED_RECONFIGURATION");
        final List<Map<String, Object>> neTypeProperties = neTypePropertiesUtil.prepareNeTypeProperties(PlatformTypeEnum.CPP, "LTE01", JobType.NODERESTART.toString(), shmNodeRestartJobData);
        Assert.assertNotNull(neTypeProperties);
    }

}
