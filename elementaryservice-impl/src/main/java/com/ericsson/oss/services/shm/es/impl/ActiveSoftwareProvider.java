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
package com.ericsson.oss.services.shm.es.impl;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.software.remote.api.SoftwareInventoryRemoteService;

/**
 * @author tcssbop This class is used to fetch the active software details.
 */
@Stateless
public class ActiveSoftwareProvider {

    @EServiceRef
    private SoftwareInventoryRemoteService softwareInventoryRemoteService;

    public Map<String, String> getActiveSoftwareDetails(final List<String> nodeNames) {
        return softwareInventoryRemoteService.getActiveSoftwareDetails(nodeNames);
    }

}
