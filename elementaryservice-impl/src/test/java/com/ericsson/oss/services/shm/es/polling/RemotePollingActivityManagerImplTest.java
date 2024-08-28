/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.polling.api.PollingData;
import com.ericsson.oss.services.shm.model.NetworkElementData;

@RunWith(MockitoJUnitRunner.class)
public class RemotePollingActivityManagerImplTest {

    private static final String CALLBACK_QUEUE = "callbackQueue";

    @InjectMocks
    private RemotePollingActivityManagerImpl objectUnderTest;

    @Mock
    private PollingActivityManager pollingActivityManagerMock;

    @Mock
    private JobActivityInfo activityInfoMock;

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private PollingData pollingData;

    @Test
    public void testUnsubscribeByActivityJobId() {
        objectUnderTest.unsubscribeByActivityJobId(123l, "activityName", "nodeName");
        Mockito.verify(pollingActivityManagerMock, Mockito.times(1)).unsubscribeByActivityJobId(123l, "activityName", "nodeName");
    }

    @Test
    public void testSubscribe() {
        objectUnderTest.subscribe(activityInfoMock, networkElementMock, pollingData, CALLBACK_QUEUE, "NODE");

        Mockito.verify(networkElementMock, Mockito.times(1)).getNeType();
        Mockito.verify(networkElementMock, Mockito.times(1)).getNetworkElementFdn();
        Mockito.verify(networkElementMock, Mockito.times(1)).getNodeModelIdentity();
        Mockito.verify(networkElementMock, Mockito.times(1)).getUtcOffset();
        Mockito.verify(networkElementMock, Mockito.times(1)).getNodeRootFdn();
        Mockito.verify(networkElementMock, Mockito.times(1)).getOssModelIdentity();
        Mockito.verify(networkElementMock, Mockito.times(1)).getNeProductVersion();

        Mockito.verify(pollingActivityManagerMock, Mockito.times(1)).subscribe(Matchers.any(JobActivityInfo.class), Matchers.any(NetworkElementData.class), Matchers.any(PollingData.class),
                Matchers.eq(CALLBACK_QUEUE), Matchers.eq("NODE"));
    }

    @Test
    public void testUpdatePollingAttributesByActivityJobId() {
        objectUnderTest.updatePollingAttributesByActivityJobId(123l, mapMock);
        Mockito.verify(pollingActivityManagerMock, Mockito.times(1)).updatePollingAttributesByActivityJobId(123l, mapMock);
    }

    @Test
    public void testPrepareAndAddPollingActivityDataToCache() {
        objectUnderTest.prepareAndAddPollingActivityDataToCache(123l, activityInfoMock, CALLBACK_QUEUE);
        Mockito.verify(pollingActivityManagerMock, Mockito.times(1)).prepareAndAddPollingActivityDataToCache(123l, activityInfoMock, CALLBACK_QUEUE);
    }
}
