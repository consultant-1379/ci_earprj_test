package com.ericsson.oss.services.shm.topologyservice;

import java.util.Set;

import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;

public interface TopologyEvaluationService {

    /**
     * fetch saved search poid based on provided saved search name
     * 
     * @param savedSearchName
     * @param userId
     * @return
     * 
     */
    String getSavedSearchPoId(final String savedSearchName, final String userId);

    /**
     * fetch collection poid based on provided collection name
     * 
     * @param collectionName
     * @param userId
     * @return
     */
    String getCollectionPoId(final String collectionName, final String userId);

    /**
     * 
     * @param jobOwner
     * @param collectionId
     * @return
     */
    Set<String> getCollectionInfo(final String jobOwner, final String collectionId);

    /**
     * 
     * @param savedSearchId
     * @param jobOwner
     * @return
     */
    Set<String> getSavedSearchInfo(final String jobOwner, final String savedSearchId);

    /**
     * Get collection details if given user had right access on the given collectionId
     * 
     * @param collectionId
     * @param jobOwner
     * @return
     * @throws DpsNotAvailableException
     */
    CollectionDetails getCollectionDetails(final String collectionId, final String jobOwner);

    /**
     * Get saved search details if given user had right access on the given savedSearchId
     * 
     * @param savedSearchId
     * @param jobOwner
     * @return
     * @throws DpsNotAvailableException
     */
    SavedSearchDetails getSavedSearchDetails(final String savedSearchId, final String jobOwner);

}
