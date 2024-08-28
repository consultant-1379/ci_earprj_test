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
package com.ericsson.oss.services.shm.webpush.notifications.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

@RunWith(MockitoJUnitRunner.class)
public class JobNotificationObserverTest {

    @InjectMocks
    JobNotificationObserver objectUnderTest;

    @Test
    public void testGetFilter() {
        assertEquals(ShmCommonConstants.SHM_JOB_NOTIFICATION_FILTER, objectUnderTest.getFilter());
    }
}
