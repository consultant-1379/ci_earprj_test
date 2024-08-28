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
package com.ericsson.oss.presentation.server.shm.util;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.NetworkElementFilterResponseProvider;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementFilterResponse;
import com.ericsson.oss.services.shm.inventory.common.FilterRequestQuery;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

@Path("neNames")
public class NetworkElementValidationFacade {

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private NetworkElementResponse networkElementResponse;

    @Inject
    private NetworkElementFilterResponseProvider networkElementFilterResponseProvider;

    @Inject
    private JobCreationFromJobDetailsPageHelper jobCreationFromJobDetailsPageHelper;

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkElementResponse filterSupportedNEs(final List<String> neNames) {
        final List<NetworkElement> networkElementsList = fdnServiceBean.getNetworkElementsByNeNames(neNames);
        networkElementResponse.setSupportedNes(networkElementsList);
        return networkElementResponse;
    }

    @POST
    @Path("/v1/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkElementResponse filterSupportedNEs(final FilterRequestQuery filterRequestQuery) {
        final List<String> neNames = filterRequestQuery.getNetworkElements();
        final NetworkElementFilterResponse networkElementFilterResponse = networkElementFilterResponseProvider.getNetworkElementsByNeNames(jobCreationFromJobDetailsPageHelper.prepareNeNames(neNames),
                filterRequestQuery.getContext());
        final List<NetworkElement> availableNes = networkElementFilterResponse.getAvailableNes();
        networkElementResponse.setSupportedNes(availableNes);
        networkElementResponse.setNesWithComponents(jobCreationFromJobDetailsPageHelper.prepareNetworkElementForComponents(availableNes, neNames));
        return networkElementResponse;
    }

}
