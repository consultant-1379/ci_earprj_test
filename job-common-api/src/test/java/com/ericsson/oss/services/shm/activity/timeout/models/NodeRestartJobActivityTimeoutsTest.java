/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.activity.timeout.models;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import static org.mockito.Mockito.when;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartJobActivityTimeoutsTest {

    @InjectMocks
    private NodeRestartJobActivityTimeouts nodeRestartJobActivityTimeouts;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private ShmJobDefaultActivityTimeouts shmJobDefaultActivityTimeouts;

    @Test
    public void testListenForRncNodeRestartManualRestartActivityTimeoutAttribute() {
        nodeRestartJobActivityTimeouts.constructTimeOutsMap();
        nodeRestartJobActivityTimeouts.listenForRncNodeRestartManualRestartActivityTimeoutAttribute(20);
        assertEquals(nodeRestartJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "NODERESTART", "manualrestart"), Integer.valueOf(20));
    }

    @Test
    public void testListenForCppNodeRestartManualRestartActivityTimeoutAttribute() {
        nodeRestartJobActivityTimeouts.listenForCppNodeRestartManualRestartActivityTimeoutAttribute(10);
        assertEquals(nodeRestartJobActivityTimeouts.getActivityTimeoutAsInteger("ERBS", "CPP", "NODERESTART", "manualrestart"), Integer.valueOf(10));
        when(shmJobDefaultActivityTimeouts.getDefaultActivityPollingWaitTimeOnPlatformAsInteger(Matchers.anyString())).thenReturn(10);
        assertEquals("PT10M", nodeRestartJobActivityTimeouts.getActivityPollWaitTime("ERBS", PlatformTypeEnum.CPP, JobTypeEnum.NODERESTART, "manualrestart"));
        assertEquals("PT10M", nodeRestartJobActivityTimeouts.getActivityTimeout("ERBS", PlatformTypeEnum.CPP, JobTypeEnum.NODERESTART, "manualrestart"));
    }

}
