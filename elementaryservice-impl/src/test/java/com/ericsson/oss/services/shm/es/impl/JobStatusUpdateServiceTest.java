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
package com.ericsson.oss.services.shm.es.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class JobStatusUpdateServiceTest {

    @Mock
    private DpsReader dpsReaderMock;

    @Mock
    private DpsWriter dpsWriterMock;

    @Mock
    PersistenceObject activityPO;

    @InjectMocks
    JobStatusUpdateService jobStatusUpdateService;

    @Test
    public void checkUpdationOfNEJobsCompletedCountReturnFalse() {
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, String> neCompleted = new HashMap<String, String>();
        final Map<String, String> neSubmitted = new HashMap<String, String>();
        final List<Map<String, String>> attributeMapList = new ArrayList<Map<String, String>>();
        neCompleted.put("key", "neCompleted");
        neCompleted.put("value", Integer.toString(0));
        neSubmitted.put("key", "submittedNEs");
        neSubmitted.put("value", Integer.toString(2));
        attributeMapList.add(neSubmitted);
        attributeMapList.add(neCompleted);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, attributeMapList);

        when(dpsReaderMock.findPOByPoId(1234)).thenReturn(activityPO);
        when(activityPO.getAllAttributes()).thenReturn(mainJobAttributes);
        doNothing().when(dpsWriterMock).update(1234, mainJobAttributes);
        assertEquals(false, jobStatusUpdateService.updateNEJobsCompletedCount(1234));
    }

    @Test
    public void checkUpdationOfNEJobsCompletedCountReturnTrue() {
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, String> neCompleted = new HashMap<String, String>();
        final Map<String, String> neSubmitted = new HashMap<String, String>();
        final List<Map<String, String>> attributeMapList = new ArrayList<Map<String, String>>();
        neCompleted.put("key", "neCompleted");
        neCompleted.put("value", Integer.toString(0));
        neSubmitted.put("key", "submittedNEs");
        neSubmitted.put("value", Integer.toString(1));
        attributeMapList.add(neSubmitted);
        attributeMapList.add(neCompleted);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, attributeMapList);

        when(dpsReaderMock.findPOByPoId(1234)).thenReturn(activityPO);
        when(activityPO.getAllAttributes()).thenReturn(mainJobAttributes);
        doNothing().when(dpsWriterMock).update(1234, mainJobAttributes);
        assertEquals(true, jobStatusUpdateService.updateNEJobsCompletedCount(1234));
    }

}
