/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.presentation.server.shm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

public class JobCreationFromJobDetailsPageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCreationFromJobDetailsPageHelper.class);

    public static final String CLUSTER_SUFFIX = "-AXE_CLUSTER";
    public static final String NODE_COMPONENT_SEPERATOR = "__";

    public Map<String, List<NetworkElement>> prepareNetworkElementForComponents(final List<NetworkElement> neNamesList, final List<String> neComponentsList) {
        final Map<String, List<NetworkElement>> neWithComponentDetails = new HashMap<>();
        if (!neNamesList.isEmpty() && !neComponentsList.isEmpty()) {
            for (NetworkElement neName : neNamesList) {
                final List<NetworkElement> componentNetworkElementsList = groupNeWithComponents(neComponentsList, neName);
                if (!componentNetworkElementsList.isEmpty()) {
                    neWithComponentDetails.put(neName.getName(), componentNetworkElementsList);
                }
            }
        }
        LOGGER.debug("Nes with selected components {}", neWithComponentDetails);
        return neWithComponentDetails;
    }

    /**
     * @param neComponents
     * @param ne
     * @param networkElementsList
     */
    private List<NetworkElement> groupNeWithComponents(final List<String> neComponentList, final NetworkElement ne) {
        final List<NetworkElement> componentNetworkElementsList = new ArrayList<>();
        for (String neComponent : neComponentList) {
            if ((neComponent.contains(NODE_COMPONENT_SEPERATOR) || neComponent.contains(CLUSTER_SUFFIX))
                    && (ne.getName().equals((neComponent.split(NODE_COMPONENT_SEPERATOR)[0])) || ne.getName().equals((neComponent.split(CLUSTER_SUFFIX)[0])))) {
                final NetworkElement networkElement = new NetworkElement();
                networkElement.setName(neComponent);
                networkElement.setPlatformType(ne.getPlatformType());
                networkElement.setNeType(ne.getNeType());
                componentNetworkElementsList.add(networkElement);
                LOGGER.debug("Ne with component names {}", componentNetworkElementsList);
            }
        }
        return componentNetworkElementsList;
    }

    public List<String> prepareNeNames(List<String> neNames) {
        final List<String> neNamesToReadNetworkElementMO = new ArrayList<>();
        if (!neNames.isEmpty()) {
            for (String nename : neNames) {
                if (nename.contains(NODE_COMPONENT_SEPERATOR)) {
                    neNamesToReadNetworkElementMO.addAll(Arrays.asList(nename.split(NODE_COMPONENT_SEPERATOR)[0]));
                } else if (nename.contains(CLUSTER_SUFFIX)) {
                    neNamesToReadNetworkElementMO.addAll(Arrays.asList(nename.split(CLUSTER_SUFFIX)[0]));
                } else {
                    neNamesToReadNetworkElementMO.add(nename);
                }
            }
        }
        return new ArrayList<>(new HashSet<>(neNamesToReadNetworkElementMO));
    }

}
