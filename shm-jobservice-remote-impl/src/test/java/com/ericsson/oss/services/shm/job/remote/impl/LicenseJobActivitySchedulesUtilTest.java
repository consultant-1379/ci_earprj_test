/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.service.common.LicenseJobActivitySchedulesUtil;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

@RunWith(MockitoJUnitRunner.class)
public class LicenseJobActivitySchedulesUtilTest {

    @InjectMocks
    private LicenseJobActivitySchedulesUtil licenseJobActivitySchedulesUtil;

    @Mock
    JobActivitiesProviderImpl jobActivitiesProviderImplMock;

    @Test
    public void testActivitySchedulePreparation() {

        final String neType = "5GRadioNode";
        final String jobType = JobType.LICENSE.toString();
        final String activityName = "install";

        final ActivityInfo activityInfoResult = new ActivityInfo(activityName, null, 0);
        final List<ActivityInfo> activityList = new ArrayList<>();
        activityList.add(activityInfoResult);

        Mockito.when(jobActivitiesProviderImplMock.getActivityInfo(PlatformTypeEnum.ECIM.toString(), neType, jobType)).thenReturn(activityList);
        final List<Map<String, Object>> activityInfo = licenseJobActivitySchedulesUtil.prepareActivitySchedules(PlatformTypeEnum.ECIM, neType, JobType.LICENSE.toString(), activityName);

        Assert.assertEquals(1, activityInfo.size());
        Assert.assertEquals(ExecMode.IMMEDIATE.name(), activityInfo.get(0).get(ShmConstants.EXECUTION_MODE));

    }

    @Test
    public void testActivitySchedulePreparationWhenActivityNameIsNotAvailable() {

        final String neType = "5GRadioNode";
        final String jobType = JobType.LICENSE.toString();
        final String activityName = "install";

        final ActivityInfo activityInfoResult = new ActivityInfo();
        final List<ActivityInfo> activityList = new ArrayList<>();
        activityList.add(activityInfoResult);

        Mockito.when(jobActivitiesProviderImplMock.getActivityInfo(PlatformTypeEnum.ECIM.toString(), neType, jobType)).thenReturn(activityList);
        final List<Map<String, Object>> activityInfo = licenseJobActivitySchedulesUtil.prepareActivitySchedules(PlatformTypeEnum.ECIM, neType, JobType.LICENSE.toString(), activityName);

        Assert.assertEquals(0, activityInfo.size());
    }
}
