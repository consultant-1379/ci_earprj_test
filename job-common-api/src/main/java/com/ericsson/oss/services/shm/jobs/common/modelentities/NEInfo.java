package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;
import java.util.Map;

public class NEInfo {
    private List<String> collectionNames;
    private List<String> neNames;
    private List<String> savedSearchIds;
    private List<Map<String, Object>> neWithComponentInfo;
    private List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails;

    public List<Map<String, Object>> getNeWithComponentInfo() {
        return neWithComponentInfo;
    }

    public void setNeWithComponentInfo(final List<Map<String, Object>> neWithComponentInfo) {
        this.neWithComponentInfo = neWithComponentInfo;
    }

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public void setCollectionNames(final List<String> collectionNames) {
        this.collectionNames = collectionNames;
    }

    public List<String> getNeNames() {
        return neNames;
    }

    public void setNeNames(final List<String> neNames) {
        this.neNames = neNames;
    }

    public List<String> getSavedSearchIds() {
        return savedSearchIds;
    }

    public void setSavedSearchIds(final List<String> savedSearchIds) {
        this.savedSearchIds = savedSearchIds;
    }

    public List<NeTypeComponentActivityDetails> getNeTypeComponentActivityDetails() {
        return neTypeComponentActivityDetails;
    }

    public void setNeTypeComponentActivityDetails(final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails) {
        this.neTypeComponentActivityDetails = neTypeComponentActivityDetails;
    }

    @Override
    public String toString() {
        return "NEInfo [collectionNames=" + collectionNames + ", neNames=" + neNames + ", savedSearchIds=" + savedSearchIds + ", neWithComponentInfo=" + neWithComponentInfo
                + ", neTypeComponentActivityDetails=" + neTypeComponentActivityDetails + "]";
    }

}
