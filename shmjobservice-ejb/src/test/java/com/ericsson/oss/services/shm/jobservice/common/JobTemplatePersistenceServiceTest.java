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
package com.ericsson.oss.services.shm.jobservice.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.exception.DuplicateEntityException;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class JobTemplatePersistenceServiceTest {

    @Mock
    private DpsWriter dpsWriter;

    @Mock
    private DpsReader dpsReader;

    @Mock
    private PersistenceObject persistenceObject;

    @Mock
    private SystemRecorder systemRecorder;

    @InjectMocks
    JobTemplatePersistenceService jobTemplatePersistenceService;

    @Test
    public void testGetJobTemplatePOSuccess() {
        final String name = "name";
        final Map<String, Object> jobTemplate = new HashMap<>();
        final List<PersistenceObject> jobTemplates = new ArrayList<>();
        final Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put("name", name);
        when(dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, attributesMap)).thenReturn(jobTemplates);
        when(dpsWriter.createPO(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, ShmConstants.VERSION, jobTemplate)).thenReturn(persistenceObject);
        when(persistenceObject.getPoId()).thenReturn(1l);
        assertEquals(jobTemplatePersistenceService.createJobTemplate(jobTemplate, name), 1l);
        verify(systemRecorder, times(3)).recordEvent(Matchers.anyString(), Matchers.any(EventLevel.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }

    @Test(expected = DuplicateEntityException.class)
    public void testGetJobTemplatePOException() {
        final String name = "name";
        final Map<String, Object> jobTemplate = new HashMap<>();
        final List<PersistenceObject> jobTemplates = new ArrayList<>();
        jobTemplates.add(persistenceObject);
        final Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put("name", name);
        when(dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, attributesMap)).thenReturn(jobTemplates);
        when(persistenceObject.getPoId()).thenReturn(1l);
        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put("templateJobId", persistenceObject.getPoId());
        when(dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictions)).thenReturn(jobTemplates);
        jobTemplatePersistenceService.createJobTemplate(jobTemplate, name);
        verify(systemRecorder).recordEvent(Matchers.anyString(), Matchers.any(EventLevel.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testGetJobTemplatePOFailed() {
        final String name = "name";
        final Map<String, Object> jobTemplate = new HashMap<>();
        final List<PersistenceObject> jobTemplates = new ArrayList<>();
        final Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put("name", name);
        when(dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, attributesMap)).thenReturn(jobTemplates);
        when(dpsWriter.createPO(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, ShmConstants.VERSION, jobTemplate)).thenReturn(null);
        assertEquals(jobTemplatePersistenceService.createJobTemplate(jobTemplate, name), 0l);
        verify(systemRecorder, times(3)).recordEvent(Matchers.anyString(), Matchers.any(EventLevel.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }
}
