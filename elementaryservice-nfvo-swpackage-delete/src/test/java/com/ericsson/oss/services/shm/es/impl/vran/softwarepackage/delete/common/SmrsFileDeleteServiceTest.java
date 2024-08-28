/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.*;

import javax.resource.ResourceException;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.ra.FileConnection;
import com.ericsson.oss.services.shm.ra.FileConnectionFactory;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class SmrsFileDeleteServiceTest {

    @Mock
    private FileConnectionFactory fileConnectionFactory;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private FileResource fileResource;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @InjectMocks
    private SmrsFileDeleteService smrsFileDeleteService;

    private static final long ACTIVITY_JOB_ID = 12345;

    private static final String FILE_PATH = "";

    @Mock
    private FileConnection fileConnection;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();

    private final Map<String, Object> jobLogMap = new HashMap<String, Object>();

    @Before
    public void setup() {
        jobLogs.add(jobLogMap);
    }

    @Test
    public void deletePackageIfExistsInENM() throws ResourceException, IOException {
        when(fileResource.isDirectoryExists(FILE_PATH)).thenReturn(true);
        when(fileConnectionFactory.getConnection()).thenReturn(fileConnection);
        smrsFileDeleteService.delete(FILE_PATH, ACTIVITY_JOB_ID);
        verify(jobAttributesPersistenceProvider).persistJobLogs(ACTIVITY_JOB_ID, jobLogs);
    }

    @Test
    public void deletePackageIfNotExistsInENM() throws ResourceException, IOException {
        when(fileResource.isDirectoryExists(FILE_PATH)).thenReturn(false);
        when(fileConnectionFactory.getConnection()).thenReturn(fileConnection);
        smrsFileDeleteService.delete(FILE_PATH, ACTIVITY_JOB_ID);
        verify(jobAttributesPersistenceProvider).persistJobLogs(ACTIVITY_JOB_ID, jobLogs);
    }

    @Test
    public void deletePackageCheckExceptionBlock() throws ResourceException, IOException {
        when(fileResource.isDirectoryExists(FILE_PATH)).thenReturn(true);
        smrsFileDeleteService.delete(FILE_PATH, ACTIVITY_JOB_ID);
        verify(jobAttributesPersistenceProvider).persistJobLogs(ACTIVITY_JOB_ID, jobLogs);
    }
}
