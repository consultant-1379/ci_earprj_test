/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.upgrade.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.exception.DataPersistenceServiceException;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.axe.upgrade.AxeUpgradeActivityConstants;
import com.ericsson.oss.services.shm.inventory.remote.axe.node.topology.api.AxeNodeTopologyRemoteService;
import com.ericsson.oss.services.shm.job.axe.cache.AxeNeUpgradeCacheData;
import com.ericsson.oss.services.shm.job.axe.cache.AxeUpgradeJobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.topology.axe.rest.api.AxeNodeTopology;
import com.ericsson.oss.services.shm.topology.axe.rest.api.AxeNodeTopologyResponse;
import com.ericsson.oss.services.shm.topology.axe.rest.api.NodesWithoutComponents;

/**
 * This class provides Axe NE cache data used in additional parameters to send to OPS for script execution. This cache contains each AXE netype against all its node names with their cpfunction names
 * and number of Apgs
 * 
 * @author xaniama
 */
@Singleton
public class NeJobsStaticDataPerNeTypeProviderImpl {

    @Inject
    private AxeUpgradeJobStaticDataProvider axeUpgradeJobStaticDataProvider;

    @Inject
    private AxeActivityNodeFunctionProvider axeActivityNodeFunctionProvider;

    @Inject
    private ActivityUtils activityUtils;

