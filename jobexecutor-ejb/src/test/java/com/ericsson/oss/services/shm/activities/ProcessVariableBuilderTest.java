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
package com.ericsson.oss.services.shm.activities;

import static org.mockito.Mockito.when;

import java.util.Arrays;
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
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

@RunWith(MockitoJUnitRunner.class)
public class ProcessVariableBuilderTest {

    @InjectMocks
    private ProcessVariableBuilder objectUnderTest;

    @Mock
    private List<Map<String, Object>> activityJobsList;

    @Mock
    private JobActivitiesProvider jobActivitiesProvider;

    @Mock
    ActivityInfo activityInfoMock;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    NetworkElement networkElementMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private PollingActivityConfiguration pollingActivityConfigurationMock;

    @Test
    public void test() {
        when(jobActivitiesProvider.getActivityInfo(PlatformTypeEnum.CPP.name(), null, JobTypeEnum.BACKUP.name())).thenReturn(Arrays.asList(activityInfoMock));
        when(activityInfoMock.getActivityName()).thenReturn("dummyName");
        when(mapMock.get(ShmConstants.NAME)).thenReturn("activity");
        when(mapMock.get(ShmConstants.ORDER)).thenReturn(1);
        when(networkElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("ERBS", PlatformTypeEnum.CPP.name(), JobTypeEnum.BACKUP.toString(), "activity")).thenReturn(2000);
        final Map<String, Object> response = objectUnderTest.build(JobTypeEnum.BACKUP, networkElementMock, Arrays.asList(mapMock), 1234l, new HashMap<String, Map<Object, Object>>());
        Assert.assertNotNull(response);
        Assert.assertEquals(30, response.size());
    }
    
    @Test
    public void TestNodeHealthCheckJobForTimeIntervals() {
        when(jobActivitiesProvider.getActivityInfo(PlatformTypeEnum.CPP.name(), null, JobTypeEnum.BACKUP.name())).thenReturn(Arrays.asList(activityInfoMock));
        when(activityInfoMock.getActivityName()).thenReturn("dummyName");
        when(mapMock.get(ShmConstants.NAME)).thenReturn("activity");
        when(mapMock.get(ShmConstants.ORDER)).thenReturn(1);
        when(networkElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(activityTimeoutsServiceMock.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME)).thenReturn("2");
        when(activityTimeoutsServiceMock.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT)).thenReturn("10");
        final Map<String, Object> response = objectUnderTest.build(JobTypeEnum.NODE_HEALTH_CHECK, networkElementMock, Arrays.asList(mapMock), 1234l, new HashMap<String, Map<Object, Object>>());
        Assert.assertNotNull(response);
        Assert.assertEquals(28, response.size());
    }
}
