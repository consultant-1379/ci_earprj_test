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
package com.ericsson.oss.services.shm.nejob.cache;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class NeJobStaticDataProviderImplTest {

    @InjectMocks
    private NeJobStaticDataProviderImpl neJobStaticDataProviderImpl;

    @Mock
    private NeJobStaticDataCache neJobStaticDataCache;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationService;

    private static final long ACTIVITY_JOB_ID = 1234L;
    private static final long neJobId = 12345L;
    private static final long mainJobId = 123456L;
    private static final String nodeName = "LTEERBS00001";
    private static final String neJobBusinessKey = nodeName + "@" + mainJobId;
    private static final String platformType = "ECIM";
    private static final long activityStartTime = 1122334455l;
    private static final String parentNodeName = "LTEERBS00001";
    private static final long toBeVerifiedActivityStartTime = 0l;

    private void mockNeJobStaticDataCache(final long startTime) {
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, neJobBusinessKey, platformType, startTime, parentNodeName);
        when(neJobStaticDataCache.get(ACTIVITY_JOB_ID)).thenReturn(neJobStaticData);
    }

    @Test
    public void testGetActivityStartTime_WhenActivityStartTimeAvailable() throws MoNotFoundException {
        mockNeJobStaticDataCache(activityStartTime);
        assertNotEquals(toBeVerifiedActivityStartTime, neJobStaticDataProviderImpl.getActivityStartTime(ACTIVITY_JOB_ID));
    }

    @Test
    public void testGetActivityStartTime_WhenActivityStartTimeIsZero() throws MoNotFoundException {
        mockNeJobStaticDataCache(0);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationService.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        assertNotEquals(toBeVerifiedActivityStartTime, neJobStaticDataProviderImpl.getActivityStartTime(ACTIVITY_JOB_ID));
    }

    @Test
    public void testGetActivityStartTime_WhenNeJobStaticDataIsNull() throws MoNotFoundException {
        neJobStaticData = null;
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(jobConfigurationService.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);
        assertNotEquals(toBeVerifiedActivityStartTime, neJobStaticDataProviderImpl.getActivityStartTime(ACTIVITY_JOB_ID));
    }
}
