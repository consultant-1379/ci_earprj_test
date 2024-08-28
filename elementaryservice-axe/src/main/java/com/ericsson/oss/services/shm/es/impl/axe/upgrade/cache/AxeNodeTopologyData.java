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
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.topology.axe.rest.api.AxeNodeTopology;
import com.ericsson.oss.services.shm.topology.axe.rest.api.NodeTopologyComponents;
import com.ericsson.oss.services.shm.topology.axe.rest.api.NodesWithoutComponents;

/**
 * Contains AxeNodeTopology PO attributes which are used to build additional parameters for ops script execution.
 * 
 * @author xaniama
 * 
 */
public class AxeNodeTopologyData {

    private String nodeName;

    /**
     * Each component under blade cluster/SPX will be mapped to respective folder names as shown in UI like (MSC-BC/HLR/SPX) For MSC Ex:{MSC-BC=[BC1, BC2, BC3, BC4],SPX=[CP0,CP1]}
     */
    private Map<String, List<String>> bladeClusterSpxComponents;

    private String numberOfAPG;

    public AxeNodeTopologyData(final AxeNodeTopology clusteredNode) {
        setAxeNodeTopologyAttributesForClusteredNodes(clusteredNode);
    }

    public AxeNodeTopologyData(final NodesWithoutComponents nonClusteredNode) {
        setAxeNodeTopologyAttributesForNonClusteredNodes(nonClusteredNode);
    }

    /**
     * @return the numberOfAPG
     */
    public String getNumberOfAPG() {
        return numberOfAPG;
    }

    /**
     * @param numberOfAPG
     *            the numberOfAPG to set
     */
    public void setNumberOfAPG(final String numberOfAPG) {
        this.numberOfAPG = numberOfAPG;
    }

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName
     *            the nodeName to set
     */
    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * @return the bladeClusterSpxComponents
     */
    public Map<String, List<String>> getComponents() {
        return bladeClusterSpxComponents;
    }

    /**
     * @param bladeClusterSpxComponents
     *            the bladeClusterSpxComponents to set
     */
    public void setComponents(final Map<String, List<String>> bladeClusterSpxComponents) {
        this.bladeClusterSpxComponents = bladeClusterSpxComponents;
    }

    /**
     * @param clusteredNode
     */
    private void setAxeNodeTopologyAttributesForClusteredNodes(final AxeNodeTopology clusteredNode) {
        this.nodeName = clusteredNode.getNodeName();
        this.numberOfAPG = String.valueOf(clusteredNode.getNumberOfAPG());
        this.bladeClusterSpxComponents = new HashMap<>();
        final List<NodeTopologyComponents> nodeTopologyComponents = clusteredNode.getComponents();
        for (final NodeTopologyComponents nodeTopologyComponent : nodeTopologyComponents) {
            this.bladeClusterSpxComponents.put(nodeTopologyComponent.getName(), nodeTopologyComponent.getCpNames());
        }
    }

    /**
     * @param nonClusteredNode
     */
    private void setAxeNodeTopologyAttributesForNonClusteredNodes(final NodesWithoutComponents nonClusteredNode) {
        this.nodeName = nonClusteredNode.getNeName();
        this.numberOfAPG = String.valueOf(nonClusteredNode.getNumberOfAPG());

    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BladeClusterSpxComponents are ").append(bladeClusterSpxComponents).append(" nodeName is ").append(nodeName).append(" numberOfAPG are ").append(numberOfAPG);
        return builder.toString();
    }
}
