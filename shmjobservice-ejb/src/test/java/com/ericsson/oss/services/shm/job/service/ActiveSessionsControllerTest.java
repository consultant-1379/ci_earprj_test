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
package com.ericsson.oss.services.shm.job.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.ServerInternalException;

@RunWith(MockitoJUnitRunner.class)
public class ActiveSessionsControllerTest {

    @InjectMocks
    private ActiveSessionsController objectUnderTest;

    @Mock
    private JobParameterChangeListener listenerMock;

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveSessionsControllerTest.class);

    @Test(expected = ServerInternalException.class)
    public void testWithInvalidApplicationContext() {
        objectUnderTest.exitIfMaxActiveSessionsReached("");
    }

    @Test(expected = ServerInternalException.class)
    public void testWithViewMainJobsWhenCurrentUsersEqualsMaxUsers() {
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
    }

    @Test(expected = ServerInternalException.class)
    public void testWithViewMainJobsWhenCurrentUsersMoreThanMaxLimit() {
        Mockito.when(listenerMock.getActiveUserSessionsMaxLimit()).thenReturn(-1);
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
    }

    @Test
    public void testWithViewMainJobsWhenCurrentUsersAndMaxLimit() {
        Mockito.when(listenerMock.getActiveUserSessionsMaxLimit()).thenReturn(2);
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
        try {
            objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
        } catch (ServerInternalException e) {
            Assert.assertEquals("Server is busy. Please try after sometime", e.getMessage());
            LOGGER.error("ServerInternalException occured. Exception is : {}", e);
        }
        Assert.assertEquals(1, objectUnderTest.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
        Assert.assertEquals(1, objectUnderTest.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
        Assert.assertEquals(1, objectUnderTest.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
        Assert.assertEquals(0, objectUnderTest.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
        objectUnderTest.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);
    }
}
