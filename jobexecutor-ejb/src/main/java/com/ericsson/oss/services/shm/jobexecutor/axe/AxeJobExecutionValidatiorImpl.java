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
package com.ericsson.oss.services.shm.jobexecutor.axe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.activities.JobExecutionValidator;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.CapabilityUnavailableException;
import com.ericsson.oss.services.shm.common.modelservice.CapabilityProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * This class validates selected AXE network elements and prepare list of unsupported Network Elements, if any of the AXE NE's netype has components
 * 
 * @author xaniama
 */
@PlatformAnnotation(name = PlatformTypeEnum.AXE)
public class AxeJobExecutionValidatiorImpl implements JobExecutionValidator {

    @Inject
    private CapabilityProviderImpl capabilityProviderImpl;

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeJobExecutionValidatiorImpl.class);

    @Override
    public Map<NetworkElement, String> findUnSupportedNEs(final JobTypeEnum jobType, final List<NetworkElement> networkElementList) {
        final Map<NetworkElement, String> msgnetworkElementMap = new HashMap<>();
        if (JobTypeEnum.LICENSE_REFRESH == jobType) {
            for (final NetworkElement networkElement : networkElementList) {
                msgnetworkElementMap.put(networkElement, networkElement.getNeType() + " type nodes does not support Node License refresh");
            }
            return msgnetworkElementMap;
        } else {
            return new HashMap<>();
        }
    }

    public boolean hasBladeComponents(final String neType) {

        if (neType == null || neType.length() == 0) {
            return false;
        }
        String hasComponents;
        try {
            hasComponents = capabilityProviderImpl.getCapabilityValueForNeType(neType, SHMCapabilities.HASCOMPONENTS_CAPABILITY);
            if (hasComponents == null) {
                return false;
            }
        } catch (CapabilityUnavailableException e) {
            LOGGER.error("Considering that neType : {} does not have components due to : {} ", neType, e);
            return false;
        }
        LOGGER.debug("Returning hasBladeComponents : {} for neType : {} ", hasComponents, neType);
        return Boolean.valueOf(hasComponents);
    }

    /**
     * This method to fetch all supported parent ne name and its components, as backup job is all and only for components no need to check supported blade components for each ne type
     */
    public Map<String, List<NetworkElement>> findNesWithComponents(final JobTypeEnum jobTypeEnum, final List<NetworkElement> platformSpecificNEList,
            final List<Map<String, Object>> selectedNesWithComponentInfo, final Map<String, String> neDetailsWithParentName, final NetworkElementResponse networkElementResponse,
            final boolean flagForValidateNes) {
        if (flagForValidateNes) {
            final Map<String, List<NetworkElement>> selectedAxeNesGroupedByNeType = groupNetworkElementsByNeType(platformSpecificNEList);
            final Map<String, List<NetworkElement>> parentNeWithComponents = new HashMap<>();
            final Map<String, List<String>> nesWithComponents = prepareNeWithComponentNamesMap(selectedNesWithComponentInfo);
            for (Map.Entry<String, List<NetworkElement>> eachNeTypeEntry : selectedAxeNesGroupedByNeType.entrySet()) {
                if (JobTypeEnum.UPGRADE == jobTypeEnum) {
                    final boolean hasComponents = hasBladeComponents(eachNeTypeEntry.getKey());
                    if (hasComponents) {
                        prepareNesWithComponentsData(eachNeTypeEntry.getValue(), nesWithComponents, neDetailsWithParentName, parentNeWithComponents, networkElementResponse);
                    }
                } else if (JobTypeEnum.BACKUP == jobTypeEnum || JobTypeEnum.DELETEBACKUP == jobTypeEnum) {
                    prepareNesWithComponentsData(eachNeTypeEntry.getValue(), nesWithComponents, neDetailsWithParentName, parentNeWithComponents, networkElementResponse);
                }
            }
            LOGGER.debug("In findNesWithComponents parentNeWithComponents details are {}", parentNeWithComponents);
            return parentNeWithComponents;
        } else {
            return new HashMap<>();
        }
    }

    private Map<String, List<NetworkElement>> groupNetworkElementsByNeType(final List<NetworkElement> networkElementList) {

        final Map<String, List<NetworkElement>> neGroupsByNeType = new HashMap<>();

        for (final NetworkElement networkElement : networkElementList) {
            final String neType = networkElement.getNeType();
            List<NetworkElement> neList = neGroupsByNeType.get(neType);
            if (neList == null) {
                neList = new ArrayList<>();
                neGroupsByNeType.put(neType, neList);
            }
            neList.add(networkElement);
        }
        return neGroupsByNeType;
    }

    private void prepareNesWithComponentsData(final List<NetworkElement> nesOfSameNeType, final Map<String, List<String>> nesWithComponents, final Map<String, String> neDetailsWithParentName,
            final Map<String, List<NetworkElement>> parentNeWithComponents, final NetworkElementResponse networkElementResponse) {

        for (final NetworkElement axeParentNe : nesOfSameNeType) {
            if (nesWithComponents != null && !nesWithComponents.isEmpty() && nesWithComponents.containsKey(axeParentNe.getName())) {
                final List<NetworkElement> componentsData = prepareNetworkElementForComponents(axeParentNe, nesWithComponents.get(axeParentNe.getName()), neDetailsWithParentName);
                parentNeWithComponents.put(axeParentNe.getName(), componentsData);
                networkElementResponse.getSupportedNes().remove(axeParentNe);
            } else {
                networkElementResponse.getSupportedNes().remove(axeParentNe);
                networkElementResponse.getInvalidNes().put(axeParentNe, "No component is selected for the node ");
                LOGGER.debug("networkElementResponse.getInvalidNes() {}", networkElementResponse.getInvalidNes());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> prepareNeWithComponentNamesMap(final List<Map<String, Object>> selectedNesWithComponentInfo) {
        Map<String, List<String>> neWithComponents = null;
        if (selectedNesWithComponentInfo != null && !selectedNesWithComponentInfo.isEmpty()) {
            neWithComponents = new HashMap<>();
            for (final Map<String, Object> nenameVsComp : selectedNesWithComponentInfo) {
                neWithComponents.put((String) nenameVsComp.get(ShmConstants.NE_NAME), (List<String>) nenameVsComp.get(ShmConstants.SELECTED_COMPONENTS));
            }
        }
        return neWithComponents;
    }

    /**
     * @param neWithComps
     * @param list
     * @param neDetailsWithParentName
     * @return
     */
    private List<NetworkElement> prepareNetworkElementForComponents(final NetworkElement mainNe, final List<String> neComponents, final Map<String, String> neDetailsWithParentName) {
        final List<NetworkElement> networkElementsList = new ArrayList<>();
        for (final String neComponentName : neComponents) {
            LOGGER.debug("Resolving NetworkElement objects for Components : {}", neComponents);
            final NetworkElement networkElement = new NetworkElement();
            networkElement.setName(neComponentName);
            networkElement.setPlatformType(mainNe.getPlatformType());
            networkElement.setNeType(mainNe.getNeType());
            networkElementsList.add(networkElement);
            neDetailsWithParentName.put(neComponentName, mainNe.getName());
        }
        return networkElementsList;
    }

}
