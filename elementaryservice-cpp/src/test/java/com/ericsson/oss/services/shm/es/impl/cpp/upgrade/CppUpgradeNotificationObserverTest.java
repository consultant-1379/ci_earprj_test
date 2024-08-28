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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

public class CppUpgradeNotificationObserverTest {

    @Test
    public void testGetFilter() {
        final CppUpgradeNotificationObserver objectUnderTest = new CppUpgradeNotificationObserver();
        assertEquals(ShmCommonConstants.SHM_UPGRADE_NOTFICATION_FILTER, objectUnderTest.getFilter());
    }

}
