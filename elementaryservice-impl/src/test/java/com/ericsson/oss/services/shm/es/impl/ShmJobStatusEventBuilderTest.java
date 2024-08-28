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
package com.ericsson.oss.services.shm.es.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.events.ShmJobStatusEvent;

@RunWith(MockitoJUnitRunner.class)
public class ShmJobStatusEventBuilderTest {

    @InjectMocks
    private ShmJobStatusEventBuilder shmJobStatusEventBuilder;

    @Test
    public void testLicenseJobSuccessCase() {

        final Map<String, Object> jobAttributes = buildTestData();

        final ShmJobStatusEvent jobStatusEvent = shmJobStatusEventBuilder.buildJobStatusEvent(jobAttributes, JobResult.SUCCESS.toString());

        assertNotNull(jobStatusEvent);
        assertEquals("LTE555GRadioNode00001", jobStatusEvent.getNodeName());
        assertEquals(com.ericsson.oss.services.shm.model.events.JobResult.SUCCESS, jobStatusEvent.getResult());
    }

    @Test
    public void testLicenseJobFailureCase() {

        final Map<String, Object> jobAttributes = buildTestData();

        final ShmJobStatusEvent jobStatusEvent = shmJobStatusEventBuilder.buildJobStatusEvent(jobAttributes, JobResult.FAILED.toString());

        assertNotNull(jobStatusEvent);
        assertEquals("LTE555GRadioNode00001", jobStatusEvent.getNodeName());
        assertEquals(com.ericsson.oss.services.shm.model.events.JobResult.FAILED, jobStatusEvent.getResult());
    }

    @Test
    public void testLicenseJobSkippedCase() {
        final Map<String, Object> jobAttributes = buildTestData();
        final ShmJobStatusEvent jobStatusEvent = shmJobStatusEventBuilder.buildJobStatusEvent(jobAttributes, JobResult.SKIPPED.toString());
        assertNotNull(jobStatusEvent);
        assertEquals("LTE555GRadioNode00001", jobStatusEvent.getNodeName());
        assertEquals(com.ericsson.oss.services.shm.model.events.JobResult.SUCCESS, jobStatusEvent.getResult());
    }

    private Map<String, Object> buildTestData() {
        final Map<String, Object> jobConfiguration = new HashMap<>();
        final Map<String, Object> jobAttributes = new HashMap<>();

        final Map<String, String> lkfPath = new HashMap<>();
        lkfPath.put(ShmConstants.KEY, "LICENSE_FILEPATH");
        lkfPath.put(ShmConstants.VALUE, "/dummy/file/path/");

        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        jobProperties.add(lkfPath);

        final Map<String, Object> selectedNEs = new HashMap<String, Object>();
        final List<String> neNames = new ArrayList<String>();
        neNames.add("LTE555GRadioNode00001");
        selectedNEs.put(ShmConstants.NENAMES, neNames);

        jobConfiguration.put(ShmConstants.SELECTED_NES, selectedNEs);
        jobAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        return jobAttributes;
    }
}
