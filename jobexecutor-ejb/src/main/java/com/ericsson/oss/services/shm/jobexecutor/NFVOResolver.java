/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * 
 * @author xeswpot
 * 
 */
public class NFVOResolver extends AbstractTargetResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NFVOResolver.class);

    @Override
    public NetworkElementResponse getNetworkElementResponse(final long mainJobId, final List<String> neNames, final long templateJobId, final Map<String, Object> attributeMap,
            final JobTypeEnum jobType, final boolean isMainJobCreated) {

        LOGGER.info("Resolving NetworkElement objects for NEs : {}", neNames);
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        final List<NetworkElement> supportedNes = new ArrayList<NetworkElement>();

        if (!neNames.isEmpty()) {
            final List<NetworkElement> networkElementsList = buildNetworkElement(neNames);
            for (final NetworkElement networkElement : networkElementsList) {
                networkElement.setPlatformType(PlatformTypeEnum.vRAN);
                networkElement.setNeType("NFVO");
                supportedNes.add(networkElement);
            }
        }

        networkElementResponse.setSupportedNes(supportedNes);
        return networkElementResponse;
    }
}
