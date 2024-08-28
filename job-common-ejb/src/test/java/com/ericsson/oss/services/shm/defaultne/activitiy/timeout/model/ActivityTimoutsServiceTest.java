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
package com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsFactory;
import com.ericsson.oss.services.shm.activity.timeout.models.ActivityTimeoutsProvider;
import com.ericsson.oss.services.shm.activity.timeout.models.NodeHealthCheckJobActivityTimeouts;
import com.ericsson.oss.services.shm.activity.timeout.models.PrecheckConfigurationProvider;
import com.ericsson.oss.services.shm.activity.timeout.models.ShmJobDefaultActivityTimeouts;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@RunWith(MockitoJUnitRunner.class)
public class ActivityTimoutsServiceTest {

    @Mock
    private ActivityTimeoutsFactory activityTimeoutsFactory;

    @Mock
    private ActivityTimeoutsProvider activityTimeoutsProvider;

    @Mock
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @InjectMocks
    private ActivityTimeoutsService mockActivityTimeoutsService;
    
    @Mock
    private NodeHealthCheckJobActivityTimeouts nodeHealthCheckJobActivityTimeouts;
    
    @Mock
    private PrecheckConfigurationProvider repeatPrecheckConfigurationProvider;

    @Test
    public void testGetActivityTimeout() {
        final String neType = "SGSN-MME";
        final String activityName = "createbackup";
        when(activityTimeoutsFactory.getActivityTimeoutsProvider(JobType.BACKUP)).thenReturn(activityTimeoutsProvider);
        when(activityTimeoutsProvider.getActivityTimeout(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName)).thenReturn("30min");
        final String timeout = mockActivityTimeoutsService.getActivityTimeout(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName);
        assertTrue(timeout.length() != 0);
    }

    @Test
    public void testGetActivityTimeoutWithDefaultTimeout() {
        final String neType = "SGSN-MME";
        final String activityName = "createbackup";
        when(shmJobDefaultActivityTimeouts.getActivityTimeout(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName)).thenReturn("30min");
        final String timeout = mockActivityTimeoutsService.getActivityTimeout(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName);
        assertTrue(timeout.length() != 0);
    }

    @Test
    public void testGetActivityTimeoutAsInteger() {
        final String neType = "SGSN-MME";
        final String activityName = "createbackup";
        final String platform = "ECIM";
        final String jobType = "BACKUP";
        when(activityTimeoutsFactory.getActivityTimeoutsProvider(JobType.getJobType(jobType))).thenReturn(activityTimeoutsProvider);
        when(activityTimeoutsProvider.getActivityTimeoutAsInteger(neType, platform, jobType, activityName)).thenReturn(30);
        final int timeout = mockActivityTimeoutsService.getActivityTimeoutAsInteger(neType, platform, jobType, activityName);
        assertTrue(timeout != 0);
    }

    @Test
    public void testGetActivityTimeoutAsIntegerWithDefaultTimeout() {
        final String neType = "SGSN-MME";
        final String activityName = "createbackup";
        final String platform = "ECIM";
        final String jobType = "BACKUP";
        when(shmJobDefaultActivityTimeouts.getActivityTimeoutAsInteger(neType, platform, jobType, activityName)).thenReturn(30);
        final int timeout = mockActivityTimeoutsService.getActivityTimeoutAsInteger(neType, platform, jobType, activityName);
        assertTrue(timeout != 0);
    }

    @Test
    public void testGetTimeoutForHandleTimeout() {
        mockActivityTimeoutsService.getTimeoutForHandleTimeout();
        verify(shmJobDefaultActivityTimeouts).getTimeoutForHandleTimeout();
    }

    @Test
    public void testGetTimeoutForHandleTimeoutAsInteger() {
        mockActivityTimeoutsService.getTimeoutForHandleTimeoutAsInteger();
        verify(shmJobDefaultActivityTimeouts).getTimeoutForHandleTimeoutAsInteger();
    }
    
