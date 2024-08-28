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
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.service.common.NodeRestartActivitySchedulesUtil;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartActivitySchedulesUtilTest {

    @Mock
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    @InjectMocks
    NodeRestartActivitySchedulesUtil activitySchedulesUtil;

    @Test
    public void testPrepareActivitySchedules() {
        ActivityInfo activityInfo = new ActivityInfo("activityName", "Immediate", 1);
        List<ActivityInfo> listOfActivityInfo = new ArrayList<ActivityInfo>();
        listOfActivityInfo.add(activityInfo);
        when(jobActivitiesProviderImpl.getActivityInfo(PlatformTypeEnum.CPP.getName(), "LTE01", JobType.NODERESTART.toString())).thenReturn(listOfActivityInfo);
        final List<Map<String, Object>> invidualActivitySchedules = activitySchedulesUtil.prepareActivitySchedules(PlatformTypeEnum.CPP, "LTE01", JobType.NODERESTART.toString());
        Assert.assertNotNull(invidualActivitySchedules);
    }
}
