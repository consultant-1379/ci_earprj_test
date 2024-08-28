/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.services.shm.loadcontrol.local.api.StagedActivityRequestBean;

@RunWith(MockitoJUnitRunner.class)
public class LoadControlQueueProducerTest {

    @InjectMocks
    private LoadControlQueueProducer loadControlQueueProducer;

    @Mock
    private StagedActivityRequestBean stagedActivityRequest;

    @Mock
    private ChannelLocator channelLocator;

    @Mock
    private Channel channelMock;

    @Test
    public void testKeepStagedActivitiesInQueue() {
        when(channelLocator.lookupChannel(Matchers.anyString())).thenReturn(channelMock);
        loadControlQueueProducer.keepStagedActivitiesInQueue(stagedActivityRequest);
        verify(channelMock, times(1)).send(stagedActivityRequest);
    }

    @Test
    public void testKeepStagedActivitiesInQueueWhenChannelisNull() {
        when(channelLocator.lookupChannel(Matchers.anyString())).thenReturn(null);
        loadControlQueueProducer.keepStagedActivitiesInQueue(stagedActivityRequest);
        verify(channelMock, times(0)).send(stagedActivityRequest);
    }
}