    @Test
    public void testGetRepeatPrecheckWaitInterval() {
        final String neType = "RadioNode";
        final String activityName = "uploadbackup";
        final String repeatPrecheckWaitInterval = "10";

        when(activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.BACKUP)).thenReturn(repeatPrecheckConfigurationProvider);
        when(repeatPrecheckConfigurationProvider.getRepeatPrecheckWaitInterval(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName)).thenReturn(repeatPrecheckWaitInterval);

        assertEquals(repeatPrecheckWaitInterval, mockActivityTimeoutsService.getRepeatPrecheckWaitInterval(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName));
    }

    @Test
    public void testGetRepeatPrecheckWaitIntervalWithDefaultWaitInterval() {
        final String neType = "RadioNode";
        final String activityName = "uploadbackup";
        final String repeatPrecheckWaitInterval = "8";

        when(activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.BACKUP)).thenReturn(null);
        when(shmJobDefaultActivityTimeouts.getRepeatPrecheckWaitInterval()).thenReturn(repeatPrecheckWaitInterval);

        assertEquals(repeatPrecheckWaitInterval, mockActivityTimeoutsService.getRepeatPrecheckWaitInterval(neType, PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, activityName));
    }

    @Test
    public void testGetRepeatPrecheckWaitIntervalAsInteger() {
        final String neType = "RadioNode";
        final String activityName = "uploadbackup";
        final Integer repeatPrecheckWaitInterval = 10;

        when(activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.BACKUP)).thenReturn(repeatPrecheckConfigurationProvider);
        when(repeatPrecheckConfigurationProvider.getPrecheckWaitInterval(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), activityName)).thenReturn(
                repeatPrecheckWaitInterval);

        assertEquals(repeatPrecheckWaitInterval, mockActivityTimeoutsService.getRepeatPrecheckWaitIntervalAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), activityName));
    }

    @Test
    public void testGetRepeatPrecheckWaitIntervalAsIntegerWithDefaultWaitInterval() {
        final String neType = "RadioNode";
        final String activityName = "uploadbackup";
        final Integer repeatPrecheckWaitInterval = 8;

        when(activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.BACKUP)).thenReturn(null);
        when(shmJobDefaultActivityTimeouts.getRepeatPrecheckWaitIntervalAsInteger()).thenReturn(repeatPrecheckWaitInterval);

        assertEquals(repeatPrecheckWaitInterval, mockActivityTimeoutsService.getRepeatPrecheckWaitIntervalAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), activityName));
    }

    @Test
    public void testGetRepeatPrecheckRetryAttempt() {
        final String neType = "RadioNode";
        final String activityName = "uploadbackup";
        final int repeatPrecheckRetryAttempt = 3;

        when(activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.BACKUP)).thenReturn(repeatPrecheckConfigurationProvider);
        when(repeatPrecheckConfigurationProvider.getPrecheckRetryAttempt(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), activityName)).thenReturn(repeatPrecheckRetryAttempt);

        assertEquals(repeatPrecheckRetryAttempt, mockActivityTimeoutsService.getRepeatPrecheckRetryAttempt(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), activityName));
    }

    @Test
    public void testGetRepeatPrecheckRetryAttemptWithDefaultRetryAttempt() {
        final String neType = "RadioNode";
        final String activityName = "uploadbackup";
        final int repeatPrecheckRetryAttempt = 1;

        when(activityTimeoutsFactory.getRepeatPrecheckConfigurationProvider(JobType.BACKUP)).thenReturn(null);
        when(shmJobDefaultActivityTimeouts.getRepeatPrecheckRetryAttempt()).thenReturn(repeatPrecheckRetryAttempt);

        assertEquals(repeatPrecheckRetryAttempt, mockActivityTimeoutsService.getRepeatPrecheckRetryAttempt(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.BACKUP.name(), activityName));
    }
    
    @Test
    public void testNodeSyncCheckWaitIntervalOrTimeOut() {
        final String nodeSyncCheckWaitIntervalOrTimeOut = mockActivityTimeoutsService.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck", ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        assertTrue(nodeSyncCheckWaitIntervalOrTimeOut == null);
    }


}
