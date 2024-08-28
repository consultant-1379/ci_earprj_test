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
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

/*
 * This class prepare list of supported NEs if any AXE nodes are selected whose ne types can have components,
 *  by adding the corresponding components and removing their parent nes
 *  */
public class SupportedNesListBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedNesListBuilder.class);

    public List<NetworkElement> buildSupportedNesListForNeJobsCreation(final Map<String, List<NetworkElement>> nesWithComponents, final List<NetworkElement> supportedNetworkElements) {
        if (nesWithComponents != null) {
            for (final Entry<String, List<NetworkElement>> neWithCompo : nesWithComponents.entrySet()) {
                supportedNetworkElements.addAll(neWithCompo.getValue());
            }
            LOGGER.debug("final list of supportedNetworkElements for job creation are {}", supportedNetworkElements);
        }
        return supportedNetworkElements;
    }
}
