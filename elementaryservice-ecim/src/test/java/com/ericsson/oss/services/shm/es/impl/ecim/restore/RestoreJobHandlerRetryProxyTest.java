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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;

@RunWith(MockitoJUnitRunner.class)
public class RestoreJobHandlerRetryProxyTest {

    @InjectMocks
    RestoreJobHandlerRetryProxy objectUnderTest;

    @Mock
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Mock
    private RestoreJobHandler restoreJobHandler;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    long activityJobId = 123;
    String nodeName = "Some Node Name";
    String propertyToBeUpdated = "Some Property";

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyReturningTrue() {
        getNoOfRetries();

        when(restoreJobHandler.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated)).thenReturn(true);

        assertTrue(objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated));
    }

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyReturningFalse() {
        getNoOfRetries();

        when(restoreJobHandler.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated)).thenReturn(false);

        assertFalse(objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated));
    }

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyThrowsEJBException() {
        getNoOfRetries();

        doThrow(EJBException.class).when(restoreJobHandler).determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);
        when(dpsAvailabilityInfoProvider.isDatabaseDown()).thenReturn(true);
        objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);

        verify(dpsConfigurationParamProvider, times(5)).getdpsWaitIntervalInMS();
    }

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyThrowsEJBTransactionRolledbackException() {
        getNoOfRetries();

        doThrow(EJBTransactionRolledbackException.class).when(restoreJobHandler).determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);
        when(dpsAvailabilityInfoProvider.isDatabaseDown()).thenReturn(false);
        objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);

        verify(dpsConfigurationParamProvider, times(5)).getDpsOptimisticLockWaitIntervalInMS();
    }

    private void getNoOfRetries() {
        when(dpsConfigurationParamProvider.getdpsRetryCount()).thenReturn(5);
    }
}
