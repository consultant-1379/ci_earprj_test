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
package com.ericsson.oss.services.shm.common.networkelement;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.model.NetworkElementData;

public class NetworkElementAttributes implements NetworkElementData, Serializable {

    private static final long serialVersionUID = 1L;
    private String neType;
    private List<Map<String, String>> neProductVersion;
    private String neFdn;
    private String nodeRootFdn;
    private String ossModelIdentity;
    private String utcOffset;
    private String nodeModelIdentity;
    private String ossPrefix;

    public NetworkElementAttributes(final String neType, final List<Map<String, String>> neProductVersion, final String neFdn, final String nodeRootFdn, final String ossModelIdentity,
            final String utcOffset, final String nodeModelIdentity) {
        this.neType = neType;
        this.neProductVersion = neProductVersion;
        this.neFdn = neFdn;
        this.nodeRootFdn = nodeRootFdn;
        this.ossModelIdentity = ossModelIdentity;
        this.utcOffset = utcOffset;
        this.nodeModelIdentity = nodeModelIdentity;
    }

    @Override
    public String getNeType() {
        return neType;
    }

    @Override
    public List<Map<String, String>> getNeProductVersion() {
        return neProductVersion;
    }

    @Override
    public String getNeFdn() {
        return neFdn;
    }

    @Override
    public String getNodeRootFdn() {
        return nodeRootFdn;
    }

    @Override
    public String getOssModelIdentity() {
        return ossModelIdentity;
    }

    @Override
    public String getUtcOffset() {
        return utcOffset;
    }

    @Override
    public void setNeType(final String neType) {
        this.neType = neType;
    }

    @Override
    public void setNeProductVersion(final List<Map<String, String>> neProductVersion) {
        this.neProductVersion = neProductVersion;
    }

    @Override
    public void setNeFdn(final String neFdn) {
        this.neFdn = neFdn;
    }

    @Override
    public void setNodeRootFdn(final String nodeRootFdn) {
        this.nodeRootFdn = nodeRootFdn;
    }

    @Override
    public void setOssModelIdentity(final String ossModelIdentity) {
        this.ossModelIdentity = ossModelIdentity;
    }

    @Override
    public void setUtcOffset(final String utcOffset) {
        this.utcOffset = utcOffset;
    }

    @Override
    public void setNodeModelIdentity(final String nodeModelIdentity) {
        this.nodeModelIdentity = nodeModelIdentity;
    }

    @Override
    public String getNodeModelIdentity() {
        return nodeModelIdentity;
    }

    public String getOssPrefix() {
        return ossPrefix;
    }

    public void setOssPrefix(final String ossPrefix) {
        this.ossPrefix = ossPrefix;
    }

    @Override
    public String toString() {
        return "NetworkElementAttributes [neType=" + neType + ", neProductVersion=" + neProductVersion + ", neFdn=" + neFdn + ", nodeRootFdn=" + nodeRootFdn + ", ossModelIdentity=" + ossModelIdentity
                + ", utcOffset=" + utcOffset + ", nodeModelIdentity=" + nodeModelIdentity + "]";
    }

}