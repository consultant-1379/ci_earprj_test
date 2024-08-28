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
package com.ericsson.oss.services.shm.jobexecutor.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.activities.NeComponentBuilder;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

/**
 * default implementation to build Ne components for platform type other than AXE
 * 
 * @author tcsanpr
 * 
 */
public class NeComponentsInfoBuilderImpl implements NeComponentBuilder {

    /**
     * @param selectedNE
     * @param neDetailsWithParentName
     * @return
     */
    public Map<String, String> findParentNeNameForSelectedNe(final NetworkElement selectedNE, final Map<String, String> neDetailsWithParentName) {
        final Map<String, String> neAndComponentNames = new HashMap<>();
        final String parentName = neDetailsWithParentName.get(selectedNE.getName());
        neAndComponentNames.put(selectedNE.getName(), parentName);
        return neAndComponentNames;
    }

    @Override
    public void prepareNeJobPoProperties(final List<NetworkElement> supportedAxeNes, final Map<String, Object> jobAttributes, final Map<String, String> neDetailsWithParentName,
            final NetworkElement selectedNE, final List<NetworkElement> unSupportedAxeNes) {
        //Default implementation is not needed
    }

}
