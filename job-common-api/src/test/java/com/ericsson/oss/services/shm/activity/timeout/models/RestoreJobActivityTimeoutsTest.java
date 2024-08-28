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
public class RestoreJobActivityTimeoutsTest {

    @InjectMocks
    private RestoreJobActivityTimeouts restoreJobActivityTimeouts;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void testListenForRncRestoreDownloadActivityTimeoutAttribute() {
        restoreJobActivityTimeouts.constructTimeOutsMap();
        restoreJobActivityTimeouts.listenForRncRestoreDownloadActivityTimeoutAttribute(60);
        assertEquals(restoreJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "RESTORE", "download"), Integer.valueOf(60));

    }

    @Test
    public void testListenForRncRestoreInstallActivityTimeoutAttribute() {
        restoreJobActivityTimeouts.constructTimeOutsMap();
        restoreJobActivityTimeouts.listenForRncRestoreInstallActivityTimeoutAttribute(120);
        assertEquals(restoreJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "RESTORE", "install"), Integer.valueOf(120));

    }

    @Test
    public void testListenForRncRestoreActivityTimeoutAttribute() {
        restoreJobActivityTimeouts.constructTimeOutsMap();
        restoreJobActivityTimeouts.listenForRncRestoreActivityTimeoutAttribute(80);
        assertEquals(restoreJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "RESTORE", "restore"), Integer.valueOf(80));

    }

}