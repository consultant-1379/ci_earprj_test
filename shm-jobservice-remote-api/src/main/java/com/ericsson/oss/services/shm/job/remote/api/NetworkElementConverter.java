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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.List;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;

/**
 * As we cannot pass a local parameter like ProductData (directly or indirectly) to a remote business method, defining ProductDataConverter attribute attribute here which is duplicate of ProductData
 * pojo
 * 
 * @author xkalkil
 *
 */
public class NetworkElementConverter implements Serializable {
    private static final long serialVersionUID = 1234567L;
    private final String networkElementFdn;
    private final String nodeRootFdn;
    private final PlatformTypeEnum platformType;
    private final String name;
    private final String neType;
    private final String ossModelIdentity;
    private final String nodeModelIdentity;
    private final List<ProductDataConverter> neProductVersion;
    private final String utcOffset;
    private final String timeZone;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public NetworkElementConverter(final String networkElementFdn, final String nodeRootFdn, final PlatformTypeEnum platformType, final String name, final String neType, final String ossModelIdentity,
            final String nodeModelIdentity, final List<ProductDataConverter> neProductVersion, final String utcOffset, final String timeZone) {
        this.networkElementFdn = networkElementFdn;
        this.nodeRootFdn = nodeRootFdn;
        this.platformType = platformType;
        this.name = name;
        this.neType = neType;
        this.ossModelIdentity = ossModelIdentity;
        this.nodeModelIdentity = nodeModelIdentity;
        this.neProductVersion = neProductVersion;
        this.utcOffset = utcOffset;
        this.timeZone = timeZone;
    }

    public String getNetworkElementFdn() {
        return networkElementFdn;
    }

    public String getNodeRootFdn() {
        return nodeRootFdn;
    }

    public PlatformTypeEnum getPlatformType() {
        return platformType;
    }

    public String getName() {
        return name;
    }

    public String getNeType() {
        return neType;
    }

    public String getOssModelIdentity() {
        return ossModelIdentity;
    }

    public String getNodeModelIdentity() {
        return nodeModelIdentity;
    }

    public List<ProductDataConverter> getNeProductVersion() {
        return neProductVersion;
    }

    public String getUtcOffset() {
        return utcOffset;
    }

    public String getTimeZone() {
        return timeZone;
    }

}
