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
public class BackupJobActivityTimeoutsTest {

    @InjectMocks
    private BackupJobActivityTimeouts backupJobActivityTimeouts;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void testListenForRncBackupUploadActivityTimeoutAttribute() {
        backupJobActivityTimeouts.constructTimeOutsMap();
        backupJobActivityTimeouts.listenForRncBackupUploadActivityTimeoutAttribute(60);
        assertEquals(backupJobActivityTimeouts.getActivityTimeoutAsInteger("RNC", "CPP", "BACKUP", "exportcv"), Integer.valueOf(60));

    }

}