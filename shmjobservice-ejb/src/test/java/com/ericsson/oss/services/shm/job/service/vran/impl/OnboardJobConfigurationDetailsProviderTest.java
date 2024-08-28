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
package com.ericsson.oss.services.shm.job.service.vran.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProviderFactory;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * Unit tests for OnboardJobConfigurationDetailsProvider class
 * 
 * @author xeswpot
 * 
 */
@RunWith(value = MockitoJUnitRunner.class)
public class OnboardJobConfigurationDetailsProviderTest {

    @InjectMocks
    private OnboardJobConfigurationDetailsProvider onboardJobConfigurationDetailsProvider;

    @Mock
    private JobTemplate jobTemplateMock;

    @Mock
    private JobConfiguration jobConfigurationMock;

    @Mock
    private ActivityParamMapper activityParamMapperMock;

    @Mock
    private JobTypeDetailsProviderFactory jobTypeDetailsProviderFactoryMock;

    @Mock
    private OnboardJobDetailsProvider onboardJobDetailsProviderMock;

    @Test
    public void testGetJobConfigurationDetails() {

        final String neType = "NFVO";
        final Set<String> neTypes = new HashSet<String>();
        neTypes.add(neType);

        final JobConfigurationDetails neJobConfigurationDetail = new JobConfigurationDetails();
        neJobConfigurationDetail.setNeType(neType);

        Mockito.when(jobTemplateMock.getJobType()).thenReturn(com.ericsson.oss.services.shm.jobs.common.modelentities.JobType.ONBOARD);
        Mockito.when(activityParamMapperMock.getNeTypes(Matchers.anyList())).thenReturn(neTypes);
        Mockito.when(jobTypeDetailsProviderFactoryMock.getJobTypeDetailsProvider(PlatformTypeEnum.vRAN, JobType.ONBOARD)).thenReturn(onboardJobDetailsProviderMock);
        Mockito.when(onboardJobDetailsProviderMock.getJobConfigParamDetails(jobConfigurationMock, neType)).thenReturn(neJobConfigurationDetail);

        final List<JobConfigurationDetails> jobConfigurationDetails = onboardJobConfigurationDetailsProvider.getJobConfigurationDetails(jobTemplateMock, jobConfigurationMock);
        Assert.assertEquals(jobConfigurationDetails.get(0).getNeType(), "NFVO");

    }

    @Test
    public void getNetworkElementsByNeNames() {

        final List<String> neNames = new ArrayList<String>();
        neNames.add("TestNFVO");

        final List<NetworkElement> result = onboardJobConfigurationDetailsProvider.getNetworkElementsByNeNames(neNames, SHMCapabilities.ONBOARD_JOB_CAPABILITY);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("TestNFVO", result.get(0).getName());
        Assert.assertEquals("NFVO", result.get(0).getNeType());
    }
}
