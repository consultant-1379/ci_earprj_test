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
package com.ericsson.oss.services.shm.es.polling;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.polling.api.ReadCallStatusEnum;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityInfoProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class PollingActivityStatusManagerTest {

    @InjectMocks
    private PollingActivityStatusManager pollingActivityStatusManager;

    @Mock
    private PollingActivityInfoProvider pollingActivityInfoProvider;

    @After
    public void setup() {
        pollingActivityStatusManager.getPollingActivitiesCache().clear();
    }

    @Test
    public void testSubscribeForPolling_subscribedSuccessfully() {
        final long activityJobId = 1l;
        final String activityName = "upgrade";
        final String moFdn = "moFdn";
        final JobTypeEnum jobType = JobTypeEnum.UPGRADE;
        final PlatformTypeEnum platform = PlatformTypeEnum.CPP;
        final int waitTimeToStartPolling = 10;
        JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, activityName, jobType, platform);
        pollingActivityStatusManager.subscribeForPolling(moFdn, jobActivityInfo, waitTimeToStartPolling);
        assertEquals(1, pollingActivityStatusManager.getPollingActivitiesCache().size());
    }

    @Test
    public void testUnsubscribeForPolling_unsubscribedSuccessfully() {
        final long activityJobId = 1l;
        final String activityName = "upgrade";
        final String moFdn = "moFdn1";
        final JobTypeEnum jobType = JobTypeEnum.UPGRADE;
        final PlatformTypeEnum platform = PlatformTypeEnum.CPP;
        final int waitTimeToStartPolling = 10;
        JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, activityName, jobType, platform);
        pollingActivityStatusManager.subscribeForPolling(moFdn, jobActivityInfo, waitTimeToStartPolling);
        assertEquals(1, pollingActivityStatusManager.getPollingActivitiesCache().size());
        pollingActivityStatusManager.unsubscribeFromPolling(moFdn + "_" + activityJobId);

        assertEquals(0, pollingActivityStatusManager.getPollingActivitiesCache().size());
    }

    @Test
    public void testUpdateReadCallStatus_readCallStatusUpdatedSuccessFully() {
        final long activityJobId = 1l;
        final String moFdn = "moFdn1";
        pollingActivityStatusManager.getPollingActivitiesCache().put(moFdn + "_" + activityJobId, pollingActivityInfoProvider);

        pollingActivityStatusManager.updateReadCallStatus(activityJobId, moFdn, ReadCallStatusEnum.IN_PROGRESS);

        verify(pollingActivityInfoProvider, times(1)).setReadCallStatus(ReadCallStatusEnum.IN_PROGRESS);
    }
}
