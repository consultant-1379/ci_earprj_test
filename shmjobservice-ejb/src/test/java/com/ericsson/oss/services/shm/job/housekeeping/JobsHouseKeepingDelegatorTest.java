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
package com.ericsson.oss.services.shm.job.housekeeping;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@RunWith(MockitoJUnitRunner.class)
public class JobsHouseKeepingDelegatorTest {

    @Mock
    JobsHouseKeepingHelper jobsHouseKeepingHelper;

    @InjectMocks
    JobsHouseKeepingDelegator jobsHouseKeepingDelegator;

    @Mock
    JobsHouseKeepingResponse jobsHouseKeepingResponse;

    @Mock
    SystemRecorder systemRecorder;

    @Test
    public void testHouseKeepingOfJobs() {

        when(jobsHouseKeepingHelper.deleteJobs(Mockito.anyString(), Mockito.anyList())).thenReturn(jobsHouseKeepingResponse);
        jobsHouseKeepingDelegator.houseKeepingOfJobs(Mockito.anyList(), Mockito.anyString());
        assertNotNull(jobsHouseKeepingResponse);

    }
}
