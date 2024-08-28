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
package com.ericsson.oss.services.shm.job.resources;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class ActivityDataFacadeTest {

    @InjectMocks
    private ActivityDataFacade objectUnderTest;

    @Mock
    private JobActivitiesQuery jobActivitiesQueryMock;

    @Mock
    private JobActivitiesProvider jobActivitiesProviderMock;

    @Test
    public void testgetJobActivitInformation() {
        final List<JobActivitiesResponse> response = objectUnderTest.getJobActivitInformation(Arrays.asList(jobActivitiesQueryMock));
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.size());
        verify(jobActivitiesProviderMock, times(1)).getNeTypeActivities(Arrays.asList(jobActivitiesQueryMock));
    }

    @Test
    public void testgetJobActivitInformation2() {
        final JobActivitiesResponse jobActivitiesResponse = new JobActivitiesResponse();
        final List<JobActivitiesResponse> responseMock = new ArrayList<>();
        jobActivitiesResponse.setJobType(JobTypeEnum.BACKUP.name());
        jobActivitiesResponse.setPlatform(PlatformTypeEnum.CPP.name());
        responseMock.add(jobActivitiesResponse);
        when(jobActivitiesProviderMock.getNeTypeActivities(Arrays.asList(jobActivitiesQueryMock))).thenReturn(responseMock);
        final List<JobActivitiesResponse> response = objectUnderTest.getJobActivitInformation(Arrays.asList(jobActivitiesQueryMock));
        Assert.assertEquals(responseMock, response);
        verify(jobActivitiesProviderMock, times(1)).getNeTypeActivities(Arrays.asList(jobActivitiesQueryMock));
    }
}
