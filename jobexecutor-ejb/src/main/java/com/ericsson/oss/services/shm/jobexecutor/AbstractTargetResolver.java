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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobexecutorlocal.TargetResolver;

/**
 * Abstract Target Resolver
 * 
 * @author xeswpot
 * 
 */
public abstract class AbstractTargetResolver implements TargetResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTargetResolver.class);

    /**
     * 
     * @param neNames
     * @return
     */
    protected List<NetworkElement> buildNetworkElement(final List<String> neNames) {

        LOGGER.debug("Building NetworkElements for : {}", neNames);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        for (final String neName : neNames) {
            final NetworkElement networkElement = new NetworkElement();
            networkElement.setName(neName);
            networkElementsList.add(networkElement);
        }
        return networkElementsList;
    }

}
