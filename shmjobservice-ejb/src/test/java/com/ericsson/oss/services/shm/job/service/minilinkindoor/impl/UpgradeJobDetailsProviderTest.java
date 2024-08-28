/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.minilinkindoor.impl;

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
public class UpgradeJobDetailsProviderTest {

    @Mock
    private ActivityParamMapper activityParamMapper;

    @InjectMocks
    UpgradeJobDetailsProvider upgradeJobDetailsProvider;

    @Test
    public void testGetJobConfigurationDetails() {
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        List<Map<String, String>> upgradeJobDetailsList = upgradeJobDetailsProvider.getJobConfigurationDetails(jobConfiguration, PlatformTypeEnum.MINI_LINK_INDOOR, "MINI-LINK-Indoor", "neName");
        for (Map<String, String> result : upgradeJobDetailsList) {
            assertNotNull(result);
        }
    }

    @Test
    public void testGetJobConfigParamDetails() {
        JobConfiguration jobConfiguration = new JobConfiguration();
        JobConfigurationDetails jobConfigurationDetails = new JobConfigurationDetails();
        Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();
        acitvityParameters.put("activate", Arrays.asList(JobPropertyConstants.STEP_BY_STEP));
        acitvityParameters.put("download", Arrays.asList(JobPropertyConstants.STEP_BY_STEP));
        acitvityParameters.put("confirm", Arrays.asList(JobPropertyConstants.STEP_BY_STEP));
        when(activityParamMapper.getJobConfigurationDetails(jobConfiguration, "MINI-LINK-Indoor", PlatformTypeEnum.MINI_LINK_INDOOR.name(), acitvityParameters)).thenReturn(jobConfigurationDetails);
        JobConfigurationDetails result = upgradeJobDetailsProvider.getJobConfigParamDetails(jobConfiguration, "MINI-LINK-Indoor");
        assertEquals(result, jobConfigurationDetails);
    }

}
