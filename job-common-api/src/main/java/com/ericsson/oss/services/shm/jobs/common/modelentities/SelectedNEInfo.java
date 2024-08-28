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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

public class SelectedNEInfo {
    private List<String> collectionNames;
    private final List<NetworkElement> networkElements;
    private List<String> savedSearchIds;
    private List<Map<String, Object>> neWithComponentInfo;
    private List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails;

    public List<Map<String, Object>> getNeWithComponentInfo() {
        return neWithComponentInfo;
    }

    public SelectedNEInfo(final List<NetworkElement> networkElements, final NEInfo neInfo) {
        this.networkElements = networkElements;
        if (neInfo != null) {
            this.collectionNames = neInfo.getCollectionNames();
            this.savedSearchIds = neInfo.getSavedSearchIds();
            this.neWithComponentInfo = neInfo.getNeWithComponentInfo();
            this.neTypeComponentActivityDetails = neInfo.getNeTypeComponentActivityDetails();
        }

    }

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public List<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    public List<String> getSavedSearchIds() {
        return savedSearchIds;
    }

    public List<NeTypeComponentActivityDetails> getNeTypeComponentActivityDetails() {
        return neTypeComponentActivityDetails;
    }

    @Override
    public String toString() {
        return "SelectedNEInfo [collectionNames=" + collectionNames + ", networkElements=" + networkElements + ", savedSearchIds=" + savedSearchIds + ", neWithComponentInfo=" + neWithComponentInfo
                + ", neTypeComponentActivityDetails=" + neTypeComponentActivityDetails + "]";
    }

}
