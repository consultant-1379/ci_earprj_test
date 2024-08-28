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
package com.ericsson.oss.services.shm.activity.timeout.models;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class NodeHealthCheckJobActivityTimeoutsTest {

    @InjectMocks
    private NodeHealthCheckJobActivityTimeouts objectUnderTest;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Test
    public void timeoutsTestForBeforeAndAfterListeningTimeout() {

        objectUnderTest.constructTimeOutsMap();
        String cppTimeout = objectUnderTest.getActivityTimeout(NodeType.ERBS.getName(), PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck");
        String ecimTimeout = objectUnderTest.getActivityTimeout(NodeType.ERBS.getName(), PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck");
        String cppNodeSyncCheckWaitIntervalForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        String cppNodeSyncCheckWaitIntervalForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        String cppNodeSyncCheckTimeoutStrForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        String cppNodeSyncCheckTimeoutStrForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        String ecimNodeSyncCheckWaitIntervalForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        String ecimNodeSyncCheckWaitIntervalForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        String ecimNodeSyncCheckTimeoutStrForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        String ecimNodeSyncCheckTimeoutStrForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        Integer syncTimeOut = objectUnderTest.getNodeSyncCheckTimeOutAsInteger(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);

        when(shmJobDefaultActivityTimeouts.getDefaultActivityTimeoutBasedOnPlatform(PlatformTypeEnum.CPP.toString())).thenReturn(0);

        Assert.assertEquals(cppTimeout, "PT0M");
        Assert.assertEquals(ecimTimeout, "PT0M");
        Assert.assertEquals(cppNodeSyncCheckWaitIntervalForNHC, "PT0M");
        Assert.assertEquals(cppNodeSyncCheckTimeoutStrForNHC, "PT0M");
        Assert.assertEquals(ecimNodeSyncCheckWaitIntervalForNHC, "PT0M");
        Assert.assertEquals(ecimNodeSyncCheckTimeoutStrForNHC, "PT0M");
        Assert.assertEquals(cppNodeSyncCheckWaitIntervalForENM, "PT0M");
        Assert.assertEquals(cppNodeSyncCheckTimeoutStrForENM, "PT0M");
        Assert.assertEquals(ecimNodeSyncCheckWaitIntervalForENM, "PT0M");
        Assert.assertEquals(ecimNodeSyncCheckTimeoutStrForENM, "PT0M");
        Assert.assertEquals(syncTimeOut.intValue(), 0);

        int cppEnmHealthCheckActivityTimeOut = 9;
        int ecimEnmHealthCheckActivityTimeOut = 8;
        int cppNodeSyncCheckWaitIntervalInMin = 2;
        int cppNodeSyncCheckTimeout = 10;
        int ecimNodeSyncCheckWaitIntervalInMin = 2;
        int ecimNodeSyncCheckTimeout = 10;

        objectUnderTest.listenCppEnmHealthCheckActivityTimeOut(cppEnmHealthCheckActivityTimeOut);
        objectUnderTest.listenEcimEnmHealthCheckActivityTimeOut(ecimEnmHealthCheckActivityTimeOut);
        objectUnderTest.listencppNodeSyncCheckWaitIntervalInMin(cppNodeSyncCheckWaitIntervalInMin);
        objectUnderTest.listencppNodeSyncCheckTimeout(cppNodeSyncCheckTimeout);
        objectUnderTest.listenECIMNodeSyncCheckWaitIntervalInMin(ecimNodeSyncCheckWaitIntervalInMin);
        objectUnderTest.listenECIMNodeSyncCheckTimeout(ecimNodeSyncCheckTimeout);
        objectUnderTest.constructTimeOutsMap();

        cppTimeout = objectUnderTest.getActivityTimeout(NodeType.ERBS.getName(), PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck");
        ecimTimeout = objectUnderTest.getActivityTimeout(NodeType.ERBS.getName(), PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck");
        cppNodeSyncCheckWaitIntervalForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        cppNodeSyncCheckWaitIntervalForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        cppNodeSyncCheckTimeoutStrForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        cppNodeSyncCheckTimeoutStrForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        ecimNodeSyncCheckWaitIntervalForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        ecimNodeSyncCheckWaitIntervalForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
        ecimNodeSyncCheckTimeoutStrForNHC = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "nodehealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        ecimNodeSyncCheckTimeoutStrForENM = objectUnderTest.getNodeSyncCheckWaitIntervalOrTimeOut(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        syncTimeOut = objectUnderTest.getNodeSyncCheckTimeOutAsInteger(PlatformTypeEnum.ECIM, JobTypeEnum.NODE_HEALTH_CHECK, "enmhealthcheck",ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
        
        Assert.assertEquals(cppTimeout, "PT9M");
        Assert.assertEquals(ecimTimeout, "PT8M");
        Assert.assertEquals(cppNodeSyncCheckWaitIntervalForNHC, "PT2M");
        Assert.assertEquals(cppNodeSyncCheckTimeoutStrForNHC, "PT10M");
        Assert.assertEquals(ecimNodeSyncCheckWaitIntervalForNHC, "PT2M");
        Assert.assertEquals(ecimNodeSyncCheckTimeoutStrForNHC, "PT10M");
        Assert.assertEquals(cppNodeSyncCheckWaitIntervalForENM, "PT2M");
        Assert.assertEquals(cppNodeSyncCheckTimeoutStrForENM, "PT10M");
        Assert.assertEquals(ecimNodeSyncCheckWaitIntervalForENM, "PT2M");
        Assert.assertEquals(ecimNodeSyncCheckTimeoutStrForENM, "PT10M");
        Assert.assertEquals(syncTimeOut.intValue(), 10);
    }

}
