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
package com.ericsson.oss.services.shm.job.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JobsReader.class)
public class JobsReaderTest {

    @InjectMocks
    JobsReader jobsReader;

    @Mock
    private MainJobDetailsReaderRetryProxy mainJobDetailsRetryProxyMock;

    @Test
    public void testGetMainJob() {
        final long poId = 1234L;

        final Map<String, Object> mainJobMock = new HashMap<>();
        when(mainJobDetailsRetryProxyMock.getMainJob(poId)).thenReturn(mainJobMock);
        jobsReader.getMainJob(poId);
        verify(mainJobDetailsRetryProxyMock, times(1)).getMainJob(poId);
    }
}
