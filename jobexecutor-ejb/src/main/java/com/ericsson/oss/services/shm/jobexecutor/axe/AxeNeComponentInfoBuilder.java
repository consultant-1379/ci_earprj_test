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

import com.ericsson.oss.services.shm.activities.NeComponentBuilder;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobexecutor.JobExecutorServiceHelper;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class builds network elements for components of selected AXE NEs and removes selected AXE NE from unSupported NetworkElements and adding their components to supported NetworkElements
 * 
 * @author xaniama
 * 
 */

@PlatformAnnotation(name = PlatformTypeEnum.AXE)
public class AxeNeComponentInfoBuilder implements NeComponentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeNeComponentInfoBuilder.class);

    @Inject
    private JobExecutorServiceHelper executorServiceHelper;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activities.NeComponentBuilder#prepareNeJobPoProperties(java.util.List, java.util.Map, java.util.Map, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void prepareNeJobPoProperties(final List<NetworkElement> supportedAxeNes, final Map<String, Object> jobAttributes, final Map<String, String> neDetailsWithParentName,
            final NetworkElement selectedNE, final List<NetworkElement> unSupportedAxeNes) {
        List<Map<String, Object>> neJobPropertiesList = (List<Map<String, Object>>) jobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (neJobPropertiesList == null) {
            neJobPropertiesList = new ArrayList<>();
        }
        if (neDetailsWithParentName.containsKey(selectedNE.getName())) {
            prepareJobPropertyList(neJobPropertiesList, ShmConstants.IS_COMPONENT_JOB, "true");
            prepareJobPropertyList(neJobPropertiesList, ShmConstants.PARENT_NAME, neDetailsWithParentName.get(selectedNE.getName()));
        }
        prepareJobProperties(supportedAxeNes, selectedNE, neDetailsWithParentName, ShmConstants.AXE_NES, neJobPropertiesList, jobAttributes);
        if (unSupportedAxeNes != null && !unSupportedAxeNes.isEmpty()) {
            prepareJobProperties(unSupportedAxeNes, selectedNE, neDetailsWithParentName, ShmConstants.AXE_UNSUPPORTED_NES, neJobPropertiesList, jobAttributes);
        }
    }

    private void prepareJobProperties(final List<NetworkElement> axeNes, final NetworkElement selectedNE, final Map<String, String> neDetailsWithParentName, final String key,
            final List<Map<String, Object>> neJobPropertiesList, final Map<String, Object> jobAttributes) {
        final Map<String, List<NetworkElement>> axeNesBasedOnNeType = executorServiceHelper.groupNetworkElementsByNeType(axeNes);
        final String neType = selectedNE.getNeType();
        final StringBuilder concatinatedNes = new StringBuilder();
        final List<NetworkElement> axeNetworkElements = axeNesBasedOnNeType.get(neType);
        if (axeNetworkElements != null && !axeNetworkElements.isEmpty()) {
            int counter = 1;
            for (NetworkElement supportedNe : axeNetworkElements) {
                final String nodeName = supportedNe.getName();
                if (neDetailsWithParentName.containsKey(nodeName)) {
                    concatinatedNes.append(nodeName + ShmConstants.AXE_NODENAME_DELIMITER + neDetailsWithParentName.get(nodeName));
                } else {
                    concatinatedNes.append(nodeName);
                }
                if (axeNes.size() != counter) {
                    concatinatedNes.append(",");
                    counter++;
                }
            }
            LOGGER.debug("axeNes in job for type {} are {} ", neType, concatinatedNes);
            prepareJobPropertyList(neJobPropertiesList, key, concatinatedNes.toString());
            jobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertiesList);
            LOGGER.info("jobAttributes in AxeUpgrade job are {}", jobAttributes);
        }
    }

    public List<Map<String, Object>> prepareJobPropertyList(final List<Map<String, Object>> jobPropertyList, final String propertyName, final String propertyValue) {
        final Map<String, Object> jobProperty = new HashMap<>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, propertyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, propertyValue);
        jobPropertyList.add(jobProperty);
        return jobPropertyList;

    }
    
}