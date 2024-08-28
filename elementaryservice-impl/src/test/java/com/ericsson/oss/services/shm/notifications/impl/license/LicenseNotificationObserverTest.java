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
package com.ericsson.oss.services.shm.notifications.impl.license;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.testng.PowerMockTestCase;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EventConsumerBean.class)
public class LicenseNotificationObserverTest extends PowerMockTestCase {

    @InjectMocks
    LicenseNotificationObserver objectUnderTest;

    @Mock
    EventConsumerBean eventConsumerBean = PowerMockito.mock(EventConsumerBean.class);

    @Test
    public void testGetFilter() {
        assertEquals(ShmCommonConstants.SHM_LICENSE_NOTFICATION_FILTER, objectUnderTest.getFilter());
    }

    @Test
    public void test_startListeningForJobNotifications() {
        Mockito.when(eventConsumerBean.startListening(Matchers.any(LicenseNotificationQueueListener.class))).thenReturn(true);
        objectUnderTest.startListeningForJobNotifications(eventConsumerBean);
    }

}
