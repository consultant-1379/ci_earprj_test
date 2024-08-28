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
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CppNodeRestartValidatorImplTest {

    @InjectMocks
    private CppNodeRestartValidatorImpl cppNodeRestartValidatorImpl;

    @Mock
    private NodeRestartUtility nodeRestartUtility;

    String neName = "Some Ne Name";

    @Test
    public void testWhenNodeIsReachable() {
        when(nodeRestartUtility.isNodeReachable(neName)).thenReturn(true);
        final boolean isnodereachable = cppNodeRestartValidatorImpl.isNodeReachable(neName);
        assertEquals(true, isnodereachable);
    }

    @Test
    public void testWhennodeIsNotReachable() {
        when(nodeRestartUtility.isNodeReachable(neName)).thenReturn(false);
        final boolean isnodereachable = cppNodeRestartValidatorImpl.isNodeReachable(neName);
        assertEquals(false, isnodereachable);
    }
}
