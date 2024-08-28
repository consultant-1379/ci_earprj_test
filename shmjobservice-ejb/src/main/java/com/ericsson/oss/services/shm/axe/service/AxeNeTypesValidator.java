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
package com.ericsson.oss.services.shm.axe.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobservice.common.NEJobInfo;

/**
 * This class is used validate Axe platform supported neTypes
 * 
 * @author Team Royals
 *
 */
public class AxeNeTypesValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeNeTypesValidator.class);

    /**
     * method to get AXE platform supported NeTypes
     * 
     * @param supportedNeTypesByPlatformsMap
     * @return supportedAxeNeTypes
     */
    public Set<String> getAxeNeTypes(final Map<String, Set<String>> supportedNeTypesByPlatformsMap) {
        if (!supportedNeTypesByPlatformsMap.isEmpty() && supportedNeTypesByPlatformsMap.containsKey(PlatformTypeEnum.AXE.getName())) {
            return supportedNeTypesByPlatformsMap.get(PlatformTypeEnum.AXE.getName());
        }
        return Collections.emptySet();
    }

    /**
     * method to get AXE platform unsupported Node Names
     * 
     * @param supportedNeTypes
     * @param neTypesToNeJobsMap
     * @return
     */
    public Set<String> getUnSupportedNodes(final Set<String> supportedNeTypes, final Map<String, List<NEJobInfo>> neTypesToNeJobsMap) {
        neTypesToNeJobsMap.keySet().removeAll(supportedNeTypes);
        final Set<String> unSupportedNodes = new HashSet<>();
        if (!neTypesToNeJobsMap.isEmpty()) {
            for (final Map.Entry<String, List<NEJobInfo>> entry : neTypesToNeJobsMap.entrySet()) {
                final List<NEJobInfo> listOfNEJobInfo = entry.getValue();
                for (final NEJobInfo eachNEJobInfo : listOfNEJobInfo) {
                    unSupportedNodes.add(eachNEJobInfo.getNodeName());
                }
            }
        }
        LOGGER.debug("UnSupported Nodes: {}", unSupportedNodes);
        return unSupportedNodes;
    }
}