    @EServiceRef
    AxeNodeTopologyRemoteService axeNodeTopologyRemoteService;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeJobsStaticDataPerNeTypeProviderImpl.class);

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 60000)
    public Map<String, Map<String, AxeNeUpgradeCacheData>> getNeJobsPerNeTypeData(final long mainJobId, final String neType, final long neJobId) throws AxeNodeTopologyDataNotFoundException {
        Map<String, Map<String, AxeNeUpgradeCacheData>> cacheData = axeUpgradeJobStaticDataProvider.getAxeNeUpgradeCacheData(mainJobId);
        LOGGER.debug("cacheData for mainJobId {} and netype {} are {} ", mainJobId, neType, cacheData);
        if (cacheData == null || cacheData.get(neType) == null) {
            cacheData = prepareCacheDataFromDps(mainJobId, neType, neJobId);
        }
        return cacheData;
    }

    /**
     * @param mainJobId
     * @param neType
     * @return
     * @throws AxeNodeTopologyDataNotFoundException
     */
    private Map<String, Map<String, AxeNeUpgradeCacheData>> prepareCacheDataFromDps(final long mainJobId, final String neType, final long neJobId) throws AxeNodeTopologyDataNotFoundException {
        final String supportedNes = getSupportedNes(neJobId);
        final Map<String, String> neAndParentNeNamesMapFromNeJobs = prepareNeWithParentNameDetails(supportedNes);
        final Map<String, Map<String, AxeNeUpgradeCacheData>> cachedNeJobDataPerNetype = new HashMap<>();
        final Map<String, AxeNeUpgradeCacheData> neNameWithAxeUpgradeJobStaticData = new HashMap<>();
        final Set<String> allDistinctMeNamesOfOneNeTpe = buildDistinctParentNeNames(neAndParentNeNamesMapFromNeJobs);
        final Map<String, Set<String>> mapOfNeTypeToNenames = new HashMap<>();
        mapOfNeTypeToNenames.put(neType, allDistinctMeNamesOfOneNeTpe);
        final Map<String, AxeNodeTopologyData> nodeNameToAxeNodeTopology = getAxeNodeTopology(mapOfNeTypeToNenames);
        for (final Entry<String, String> neAndParentNamesEntry : neAndParentNeNamesMapFromNeJobs.entrySet()) {
            buildAxeUpgradeNeData(neType, neNameWithAxeUpgradeJobStaticData, nodeNameToAxeNodeTopology, neAndParentNamesEntry);
        }
        cachedNeJobDataPerNetype.put(neType, neNameWithAxeUpgradeJobStaticData);
        LOGGER.info("cachedNeJobDataPerNetype {}", cachedNeJobDataPerNetype);
        axeUpgradeJobStaticDataProvider.put(mainJobId, cachedNeJobDataPerNetype);
        return cachedNeJobDataPerNetype;

    }

    /**
     * @param neType
     * @param neNameWithAxeUpgradeJobStaticData
     * @param nodeNameToAxeNodeTopology
     * @param neAndParentNamesEntry
     */
    private void buildAxeUpgradeNeData(final String neType, final Map<String, AxeNeUpgradeCacheData> neNameWithAxeUpgradeJobStaticData,
            final Map<String, AxeNodeTopologyData> nodeNameToAxeNodeTopology, final Entry<String, String> neAndParentNamesEntry) {
        final String parentName = neAndParentNamesEntry.getValue() != null ? neAndParentNamesEntry.getValue() : neAndParentNamesEntry.getKey();
        final String neName = neAndParentNamesEntry.getKey();
        LOGGER.info("preparing data for neJobNodename  {}  with parent as  {}", neName, parentName);
        final AxeNodeTopologyData axeNodeTopologyData = nodeNameToAxeNodeTopology.get(parentName);
        String numberOfApgs = null;
        if (axeNodeTopologyData != null) {
            numberOfApgs = axeNodeTopologyData.getNumberOfAPG();
            LOGGER.info("got numberOfApgs as {} from DPS for node {}  ", numberOfApgs, neName);
            LOGGER.info("axeNodeTopologyData {} for node {} is ", axeNodeTopologyData, neName);
            final String cpFunctionName = prepareCpNameAndCpFunctions(neType, neAndParentNamesEntry, parentName, neName, axeNodeTopologyData);
            LOGGER.info("cpFunctionName {} and numberOfApgs {} for neJobNodeName {} are ", cpFunctionName, numberOfApgs, neName);
            final AxeNeUpgradeCacheData axeNedata = new AxeNeUpgradeCacheData(cpFunctionName, numberOfApgs, parentName);
            LOGGER.info("axeNedata {} for node {} is ", axeNedata, neName);
            neNameWithAxeUpgradeJobStaticData.put(neName, axeNedata);
        }
    }

    /**
     * @param neType
     * @param neAndParentNamesEntry
     * @param parentName
     * @param neName
     * @param axeNodeTopologyData
     * @param cpFunctionName
     * @return
     */
    private String prepareCpNameAndCpFunctions(final String neType, final Entry<String, String> neAndParentNamesEntry, final String parentName, final String neName,
            final AxeNodeTopologyData axeNodeTopologyData) {
        String cpFunctionName = null;
        if (neAndParentNamesEntry.getValue() != null && neName.contains(AxeUpgradeActivityConstants.NODENAME_COMPONENT_SEPARATOR)) {
            final String cpName = neName.substring(parentName.length() + 2);
            final String nodeFunction = axeActivityNodeFunctionProvider.getNodeFunctionBasedOnNeType(neType);
            LOGGER.debug("got nodeFunction {}  for neType {}", nodeFunction, neType);
            if (axeNodeTopologyData != null && axeNodeTopologyData.getComponents() != null) {
                cpFunctionName = getCpFunctionName(axeNodeTopologyData, cpFunctionName, cpName, nodeFunction);
            }
        } else {
            LOGGER.info("neJobNodeName.getValue() is {}", neAndParentNamesEntry.getValue());
            cpFunctionName = AxeUpgradeActivityConstants.NO_BLADE;
        }
        return cpFunctionName;
    }

    /**
     * @param axeNodeTopologyData
     * @param cpFunctionName
     * @param cpName
     * @param nodeFunction
     * @return
     */
    private String getCpFunctionName(final AxeNodeTopologyData axeNodeTopologyData, String cpFunctionName, final String cpName, final String nodeFunction) {
        for (final Entry<String, List<String>> allComponents : axeNodeTopologyData.getComponents().entrySet()) {
            if (allComponents.getValue().contains(cpName)) {
                if (allComponents.getKey().equals(AxeUpgradeActivityConstants.SPX)) {
                    cpFunctionName = AxeUpgradeActivityConstants.SPXFUNCTION;
                } else {
                    cpFunctionName = nodeFunction.equals(AxeUpgradeActivityConstants.MSC) ? AxeUpgradeActivityConstants.MSCSERVERFUNCTION : AxeUpgradeActivityConstants.HLRSERVERFUNCTION;
                }
            }
        }
        return cpFunctionName;
    }

    /**
     * @param neType
     * @param neAndParentNeNamesMapFromNeJobs
     * @return
     */
    private Set<String> buildDistinctParentNeNames(final Map<String, String> neAndParentNeNamesMapFromNeJobs) {
        final Set<String> allDistinctMeNamesOfOneNeTpe = new HashSet<>();
        if (neAndParentNeNamesMapFromNeJobs != null) {
            for (final Entry<String, String> neNameInNeJob : neAndParentNeNamesMapFromNeJobs.entrySet()) {
                final String meName = neNameInNeJob.getValue() != null ? neNameInNeJob.getValue() : neNameInNeJob.getKey();
                allDistinctMeNamesOfOneNeTpe.add(meName);
            }
        }
        return allDistinctMeNamesOfOneNeTpe;
    }

    /**
     * @param mainJobId
     * @param neType
     * @return
     */

    public String getSupportedNes(final long neJobId) {
        String nodeNames = null;
        try {
            final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
            final List<Map<String, String>> neJobProperties = (List<Map<String, String>>) neJobAttributes.get(ShmConstants.JOBPROPERTIES);
            if (neJobProperties != null) {
                for (final Map<String, String> neJobProperty : neJobProperties) {
                    if (ShmConstants.AXE_NES.equals(neJobProperty.get(ShmConstants.KEY))) {
                        nodeNames = neJobProperty.get(ShmConstants.VALUE);
                        break;
                    }
                }
            }
        } catch (final DataPersistenceServiceException e) {
            LOGGER.error("Exception occurred while retrieving NE Job attributes {} ", neJobId, e);
        }
        LOGGER.info("Supported nodeNames {} ", nodeNames);
        return nodeNames;

    }

    /**
     * @param neWithParentDetails
     * @param projectionQueryResult
     */
    private Map<String, String> prepareNeWithParentNameDetails(final String nodeNames) {
        final Map<String, String> neWithParentDetails = new HashMap<>();
        final String[] supportedNes = nodeNames.split(",");
        for (String supportedNe : supportedNes) {
            if (supportedNe.contains(ShmConstants.AXE_NODENAME_DELIMITER)) {

                final String[] neWithParentName = supportedNe.split(ShmConstants.AXE_NODENAME_DELIMITER);
                final String nodeName = neWithParentName[0];
                final String parentName = neWithParentName[1];
                neWithParentDetails.put(nodeName, parentName);
            } else {
                neWithParentDetails.put(supportedNe, null);
            }
        }
        return neWithParentDetails;
    }

    /**
     * Return a map of node to its AxeNodeTopologyDto if the node entry is present in DB.
     * 
     * @param mapOfNeTypeToNenames
     *            - map of neType to Node Names.
     * @return
     * @return Map of node to its AxeNodeTopologyDto
     * @throws AxeNodeTopologyDataNotFoundException
     */
    public Map<String, AxeNodeTopologyData> getAxeNodeTopology(final Map<String, Set<String>> mapOfNeTypeToNenames) throws AxeNodeTopologyDataNotFoundException {
        AxeNodeTopologyResponse axeNodeTopologyResponse = null;
        final Map<String, AxeNodeTopologyData> nodeNameToAxeNodeTopology = new HashMap<>();
        try {
            axeNodeTopologyResponse = axeNodeTopologyRemoteService.getNodeTopologyAndApgInfo(mapOfNeTypeToNenames);
            LOGGER.debug("Received Axe Node topology Info from Remote Interface: {}", axeNodeTopologyResponse);
        } catch (final Exception exception) {
            LOGGER.error("Errror while fetching AxeNodeTopology for nodes : {}", mapOfNeTypeToNenames.values());
            throw new AxeNodeTopologyDataNotFoundException();
        }
        if (axeNodeTopologyResponse == null) {
            LOGGER.debug("Retrieved nodeTopologyResulte :  {} ", nodeNameToAxeNodeTopology.size());
            return nodeNameToAxeNodeTopology;
        }
        buildAxeNodeTopologyDataForNe(axeNodeTopologyResponse, nodeNameToAxeNodeTopology);
        LOGGER.info("End of getAxeNodeTopology with : {} ", nodeNameToAxeNodeTopology);
        return nodeNameToAxeNodeTopology;
    }

    /**
     * @param persistentObjectsIterator
     * @param nodeNameToAxeNodeTopology
     */
    private void buildAxeNodeTopologyDataForNe(final AxeNodeTopologyResponse axeNodeTopologyResponse, final Map<String, AxeNodeTopologyData> nodeNameToAxeNodeTopology) {
        final List<AxeNodeTopology> clusteredNodes = axeNodeTopologyResponse.getNodeTopology();
        final List<NodesWithoutComponents> nonClusteredNodes = axeNodeTopologyResponse.getNodesWithoutComponents();
        if (!clusteredNodes.isEmpty()) {
            for (final AxeNodeTopology clusteredNode : clusteredNodes) {
                final AxeNodeTopologyData axeNodeTopologyData = new AxeNodeTopologyData(clusteredNode);
                nodeNameToAxeNodeTopology.put(axeNodeTopologyData.getNodeName(), axeNodeTopologyData);
            }
        }
        if (!nonClusteredNodes.isEmpty()) {
            for (final NodesWithoutComponents nonClusteredNode : nonClusteredNodes) {
                final AxeNodeTopologyData axeNodeTopologyData = new AxeNodeTopologyData(nonClusteredNode);
                nodeNameToAxeNodeTopology.put(axeNodeTopologyData.getNodeName(), axeNodeTopologyData);
            }
        }
    }

    /**
     * 
     * @param cacheData
     * @param nodeName
     * @param neType
     * 
     * @return
     */
    @Lock(LockType.READ)
    @AccessTimeout(value = 60000)
    public boolean isNodeDataExistInCacheData(final Map<String, Map<String, AxeNeUpgradeCacheData>> cacheData, final String nodeName, final String neType) {
        final boolean isNodeDataExist;
        if (cacheData.get(neType) != null && cacheData.get(neType).keySet().contains(nodeName)) {
            isNodeDataExist = true;
        } else {
            isNodeDataExist = false;
        }
        return isNodeDataExist;
    }

}
