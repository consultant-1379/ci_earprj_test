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
package com.ericsson.oss.services.shm.cpp.inventory.service.registration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.services.shm.cluster.events.ClusterMembershipChangeEventGenerator;
import com.ericsson.oss.services.shm.loadcontrol.local.api.PrepareLoadControllerLocalCounterService;

@RunWith(MockitoJUnitRunner.class)
public class CppInventorySynchEventListenerRegistrationTest {
    @Mock
    private MembershipChangeEvent changeEvent;

    @Mock
    private ClusterMembershipChangeEventGenerator shmMemberShipEventGenerator;

    @Mock
    private PrepareLoadControllerLocalCounterService prepareLoadControllerLocalCounterService;

    @Mock
    private Logger loggerMock;
    @InjectMocks
    private CppInventorySynchEventListenerRegistration cppListnrReg;

    @Test
    public void test_whenEventisMaster() {
        when(changeEvent.isMaster()).thenReturn(true);
        cppListnrReg.listenForMembershipChange(changeEvent);
        assertTrue(cppListnrReg.isMaster());
        verify(shmMemberShipEventGenerator, times(1)).generateMemberShipChangeEvent();
    }

    @Test
    public void test_whenEventisNotAMaster() {
        when(changeEvent.isMaster()).thenReturn(false);
        cppListnrReg.listenForMembershipChange(changeEvent);
        assertFalse(cppListnrReg.isMaster());
    }

    @Test
    public void testGetCurrentNumberOfMembers() {
        when(changeEvent.getCurrentNumberOfMembers()).thenReturn(2);
        cppListnrReg.listenForMembershipChange(changeEvent);
        verify(prepareLoadControllerLocalCounterService, times(1)).prepareMaxCountMap(2);
    }
}
