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
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.remote.api.BackupActivityEnum;
import com.ericsson.oss.services.shm.job.service.common.BackupActivitySchedulesUtil;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

@RunWith(MockitoJUnitRunner.class)
public class BackupActivitySchedulesUtilTest {

    @Mock
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    @InjectMocks
    BackupActivitySchedulesUtil backupActivitySchedulesUtil;

    @Test
    public void testPrepareActivitySchedules() {
        List<BackupActivityEnum> backupActivities = new ArrayList<BackupActivityEnum>();
        backupActivities.add(BackupActivityEnum.CREATE_CV);
        List<ActivityInfo> activityInfos = new ArrayList<ActivityInfo>();
        ActivityInfo activityInfo = new ActivityInfo("createcv", "IMMEDIATE", 2);
        activityInfos.add(activityInfo);
        when(jobActivitiesProviderImpl.getActivityInfo(PlatformTypeEnum.CPP.getName(), "ERBS", "BACKUP")).thenReturn(activityInfos);
        backupActivitySchedulesUtil.prepareActivitySchedules(PlatformTypeEnum.CPP, "ERBS", "BACKUP", backupActivities);
    }
}
