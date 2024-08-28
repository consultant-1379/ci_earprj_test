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
package com.ericsson.oss.services.shm.job.remote.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.NodeTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.dto.CollectionDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.ManagedObjectDTO;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@RunWith(MockitoJUnitRunner.class)
public class SupportedPlatformAndNeTypeFinderTest {

    @Mock
    PlatformTypeProviderImpl platformTypeProviderImpl;

    @Mock
    NodeTypeProviderImpl nodeTypeProviderImpl;

    @InjectMocks
    SupportedPlatformAndNeTypeFinder supportedPlatformAndNeTypeFinder;

    @Mock
    TopologyEvaluationService topologyEvaluationService;

    @Mock
    List<NetworkElement> neElementListMock;

    @Mock
    NetworkElement neElementMock;

    @Mock
    FdnServiceBeanRetryHelper networkElementsProvider;

    @Test
    public void testFindSupportedPlatformAndNodeTypes() {

        when(platformTypeProviderImpl.getPlatformType("ERBS")).thenReturn(PlatformTypeEnum.CPP);
        final List<ManagedObjectDTO> elements = new ArrayList<ManagedObjectDTO>();
        final CollectionDTO collectionDTO = new CollectionDTO();
        final ManagedObjectDTO managedObjectDTO = new ManagedObjectDTO();
        managedObjectDTO.setFdn("NetworkElement=LTE04ERBS00001");
        managedObjectDTO.setId("281474978168441");
        managedObjectDTO.setType("ERBS");
        elements.add(managedObjectDTO);
        collectionDTO.setElements(elements);
        final JobInfo jobInfo = new JobInfo();
        final List<String> collectionIds = new ArrayList<String>();
        final Set<String> neNames = new HashSet<String>();
        neNames.add("LTE04ERBS00001");
        collectionIds.add("mycollection");
        jobInfo.setcollectionNames(collectionIds);
        final ShmRemoteJobData shmRemoteJobData = new ShmRemoteJobData();
        shmRemoteJobData.setLoggedInUser("administrator");
        shmRemoteJobData.setNeNames(neNames);
        final List<NetworkElement> nesList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElement.setNeType("ERBS");
        nesList.add(networkElement);
        when(networkElementsProvider.getNetworkElementsByNeNames(new ArrayList<String>())).thenReturn(nesList);

        final Map<PlatformTypeEnum, List<String>> returnedMap = supportedPlatformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY,
                shmRemoteJobData);
        assertNotNull(returnedMap);
    }

    @Test
    public void testFindSupportedPlatformAndNodeTypesWithMoreThanOneNeTypes() {
        final ShmRemoteJobData shmRemoteJobData = new ShmRemoteJobData();
        shmRemoteJobData.setLoggedInUser("admin");
        final List<String> collectionIds = new ArrayList<String>();
        final String collectionId = "myCollection";
        collectionIds.add(collectionId);
        final String jobOwner = "admin";
        final Set<String> collectionDTOSet = new HashSet<>(Arrays.asList("NetworkElement=LTE04ERBS00001", "NetworkElement=LTE04ERBS00002"));
        when(topologyEvaluationService.getCollectionInfo(jobOwner, collectionId)).thenReturn(collectionDTOSet);
        supportedPlatformAndNeTypeFinder.evaluateSelectedNes(shmRemoteJobData);
        //Assert.assertEquals(shmRemoteJobData.getNeNames().size(), 2);
    }

    @Test
    public void testFindSupportedPlatformAndNodeTypesWithSavedSearch() throws com.ericsson.oss.services.topologyCollectionsService.exception.TopologyCollectionsServiceException {
        final ShmRemoteJobData shmRemoteJobData = new ShmRemoteJobData();
        shmRemoteJobData.setLoggedInUser("admin");
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearch = "mySavedSearch";
        savedSearchIds.add(savedSearch);

        final String jobOwner = "admin";
        final Set<String> neNames = new HashSet<String>();
        neNames.add("node1");
        shmRemoteJobData.setNeNames(neNames);

        when(topologyEvaluationService.getSavedSearchInfo(jobOwner, savedSearch)).thenReturn(neNames);
        supportedPlatformAndNeTypeFinder.evaluateSelectedNes(shmRemoteJobData);
        supportedPlatformAndNeTypeFinder.populateNeNamesFromSavedSearchId(shmRemoteJobData, savedSearch);
        Assert.assertEquals(shmRemoteJobData.getNeNames().size(), 1);
    }

    @Test(expected = com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException.class)
    public void testPopulateNeNamesFromSavedSearchesThrowsTopologyCollectionsServiceException() {
        final ShmRemoteJobData shmRemoteJobData = new ShmRemoteJobData();
        final String jobOwner = "admin";
        final String savedSearchName = "TestSavedSearch";
        shmRemoteJobData.setLoggedInUser(jobOwner);
        shmRemoteJobData.setSavedSearchId(savedSearchName);
        when(topologyEvaluationService.getSavedSearchPoId(shmRemoteJobData.getSavedSearchId(), shmRemoteJobData.getLoggedInUser())).thenReturn(savedSearchName);
        when(topologyEvaluationService.getSavedSearchInfo(jobOwner, savedSearchName))
                .thenThrow(new RetriableCommandException(new TopologyCollectionsServiceException("Caught TopologyCollectionsServiceException")));
        supportedPlatformAndNeTypeFinder.populateNeNamesFromSavedSearchId(shmRemoteJobData, savedSearchName);
    }

    @Test(expected = com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException.class)
    public void testPopulateNeNamesFromCollectionIdThrowsTopologyCollectionsServiceException() {
        final ShmRemoteJobData shmRemoteJobData = new ShmRemoteJobData();
        final String jobOwner = "admin";
        final String collectionName = "TestCollection";
        shmRemoteJobData.setLoggedInUser(jobOwner);
        shmRemoteJobData.setCollection(collectionName);
        when(topologyEvaluationService.getCollectionPoId(shmRemoteJobData.getCollection(), shmRemoteJobData.getLoggedInUser())).thenReturn(collectionName);
        when(topologyEvaluationService.getCollectionInfo(jobOwner, collectionName))
                .thenThrow(new RetriableCommandException(new TopologyCollectionsServiceException("Caught TopologyCollectionsServiceException")));
        supportedPlatformAndNeTypeFinder.populateNeNamesFromCollectionId(shmRemoteJobData);
    }

}
