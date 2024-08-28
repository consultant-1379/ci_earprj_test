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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.*;

import javax.ejb.SessionContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsIllegalStateException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService;
import com.ericsson.oss.services.topologyCollectionsService.dto.Category;
import com.ericsson.oss.services.topologyCollectionsService.dto.CollectionDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.ManagedObjectDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.SavedSearchDTO;
import com.ericsson.oss.services.topologyCollectionsService.exception.PrivateCollectionException;
import com.ericsson.oss.services.topologyCollectionsService.exception.TopologyCollectionsServiceException;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsIntegrationException;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsNotAvailableException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.CollectionNotFoundException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.OwnedByOtherUserException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.SavedSearchNotFoundException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchQueryException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchServiceException;
import com.ericsson.oss.services.topologySearchService.service.api.SearchExecutor;
import com.ericsson.oss.services.topologySearchService.service.api.TopologySearchService;
import com.ericsson.oss.services.topologySearchService.service.api.dto.NetworkExplorerResponse;


@RunWith(MockitoJUnitRunner.class)
public class TopologyEvaluationServiceManagerTest {

    @InjectMocks
    TopologyEvaluationServiceManager objectUnderTest;

    @Mock
    EAccessControl accessControl;

    @Mock
    TopologyRetryService topologyRetryService;

    @Mock
    SearchExecutor searchExecutor;

    @Mock
    TopologySearchService topologySearchService;

    @Mock
    NetworkExplorerResponse networkExplorerResponse;

    @Mock
    SessionContext ctx;

    @Mock
    Principal callerPrincipal;

    @Mock
    TopologyCollectionsEjbService topologyCollectionsEjbServiceMock;

    @Mock
    DpsReader dpsReader;
    
    @Mock
    DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    List<PersistenceObject> persistenceObjects = null;

    private static final String NAMESPACE = "CPP_NRM_OSS_DEF";
    private static final String TYPE = "HardwareItem";
    private static final String VERSION = "2.1.0";

 

