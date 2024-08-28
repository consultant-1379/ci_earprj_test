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
package com.ericsson.oss.services.shm.jobexecutor.cpp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.activities.JobExecutionValidator;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

@PlatformAnnotation(name = PlatformTypeEnum.CPP)
public class CppJobExecutionValidatiorImpl implements JobExecutionValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CppJobExecutionValidatiorImpl.class);

    @Inject
    private NetworkElementGroupPreparator neGroupPreparator;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activities.JobExecutionValidator#findUnSupportedNEs(com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum, java.util.List)
     */
    @Override
    public Map<NetworkElement, String> findUnSupportedNEs(final JobTypeEnum jobType, final List<NetworkElement> networkElementList) {
        final Map<NetworkElement, String> msgnetworkElementMap = new HashMap<>();
        if (JobTypeEnum.LICENSE_REFRESH == jobType) {
            LOGGER.info("Unsupported nodes for License Refresh job {}", networkElementList);
            for (final NetworkElement networkElement : networkElementList) {
                msgnetworkElementMap.put(networkElement, networkElement.getNeType() + " type nodes does not support License Refresh job.");
            }
            return msgnetworkElementMap;
        } else {
            final Map<NetworkElement, String> MsgnetworkElementMap = new HashMap<NetworkElement, String>();
            final NetworkElementGroup cppNetworkElementGroup = neGroupPreparator.groupNetworkElementsByModelidentity(networkElementList);

            final Map<String, String> cppNetworkElementMap = cppNetworkElementGroup.getUnSupportedNetworkElements();

            //Prepare a map of all the selected CPP NEs, with key as node name.
            final Map<String, NetworkElement> networkElementMap = new HashMap<String, NetworkElement>();
            for (final NetworkElement ecimNE : networkElementList) {
                networkElementMap.put(ecimNE.getName(), ecimNE);
            }
            for (Entry<String, String> networkElement : cppNetworkElementMap.entrySet()) {
                if (networkElementMap.containsKey(networkElement.getKey())) {
                    MsgnetworkElementMap.put(networkElementMap.get(networkElement.getKey()), networkElement.getValue());
                }
            }
            return MsgnetworkElementMap;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activities.JobExecutionValidator#findNesWithComponents(java.util.List, java.util.List, java.util.Map,
     * com.ericsson.oss.services.shm.networkelement.NetworkElementResponse)
     */
    @Override
    public Map<String, List<NetworkElement>> findNesWithComponents(final JobTypeEnum jobTypeEnum, final List<NetworkElement> platformSpecificNEList,
            final List<Map<String, Object>> nesWithComponentInfo, final Map<String, String> neDetailsWithParentName, final NetworkElementResponse networkElementResponse,
            final boolean flagForValidateNes) {
        return null;
    }

}
