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
package com.ericsson.oss.services.shm.jobservice.axe;

/**
 * This is a pojo class which is used to send as response of /ops-sessionid-clusterid rest call.It is specific to AXE
 *
 * @author Team Royals
 */
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpsResponseData {

    private Set<String> unSupportedNodes;
    private Map<String, List<OpsSessionAndClusterIdInfo>> opsSessionAndClusterIdInfo;
    private boolean hasAccessToOPSGUI;

    /**
     * @return the opsSessionAndClusterIdInfo
     */
    public Map<String, List<OpsSessionAndClusterIdInfo>> getOpsSessionAndClusterIdInfo() {
        return opsSessionAndClusterIdInfo;
    }

    /**
     * @param opsSessionAndClusterIdInfo
     *            the opsSessionAndClusterIdInfo to set
     */
    public void setOpsSessionAndClusterIdInfo(final Map<String, List<OpsSessionAndClusterIdInfo>> opsSessionAndClusterIdInfo) {
        this.opsSessionAndClusterIdInfo = opsSessionAndClusterIdInfo;
    }

    /**
     * @return the unSupportedNodes
     */
    public Set<String> getUnSupportedNodes() {
        return unSupportedNodes;
    }

    /**
     * @param unSupportedNodes
     *            the unSupportedNodes to set
     */
    public void setUnSupportedNodes(final Set<String> unSupportedNodes) {
        this.unSupportedNodes = unSupportedNodes;
    }

    public boolean isHasAccessToOPSGUI() {
        return hasAccessToOPSGUI;
    }

    public void setHasAccessToOPSGUI(final boolean hasAccessToOPSGUI) {
        this.hasAccessToOPSGUI = hasAccessToOPSGUI;
    }

    @Override
    public String toString() {
        final StringBuilder opsResponseData = new StringBuilder();
        opsResponseData.append("{unSupportedNodes:").append(this.unSupportedNodes).append(",opsSessionAndClusterIdInfo:").append(this.opsSessionAndClusterIdInfo).append(",hasAccessToOPSGUI:")
                .append(hasAccessToOPSGUI).append("}");
        return opsResponseData.toString();
    }
}