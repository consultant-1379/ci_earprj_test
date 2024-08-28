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
package com.ericsson.oss.services.shm.activities;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

/**
 * This interface builds network elements for components of selected AXE NEs and removes selected AXE NE from unSupported NetworkElements and adding their components to supported NetworkElements
 * 
 * @author xaniama
 * 
 */

public interface NeComponentBuilder {

    void prepareNeJobPoProperties(List<NetworkElement> list, Map<String, Object> jobAttributes, Map<String, String> neDetailsWithParentName, NetworkElement string,
            List<NetworkElement> unSupportedNes);

}
