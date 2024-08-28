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
package com.ericsson.oss.services.shm.jobexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobExecutionIndexAndState;
import com.ericsson.oss.services.topologyCollectionsService.exception.PrivateCollectionException;
import com.ericsson.oss.services.topologyCollectionsService.exception.TopologyCollectionsServiceException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.OwnedByOtherUserException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchQueryException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchServiceException;

@RunWith(MockitoJUnitRunner.class)
public class JobExecutorServiceHelperTest {

    @InjectMocks
    JobExecutorServiceHelper jobExecutorServiceHelperMock;

    @Inject
    @Mock
    DpsReader dpsReaderMock;

    @Inject
    @Mock
    private DpsWriter dpsWriter;

    @Mock
    Map<String, Object> mapMock;

    @Inject
    @Mock
    JobExecutionIndexAndState indexAndStateMock;

    @Mock
    PersistenceObject persistenceObject;

    @Mock
    List<PersistenceObject> persistenceObjectList;

    @Mock
    TopologyEvaluationServiceManager topologyEvaluationServiceManager;

    @Mock
    JobUpdateService jobUpdateService;

    @Test
    public void testGetLatestJobExecutionIndexAndStateWhenListIsEmpty() {
        final long jobTemplateId = 1234L;
        final Map<String, Object> attributeRestrictionMap = new HashMap<String, Object>();
        final List<JobExecutionIndexAndState> jobExecutionIndexAndStates = new ArrayList<JobExecutionIndexAndState>();
        attributeRestrictionMap.put(ShmConstants.JOB_TEMPLATE_ID, jobTemplateId);
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(ShmConstants.EXECUTIONINDEX, 123);
        when(persistenceObject.getAllAttributes()).thenReturn(poAttributes);
        when(persistenceObject.getPoId()).thenReturn(123L);
        when(dpsReaderMock.findPOs(Matchers.any(String.class), Matchers.any(String.class), Matchers.any(Map.class))).thenReturn(Arrays.asList(persistenceObject));

        jobExecutionIndexAndStates.add(indexAndStateMock);
        final JobExecutionIndexAndState response = jobExecutorServiceHelperMock.getLatestJobExecutionIndexAndState(jobTemplateId);
        Assert.assertNotNull(response);
        Assert.assertEquals(123, response.getJobExecutionIndex());

    }

