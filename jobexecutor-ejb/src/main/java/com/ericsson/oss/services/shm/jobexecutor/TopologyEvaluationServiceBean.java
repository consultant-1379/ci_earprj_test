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

import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsNotAvailableException;

/**
 * 
 * @author xnitpar
 * 
 */
/**
 * 
 * Fetch PoID/Info based on collections/SavedSearch
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TopologyEvaluationServiceBean implements TopologyEvaluationService {

    @Inject
    TopologyEvaluationServiceManager topologyEvaluationServiceManager;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String getSavedSearchPoId(final String savedSearchName, final String userId) {
        return topologyEvaluationServiceManager.getSavedSearchPoId(savedSearchName, userId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String getCollectionPoId(final String collectionName, final String userId) {
        return topologyEvaluationServiceManager.getCollectionPoId(collectionName, userId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<String> getCollectionInfo(final String jobOwner, final String collectionId) {
        return topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<String> getSavedSearchInfo(final String jobOwner, final String savedSearchId) {
        return topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner);
    }

    @Override
    public CollectionDetails getCollectionDetails(final String collectionId, final String jobOwner) {
        try {
            return topologyEvaluationServiceManager.getCollectionDetails(collectionId, jobOwner);
        } catch (DpsNotAvailableException dpsNotAvailableException) {
            throw new DatabaseNotAvailableException(ShmCommonConstants.DATABASE_SERVICE_NOT_AVAILABE, dpsNotAvailableException);
        }
    }

    @Override
    public SavedSearchDetails getSavedSearchDetails(final String savedSearchId, final String jobOwner) {
        try {
            return topologyEvaluationServiceManager.getSavedSearchDetails(savedSearchId, jobOwner);
        } catch (DpsNotAvailableException dpsNotAvailableException) {
            throw new DatabaseNotAvailableException(ShmCommonConstants.DATABASE_SERVICE_NOT_AVAILABE, dpsNotAvailableException);
        }
    }
}
