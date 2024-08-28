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
package com.ericsson.oss.services.shm.jobexecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService;
import com.ericsson.oss.services.topologyCollectionsService.dto.CollectionDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.SavedSearchDTO;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsIntegrationException;
import com.ericsson.oss.services.topologyCollectionsService.exception.integration.DpsNotAvailableException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.CollectionNotFoundException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.OwnedByOtherUserException;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.SavedSearchNotFoundException;
import com.ericsson.oss.services.topologyCollectionsService.service.sort.OrderDirection;

/**
 * To provide the method calls to {@link TopologyEvaluationService} with retry mechanism
 * 
 * @author xgeegun
 */
@ApplicationScoped
public class TopologyRetryService {

    private final static Logger logger = LoggerFactory.getLogger(TopologyRetryService.class);

    @EServiceRef
    private TopologyCollectionsEjbService topologyCollectionsEjbService;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    /**
     * @param collectionId
     * @param jobOwner
     * @param attributes
     * @return collectionInfo
     */
    /* Retry method for collection poid based on provided collection Id from topologyCollectionsEjbService */

    public CollectionDTO getCollectionInfoWithRetry(final String collectionId, final String jobOwner) {
        final CollectionDTO collectionInfo = retryManager.executeCommand(dpsRetryPolicies.getTopologyServiceRetryPolicy(), new ShmDpsRetriableCommand<CollectionDTO>() {
            @Override
            public CollectionDTO execute() throws OwnedByOtherUserException, CollectionNotFoundException, DpsNotAvailableException, DpsIntegrationException {
                logger.debug("Request received to fetch the collection from Topology Retry Service .");
                return topologyCollectionsEjbService.getCollectionWithContents(collectionId, jobOwner, null, OrderDirection.ASC, true, null);

            }
        });
        return collectionInfo;

    }

    /**
     * @param savedSearchId
     * @param jobOwner
     * @return SavedSearchResponse
     */
    /* Retry method for saved search poid based on provided saved search Id from topologyCollectionsService */

    public SavedSearchDTO getSavedSearchInfoWithRetry(final String savedSearchId, final String jobOwner) {
        final SavedSearchDTO savedSearchDTO = retryManager.executeCommand(dpsRetryPolicies.getTopologyServiceRetryPolicy(), new ShmDpsRetriableCommand<SavedSearchDTO>() {
            @Override
            public SavedSearchDTO execute() throws OwnedByOtherUserException, SavedSearchNotFoundException, DpsNotAvailableException, DpsIntegrationException {
                logger.debug("Request received to fetch the SavedSearch from Topology Retry Service .");
                return topologyCollectionsEjbService.getSavedSearchByID(savedSearchId, jobOwner);
            }
        });

        return savedSearchDTO;
    }
}