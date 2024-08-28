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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService;
import com.ericsson.oss.services.topologyCollectionsService.dto.CollectionDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.ManagedObjectDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.SavedSearchDTO;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsIntegrationException;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsNotAvailableException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.CollectionNotFoundException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.OwnedByOtherUserException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.SavedSearchNotFoundException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchQueryException;
import com.ericsson.oss.services.topologySearchService.exception.TopologySearchServiceException;
import com.ericsson.oss.services.topologySearchService.service.api.SearchExecutor;
import com.ericsson.oss.services.topologySearchService.service.api.dto.NetworkExplorerResponse;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TopologyEvaluationServiceManager {

    private final static Logger logger = LoggerFactory.getLogger(TopologyEvaluationServiceManager.class);

    // TODO This API use is only for CLI. Cleanup will be plan with improvement :TORF-152188
    @EServiceRef
    private TopologyCollectionsEjbService topologyCollectionsEjbService;

    @EServiceRef
    private SearchExecutor searchExecutor;

    @Inject
    EAccessControl accessControl;

    @Inject
    private TopologyRetryService topologyRetryService;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<String> getCollectionInfo(final String jobOwner, final String collectionId)
            throws OwnedByOtherUserException, CollectionNotFoundException, DpsNotAvailableException, DpsIntegrationException {
        accessControl.setAuthUserSubject(jobOwner);
        final CollectionDTO collectionInfo = topologyRetryService.getCollectionInfoWithRetry(collectionId, jobOwner);
        final List<ManagedObjectDTO> managedObjectDTOList = collectionInfo.getElements();
        return getNeFdnsFromManagedObjectDTO(managedObjectDTOList);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<String> getSavedSearchInfo(final String savedSearchId, final String jobOwner) throws OwnedByOtherUserException, SavedSearchNotFoundException, DpsNotAvailableException,
            DpsIntegrationException, TopologySearchQueryException, SecurityViolationException, TopologySearchServiceException {
        logger.debug("calling Topology Service to fetch NEs in saved searches {}", savedSearchId);
        return getSavedSearchInfoById(savedSearchId, jobOwner);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String getCollectionName(final String jobOwner, final String collectionId) throws OwnedByOtherUserException, CollectionNotFoundException, DpsNotAvailableException, DpsIntegrationException {
        final CollectionDTO collectionInfo = topologyRetryService.getCollectionInfoWithRetry(collectionId, jobOwner);
        return collectionInfo.getName();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String getSavedSearchName(final String savedSearchId, final String jobOwner)
            throws OwnedByOtherUserException, CollectionNotFoundException, DpsNotAvailableException, DpsIntegrationException {
        final SavedSearchDTO savedSearchInfo = topologyRetryService.getSavedSearchInfoWithRetry(savedSearchId, jobOwner);
        return savedSearchInfo.getName();
    }

    /**
     * @param savedSearchId
     * @param jobOwner
     * @return
     * @throws TopologyCollectionsServiceException
     */
    private Set<String> getSavedSearchInfoById(final String savedSearchId, final String jobOwner) {
        NetworkExplorerResponse networkExplorerResponse;
        Set<String> neFdns = new HashSet<String>();
        Collection<CmObject> cmPersistenceObjects = new LinkedList<CmObject>();
        accessControl.setAuthUserSubject(jobOwner);
        final SavedSearchDTO savedSearchDTO = topologyRetryService.getSavedSearchInfoWithRetry(savedSearchId, jobOwner);
        boolean databaseIsDown = false;
        try {
            if (savedSearchDTO != null) {
                final String searchQuery = savedSearchDTO.getQuery();
                final String userId = jobOwner;
                final String orderBy = "";
                networkExplorerResponse = searchExecutor.search(searchQuery, userId, orderBy);
                cmPersistenceObjects = networkExplorerResponse.getCmObjects();
                final List<Long> savedSearchPoIdList = new ArrayList<Long>();
                for (final CmObject cmObject : cmPersistenceObjects) {
                    savedSearchPoIdList.add(cmObject.getPoId());

                }

                databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
                if (!databaseIsDown) {
                    final List<PersistenceObject> persistenceObjects = dpsReader.findPOsByPoIds(savedSearchPoIdList);

                    if (persistenceObjects != null) {
                        for (final PersistenceObject persistenceObject : persistenceObjects) {
                            final CmObject cmObject = populateCmObject(persistenceObject);
                            cmPersistenceObjects.add(cmObject);
                        }
                    }
                    neFdns = getNeFdnsFromcmPersistenceObject(cmPersistenceObjects);
                }

            }

        } catch (final Exception ex) {

            logger.error("Exception occured while retrieving NodeNames for savedsearch Id:{}. Exception is: {}.", savedSearchId, ex);

        }
        return neFdns;
    }

    /**
     * fetch collection poid based on provided collection name
     * 
     * @param collectionName
     * @param userId
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String getCollectionPoId(final String collectionName, final String userId) {
        final Collection<CollectionDTO> collectionDTOs = topologyCollectionsEjbService.getCollectionsByName(userId, collectionName);
        if (collectionDTOs == null) {
            return null;
        }
        final Iterator<CollectionDTO> collectionIterator = collectionDTOs.iterator();
        while (collectionIterator.hasNext()) {
            final CollectionDTO collection = collectionIterator.next();
            if (collection.getName().equals(collectionName)) {
                return collection.getId();
            }
        }
        logger.debug("Could not evaluate collection: {}", collectionName);
        return null;
    }

    /**
     * fetch collection poid based on provided collection name
     * 
     * @param collectionName
     * @param userId
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CollectionDTO getCollectionByName(final String collectionName, final String userId) {
        final Collection<CollectionDTO> collectionDTOs = topologyCollectionsEjbService.getCollectionsByName(userId, collectionName);
        if (collectionDTOs == null) {
            return null;
        }
        final Iterator<CollectionDTO> collectionIterator = collectionDTOs.iterator();
        while (collectionIterator.hasNext()) {
            final CollectionDTO collection = collectionIterator.next();
            if (collection.getName().equals(collectionName)) {
                return collection;
            }
        }
        logger.debug("Could not evaluate collection: {}", collectionName);
        return null;
    }

    /**
     * Fetch saved search poid based on provided saved search name.
     * 
     * @param savedSearchName
     * @param userId
     * @return
     * 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String getSavedSearchPoId(final String savedSearchName, final String userId) {
        final Collection<SavedSearchDTO> savedSearchDTOs = topologyCollectionsEjbService.getSavedSearchesByName(userId, savedSearchName);
        final Iterator<SavedSearchDTO> savedSearchIterator = savedSearchDTOs.iterator();
        while (savedSearchIterator.hasNext()) {
            final SavedSearchDTO savedSearch = savedSearchIterator.next();
            if (savedSearch.getName().equals(savedSearchName)) {
                return savedSearch.getId();
            }
        }
        logger.debug("Could not evaluate the saved search {}", savedSearchName);
        return null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CollectionDetails getCollectionDetails(final String collectionId, final String jobOwner) throws DpsNotAvailableException {
        CollectionDetails collectionDetail = new CollectionDetails();
        if (StringUtils.isNotEmpty(collectionId)) {
            try {
                final CollectionDTO collectionDTO = topologyCollectionsEjbService.getCollectionByID(collectionId, jobOwner);
                prepareCollectionDetail(collectionDTO, collectionDetail);
            } catch (final CollectionNotFoundException exception) {
                logger.error("collection not found for the requested collection id: {}  with exception: {}", collectionId, exception);
                collectionDetail = null;
            } catch (final OwnedByOtherUserException exception) {
                logger.error("The user doesn't  have rights to access the collection {}", exception);
                collectionDetail.setErrorMessage(CollectionDetails.FORBIDDEN_ERROR);
            } catch (final DpsIntegrationException exception) {
                logger.error("DPS exception while retrieving the collection data", exception);
                throw exception;
            }
        }
        return collectionDetail;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public SavedSearchDetails getSavedSearchDetails(final String savedSearchId, final String jobOwner) throws DpsNotAvailableException {
        SavedSearchDetails savedSearchDetail = new SavedSearchDetails();
        if (StringUtils.isNotEmpty(savedSearchId)) {
            try {
                final SavedSearchDTO savedSearchDTO = topologyCollectionsEjbService.getSavedSearchByID(savedSearchId, jobOwner);
                prepareSavedSearchDetail(savedSearchDTO, savedSearchDetail);
            } catch (final SavedSearchNotFoundException exception) {
                logger.error("Saved Search not found for the requested Saved Search id: {} with exception: {}", savedSearchId, exception);
                savedSearchDetail = null;
            } catch (final OwnedByOtherUserException exception) {
                logger.error("The user doesn't  have rights to access the Saved Search {}", exception);
                savedSearchDetail.setErrorMessage(SavedSearchDetails.FORBIDDEN_ERROR);
            } catch (final DpsIntegrationException exception) {
                logger.error("DPS exception while retrieving the saved search data", exception);
                throw exception;
            }
        }
        return savedSearchDetail;
    }

    private void prepareCollectionDetail(final CollectionDTO collectionDTO, final CollectionDetails collectionDetail) {
        final Set<String> managedObejectIds = collectionDTO.getManagedObjectsIDs();
        final String collectionName = collectionDTO.getName();
        final String collectionId = collectionDTO.getId();
        final String category = collectionDTO.getCategory().toString();
        collectionDetail.setCollectionId(collectionId);
        collectionDetail.setManagedObjectsIDs(managedObejectIds);
        collectionDetail.setName(collectionName);
        collectionDetail.setCategory(category);
    }

    private void prepareSavedSearchDetail(final SavedSearchDTO savedSearchDTO, final SavedSearchDetails savedSearchDetail) {
        final String savedSearchName = savedSearchDTO.getName();
        final String query = savedSearchDTO.getQuery();
        final String savedSearchId = savedSearchDTO.getId();
        final String category = savedSearchDTO.getCategory().toString();
        savedSearchDetail.setSavedSearchId(savedSearchId);
        savedSearchDetail.setName(savedSearchName);
        savedSearchDetail.setQuery(query);
        savedSearchDetail.setCategory(category);
    }

    private Set<String> getNeFdnsFromManagedObjectDTO(final List<ManagedObjectDTO> managedObjectDTOList) {
        final Set<String> neFdns = new HashSet<String>();
        if (managedObjectDTOList != null && !managedObjectDTOList.isEmpty()) {
            for (final ManagedObjectDTO managedObjectDTO : managedObjectDTOList) {
                logger.debug("Node FDN from managedObjectDTO {}", managedObjectDTO.getFdn());
                if (managedObjectDTO.getFdn() != null) {
                    neFdns.add(managedObjectDTO.getFdn());
                }
            }
        }
        return neFdns;
    }

    private Set<String> getNeFdnsFromcmPersistenceObject(final Collection<CmObject> cmPersistenceObjects) {
        final Set<String> neFdns = new HashSet<String>();
        if (cmPersistenceObjects != null) {
            for (final CmObject cmObject : cmPersistenceObjects) {
                logger.debug("Node FDN from cmObject {}", cmObject.getFdn());
                if (cmObject.getFdn() != null) {
                    neFdns.add(cmObject.getFdn());
                }
            }
        }
        return neFdns;
    }

    /**
     * Populates metadata in CmObject
     * 
     * @param persistenceObject
     * @return cmObject
     */
    private CmObject populateCmObject(final PersistenceObject persistenceObject) {
        final CmObject cmObject = new CmObject();
        cmObject.setPoId(persistenceObject.getPoId());
        cmObject.setType(persistenceObject.getType());
        cmObject.setNamespace(persistenceObject.getNamespace());
        cmObject.setNamespaceVersion(persistenceObject.getVersion());
        if (persistenceObject instanceof ManagedObject) {
            final ManagedObject managedObject = (ManagedObject) persistenceObject;
            cmObject.setFdn(managedObject.getFdn());
            logger.debug("Inside preppoulated cmobject{}", managedObject.getFdn());
            cmObject.setName(managedObject.getName());
        }
        return cmObject;
    }
}
