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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade.cache;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.modelservice.CapabilityProviderImpl;

/**
 * This class provides nodeFunction value for the provided neType
 * 
 * @author xaniama
 * 
 */
public class AxeActivityNodeFunctionProvider {

    @Inject
    private CapabilityProviderImpl capabilityProviderImpl;

    public String getNodeFunctionBasedOnNeType(final String neType) {
        return capabilityProviderImpl.getCapabilityValueForNeType(neType, SHMCapabilities.NODEFUNCTION_CAPABILITY);
    }
}
