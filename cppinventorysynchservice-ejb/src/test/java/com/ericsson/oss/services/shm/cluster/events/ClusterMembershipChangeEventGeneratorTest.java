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
package com.ericsson.oss.services.shm.cluster.events;

import static org.mockito.Mockito.*;

import javax.enterprise.event.Event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.cluster.ClusterMembershipChangeEvent;

@RunWith(MockitoJUnitRunner.class)
public class ClusterMembershipChangeEventGeneratorTest {

    @Mock
    Event<ClusterMembershipChangeEvent> clusterMembershipChangeEvents;

    @InjectMocks
    ClusterMembershipChangeEventGenerator clusterMembershipChangeEventGenerator;

    @Test
    public void testgenerateMemeberShipChangeEvent() {
        clusterMembershipChangeEventGenerator.generateMemberShipChangeEvent();
        verify(clusterMembershipChangeEvents, times(1)).fire(Matchers.any(ClusterMembershipChangeEvent.class));
    }

}