    @Test
    public void testGetCollectionInfo() throws PrivateCollectionException, TopologyCollectionsServiceException {
        final String collectionId = "collection1";
        final String jobOwner = "owner";

        doNothing().when(accessControl).setAuthUserSubject(jobOwner);

        when(ctx.getCallerPrincipal()).thenReturn(callerPrincipal);
        final ManagedObjectDTO managedObject = new ManagedObjectDTO();
        managedObject.setFdn("NetworkElement=LTE04ERBS00001");
        final List<ManagedObjectDTO> elements = new ArrayList<>();
        elements.add(managedObject);
        final CollectionDTO collectionDto = new CollectionDTO();
        collectionDto.setElements(elements);
        when(topologyRetryService.getCollectionInfoWithRetry(collectionId, jobOwner)).thenReturn(collectionDto);

        final Set<String> actualInfo = objectUnderTest.getCollectionInfo(jobOwner, collectionId);
        assertEquals("NetworkElement=LTE04ERBS00001", actualInfo.iterator().next());
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void testGetSavedSearchInfo() throws TopologySearchQueryException, SecurityViolationException, PrivateCollectionException, TopologyCollectionsServiceException,
            TopologySearchServiceException {
        final String savedSearchId = "savedSearch1";
        final String jobOwner = "owner";
        final String searchQuery = "searchQuery";
        final long poId = 1233;
        final String savedSearchAttribute = "savedSearchAttribute";

        doNothing().when(accessControl).setAuthUserSubject(jobOwner);

        final SavedSearchDTO savedSearchResponse = new SavedSearchDTO("name", "administrator", Category.PUBLIC, searchQuery);
        when(topologyRetryService.getSavedSearchInfoWithRetry(savedSearchId, jobOwner)).thenReturn(savedSearchResponse);

        final Collection<CmObject> cmPersistenceObjects = new LinkedList<CmObject>();
        final CmObject cmObject = new CmObject();

        cmObject.setPoId(poId);
        cmObject.setFdn("NetworkElement=LTE04ERBS00001");
        cmPersistenceObjects.add(cmObject);

        final Set<String> attributes = new HashSet<String>();
        attributes.add(savedSearchAttribute);
        when(searchExecutor.search(searchQuery, jobOwner, "")).thenReturn(networkExplorerResponse);
        when(networkExplorerResponse.getCmObjects()).thenReturn(cmPersistenceObjects);

        final List<Long> savedSearchPoIdList = new ArrayList<Long>();
        savedSearchPoIdList.add(poId);
        final Set<String> savedSearchAttrib = new HashSet<String>();
        savedSearchAttrib.add(savedSearchAttribute);

        createPersistenceObjects();
        when(dpsAvailabilityInfoProvider.isDatabaseDown()).thenReturn(false);
        
        when(dpsReader.findPOsByPoIds(savedSearchPoIdList)).thenReturn(persistenceObjects);

        final Set<String> actualResponse = objectUnderTest.getSavedSearchInfo(savedSearchId, jobOwner);
        assertEquals(persistenceObjects.size(), actualResponse.size());
    }

    @Test
    public void testGetCollectionDetailsShouldReturnCollectionDetailsWhenCollectionIdIsGiven() {
        final String collectionId = "123456789";
        final String jobOwner = "User";
        final CollectionDTO collectionDTO = new CollectionDTO();
        collectionDTO.setId("123456789");
        collectionDTO.setName("Collection_1");
        collectionDTO.setCategory(Category.PUBLIC);
        final Set<String> managedObjectsIDs = new HashSet<>();
        managedObjectsIDs.add("12345");
        managedObjectsIDs.add("456123");
        collectionDTO.setManagedObjectsIDs(managedObjectsIDs);

        when(topologyCollectionsEjbServiceMock.getCollectionByID(collectionId, jobOwner)).thenReturn(collectionDTO);
        final CollectionDetails collectionDetails = objectUnderTest.getCollectionDetails(collectionId, jobOwner);

        if (collectionDetails.getCollectionId().equalsIgnoreCase(collectionDTO.getId())) {
            assertEquals(collectionDetails.getCollectionId(), collectionDTO.getId());
            assertEquals(collectionDetails.getName(), collectionDTO.getName());
            assertEquals(collectionDetails.getCategory(), collectionDTO.getCategory().toString());
        }
    }

    @Test
    public void testGetSavedSearchDetailsShouldReturnSavedSearchDetailsWhenSavedSearchIdIsGiven() {
        final String savedSearchId = "12345678912";
        final String jobOwner = "User";

        final SavedSearchDTO savedSearchDTO = new SavedSearchDTO();
        savedSearchDTO.setId("12345678912");
        savedSearchDTO.setName("Saved_Search_1");
        savedSearchDTO.setQuery("NetworkElement where name = LTE01");
        savedSearchDTO.setCategory(Category.PRIVATE);

        when(topologyCollectionsEjbServiceMock.getSavedSearchByID(savedSearchId, jobOwner)).thenReturn(savedSearchDTO);

        final SavedSearchDetails savedSearchDetails = objectUnderTest.getSavedSearchDetails(savedSearchId, jobOwner);
        assertEquals(savedSearchDetails.getSavedSearchId(), savedSearchDTO.getId());
        assertEquals(savedSearchDetails.getName(), savedSearchDTO.getName());
        assertEquals(savedSearchDetails.getQuery(), savedSearchDTO.getQuery());
        assertEquals(savedSearchDetails.getCategory(), savedSearchDTO.getCategory().toString());
    }

    @Test
    public void testGetCollectionDetailsShouldReturnNullWhenCollectionIdDoesnotExists() {
        final String collectionId = "78546215";
        final String user = "User_1";
        doThrow(CollectionNotFoundException.class).when(topologyCollectionsEjbServiceMock).getCollectionByID(collectionId, user);
        final CollectionDetails collectionDetails = objectUnderTest.getCollectionDetails(collectionId, user);
        assertNull(collectionDetails);
    }

    @Test
    public void testGetCollectionDetailsShouldReturnForbiddenMessageWhenUserDoesnotHaveRightsToViewTheCollection() {
        final String collectionId = "78546215123";
        final String user = "User_2";
        doThrow(OwnedByOtherUserException.class).when(topologyCollectionsEjbServiceMock).getCollectionByID(collectionId, user);
        final CollectionDetails collectionDetails = objectUnderTest.getCollectionDetails(collectionId, user);
        assertEquals(collectionDetails.getErrorMessage(), CollectionDetails.FORBIDDEN_ERROR);
    }

    @Test(expected = DpsNotAvailableException.class)
    public void testGetCollectionDetailsShouldThrowDpsNotAvailableExceptionWhenDpsIsNotAvailable() {
        final String collectionId = "7854621512233";
        final String user = "User_3";
        doThrow(DpsNotAvailableException.class).when(topologyCollectionsEjbServiceMock).getCollectionByID(collectionId, user);
        objectUnderTest.getCollectionDetails(collectionId, user);
    }

    @Test(expected = DpsIntegrationException.class)
    public void testGetCollectionDetailsShouldThrowDpsIntegrationExceptionWhenDpsIsNotIntegrated() {
        final String collectionId = "7854621512345";
        final String user = "User_4";
        doThrow(DpsIntegrationException.class).when(topologyCollectionsEjbServiceMock).getCollectionByID(collectionId, user);
        objectUnderTest.getCollectionDetails(collectionId, user);
    }

    @Test
    public void testGetSavedSearchDetailsShouldReturnNullWhenSavedSearchIdDoesnotExists() {
        final String savedSearchId = "78546215";
        final String user = "User_1";
        doThrow(SavedSearchNotFoundException.class).when(topologyCollectionsEjbServiceMock).getSavedSearchByID(savedSearchId, user);
        final SavedSearchDetails savedSearchDetails = objectUnderTest.getSavedSearchDetails(savedSearchId, user);
        assertNull(savedSearchDetails);
    }

    @Test
    public void testGetSavedSearchDetailsShouldReturnForbiddenMessageWhenUserDoesnotHaveRightsToViewTheSavedSearch() {
        final String savedSearchId = "78546215123";
        final String user = "User_2";
        doThrow(OwnedByOtherUserException.class).when(topologyCollectionsEjbServiceMock).getSavedSearchByID(savedSearchId, user);
        final SavedSearchDetails savedSearchDetails = objectUnderTest.getSavedSearchDetails(savedSearchId, user);
        assertEquals(savedSearchDetails.getErrorMessage(), SavedSearchDetails.FORBIDDEN_ERROR);
    }

    @Test(expected = DpsNotAvailableException.class)
    public void testGetSavedSearchDetailsShouldThrowDpsNotAvailableExceptionWhenDpsIsNotAvailable() {
        final String savedSearchId = "7854621512233";
        final String user = "User_3";
        doThrow(DpsNotAvailableException.class).when(topologyCollectionsEjbServiceMock).getSavedSearchByID(savedSearchId, user);
        objectUnderTest.getSavedSearchDetails(savedSearchId, user);
    }

    @Test(expected = DpsIntegrationException.class)
    public void testGetSavedSearchDetailsShouldThrowDpsIntegrationExceptionWhenDpsIsNotIntegrated() {
        final String savedSearchId = "7854621512345";
        final String user = "User_4";
        doThrow(DpsIntegrationException.class).when(topologyCollectionsEjbServiceMock).getSavedSearchByID(savedSearchId, user);
        objectUnderTest.getSavedSearchDetails(savedSearchId, user);
    }

    private void createPersistenceObjects() {
        persistenceObjects = new ArrayList<PersistenceObject>();
        for (int index = 1; index <= 1; index++) {
            final PersistenceObject persistenceObject = new AbstractPersistenceObject(NAMESPACE, TYPE + "-" + index, VERSION, new HashMap<String, Object>()) {

                @Override
                public String getVersion() {
                    return version;
                }

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public String getNamespace() {
                    return namespace;
                }

                @Override
                public Map<String, Object> getAllAttributes() {
                    return attributesMap;
                }

                @Override
                public PersistenceObject getTarget() {

                    return null;
                }

                @Override
                public void setTarget(PersistenceObject arg0) {

                }

                @Override
                public int getAssociatedObjectCount(String endpointName) throws NotDefinedInModelException {
                   
                    return 0;
                }

                @Override
                public Map<String, Object> readAttributesFromDelegate(String... attrsNames) throws DpsIllegalStateException {
                   
                    return null;
                }
            };
            persistenceObjects.add(persistenceObject);
        }
    }

}
