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
package com.ericsson.oss.services.shm.job.service.cpp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartJobDetailsProviderTest {

    @Mock
    private ActivityParamMapper activityParamMapper;

    @InjectMocks
    NodeRestartJobDetailsProvider nodeRestartJobDetailsProvider;

    @Test
    public void testGetJobConfigurationDetails() {
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        List<Map<String, String>> result = nodeRestartJobDetailsProvider.getJobConfigurationDetails(jobConfiguration, PlatformTypeEnum.CPP, "ERBS", "LTE02ERBS00001");
        assertNotNull(result);
    }

    @Test
    public void test() {
        final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();
        JobConfigurationDetails jobConfigurationDetails = new JobConfigurationDetails();
        JobConfiguration jobConfiguration = new JobConfiguration();
        acitvityParameters.put("manualrestart", Arrays.asList(JobPropertyConstants.RESTART_RANK, JobPropertyConstants.RESTART_REASON, JobPropertyConstants.RESTART_INFO));
        when((activityParamMapper.getJobConfigurationDetails(jobConfiguration, "ERBS", PlatformTypeEnum.CPP.name(), acitvityParameters))).thenReturn(jobConfigurationDetails);
        final JobConfigurationDetails result = nodeRestartJobDetailsProvider.getJobConfigParamDetails(jobConfiguration, "ERBS");
        assertEquals(result, jobConfigurationDetails);
    }

}
