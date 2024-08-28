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
package com.ericsson.oss.services.shm.job.service;

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

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.cpp.impl.BackupJobDetailsProvider;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * Unit test cases for DefaultJobConfigurationDetailsProvider class
 * 
 * @author xeswpot
 * 
 */
@RunWith(value = MockitoJUnitRunner.class)
public class DefaultJobConfigurationDetailsProviderTest {

    @InjectMocks
    private DefaultJobConfigurationDetailsProvider defaultJobConfigurationDetailsProvider;

    @Mock
    private JobTemplate jobTemplateMock;

    @Mock
    private JobConfiguration jobConfigurationMock;

    @Mock
    private ActivityParamMapper activityParamMapperMock;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImplMock;

    @Mock
    private JobTypeDetailsProviderFactory jobTypeDetailsProviderFactoryMock;

    @Mock
    private BackupJobDetailsProvider backupJobDetailsProviderMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private JobCapabilityProvider capabilityProviderMock;

    @Test
    public void testGetJobConfigurationDetails() {

        final String neType = "ERBS";
        final Set<String> neTypes = new HashSet<String>();
        neTypes.add(neType);

        final JobConfigurationDetails neJobConfigurationDetail = new JobConfigurationDetails();
        neJobConfigurationDetail.setNeType(neType);

        Mockito.when(jobTemplateMock.getJobType()).thenReturn(com.ericsson.oss.services.shm.jobs.common.modelentities.JobType.BACKUP);
        Mockito.when(activityParamMapperMock.getNeTypes(Matchers.anyList())).thenReturn(neTypes);
        Mockito.when(capabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(neType, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(PlatformTypeEnum.CPP);
        Mockito.when(jobTypeDetailsProviderFactoryMock.getJobTypeDetailsProvider(PlatformTypeEnum.CPP, JobType.BACKUP)).thenReturn(backupJobDetailsProviderMock);
        Mockito.when(backupJobDetailsProviderMock.getJobConfigParamDetails(jobConfigurationMock, neType)).thenReturn(neJobConfigurationDetail);

        final List<JobConfigurationDetails> jobConfigurationDetails = defaultJobConfigurationDetailsProvider.getJobConfigurationDetails(jobTemplateMock, jobConfigurationMock);
        Assert.assertEquals(jobConfigurationDetails.get(0).getNeType(), "ERBS");
    }

    @Test
    public void getNetworkElementsByNeNames() {

        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");

        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setName("node1");
        networkElement.setNeType("ERBS");
        networkElement.setNetworkElementFdn("NetworkElement=node1");
        networkElements.add(networkElement);

        Mockito.when(capabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, null)).thenReturn(networkElements);
        final List<NetworkElement> result = defaultJobConfigurationDetailsProvider.getNetworkElementsByNeNames(neNames, null);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("node1", result.get(0).getName());
        Assert.assertEquals("ERBS", result.get(0).getNeType());
        Assert.assertEquals("NetworkElement=node1", result.get(0).getNetworkElementFdn());
    }

    @Test
    public void getNetworkElementsByNeNamesWhenCapabilityIsProvided() {

        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");

        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setName("node1");
        networkElement.setNeType("ERBS");
        networkElement.setNetworkElementFdn("NetworkElement=node1");
        networkElements.add(networkElement);

        Mockito.when(capabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElements);
        final List<NetworkElement> result = defaultJobConfigurationDetailsProvider.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("node1", result.get(0).getName());
        Assert.assertEquals("ERBS", result.get(0).getNeType());
        Assert.assertEquals("NetworkElement=node1", result.get(0).getNetworkElementFdn());
    }
}
