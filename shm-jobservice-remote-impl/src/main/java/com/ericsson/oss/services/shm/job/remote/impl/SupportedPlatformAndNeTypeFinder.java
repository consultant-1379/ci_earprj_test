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
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException;
import com.ericsson.oss.services.shm.common.modelservice.NodeTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

/**
 * Class to find all supported and deployed NE types
 * 
 * @author tcsmaup
 * 
 */
public class SupportedPlatformAndNeTypeFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedPlatformAndNeTypeFinder.class);

    @Inject
    FdnServiceBeanRetryHelper networkElementsProvider;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    TopologyEvaluationService topologyEvaluationService;

    @Inject
    private NodeTypeProviderImpl nodeTypeProviderImpl;

    private static final String NO_CAPABILITY = null;

    /**
     * Returns platformType associated to all supported nodes in ENM.
     * 
     * @return Map of Platform and relevant NE Types
     */
    public Map<PlatformTypeEnum, List<String>> findSupportedPlatformAndNodeTypes(final ShmRemoteJobData shmRemoteJobData) {
        return findSupportedPlatformAndNodeTypes(NO_CAPABILITY, shmRemoteJobData);
    }

    /**
     * Returns platformType associated to all supported nodes in ENM. Functionality based capability to be passed.
     * 
     * @return Map of Platform and relevant NE Types
     */
    public Map<PlatformTypeEnum, List<String>> findSupportedPlatformAndNodeTypes(final String capability, final ShmRemoteJobData shmRemoteJobData) {

        final Set<String> supportedNeTypes = evaluateSelectedNes(capability, shmRemoteJobData);

        return buildPlatformAndItsNodeTypes(capability, supportedNeTypes);
    }

    /**
     * Returns platformType associated to all supported nodes in ENM. Functionality based capability to be passed.
     * 
     * @return Map of Platform and relevant NE Types
     */
    public Map<PlatformTypeEnum, List<String>> findSupportedPlatformAndNodeTypes(final String capability) {

        final Set<String> supportedNeTypes = nodeTypeProviderImpl.getsupportedNeTypes();

        return buildPlatformAndItsNodeTypes(capability, supportedNeTypes);
    }

    /**
     * @param capability
     * @param supportedNeTypes
     */
    private Map<PlatformTypeEnum, List<String>> buildPlatformAndItsNodeTypes(final String capability, final Set<String> supportedNeTypes) {
        final Map<PlatformTypeEnum, List<String>> platformTypeAndNodeTypes = new HashMap<PlatformTypeEnum, List<String>>();
        for (String neType : supportedNeTypes) {
            try {
                final PlatformTypeEnum platformTypeEnum = platformTypeProviderImpl.getPlatformTypeBasedOnCapability(neType, capability);
                if (platformTypeEnum == null) {
                    continue;
                }
                if (platformTypeAndNodeTypes.get(platformTypeEnum) != null) {
                    final List<String> neTypes = platformTypeAndNodeTypes.get(platformTypeEnum);
                    if (neTypes.contains(neType)) {
                        continue;
                    } else {
                        neTypes.add(neType);
                        platformTypeAndNodeTypes.put(platformTypeEnum, neTypes);
                    }
                } else {
                    final List<String> neTypes = new ArrayList<String>();
                    neTypes.add(neType);
                    platformTypeAndNodeTypes.put(platformTypeEnum, neTypes);
                }
            } catch (final UnsupportedPlatformException e) {
                LOGGER.debug("UnsupportedPlatformException is not being re-thrown. This was intentionally swallowed because we don't want to fail the request when one neType fails. Exception is:{}",
                        e);
                continue;

            }
        }
        return platformTypeAndNodeTypes;
    }

    public Set<String> evaluateSelectedNes(final ShmRemoteJobData shmRemoteJobData) {
        return evaluateSelectedNes(NO_CAPABILITY, shmRemoteJobData);
    }

    /**
     * Evaluating all NEs existing in the selected collection/savedsearch
     * 
     * @param shmRemoteJobData
     * @param jobInfo
     * @return
     */
    public Set<String> evaluateSelectedNes(final String capability, final ShmRemoteJobData shmRemoteJobData) {
        final String collectionName = shmRemoteJobData.getCollection();
        final String savedSearchName = shmRemoteJobData.getSavedSearchId();
        List<String> neNamesList = null;
        final Set<String> fdns = shmRemoteJobData.getFdns();
        final Set<String> nodeNameSet = new HashSet<String>();
        if (collectionName != null && !collectionName.isEmpty()) {
            populateNeNamesFromCollectionId(shmRemoteJobData);
        }
        if (savedSearchName != null && !savedSearchName.isEmpty()) {
            populateNeNamesFromSavedSearchId(shmRemoteJobData, savedSearchName);
        }
        if (fdns != null && !fdns.isEmpty()) {
            prepareFinalListOfNEsForFdns(nodeNameSet, fdns);
        }
        if (shmRemoteJobData.getNeNames() != null && !shmRemoteJobData.getNeNames().isEmpty()) {
            nodeNameSet.addAll(shmRemoteJobData.getNeNames());
        }
        if (nodeNameSet != null && !nodeNameSet.isEmpty()) {
            neNamesList = new ArrayList<String>(nodeNameSet);
        }
        List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final Set<String> neTypes = new HashSet<String>();

        if (neNamesList != null && !neNamesList.isEmpty()) {
            networkElementsList = networkElementsProvider.getNetworkElementsByNeNames(neNamesList, capability);
        }
        if (networkElementsList != null && !networkElementsList.isEmpty()) {
            for (final NetworkElement networkElement : networkElementsList) {
                neTypes.add(networkElement.getNeType());
            }
        }
        return neTypes;
    }

    public void populateNeNamesFromCollectionId(final ShmRemoteJobData shmRemoteJobData) {

        Set<String> neFdns = null;
        Set<String> neNames = new HashSet<>();
        try {
            final String collectionId = topologyEvaluationService.getCollectionPoId(shmRemoteJobData.getCollection(), shmRemoteJobData.getLoggedInUser());
            neFdns = topologyEvaluationService.getCollectionInfo(shmRemoteJobData.getLoggedInUser(), collectionId);
            LOGGER.debug("Retreived neFdns {}", neFdns);
        } catch (final TopologyCollectionsServiceException e) {
            throw e;
        } catch (final Exception exception) {
            LOGGER.error("Exception caught while retrieving NEs from collection with message {}", exception);
            if (exception.getCause() instanceof TopologyCollectionsServiceException) {
                throw new TopologyCollectionsServiceException(exception.getCause().getMessage());
            } else if (exception.getCause().getCause() instanceof TopologyCollectionsServiceException) {
                throw new TopologyCollectionsServiceException(exception.getCause().getCause().getMessage());
            }
        }
        if (shmRemoteJobData.getNeNames() != null && !shmRemoteJobData.getNeNames().isEmpty()) {
            neNames = shmRemoteJobData.getNeNames();
        }
        prepareFinalListOfNEsForFdns(neNames, neFdns);

        shmRemoteJobData.setNeNames(neNames);

    }

    // Need to test/agree/accept with others as this has been re-fractored with the newer implementation.
    private static void prepareFinalListOfNEsForFdns(final Set<String> neNames, final Set<String> neFdns) {
        for (final String netWorkElementFdn : neFdns) {
            final String nodeName = FdnUtils.getNodeName(netWorkElementFdn);
            LOGGER.debug("Extracted Fdn {} from Topology Collection service ", netWorkElementFdn);
            if (nodeName != null) {
                neNames.add(nodeName);
            }
        }
    }

    public void populateNeNamesFromSavedSearchId(final ShmRemoteJobData shmRemoteJobData, final String savedSearchName) {
        Set<String> neFdns = null;
        Set<String> neNames = new HashSet<>();
        try {
            final String savedSearchId = topologyEvaluationService.getSavedSearchPoId(savedSearchName, shmRemoteJobData.getLoggedInUser());
            neFdns = topologyEvaluationService.getSavedSearchInfo(shmRemoteJobData.getLoggedInUser(), savedSearchId);
            LOGGER.debug("Retreived neFdns {}", neFdns);
        } catch (final TopologyCollectionsServiceException e) {
            throw e;
        } catch (final Exception exception) {
            LOGGER.error("Exception caught while retrieving NEs from collection with message: {}", exception);
            if (exception.getCause() instanceof TopologyCollectionsServiceException) {
                throw new TopologyCollectionsServiceException(exception.getCause().getMessage());
            } else if (exception.getCause().getCause() instanceof TopologyCollectionsServiceException) {
                throw new TopologyCollectionsServiceException(exception.getCause().getCause().getMessage());
            }
        }
        if (shmRemoteJobData.getNeNames() != null && !shmRemoteJobData.getNeNames().isEmpty()) {
            neNames = shmRemoteJobData.getNeNames();
        }
        prepareFinalListOfNEsForFdns(neNames, neFdns);

        shmRemoteJobData.setNeNames(neNames);
    }

}