    @Test
    public void testcreatePO() throws OwnedByOtherUserException {
        when(dpsWriter.createPO("namespace", "type", "version", mapMock)).thenReturn(persistenceObject);
        final Map<String, Object> response = jobExecutorServiceHelperMock.createPO("namespace", "type", "version", mapMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
    }

    @Test
    public void testPopulateNeNamesFromCollections() throws OwnedByOtherUserException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> collectionIds = new ArrayList<String>();
        final String collectionId = "collection1";
        collectionIds.add(collectionId);
        final Set<String> collectionInfo = new HashSet<String>();
        collectionInfo.add("NetworkElement=LTE04ERBS00001");
        final String jobOwner = "owner";
        when(topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId)).thenReturn(collectionInfo);
        assertTrue(jobExecutorServiceHelperMock.populateNeNamesFromCollections(mainJobId, neNames, topologyJobLogList, collectionIds, jobOwner));
    }

    @Test
    public void testPopulateNeNamesFromCollectionsFDN() {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> collectionIds = new ArrayList<String>();
        final String collectionId = "collection1";
        collectionIds.add(collectionId);
        final Set<String> collectionInfo = new HashSet<String>();
        collectionInfo.add("NetworkElement=LTE04ERBS00002");
        collectionInfo.add("MeContext=LTE04ERBS00001,ManagedElement=1");
        collectionInfo.add("SubNetwork=SGSN-16A-CP01-V101,MeContext=SGSN-16A-CP01-V101,ManagedElement=SGSN-16A-CP01-V101");

        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId)).thenReturn(collectionInfo);

        assertTrue(jobExecutorServiceHelperMock.populateNeNamesFromCollections(mainJobId, neNames, topologyJobLogList, collectionIds, jobOwner));
        assertEquals(3, neNames.size());
        assertTrue(neNames.contains("SGSN-16A-CP01-V101"));
        assertTrue(neNames.contains("LTE04ERBS00001"));
        assertTrue(neNames.contains("LTE04ERBS00002"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromCollectionsThrowsPrivateCollectionException() throws OwnedByOtherUserException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> collectionIds = new ArrayList<String>();
        final String collectionId = "collection1";
        collectionIds.add(collectionId);
        final Map<String, Object> collectionInfo = new HashMap<String, Object>();
        final List<CmObject> cmObjects = new ArrayList<CmObject>();
        final CmObject cmObject = new CmObject();
        cmObject.setName("node1");
        cmObjects.add(cmObject);
        collectionInfo.put(ShmConstants.PO_LIST, cmObjects);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId)).thenThrow(OwnedByOtherUserException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromCollections(mainJobId, neNames, topologyJobLogList, collectionIds, jobOwner));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromCollectionsThrowsTopologyCollectionsServiceException() throws OwnedByOtherUserException, TopologyCollectionsServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> collectionIds = new ArrayList<String>();
        final String collectionId = "collection1";
        collectionIds.add(collectionId);
        final Map<String, Object> collectionInfo = new HashMap<String, Object>();
        final List<CmObject> cmObjects = new ArrayList<CmObject>();
        final CmObject cmObject = new CmObject();
        cmObject.setName("node1");
        cmObjects.add(cmObject);
        collectionInfo.put(ShmConstants.PO_LIST, cmObjects);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId)).thenThrow(TopologyCollectionsServiceException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromCollections(mainJobId, neNames, topologyJobLogList, collectionIds, jobOwner));
    }

    @Test
    public void testPopulateNeNamesFromSavedSearches()
            throws OwnedByOtherUserException, TopologyCollectionsServiceException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearchId = "savedSearch1";
        savedSearchIds.add(savedSearchId);
        final String jobOwner = "owner";

        final Set<String> cmPersistenceObjects = new HashSet<String>();
        cmPersistenceObjects.add("SubNetwork=SGSN-16A-CP01-V101,MeContext=SGSN-16A-CP01-V101,ManagedElement=SGSN-16A-CP01-V101");
        cmPersistenceObjects.add("MeContext=LTE100ERBS00004,ManagedElement=1");
        cmPersistenceObjects.add("MeContext=LTE100ERBS00004,ManagedElement=1"); //duplicate node CmObject        
        when(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner)).thenReturn(cmPersistenceObjects);

        assertTrue(jobExecutorServiceHelperMock.populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner));
        assertEquals(2, neNames.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromSavedSearchesThrowsPrivateCollectionException()
            throws OwnedByOtherUserException, TopologyCollectionsServiceException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearchId = "savedSearch1";
        savedSearchIds.add(savedSearchId);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner)).thenThrow(PrivateCollectionException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromSavedSearchesThrowsTopologyCollectionsServiceException()
            throws OwnedByOtherUserException, TopologyCollectionsServiceException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearchId = "savedSearch1";
        savedSearchIds.add(savedSearchId);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner)).thenThrow(TopologyCollectionsServiceException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromSavedSearchesThrowsTopologySearchQueryException()
            throws OwnedByOtherUserException, TopologyCollectionsServiceException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearchId = "savedSearch1";
        savedSearchIds.add(savedSearchId);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner)).thenThrow(TopologySearchQueryException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromSavedSearchesThrowsSecurityViolationException()
            throws OwnedByOtherUserException, TopologyCollectionsServiceException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearchId = "savedSearch1";
        savedSearchIds.add(savedSearchId);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner)).thenThrow(SecurityViolationException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateNeNamesFromSavedSearchesThrowsTopologySearchServiceException()
            throws OwnedByOtherUserException, TopologyCollectionsServiceException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        final long mainJobId = 123;
        final List<String> neNames = new ArrayList<String>();
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        final List<String> savedSearchIds = new ArrayList<String>();
        final String savedSearchId = "savedSearch1";
        savedSearchIds.add(savedSearchId);
        final String jobOwner = "owner";

        when(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner)).thenThrow(TopologySearchServiceException.class);

        assertFalse(jobExecutorServiceHelperMock.populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner));
    }

    @Test
    public void testGetFilteredUnSupportedNodes() {
        final NetworkElement networkElement1 = new NetworkElement();
        networkElement1.setName("supportedNode1");
        networkElement1.setNeType("ERBS");

        final NetworkElement networkElement2 = new NetworkElement();
        networkElement2.setName("supportedNode2");

        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        unSupportedNodes.put(networkElement1, "Supported Node");
        unSupportedNodes.put(networkElement2, "Supported Node");

        final List<NetworkElement> unAuthorizedNodes = new ArrayList<>();
        unAuthorizedNodes.add(networkElement1);
        final Map<NetworkElement, String> filteredUnSupportedNodes = jobExecutorServiceHelperMock.getFilteredUnSupportedNodes(unSupportedNodes, unAuthorizedNodes);

        assertEquals(1, filteredUnSupportedNodes.size());
        assertEquals("Supported Node", filteredUnSupportedNodes.get(networkElement2));
    }

    @Test
    public void testGetFilteredUnSupportedNodesWithEmptyUnAuthorized() {
        final NetworkElement networkElement1 = new NetworkElement();
        networkElement1.setName("supportedNode1");
        networkElement1.setNeType("ERBS");

        final NetworkElement networkElement2 = new NetworkElement();
        networkElement2.setName("supportedNode2");

        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        unSupportedNodes.put(networkElement1, "Supported Node");
        unSupportedNodes.put(networkElement2, "Supported Node");

        final List<NetworkElement> unAuthorizedNodes = new ArrayList<>();
        final Map<NetworkElement, String> filteredUnSupportedNodes = jobExecutorServiceHelperMock.getFilteredUnSupportedNodes(unSupportedNodes, unAuthorizedNodes);

        assertEquals(2, filteredUnSupportedNodes.size());
        assertEquals("Supported Node", filteredUnSupportedNodes.get(networkElement2));
    }

    @Test
    public void testGetFilteredSupportedNodes() {
        final NetworkElement networkElement1 = new NetworkElement();
        networkElement1.setName("unAuthorizedNode1");
        networkElement1.setNeType("ERBS");

        final NetworkElement networkElement2 = new NetworkElement();
        networkElement2.setName("supportedNode2");

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        supportedNodes.add(networkElement1);
        supportedNodes.add(networkElement2);

        final List<NetworkElement> unAuthorizedNodes = new ArrayList<>();
        unAuthorizedNodes.add(networkElement1);
        final List<NetworkElement> filteredSupportedNodes = jobExecutorServiceHelperMock.getFilteredSupportedNodes(supportedNodes, unAuthorizedNodes);

        assertEquals(1, filteredSupportedNodes.size());
        assertEquals("supportedNode2", filteredSupportedNodes.get(0).getName());
    }

    @Test
    public void testGetFilteredSupportedNodesWithEmptyUnauthorizedNode() {
        final NetworkElement networkElement1 = new NetworkElement();
        networkElement1.setName("supportedNode1");
        networkElement1.setNeType("ERBS");

        final NetworkElement networkElement2 = new NetworkElement();
        networkElement2.setName("supportedNode2");

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        supportedNodes.add(networkElement1);
        supportedNodes.add(networkElement2);

        final List<NetworkElement> unAuthorizedNodes = new ArrayList<>();
        final List<NetworkElement> filteredSupportedNodes = jobExecutorServiceHelperMock.getFilteredSupportedNodes(supportedNodes, unAuthorizedNodes);

        assertEquals(2, filteredSupportedNodes.size());
        assertEquals("supportedNode1", filteredSupportedNodes.get(0).getName());
    }
}
