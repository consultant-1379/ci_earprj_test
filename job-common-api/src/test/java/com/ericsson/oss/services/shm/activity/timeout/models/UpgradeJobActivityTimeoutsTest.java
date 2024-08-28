/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.activity.timeout.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeJobActivityTimeoutsTest {

    @InjectMocks
    private UpgradeJobActivityTimeouts upgradeJobActivityTimeouts;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void testListenForRncUpgradeConfirmActivityTimeoutAttribute() {
        upgradeJobActivityTimeouts.constructTimeOutsMap();
        upgradeJobActivityTimeouts.listenForRncUpgradeConfirmActivityTimeoutAttribute(10);
        assertEquals(upgradeJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "UPGRADE", "confirm"), Integer.valueOf(10));

    }


    @Test
    public void testListenForRncUpgradeUpgradeActivityTimeoutAttribute() {
        upgradeJobActivityTimeouts.constructTimeOutsMap();
        upgradeJobActivityTimeouts.listenForRncUpgradeUpgradeActivityTimeoutAttribute(60);
        assertEquals(upgradeJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "UPGRADE", "upgrade"), Integer.valueOf(60));

    }

    @Test
    public void testListenForRncUpgradeInstallActivityTimeoutAttribute() {
        upgradeJobActivityTimeouts.constructTimeOutsMap();
        upgradeJobActivityTimeouts.listenForRncUpgradeInstallActivityTimeoutAttribute(120);
        assertEquals(upgradeJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "UPGRADE", "install"), Integer.valueOf(120));

    }

    @Test
    public void testListenForRbsUpgradeConfirmActivityTimeoutAttribute() {
        upgradeJobActivityTimeouts.listenForRbsUpgradeConfirmActivityTimeoutAttribute(5);
        assertEquals(upgradeJobActivityTimeouts.getActivityTimeoutAsInteger("RBS", "CPP", "UPGRADE", "confirm"), Integer.valueOf(5));

    }

}